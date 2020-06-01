package com.legion.process.commons;

/**
 * JSON property names used for serialization/deserialization.
 *
 * @author lance
 * 4/23/2019 14:07
 */
public interface PropertyNames {

    /*---------------------------------------------------------------
    |                   Common property names                       |
    ---------------------------------------------------------------*/

    String TYPE = "Type";
    String COMMENT = "Comment";
    String NEXT = "Next";
    String TIMEOUT_SECONDS = "TimeoutSeconds";
    String START = "Start";
    String STATES = "States";
    String RESULT = "Result";
    String RESULT_PATH = "ResultPath";
    String INPUT_PATH = "InputPath";
    String OUTPUT_PATH = "OutputPath";
    String END = "End";

    /*---------------------------------------------------------------
    |                   TaskState property names                    |
    ---------------------------------------------------------------*/
    /**
     * TaskState property names
     */
    String RESOURCE = "Resource";
    String HEARTBEAT_SECONDS = "HeartbeatSeconds";
    String GROUP = "Group";
    String TAG = "Tag";
    /*---------------------------------------------------------------
    |                    ParallelState property names               |
    ---------------------------------------------------------------*/
    /**
     * ParallelState property names
     */
    String BRANCHES = "Branches";

    /*---------------------------------------------------------------
    |                   FailState property names                    |
    ---------------------------------------------------------------*/
    /**
     * FailState property names
     */
    String ERROR = "Error";
    String CAUSE = "Cause";

    /*---------------------------------------------------------------
    |                    ChoiceState propert names                  |
    ---------------------------------------------------------------*/
    /**
     * ChoiceState propert names
     */
    String DEFAULT_STATE = "Default";
    String CHOICES = "Choices";

    /*---------------------------------------------------------------
    |                    Retrier/Catcher property names             |
    ---------------------------------------------------------------*/
    /**
     * Retrier/Catcher property names
     */
    String RETRY = "Retry";
    String CATCH = "Catch";
    String ERROR_EQUALS = "ErrorEquals";
    String INTERVAL_SECONDS = "IntervalSeconds";
    String MAX_ATTEMPTS = "MaxAttempts";
    String BACKOFF_RATE = "BackoffRate";

    /*---------------------------------------------------------------
    |             WaitState property names                          |
    ---------------------------------------------------------------*/
    /**
     * WaitState property names
     */
    String SECONDS = "Seconds";
    String TIMESTAMP = "Timestamp";
    String TIMESTAMP_PATH = "TimestampPath";
    String SECONDS_PATH = "SecondsPath";

    /*---------------------------------------------------------------
    |              Binary condition property names                  |
    ---------------------------------------------------------------*/
    /**
     * Binary condition property names
     */
    String VARIABLE = "Variable";

    /*---------------------------------------------------------------
    |       Binary string condition property names                  |
    ---------------------------------------------------------------*/
    /**
     * Binary string condition property names
     */
    String STRING_EQUALS = "StringEquals";
    String STRING_NOT_EQUALS = "StringNotEquals";
    String STRING_LESS_THAN = "StringLessThan";
    String STRING_GREATER_THAN = "StringGreaterThan";
    String STRING_GREATER_THAN_EQUALS = "StringGreaterThanEquals";
    String STRING_LESS_THAN_EQUALS = "StringLessThanEquals";

    /*---------------------------------------------------------------
    |         Binary numeric condition property names               |
    ---------------------------------------------------------------*/
    /**
     * Binary numeric condition property names
     */
    String NUMERIC_EQUALS = "NumericEquals";
    String NUMERIC_LESS_THAN = "NumericLessThan";
    String NUMERIC_GREATER_THAN = "NumericGreaterThan";
    String NUMERIC_GREATER_THAN_EQUALS = "NumericGreaterThanEquals";
    String NUMERIC_LESS_THAN_EQUALS = "NumericLessThanEquals";

    /*---------------------------------------------------------------
    |          Binary timestamp condition property names            |
    ---------------------------------------------------------------*/
    /**
     * Binary timestamp condition property names
     */
    String TIMESTAMP_EQUALS = "TimestampEquals";
    String TIMESTAMP_LESS_THAN = "TimestampLessThan";
    String TIMESTAMP_GREATER_THAN = "TimestampGreaterThan";
    String TIMESTAMP_GREATER_THAN_EQUALS = "TimestampGreaterThanEquals";
    String TIMESTAMP_LESS_THAN_EQUALS = "TimestampLessThanEquals";

    /*---------------------------------------------------------------
    |                  Binary boolean condition property names      |
    ---------------------------------------------------------------*/
    /**
     * Binary boolean condition property names
     */
    String BOOLEAN_EQUALS = "BooleanEquals";

    /*---------------------------------------------------------------
    |                   Composite conditions property names         |
    ---------------------------------------------------------------*/
    /**
     * Composite conditions property names
     */
    String AND = "And";
    String OR = "Or";
    String NOT = "Not";
}
