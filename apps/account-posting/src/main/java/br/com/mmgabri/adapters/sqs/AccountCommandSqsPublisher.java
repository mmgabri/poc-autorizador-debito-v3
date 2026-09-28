package br.com.mmgabri.adapters.sqs;

import br.com.mmgabri.domain.AccountCommand;
import br.com.mmgabri.services.MetricsService;
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

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class AccountCommandSqsPublisher {

    private static final Logger logger = LoggerFactory.getLogger(AccountCommandSqsPublisher.class);

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final MetricsService metricsService;

    @Value("${aws.sqs.account-command-queue-name}")
    private String queueName;

    private volatile String queueUrl;

    public void publish(AccountCommand command) {
        var startTime = OffsetDateTime.now();
        try {
            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(resolveQueueUrl())
                    .messageBody(toJson(command))
                    .build());

            metricsService.incrementMetric("app_ledger_duration_publish_sqs", startTime, "instanceId:" + command.instanceId());
            metricsService.incrementMetricCounter("app_ledger_msg_send_conta");
            logger.debug("Command published to SQS for conta. correlationId={}", command.correlationId());
        } catch (QueueDoesNotExistException e) {
            logger.error("SQS queue not found. queueName={}", queueName, e);
            queueUrl = null;
            throw new IllegalStateException("SQS queue not found: " + queueName, e);
        } catch (SqsException e) {
            logger.error("Failed to publish to SQS queue. queueName={} statusCode={}", queueName, e.statusCode(), e);
            throw new IllegalStateException("Failed to publish to SQS queue: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    private String resolveQueueUrl() {
        if (queueUrl == null) {
            synchronized (this) {
                if (queueUrl == null) {
                    queueUrl = sqsClient.getQueueUrl(
                            GetQueueUrlRequest.builder().queueName(queueName).build()
                    ).queueUrl();
                    logger.debug("SQS queue resolved. queueUrl={}", queueUrl);
                }
            }
        }
        return queueUrl;
    }

    private String toJson(AccountCommand command) {
        try {
            return objectMapper.writeValueAsString(command);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize AccountCommand", e);
        }
    }
}
