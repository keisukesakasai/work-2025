# OpenTelemetry Injector Demo

このプロジェクトは、**OpenTelemetry Injector**を使用してJavaとNode.jsアプリケーションを自動的にインストルメンテーションし、分散トレースをJaegerで可視化するデモ環境です。

## プロジェクト構造

```
otel-injector/
├── README.md                        # プロジェクトドキュメント
├── compose.yaml                     # Docker Compose設定
├── otel-collector-config.yaml       # OpenTelemetry Collector設定
└── app/
    ├── java/                        # Java Load Generator
    │   ├── Dockerfile
    │   ├── pom.xml
    │   └── src/
    │       └── main/java/com/example/
    │           └── LoadGenerator.java
    └── nodejs/                      # Node.js Server
        ├── Dockerfile
        ├── package.json
        └── server.js
```

## 構成

### サービス

1. **nodejs-server** - メインのNode.jsアプリケーション
   - Express.jsでHTTP APIエンドポイントを提供
   - OpenTelemetry Injectorによる自動インストルメンテーション
   - ポート: 3000

2. **java-load-generator** - Java負荷生成ツール
   - JavaでHTTPクライアントを実装
   - NodeJSサーバーに定常的にリクエストを送信
   - OpenTelemetry Injectorによる自動インストルメンテーション
   - 分散トレース対応（Java → Node.js）

3. **otel-collector** - OpenTelemetry Collector
   - テレメトリデータの収集・処理・転送
   - gRPC (4317) と HTTP (4318) でOTLPを受信
   - Jaegerにトレースデータを送信

4. **jaeger** - 分散トレーシング可視化
   - Jaeger UI: http://localhost:16686
   - OTLP対応でトレースデータを受信

### エンドポイント

Node.jsサーバーは以下のエンドポイントを提供：

- `GET /` - ホームエンドポイント
- `GET /api/users` - ユーザー一覧API
- `GET /api/orders` - 注文一覧API（10%の確率でエラーが発生）
- `GET /health` - ヘルスチェック

## 使用方法

### 1. Docker Composeで起動

```bash
docker compose up --build
```

### 2. サービスの確認

起動後、以下のサービスが利用可能になります：

- **Node.js Server**: http://localhost:3000
- **Jaeger UI**: http://localhost:16686
- **OpenTelemetry Collector Health**: http://localhost:13133
- **OpenTelemetry Collector zPages**: http://localhost:55679/debug/tracez

### 3. 分散トレースの確認

1. Jaeger UIにアクセス: http://localhost:16686
2. サービス一覧から以下のサービスが確認できます：
   - **java-load-generator** - Javaクライアントアプリケーション
   - **nodejs-server** - Node.jsサーバーアプリケーション
3. 「Find Traces」をクリック
4. Java Load Generatorが自動的に生成した**分散トレース**が表示されます
5. トレースを選択すると、Java → Node.js間の呼び出し関係とタイミングが可視化されます

### 4. 手動でAPIテスト

```bash
# ホームエンドポイント
curl http://localhost:3000/

# ユーザーAPI
curl http://localhost:3000/api/users

# 注文API（エラーが発生する可能性あり）
curl http://localhost:3000/api/orders

# ヘルスチェック
curl http://localhost:3000/health
```

## OpenTelemetry Injector について

このデモでは、OpenTelemetry Injectorの動作を**シミュレート**しています：

### 実際のOpenTelemetry Injectorでは：

1. `.deb`/.rpm`パッケージをインストール
2. `/usr/lib/opentelemetry/libotelinject.so`が提供される
3. `/etc/ld.so.preload`に追加してシステム全体で有効化
4. `/etc/opentelemetry/otelinject/node.conf`で設定

### このデモでの実装：

- `app/nodejs/Dockerfile`と`app/java/Dockerfile`内で環境を模擬
- Java・Node.js自動インストルメンテーションを直接使用
- 設定ファイルから環境変数を読み込み
- 分散トレース対応でJava → Node.js間のトレースを可視化

## ログとモニタリング

### ログの確認

```bash
# 全サービスのログを表示
docker compose logs -f

# Node.jsサーバーのログのみ
docker compose logs -f nodejs-server

# Java Load Generatorのログのみ
docker compose logs -f java-load-generator

# OpenTelemetry Collectorのログのみ
docker compose logs -f otel-collector
```

### トラブルシューティング

1. **トレースが表示されない場合**
   - OpenTelemetry Collectorが正常に起動しているか確認
   - Jaegerが正常に起動しているか確認
   - Node.jsサーバーのログでインストルメンテーションが動作しているか確認

2. **Load Generatorがエラーを出す場合**
   - Node.jsサーバーが起動完了するまで待つ（5秒の待機時間あり）
   - ネットワーク接続を確認

## 停止方法

```bash
# サービスを停止
docker compose down

# イメージも削除
docker compose down --rmi all

# ボリュームも削除
docker compose down -v
```

## 参考リンク

- [OpenTelemetry Injector GitHub](https://github.com/open-telemetry/opentelemetry-injector)
- [OpenTelemetry Node.js](https://opentelemetry.io/docs/instrumentation/js/)
- [Jaeger](https://www.jaegertracing.io/)
- [OpenTelemetry Collector](https://opentelemetry.io/docs/collector/) 