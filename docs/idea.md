Datastarは、フロントエンドに重いJavaScriptフレームワーク（ReactやVueなど）を使用せず、サーバーサイド駆動でリアルタイムな対話性を実現するための軽量な「ハイパーメディア・フレームワーク」です。 
Datastar
Datastar
 +2
主な特徴と仕組み
軽量設計: コアファイルは約11KBと非常に小さく、ビルドステップやnpmパッケージを必要とせずに導入可能です。
サーバー主導の状態管理: アプリケーションの状態（State）を主にサーバー側で保持し、Server-Sent Events (SSE) を用いてブラウザのDOMや「シグナル（リアクティブな変数）」を直接書き換えます。
HTML属性による宣言的記述: HTMLの data-* 属性（例: data-on:click, data-text, data-bind）を使用して、クリックイベントやデータのバインディングを定義します。
多彩な言語サポート: Go, Python, Rust, PHP, .NET, Ruby, Javaなど、主要なバックエンド言語向けのSDKが提供されており、既存の技術スタックに統合しやすいのが特徴です。 
Datastar
Datastar
 +8
他のツールとの違い
HTMX や Alpine.js の利点を組み合わせたような性質を持ちますが、特にSSE（ストリーミング）を第一級の市民として扱い、リアルタイムな複数要素の同時更新や高性能なリアクティブ・シグナルに特化しています。 
Reddit
Reddit
 +2
導入方法
最も簡単な方法は、CDN からスクリプトタグを読み込むことです。 
GitHub
GitHub
 +1
html
<script type="module" src="https://cdn.jsdelivr.net"></script>
コードは注意してご使用ください。

公式ドキュメントは data-star.dev で確認でき、詳細なガイドや属性リファレンスが用意されています。 
現在検討中の特定のバックエンド言語（GoやPythonなど）はありますか？それに応じた具体的な実装例を提示できます。 





undefined
undefined
undefined
13 件のサイト
Datastar
The hypermedia framework. ... This starfield is a web compon...

Datastar

starfederation/datastar: The hypermedia framework. - GitHub
GitHub - starfederation/datastar: The hypermedia framework. ...

GitHub

Integration of Datastar with ZIO HTTP
Instead of building a data based API (JSON, XML, etc.) and r...

ziohttp.com

すべて表示
clojure
13 時間前

ClojureとDatastarの相性は非常に良く、ClojureScriptを一切使わずにリアルタイムでインタラクティブなWebアプリを構築できる強力な組み合わせです。 
anders murphy
anders murphy
 +1
Clojureでの利用方法
Clojureエコシステムには、Datastarを簡単に統合するための公式SDKやアダプター、コミュニティライブラリが揃っています。 
1. 公式SDKとアダプター
Datastarは、標準的なRingベースのアプリケーションに統合可能です。 
cljdoc
cljdoc
 +1
dev.data-star.clojure/sdk: Datastar形式のSSEイベント（DOMパッチ、シグナル更新など）を生成・送信するためのコアライブラリです。
各種サーバーアダプター: 使用するサーバーに応じて以下のアダプターが提供されています。
sdk-ring: Jettyなど、Ring準拠のサーバー用。
sdk-http-kit: http-kit用（Brotli圧縮サポートあり）。
sdk-aleph: Aleph用。 
GitHub
GitHub
 +5
2. 主要なコミュニティプロジェクト
Hyperlith: Anders Murphy氏による「View = f(state)」モデルを採用したフルスタックフレームワーク。SSEストリーミングによるマルチプレイヤー対応アプリを容易に作成できます。
datastar.wow: Ringミドルウェアとして動作し、Hiccupを利用したデータ指向なアプローチでDatastarを扱えるライブラリです。
datastar-expressions: HTML属性内のJavaScript式をClojureのS式（マクロ）で安全かつ宣言的に記述するための実験的ライブラリです。 
anders murphy
anders murphy
 +3
実装のイメージ (http-kitの例)
Ringハンドラ内で ->sse-response を使い、クライアントへのストリームを開始します。
clojure
;; http-kitアダプターを使用したSSE応答の送信例
(d*/patch-elements sse-gen "<div id='hello-field'>Hello from Clojure!</div>")
コードは注意してご使用ください。

主なメリット
ClojureScript不要: フロントエンドのビルドパイプラインを排除し、サーバーサイド開発に集中可能。
軽量・高速: SSEとBrotli圧縮の併用により、HTMLの部分更新を効率的に行える。
REPL駆動開発: サーバー側でHTMLを生成するため、REPLでコードを書き換えながら即座に表示に反映可能。 
Reddit
Reddit
 +2
