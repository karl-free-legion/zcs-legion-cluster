package com.legion.process.conditions;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.legion.process.commons.PropertyNames;

import java.util.Map;

/**
 * Represents the logical NOT of a single condition. May be used in a { ChoiceState}.
 **/
public final class NotCondition implements Condition {

    @JsonProperty(PropertyNames.NOT)
    private final Condition condition;

    private NotCondition(Builder builder) {
        this.condition = builder.condition.build();
    }

    /**
     * @return BuilderAbstract instance to construct a {@link NotCondition}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return The condition being negated.
     */
    public Condition getCondition() {
        return condition;
    }

    @Override
    public boolean check(Map<String, Object> params) {
        return !condition.check(params);
    }

    /**
     * BuilderAbstract for a {@link NotCondition}.
     */
    public static final class Builder implements Condition.Builder {

        private Condition.Builder condition = NULL_BUILDER;

        private Builder() {
        }

        /**
         * Sets the condition to be negated. May be another composite condition or a simple condition.
         *
         * @param conditionBuilder Instance of {@link Condition.Builder}. Note that the {@link Condition} object is not built
         *                         until the {@link NotCondition} is built so any modifications on the state model will be reflected in this object.
         * @return This object for method chaining.
         */
        public Builder condition(Condition.Builder conditionBuilder) {
            this.condition = conditionBuilder;
            return this;
        }

        /**
         * @return An immutable {@link NotCondition} object.
         */
        @Override
        public Condition build() {
            return new NotCondition(this);
        }
    }
}
