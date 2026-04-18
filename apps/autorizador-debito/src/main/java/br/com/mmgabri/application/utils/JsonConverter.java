package br.com.mmgabri.application.utils;

import br.com.mmgabri.application.domains.Payload;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JsonConverter {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(JsonConverter.class);


    @SneakyThrows
    public Payload jsonToObj(String json) {
        return objectMapper.readValue(json, new TypeReference<Payload>() {
        });
    }

    @SneakyThrows
    public String objToJson(Payload obj) {
        return objectMapper.writeValueAsString(obj);
    }
}
