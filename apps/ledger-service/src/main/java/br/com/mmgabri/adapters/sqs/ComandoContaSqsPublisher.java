package br.com.mmgabri.adapters.sqs;

import br.com.mmgabri.domain.ComandoContaRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;

@Component
@RequiredArgsConstructor
public class ComandoContaSqsPublisher {

    private static final Logger logger = LoggerFactory.getLogger(ComandoContaSqsPublisher.class);

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.comando-conta-queue-name}")
    private String queueName;

    private volatile String queueUrl;

    public void publish(ComandoContaRequest request) {
        try {
            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(resolveQueueUrl())
                    .messageBody(toJson(request))
                    .build());

            logger.debug("Comando publicado no SQS para o conta. correlationId={}", request.correlationId());
        } catch (QueueDoesNotExistException e) {
            logger.error("Fila SQS não encontrada. queueName={}", queueName, e);
            queueUrl = null;
            throw new IllegalStateException("Fila SQS não encontrada: " + queueName, e);
        } catch (SqsException e) {
            logger.error("Erro ao publicar na fila SQS. queueName={} statusCode={}", queueName, e.statusCode(), e);
            throw new IllegalStateException("Erro ao publicar na fila SQS: " + e.awsErrorDetails().errorMessage(), e);
        }
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
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao serializar ComandoContaRequest", e);
        }
    }
}
