package com.observability.commons.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.ResourceAttributes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for OpenTelemetry distributed tracing.
 * 
 * <p>This configuration sets up OpenTelemetry SDK with OTLP exporter for
 * sending traces to the observability backend (e.g., Grafana Tempo via Alloy).</p>
 * 
 * <p>The configuration is enabled by default but can be disabled by setting
 * the property {@code otel.sdk.disabled=true}.</p>
 *
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(name = "otel.sdk.disabled", havingValue = "false", matchIfMissing = true)
public class OpenTelemetryConfig {

    private final String otlpEndpoint;
    private final String serviceName;

    /**
     * Constructs a new OpenTelemetryConfig with the required configuration values.
     *
     * @param otlpEndpoint the OTLP exporter endpoint URL
     * @param serviceName the name of this service for trace identification
     */
    public OpenTelemetryConfig(
            @Value("${otel.exporter.otlp.endpoint}") final String otlpEndpoint,
            @Value("${spring.application.name}") final String serviceName) {
        this.otlpEndpoint = otlpEndpoint;
        this.serviceName = serviceName;
    }

    /**
     * Creates and configures the OpenTelemetry instance with OTLP exporter.
     * 
     * <p>The OpenTelemetry instance is configured with:</p>
     * <ul>
     *   <li>Service name and version resource attributes</li>
     *   <li>Batch span processor for efficient trace export</li>
     *   <li>W3C Trace Context propagation</li>
     * </ul>
     *
     * @return configured OpenTelemetry instance registered globally
     */
    @Bean
    public OpenTelemetry openTelemetry() {
        final var resource = Resource.getDefault()
            .merge(Resource.create(
                Attributes.of(
                    ResourceAttributes.SERVICE_NAME, serviceName,
                    ResourceAttributes.SERVICE_VERSION, "1.0.0"
                )
            ));

        final var sdkTracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(
                OtlpGrpcSpanExporter.builder()
                    .setEndpoint(otlpEndpoint)
                    .build()
            ).build())
            .setResource(resource)
            .build();

        return OpenTelemetrySdk.builder()
            .setTracerProvider(sdkTracerProvider)
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .buildAndRegisterGlobal();
    }

    /**
     * Creates a Tracer instance for creating spans in this service.
     *
     * @param openTelemetry the OpenTelemetry instance to create the tracer from
     * @return a Tracer configured with this service's name and version
     */
    @Bean
    public Tracer tracer(final OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer(serviceName, "1.0.0");
    }
}
