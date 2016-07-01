package org.androidannotations.test15;

import android.content.Context;
import org.androidannotations.api.FactoryHook;
import org.androidannotations.test15.ebean.SomeBean;
import org.androidannotations.test15.ebean.SomeBean_;
import org.androidannotations.test15.ebean.SomeSingleton;
import org.androidannotations.test15.ebean.SomeSingleton_;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Field;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class FactoryHookTest {

    private Context context;
    private Hook hook;

    @Before
    public void setUp() throws Exception {
        FactoryHook.setHook(null);
        resetSingletonToNull();
        context = new EmptyActivityWithoutLayout_();
        hook = new Hook();
    }

    @AfterClass
    public static void clearHook() {
        FactoryHook.setHook(null);
    }

    @Test
    public void nonSingletonBeanFirstRequested() {
        FactoryHook.setHook(hook);
        SomeBean bean = SomeBean_.getInstance_(context);
        assertThat(hook.instanceRequested).isSameAs(bean);
    }

    @Test
    public void nonSingletonBeanSecondRequested() {
        SomeBean_.getInstance_(context);
        FactoryHook.setHook(hook);
        SomeBean bean = SomeBean_.getInstance_(context);
        assertThat(hook.instanceRequested).isSameAs(bean);
    }

    @Test
    public void nonSingletonBeanFirstCreated() {
        FactoryHook.setHook(hook);
        SomeBean bean = SomeBean_.getInstance_(context);
        assertThat(hook.instanceCreated).isSameAs(bean);
    }

    @Test
    public void nonSingletonBeanSecondCreated() {
        SomeBean_.getInstance_(context);
        FactoryHook.setHook(hook);
        SomeBean bean = SomeBean_.getInstance_(context);
        assertThat(hook.instanceCreated).isSameAs(bean);
    }

    @Test
    public void singletonBeanFirstRequested() {
        FactoryHook.setHook(hook);
        SomeSingleton bean = SomeSingleton_.getInstance_(context);
        assertThat(hook.instanceRequested).isSameAs(bean);
    }

    @Test
    public void singletonBeanSecondRequested() {
        SomeSingleton_.getInstance_(context);
        FactoryHook.setHook(hook);
        SomeSingleton bean = SomeSingleton_.getInstance_(context);
        assertThat(hook.instanceRequested).isSameAs(bean);
    }

    @Test
    public void singletonBeanFirstCreated() {
        FactoryHook.setHook(hook);
        SomeSingleton bean = SomeSingleton_.getInstance_(context);
        assertThat(hook.instanceCreated).isSameAs(bean);
    }

    @Test
    public void singletonBeanSecondNotCreated() {
        SomeSingleton_.getInstance_(context);
        FactoryHook.setHook(hook);
        SomeSingleton_.getInstance_(context);
        assertThat(hook.instanceCreated).isNull();
    }

    private void resetSingletonToNull() throws IllegalAccessException, NoSuchFieldException {
        Field instanceField = SomeSingleton_.class.getDeclaredField("instance_");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    private static class Hook implements FactoryHook.Hook {

        private Object instanceRequested;
        private Object instanceCreated;

        @Override
        public void onInstanceRequested(Object eComponent) {
            instanceRequested = eComponent;
        }

        @Override
        public void onInstanceCreated(Object eComponent) {
            instanceCreated = eComponent;
        }

    }

}
