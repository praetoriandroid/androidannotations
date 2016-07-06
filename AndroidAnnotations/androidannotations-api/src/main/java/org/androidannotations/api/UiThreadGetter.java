package org.androidannotations.api;

import android.os.Handler;
import android.os.Looper;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

public class UiThreadGetter {

    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

    public static <T> T getOnUiThread(RunnableFuture<T> runnableFuture) {
        HANDLER.post(runnableFuture);

        return waitFuture(runnableFuture);
    }

    private static <T> T waitFuture(Future<T> future) {
        boolean wasInterrupted = false;
        while (true) {
            try {
                T result = future.get();
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

}
