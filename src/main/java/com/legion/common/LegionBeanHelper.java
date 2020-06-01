/*
 * Copyright 2017-2019 CodingApi .
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.legion.common;

import com.legion.gms.GossipStage;
import com.legion.gms.handler.GossipHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Description: BeanName 获取工具类
 */
@Component
@Slf4j
public class LegionBeanHelper {

    /**
     * Gossip handler 名称格式
     * gossip_handler_%
     * 处理器：%s（init, diff, confirm）
     */
    private static final String GOSSIP_HANDLER_FORMAT = "gossip_%s_handler";


    private final ApplicationContext spring;

    @Autowired
    public LegionBeanHelper(ApplicationContext spring) {
        this.spring = spring;
    }

    public GossipHandler loadGossipHandler(GossipStage gossipStage) {
        String beanName = String.format(GOSSIP_HANDLER_FORMAT, gossipStage.getCode());
        return spring.getBean(beanName, GossipHandler.class);
    }

}
