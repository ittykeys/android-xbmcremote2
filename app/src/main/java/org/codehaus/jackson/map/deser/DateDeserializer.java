package org.codehaus.jackson.map.deser;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;

import java.io.IOException;
import java.util.Date;

/**
 * Simple deserializer for handling {@link java.util.Date} values.
 * <p>
 * One way to customize Date formats accepted is to override method
 * {@link DeserializationContext#parseDate} that this basic
 * deserializer calls.
 */
public class DateDeserializer
        extends StdScalarDeserializer<Date> {
    public DateDeserializer() {
        super(Date.class);
    }

    @Override
    public java.util.Date deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        return _parseDate(jp, ctxt);
    }
}
