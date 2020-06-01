package com.legion.node.controller;

import com.legion.common.event.LegionEvent;

/**
 * Create At 2019/8/20
 *
 * @author Zing
 * @version 0.0.1
 */
public enum ClusterEvent implements LegionEvent {
    /**
     * 节点加入
     */
    ADD_NODE(0, "addNode"),
    /**
     * 节点等待
     */
    MAKE_PENDING(1, "makeNodePending"),
    /**
     * 节点可用
     */
    MAKE_RUNNING(2, "makeNodeRunning"),
    /**
     * 节点删除
     */
    MAKE_REMOVE(3, "deleteNode");

    int code;
    String str;

    ClusterEvent(int code, String str) {
        this.code = code;
        this.str = str;
    }


    @Override
    public String getEventStr() {
        return this.str;
    }

    public int getEventCode() {
        return this.code;
    }

}