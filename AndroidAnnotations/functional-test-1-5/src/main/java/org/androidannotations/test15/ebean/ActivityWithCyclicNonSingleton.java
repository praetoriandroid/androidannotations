package org.androidannotations.test15.ebean;

import android.app.Activity;
import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;

@EActivity
public class ActivityWithCyclicNonSingleton extends Activity {

    @Bean
    CyclicNonSingletonB bean;

    @AfterInject
    void init() {
        bean.baz();
    }

}
