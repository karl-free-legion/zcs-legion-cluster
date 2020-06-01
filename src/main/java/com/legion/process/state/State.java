package com.legion.process.state;

import com.fasterxml.jackson.annotation.*;
import com.legion.process.commons.Buildable;
import com.legion.process.commons.PropertyNames;

/**
 * Base interface for all states that can be used in a {StateMachine}.
 *
 * <p>This interface should not be implemented outside the SDK.</p>
 */
public interface State {

    /**
     * Type identifier for a {@link ChoiceState}.
     */
    String CHOICE = "Choice";

    /**
     * Type identifier for a {@link FailState}.
     */
    String FAIL = "Fail";

    /**
     * Type identifier for a {@link PassState}.
     */
    String PASS = "Pass";

    /**
     * Type identifier for a {@link SucceedState}.
     */
    String SUCCEED = "Succeed";

    /**
     * Type identifier for a {@link TaskState}.
     */
    String TASK = "Task";

    /**
     * @return The type identifier for this state.
     */
    @JsonProperty(PropertyNames.TYPE)
    String getType();

    @JsonIgnore
    boolean isTerminalState();

    /**
     * Base model interface for {@link State}s.
     */
    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = PropertyNames.TYPE)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = ChoiceState.Builder.class, name = CHOICE),
            @JsonSubTypes.Type(value = FailState.Builder.class, name = FAIL),
            @JsonSubTypes.Type(value = PassState.Builder.class, name = PASS),
            @JsonSubTypes.Type(value = SucceedState.Builder.class, name = SUCCEED),
            @JsonSubTypes.Type(value = TaskState.Builder.class, name = TASK)
    })
    @JsonIgnoreProperties(value = {PropertyNames.TYPE}, allowGetters = true)
    interface Builder extends Buildable<State> {
    }

}
