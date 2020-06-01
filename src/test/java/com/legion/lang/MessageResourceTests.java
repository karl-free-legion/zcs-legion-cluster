package com.legion.lang;

import com.legion.common.local.Lang;
import com.legion.common.local.MessageResource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;

/**
 * 读取资源文件, 并转换消息内容
 *
 * @author lance
 * 8/20/2019 18:12
 */
@Slf4j
public class MessageResourceTests {

    @Test
    public void run() {
        String notFound = "-401";
        // final String[] params = {"pay", "/apply/se"};
        log.info("===>cn: {}", MessageResource.getInstance().getMessage(notFound, "pay", "/apply/se"));
        log.info("===>en: {}", MessageResource.getInstance().getMessage(Lang.EN, notFound, "pay", "/apply/se"));

        String notAuth = "-404";
        log.info("===>zh: {}", MessageResource.getInstance().getMessage(notAuth, "pay", "/apply/se"));
        log.info("===>en: {}", MessageResource.getInstance().getMessage(Lang.EN, notAuth, "pay", "/apply/se"));

        log.info("===>zh: {}", MessageResource.getInstance().getMessage("2", "pay", "/apply/se"));
    }
}
