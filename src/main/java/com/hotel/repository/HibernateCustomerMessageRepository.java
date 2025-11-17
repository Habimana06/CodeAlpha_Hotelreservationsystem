package com.hotel.repository;

import com.hotel.config.HibernateUtil;
import com.hotel.model.CustomerMessage;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;
import java.util.Optional;

public class HibernateCustomerMessageRepository {
    private final SessionFactory sessionFactory = HibernateUtil.getSessionFactory();

    public CustomerMessage save(CustomerMessage message) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.persist(message);
            transaction.commit();
            return message;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Failed to save customer message", e);
        }
    }

    public CustomerMessage update(CustomerMessage message) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.merge(message);
            transaction.commit();
            return message;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Failed to update customer message", e);
        }
    }

    public List<CustomerMessage> findAll() {
        try (Session session = sessionFactory.openSession()) {
            Query<CustomerMessage> query = session.createQuery(
                "FROM CustomerMessage m LEFT JOIN FETCH m.user ORDER BY m.createdAt DESC", 
                CustomerMessage.class
            );
            return query.getResultList();
        }
    }

    public List<CustomerMessage> findByUserId(long userId) {
        try (Session session = sessionFactory.openSession()) {
            Query<CustomerMessage> query = session.createQuery(
                "FROM CustomerMessage m LEFT JOIN FETCH m.user WHERE m.user.id = :userId ORDER BY m.createdAt DESC", 
                CustomerMessage.class
            );
            query.setParameter("userId", userId);
            return query.getResultList();
        }
    }

    public Optional<CustomerMessage> findById(long id) {
        try (Session session = sessionFactory.openSession()) {
            CustomerMessage message = session.get(CustomerMessage.class, id);
            if (message != null) {
                // Force loading of associations
                message.getUser();
            }
            return Optional.ofNullable(message);
        }
    }

    public List<CustomerMessage> findByStatus(String status) {
        try (Session session = sessionFactory.openSession()) {
            Query<CustomerMessage> query = session.createQuery(
                "FROM CustomerMessage m LEFT JOIN FETCH m.user WHERE m.status = :status ORDER BY m.createdAt DESC", 
                CustomerMessage.class
            );
            query.setParameter("status", status);
            return query.getResultList();
        }
    }
}

