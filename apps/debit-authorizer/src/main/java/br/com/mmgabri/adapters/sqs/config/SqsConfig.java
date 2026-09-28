package br.com.mmgabri.adapters.sqs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.time.Duration;

@Configuration
public class SqsConfig {

    @Bean
    public SqsClient sqsClient() {
        var httpClient = ApacheHttpClient.builder()
                .maxConnections(200)
                .connectionTimeout(Duration.ofSeconds(2))
                .socketTimeout(Duration.ofSeconds(5))
                .build();

        return SqsClient.builder()
                .region(Region.US_EAST_1)
                .httpClient(httpClient)
                .build();
    }
}