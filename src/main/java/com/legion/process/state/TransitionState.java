package com.legion.process.state;

import com.legion.process.transitions.Transition;

/**
 * Do not directly use this class, it is intended for internal usage only.
 */
public abstract class TransitionState implements State {

    /**
     * @return The transition that will occur when this state completes successfully.
     */
    public abstract Transition getTransition();

    @Override
    public final boolean isTerminalState() {
        return getTransition() != null && getTransition().isTerminal();
    }

}
