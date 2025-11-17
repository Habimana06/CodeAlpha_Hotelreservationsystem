package com.hotel.repository;

import com.hotel.config.HibernateUtil;
import com.hotel.model.Payment;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;
import java.util.Optional;

public class HibernatePaymentRepository {
    private final SessionFactory sessionFactory = HibernateUtil.getSessionFactory();

    public Payment save(Payment payment) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.persist(payment);
            transaction.commit();
            return payment;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new IllegalStateException("Unable to save payment", e);
        }
    }

    public Payment update(Payment payment) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.merge(payment);
            transaction.commit();
            return payment;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new IllegalStateException("Unable to update payment", e);
        }
    }

    public Optional<Payment> findById(long id) {
        try (Session session = sessionFactory.openSession()) {
            Payment payment = session.get(Payment.class, id);
            if (payment != null) {
                payment.getReservation();
            }
            return Optional.ofNullable(payment);
        }
    }

    public List<Payment> findByReservation(long reservationId) {
        try (Session session = sessionFactory.openSession()) {
            Query<Payment> query = session.createQuery(
                    "FROM Payment p LEFT JOIN FETCH p.reservation WHERE p.reservation.id = :reservationId",
                    Payment.class);
            query.setParameter("reservationId", reservationId);
            return query.getResultList();
        }
    }

    public List<Payment> findAll() {
        try (Session session = sessionFactory.openSession()) {
            Query<Payment> query = session.createQuery(
                    "FROM Payment p LEFT JOIN FETCH p.reservation",
                    Payment.class);
            return query.getResultList();
        }
    }
}

