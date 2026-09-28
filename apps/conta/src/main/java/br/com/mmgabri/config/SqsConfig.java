package br.com.mmgabri.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.time.Duration;

@Configuration
public class SqsConfig {

    private static final Logger logger = LoggerFactory.getLogger(SqsConfig.class);

    @Bean
    public SqsClient sqsClient(@Value("${aws.sqs.region:us-east-1}") String awsRegion) {
        try {
            logger.info("Creating AWS SQS client. region={}", awsRegion);
            var httpClient = ApacheHttpClient.builder()
                    .maxConnections(200)
                    .connectionTimeout(Duration.ofSeconds(2))
                    // receiveMessage long-polls (aws.sqs.wait-time-seconds, up to 20s) - the
                    // socket stays idle on purpose for that long; socketTimeout must exceed it
                    // with real headroom (local network/AWS latency can go slightly past 20s),
                    // otherwise every receiveMessage times out before the long-poll ends (it
                    // was 5s here, copied unchanged from the authorizer/ledger publisher,
                    // which never long-polls).
                    .socketTimeout(Duration.ofSeconds(35))
                    .connectionAcquisitionTimeout(Duration.ofSeconds(1))
                    .build();

            SqsClient sqsClient = SqsClient.builder()
                    .region(Region.of(awsRegion))
                    .httpClient(httpClient)
                    .build();
            logger.info("AWS SQS client created. region={}", awsRegion);
            return sqsClient;
        } catch (Exception e) {
            logger.error("Failed to create AWS SQS client. region={}, error={}", awsRegion, e.getMessage(), e);
            throw e;
        }
    }
}
