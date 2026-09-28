package br.com.mmgabri.awstest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;

import java.util.ArrayList;
import java.util.List;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

/**
 * Replaces the real DynamoDbClient/SqsClient beans (DynamoDbConfig/SqsConfig, in main)
 * with versions pointing to a single LocalStack container, marked @Primary to win
 * resolution by type. Repositories/ReversalNotificationSqsAdapter do not change - they
 * still hit real DynamoDB/SQS, just local.
 *
 * Tables/queue are not created by the application (see DynamoDbConfig/SqsConfig in
 * main - no CreateTable/CreateQueue there, infra is assumed provisioned), so this
 * config creates the minimum schema itself before exposing the beans.
 *
 * withReuse(true) keeps the container up after the test ends (requires
 * testcontainers.reuse.enable=true in ~/.testcontainers.properties), so the tables
 * can be queried with the AWS CLI afterwards. That is why there is no destroyMethod
 * here - stopping the container manually would defeat the purpose of reuse.
 */
@TestConfiguration
public class LocalStackTestConfig {

    private static final Logger logger = LoggerFactory.getLogger(LocalStackTestConfig.class);
    private static final String QUEUE_COMPENSATION_TRANSACTION = "queue-compensation-transaction";

    @Bean
    public LocalStackContainer localStackContainer() {
        LocalStackContainer container = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.8"))
                .withServices(DYNAMODB, SQS)
                .withReuse(true);
        container.start();
        logger.info("LocalStack up (reuse=true) - DynamoDB/SQS endpoint: {}", container.getEndpointOverride(DYNAMODB));
        return container;
    }

    @Bean
    @Primary
    public DynamoDbClient localStackDynamoDbClient(LocalStackContainer localStackContainer) {
        DynamoDbClient client = DynamoDbClient.builder()
                .endpointOverride(localStackContainer.getEndpointOverride(DYNAMODB))
                .region(Region.of(localStackContainer.getRegion()))
                .credentialsProvider(testCredentials(localStackContainer))
                .build();

        createTable(client, "idempotency", "correlationId", null);
        createTable(client, "transaction_context", "transactionId", null);
        createTable(client, "service_context", "transactionId", "serviceName");

        return client;
    }

    @Bean
    @Primary
    public SqsClient localStackSqsClient(LocalStackContainer localStackContainer) {
        SqsClient client = SqsClient.builder()
                .endpointOverride(localStackContainer.getEndpointOverride(SQS))
                .region(Region.of(localStackContainer.getRegion()))
                .credentialsProvider(testCredentials(localStackContainer))
                .build();

        client.createQueue(CreateQueueRequest.builder().queueName(QUEUE_COMPENSATION_TRANSACTION).build());

        return client;
    }

    private StaticCredentialsProvider testCredentials(LocalStackContainer localStackContainer) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(localStackContainer.getAccessKey(), localStackContainer.getSecretKey()));
    }

    private void createTable(DynamoDbClient client, String tableName, String partitionKey, String sortKey) {
        List<KeySchemaElement> keySchema = new ArrayList<>();
        List<AttributeDefinition> attributeDefinitions = new ArrayList<>();

        keySchema.add(KeySchemaElement.builder().attributeName(partitionKey).keyType(KeyType.HASH).build());
        attributeDefinitions.add(AttributeDefinition.builder()
                .attributeName(partitionKey)
                .attributeType(ScalarAttributeType.S)
                .build());

        if (sortKey != null) {
            keySchema.add(KeySchemaElement.builder().attributeName(sortKey).keyType(KeyType.RANGE).build());
            attributeDefinitions.add(AttributeDefinition.builder()
                    .attributeName(sortKey)
                    .attributeType(ScalarAttributeType.S)
                    .build());
        }

        try {
            client.createTable(CreateTableRequest.builder()
                    .tableName(tableName)
                    .keySchema(keySchema)
                    .attributeDefinitions(attributeDefinitions)
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build());
        } catch (ResourceInUseException e) {
            // Container reused (withReuse) from a previous run - table already exists.
            logger.debug("Table {} already exists (reused container)", tableName);
        }
    }
}
