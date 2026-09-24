package io.github.yilers.web.jackson;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JacksonConfigTest {

    private final JsonMapper jsonMapper = createJsonMapper();

    @Test
    void serializesLocalDateTimeWithUnifiedFormat() {
        LocalDateTime value = LocalDateTime.of(2026, 9, 24, 10, 27, 12);

        assertEquals("\"2026-09-24 10:27:12\"", jsonMapper.writeValueAsString(value));
    }

    @Test
    void deserializesLocalDateTimeWithUnifiedFormat() {
        LocalDateTime value = jsonMapper.readValue("\"2026-09-24 10:27:12\"", LocalDateTime.class);

        assertEquals(LocalDateTime.of(2026, 9, 24, 10, 27, 12), value);
    }

    @Test
    void keepsLongSerializationAsString() {
        assertEquals("\"9007199254740993\"", jsonMapper.writeValueAsString(9_007_199_254_740_993L));
    }

    private static JsonMapper createJsonMapper() {
        JsonMapper.Builder builder = JsonMapper.builder();
        new JacksonConfig().jsonMapperBuilderCustomizer().customize(builder);
        return builder.build();
    }
}
