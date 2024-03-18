package org.codehaus.jackson.map.jsontype.impl;

import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.TypeSerializer;
import org.codehaus.jackson.map.jsontype.TypeIdResolver;

/**
 * @author tatus
 * @since 1.5
 */
public abstract class TypeSerializerBase extends TypeSerializer {
    protected final TypeIdResolver _idResolver;

    protected TypeSerializerBase(TypeIdResolver idRes) {
        _idResolver = idRes;
    }

    @Override
    public abstract JsonTypeInfo.As getTypeInclusion();

    @Override
    public String getPropertyName() {
        return null;
    }

    public TypeIdResolver getTypeIdResolver() {
        return _idResolver;
    }
}
