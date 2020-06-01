package com.legion.process.commons;

import com.fasterxml.jackson.databind.JsonNode;
import com.legion.process.conditions.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

/**
 * Custom deserializer for a {@link Condition}.
 */
public class ConditionDeserializer {

    public Condition.Builder deserializeCondition(JsonNode node) {
        if (node.has(PropertyNames.VARIABLE)) {
            if (node.has(PropertyNames.STRING_EQUALS)) {
                return deserializeBinaryCondition(StringEqualsCondition.builder(), node);
            } else if (node.has(PropertyNames.STRING_NOT_EQUALS)) {
                return deserializeBinaryCondition(StringNotEqualsCondition.builder(), node);
            } else if (node.has(PropertyNames.NUMERIC_EQUALS)) {
                return deserializeBinaryCondition(NumericEqualsCondition.builder(), node);
            } else if (node.has(PropertyNames.NUMERIC_GREATER_THAN)) {
                return deserializeBinaryCondition(NumericGreaterThanCondition.builder(), node);
            } else if (node.has(PropertyNames.NUMERIC_GREATER_THAN_EQUALS)) {
                return deserializeBinaryCondition(NumericGreaterThanOrEqualCondition.builder(), node);
            } else if (node.has(PropertyNames.NUMERIC_LESS_THAN)) {
                return deserializeBinaryCondition(NumericLessThanCondition.builder(), node);
            } else if (node.has(PropertyNames.NUMERIC_LESS_THAN_EQUALS)) {
                return deserializeBinaryCondition(NumericLessThanOrEqualCondition.builder(), node);
            } else if (node.has(PropertyNames.BOOLEAN_EQUALS)) {
                return deserializeBinaryCondition(BooleanEqualsCondition.builder(), node);
            }
        } else if (node.has(PropertyNames.AND)) {
            AndCondition.Builder builder = AndCondition.builder();
            for (JsonNode inner : node.get(PropertyNames.AND)) {
                builder.condition(deserializeCondition(inner));
            }
            return builder;
        } else if (node.has((PropertyNames.OR))) {
            OrCondition.Builder builder = OrCondition.builder();
            for (JsonNode inner : node.get(PropertyNames.OR)) {
                builder.condition(deserializeCondition(inner));
            }
            return builder;
        } else if (node.has((PropertyNames.NOT))) {
            return NotCondition.builder()
                    .condition(deserializeCondition(node.get(PropertyNames.NOT)));
        }
        throw new RuntimeException("Condition must be provided");
    }

    private Condition.Builder deserializeBinaryCondition(
            AbstractBinaryConditionBuilder builder,
            JsonNode node) {
        return builder
                .variable(node.get(PropertyNames.VARIABLE).asText())
                .expectedValue(node.get(builder.type()));
    }

    public static String json(String jsonName) {
        Path path = Paths.get("src/test/resources/state_machines/" + jsonName + ".json");
        try {
            return Files.readAllLines(path, Charset.forName("utf-8")).stream().map(String::trim).collect(Collectors.joining());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
