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

- [ ] フォーム送信・バリデーション
- [ ] リスト表示と動的挿入
- [ ] 複数要素の同時更新
- [ ] データベース統合
- [ ] React コンポーネントの段階的移行ガイド
- [ ] パフォーマンスベンチマーク

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
