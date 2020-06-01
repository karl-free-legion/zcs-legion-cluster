/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.legion.gms;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.legion.core.api.Gossip;

import java.util.Optional;

public enum GossipStage {
    /**
     * Init节点发送的初始化消息
     *
     * @ Gossip.GossipInit
     */
    INIT("init", Gossip.GossipInit.class),

    /**
     * Peer节点接收到GossipInit后反馈的对比信息
     *
     * @ Gossip.GossipDiff
     */
    DIFF("diff", Gossip.GossipDiff.class),

    /**
     * Init节点接收GossipDiff后反馈的确认消息给Peer
     *
     * @ Gossip.GossipConfirm
     */
    CONFIRM("confirm", Gossip.GossipConfirm.class);


    private String code;

    private Class<? extends Message> stageData;

    GossipStage(String code, Class<? extends Message> stageData) {
        this.code = code;
        this.stageData = stageData;
    }

    public String getCode() {
        return code;
    }

    /**
     * 反序列化
     *
     * @param data
     * @return
     */
    public Optional<? extends Message> deserialize(ByteString data) throws InvalidProtocolBufferException {
        Optional<? extends Message> unpack = Optional.of(Any.parseFrom(data).unpack(stageData));
        return unpack;
    }
}
