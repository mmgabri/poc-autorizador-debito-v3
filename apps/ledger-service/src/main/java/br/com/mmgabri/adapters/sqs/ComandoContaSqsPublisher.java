package br.com.mmgabri.adapters.sqs;

import br.com.mmgabri.domain.ComandoContaRequest;
import br.com.mmgabri.services.MetricsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.time.OffsetDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ComandoContaSqsPublisher {

    private static final Logger logger = LoggerFactory.getLogger(ComandoContaSqsPublisher.class);

    private final MetricsService metricsService;
    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.queue-name}")
    private String queueName;

    private volatile String queueUrl;

    public void publish(ComandoContaRequest request) {
        var startTime = OffsetDateTime.now();
        String payload = toJson(request);
        String url = resolveQueueUrl();

        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(url)
                .messageBody(payload)
                .messageAttributes(Map.of(
                        "instanceId", MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(request.instanceId())
                                .build()
                ))
                .build());

        metricsService.incrementMetric("app_ledger_duration_publish_sqs", startTime, "instanceId:"+request.instanceId());
        metricsService.incrementMetricCounter("app_ledger_msg_send_conta");

        logger.debug("Comando publicado no SQS. correlationId={} instanceId={}", request.correlationId(), request.instanceId());
    }

    private String resolveQueueUrl() {
        if (queueUrl == null) {
            synchronized (this) {
                if (queueUrl == null) {
                    queueUrl = sqsClient.getQueueUrl(
                            GetQueueUrlRequest.builder().queueName(queueName).build()
                    ).queueUrl();
                    logger.debug("Fila SQS resolvida. queueUrl={}", queueUrl);
                }
            }
        }
        return queueUrl;
    }

    private String toJson(ComandoContaRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Erro ao serializar ComandoContaRequest", e);
        }
    }
}
