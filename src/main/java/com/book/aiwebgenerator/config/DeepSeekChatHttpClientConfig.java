package com.book.aiwebgenerator.config;

import dev.langchain4j.exception.HttpException;
import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.http.client.HttpRequest;
import dev.langchain4j.http.client.SuccessfulHttpResponse;
import dev.langchain4j.http.client.sse.ServerSentEventListener;
import dev.langchain4j.http.client.sse.ServerSentEventParser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class DeepSeekChatHttpClientConfig {

    // langchain4j's SpringRestClient constructor calls ClientHttpRequestFactories.get(DEFAULTS),
    // which auto-picks Apache HttpClient5 from the classpath (pulled in transitively via
    // webdrivermanager -> docker-java-transport-httpclient5). That factory truncates DeepSeek's
    // chunked CloudFront responses to a single "{" byte. Provide a minimal HttpClient that
    // uses Spring's RestClient directly with JdkClientHttpRequestFactory, which handles the
    // response correctly.
    @Bean
    @ConditionalOnProperty("langchain4j.open-ai.chat-model.api-key")
    HttpClientBuilder openAiChatModelHttpClientBuilder() {
        return new JdkRestClientBuilder();
    }

    private static final class JdkRestClientBuilder implements HttpClientBuilder {

        private Duration connectTimeout;
        private Duration readTimeout;

        @Override
        public Duration connectTimeout() {
            return connectTimeout;
        }

        @Override
        public HttpClientBuilder connectTimeout(Duration timeout) {
            this.connectTimeout = timeout;
            return this;
        }

        @Override
        public Duration readTimeout() {
            return readTimeout;
        }

        @Override
        public HttpClientBuilder readTimeout(Duration timeout) {
            this.readTimeout = timeout;
            return this;
        }

        @Override
        public HttpClient build() {
            java.net.http.HttpClient.Builder jdk = java.net.http.HttpClient.newBuilder();
            if (connectTimeout != null) {
                jdk.connectTimeout(connectTimeout);
            }
            JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(jdk.build());
            if (readTimeout != null) {
                factory.setReadTimeout(readTimeout);
            }
            RestClient restClient = RestClient.builder().requestFactory(factory).build();
            return new JdkRestClient(restClient);
        }
    }

    private record JdkRestClient(RestClient delegate) implements HttpClient {

        @Override
        public SuccessfulHttpResponse execute(HttpRequest request) throws HttpException {
            RestClient.RequestBodySpec spec = (RestClient.RequestBodySpec) delegate
                    .method(HttpMethod.valueOf(request.method().name()))
                    .uri(request.url())
                    .headers(headers -> request.headers().forEach(headers::addAll));
            if (request.body() != null) {
                spec.body(request.body());
            }
            ResponseEntity<String> response = spec.retrieve().toEntity(String.class);
            return SuccessfulHttpResponse.builder()
                    .statusCode(response.getStatusCode().value())
                    .headers(response.getHeaders())
                    .body(response.getBody())
                    .build();
        }

        @Override
        public void execute(HttpRequest request, ServerSentEventParser parser, ServerSentEventListener listener) {
            throw new UnsupportedOperationException("Streaming is handled by the streaming chat model");
        }
    }
}
