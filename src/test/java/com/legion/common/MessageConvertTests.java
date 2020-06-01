package com.legion.common;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.legion.core.api.X;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

/**
 * 测试消息转换
 *
 * @author lance
 * 8/19/2019 16:23
 */
@Slf4j
public class MessageConvertTests {

    @Test
    public void run() throws InvalidProtocolBufferException {
        X.XNodeSource request = X.XNodeSource.newBuilder()
                .setNodeId("tcp://127.0.0.1:8820")
                .setHost("127.0.0.0.1")
                .setPort(8820)
                .build();

        //发送消息
        X.XMessage message = MessageTests.send(request);
        log.info("===>XMessage: {}", message);

        //收到消息
        X.XNodeSource receive = MessageTests.receive(message, X.XNodeSource.class);
        log.info("===>Receive: {}", receive);
    }
}

class MessageTests {

    static <T extends Message> X.XMessage send(T message) {
        X.XMessage.Builder builder = X.XMessage.newBuilder();
        builder.setBody(Any.pack(message).toByteString());
        return builder.build();
    }

    static <K extends Message> K receive(X.XMessage message, Class<K> clazz) throws InvalidProtocolBufferException {
        ByteString body = message.getBody();
        return Any.parseFrom(body).unpack(clazz);
    }
}
