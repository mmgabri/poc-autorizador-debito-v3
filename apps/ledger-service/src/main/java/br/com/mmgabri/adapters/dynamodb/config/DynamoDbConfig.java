package br.com.mmgabri.adapters.dynamodb.config;

import br.com.mmgabri.adapters.dynamodb.entity.ComandoContaEntity;
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
    public DynamoDbClient dynamoDbClient() {
        var httpClient = ApacheHttpClient.builder()
                .maxConnections(200)
                .connectionTimeout(Duration.ofSeconds(2))
                .socketTimeout(Duration.ofSeconds(5))
                .connectionAcquisitionTimeout(Duration.ofSeconds(1))
                .build();

        var override = ClientOverrideConfiguration.builder()
                .apiCallAttemptTimeout(Duration.ofSeconds(2))
                .apiCallTimeout(Duration.ofSeconds(5))
                .build();

        return DynamoDbClient.builder()
                .region(Region.US_EAST_1)
                .httpClient(httpClient)
                .overrideConfiguration(override)
                .build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    private static final TableSchema<ComandoContaEntity> COMANDO_CONTA_SCHEMA =
            TableSchema.fromBean(
                    BeanTableSchemaParams.builder(ComandoContaEntity.class)
                            .lookup(MethodHandles.lookup())
                            .build()
            );

    @Bean
    public DynamoDbTable<ComandoContaEntity> comandoContaTable(DynamoDbEnhancedClient enhancedClient) {
        return enhancedClient.table(ComandoContaEntity.TABLE_NAME, COMANDO_CONTA_SCHEMA);
    }
}
