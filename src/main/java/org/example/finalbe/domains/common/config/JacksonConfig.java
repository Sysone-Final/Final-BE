package org.example.finalbe.domains.common.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

@Configuration
public class JacksonConfig {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        // 커스텀 Instant Serializer
        SimpleModule instantModule = new SimpleModule();
        instantModule.addSerializer(Instant.class, new JsonSerializer<Instant>() {
            @Override
            public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers)
                    throws IOException {
                if (value == null) {
                    gen.writeNull();
                } else {
                    ZonedDateTime seoulTime = value.atZone(SEOUL_ZONE);
                    gen.writeString(seoulTime.format(FORMATTER));
                }
            }
        });

        return Jackson2ObjectMapperBuilder.json()
                .timeZone(TimeZone.getTimeZone("Asia/Seoul"))
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .modules(new JavaTimeModule(), instantModule)
                .build();
    }
}