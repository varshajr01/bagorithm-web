package com.bagorithm;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class DatabaseListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent event) {
        try {
            Db.initialize();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not initialize the BAGORITHM database.", exception);
        }
    }
}
