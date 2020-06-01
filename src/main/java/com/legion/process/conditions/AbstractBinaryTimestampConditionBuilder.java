package com.legion.process.conditions;

import com.fasterxml.jackson.databind.JsonNode;
import com.legion.process.commons.DateModule;

import java.util.Date;

abstract class AbstractBinaryTimestampConditionBuilder extends AbstractBinaryConditionBuilder {

    public abstract AbstractBinaryTimestampConditionBuilder expectedValue(Date expectedValue);

    @Override
    public final AbstractBinaryTimestampConditionBuilder expectedValue(JsonNode expectedValue) {
        expectedValue(DateModule.fromJson(expectedValue.asText()));
        return this;
    }
}
