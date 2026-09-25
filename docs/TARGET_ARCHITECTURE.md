# Target Architecture

## 目的

現在の React SPA では、React Router と feature ごとの Zustand store 群がブラウザ内に仮想的なアプリケーション空間を作り、画面、データ区画、ページ内外の state を管理している。

このプロジェクトでは、その責任をサーバー側の URL、ページ、サービス境界へ移す。React 自体は廃止せず、複雑なクライアント UI を実装するページ内コンポーネントとして残す。

```text
Current:
  React Router
    -> monolithic React page tree
    -> feature-scoped Zustand stores
    -> single-deployment Spring Boot REST backend

Target:
  Server URL routing
    -> Hiccup page composition
    -> Datastar regions + React components
    -> backend services
```

## 基本原則

1. ページは HTTP の URL とレスポンスを境界とする。
2. ページ全体の構成は Clojure/Hiccup が所有する。
3. ページ遷移には通常の link と full-page navigation を使う。
4. React と Datastar は、割り当てられた別々の DOM subtree を所有する。
5. React はページ内で高度な対話性が必要な UI に限定する。
6. Datastar はサーバー主導で十分な UI と部分更新に使う。
7. 業務データと業務ルールは backend service が所有する。
8. 分散化は目的にせず、業務境界と独立運用の価値がある単位に限定する。

これらは目標状態の原則である。移行初期には、既存 React SPA を 1 つの React
root として Hiccup shell に収容し、React Router と既存 global store を一時的に
維持してよい。最初から SPA を分解することは前提にせず、shell 側で認証、通知、
asset 配信などの境界を確認してから、ページ単位で責任を移す。

## 責任分担

### Clojure/Hiccup shell

- URL routing とページ選択
- 認証済み request context の解決
- ページ全体の HTML 構成
- Datastar 領域と React mount point の配置
- ページに必要な service 呼び出しの調整
- React component に渡す初期 props の生成

shell はページ composition を担当するが、業務ルールや永続データを抱え込まない。

### Datastar

- サーバー主導で十分なフォームや表示
- HTML fragment と signal の部分更新
- SSE による通知や継続更新

### React component

- grid、chart、editor など、複雑なクライアント操作
- 割り当てられた mount point 内の DOM
- ページ内だけで完結する一時的な UI state

React component はページ routing やアプリケーション全体の global state を所有しない。

### Backend service

- 業務ロジック
- 永続データ
- transaction と整合性
- 業務境界ごとの API

最初から細かな microservice へ分割しない。既存 Spring Boot は当面 service として利用し、変更と運用を独立させる価値が確認できた境界だけを後から分割する。

既存 backend は `common-app` / `common-lib` と業務別 package による内部構造をすでに持つ。移行ではこの構造を無視して作り直すのではなく、まず単一デプロイの Spring Boot service として接続し、実際の変更頻度や運用要件から独立させる境界を判断する。

## State の配置

| State | 配置先 |
|-------|--------|
| URL で表現できる選択や検索条件 | path / query parameter |
| 認証と利用者 context | session / token / server request context |
| 業務データ | backend service / database |
| ページ生成に必要な一時データ | Clojure request processing |
| Datastar の入力値や表示制御 | signals |
| React 内で完結する操作状態 | component local state |

ページをまたぐ巨大な client-side global store は作らない。次のページで必要な情報は URL、認証 context、または backend service から再構築する。

## React の組み込み方

### 移行の入口: 既存 SPA を丸ごと収容

最初の段階では、既存 React SPA 全体を 1 つの mount point に配置できる。

```text
Clojure/Hiccup shell
  Datastar-owned notification region
  React root
    existing React SPA
```

これは React の「既存ページの一部へ React root を追加する」標準的な統合方式と
同じである。違いは、Hiccup shell を将来のページ composition 境界として置き、
既存 SPA を段階移行の対象として内側に収容する点にある。

