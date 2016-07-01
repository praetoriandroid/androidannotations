package org.androidannotations.api;

public class FactoryHook {

    private static volatile Hook hook;

    public static void setHook(Hook hook) {
        FactoryHook.hook = hook;
    }

    public static void onInstanceRequested(Object eComponent) {
        Hook hook = FactoryHook.hook;
        if (hook != null) {
            hook.onInstanceRequested(eComponent);
        }
    }

    public static void onInstanceCreated(Object eComponent) {
        Hook hook = FactoryHook.hook;
        if (hook != null) {
            hook.onInstanceCreated(eComponent);
        }
    }

    public interface Hook {
        void onInstanceRequested(Object eComponent);
        void onInstanceCreated(Object eComponent);
    }
}
