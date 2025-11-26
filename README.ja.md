# Grafanaスタックによるマイクロサービスオブザーバビリティ

[![CI Build and Test](https://github.com/drag0sd0g/microservice-observability-instrumentation/actions/workflows/ci.yml/badge.svg)](https://github.com/drag0sd0g/microservice-observability-instrumentation/actions/workflows/ci.yml)
[![CodeQL](https://github.com/drag0sd0g/microservice-observability-instrumentation/actions/workflows/codeql.yml/badge.svg)](https://github.com/drag0sd0g/microservice-observability-instrumentation/actions/workflows/codeql.yml)
[![License](https://img.shields.io/github/license/drag0sd0g/microservice-observability-instrumentation)](LICENSE)
[![Java Version](https://img.shields.io/badge/Java-21-blue.svg)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![Gradle Version](https://img.shields.io/badge/Gradle-8.11.1-blue.svg)](https://gradle.org/)

🌐 **[English version here (英語版)](README.md)**

分散マイクロサービスアーキテクチャにおいて、Grafanaスタック（Prometheus、Loki、Tempo、Alloy、Grafana）とOpenTelemetry計装を使用した、モダンなオブザーバビリティ実践の包括的なデモンストレーションです。

## 🎯 プロジェクト概要

このプロジェクトでは、以下の機能を備えた完全なオブザーバビリティソリューションを紹介しています：

- **分散トレーシング** - TempoとOpenTelemetryを使用
- **メトリクス収集** - PrometheusとGrafanaを使用
- **ログ集約** - Lokiを使用
- **テレメトリパイプライン** - Grafana Alloyを使用
- **アラート** - Prometheus Alertmanagerを使用
- **相関** - ログ、メトリクス、トレース間の相関

## 🏗️ アーキテクチャ

```
┌─────────────────────────────────────────────────────────────────────────┐
│                            クライアント/ブラウザ                           │
└────────────────────────────────┬────────────────────────────────────────┘
                                 │
                    ┌────────────▼──────────────┐
                    │   Gateway Service :8080   │
                    │   （REST APIゲートウェイ）    │
                    └────────┬──────────┬───────┘
                             │          │
              ┌──────────────┘          └─────────────┐
              │                                       │
    ┌─────────▼──────────┐                 ┌──────────▼─────────┐
    │ Order Service      │                 │ Inventory Service  │
    │ :8081              │                 │ :8082              │
    │ (注文管理)          │                 │ (在庫確認)          │
    └─────────┬──────────┘                 └──────────┬─────────┘
              │                                       │
              └────────────────┬──────────────────────┘
                               │
                    ┌──────────▼──────────┐
                    │   CockroachDB       │
                    │   :26257            │
                    │   (分散DB)           │
                    └─────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                        オブザーバビリティスタック                           │
│                                                                         │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────────────┐      │
│  │ Services │──▶│  Alloy   │──▶│Prometheus│──▶│    Grafana       │      │
│  │  OTLP    │   │  :4317   │   │  :9090   │   │    :3000         │      │
│  └──────────┘   └────┬─────┘   └──────────┘   └──────────────────┘      │
│                      │                                  ▲               │
│                      ├────────────────┐                 │               │
│                      │                │                 │               │
│                 ┌────▼─────┐    ┌────▼─────┐            │               │
│                 │   Loki   │    │  Tempo   │            │               │
│                 │  :3100   │    │  :3200   │────────────┘               │
│                 └──────────┘    └──────────┘                            │
│                                                                         │
│                 ┌──────────────┐                                        │
│                 │ Alertmanager │                                        │
│                 │    :9093     │                                        │
│                 └──────────────┘                                        │
└─────────────────────────────────────────────────────────────────────────┘
```

## 🚀 サービス

### Gateway Service（ポート8080）
- 下流サービスへのリクエストをルーティングするREST APIゲートウェイ
- OrderサービスとInventoryサービス間の呼び出しを調整
- 注文管理のエンドポイントを公開

### Order Service（ポート8081）
- 注文の作成、取得、一覧表示を管理
- CockroachDBに注文を永続化
- カスタムビジネスメトリクス（orders_created_total）を出力

### Inventory Service（ポート8082）
- 在庫の可用性確認を処理
- カオスエンジニアリング機能を搭載：
  - 設定可能なレイテンシ注入（500-2000ms）
  - ランダムエラー生成（設定可能なレート）
- CockroachDBに在庫データを永続化

## 📊 オブザーバビリティスタック

### Prometheus（ポート9090）
- `/actuator/prometheus`経由で全サービスからメトリクスを収集
- Alloyからメトリクスを収集
- アラートルールを評価
- メトリクスの保存とクエリを提供

### Loki（ポート3100）
- サービスからの構造化JSONログを集約
- Alloy OTLPパイプライン経由でログを受信
- ログのクエリとフィルタリングを提供

### Tempo（ポート3200）
- 分散トレースを保存およびクエリ
- Alloy経由でOTLPトレースを受信
- サービスグラフとスパンメトリクスを生成

### Grafana Alloy（ポート4317、4318、12345）
- OpenTelemetryコレクターとして機能
- OTLPトレース、メトリクス、ログを受信
- テレメトリを処理してバックエンドにルーティング
- サービスからPrometheusメトリクスを収集

### Grafana（ポート3000）
- 統合オブザーバビリティUI
- 事前設定済みデータソース（Prometheus、Loki、Tempo）
- 事前構築済みダッシュボード：
  - サービス概要ダッシュボード
  - REDメトリクスダッシュボード
- 認証情報：admin/admin

### Alertmanager（ポート9093）
- Prometheusからのアラートを管理
- 複数チャネルへのルーティングをサポート（設定可能）

## 🔧 技術スタック

- **言語**: Java 21
- **フレームワーク**: Spring Boot 3.2.0
- **ビルドツール**: Gradle 8.11.1
- **データベース**: CockroachDB（PostgreSQL互換）
- **計装**: OpenTelemetry SDK
- **ロギング**: Logback with Logstash JSON encoder
- **メトリクス**: Micrometer with Prometheus registry
- **コンテナ**: Docker & Docker Compose

## 📋 要件

- Docker 20.10+
- Docker Compose 2.0+
- 最低8GB RAM（推奨：16GB）
- 使用可能ポート：3000、3100、3200、4317、4318、8080、8081、8082、8090、9090、9093、12345、26257

### macOSユーザー向け
- Docker Desktopに少なくとも6GBのメモリを割り当てる（設定 → リソース → メモリ）
- プロジェクトディレクトリのファイル共有が有効になっていることを確認（設定 → リソース → ファイル共有）
- M1/M2 Mac：すべてのイメージがARM64アーキテクチャをサポート

## 🚀 クイックスタート

### 1. リポジトリのクローン

```bash
git clone https://github.com/drag0sd0g/microservice-observability-instrumentation.git
cd microservice-observability-instrumentation
```

**重要**: 以降のすべてのコマンドは、このディレクトリ（プロジェクトルート）から実行する必要があります。

### 2. サービスのビルド

```bash
# ルートから全サービスをビルド
./gradlew clean build
```

### 3. スタックの起動

```bash
# 重要：プロジェクトルートディレクトリから実行
docker compose up -d
```

すべてのサービスが正常に起動するまで待機（30-60秒）：

```bash
docker compose ps
```

サービスの起動に失敗した場合は、[トラブルシューティング](#-トラブルシューティング)セクションを確認してください。

### 4. UIへのアクセス

- **Grafana**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9090
- **Alloy**: http://localhost:12345
- **Alertmanager**: http://localhost:9093
- **CockroachDB UI**: http://localhost:8090
- **Gateway Service**: http://localhost:8080/api/health

## 📝 APIコール例

### ヘルスチェック
```bash
curl http://localhost:8080/api/health
```

### 注文の作成
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"itemId":"ITEM-123","quantity":5}'
```

### 全注文の取得
```bash
curl http://localhost:8080/api/orders
```

### 特定の注文の取得
```bash
curl http://localhost:8080/api/orders/{order-id}
```

### 在庫確認
```bash
curl http://localhost:8080/api/inventory/ITEM-123
```

## 🎭 デモスクリプト

プロジェクトには、リアルなトラフィックを生成するデモスクリプトが含まれています：

### トラフィックシミュレーションの実行
```bash
./demo.sh traffic 300  # 300秒（5分間）実行
```

### 手動操作
```bash
# 単一の注文を作成
./demo.sh order ITEM-5 3

# 在庫確認
./demo.sh inventory ITEM-5

# 全注文を取得
./demo.sh orders
```

### カオスエンジニアリング
```bash
# レイテンシカオスを有効化（500-2000ms）
./demo.sh chaos-latency-on

# レイテンシカオスを無効化
./demo.sh chaos-latency-off

# エラーカオスを有効化（20%エラーレート）
./demo.sh chaos-errors-on

# エラーカオスを無効化
./demo.sh chaos-errors-off
```

## 📈 オブザーバビリティデータの表示

### ダッシュボード

1. **サービス概要ダッシュボード** (`http://localhost:3000/d/service-overview`)
   - サービスごとのリクエストレート
   - サービスごとのエラーレート
   - レイテンシパーセンタイル（P50/P90/P99）
   - ライブログストリーム
   - トレースエクスプローラー

2. **REDメトリクスダッシュボード** (`http://localhost:3000/d/red-metrics`)
   - レート：サービスごとのリクエスト/秒
   - エラー：サービスごとのエラー率
   - 期間：サービスごとのP99レイテンシ
   - ビジネスメトリクス：注文作成カウンター

### ログの探索

1. Grafana → Exploreに移動
2. **Loki**データソースを選択
3. クエリ例：
   ```logql
   {service_name="gateway-service"}
   {service_name="order-service"} |= "error"
   {service_name=~".+"} | json | severity="ERROR"
   ```

### トレースの表示

1. Grafana → Exploreに移動
2. **Tempo**データソースを選択
3. TraceQLクエリを使用：
   ```traceql
   {service.name="gateway-service"}
   {name="create-order-flow"}
   ```
4. 任意のトレースをクリックしてスパンウォーターフォールを表示

### ログ、メトリクス、トレースの相関

1. トレースビューで「Logs for this span」をクリック
2. Grafanaが自動的にtrace_idでLokiをクエリ
3. trace_idを含むログ行から、TraceIDリンクをクリックしてTempoにジャンプ
4. トレースビューで「Metrics」をクリックして関連するPrometheusメトリクスを表示

## 🔔 アラート

### 設定済みアラート

1. **高エラーレート**（2分間で10%超）
2. **高レイテンシ**（5分間でP99が500ms超）
3. **サービスダウン**（1分間メトリクスなし）
4. **高リクエストレート**（5分間で100 req/s超）

### アクティブアラートの表示
```bash
# Prometheusアラートを確認
curl http://localhost:9090/api/v1/alerts

# Alertmanagerを確認
curl http://localhost:9093/api/v2/alerts
```

### 通知の設定

`observability/alertmanager/config.yml`を編集して以下を追加：
- メール通知
- Slack Webhook
- PagerDuty連携
- カスタムWebhook

## 🔍 テレメトリフロー

```
┌──────────────┐
│   Services   │
│  (Java App)  │
└──────┬───────┘
       │
       │ OpenTelemetry SDK
       │ - Traces (OTLP/gRPC)
       │ - Metrics (Prometheus)
       │ - Logs (JSON stdout)
       │
       ▼
┌──────────────┐
│    Alloy     │ ◄─── Prometheusメトリクスを収集
│  (Collector) │      /actuator/prometheusから
└──────┬───────┘
       │
       ├────────────┬─────────────┐
       │            │             │
       ▼            ▼             ▼
┌──────────┐ ┌──────────┐ ┌──────────┐
│Prometheus│ │   Loki   │ │  Tempo   │
│(メトリクス)│ │ (ログ)   │ │(トレース) │
└──────────┘ └──────────┘ └──────────┘
       │            │             │
       └────────────┴─────────────┘
                    │
                    ▼
             ┌──────────────┐
             │   Grafana    │
             │   (統合UI)   │
             └──────────────┘
```

## 🧪 テストシナリオ

### シナリオ1：通常運用
```bash
./demo.sh traffic 120
```
- 通常のレイテンシを観察（P99 < 100ms）
- Grafanaでサービスログを確認
- 分散トレースを表示

### シナリオ2：高レイテンシ
```bash
./demo.sh chaos-latency-on
./demo.sh traffic 60
./demo.sh chaos-latency-off
```
- ダッシュボードでレイテンシスパイクを監視
- 高レイテンシアラートをトリガー
- 遅いトレースとログを相関

### シナリオ3：エラー注入
```bash
./demo.sh chaos-errors-on
./demo.sh traffic 60
./demo.sh chaos-errors-off
```
- エラーレートの増加を監視
- 高エラーレートアラートをトリガー
- トレースでエラースパンを表示

### シナリオ4：サービス障害
```bash
docker-compose stop inventory-service
./demo.sh traffic 30
docker-compose start inventory-service
```
- サービスダウンアラートをトリガー
- ゲートウェイでのカスケードエラーを観察
- 復旧時間を確認

## 🛠️ 開発

### テストの実行

```bash
# Gateway Service
cd services/gateway-service
./gradlew test

# Order Service
cd services/order-service
./gradlew test

# Inventory Service
cd services/inventory-service
./gradlew test
```

### ローカル開発（Dockerなし）

1. ローカルでCockroachDBを起動
2. ローカルプロファイルで各サービスを起動：
```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### カスタムメトリクスの追加

```java
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Autowired
private MeterRegistry meterRegistry;

Counter customCounter = Counter.builder("custom_metric_total")
    .description("カスタムメトリクスの説明")
    .register(meterRegistry);

customCounter.increment();
```

### カスタムスパンの追加

```java
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

@Autowired
private Tracer tracer;

Span span = tracer.spanBuilder("custom-operation").startSpan();
try {
    span.setAttribute("custom.attribute", "value");
    // ビジネスロジック
} finally {
    span.end();
}
```

## 📊 主要メトリクス

### REDメトリクス（Rate、Errors、Duration）
- `http_server_requests_seconds_count` - リクエスト数
- `http_server_requests_seconds_sum` - 合計時間
- `http_server_requests_seconds_bucket` - レイテンシヒストグラム

### ビジネスメトリクス
- `orders_created_total` - 作成された注文の合計

### トレースのカスタム属性
- `service.name` - サービス識別子
- `trace_id` - 分散トレースID
- `span_id` - スパン識別子
- 操作ごとのカスタム属性

## 🔐 セキュリティに関する考慮事項

⚠️ **これはデモプロジェクトです。本番デプロイメントには以下を含める必要があります：**

- 認証と認可
- TLS/SSL暗号化
- シークレット管理（ハードコードされた認証情報は不可）
- ネットワークポリシーとセグメンテーション
- レート制限
- 入力検証とサニタイズ

## 🐛 トラブルシューティング

### macOS固有の問題

#### Liquibase初期化が「file does not exist」で失敗する
`/liquibase/changelog/init-databases.yaml does not exist`のようなエラーが表示された場合：

1. **Docker Desktopのファイル共有が有効になっていることを確認**：
   - Docker Desktop → 設定/環境設定 → リソース → ファイル共有を開く
   - このプロジェクトを含むディレクトリ（または親ディレクトリ）がリストされていることを確認
   - macOSでは、`/Users`は通常デフォルトで共有されています
   - 変更した場合は「適用して再起動」をクリック

2. **プロジェクトルートディレクトリからdocker composeを実行**：
   ```bash
   cd microservice-observability-instrumentation
   docker compose up
   ```
   親ディレクトリまたはサブディレクトリから実行しないでください。

3. **ファイル権限を確認**：
   ```bash
   ls -la observability/liquibase/
   # ファイルは読み取り可能である必要があります（最低でもr--）
   ```

4. **ボリュームマウントが機能していることを確認**：
   ```bash
   docker run --rm -v "$(pwd)/observability/liquibase:/test" alpine ls -la /test
   # init-databases.yamlとliquibase.propertiesが表示されるはずです
   ```

#### 低メモリ（8GB RAM）設定
サービスの起動に失敗したり、動作が遅い場合：

1. **Docker Desktopのメモリ割り当てを増やす**：
   - Docker Desktop → 設定/環境設定 → リソースを開く
   - メモリを少なくとも6GB（合計8GB中）に増やす
   - 「適用して再起動」をクリック

2. **一度にすべてではなく、段階的にサービスを起動**：
   ```bash
   # 最初にデータベースとオブザーバビリティスタックを起動
   docker compose up -d cockroachdb prometheus loki tempo grafana alloy
   
   # 正常になるまで待機（30-60秒）
   docker compose ps
   
   # 次にマイクロサービスを起動
   docker compose up -d gateway-service order-service inventory-service
   ```

3. **未使用のコンテナを停止してリソース使用量を削減**：
   ```bash
   # 開発に必要なサービスのみを実行
   docker compose up -d cockroachdb gateway-service order-service inventory-service grafana
   ```

### サービスが起動しない
```bash
# ログを確認
docker compose logs gateway-service
docker compose logs order-service
docker compose logs inventory-service

# サービスを再起動
docker compose restart
```

### データベース接続の問題
```bash
# CockroachDBの状態を確認
curl http://localhost:8090/health

# CockroachDB SQLシェルにアクセス
docker exec -it cockroachdb ./cockroach sql --insecure

# データベースが作成されたか確認
docker exec -it cockroachdb ./cockroach sql --insecure -e "SHOW DATABASES"
```

### Grafanaダッシュボードが読み込まれない
```bash
# Grafanaを再起動
docker compose restart grafana

# Grafana UIでデータソースの接続を確認
```

### メトリクス/トレース/ログが表示されない
```bash
# Alloyログを確認
docker compose logs alloy

# サービスがテレメトリを送信していることを確認
curl http://localhost:8080/actuator/prometheus
```

### ポート競合
```bash
# ポートを使用しているものを確認
netstat -tuln | grep -E '(3000|8080|9090)'

# 競合するサービスを停止するか、docker-compose.ymlでポートを変更
```

### 完全リセット
すべてが失敗した場合、完全リセットを実行：
```bash
# すべてのコンテナ、ネットワーク、ボリュームを停止して削除
docker compose down -v

# 残っているコンテナを削除
docker system prune -f

# 新規に開始
docker compose up -d
```

## 📚 参考資料

- [OpenTelemetryドキュメント](https://opentelemetry.io/docs/)
- [Grafanaドキュメント](https://grafana.com/docs/)
- [Prometheusドキュメント](https://prometheus.io/docs/)
- [Lokiドキュメント](https://grafana.com/docs/loki/)
- [Tempoドキュメント](https://grafana.com/docs/tempo/)
- [Grafana Alloyドキュメント](https://grafana.com/docs/alloy/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)

## 🤝 コントリビューション

コントリビューションを歓迎します！お気軽にイシューやプルリクエストを提出してください。

## 📄 ライセンス

このプロジェクトはLICENSEファイルに記載された条件に基づいてライセンスされています。

## 🎓 学習成果

このプロジェクトを使用することで、以下を理解できるようになります：

1. ✅ OpenTelemetryでJavaアプリケーションを計装する方法
2. ✅ 完全なGrafanaオブザーバビリティスタックのセットアップ
3. ✅ Grafana Alloyでテレメトリパイプラインを構成
4. ✅ ログ、メトリクス、トレース間の相関を作成
5. ✅ マイクロサービス用のGrafanaダッシュボードを構築
6. ✅ アラートルールと通知のセットアップ
7. ✅ 回復力テスト用のカオスエンジニアリングを実装
8. ✅ マイクロサービスにおける分散トレーシングを理解
9. ✅ 構造化ロギングのベストプラクティス
10. ✅ サービス監視のREDメトリクスパターン
