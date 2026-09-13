# Datastar SDK 評価メモ

## 現時点の結論

Datastar プロトコル用の SSE 生成には、Clojure/Aleph SDK を使う価値がある。

一方で、既存の汎用 JSON SSE ストリームである `/events` は、当面は手書きの Aleph/Manifold 実装を維持する。

つまり、現時点の方針は次の通り。

```text
Datastar interaction:
  dev.data-star.clojure/aleph + SDK

Plain notification stream:
  existing Aleph/Manifold helper
```

## 採用する価値がある理由

- `dev.data-star.clojure/aleph` は現在の Aleph サーバー構成でロードできる。
- `starfederation.datastar.clojure.adapter.aleph/->sse-response` で Datastar 用 SSE レスポンスを作れる。
- `starfederation.datastar.clojure.api/patch-elements!` により、手書きしていた `datastar-patch-elements` フレーム生成を置き換えられる。
- `+1` ボタンから `data-on:click="@get('/increment')"` を実行し、SDK 経由の `patch-elements!` で `#count` が更新されることをブラウザで確認済み。
- 既存の `/events` による通知ストリームも壊れていない。

## 注意点

- 依存は `1.0.0-RC10` であり、まだ RC 版。
- 単発の Datastar レスポンスでも、`->sse-response` と `on-open` callback を使うため、手書き helper より記述は少し重く見える。
- Aleph adapter の `on-open` は同期実行されるため、重い処理やブロッキング処理は置かない。
- Datastar SDK を使う対象は、まず Datastar プロトコルのイベント生成に限定する。

## まだ評価していないこと

- `patch-signals!`
- `remove-element!`
- `execute-script!`
- 複数の Datastar event を 1 つのレスポンスで流す構成
- 長時間接続する Datastar SSE stream を SDK adapter に任せる設計
- `1.0.0-RC10` とより新しい RC 版の差分
- SDK 追加による依存関係やバージョン影響

## 現時点の判断

SDK は「すべての SSE を置き換える道具」としてではなく、「Datastar プロトコル部分を任せる道具」として採用するのがよい。

今後 Datastar の利用範囲が増える場合は、手書きで SSE frame を組み立てるよりも、SDK の API 名に寄せた実装の方が意図を読み取りやすく、プロトコル変更にも追従しやすい。
