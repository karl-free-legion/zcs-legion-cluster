package com.legion.process.conditions;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.legion.process.commons.PropertyNames;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Objects;

/**
 * Binary condition for String equality comparison.
 *
 * @author lance
 */
@Slf4j
@Getter
public final class StringNotEqualsCondition implements BinaryCondition<String> {
    @JsonProperty(PropertyNames.VARIABLE)
    private final String variable;

    @JsonProperty(PropertyNames.STRING_NOT_EQUALS)
    private final String expectedValue;

    private StringNotEqualsCondition(BuilderAbstract builder) {
        this.variable = builder.variable;
        this.expectedValue = builder.expectedValue;
    }

    /**
     * @return BuilderAbstract instance to construct a StringNotEqualsCondition
     */
    public static BuilderAbstract builder() {
        return new BuilderAbstract();
    }

    @Override
    public boolean check(Map<String, Object> params) {
        String exp = getExpectedValue();
        Object fact = params.get(variable);
        if (Objects.nonNull(fact) && StringUtils.isNoneBlank(fact.toString(), exp)) {
            return !StringUtils.equals(fact.toString(), exp);
        }
        return false;
    }

    /**
     * BuilderAbstract for a {StringNotEqualsCondition}.
     */
    public static final class BuilderAbstract extends AbstractBinaryStringConditionBuilder {

        @JsonProperty(PropertyNames.VARIABLE)
        private String variable;

        @JsonProperty(PropertyNames.STRING_NOT_EQUALS)
        private String expectedValue;

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
        @Override
        public BuilderAbstract expectedValue(String expectedValue) {
            this.expectedValue = expectedValue;
            return this;
        }

        @Override
        public String type() {
            return PropertyNames.STRING_NOT_EQUALS;
        }

        /**
         * @return An immutable {StringNotEqualsCondition} object.
         */
        @Override
        public StringNotEqualsCondition build() {
            return new StringNotEqualsCondition(this);
        }
    }
}
