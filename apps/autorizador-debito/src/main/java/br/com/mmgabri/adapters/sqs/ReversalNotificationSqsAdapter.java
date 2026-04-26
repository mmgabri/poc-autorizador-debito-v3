package br.com.mmgabri.adapters.sqs;

import br.com.mmgabri.application.services.CompensationTransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
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
public class ReversalNotificationSqsAdapter implements CompensationTransactionService {

    private static final Logger logger = LoggerFactory.getLogger(ReversalNotificationSqsAdapter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final SqsClient sqsClient;

    @Value("${aws.sqs.compensation-transaction-queue-name}")
    private String queueName;

    private volatile String queueUrl;

    @Override
    @SneakyThrows
    public void publish(String transactionId) {
        try {

            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(resolveQueueUrl())
                    .messageBody(transactionId)
                    .build());

            logger.debug("Compensation transaction published to SQS. transactionId={}", transactionId);
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
                    try {
                        queueUrl = sqsClient.getQueueUrl(
                                GetQueueUrlRequest.builder().queueName(queueName).build()
                        ).queueUrl();
                        logger.debug("Fila SQS resolvida. queueUrl={}", queueUrl);
                    } catch (QueueDoesNotExistException e) {
                        logger.error("Fila SQS não encontrada. queueName={}", queueName, e);
                        throw new IllegalStateException("Fila SQS não encontrada: " + queueName, e);
                    }
                }
            }
        }
        return queueUrl;
    }
}