package com.hotel.repository;

import com.hotel.config.HibernateUtil;
import com.hotel.model.Reservation;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class HibernateReservationRepository {
    private final SessionFactory sessionFactory = HibernateUtil.getSessionFactory();

    public Reservation save(Reservation reservation) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.persist(reservation);
            transaction.commit();
            return reservation;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new IllegalStateException("Unable to save reservation", e);
        }
    }

    public Reservation update(Reservation reservation) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.merge(reservation);
            transaction.commit();
            return reservation;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new IllegalStateException("Unable to update reservation", e);
        }
    }

    public Optional<Reservation> findById(long id) {
        try (Session session = sessionFactory.openSession()) {
            Reservation reservation = session.get(Reservation.class, id);
            if (reservation != null) {
                reservation.getUser();
                reservation.getRoom();
                reservation.getPayments().size();
            }
            return Optional.ofNullable(reservation);
        }
    }

    public List<Reservation> findByUser(long userId) {
        try (Session session = sessionFactory.openSession()) {
            Query<Reservation> query = session.createQuery(
                    "FROM Reservation r LEFT JOIN FETCH r.user LEFT JOIN FETCH r.room WHERE r.user.id = :userId ORDER BY r.checkIn DESC",
                    Reservation.class);
            query.setParameter("userId", userId);
            return query.getResultList();
        }
    }

    public List<Reservation> findAll() {
        try (Session session = sessionFactory.openSession()) {
            Query<Reservation> query = session.createQuery(
                    "FROM Reservation r LEFT JOIN FETCH r.user LEFT JOIN FETCH r.room ORDER BY r.checkIn DESC",
                    Reservation.class);
            return query.getResultList();
        }
    }

    public List<Reservation> findByDateRange(LocalDate start, LocalDate end) {
        try (Session session = sessionFactory.openSession()) {
            Query<Reservation> query = session.createQuery(
                    "FROM Reservation r LEFT JOIN FETCH r.user LEFT JOIN FETCH r.room " +
                    "WHERE (r.checkIn <= :end AND r.checkOut >= :start) ORDER BY r.checkIn",
                    Reservation.class);
            query.setParameter("start", start);
            query.setParameter("end", end);
            return query.getResultList();
        }
    }

    public void cancelReservation(long reservationId) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            Reservation reservation = session.get(Reservation.class, reservationId);
            if (reservation != null) {
                reservation.setStatus("CANCELLED");
                session.merge(reservation);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new IllegalStateException("Unable to cancel reservation", e);
        }
    }
}

