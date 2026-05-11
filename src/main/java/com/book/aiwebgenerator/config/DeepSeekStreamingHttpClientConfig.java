package com.book.aiwebgenerator.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.exception.HttpException;
import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.http.client.HttpRequest;
import dev.langchain4j.http.client.SuccessfulHttpResponse;
import dev.langchain4j.http.client.spring.restclient.SpringRestClient;
import dev.langchain4j.http.client.sse.ServerSentEventListener;
import dev.langchain4j.http.client.sse.ServerSentEventParser;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class DeepSeekStreamingHttpClientConfig {

    private static final String STREAMING_CHAT_MODEL_HTTP_CLIENT_BUILDER = "openAiStreamingChatModelHttpClientBuilder";
    private static final String STREAMING_CHAT_MODEL_TASK_EXECUTOR = "openAiStreamingChatModelTaskExecutor";

    @Bean(STREAMING_CHAT_MODEL_HTTP_CLIENT_BUILDER)
    @ConditionalOnProperty("langchain4j.open-ai.streaming-chat-model.api-key")
    HttpClientBuilder openAiStreamingChatModelHttpClientBuilder(
            ObjectProvider<RestClient.Builder> restClientBuilder,
            @Qualifier(STREAMING_CHAT_MODEL_TASK_EXECUTOR) AsyncTaskExecutor executor) {
        HttpClientBuilder delegate = SpringRestClient.builder()
                .restClientBuilder(restClientBuilder.getIfAvailable(RestClient::builder))
                .streamingRequestExecutor(executor);
        return new DeepSeekThinkingModeHttpClientBuilder(delegate);
    }

    private static final class DeepSeekThinkingModeHttpClientBuilder implements HttpClientBuilder {

        private final HttpClientBuilder delegate;

        private DeepSeekThinkingModeHttpClientBuilder(HttpClientBuilder delegate) {
            this.delegate = delegate;
        }

        @Override
        public Duration connectTimeout() {
            return delegate.connectTimeout();
        }

        @Override
        public HttpClientBuilder connectTimeout(Duration timeout) {
            delegate.connectTimeout(timeout);
            return this;
        }

        @Override
        public Duration readTimeout() {
            return delegate.readTimeout();
        }

        @Override
        public HttpClientBuilder readTimeout(Duration timeout) {
            delegate.readTimeout(timeout);
            return this;
        }

        @Override
        public HttpClient build() {
            return new DeepSeekThinkingModeHttpClient(delegate.build());
        }
    }

    private static final class DeepSeekThinkingModeHttpClient implements HttpClient {

        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

        private final HttpClient delegate;

        private DeepSeekThinkingModeHttpClient(HttpClient delegate) {
            this.delegate = delegate;
        }

        @Override
        public SuccessfulHttpResponse execute(HttpRequest request) throws HttpException, RuntimeException {
            return delegate.execute(disableThinkingForDeepSeekV4(request));
        }

        @Override
        public void execute(HttpRequest request, ServerSentEventParser parser, ServerSentEventListener listener) {
            delegate.execute(disableThinkingForDeepSeekV4(request), parser, listener);
        }

        private HttpRequest disableThinkingForDeepSeekV4(HttpRequest request) {
            String body = request.body();
            if (body == null || body.isBlank()) {
                return request;
            }
            try {
                JsonNode root = OBJECT_MAPPER.readTree(body);
                if (!(root instanceof ObjectNode requestBody)) {
                    return request;
                }
                JsonNode modelNode = requestBody.get("model");
                if (modelNode == null || !modelNode.asText("").startsWith("deepseek-v4-")) {
                    return request;
                }
                if (!requestBody.has("thinking")) {
                    // LangChain4j does not persist DeepSeek reasoning_content across tool calls,
                    // so disable thinking mode for V4 tool-calling streams.
                    ObjectNode thinking = OBJECT_MAPPER.createObjectNode();
                    thinking.put("type", "disabled");
                    requestBody.set("thinking", thinking);
                }
                return HttpRequest.builder()
                        .method(request.method())
                        .url(request.url())
                        .headers(request.headers())
                        .body(OBJECT_MAPPER.writeValueAsString(requestBody))
                        .build();
            } catch (Exception ignored) {
                return request;
            }
        }
    }
}
