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
            logger.info("Criando cliente AWS SQS. region={}", awsRegion);
            var httpClient = ApacheHttpClient.builder()
                    .maxConnections(200)
                    .connectionTimeout(Duration.ofSeconds(2))
                    // receiveMessage faz long-poll (aws.sqs.wait-time-seconds, até 20s) - o
                    // socket fica de propósito sem tráfego até esse tempo; socketTimeout
                    // precisa ser maior que isso com folga real (rede local/latência da AWS
                    // podem passar um pouco dos 20s), senão todo receiveMessage estoura
                    // antes do long-poll terminar (era 5s aqui, copiado sem ajuste do
                    // publisher do autorizador/ledger, que nunca faz long-poll).
                    .socketTimeout(Duration.ofSeconds(35))
                    .connectionAcquisitionTimeout(Duration.ofSeconds(1))
                    .build();

            SqsClient sqsClient = SqsClient.builder()
                    .region(Region.of(awsRegion))
                    .httpClient(httpClient)
                    .build();
            logger.info("Cliente AWS SQS criado com sucesso. region={}", awsRegion);
            return sqsClient;
        } catch (Exception e) {
            logger.error("Erro ao criar cliente AWS SQS. region={}, error={}", awsRegion, e.getMessage(), e);
            throw e;
        }
    }
}
