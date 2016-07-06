package org.androidannotations.test15.ebean;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.api.Lazy;

@EBean(scope = EBean.Scope.Singleton)
public class BeanWithLazyBeans {

    @EBean(scope = EBean.Scope.Singleton)
    static class Singleton {

        @Bean
        BeanWithLazyBeans parent;

        @AfterInject
        void init() {
            System.out.println(parent);
            parent.childInitialized();
        }

    }

    @EBean
    static class NotSingleton {

        @Bean
        BeanWithLazyBeans parent;

        @AfterInject
        void init() {
            parent.childInitialized();
        }

    }

    @Bean
    Lazy<Singleton> singleton;

    @Bean
    Lazy<NotSingleton> notSingleton;

    int initChildren = 0;

    void childInitialized() {
        initChildren++;
    }

    Singleton getSingleton() {
        return singleton.get();
    }

    NotSingleton getNotSingleton() {
        return notSingleton.get();
    }

}
