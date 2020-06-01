package com.legion.process.state;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.legion.process.commons.PropertyNames;
import com.legion.process.transitions.EndTransition;
import com.legion.process.transitions.NextStateTransition;
import com.legion.process.transitions.Transition;

/**
 * Base class for states that allow transitions to either another state or
 * machine termination.
 */
abstract class TransitionStateBuilder implements State.Builder {

    public abstract TransitionStateBuilder transition(Transition.Builder builder);

    @JsonProperty(PropertyNames.END)
    private void setEnd(boolean isEnd) {
        if (isEnd) {
            transition(EndTransition.builder());
        }
    }

    @JsonProperty(PropertyNames.NEXT)
    private void setNext(String nextStateName) {
        transition(NextStateTransition.builder().nextStateName(nextStateName));
    }
}
