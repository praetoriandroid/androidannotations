package org.androidannotations.api;

import android.os.Handler;
import android.os.Looper;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public abstract class CachedUiThreadFactory<T> implements Callable<T> {

    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

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

        HANDLER.post(futureTask);

        return waitFuture(futureTask);
    }

    private T waitFuture(Future<T> futureTask) {
        boolean wasInterrupted = false;
        while (true) {
            try {
                T result = futureTask.get();
                if (wasInterrupted) {
                    Thread.currentThread().interrupt();
                }
                return result;
            } catch (InterruptedException e) {
                wasInterrupted = true;
            } catch (ExecutionException e) {
                try {
                    throw e.getCause();
                } catch (RuntimeException ex) {
                    throw ex;
                } catch (Error ex) {
                    throw ex;
                } catch (Throwable throwable) {
                    throw new UndeclaredThrowableException(throwable);
                }
            }
        }
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
