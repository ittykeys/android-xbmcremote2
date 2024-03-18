package org.codehaus.jackson.node;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.SerializerProvider;

import java.io.IOException;

/**
 * This singleton value class is used to contain explicit JSON null
 * value.
 */
public final class NullNode
        extends ValueNode {
    // // Just need a fly-weight singleton

    public final static NullNode instance = new NullNode();

    private NullNode() {
    }

    public static NullNode getInstance() {
        return instance;
    }

    @Override
    public JsonToken asToken() {
        return JsonToken.VALUE_NULL;
    }

    @Override
    public boolean isNull() {
        return true;
    }

    public String getValueAsText() {
        return "null";
    }

    @Override
    public final void serialize(JsonGenerator jg, SerializerProvider provider)
            throws IOException, JsonProcessingException {
        jg.writeNull();
    }

    public boolean equals(Object o) {
        return (o == this);
    }
}
