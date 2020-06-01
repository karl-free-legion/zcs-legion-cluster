package com.legion.process.commons;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.apache.commons.lang3.time.DateUtils;

import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Jackson
 *
 * @author lance
 * 4/23/2019 14:11
 */
public class DateModule {

    public static final SimpleModule INSTANCE = new SimpleModule();
    private static final String FORMATTER = "yyyy-MM-dd HH:mm:ss";

    static {
        INSTANCE.addSerializer(Date.class, new StdSerializer<Date>(Date.class) {
            @Override
            public void serialize(Date date,
                                  JsonGenerator jsonGenerator,
                                  SerializerProvider serializerProvider) throws
                    IOException {
                DateTimeFormatter formatter =
                        DateTimeFormatter.ofPattern(FORMATTER);
                LocalDateTime dateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneOffset.ofHours(8));
                jsonGenerator.writeString(dateTime.format(formatter));
            }
        });
        INSTANCE.addDeserializer(Date.class, new StdDeserializer<Date>(Date.class) {
            @Override
            public Date deserialize(JsonParser jsonParser,
                                    DeserializationContext deserializationContext) throws IOException {

                return fromJson(jsonParser.getValueAsString());
            }
        });
    }

    public static Date fromJson(String jsonText) {
        try {
            return DateUtils.parseDate(jsonText, FORMATTER);
        } catch (ParseException e) {
            return new Date();
        }
    }
}
