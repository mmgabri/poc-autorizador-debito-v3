package br.com.mmgabri.services;

import br.com.mmgabri.domains.ReversalRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Service
@RequiredArgsConstructor
public class ReversalService {

    private static final Logger logger = LoggerFactory.getLogger(ReversalService.class);

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.reversal-queue-name}")
    private String reversalQueueName;

    private volatile String queueUrl;

    public void publishReversal(ReversalRequest request) {
        String payload = toJson(request);
        String url = resolveQueueUrl();

        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(url)
                .messageBody(payload)
                .build());

        logger.info("Reversal publicado na fila SQS. transactionId={}", request.getTransactionId());
    }

    private String resolveQueueUrl() {
        if (queueUrl == null) {
            synchronized (this) {
                if (queueUrl == null) {
                    queueUrl = sqsClient.getQueueUrl(
                            GetQueueUrlRequest.builder().queueName(reversalQueueName).build()
                    ).queueUrl();
                    logger.info("Fila SQS de reversal resolvida. queueUrl={}", queueUrl);
                }
            }
        }
        return queueUrl;
    }

    private String toJson(ReversalRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Erro ao serializar ReversalRequest", e);
        }
    }
}
