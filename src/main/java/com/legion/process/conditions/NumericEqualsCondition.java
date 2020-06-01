package com.legion.process.conditions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.legion.process.commons.PropertyNames;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Map;
import java.util.Objects;

/**
 * Binary condition for Numeric equality comparison. Supports both integral and floating point numeric types.
 */
@Slf4j
public final class NumericEqualsCondition implements BinaryCondition<String> {

    @JsonProperty(PropertyNames.VARIABLE)
    private final String variable;

    @JsonProperty(PropertyNames.NUMERIC_EQUALS)
    private final NumericNode expectedValue;

    private NumericEqualsCondition(BuilderAbstract builder) {
        this.variable = builder.variable;
        this.expectedValue = builder.expectedValue;
    }

    /**
     * @return BuilderAbstract instance to construct a {@link NumericEqualsCondition}.
     */
    public static BuilderAbstract builder() {
        return new BuilderAbstract();
    }

    /**
     * @return The JSONPath expression that determines which piece of the input document is used for the comparison.
     */
    @Override
    public String getVariable() {
        return variable;
    }

    /**
     * @return The expected value for this condition.
     */
    @Override
    @JsonIgnore
    public String getExpectedValue() {
        return expectedValue.asText();
    }

    @Override
    public boolean check(Map<String, Object> params) {
        String exp = getExpectedValue();
        Object fact = params.get(variable);
        if (NumberUtils.isCreatable(exp) && Objects.nonNull(fact)) {
            return Double.valueOf(exp) - Double.valueOf(params.get(variable).toString()) == 0;
        }
        return false;
    }

    /**
     * BuilderAbstract for a {@link NumericEqualsCondition}.
     */
    public static final class BuilderAbstract extends AbstractBinaryConditionBuilder {

        @JsonProperty(PropertyNames.VARIABLE)
        private String variable;

        @JsonProperty(PropertyNames.NUMERIC_EQUALS)
        private NumericNode expectedValue;

        private BuilderAbstract() {
        }

        /**
         * Sets the JSONPath expression that determines which piece of the input document is used for the comparison.
         *
         * @param variable Reference path.
         * @return This object for method chaining.
         */
        @Override
        public BuilderAbstract variable(String variable) {
            this.variable = variable;
            return this;
        }

        /**
         * Sets the expected value for this condition.
         *
         * @param expectedValue Expected value.
         * @return This object for method chaining.
         */
        public BuilderAbstract expectedValue(long expectedValue) {
            this.expectedValue = new LongNode(expectedValue);
            return this;
        }

        /**
         * Sets the expected value for this condition.
         *
         * @param expectedValue Expected value.
         * @return This object for method chaining.
         */
        public BuilderAbstract expectedValue(double expectedValue) {
            this.expectedValue = new DoubleNode(expectedValue);
            return this;
        }

        @Override
        public BuilderAbstract expectedValue(JsonNode expectedValue) {
            // TODO handle not numeric
            this.expectedValue = (NumericNode) expectedValue;
            return this;
        }

        @Override
        public String type() {
            return PropertyNames.NUMERIC_EQUALS;
        }

        /**
         * @return An immutable {@link NumericEqualsCondition} object.
         */
        @Override
        public NumericEqualsCondition build() {
            return new NumericEqualsCondition(this);
        }
    }
}
