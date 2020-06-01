package com.legion.process.conditions;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Base class for Binary String conditions. Handles marshalling a {@link JsonNode} into String.
 */
abstract class AbstractBinaryStringConditionBuilder extends AbstractBinaryConditionBuilder {

    public abstract AbstractBinaryStringConditionBuilder expectedValue(String expectedValue);

    @Override
    public final AbstractBinaryStringConditionBuilder expectedValue(JsonNode expectedValue) {
        expectedValue(expectedValue.asText());
        return this;
    }
}
