package com.legion.process.transitions;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.legion.process.commons.PropertyNames;

/**
 * Terminal transition that indicates the state machine should terminate.
 */
public final class EndTransition implements Transition {
    @JsonProperty(PropertyNames.END)
    private final boolean end = true;

    private EndTransition() {
    }

    /**
     * @return BuilderAbstract instance to construct a {@link EndTransition}.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean isTerminal() {
        return true;
    }

    /**
     * BuilderAbstract for {@link EndTransition}
     */
    public static final class Builder implements Transition.Builder {

        private Builder() {
        }

        /**
         * @return An immutable {@link EndTransition} object.
         */
        @Override
        public EndTransition build() {
            return new EndTransition();
        }
    }
}
