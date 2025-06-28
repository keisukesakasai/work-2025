// tracing.js - OpenTelemetry初期化ファイル
console.log('🔧 Loading OpenTelemetry tracing module...');

const { NodeSDK } = require('@opentelemetry/sdk-node');
const { getNodeAutoInstrumentations } = require('@opentelemetry/auto-instrumentations-node');
const { OTLPTraceExporter } = require('@opentelemetry/exporter-trace-otlp-grpc');

console.log('✅ OpenTelemetry modules loaded successfully');

// デバッグ情報を表示
console.log(`📝 OTEL_SERVICE_NAME: ${process.env.OTEL_SERVICE_NAME}`);
console.log(`📝 OTEL_EXPORTER_OTLP_ENDPOINT: ${process.env.OTEL_EXPORTER_OTLP_ENDPOINT}`);
console.log(`📝 Node.js version: ${process.version}`);

// OTLP Trace Exporterを作成
const traceExporter = new OTLPTraceExporter({
  url: process.env.OTEL_EXPORTER_OTLP_ENDPOINT || 'http://otel-collector:4317',
});

console.log('🔗 OTLP Trace Exporter created');

// NodeSDKを初期化
const sdk = new NodeSDK({
  traceExporter,
  instrumentations: [getNodeAutoInstrumentations({
    // 明示的に有効にする
    '@opentelemetry/instrumentation-express': { enabled: true },
    '@opentelemetry/instrumentation-http': { enabled: true },
    '@opentelemetry/instrumentation-https': { enabled: true },
    // 不要なものを無効化
    '@opentelemetry/instrumentation-fs': { enabled: false },
  })],
});

console.log('🚀 NodeSDK initialized, starting...');

// SDK開始（新しいバージョンではPromiseを返さない）
try {
  sdk.start();
  console.log('✅ OpenTelemetry SDK started successfully');
  console.log('🎯 Tracing is now active for nodejs-server');
} catch (error) {
  console.error('❌ Error starting OpenTelemetry SDK:', error);
  process.exit(1);
}

// プロセス終了時のクリーンアップ
process.on('SIGTERM', () => {
  console.log('🛑 Shutting down OpenTelemetry...');
  try {
    sdk.shutdown();
    console.log('🛑 OpenTelemetry shutdown complete');
  } catch (error) {
    console.error('❌ Error shutting down OpenTelemetry:', error);
  } finally {
    process.exit(0);
  }
});

// エクスポート（他のファイルから使用可能にする）
module.exports = sdk; 