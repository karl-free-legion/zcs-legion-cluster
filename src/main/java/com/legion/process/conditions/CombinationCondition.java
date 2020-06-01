package com.legion.process.conditions;

import java.util.List;

/**
 * 组合条件(or/and)
 *
 * @author lance
 */
public interface CombinationCondition extends Condition {

    /**
     * List of conditions contained in the combination expression(or/and)
     *
     * @return Conditions
     */
    List<Condition> getConditions();

}
