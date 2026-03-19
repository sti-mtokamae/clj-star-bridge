# clj-star-bridge

React SPA から **Datastar + Clojure** へのアーキテクチャ移行を段階的に実現するための実験プロジェクト。

既存の重い JavaScript フレームワークから脱却し、サーバー主導のシンプルで軽量なリアルタイム Web アプリケーションへの「架け橋」となることを目指しています。

## 📌 プロジェクトの目的

| 項目 | React SPA | Datastar + Clojure |
|------|-----------|------------------|
| フロントエンド | 重い（React, Vue など） | 軽量（11KB） |
| 状態管理 | クライアント側（useState） | サーバー側（Atom） |
| ビルド | 必須（npm, webpack） | 不要 |
| 更新方式 | 仮想 DOM の差分検出 | SSE による部分更新 |
| 開発体験 | npm ビルド待機 | REPL 駆動開発 |

## 🚀 クイックスタート

### 前提
- **Clojure** がインストール済み（`clj` コマンド）
- **Java** 11 以上

### セットアップ

```bash
# リポジトリをクローン
git clone https://github.com/your-org/clj-star-bridge.git
cd clj-star-bridge

# サーバー起動
clj -M -m clj-star-bridge.core
```

ブラウザで [http://localhost:8080](http://localhost:8080) を開くと、シンプルなカウンター UI が表示されます。

## 📚 実装例：カウンター

### サーバーサイド（Clojure）

```clojure
(defonce state (atom 0))

(defn counter-component [count]
  (h/html
    [:div#counter-output
     [:p "現在のカウント: " count]
     [:button {:data-on-click "@get('/increment')"} "+1 する"]]))

(defn app [{:keys [uri]}]
  (case uri
    "/" {:status 200 :body (layout)}
    "/increment"
    (d*/->sse-response
      (fn [sse-gen]
        (let [new-val (swap! state inc)]
          (d*/patch-elements sse-gen (str (counter-component new-val))))))))
```

### クライアント側（HTML）

```html
<script type="module" src="https://cdn.jsdelivr.net/gh/starfederation/datastar@v1.0.0-RC.8/bundles/datastar.js"></script>
<button data-on:click="@get('/increment')">+1 する</button>
<div id="counter-output">現在のカウント: 0</div>
```

ボタンをクリック → `/increment` へ GET リクエスト → サーバーが state を更新 → SSE で `#counter-output` を再レンダリング

## 🏗️ アーキテクチャ

### Datastar の特徴

- **軽量設計**：コア 11KB、ビルド不要
- **SSE 第一級**：リアルタイムなサーバー→クライアント更新
- **HTML 属性駆動**：`data-on-click`, `data-text` など
- **多言語対応**：Clojure, Go, Python, Rust, PHP, .NET など

### リソース

- 📖 [Datastar 公式ドキュメント](https://data-star.dev)
- 💻 [Datastar Clojure SDK](https://github.com/starfederation/datastar-clojure)
- 🎯 [Hyperlith](https://github.com/andersmurphy/hyperlith) - Datastar ベースのフルスタックフレームワーク

## 📋 今後の実装予定

### Phase 1: サーバー側からの通知機能 ✅ **実装中**

React SPA がログイン後、Datastar でサーバーからのリアルタイム通知をキャッチして表示する機能を実装

**実装内容：**
- [x] Clojure で複数 SSE クライアント接続の管理
- [x] Spring Boot ← → Clojure webhook 通知機構
- [x] ブラウザ側 `#notifications-area` へのリアルタイム配信
- [x] JWT / CSRF トークン検証統合（基本実装）
- [x] Cookie からの JWT 抽出ロジック

**アーキテクチャ：**

```
ブラウザ（React SPA）
  ├─ ログイン済み（JWT Cookie）
  └─ <div id="notifications-area"> ← SSE で自動更新
       ↓ SSE 接続
       
Clojure App Service
  ├─ /events/notifications → SSE stream（クライアント接続受け取り）
  ├─ /api/notify → webhook（Spring Boot から通知）
  ├─ sse-clients atom → 複数クライアント追跡
  └─ broadcast-notification → 全クライアントに配信
       ↓ API 呼び出し
       
Spring Boot
  └─ POST /api/notify ← Clojure に webhook 送信
```

**期待される効果：**
- サーバーイベント（例：レポート完成、データ更新）が全ユーザーに同時配信
- ポーリングが不要 → 無駄なリクエスト削減
- ユーザー体験向上（リアルタイム性）

---

### Phase 1 使用方法

#### 開発環境での動作確認：

```bash
# サーバー起動
clj -M -m clj-star-bridge.core
```

ブラウザで [http://localhost:8080](http://localhost:8080) を開くと：
- カウンター UI（既存）
- リアルタイム通知エリア（新規）
- テスト用ボタン で手動トリガー可能

#### Spring Boot からの通知送信：

```bash
# Clojure に webhook 送信（通知配信）
curl -X POST http://localhost:8080/api/notify \
  -H "Content-Type: application/json" \
  -H "Cookie: Authorization=<JWT_TOKEN>" \
  -d '{"message":"新しいレポートが利用可能です"}'
```

すると、全ブラウザの `#notifications-area` に通知が同時出現。

---

### Phase 2-4: 段階的機能移行

- [ ] フォーム送信・バリデーション
- [ ] リスト表示と動的挿入
- [ ] 複数要素の同時更新
- [ ] 複雑な状態管理（Zustand → Clojure Atom）
- [ ] データベース統合
- [ ] React コンポーネントの段階的移行ガイド
- [ ] パフォーマンスベンチマーク

**最終形：** Spring Boot は認証・権限のみ、ほぼ全 UI が Clojure 化

## 🔗 関連技術

- **Clojure / ClojureJVM**
- **http-kit**：軽量 HTTP サーバー（Brotli 圧縮対応）
- **Hiccup**：Clojure での HTML テンプレート
- **Datastar**：軽量ハイパーメディアフレームワーク
- **SSE (Server-Sent Events)**：リアルタイム通信

## 📄 ライセンス

MIT

---

**質問・提案・プルリクエスト** 歓迎です！
