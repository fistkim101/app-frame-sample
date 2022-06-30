package com.fistkim.cachesupport.support;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import lombok.SneakyThrows;

import java.lang.reflect.Method;

public class EnumDeserializer<T> extends StdDeserializer<T> {

    public EnumDeserializer() {
        this(null);
    }

    protected EnumDeserializer(Class<?> vc) {
        super(vc);
    }

    @SneakyThrows
    @Override
    public T deserialize(JsonParser jp, DeserializationContext context) {
        JsonNode node = jp.getCodec().readTree(jp);
        String code = node.get("code").asText();
        String name = node.get("name").asText();

        if (code == null || name == null) {
            return null;
        }

        String ENUM_FIND_METHOD_NAME = "getValue";
        Class<T> targetEnumClass = (Class<T>) this.handledType();
        Method method = targetEnumClass.getMethod(ENUM_FIND_METHOD_NAME, String.class, String.class);

        return (T) method.invoke(targetEnumClass, code, name);
    }

}
