package com.legion.process.conditions;

import com.legion.process.commons.Buildable;

import java.util.Map;

/**
 * Base interface for conditions used in {Choice}s.
 *
 * <p>This interface should not be implemented outside of the SDK.</p>
 */
public interface Condition {

    /**
     * No-op model that always returns null.
     */
    Builder NULL_BUILDER = () -> null;

    /**
     * Base model interface for conditions used in {Choice}s.
     */
    interface Builder extends Buildable<Condition> {

    }

    /**
     * 校验方法
     *
     * @param params params
     * @return true/false
     */
    boolean check(Map<String, Object> params);
}
