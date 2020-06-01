package com.legion.common.local;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 读取国际化配置
 * Object[]params = {"pay", "/apply/se"};
 * String message = messageResource.getMessage(Lang.CN, errorCode, params);
 *
 * @author lance
 * 8/20/2019 17:32
 */
@Slf4j
public class MessageResource {
    private static class MessageResourceHolder {
        private final static MessageResource INSTANCE = new MessageResource();
    }

    private Function<String, String> keyFun = line -> StringUtils.substringBefore(line, "=");
    private Function<String, String> valueFun = line -> StringUtils.substringAfter(line, "=");
    private Map<String, Map<String, String>> lang = Maps.newHashMap();

    private MessageResource() {
        init();
    }

    public static MessageResource getInstance() {
        return MessageResourceHolder.INSTANCE;
    }

    /**
     * 初始化数据
     */
    public void init() {
        try {
            File dir = ResourceUtils.getFile("classpath:i18n");
            if (dir.isDirectory()) {
                File[] files = dir.listFiles(((dir1, name) -> name.startsWith("message_")));
                if (files == null || files.length == 0) {
                    return;
                }

                Arrays.stream(files).parallel().forEach(file -> {
                    try {
                        handlerMessage(file);
                    } catch (IOException e) {
                        log.error("Load message.properties fail: ", e);
                    }
                });
            }
        } catch (IOException e) {
            log.error("Load message.properties fail: ", e);
        }
    }

    /**
     * 获取转换, 默认中文数据
     *
     * @param code   errorCode
     * @param params 参数
     * @return 返回错误信息
     */
    public String getMessage(String code, Object... params) {
        Assert.hasText(code, "ErrorCode require not null");
        return getMessage(Lang.CN, code, params);
    }

    /**
     * 获取转换
     *
     * @param local  Lang
     * @param code   errorCode
     * @param params 参数
     * @return 返回错误信息
     */
    public String getMessage(Lang local, String code, Object... params) {
        Assert.hasText(code, "ErrorCode require not null");

        Map<String, String> map = lang.get(local.code());
        if (map == null) {
            return "No corresponding language properties.";
        }

        String pattern = map.get(code);
        if (StringUtils.isBlank(pattern)) {
            return "No corresponding error code";
        }

        return MessageFormat.format(pattern, params);
    }

    /**
     * 处理properties中内容
     *
     * @param file file
     * @throws IOException IOException
     */
    private void handlerMessage(File file) throws IOException {
        String fileName = file.getName();
        String local = StringUtils.substringBetween(fileName, "message_", ".");
        List<String> lines = Files.readAllLines(file.toPath(), Charsets.UTF_8);

        lang.put(local, lines.stream().filter(line -> !line.startsWith("#") && StringUtils.isNotBlank(line))
                .collect(Collectors.toMap(keyFun, valueFun)));
    }
}
