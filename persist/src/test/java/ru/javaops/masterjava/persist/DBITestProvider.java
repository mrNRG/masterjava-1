package ru.javaops.masterjava.persist;

import com.typesafe.config.Config;
import ru.javaops.masterjava.config.common.Configs;

import java.sql.DriverManager;

/**
 * gkislin
 * 27.10.2016
 */
public class DBITestProvider {
    public static void initDBI() {
        //initDBI("jdbc:postgresql://localhost:5434/masterjava", "postgres", "postgres");
        Config db = Configs.getConfig("persist.conf","db");
        initDBI(db.getString("url"), db.getString("user"), db.getString("password"));
    }

    public static void initDBI(String dbUrl, String dbUser, String dbPassword) {
        DBIProvider.init(() -> {
            try {
                Class.forName("org.postgresql.Driver");
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("PostgreSQL driver not found", e);
            }
            return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        });
    }
}