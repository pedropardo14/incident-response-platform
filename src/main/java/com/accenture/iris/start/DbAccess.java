package com.accenture.iris.start;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Static accessor for JdbcTemplate.
 *
 * Neither MainApp nor @PreLoad functions are Spring-managed (Mercury creates
 * them via reflection), so @Autowired doesn't work in those classes.
 *
 * This class IS a Spring @Component, so Spring injects JdbcTemplate into it
 * via constructor injection during Spring startup — before Mercury calls
 * MainApp.start(). All Mercury-managed classes then call DbAccess.get().
 */
@Component
public class DbAccess {
    private static JdbcTemplate instance;

    @Autowired
    public DbAccess(JdbcTemplate jdbc) {
        instance = jdbc;
    }

    public static JdbcTemplate get() {
        if (instance == null) {
            throw new IllegalStateException("DbAccess not initialized — Spring context not ready yet");
        }
        return instance;
    }
}