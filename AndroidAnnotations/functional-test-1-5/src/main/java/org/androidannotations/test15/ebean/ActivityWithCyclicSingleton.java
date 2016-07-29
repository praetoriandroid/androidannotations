package org.androidannotations.test15.ebean;

import android.app.Activity;
import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;

@EActivity
public class ActivityWithCyclicSingleton extends Activity {

    @Bean
    CyclicSingletonB bean;

    @AfterInject
    void init() {
        bean.foo();
    }

}
