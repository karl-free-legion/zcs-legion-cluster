package com.legion.process.state;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.legion.process.commons.PropertyNames;

/**
 * Terminal state that terminates the state machine and marks it as a failure.
 */
public final class FailState implements State {

    @JsonProperty(PropertyNames.COMMENT)
    private final String comment;

    @JsonProperty(PropertyNames.ERROR)
    private final String error;

    @JsonProperty(PropertyNames.CAUSE)
    private final String cause;

    private FailState(Builder builder) {
        this.comment = builder.comment;
        this.error = builder.error;
        this.cause = builder.cause;
    }

    /**
     * @return BuilderAbstract instance to construct a {@link FailState}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return Type identifier of {@link FailState}.
     */
    @Override
    public String getType() {
        return "Fail";
    }

    /**
     * @return Human readable description for the state.
     */
    public String getComment() {
        return comment;
    }

    /**
     * @return Error code that can be referenced in {Retrier}s or {Catcher}s and can also be used for diagnostic
     * purposes.
     */
    public String getError() {
        return error;
    }

    /**
     * @return Human readable message describing the failure. Used for diagnostic purposes only.
     */
    public String getCause() {
        return cause;
    }

    /**
     * {@link FailState} is always a terminal state.
     *
     * @return True.
     */
    @Override
    public boolean isTerminalState() {
        return true;
    }

    /**
     * BuilderAbstract for a {@link FailState}.
     */
    public static final class Builder implements State.Builder {

        @JsonProperty(PropertyNames.COMMENT)
        private String comment;

        @JsonProperty(PropertyNames.ERROR)
        private String error;

        @JsonProperty(PropertyNames.CAUSE)
        private String cause;

        private Builder() {
        }

        /**
         * OPTIONAL. Human readable description for the state.
         *
         * @param comment New comment.
         * @return This object for method chaining.
         */
        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        /**
         * REQUIRED. Error code that can be referenced in {Retrier}s or {Catcher}s and can also be used for
         * diagnostic
         * purposes.
         *
         * @param error Error code value.
         * @return This object for method chaining.
         */
        public Builder error(String error) {
            this.error = error;
            return this;
        }

        /**
         * REQUIRED. Human readable message describing the failure. Used for diagnostic purposes only.
         *
         * @param cause Cause description.
         * @return This object for method chaining.
         */
        public Builder cause(String cause) {
            this.cause = cause;
            return this;
        }

        /**
         * @return An immutable {@link FailState} object.
         */
        @Override
        public FailState build() {
            return new FailState(this);
        }
    }
}
