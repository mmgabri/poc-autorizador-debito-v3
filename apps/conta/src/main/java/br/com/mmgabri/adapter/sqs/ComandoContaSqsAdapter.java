package br.com.mmgabri.adapter.sqs;

import br.com.mmgabri.domain.ComandoContaRequest;
import br.com.mmgabri.services.ContaService;
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

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final ContaService contaService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExecutorService pollExecutor;
    private ExecutorService messageExecutor;
    private String queueUrl;

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        messageExecutor = Executors.newFixedThreadPool(consumerThreads);
        pollExecutor = Executors.newSingleThreadExecutor();
        pollExecutor.submit(this::pollLoop);
        logger.info("SQS adapter iniciado. queueName={}, consumerThreads={}", queueName, consumerThreads);
    }

    private void pollLoop() {
        while (running.get()) {
            try {
                if (queueUrl == null || queueUrl.isBlank()) {
                    queueUrl = sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build()).queueUrl();
                    logger.info("Fila SQS resolvida---------------. queueUrl={}", queueUrl);
                }

                ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(maxMessages)
                        .waitTimeSeconds(waitTimeSeconds)
                        .visibilityTimeout(visibilityTimeoutSeconds)
                        .messageAttributeNames("instanceId")
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
        String messageId = message.messageId();

        try {
            logger.debug("Mensagem recebida da fila SQS. messageId={} payloadLength={}", messageId, message.body() != null ? message.body().length() : 0);
            ComandoContaRequest body = objectMapper.readValue(message.body(), ComandoContaRequest.class);
            String instanceId = message.messageAttributes().get("instanceId").stringValue();
            ComandoContaRequest request = new ComandoContaRequest(body.correlationId(), instanceId, body.customReturnConta(), body.sleepConta());

            contaService.execute(request);

            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build());

            logger.debug("Mensagem processada com sucesso. messageId={} correlationId={}", messageId, request.correlationId());
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
