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

## ✅ 実装進捗

### Phase 1: SSE Notification System ✅ COMPLETE

リアルタイム通知システムの基盤を構築。

**実装済み:**
- ✅ Browser EventSource 接続（`/events` endpoint）
- ✅ Webhook による通知受信（`POST /api/notify`）
- ✅ 複数クライアントへのブロードキャスト
- ✅ カウンター画面（`/increment` endpoint）
- ✅ Aleph HTTP サーバー（ネイティブ SSE サポート）
- ✅ エラーハンドリングと接続管理

**技術スタック (Phase 1):**
- **Aleph 0.4.7**: ネイティブ SSE 対応の Async HTTP サーバー
- **Manifold**: ストリーム管理ライブラリ
- **Hiccup 2.0.0**: Clojure のデータ構造から HTML を生成
- **Cheshire**: JSON パース・生成
- **Clojure 1.12.0**: コア言語

### Phase 2: Datastar Components (計画中)

**Datastar とは？**
軽量フロントエンドフレームワーク（11KB）。サーバーからの SSE ストリームを通じて、HTML フラグメントを受け取り、**部分的に DOM を更新** するアプローチ。
React の仮想 DOM 比較ではなく、サーバーが「どの部分を更新するか」を明示的に指定します。

**実装予定:**
- [ ] Datastar SDK 統合（`@data-star` スクリプト）
- [ ] Hiccup で生成した HTML フラグメントを SSE で送信
- [ ] OOB (Out of Band) スワップで特定要素のみ更新
- [ ] リアクティブな UI コンポーネント（セレクタベース更新）

### Phase 3: React Component Gradual Migration (計画中)

- [ ] 既存 React SPA の段階的置き換え
- [ ] 状態管理の Clojure 移行

### Phase 4: Full Clojure SSR (計画中) - 最終形イメージ

**焦点：アプリケーション層 - 状態管理とHTMLレンダリング**

```
┌────────────────────────────────────────┐
│      Browser (Datastar)                │
│  ┌──────────────────────────────────┐  │
│  │ <div data-star="...">            │  │
│  │   Counter: 1                     │  │
│  │   <button>+1</button>            │  │
│  │ </div>                           │  │
│  └──────────────────────────────────┘  │
└────────────┬─────────────────────────────┘
             │
      SSE Stream (HTML fragments)
             │
             ▼
┌────────────────────────────────────────┐
│   Clojure Server (Aleph + Hiccup)      │
│                                        │
│  Handler:                              │
│   GET / → page/html5 + Datastar JS    │
│   GET /events → SSE Stream             │
│   POST /increment → new counter        │
│          ↓ broadcast HTML fragment     │
│          (Counter div only)            │
│                                        │
│  State: (atom {:counter 0 :...})      │
│  No React, No JavaScript build         │
└────────────────────────────────────────┘
```

**最終目標:**
- ✅ 完全サーバーサイドレンダリング
- ✅ React 完全削除
- ✅ ビルドプロセス不要
- ✅ REPL 駆動開発でリアルタイム更新

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

ブラウザで [http://localhost:8080](http://localhost:8080) を開くと、SSE 通知システムが表示されます。

## 📊 アーキテクチャ

### Phase 1: Core SSE Infrastructure

**焦点：通信層 - 複数クライアントのSSE接続管理**

```
┌─────────────────────────────────────────────┐
│         Browser (localhost:8080)            │
├─────────────────────────────────────────────┤
│                                             │
│  <script>                                   │
│    const es = new EventSource('/events');   │
│    es.onmessage = (e) => {                  │
│      // Display notification                │
│    };                                       │
│  </script>                                  │
│                                             │
└────────────────┬────────────────────────────┘
                 │
        SSE Stream (text/event-stream)
                 │
                 ▼
┌─────────────────────────────────────────────┐
│     Aleph HTTP Server (port 8080)           │
├─────────────────────────────────────────────┤
│                                             │
│  GET /events → Stream<Channel>              │
│  POST /api/notify → Broadcast to all        │
│  GET /increment → Counter increment         │
│  GET / → HTML page                          │
│                                             │
└─────────────────────────────────────────────┘
```

### 通信フロー

**複数クライアント接続の管理方法**

```
Browser 1: EventSource('/events')
   ▲
   │ SSE Stream
   │
Browser N: EventSource('/events')
   ▲
   │ SSE Stream
   │
──────────────────────────────────────────
   ▼
[Aleph Server - sse-clients (atom with streams)]
   ▲
   │
   │ POST /api/notify
   │ {"message": "..."}
   │
Webhook Source (e.g., external system)
```

## 🧪 テスト

### Webhook 通知テスト

サーバー起動後、別ターミナルで：

```bash
curl -X POST http://localhost:8080/api/notify \
  -H "Content-Type: application/json" \
  -d '{"message":"✅ テスト通知"}'
```

ブラウザに **青色** の "✅ テスト通知" が表示されます。

### カウンター機能テスト

ブラウザで "+1" ボタンをクリックするとカウンターが増加します。

## 📚 参考資料

- 📖 [Datastar 公式ドキュメント](https://data-star.dev)
- 💻 [Aleph Documentation](https://aleph.io/)
- 🎯 [Manifold Streams](https://github.com/clj-commons/manifold)

## 📝 ライセンス

MIT
