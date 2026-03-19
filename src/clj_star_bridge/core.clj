(ns clj-star-bridge.core
  (:require [org.httpkit.server :as server]
            [starfederation.datastar.clojure.api :as d*]
            [starfederation.datastar.clojure.adapter.http-kit :as hk-gen]
            [hiccup2.core :as h]))

;; --- UIの定義 (Reactコンポーネントに相当するもの) ---

(defn counter-component [count]
  (str "<div id=\"counter-output\">"
       "<p>現在のカウント: " count "</p>"
       "<button data-on:click=\"@get('/increment')\">+1 する</button>"
       "</div>"))

(defn layout []
  (str "<!DOCTYPE html>"
       "<html>"
       "<head>"
       "<title>clj-star-bridge</title>"
       "<script type=\"module\" src=\"https://cdn.jsdelivr.net/gh/starfederation/datastar@v1.0.0-RC.8/bundles/datastar.js\"></script>"
       "</head>"
       "<body>"
       "<h1>React SPA からの移行テスト</h1>"
       (counter-component 0)
       "</body>"
       "</html>"))

;; --- 状態管理とルーティング ---

(defonce state (atom 0)) ; ReactのuseStateの代わり（サーバーサイドAtom）

(defn app [{:keys [uri] :as request}]
  (println (str "Request: " uri))
  (case uri
    "/" {:status 200 :headers {"Content-Type" "text/html; charset=utf-8"} :body (layout)}
    
    "/increment"
    (do
      (println "Increment requested")
      (hk-gen/->sse-response request
        {hk-gen/on-open
         (fn [sse-gen]
           (let [new-val (swap! state inc)]
             (println (str "Counter updated to: " new-val))
             (d*/patch-elements! sse-gen (counter-component new-val))
             (d*/close-sse! sse-gen)))}))

    {:status 404 :body "Not Found"}))

(defn -main []
  (println "Server starting on http://localhost:8080")
  (server/run-server #'app {:port 8080}))
