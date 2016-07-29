package org.androidannotations.test15.ebean;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;

@EBean()
public class CyclicNonSingletonB {

    @Bean
    CyclicSingletonNonSingletonA nba;

    @AfterInject
    void baz() {
        nba.foo();
    }

}
