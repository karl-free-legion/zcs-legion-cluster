package com.legion.process.conditions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.legion.process.commons.PropertyNames;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Map;
import java.util.Objects;

/**
 * Binary condition for Numeric greater then or equal to comparison. Supports both integral and floating point numeric types.
 */
public final class NumericGreaterThanOrEqualCondition implements BinaryCondition<String> {

    @JsonProperty(PropertyNames.VARIABLE)
    private final String variable;

    @JsonProperty(PropertyNames.NUMERIC_GREATER_THAN_EQUALS)
    private final NumericNode expectedValue;

    private NumericGreaterThanOrEqualCondition(BuilderAbstract builder) {
        this.variable = builder.variable;
        this.expectedValue = builder.expectedValue;
    }

    /**
     * @return BuilderAbstract instance to construct a {@link NumericGreaterThanOrEqualCondition}.
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
    @JsonIgnore
    @Override
    public String getExpectedValue() {
        return expectedValue.asText();
    }

    @Override
    public boolean check(Map<String, Object> params) {
        String exp = getExpectedValue();
        Object fact = params.get(variable);
        if (NumberUtils.isCreatable(exp) && Objects.nonNull(fact)) {
            return Double.valueOf(params.get(variable).toString()) >= Double.valueOf(exp);
        }
        return false;
    }

    /**
     * BuilderAbstract for a {@link NumericGreaterThanOrEqualCondition}.
     */
    public static final class BuilderAbstract extends AbstractBinaryConditionBuilder {

        @JsonProperty(PropertyNames.VARIABLE)
        private String variable;

        @JsonProperty(PropertyNames.NUMERIC_GREATER_THAN_EQUALS)
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
        public AbstractBinaryConditionBuilder expectedValue(JsonNode expectedValue) {
            this.expectedValue = (NumericNode) expectedValue;
            return this;
        }

        @Override
        public String type() {
            return PropertyNames.NUMERIC_GREATER_THAN_EQUALS;
        }

        /**
         * @return An immutable {@link NumericGreaterThanOrEqualCondition} object.
         */
        @Override
        public NumericGreaterThanOrEqualCondition build() {
            return new NumericGreaterThanOrEqualCondition(this);
        }
    }
}
