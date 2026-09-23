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

## SSE 実装の役割分担

このプロジェクトでは、手書き SSE と Datastar SDK の SSE をどちらか一方に統一しない。

- `/events` は、ブラウザの `EventSource` に JSON を流す汎用通知ストリームとして維持する。
- `/events` の実装は、SSE frame、`data:` 行、空行終端、クライアント接続管理、切断時 cleanup が見える低レベルな参考実装でもある。
- Datastar action response は、`patch-elements!` や将来の `patch-signals!` など、Datastar プロトコル固有のイベント生成を SDK に任せる。
- Aleph は Datastar 専用の通信方式ではなく、手書き SSE と Datastar SDK の両方を流す HTTP/streaming 基盤として採用する。

`/events` と `/live-status` はどちらも接続を開いたままサーバーからブラウザへ送信し続けられるが、実装パターンは別物として扱う。`/events` は接続 stream を `sse-clients` に保持し、外部イベント発生時に `broadcast!` する。`/live-status` は Datastar SDK adapter の `on-open` で接続ごとの SSE generator を受け取り、virtual thread から定期的に `patch-elements!` を送る。

## 採用する価値がある理由

- `dev.data-star.clojure/aleph` は現在の Aleph サーバー構成でロードできる。
- `starfederation.datastar.clojure.adapter.aleph/->sse-response` で Datastar 用 SSE レスポンスを作れる。
- `starfederation.datastar.clojure.api/patch-elements!` により、手書きしていた `datastar-patch-elements` フレーム生成を置き換えられる。
- `+1` ボタンから `data-on:click="@get('/increment')"` を実行し、SDK 経由の `patch-elements!` で、Hiccup が生成した `#counter-panel` 全体を差し替えられることをブラウザで確認済み。
- 差し替え後の `+1` ボタンから再度リクエストでき、サーバー生成 UI fragment の連続更新も確認済み。
- 1 つの Datastar レスポンスで `patch-elements!` を複数回実行し、`#counter-panel` と `#activity-status` を同じレスポンス内で順に更新できる。
- `data-bind="name"` と `@post('/greet')` で送られた signal を `get-signals` で読み取り、サーバーで正規化した値を `patch-elements!` と `patch-signals!` でブラウザへ返せる。
- 前後に空白を含む名前を送信すると、挨拶 fragment と入力欄の signal が正規化後の値へ更新されることをブラウザで確認済み。
- `/live-status` では、SDK adapter の接続を開いたまま `patch-elements!` を繰り返し、サーバー時刻と更新回数を継続送信できる。
- ブラウザで `Live Status` のサーバー時刻と更新回数が 1 秒ごとに更新されることを確認済み。
- 長時間処理を virtual thread へ分離することで、同期実行される `on-open` callback をブロックせずにレスポンスを返せる。
- クライアント切断時に `on-close` callback が発火し、更新処理を中断できることを確認済み。
- 既存の `/events` による通知ストリームも壊れていない。

## 注意点

- 依存は `1.0.0-RC10` であり、まだ RC 版。
- 単発の Datastar レスポンスでも、`->sse-response` と `on-open` callback を使うため、手書き helper より記述は少し重く見える。
- Aleph adapter の `on-open` は同期実行されるため、重い処理やブロッキング処理は置かない。
- `patch-elements!` が返す deferred の結果を確認し、stream が更新を受け付けなくなった場合は送信ループを終了する。
- Datastar SDK を使う対象は、まず Datastar プロトコルのイベント生成に限定する。

## まだ評価していないこと

- `remove-element!`
- `execute-script!`
- `1.0.0-RC10` とより新しい RC 版の差分
- SDK 追加による依存関係やバージョン影響

## Phase 3 で評価したこと

- 長時間接続する Datastar SSE stream を SDK adapter に任せる設計
  - 既存の `/events` は、手書き JSON SSE の参考実装として維持する。
  - Datastar SDK 版は `/live-status` として新設し、`/events` の置き換えではなく比較対象として扱う。
  - `data-init` で長時間接続を開始する親要素は固定し、配下の `#live-status` だけを DOM patch の対象にする。
  - サーバー時刻と更新回数を 1 秒ごとに送る処理を virtual thread で実行する。
  - curl で複数の `datastar-patch-elements` event と、接続終了後の `on-close` callback を確認した。

## 現時点の判断

SDK は「すべての SSE を置き換える道具」としてではなく、「Datastar プロトコル部分を任せる道具」として採用するのがよい。

今後 Datastar の利用範囲が増える場合は、手書きで SSE frame を組み立てるよりも、SDK の API 名に寄せた実装の方が意図を読み取りやすく、プロトコル変更にも追従しやすい。
