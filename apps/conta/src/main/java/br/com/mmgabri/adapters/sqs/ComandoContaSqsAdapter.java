package br.com.mmgabri.adapters.sqs;

import br.com.mmgabri.domain.ComandoContaRequest;
import br.com.mmgabri.services.ContaService;
import br.com.mmgabri.services.MetricsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@ConditionalOnProperty(prefix = "aws.sqs", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class ComandoContaSqsAdapter implements SmartLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(ComandoContaSqsAdapter.class);

    @Value("${aws.sqs.enabled}")
    private boolean enabled;

    @Value("${aws.sqs.queue-name}")
    private String queueName;

    @Value("${aws.sqs.max-messages}")
    private int maxMessages;

    @Value("${aws.sqs.wait-time-seconds}")
    private int waitTimeSeconds;

    @Value("${aws.sqs.visibility-timeout-seconds}")
    private int visibilityTimeoutSeconds;

    @Value("${aws.sqs.error-backoff-millis}")
    private long errorBackoffMillis;

    @Value("${aws.sqs.consumer-threads:10}")
    private int consumerThreads;

    @Value("${aws.sqs.poll-threads:5}")
    private int pollThreads;

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final ContaService contaService;
    private final MetricsService metricsService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExecutorService pollExecutor;
    private ExecutorService messageExecutor;
    private volatile String queueUrl;

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        queueUrl = sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build()).queueUrl();
        messageExecutor = Executors.newVirtualThreadPerTaskExecutor();
        pollExecutor = Executors.newVirtualThreadPerTaskExecutor();
        for (int i = 0; i < pollThreads; i++) {
            pollExecutor.submit(this::pollLoop);
        }
        logger.info("SQS adapter iniciado. queueName={}, pollThreads={}, consumerThreads={}", queueName, pollThreads, consumerThreads);
    }

    private void pollLoop() {
        while (running.get()) {
            try {
                ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(maxMessages)
                        .waitTimeSeconds(waitTimeSeconds)
                        .visibilityTimeout(visibilityTimeoutSeconds)
                        .build();

                List<Message> messages = sqsClient.receiveMessage(request).messages();
                for (Message message : messages) {
                    messageExecutor.submit(() -> processMessage(message));
                }
            } catch (Exception e) {
                logger.error("Erro no polling da fila {}", queueName, e);
                sleep(errorBackoffMillis);
            }
        }
    }

    private void processMessage(Message message) {
        var startTime = OffsetDateTime.now();
        String messageId = message.messageId();

        try {
            metricsService.incrementMetricCounter("app_conta_msg_received_ledger");
            ComandoContaRequest request = objectMapper.readValue(message.body(), ComandoContaRequest.class);
            logger.debug("Mensagem recebida da fila SQS. messageId={} correlationId={}", messageId, request.correlationId());

            contaService.execute(request);

            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build());

            metricsService.incrementMetric("app_conta_duration_transaction", startTime, "method:processMessage");
            logger.debug("Mensagem processada com sucesso em {} ms | correlationId={}", Duration.between(startTime, OffsetDateTime.now()).toMillis(), request.correlationId());
        } catch (Exception e) {
            logger.error("Falha ao processar mensagem SQS. messageId={}", messageId, e);
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        if (pollExecutor != null) pollExecutor.shutdownNow();
        if (messageExecutor != null) messageExecutor.shutdownNow();

        logger.info("SQS adapter finalizado. queueName={}", queueName);
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }
}
