package com.legion.process.conditions;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.legion.process.commons.PropertyNames;

import java.util.Map;

/**
 * Binary condition for Boolean equality comparison.
 */
public final class BooleanEqualsCondition implements BinaryCondition<Boolean> {

    @JsonProperty(PropertyNames.VARIABLE)
    private final String variable;

    @JsonProperty(PropertyNames.BOOLEAN_EQUALS)
    private final Boolean expectedValue;

    private BooleanEqualsCondition(BuilderAbstract builder) {
        this.variable = builder.variable;
        this.expectedValue = builder.expectedValue;
    }

    /**
     * @return BuilderAbstract instance to construct a {@link BooleanEqualsCondition}.
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
    public Boolean getExpectedValue() {
        return expectedValue;
    }

    @Override
    public boolean check(Map<String, Object> params) {
        return Boolean.valueOf(params.get(variable).toString()).equals(getExpectedValue());
    }

    /**
     * BuilderAbstract for a {@link BooleanEqualsCondition}.
     */
    public static final class BuilderAbstract extends AbstractBinaryConditionBuilder {

        @JsonProperty(PropertyNames.VARIABLE)
        private String variable;

        @JsonProperty(PropertyNames.BOOLEAN_EQUALS)
        private Boolean expectedValue;

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
        public BuilderAbstract expectedValue(boolean expectedValue) {
            this.expectedValue = expectedValue;
            return this;
        }

        @Override
        public AbstractBinaryConditionBuilder expectedValue(JsonNode expectedValue) {
            return expectedValue(expectedValue.booleanValue());
        }

        @Override
        public String type() {
            return PropertyNames.BOOLEAN_EQUALS;
        }

        /**
         * @return An immutable {@link BooleanEqualsCondition} object.
         */
        @Override
        public BooleanEqualsCondition build() {
            return new BooleanEqualsCondition(this);
        }
    }
}
