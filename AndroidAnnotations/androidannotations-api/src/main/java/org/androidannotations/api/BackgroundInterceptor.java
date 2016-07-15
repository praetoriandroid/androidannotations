package org.androidannotations.api;

/**
 * Intercepts tasks to perform custom execution.
 *
 * @see BackgroundExecutor#execute(BackgroundExecutor.Task)
 */
public interface BackgroundInterceptor {

    /**
     * Intercepts execution
     * @param task task for being executed
     * @return true if intercepted, false otherwise
     */
    boolean intercept(BackgroundExecutor.Task task);

}
