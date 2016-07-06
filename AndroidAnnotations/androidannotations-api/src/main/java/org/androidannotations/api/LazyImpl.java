package org.androidannotations.api;

public abstract class LazyImpl<T> implements Lazy<T> {

    private final CachedUiThreadFactory<T> cachedUiThreadFactory = new CachedUiThreadFactory<T>() {
        @Override
        protected T createInUiThread() {
            return create();
        }
    };

    @Override
    public T get() {
        return cachedUiThreadFactory.get();
    }

    protected abstract T create();

}
