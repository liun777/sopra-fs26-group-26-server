package ch.uzh.ifi.hase.soprafs26.config;

import org.hibernate.type.format.AbstractJsonFormatMapper;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Type;

// Hibernate 7.1 expects jackson 2 classes for its built-in mapper.
// This mapper bridges Hibernate JSON serialization to Jackson 3 (tools.jackson) used by Spring Boot 4.
public class ToolsJacksonJsonFormatMapper extends AbstractJsonFormatMapper {

    private final ObjectMapper objectMapper;

    public ToolsJacksonJsonFormatMapper() {
        this(new ObjectMapper());
    }

    public ToolsJacksonJsonFormatMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> T fromString(CharSequence charSequence, Type type) {
        try {
            return objectMapper.readValue(charSequence.toString(), objectMapper.constructType(type));
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not deserialize JSON value", e);
        }
    }

    @Override
    public <T> String toString(T value, Type type) {
        try {
            return objectMapper.writerFor(objectMapper.constructType(type)).writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not serialize JSON value", e);
        }
    }
}