この段階では React SPA 内の router、store、API client をそのまま利用できる。
Datastar は React root の外側にある通知 banner、接続状態、処理進捗などを担当し、
React の DOM を直接変更しない。

```text
tradehub-web-backend
  -> event / webhook
  -> clj-star-bridge
  -> long-lived Datastar SSE
  -> notification region outside the React root
```

既存 SPA の収容は最終構成ではなく移行入口である。動作を保ったまま、必要性が
確認できたページから server URL routing と Hiccup composition へ責任を移す。

### ページ固有 component への分解

Hiccup が React component 用の mount point を出力する。

```clojure
[:div {:data-react-component "counter"
       :data-react-props "{\"initialCount\":0}"
       :data-ignore-morph true}]
```

React bundle はページ読み込み時に対象要素を探して一度だけ mount する。

```javascript
document.querySelectorAll("[data-react-component]").forEach(mountComponent)
```

ページ遷移は full-page navigation のため、ブラウザがページと React root をまとめて破棄する。最初の構成では、クライアント側 router や動的な mount/unmount orchestrator を導入しない。

## DOM の所有権

```text
Hiccup:
  page structure and React mount point

Datastar:
  Datastar-owned subtree only

React:
  content inside its mount point only
```

Datastar は React mount point の内側を patch しない。React mount point を含む広い範囲を morph する必要がある場合は、`data-ignore-morph`で保護する。

## Asset 配信

### 開発時

- Hiccup page は `clj-star-bridge` が配信する。
- 最初の PoC では `clj-react-hack` を Shadow-CLJS の watch mode で起動し、生成された ClojureScript/React の JS と CSS を development HTTP server から Hiccup page が読み込む。
- `clj-react-hack` 側には、Hiccup が生成した mount point を対象に component を mount する entry point を用意し、開発時の cross-origin asset 読み込みを確認する。
- Datastar は現在と同様に CDN bundle を利用する。

### 本番時

- React component を production build する。
- version を固定した JS/CSS asset を静的配信または CDN から配信する。
- Hiccup page は必要なページでだけ React asset を読み込む。

開発時と本番時で、ページ本体や React mount point の Hiccup は変更しない。JS/CSS の URL は環境設定から共通の asset helper へ渡し、開発時は Shadow-CLJS development HTTP server、本番時は静的配信先または CDN を参照する。production build で hash 付きファイル名を使う場合は、asset manifest を helper が解決する。

```clojure
(page
  (react-assets config)
  (react-mount "counter" {:initial-count 0}))
```

`react-assets` の出力だけが環境によって変わり、`react-mount` とページ composition は共通に保つ。

## 採用しないもの

現段階では次を導入しない。

- `single-spa`
- Module Federation
- client-side application router
- React component 間の global state 共有基盤
- component 単位の backend microservice
- 既存 Spring Boot の一括分割
- React の完全削除

実在する要件で必要性が確認できた場合にだけ再評価する。

## 最初の検証（確認済み）

`clj-react-hack` の demo application 全体を 1 つの React root として、
`clj-star-bridge` の Hiccup page に載せた。

検証項目:

1. Hiccup が生成した mount point に React component を表示できる。
2. 同じページ上の Datastar 領域が影響を受けない。
3. React component が自身の DOM subtree だけを更新する。
4. 通常のページ再読み込みで component が再構築される。
5. React asset が取得できない場合でも、ページ全体の構造が壊れない。

この検証では backend service の分割や実業務データとの接続は行っていない。
ページ composition と DOM 所有境界、別 origin の開発 asset 配信を確認した。

## 次の検証

1. `tradehub-web-frontend` を既存 SPA のまま React root に mount する。
2. 既存 routing、store、Spring Boot API 接続を壊さず動かす。
3. `tradehub-web-backend` から bridge へテスト通知を送る。
4. React root 外の Datastar 通知領域へ一斉通知を push する。
5. 認証済み利用者や組織を基準に通知対象を制御するための境界を整理する。
