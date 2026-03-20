# Phase 2: Datastar 統合 - 移行戦略書

## 📋 目的
既存の **React SPA + Spring Boot** 運用システムに対して、段階的に **Datastar + Clojure** を統合する。
本番運用を継続しながら、限定的な機能から置き換えていく「共存・移行」アプローチ。

---

## 🔐 既存システムの認証・CSRF対策

### 現状確認（tradehub-web）
React SPA は以下の認証メカニズムを採用：

1. **ログイン処理**
   - エンドポイント: `POST /api/auth/login`
   - 入力: companyCode, loginCode, password
   - 出力: `accessToken` (JWT)
   - 保管場所: localStorage

2. **CSRF対策**
   - エンドポイント: `GET /api/csrf`
   - 出力: CSRF token
   - 使用: POST/PUT/DELETE リクエスト時に `X-XSRF-TOKEN` ヘッダーに付加
   - 例外: `ignoreCsrfUrls` に登録されたエンドポイント（ログイン等）

3. **API呼び出し時の認証**
   - すべてのリクエストに `Authorization: Bearer {accessToken}` ヘッダーを付加
   - Cookie は `withCredentials: true` で自動送信
   - Axios interceptor で自動処理

### 重要なコンポーネント
```
src/lib/api.ts              → API インスタンス設定
src/lib/axios/interceptors.ts → 認証・CSRF処理
src/stores/auth/csrfTokenStore.ts → CSRF token管理
src/utils/auth.ts           → JWT token管理（getAccessToken等）
```

---

## 🎯 Phase 2 実装方針

### 基本戦略：「既存認証に乗る」

**重要な前提：**
- React は**ログイン・認証の主役のままにする**
- Datastar は**既存のJWT・CSRF tokenを活用**して動作
- つまり、Datastar は認証済みセッション内での「部分的なUI更新」に特化

### 実装フロー

#### Step 1: 認証環境の下準備
1. Clojure側で JWT token 検証ミドルウェアを実装
2. CSRF token 検証を有効化
3. Spring Bootと同じDB（またはユーザーテーブル）へのアクセス

#### Step 2: 最小MVPを選定
**候補：`/api/users/me` エンドポイント + ユーザープロフィール画面**

理由：
- バックエンド側がシンプル（JWTから user ID抽出 → DB照会）
- フロント側の依存度が低い
- Datastarの置き換え効果が明確に見える
- 本番環境への影響が最小

#### Step 3: Clojure実装構成
```
clj-star-bridge/
├── src/clj_star_bridge/
│   ├── core.clj           (既存：SSE基盤)
│   ├── auth.clj          NEW (JWT検証、CSRF検証)
│   ├── db.clj            NEW (DB接続、user照会)
│   └── user.clj          NEW (/api/users/me ハンドラ)
└── deps.edn              (DB ドライバ追加 ex: PostgreSQL)
```

#### Step 4: Datastar JS設定
- React コンポーネントから Datastar スクリプトを読み込み
- JWT token をグローバル変数で Datastar JS から参照可能に
- CSRF token も同様

#### Step 5: SSE通信での送信
- `/api/users/me` をSSE対応に変更（Datastar フレームワークに統合）
- Hiccup で HTMLフラグメント生成
- SSE ストリームで部分更新

---

## 🛠️ 候補エンドポイント優先度

| 優先度 | エンドポイント | 理由 | 複雑度 |
|-------|---------|------|-------|
| 🔴 1 | `/api/users/me` | シンプル、認証フロー標準 | ⭐ |
| 🟡 2 | `/api/users/list` | リスト表示、検索可能 | ⭐⭐ |
| 🟡 3 | `/api/projects/{id}` | プロジェクト詳細 | ⭐⭐⭐ |
| 🟢 4 | `/api/projects` | 複数プロジェクト管理 | ⭐⭐⭐⭐ |

**推奨：1番目の `/api/users/me` から開始**

---

## 📝 React側への組み込み方

### 案：ハイブリッド画面

```html
<!-- src/features/profile/UserProfile.tsx -->

<div className="profile-container">
  {/* React部分：従来のUIそのまま */}
  <h1>ユーザープロフィール</h1>
  <p>ログアウト: {userData.name}</p>

  {/* Datastar統合部分：NEW */}
  <div id="user-profile-datastar" data-on-load="@get('/api/users/me-datastar')">
    <!-- ここにDatastarが SSE で HTML を流し込む -->
    <p>読込中...</p>
  </div>
</div>

<script type="module" src="https://cdn.jsdelivr.net/...datastar.js"></script>
<script>
  // React から Datastar へ token を渡す
  window.accessToken = userData.accessToken;
  window.csrfToken = csrfToken;
</script>
```

---

## ⚠️ 実装時の注意点

1. **JWT検証**
   - Spring Boot と同じ署名鍵を使用（環境変数で管理）
   - token 有効期限チェック

2. **CSRF対策**
   - `POST /api/users/me-datastar` は CSRF token 検証必須
   - ただし、`GET` でのSSE接続は CSRF対象外（HTTP スペックでは GET は state-changing ではない）

3. **DBアクセス**
   - Spring Boot と同じDB（PostgreSQL等）にアクセス
   - コネクションプール設定（c3p0 or HikariCP相当）

4. **SSE + React の共存**
   - React の state と Datastar の DOM の不整合に注意
   - 画面分割が重要：React 領域と Datastar 領域は分離

5. **段階的ロールアウト**
   - 本番環境では `feature flag` で Datastar エンドポイントを制御
   - 環境変数 `USE_DATASTAR_PROFILE=false` → 従来の React APIに切り替え

---

## 📅 実装タイムライン（予想）

| Phase | タスク | 見積 |
|-------|--------|------|
| 準備 | JWT/CSRF ミドルウェア実装 | 3-4h |
| 準備 | DB接続、user テーブル照会 | 2-3h |
| 実装 | `/api/users/me` の SSE版実装 | 3-4h |
| テスト | ローカル統合テスト | 2h |
| デプロイ | ステージング・本番反映 | 1-2h |
| **合計** | | **11-16h** |

---

## 📌 次のステップ

### すぐにやること
1. Spring Boot のユーザーテーブルスキーマ確認
2. JWT署名鍵の入手（Spring Boot から秘密鍵を取得）
3. DBコネクション情報確認（PostgreSQL ホスト、ポート等）
4. Clojure の依存ライブラリ追加（JDBC driver等）

### コード実装開始
1. JWT検証ミドルウェア実装 (`auth.clj`)
2. DB照会ロジック実装 (`db.clj`)
3. `/api/users/me` ハンドラ実装 (`user.clj`)
4. ローカルテスト実行

---

## 📝 実装メモ
実装中に新たに発見した課題・制約があれば、ここに追記してください。
