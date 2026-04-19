package br.com.mmgabri.adapters.dynamodb.config;

import br.com.mmgabri.adapters.dynamodb.entity.IdempotencyEntity;
import br.com.mmgabri.adapters.dynamodb.entity.ServiceContextEntity;
import br.com.mmgabri.adapters.dynamodb.entity.TransactionContextEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchemaParams;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.lang.invoke.MethodHandles;
import java.time.Duration;

@Configuration
public class DynamoDbConfig {

    @Bean
    public DynamoDbClient dynamoDbClientProd() {
        var httpClient = ApacheHttpClient.builder()
                .maxConnections(200) // ajuste: 200 bom ponto de partida
                .connectionTimeout(Duration.ofSeconds(2))
                .socketTimeout(Duration.ofSeconds(5))
                .connectionAcquisitionTimeout(Duration.ofMillis(300))
                .build();

        var override = ClientOverrideConfiguration.builder()
                .apiCallAttemptTimeout(Duration.ofSeconds(2))
                .apiCallTimeout(Duration.ofSeconds(5))
                //.retryStrategy(r -> r.retryOnException()
                .build();

        return DynamoDbClient.builder()
                .region(Region.US_EAST_1)
                .httpClient(httpClient)
                .overrideConfiguration(override)
                .build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClientProd) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClientProd)
                .build();
    }

    private static final TableSchema<TransactionContextEntity> TRANSACTION_CONTEXT_SCHEMA =
            TableSchema.fromBean(
                    BeanTableSchemaParams.builder(TransactionContextEntity.class)
                            .lookup(MethodHandles.lookup())
                            .build()
            );

    private static final TableSchema<ServiceContextEntity> SERVICE_CONTEXT_SCHEMA =
            TableSchema.fromBean(
                    BeanTableSchemaParams.builder(ServiceContextEntity.class)
                            .lookup(MethodHandles.lookup())
                            .build()
            );

    private static final TableSchema<IdempotencyEntity> IDEMPOTENCY_SCHEMA =
            TableSchema.fromBean(
                    BeanTableSchemaParams.builder(IdempotencyEntity.class)
                            .lookup(MethodHandles.lookup())
                            .build()
            );

    @Bean
    public DynamoDbTable<TransactionContextEntity> transactionContextTable(DynamoDbEnhancedClient enhancedClient) {
        return enhancedClient.table(TransactionContextEntity.TABLE_NAME, TRANSACTION_CONTEXT_SCHEMA);
    }

    @Bean
    public DynamoDbTable<ServiceContextEntity> serviceConextTable(DynamoDbEnhancedClient enhancedClient) {
        return enhancedClient.table(ServiceContextEntity.TABLE_NAME, SERVICE_CONTEXT_SCHEMA);
    }

    @Bean
    public DynamoDbTable<IdempotencyEntity> idempotencyTable(DynamoDbEnhancedClient enhancedClient) {
        return enhancedClient.table(IdempotencyEntity.TABLE_NAME, IDEMPOTENCY_SCHEMA);
    }
}
