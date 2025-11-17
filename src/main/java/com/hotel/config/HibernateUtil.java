package com.hotel.config;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {
    private static SessionFactory sessionFactory;
    private static final DatabaseConfig dbConfig = new DatabaseConfig();

    static {
        try {
            Configuration configuration = new Configuration();
            configuration.setProperty("hibernate.connection.driver_class", "com.mysql.cj.jdbc.Driver");
            String url = dbConfig.getUrl();
            if (!url.contains("?")) {
                url += "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
            } else {
                url += "&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
            }
            configuration.setProperty("hibernate.connection.url", url);
            configuration.setProperty("hibernate.connection.username", dbConfig.getUsername());
            configuration.setProperty("hibernate.connection.password", dbConfig.getPassword());
            // Dialect is auto-detected in Hibernate 6, no need to specify
            configuration.setProperty("hibernate.show_sql", "false");
            configuration.setProperty("hibernate.format_sql", "true");
            configuration.setProperty("hibernate.hbm2ddl.auto", "update");
            configuration.setProperty("hibernate.current_session_context_class", "thread");
            configuration.setProperty("hibernate.connection.pool_size", "10");

            StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                    .applySettings(configuration.getProperties())
                    .build();

            MetadataSources sources = new MetadataSources(registry);
            sources.addAnnotatedClass(com.hotel.model.User.class);
            sources.addAnnotatedClass(com.hotel.model.Role.class);
            sources.addAnnotatedClass(com.hotel.model.PrivilegeEntity.class);
            sources.addAnnotatedClass(com.hotel.model.RoomCategory.class);
            sources.addAnnotatedClass(com.hotel.model.Room.class);
            sources.addAnnotatedClass(com.hotel.model.Reservation.class);
            sources.addAnnotatedClass(com.hotel.model.Payment.class);
            sources.addAnnotatedClass(com.hotel.model.CustomerMessage.class);

            Metadata metadata = sources.getMetadataBuilder().build();
            sessionFactory = metadata.getSessionFactoryBuilder().build();
        } catch (Exception e) {
            System.err.println("Initial SessionFactory creation failed: " + e);
            throw new ExceptionInInitializerError(e);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void shutdown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }
}

