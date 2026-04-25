package br.com.mmgabri.adapters.sqs;

import br.com.mmgabri.domains.FormatadorRequest;
import br.com.mmgabri.services.ReversalService;
import com.fasterxml.jackson.core.JsonProcessingException;
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
public class ReversalSqsAdapter implements ReversalService {

    private static final Logger logger = LoggerFactory.getLogger(ReversalSqsAdapter.class);

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.reversal-queue-name}")
    private String reversalQueueName;

    private volatile String queueUrl;

    @Override
    public void publish(FormatadorRequest request) {
        try {
            String payload = toJson(request.getMessageIso());
            String url = resolveQueueUrl();

            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(url)
                    .messageBody(payload)
                    .build());

            logger.info("Reversal publicado na fila SQS. mti={} transactionIdReversal={}",
                    request.getMessageIso().get("mti"), request.getTransactionIdReversal());

        } catch (QueueDoesNotExistException e) {
            logger.error("Fila SQS de reversal não encontrada. queueName={}", reversalQueueName, e);
            queueUrl = null;
            throw new IllegalStateException("Fila SQS de reversal não encontrada: " + reversalQueueName, e);
        } catch (SqsException e) {
            logger.error("Erro ao publicar reversal na fila SQS. queueName={} statusCode={}", reversalQueueName, e.statusCode(), e);
            throw new IllegalStateException("Erro ao publicar reversal na fila SQS: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    private String resolveQueueUrl() {
        if (queueUrl == null) {
            synchronized (this) {
                if (queueUrl == null) {
                    try {
                        queueUrl = sqsClient.getQueueUrl(
                                GetQueueUrlRequest.builder().queueName(reversalQueueName).build()
                        ).queueUrl();
                        logger.info("Fila SQS de reversal resolvida. queueUrl={}", queueUrl);
                    } catch (QueueDoesNotExistException e) {
                        logger.error("Fila SQS de reversal não encontrada. queueName={}", reversalQueueName, e);
                        throw new IllegalStateException("Fila SQS de reversal não encontrada: " + reversalQueueName, e);
                    }
                }
            }
        }
        return queueUrl;
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Erro ao serializar payload do reversal", e);
        }
    }
}