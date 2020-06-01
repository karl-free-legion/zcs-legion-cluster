package com.legion.process.conditions;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.legion.process.commons.Buildable;
import com.legion.process.commons.PropertyNames;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents the logical AND of multiple conditions. May be used in a {ChoiceState}.
 *
 * @author lance
 */
@Slf4j
public final class AndCondition implements CombinationCondition {
    @JsonProperty(PropertyNames.AND)
    private final List<Condition> conditions;

    private AndCondition(Builder builder) {
        this.conditions = Buildable.Utils.build(builder.conditions);
    }

    /**
     * @return BuilderAbstract instance to construct a {AndCondition}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return List of conditions contained in the AND expression.
     */
    @Override
    public List<Condition> getConditions() {
        return conditions;
    }

    @Override
    public boolean check(Map<String, Object> params) {
        return conditions.stream().allMatch(c -> c.check(params));
    }

    /**
     * BuilderAbstract for a {@link AndCondition}.
     */
    public static final class Builder implements Condition.Builder {

        private final List<Condition.Builder> conditions = new ArrayList<>();

        private Builder() {
        }

        /**
         * Adds a condition to the AND expression. May be another composite condition or a simple condition.
         *
         * @param conditionBuilder Instance of {@link Builder}.
         *                         Note that the
         *                         {@link Condition} object is not built until the {@link AndCondition} is built so any
         *                         modifications on the state model will be reflected in this object.
         * @return This object for method chaining.
         */
        public Builder condition(Condition.Builder conditionBuilder) {
            this.conditions.add(conditionBuilder);
            return this;
        }

        /**
         * Adds the conditions to the AND expression. May be other composite conditions or simple conditions.
         *
         * @param conditionBuilders Instances of {@link Builder}.
         *                          Note that the
         *                          {@link Condition} object is not built until the {@link AndCondition} is built so any
         *                          modifications on the state model will be reflected in this object.
         * @return This object for method chaining.
         */
        public Builder conditions(Condition.Builder... conditionBuilders) {
            for (Condition.Builder conditionBuilder : conditionBuilders) {
                condition(conditionBuilder);
            }
            return this;
        }

        /**
         * @return An immutable {@link AndCondition} object.
         */
        @Override
        public Condition build() {
            return new AndCondition(this);
        }
    }
}
