package com.legion.process;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legion.process.commons.Buildable;
import com.legion.process.commons.DateModule;
import com.legion.process.commons.PropertyNames;
import com.legion.process.state.State;
import lombok.Getter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a StepFunctions state machine. A state machine must have at least one state.
 *
 * @author lance
 * 4/23/2019 14:10
 */
@Getter
public final class StateMachine {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            .registerModule(DateModule.INSTANCE);

    @JsonProperty(PropertyNames.COMMENT)
    private final String comment;

    @JsonProperty(PropertyNames.START)
    private final String start;

    @JsonProperty(PropertyNames.TIMEOUT_SECONDS)
    private final Integer timeoutSeconds;

    @JsonProperty(PropertyNames.STATES)
    private final Map<String, State> states;

    private StateMachine(Builder builder) {
        this.comment = builder.comment;
        this.start = builder.start;
        this.timeoutSeconds = builder.timeoutSeconds;
        this.states = Buildable.Utils.build(builder.states);
    }

    /**
     * Deserializes a JSON representation of a state machine into a {@link Builder} .
     *
     * @param json JSON representing State machine.
     * @return Mutable {@link Builder} deserialized from JSON representation.
     */
    public static Builder fromJson(String json) {
        try {
            return MAPPER.readValue(json, Builder.class);
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Could not deserialize state machine.\n%s", json), e);
        }
    }

    /**
     * @return BuilderAbstract instance to construct a {@link StateMachine}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return Compact JSON representation of this StateMachine.
     */
    public String toJson() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not serialize state machine.", e);
        }
    }

    /**
     * @return Formatted JSON representation of this StateMachine.
     */
    public String toPrettyJson() {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not serialize state machine.", e);
        }
    }

    /**
     * BuilderAbstract for a {@link StateMachine}.
     */
    public static final class Builder {
        @JsonProperty(PropertyNames.STATES)
        private final Map<String, State.Builder> states = new LinkedHashMap<String, State.Builder>();
        @JsonProperty(PropertyNames.COMMENT)
        private String comment;
        @JsonProperty(PropertyNames.START)
        private String start;
        @JsonProperty(PropertyNames.TIMEOUT_SECONDS)
        private Integer timeoutSeconds;

        private Builder() {
        }

        /**
         * OPTIONAL. Human readable description for the state machine.
         *
         * @param comment New comment.
         * @return This object for method chaining.
         */
        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        /**
         * REQUIRED. Name of the state to start execution at. Must match a state name provided via {@link #state(String,
         * State.Builder)}.
         *
         * @param start Name of starting state.
         * @return This object for method chaining.
         */
        public Builder start(String start) {
            this.start = start;
            return this;
        }

        /**
         * OPTIONAL. Timeout, in seconds, that a state machine is allowed to run. If the machine execution runs longer than this
         * timeout the execution fails with a { ErrorCodes#TIMEOUT} error
         *
         * @param timeoutSeconds Timeout value.
         * @return This object for method chaining.
         */
        public Builder timeoutSeconds(Integer timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        /**
         * REQUIRED. Adds a new state to the state machine. A state machine MUST have at least one state.
         *
         * @param stateName    Name of the state
         * @param stateBuilder Instance of {@link State.Builder}. Note that the {@link State}
         *                     object is not built until the {@link StateMachine} is built so any modifications on the state
         *                     model will be reflected in this object.
         * @return This object for method chaining.
         */
        public Builder state(String stateName, State.Builder stateBuilder) {
            this.states.put(stateName, stateBuilder);
            return this;
        }

        public StateMachine build() {
            return new StateMachine(this);
        }
    }
}
