package org.androidannotations.api;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import static org.androidannotations.api.UiThreadGetter.getOnUiThread;

public abstract class CachedUiThreadFactory<T> implements Callable<T> {

    private T cache;

    public T get() {
        if (BackgroundExecutor.isUiThread()) {
            if (cache != null) {
                return cache;
            }
            return call();
        }

        T cachedResult = getCachedResultSync();

        if (cachedResult != null) {
            return cachedResult;
        }

        FutureTask<T> futureTask = new FutureTask<T>(this);

        return getOnUiThread(futureTask);
    }

    private synchronized T getCachedResultSync() {
        return cache;
    }

    public synchronized final T call() {
        if (cache != null) {
            return cache;
        }
        cache = createInUiThread();
        return cache;
    }

    protected abstract T createInUiThread();

}
