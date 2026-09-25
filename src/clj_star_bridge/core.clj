(ns clj-star-bridge.core
  (:require [aleph.http :as http]
            [clojure.string :as str]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [cheshire.core :as json]
            [hiccup2.core :as h]
            [hiccup.page :as page]
            [starfederation.datastar.clojure.api :as d*]
            [starfederation.datastar.clojure.adapter.aleph :as datastar-aleph]))

;; グローバル状態
(defonce counter (atom 0))
(defonce sse-clients (atom #{}))

(def sse-headers
  {"Content-Type" "text/event-stream"
   "Cache-Control" "no-cache"
   "X-Accel-Buffering" "no"})

(def datastar-script-url
  "https://cdn.jsdelivr.net/gh/starfederation/datastar@v1.0.3/bundles/datastar.js")

(def react-asset-base-url
  (or (System/getenv "CLJ_REACT_ASSET_BASE_URL")
      "http://localhost:3000"))

(defn react-asset-url [path]
  (str (str/replace react-asset-base-url #"/$" "") path))

(defn react-assets []
  [[:link {:rel "stylesheet"
           :href (react-asset-url "/output.css")}]
   [:script {:defer true
             :src (react-asset-url "/js/main.js")}]])

(defn sse-data-lines [data]
  (let [body (if (string? data)
               data
               (json/generate-string data))]
    (map #(str "data: " %) (str/split-lines body))))

(defn sse-frame
  "Build a standards-compliant SSE frame from optional metadata and data."
  [{:keys [event id retry data] :as message}]
  (let [data (if (contains? message :data) data message)
        fields (cond-> []
                 id (conj (str "id: " id))
                 event (conj (str "event: " event))
                 retry (conj (str "retry: " retry)))]
    (str (str/join "\n" (concat fields (sse-data-lines data)))
         "\n\n")))

(defn sse-response [body]
  {:status 200
   :headers sse-headers
   :body body})

(defn counter-panel [n]
  [:section#counter-panel
   [:p "Count: " [:span#count n]]
   [:button {:data-on:click "@get('/increment')"} "+1"]])

(defn activity-status [message]
  [:p#activity-status message])

(defn greeting-message [name]
  (if (str/blank? name)
    "Enter your name"
    (str "Hello, " name "!")))

(defn greeting [name]
  [:p#greeting (greeting-message name)])

(defn signal-demo []
  [:section#signal-demo
   [:h2 "Signals"]
   [:label {:for "name"} "Name: "]
   [:input#name {:type "text"
                 :data-bind "name"}]
   [:button {:data-on:click "@post('/greet')"} "Greet"]
   (greeting "")])

(defn live-status [update-count server-time]
  [:div#live-status
   [:p "Server time: " [:time server-time]]
   [:p "Updates: " [:span#live-update-count update-count]]])

(defn live-status-stream []
  [:section#live-status-stream {:data-init "@get('/live-status')"}
   [:h2 "Live Status"]
   (live-status 0 "Connecting...")])

(defn react-demo []
  [:section#react-demo
   [:h2 "React Component"]
   [:div#app {:data-ignore-morph true}
    [:p "Loading React component..."]]])

(defn counter-panel-fragment [n]
  (str (h/html (counter-panel n))))

(defn activity-status-fragment [message]
  (str (h/html (activity-status message))))

(defn greeting-fragment [name]
  (str (h/html (greeting name))))

(defn live-status-fragment [update-count]
  (str (h/html (live-status update-count (str (java.time.Instant/now))))))

(defn increment-response [request n message]
  (if (d*/datastar-request? request)
    (datastar-aleph/->sse-response
     request
     {datastar-aleph/on-open
      (fn [sse]
        (d*/with-open-sse sse
          (d*/patch-elements! sse (counter-panel-fragment n))
          (d*/patch-elements! sse (activity-status-fragment message))))})
    {:status 200
     :headers {"Content-Type" "text/plain"}
     :body (str n)}))

(defn request-signals [request]
  (if-let [body (d*/get-signals request)]
    (json/parse-string (slurp body) true)
    {}))

(defn normalize-name [value]
  (if (string? value)
    (str/trim value)
    ""))

(defn greeting-response [request name]
  (if (d*/datastar-request? request)
    (datastar-aleph/->sse-response
     request
     {datastar-aleph/on-open
      (fn [sse]
        (d*/with-open-sse sse
          (d*/patch-elements! sse (greeting-fragment name))
          (d*/patch-signals! sse (json/generate-string {:name name}))))})
    {:status 200
     :headers {"Content-Type" "text/plain; charset=utf-8"}
     :body (greeting-message name)}))

(defn start-live-status! [sse]
  (Thread/startVirtualThread
   (fn []
     (try
       (loop [update-count 1]
         (when @(d*/patch-elements! sse (live-status-fragment update-count))
           (Thread/sleep 1000)
           (recur (inc update-count))))
       (catch InterruptedException _)
       (catch Exception e
         (println (str "Datastar live status failed: " e)))
       (finally
         (d*/close-sse! sse))))))

(defn live-status-response [request]
  (let [worker (atom nil)]
    (datastar-aleph/->sse-response
     request
     {datastar-aleph/on-open
      (fn [sse]
        (println "Datastar live status connected")
        (reset! worker (start-live-status! sse)))
      datastar-aleph/on-close
      (fn [_]
        (println "Datastar live status disconnected")
        (when-let [thread @worker]
          (.interrupt ^Thread thread)))})))

(defn remove-client! [ch]
  (swap! sse-clients disj ch))

(defn send-sse! [ch payload]
  (try
    (d/on-realized
     (s/put! ch (sse-frame {:data payload}))
     (fn [accepted?]
       (when-not accepted?
         (remove-client! ch)))
     (fn [e]
       (println (str "Error sending SSE: " e))
       (remove-client! ch)))
    (catch Exception e
      (println (str "Error sending SSE: " e))
      (remove-client! ch))))

(defn broadcast! [payload]
  (doseq [ch @sse-clients]
    (send-sse! ch payload)))

;; HTML ページ
(defn layout []
  (page/html5
   (into
    [:head
     [:meta {:charset "UTF-8"}]
     [:title "clj-star-bridge"]
     [:script {:type "module"
               :src datastar-script-url}]]
    (react-assets))
   [:body
    [:h1 "SSE Notifications"]
    (counter-panel @counter)
    (activity-status "Ready")
    (signal-demo)
    (live-status-stream)
    (react-demo)
    [:div#notifications]
    [:script "const es = new EventSource('/events');\n    es.onopen = () => {\n      console.log('✅ Connected');\n      document.getElementById('notifications').innerHTML = '<p style=\"color:green\">✅ Connected</p>';\n    };\n    es.onmessage = (e) => {\n      const event = JSON.parse(e.data);\n      if (event.count !== undefined) {\n        document.getElementById('count').textContent = event.count;\n      }\n      if (event.message) {\n        document.getElementById('notifications').innerHTML += '<p style=\"color:blue\">' + event.message + '</p>';\n      }\n    };\n    es.onerror = (e) => {\n      console.error('❌ Error:', e.readyState);\n    };"]]))


;; Webhook ハンドラー
(defn notify-webhook [request]
  (let [body (slurp (:body request))
        data (json/parse-string body true)
        message (:message data "No message")]
    (println (str "Webhook received: " message))
    (broadcast! {:type "notification"
                 :message message})
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (json/generate-string {:status "ok"})}))

;; ルーティング
(defn base-handler [{:keys [uri request-method] :as request}]
  (cond
    (= uri "/") 
    {:status 200
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :body (layout)}
    
    (= uri "/increment")
    (let [new (swap! counter inc)
          message (str "Count updated to " new)]
      (broadcast! {:type "count"
                   :count new
                   :message message})
      (increment-response request new message))
    
    (= uri "/events")
    :sse-stream
    
    (and (= uri "/api/notify") (= request-method :post))
    (notify-webhook request)

    (and (= uri "/greet") (= request-method :post))
    (let [signals (request-signals request)
          name (normalize-name (:name signals))]
      (greeting-response request name))

    (and (= uri "/live-status") (= request-method :get))
    (live-status-response request)
    
    :else
    {:status 404 :body "Not Found"}))

;; SSE ハンドラーラッパー
(defn handler [request]
  (let [response (base-handler request)]
    (if (= response :sse-stream)
      (let [ch (s/stream)]
        (println "SSE client connected")
        (swap! sse-clients conj ch)
        (send-sse! ch {:type "connected"
                       :message "SSE connected"
                       :count @counter})
        (s/on-closed ch (fn []
          (println "SSE client disconnected")
          (remove-client! ch)))
        {:status 200
         :headers (assoc sse-headers "Connection" "keep-alive")
         :body ch})
      response)))

;; サーバー起動
(defn -main []
  (println "🚀 Starting on http://localhost:8080")
  (http/start-server #'handler {:port 8080}))