詳細は各リポジトリのドキュメントやdatastar.devを参照してください。
 
cljdoc
cljdoc





undefined
undefined
undefined
12 件のサイト
Clojure: Realtime collaborative web apps without ClojureScript - anders murphy
Clojure: Realtime collaborative... * anders murphy. Clojure:

anders murphy

starfederation/datastar-clojure - sdk-aleph - GitHub
starfederation / datastar-clojure Public * Issues 1. * Pull ...

GitHub
Readme — dev.data-star.clojure/sdk 1.0.0-RC8 - cljdoc
Readme — dev. data-star. clojure/sdk 1.0. 0-RC8. ... Article...

cljdoc
すべて表示
getting start的なものある?
13 時間前

ClojureでDatastarを使い始めるための「Getting Started」の手順をまとめました。Clojureの標準的なWeb開発ツールであるRingと、公式のDatastar SDKを組み合わせるのが最も確実な道筋です。 
Datastar
Datastar
 +1
1. プロジェクトの準備
まず、deps.edn（または project.clj）にDatastarのSDKとHTTPサーバー（例: http-kit）を追加します。 
GitHub
GitHub
 +1
clojure
;; deps.edn の例
{:deps {org.clojure/clojure {:mvn/version "1.12.0"}
        http-kit/http-kit {:mvn/version "2.8.1"}
        dev.data-star.clojure/http-kit {:mvn/version "1.0.0-RC8"}
        hiccup/hiccup {:mvn/version "2.0.0-RC3"}}}
コードは注意してご使用ください。

