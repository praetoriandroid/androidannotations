package org.androidannotations.test15.ebean;

import android.content.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Field;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class BeanWithLazyBeansTest {

    @Before
    public void setUp() throws Exception {
        resetSingletonToNull();
    }

    @Test
    public void singletonIsLazy() throws Exception {
        Context context = mockContext();
        BeanWithLazyBeans bean = BeanWithLazyBeans_.getInstance_(context);
        assertThat(bean.initChildren).isEqualTo(0);
        bean.getSingleton();
        assertThat(bean.initChildren).isEqualTo(1);
    }

    @Test
    public void notSingletonIsLazy() throws Exception {
        Context context = mockContext();
        BeanWithLazyBeans bean = BeanWithLazyBeans_.getInstance_(context);
        assertThat(bean.initChildren).isEqualTo(0);
        bean.getNotSingleton();
        assertThat(bean.initChildren).isEqualTo(1);
    }

    @Test
    public void singletonIsTheSame() throws Exception {
        Context context = mockContext();
        BeanWithLazyBeans bean = BeanWithLazyBeans_.getInstance_(context);
        assertThat(bean.getSingleton()).isSameAs(bean.getSingleton());
    }

    @Test
    public void notSingletonIsTheSame() throws Exception {
        Context context = mockContext();
        BeanWithLazyBeans bean = BeanWithLazyBeans_.getInstance_(context);
        assertThat(bean.getNotSingleton()).isSameAs(bean.getNotSingleton());
    }

    private void resetSingletonToNull() throws IllegalAccessException, NoSuchFieldException {
        resetSingleton(BeanWithLazyBeans_.class);
        resetSingleton(BeanWithLazyBeans_.Singleton_.class);
    }

    private void resetSingleton(Class<?> c) throws NoSuchFieldException, IllegalAccessException {
        Field instanceField = c.getDeclaredField("instance_");

        instanceField.setAccessible(true);

        instanceField.set(null, null);
    }

    private Context mockContext() {
        Context context = mock(Context.class);
        when(context.getApplicationContext()).thenReturn(context);
        return context;
    }
}
