(ns clj-star-bridge.core
  (:require [aleph.http :as http]
            [manifold.stream :as s]
            [cheshire.core :as json]))

;; グローバル状態
(defonce counter (atom 0))
(defonce sse-clients (atom []))

;; HTML ページ
(defn layout []
  "<!DOCTYPE html>
<html>
<head>
  <meta charset=\"UTF-8\" />
  <title>clj-star-bridge</title>
</head>
<body>
  <h1>SSE Notifications</h1>
  <p>Count: <span id=\"count\">0</span></p>
  <button onclick=\"fetch('/increment').then(r => r.text()).then(c => { document.getElementById('count').textContent = c; })\">+1</button>
  
  <div id=\"notifications\"></div>
  
  <script>
    const es = new EventSource('/events');
    es.onopen = () => {
      console.log('✅ Connected');
      document.getElementById('notifications').innerHTML = '<p style=\"color:green\">✅ Connected</p>';
    };
    es.onmessage = (e) => {
      const msg = JSON.parse(e.data).message;
      document.getElementById('notifications').innerHTML += '<p style=\"color:blue\">' + msg + '</p>';
    };
    es.onerror = (e) => {
      console.error('❌ Error:', e.readyState);
    };
  </script>
</body>
</html>")

;; Webhook ハンドラー
(defn notify-webhook [request]
  (let [body (slurp (:body request))
        data (json/parse-string body true)
        message (:message data "No message")]
    (println (str "Webhook received: " message))
    (doseq [ch @sse-clients]
      (try
        (s/put! ch (str "data: " (json/generate-string {:message message}) "\n\n"))
        (catch Exception e
          (println (str "Error sending: " e))
          (swap! sse-clients (fn [clients] (vec (remove #(= % ch) clients)))))))
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
        (s/on-closed ch (fn []
          (println "SSE client disconnected")
          (swap! sse-clients (fn [c] (vec (remove #{ch} c))))))
        {:status 200
         :headers {"Content-Type" "text/event-stream"
                   "Cache-Control" "no-cache"
                   "Connection" "keep-alive"}
         :body ch})
      response)))

;; サーバー起動
(defn -main []
  (println "🚀 Starting on http://localhost:8080")
  (http/start-server #'handler {:port 8080}))
