package com.legion.process.transitions;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.legion.process.commons.PropertyNames;

/**
 * Non-terminal transition to another state in the state machine.
 *
 * @author lance
 */
public final class NextStateTransition implements Transition {

    @JsonProperty(PropertyNames.NEXT)
    private final String nextStateName;

    private NextStateTransition(Builder builder) {
        this.nextStateName = builder.nextStateName;
    }

    /**
     * @return BuilderAbstract instance to construct a {@link NextStateTransition}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return The name of the state to transition to.
     */
    public String getNextStateName() {
        return nextStateName;
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    /**
     * BuilderAbstract for {@link NextStateTransition}
     */
    public static final class Builder implements Transition.Builder {

        private String nextStateName;

        private Builder() {
        }

        /**
         * REQUIRED. Sets the name of the state to transition to. Must be a valid state in the state machine.
         *
         * @param nextStateName State name
         * @return This object for method chaining.
         */
        public Builder nextStateName(String nextStateName) {
            this.nextStateName = nextStateName;
            return this;
        }

        /**
         * @return An immutable {WaitState} object.
         */
        @Override
        public NextStateTransition build() {
            return new NextStateTransition(this);
        }
    }
}
