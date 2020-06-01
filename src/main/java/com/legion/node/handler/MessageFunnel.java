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
package com.legion.node.handler;

import com.google.protobuf.Message;
import com.legion.core.api.X;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.BiPredicate;


/**
 * Message 接收过滤器
 */
@Slf4j
public class MessageFunnel {
    public interface Funnel {
        <T extends Message> boolean accept(X.XMessage message, ChannelHandlerContext nettyHandlerContext);
    }

    private static class Filtered implements Funnel {
        final BiPredicate<X.XMessage, ChannelHandlerContext> condition;
        final Funnel next;

        private Filtered(BiPredicate<X.XMessage, ChannelHandlerContext> condition, Funnel next) {
            this.condition = condition;
            this.next = next;
        }

        @Override
        public <T extends Message> boolean accept(X.XMessage message, ChannelHandlerContext nettyHandlerContext) {
            try {
                if (condition.test(message, nettyHandlerContext))
                    return next.accept(message, nettyHandlerContext);
                log.error("message handler failed due to condition failed with no exception, need code review!!!!");
            } catch (Throwable e) {
                log.error("message handler failed due to condition failed with no exception, need code review!!!!");
            }
            return true;
        }
    }

    private volatile Funnel Funnel;
    private static final AtomicReferenceFieldUpdater<MessageFunnel, Funnel> FunnelUpdater
            = AtomicReferenceFieldUpdater.newUpdater(MessageFunnel.class, Funnel.class, "Funnel");

    MessageFunnel(Funnel Funnel) {
        this.Funnel = Funnel;
    }

    public boolean accept(X.XMessage message, ChannelHandlerContext nettyHandlerContext) {

        return Funnel.accept(message, nettyHandlerContext);
    }

    public void add(BiPredicate<X.XMessage, ChannelHandlerContext> filter) {
        FunnelUpdater.updateAndGet(this, Funnel -> new Filtered(filter, Funnel));
    }

    public void remove(BiPredicate<X.XMessage, ChannelHandlerContext> allow) {
        FunnelUpdater.updateAndGet(this, Funnel -> without(Funnel, allow));
    }

    public void clear() {
        FunnelUpdater.updateAndGet(this, MessageFunnel::clear);
    }

    private static Funnel clear(Funnel Funnel) {
        while (Funnel instanceof MessageFunnel.Filtered)
            Funnel = ((MessageFunnel.Filtered) Funnel).next;
        return Funnel;
    }

    private static Funnel without(Funnel Funnel, BiPredicate<X.XMessage, ChannelHandlerContext> condition) {
        if (!(Funnel instanceof Filtered))
            return Funnel;

        Filtered filtered = (Filtered) Funnel;
        Funnel next = without(filtered.next, condition);
        return condition.equals(filtered.condition) ? next
                : next == filtered.next
                ? Funnel
                : new Filtered(filtered.condition, next);
    }

}
