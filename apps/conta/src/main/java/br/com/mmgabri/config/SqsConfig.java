package br.com.mmgabri.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class SqsConfig {

    private static final Logger logger = LoggerFactory.getLogger(SqsConfig.class);

    @Bean
    public SqsClient sqsClient(@Value("${aws.sqs.region:us-east-1}") String awsRegion) {
        try {
            logger.info("Criando cliente AWS SQS. region={}", awsRegion);
            SqsClient sqsClient = SqsClient.builder()
                    .region(Region.of(awsRegion))
                    .build();
            logger.info("Cliente AWS SQS criado com sucesso. region={}", awsRegion);
            return sqsClient;
        } catch (Exception e) {
            logger.error("Erro ao criar cliente AWS SQS. region={}, error={}", awsRegion, e.getMessage(), e);
            throw e;
        }
    }
}
