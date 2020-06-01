package com.legion.process.conditions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * BuilderAbstract abstraction for binary conditions used in {@link com.fasterxml.jackson.databind.deser.ContextualDeserializer}
 */
public abstract class AbstractBinaryConditionBuilder implements Condition.Builder {

    /**
     * AbstractBinaryConditionBuilder
     *
     * @param variable variable
     * @return AbstractBinaryConditionBuilder
     */
    public abstract AbstractBinaryConditionBuilder variable(String variable);

    /**
     * Internal API to set the expected value of a condition from a JSON document. Subclass handles marshalling to appropriate
     * type.
     *
     * @param expectedValue JSON document representing the expected value.
     * @return This object for method chaining.
     */
    public abstract AbstractBinaryConditionBuilder expectedValue(JsonNode expectedValue);

    /**
     * Type identifier for condition. Used as field name for the expected value.
     *
     * @return type
     */
    @JsonIgnore
    public abstract String type();
}
