package com.legion.process.commons;

/**
 * ErrorCodes
 *
 * @author lance
 * 4/25/2019 23:18
 */
public interface ErrorCodes {

    /**
     * A wild-card which matches any Error Name.
     */
    String ALL = "States.ALL";
    /**
     * A Task State either ran longer than the “TimeoutSeconds” value, or failed to heartbeat for a time longer than the
     * “HeartbeatSeconds” value.
     */
    String TIMEOUT = "States.Timeout";
    /**
     * A Task State failed during the execution.
     */
    String TASK_FAILED = "States.TaskFailed";
    /**
     * A Task State failed because it had insufficient privileges to execute the specified code.
     */
    String PERMISSIONS = "States.Permissions";
    /**
     * A Task State’s “ResultPath” field cannot be applied to the input the state received.
     */
    String RESULT_PATH_MATCH_FAILURE = "States.ResultPathMatchFailure";
    /**
     * A branch of a Parallel state failed.
     */
    String BRANCH_FAILED = "States.BranchFailed";
    /**
     * A Choice state failed to find a match for the condition field extracted from its input.
     */
    String NO_CHOICE_MATCHED = "States.NoChoiceMatched";

}
