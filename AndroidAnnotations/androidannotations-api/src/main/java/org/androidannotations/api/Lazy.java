package org.androidannotations.api;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;

/**
 * Use with {@link Bean} annotation to initialize a bean only on the first usage
 * @param <T> a dependency with {@link EBean} annotation
 */
public interface Lazy<T> {

    /**
     * Accessor for the lazy dependency
     * @return
     */
    T get();

}
