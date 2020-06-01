package com.legion.process.core;

/**
 * Activity类型
 */
public enum ActivityType {
    Activity,  //单个
    Exclusive, //分支
    Parallel,  //并行
    Inclusive  //汇聚
}