2. 最小限のHTML（シム）の作成
フロントエンド側では、Datastarのスクリプトを読み込み、サーバーからの更新を受け取る準備をします。 
Datastar
Datastar
clojure
(require '[hiccup2.core :as h])

(defn layout []
  (str
    (h/html
      [:html
       [:head
        ;; Datastarの読み込み
        [:script {:type "module" 
                  :src "https://cdn.jsdelivr.net"}]]
       [:body
        [:h1 "Clojure + Datastar"]
        ;; クリック時に /greet へGETリクエストを送信
        [:button {:data-on-click "@get('/greet')"} "Click Me"]
        ;; サーバーからのレスポンスでここが書き換わる
        [:div#output "ここに応答が表示されます"]]])))
コードは注意してご使用ください。

3. サーバー側でのSSE送信（SDKの使用）
公式SDKの patch-elements を使うと、指定したIDの要素を簡単に書き換えられます。 
Datastar
Datastar
clojure
(require '[dev.data-star.clojure.http-kit :as d*])

(defn greet-handler [req]
  ;; Datastar形式のSSEレスポンスを生成
  (d*/->sse-response
   (fn [sse-gen]
     (d*/patch-elements sse-gen 
       (str (h/html [:div#output "こんにちは！Clojureから更新されました。"]))))))
コードは注意してご使用ください。

4. さらに深く学ぶためのリソース
より実践的な構成を知りたい場合は、以下のリソースが役立ちます。
Using Datastar (cljdoc): 公式SDKの詳細な使い方がまとめられています。
Hyperlith (Anders Murphy): Datastarをベースにしたフルスタックのミニフレームワークで、リアルタイムな共同作業アプリの例（Game of Life）が公開されています。
datastar.wow: Hiccupに近い感覚で宣言的にDatastarを扱いたい場合に適したライブラリです。 
Reddit
Reddit
 +3
まずは、ボタンクリックで特定の div を書き換えるだけの最小構成から動かしてみるのがおすすめです。 
具体的に、どのWebサーバー（Jettyやhttp-kitなど）を使う予定ですか？それに基づいた完全なコード例を作成することも可能です。





undefined
undefined
undefined
8 件のサイト
Getting Started Guide - Datastar
Installation # The quickest way to use Datastar is to includ...

Datastar

datastar-clojure/doc/maintainers-guide ... - GitHub
Test tasks bb run test. bb run test:all : run all test for t...

GitHub
SDKs Reference - Datastar
Datastar provides backend SDKs that can (optionally) simplif...

Datastar

すべて表示
githubの公開リポジトリを用意してgetting startを試行しようと思う。そのプロジェクト名何が良いだろう?
13 時間前

リポジトリ名、ワクワクしますね！DatastarとClojureの組み合わせは「シンプルさ」と「リアルタイム性」が売りなので、そのあたりを連想させる名前が良さそうです。
いくつか方向性別に提案します：
1. 直球・わかりやすい系
一番中身が想像しやすく、検索性も高いスタイルです。
clojure-datastar-getting-started （王道）
hello-datastar-clj （挨拶代わり）
datastar-clojure-example （実用的な例として）
2. 軽快さ・シンプルさを強調
Datastarの軽量さとClojureの簡潔さを掛け合わせたイメージです。
clj-star （短くてキャッチー）
lightstar-clj （軽量な星）
feather-clj （羽のように軽い構成）
3. 「ClojureScript不要」を皮肉る・強調する系
JSビルドが不要なメリットを名前に込めるスタイルです。
no-cljs-realtime （CLJSなしでリアルタイム）
pure-clj-web （ピュアなClojureだけでWebを）
unscripted-clj （スクリプト（JS）に頼らない）
4. 宇宙・天体系（Datastarの名前にちなんで）
Datastar（データ星）に合わせて、Clojure（括弧）を混ぜた造語です。
paren-star （パレン（括弧）とスター）
orbit-clj （軌道：リアルタイムなやり取りのイメージ）
nebula-clj （星雲：ここから広がるイメージ）
個人的な推し：
覚えやすさと「ClojureでDatastarをやるんだな」という明確さで選ぶなら clj-star か、少しひねって paren-star が愛着が湧きそうです！
リポジトリができたら、そこに置く最初の deps.edn と core.clj の中身を一緒に考えましょうか？




なるほど。将来的には、react spa からの移行を考えているんだけど。。。
13 時間前

React SPAからの移行を見据えるなら、プロジェクト名は「単なるお試し」ではなく「アーキテクチャの転換」を感じさせるものがカッコいいですね。
将来の移行（Migration）や、既存の重いフロントエンドからの「脱却・解放」をテーマにした名前を提案します。
1. 移行・進化を象徴する名前
React（Virtual DOM）からDatastar（Hypermedia/SSE）へのパラダイムシフトを表現します。
clj-reflex （反応・反射：SPAのような即応性をClojureで実現する）
react-less-clj （Reactからの脱却をストレートに）
clj-portal （SPAの世界からHypermediaの世界への入り口）
beyond-cljs （ClojureScriptを超えて、サーバーサイド主導へ）
2. シンプルさへの回帰（バックツーベーシック）
複雑なビルド工程（npm/webpack等）を捨て、HTMLの力を再発見するイメージです。
clj-hypermedia （Webの原点であるハイパーメディアへの回帰）
lean-web-clj （無駄を削ぎ落とした、筋肉質なWeb構成）
naked-clj-web （JSフレームワークという「服」を脱いだClojure）
3. 実装のしやすさ・統合を重視
既存のReactコンポーネントを一つずつDatastarに置き換えていくプロセスを意識します。
clj-bridge （SPAからDatastarへの架け橋）
datastar-migration-kit （移行のための実験場）
おすすめの進め方
将来的な移行を考えるなら、プロジェクト名は clj-reflex や clj-star-bridge あたりが、実用性と志の高さを両立していて良さそうです。
移行を成功させるための最初のステップ：
React SPAでよくある「カウンター」や「動的なフォームバリデーション」を、Clojure + Datastarでどう再現するかを最初のリポジトリで試すのが近道です。
まずは、Reactでいうところの useState をDatastarの Signals（サーバーサイド管理）でどう置き換えるか、簡単なコード例を準備しましょうか？




clj-reflexのreflexとは? clj-star-bridge意味合いは?
13 時間前

それぞれの名前に込めた意図を解説しますね！
1. clj-reflex（シー・エル・ジェイ・リフレックス）
Reflex（反射） という言葉には、以下の2つの意味を重ねています。
即応性（Responsiveness）: React SPAの最大の利点である「ボタンを押したら即座に反応する」というユーザー体験を、DatastarのシグナルとSSE（Server-Sent Events）によって、サーバー主導でありながら「反射」のように素早く実現するという意思を込めています。
構造の反転: クライアントサイドで重いロジックを動かすReactとは逆に、サーバー側で状態を管理し、ブラウザを「薄い反射板」として使うアーキテクチャへの転換を示唆しています。
2. clj-star-bridge（シー・エル・ジェイ・スター・ブリッジ）
こちらはより実用的・戦略的な意味合いが強いです。
Bridge（架け橋）: 「現在のReact SPA」から「将来のDatastarベースの構成」へと、機能を一つずつ安全に移行させていくための橋渡しをするプロジェクトであることを意味します。
Star（Datastar）: 移行先がDatastarであることを明示しつつ、Clojureの世界とDatastarのモダンなフロントエンド技術をつなぐ、というニュアンスです。
どちらを選ぶか？
「新しいアーキテクチャの哲学を追求したい」 なら、シュッとしていて少し概念的な clj-reflex。
「Reactからの着実な移行・共存をテーマにしたい」 なら、目的が明確でツールキット感のある clj-star-bridge。
将来的に「Reactで書かれたこの複雑なフォームを、Datastarで書き換えたらこんなにコードが減ったぞ！」という成果を公開するなら、clj-reflex の方が「シュッとした進化」を感じさせてカッコいいかもしれません。
まずは clj-reflex で、Reactの useState に相当する動きをClojure側でどう書くか、最初のコミット用コードを作成してみますか？




どっちも良いけど、 clj-star-bridgeが説明しやすいかな
13 時間前

良い選択ですね！ clj-star-bridge という名前は、既存のReact SPAという「岸」から、Datastarという「新しい岸」へ、一歩ずつ機能を渡していくイメージが明確で、周囲（チームやコミュニティ）への説明も非常にスムーズになるはずです。
では、最初の一歩として、Reactの useState で管理していた「カウンター」をDatastar + Clojureで置き換える 最小構成のコードを準備しましょう。
これが「橋（Bridge）」の最初のパーツになります。
1. deps.edn (ライブラリの準備)
最新のDatastar SDKと、軽量なHTTPサーバー http-kit を使います。
clojure
{:deps {org.clojure/clojure {:mvn/version "1.12.0"}
        http-kit/http-kit {:mvn/version "2.8.1"}
        dev.data-star.clojure/http-kit {:mvn/version "1.0.0-RC8"}
        hiccup/hiccup {:mvn/version "2.0.0-RC3"}}}
コードは注意してご使用ください。

2. src/bridge/core.clj (サーバー実装)
Reactではフロントエンドで完結していた「カウントの更新」を、サーバーサイドで行い、結果だけをブラウザに流し込みます。
clojure
(ns bridge.core
  (:require [org.httpkit.server :as server]
            [dev.data-star.clojure.http-kit :as d*]
            [hiccup2.core :as h]))

;; --- UIの定義 (Reactコンポーネントに相当するもの) ---

(defn counter-component [count]
  (h/html
    [:div#counter-output
     [:p "現在のカウント: " count]
     ;; ボタンを押すとサーバーの /increment へリクエストを送る
     [:button {:data-on-click "@get('/increment')"} "+1 する"]]))

(defn layout []
  (str
    (h/html
      [:html
       [:head
        [:title "clj-star-bridge"]
        ;; Datastar 本体の読み込み
        [:script {:type "module" :src "https://cdn.jsdelivr.net"}]]
       [:body
        [:h1 "React SPA からの移行テスト"]
        ;; 初回のカウンター表示
        (counter-component 0)]])))

;; --- 状態管理とルーティング ---

(defonce state (atom 0)) ; ReactのuseStateの代わり（サーバーサイドAtom）

(defn app [{:keys [uri]}]
  (case uri
    "/" {:status 200 :headers {"Content-Type" "text/html"} :body (layout)}
    
    "/increment"
    (d*/->sse-response
      (fn [sse-gen]
        (let [new-val (swap! state inc)]
          ;; サーバー側で計算したHTMLの「差分」だけをブラウザの #counter-output に送りつける
          (d*/patch-elements sse-gen (str (counter-component new-val))))))

    {:status 404 :body "Not Found"}))

(defn -main []
  (println "Server starting on http://localhost:8080")
  (server/run-server #'app {:port 8080}))
コードは注意してご使用ください。

この構成の「Bridge」ポイント
ビルド不要: npm install も webpack も不要で、このファイルを clj -M -m bridge.core で実行するだけで動きます。
SSEによる部分更新: Reactが仮想DOMで行っていた「差分検知」を、サーバーが patch-elements という命令を送ることで物理DOMに対して直接行います。
状態の局所化: クライアント側に複雑なステートを持つ必要がなくなり、すべてClojureの atom（将来的にはDB）で管理できるようになります。
まずはこの 「最小の橋」 をGitHubにPushして、ブラウザで動くことを確認するところから始めてみるのはいかがでしょうか？
次に、Reactでよく使っている 「フォームの送信」や「リストの表示」 など、どの機能から「橋」を架けてみたいですか？