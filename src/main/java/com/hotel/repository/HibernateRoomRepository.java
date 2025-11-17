package com.hotel.repository;

import com.hotel.config.HibernateUtil;
import com.hotel.model.Room;
import com.hotel.model.RoomCategory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class HibernateRoomRepository {
    private final SessionFactory sessionFactory = HibernateUtil.getSessionFactory();

    public List<Room> findAll() {
        try (Session session = sessionFactory.openSession()) {
            Query<Room> query = session.createQuery("FROM Room r LEFT JOIN FETCH r.category", Room.class);
            return query.getResultList();
        }
    }

    public Optional<Room> findById(long id) {
        try (Session session = sessionFactory.openSession()) {
            Room room = session.get(Room.class, id);
            if (room != null) {
                room.getCategory();
            }
            return Optional.ofNullable(room);
        }
    }

    public Optional<Room> findByRoomNumber(String roomNumber) {
        try (Session session = sessionFactory.openSession()) {
            Query<Room> query = session.createQuery("FROM Room r LEFT JOIN FETCH r.category WHERE r.roomNumber = :roomNumber", Room.class);
            query.setParameter("roomNumber", roomNumber);
            return query.uniqueResultOptional();
        }
    }

    public List<Room> findAvailableRooms(LocalDate checkIn, LocalDate checkOut) {
        try (Session session = sessionFactory.openSession()) {
            String hql = "SELECT DISTINCT r FROM Room r LEFT JOIN FETCH r.category " +
                    "WHERE r.status = 'AVAILABLE' " +
                    "AND r.id NOT IN (" +
                    "SELECT res.room.id FROM Reservation res " +
                    "WHERE res.status != 'CANCELLED' " +
                    "AND ((res.checkIn <= :checkOut AND res.checkOut >= :checkIn))" +
                    ")";
            Query<Room> query = session.createQuery(hql, Room.class);
            query.setParameter("checkIn", checkIn);
            query.setParameter("checkOut", checkOut);
            return query.getResultList();
        }
    }

    public List<Room> findByCategory(long categoryId) {
        try (Session session = sessionFactory.openSession()) {
            Query<Room> query = session.createQuery("FROM Room r LEFT JOIN FETCH r.category WHERE r.category.id = :categoryId", Room.class);
            query.setParameter("categoryId", categoryId);
            return query.getResultList();
        }
    }

    public Room save(Room room) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.persist(room);
            transaction.commit();
            return room;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new IllegalStateException("Unable to save room", e);
        }
    }

    public Room update(Room room) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.merge(room);
            transaction.commit();
            return room;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new IllegalStateException("Unable to update room", e);
        }
    }

    public void updateStatus(long roomId, String status) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            Room room = session.get(Room.class, roomId);
            if (room != null) {
                room.setStatus(status);
                session.merge(room);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new IllegalStateException("Unable to update room status", e);
        }
    }

    public List<RoomCategory> findAllCategories() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM RoomCategory", RoomCategory.class).getResultList();
        }
    }

    public Optional<RoomCategory> findCategoryById(long id) {
        try (Session session = sessionFactory.openSession()) {
            return Optional.ofNullable(session.get(RoomCategory.class, id));
        }
    }

    public RoomCategory saveCategory(RoomCategory category) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.persist(category);
            transaction.commit();
            return category;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new IllegalStateException("Unable to save category", e);
        }
    }
}

