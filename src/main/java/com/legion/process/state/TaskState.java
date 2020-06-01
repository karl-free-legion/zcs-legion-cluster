package com.legion.process.state;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.legion.process.commons.PropertyNames;
import com.legion.process.transitions.Transition;
import lombok.Getter;

/**
 * The Task State causes the interpreter to execute the work identified by the state’s “Resource” field.
 *
 * <p>Currently allowed resources include Lambda functions and States activities.</p>
 */
public final class TaskState extends TransitionState {

    @JsonProperty(PropertyNames.RESOURCE)
    private final String resource;

    @JsonProperty(PropertyNames.INPUT_PATH)
    private final String inputPath;

    @JsonProperty(PropertyNames.RESULT_PATH)
    private final String resultPath;

    @JsonProperty(PropertyNames.OUTPUT_PATH)
    private final String outputPath;

    @JsonProperty(PropertyNames.COMMENT)
    private final String comment;

    @JsonProperty(PropertyNames.TIMEOUT_SECONDS)
    private final Integer timeoutSeconds;

    @JsonProperty(PropertyNames.HEARTBEAT_SECONDS)
    private final Integer heartbeatSeconds;

    /**
     * define Group
     */
    @Getter
    @JsonProperty(PropertyNames.GROUP)
    private final String group;

    /**
     * define Tag
     */
    @Getter
    @JsonProperty(PropertyNames.TAG)
    private final String tag;

    @JsonUnwrapped
    private final Transition transition;


    private TaskState(Builder builder) {
        this.resource = builder.resource;
        this.inputPath = builder.inputPath;
        this.resultPath = builder.resultPath;
        this.outputPath = builder.outputPath;
        this.comment = builder.comment;
        this.timeoutSeconds = builder.timeoutSeconds;
        this.heartbeatSeconds = builder.heartbeatSeconds;
        this.group = builder.group;
        this.tag = builder.tag;
        this.transition = builder.transition.build();
    }

    /**
     * @return BuilderAbstract instance to construct a {@link TaskState}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return Type identifier of {@link TaskState}.
     */
    @Override
    public String getType() {
        return "Task";
    }

    /**
     * @return URI of the resource to be executed by this task.
     */
    public String getResource() {
        return resource;
    }

    /**
     * @return The input path expression that may optionally transform the input to this state.
     */
    public String getInputPath() {
        return inputPath;
    }

    /**
     * @return The result path expression that may optionally combine or replace the state's raw input with it's result.
     */
    public String getResultPath() {
        return resultPath;
    }

    /**
     * @return The output path expression that may optionally transform the output to this state.
     */
    public String getOutputPath() {
        return outputPath;
    }

    /**
     * @return The transition that will occur when this task completes successfully.
     */
    @Override
    public Transition getTransition() {
        return transition;
    }

    /**
     * @return Human readable description for the state.
     */
    public String getComment() {
        return comment;
    }

    /**
     * @return Timeout, in seconds, that a task is allowed to run.
     */
    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    /**
     * @return Allowed time between "Heartbeats".
     */
    public Integer getHeartbeatSeconds() {
        return heartbeatSeconds;
    }

    /**
     * BuilderAbstract for a {@link TaskState}.
     */
    public static final class Builder extends TransitionStateBuilder {
        @JsonProperty(PropertyNames.RESOURCE)
        private String resource;
        @JsonProperty(PropertyNames.INPUT_PATH)
        private String inputPath;
        @JsonProperty(PropertyNames.RESULT_PATH)
        private String resultPath;
        @JsonProperty(PropertyNames.OUTPUT_PATH)
        private String outputPath;
        @JsonProperty(PropertyNames.COMMENT)
        private String comment;
        @JsonProperty(PropertyNames.TIMEOUT_SECONDS)
        private Integer timeoutSeconds;
        @JsonProperty(PropertyNames.HEARTBEAT_SECONDS)
        private Integer heartbeatSeconds;
        /**
         * define Group
         */
        @JsonProperty(PropertyNames.GROUP)
        private String group;
        /**
         * define Tag
         */
        @JsonProperty(PropertyNames.TAG)
        private String tag;

        private Transition.Builder transition = Transition.NULL_BUILDER;

        private Builder() {
        }

        /**
         * REQUIRED. Sets the resource to be executed by this task. Must be a URI that uniquely identifies the specific task to
         * execute. This may be the ARN of a Lambda function or a States Activity.
         *
         * @param resource URI of resource.
         * @return This object for method chaining.
         */
        public Builder resource(String resource) {
            this.resource = resource;
            return this;
        }

        /**
         * OPTIONAL. The value of “InputPath” MUST be a Path, which is applied to a State’s raw input to select some or all of
         * it;
         * that selection is used by the state. If not provided then the whole output from the previous state is used as input to
         * this state.
         *
         * @param inputPath New path value.
         * @return This object for method chaining.
         */
        public Builder inputPath(String inputPath) {
            this.inputPath = inputPath;
            return this;
        }

        /**
         * OPTIONAL. The value of “ResultPath” MUST be a Reference Path, which specifies the combination with or replacement of
         * the state’s result with its raw input. If not provided then the output completely replaces the input.
         *
         * @param resultPath New path value.
         * @return This object for method chaining.
         */
        public Builder resultPath(String resultPath) {
            this.resultPath = resultPath;
            return this;
        }

        /**
         * OPTIONAL. The value of “OutputPath” MUST be a path, which is applied to the state’s output after the application of
         * ResultPath, leading in the generation of the raw input for the next state. If not provided then the whole output is
         * used.
         *
         * @param outputPath New path value.
         * @return This object for method chaining.
         */
        public Builder outputPath(String outputPath) {
            this.outputPath = outputPath;
            return this;
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
         * OPTIONAL. Timeout, in seconds, that a task is allowed to run. If the task execution runs longer than this timeout the
         * execution fails with a { ErrorCodes#TIMEOUT} error.
         *
         * @param timeoutSeconds Timeout value.
         * @return This object for method chaining.
         */
        public Builder timeoutSeconds(Integer timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        /**
         * OPTIONAL. Allowed time between "Heartbeats". If the task does not send "Heartbeats" within the timeout then execution
         * fails with a { ErrorCodes#TIMEOUT}. If not set then no heartbeats are required. Heartbeats are a more granular
         * way
         * for a task to report it's progress to the state machine.
         *
         * @param heartbeatSeconds Heartbeat value.
         * @return This object for method chaining.
         */
        public Builder heartbeatSeconds(Integer heartbeatSeconds) {
            this.heartbeatSeconds = heartbeatSeconds;
            return this;
        }

        public Builder group(String group) {
            this.group = group;
            return this;
        }

        public Builder tag(String tag) {
            this.tag = tag;
            return this;
        }

        /**
         * REQUIRED. Sets the transition that will occur when the task completes successfully.
         *
         * @param transition New transition.
         * @return This object for method chaining.
         */
        @Override
        public Builder transition(Transition.Builder transition) {
            this.transition = transition;
            return this;
        }

        /**
         * @return An immutable {@link TaskState} object.
         */
        @Override
        public TaskState build() {
            return new TaskState(this);
        }
    }
}
