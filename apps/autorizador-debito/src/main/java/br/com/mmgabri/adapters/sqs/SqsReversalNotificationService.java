package br.com.mmgabri.adapters.sqs;

import br.com.mmgabri.application.domains.Payload;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SqsReversalNotificationService {
    private static final Logger logger = LoggerFactory.getLogger(SqsReversalNotificationService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final SqsClient sqsClient;

    @Value("${aws.sqs.reversal-notification-queue-url}")
    private String queueUrl;

    @SneakyThrows
    public void publish(Payload payload, boolean limitApproved, boolean ledgerApproved, boolean antiFraudApproved) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("transactionId", payload.getHeaderMessage().getTransactionId());
            message.put("correlationId", payload.getHeaderMessage().getCorrelationId());
            message.put("contaId", payload.getDataEnrichment().getConta().getContaId());
            message.put("valor", payload.getMessageIso().get("004"));
            message.put("limitApproved", limitApproved);
            message.put("ledgerApproved", ledgerApproved);
            message.put("antiFraudApproved", antiFraudApproved);

            String body = objectMapper.writeValueAsString(message);

            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(body)
                    .build());

            logger.info("Reversal notification published to SQS. transactionId={}", payload.getHeaderMessage().getTransactionId());
        } catch (Exception e) {
            logger.error("Failed to publish reversal notification to SQS. transactionId={}",
                    payload.getHeaderMessage().getTransactionId(), e);
        }
    }
}
