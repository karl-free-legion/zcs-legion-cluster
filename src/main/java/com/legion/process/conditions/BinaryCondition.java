package com.legion.process.conditions;

/**
 * Interface for all binary conditions.
 *
 * <p>This interface should not be implemented outside of the SDK.</p>
 *
 * @param <T> Type of expected value.
 * @author lance
 */
public interface BinaryCondition<T> extends Condition {

    /**
     * The JSONPath expression that determines which piece of the input document is used for the comparison.
     *
     * @return variable
     */
    String getVariable();

    /**
     * The expected value for this condition.
     *
     * @return value
     */
    T getExpectedValue();
}
