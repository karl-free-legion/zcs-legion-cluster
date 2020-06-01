package com.legion.common.local;

import lombok.AllArgsConstructor;

/**
 * 定义语言类型
 *
 * @author lance
 * 8/20/2019 17:35
 */
@AllArgsConstructor
public enum Lang {
    /**
     * 中文
     */
    CN("cn", "简体中文"),
    EN("en", "English");

    private String code;
    private String name;

    public String code() {
        return code;
    }
}
