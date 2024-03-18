package org.codehaus.jackson.map.util;

import java.util.Collection;

/**
 * Simple helper class used for decoupling instantiation of
 * optionally loaded handlers, like deserializers and deserializers
 * for libraries that are only present on some platforms.
 *
 * @param <T> Type of objects provided
 * @author tatu
 */
public interface Provider<T> {
    /**
     * Method used to request provider to provide entries it has
     */
    public Collection<T> provide();
}

