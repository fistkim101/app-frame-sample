package com.fistkim.cachesupport.support;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Component
public class ObjectMapperFactory {

    private ObjectMapper objectMapper;
    private final String DEFAULT_DATE_FORMAT_STR = "yyyy-MM-dd";
    private final DateTimeFormatter DEFAULT_DATE_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT_STR);
    private final String ONLY_DIGIT_DATE_FORMAT_STR = "yyyyMMdd";
    private final DateTimeFormatter ONLY_DIGIT_DATE_FORMATTER = DateTimeFormatter.ofPattern(ONLY_DIGIT_DATE_FORMAT_STR);
    private final String DEFAULT_DATE_TIME_FORMAT_STR = "yyyy-MM-dd HH:mm:ss";
    private final DateTimeFormatter DEFAULT_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT_STR);
    private final String TIME_FORMAT_STR = "HH:mm:ss";
    private final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(TIME_FORMAT_STR);

    public ObjectMapper getObjectMapper() {
        if (this.objectMapper != null) {
            return this.objectMapper;
        }

        this.objectMapper = new ObjectMapper();
        this.registerTimeModule();
        this.registerDeserializer();
        return this.objectMapper;
    }

    public ObjectMapper getObjectMapper(JsonFactory jsonFactory) {
        if (this.objectMapper != null) {
            return this.objectMapper;
        }

        this.objectMapper = new ObjectMapper(jsonFactory);
        this.registerTimeModule();
        this.registerDeserializer();
        return this.objectMapper;
    }

    private void registerTimeModule() {
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(DEFAULT_DATE_FORMATTER));
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DEFAULT_DATE_FORMATTER));
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DEFAULT_DATE_TIME_FORMATTER));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DEFAULT_DATE_TIME_FORMATTER));
        javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(TIME_FORMATTER));
        javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ISO_LOCAL_TIME));

        this.objectMapper.registerModule(javaTimeModule);
    }

    private void registerDeserializer() {
        SimpleModule module = new SimpleModule();
        // module.addDeserializer(classType, Deserializer);
        this.objectMapper.registerModule(module);
    }

}
