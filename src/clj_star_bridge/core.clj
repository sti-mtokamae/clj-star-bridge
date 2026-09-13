(ns clj-star-bridge.core
  (:require [aleph.http :as http]
            [clojure.string :as str]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [cheshire.core :as json]
            [hiccup.page :as page]))

;; グローバル状態
(defonce counter (atom 0))
(defonce sse-clients (atom #{}))

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
   [:head
    [:meta {:charset "UTF-8"}]
    [:title "clj-star-bridge"]]
   [:body
    [:h1 "SSE Notifications"]
    [:p "Count: " [:span#count "0"]]
    [:button {:onclick "fetch('/increment').then(r => r.text()).then(c => { document.getElementById('count').textContent = c; })"} "+1"]
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
    {:status 200 :headers {"Content-Type" "text/html; charset=utf-8"} :body (layout)}
    
    (= uri "/increment")
    (let [new (swap! counter inc)]
      (broadcast! {:type "count"
                   :count new
                   :message (str "Count updated to " new)})
      {:status 200 :headers {"Content-Type" "text/plain"} :body (str new)})
    
    (= uri "/events")
    :sse-stream
    
    (and (= uri "/api/notify") (= request-method :post))
    (notify-webhook request)
    
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
         :headers {"Content-Type" "text/event-stream"
                   "Cache-Control" "no-cache"
                   "X-Accel-Buffering" "no"
                   "Connection" "keep-alive"}
         :body ch})
      response)))

;; サーバー起動
(defn -main []
  (println "🚀 Starting on http://localhost:8080")
  (http/start-server #'handler {:port 8080}))
