package org.androidannotations.test15.ebean;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;

import static org.androidannotations.annotations.EBean.Scope.Singleton;

@EBean(scope = Singleton)
public class CyclicSingletonNonSingletonA {

    @Bean
    CyclicNonSingletonB snab;

    @Bean
    CyclicNonSingletonC snac;

    public void foo() {
        snac.bar();
    }

}
