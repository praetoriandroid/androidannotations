package org.androidannotations.test15.ebean;

import android.app.NotificationManager;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.SystemService;

import static org.androidannotations.annotations.EBean.Scope.Singleton;

@EBean(scope = Singleton)
public class CyclicSingletonSingletonA {

    @Bean
    CyclicSingletonB ssab;

    @Bean
    CyclicNonSingletonC ssac;

    @SystemService
    NotificationManager notificationManager;

    public void bar() {
        ssac.bar();
    }

}
