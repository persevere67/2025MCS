package com.medical.qna.medical_qna_system.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        // 配置HttpClient，用于WebClient.Builder
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000) // 连接超时30秒
                .responseTimeout(Duration.ofMinutes(5)) // 响应超时5分钟
                .keepAlive(true);

        // 返回一个预配置的WebClient.Builder实例
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> {
                    configurer.defaultCodecs().maxInMemorySize(32 * 1024 * 1024); // 32MB
                    configurer.defaultCodecs().enableLoggingRequestDetails(true);
                });
    }

    @Bean
    // *** 新增：提供一个默认的WebClient实例 ***
    // Spring会自动注入上面定义的webClientBuilder
    public WebClient webClient(WebClient.Builder webClientBuilder) {
        // 使用配置好的builder来构建一个WebClient实例
        return webClientBuilder.build();
    }
}
