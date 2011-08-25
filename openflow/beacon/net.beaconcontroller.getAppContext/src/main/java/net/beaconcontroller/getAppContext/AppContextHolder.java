package net.beaconcontroller.getAppContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 *
 * @author Vertika Singh
 */
public class AppContextHolder implements ApplicationContextAware {
    protected static Logger log = LoggerFactory.getLogger(AppContextHolder.class);
    protected static ApplicationContext applicationContext;
    protected static Object applicationContextLock = new Object();

    public AppContextHolder() {
    }

    public void setApplicationContext(ApplicationContext context)
            throws BeansException {
        synchronized (AppContextHolder.applicationContextLock) {
            AppContextHolder.applicationContext = context;
            AppContextHolder.applicationContextLock.notifyAll();
        }
    }

    public static ApplicationContext getApplicationContext(boolean block) {
        if (block) {
            synchronized (AppContextHolder.applicationContextLock) {
                if (AppContextHolder.applicationContext == null) {
                    try {
                        System.out.println("Waiting for bean");
                        AppContextHolder.applicationContextLock.wait();
                    } catch (InterruptedException e) {
                        log.error("Interupted while waiting for ApplicationContext to be set", e);
                    }
                }
            }
        }
        
        System.out.println("returning bean");
        return AppContextHolder.applicationContext;
    }
}
