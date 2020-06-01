package com.legion.process.transitions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.legion.process.commons.Buildable;

/**
 * Represents a transition in the state machine (i.e to another state or termination
 * of the state machine).
 *
 * <p>This interface should not be implemented outside the SDK.</p>
 *
 * @author lance
 */
public interface Transition {

    /**
     * No-op model that always returns null.
     */
    Builder NULL_BUILDER = () -> null;

    /**
     * True if this transition represents a terminal transition (i.e. one that would cause the state machine to exit).
     * False if this is a non terminal transition (i.e. to another state in the state machine).
     *
     * @return true/false
     */
    @JsonIgnore
    boolean isTerminal();

    /**
     * BuilderAbstract interface for {@link Transition}s.
     */
    interface Builder extends Buildable<Transition> {
    }
}
