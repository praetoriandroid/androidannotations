package org.androidannotations.test15.ebean;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;

import static org.androidannotations.annotations.EBean.Scope.Singleton;

@EBean(scope = Singleton)
public class CyclicSingletonNonSingleton2 {

    @Bean
    CyclicNonSingletonB snab;

    @Bean
    CyclicNonSingletonB snab2;

    public void foo() {
        snab.baz();
        snab2.baz();
    }

}
