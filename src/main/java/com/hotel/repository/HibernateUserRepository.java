package com.hotel.repository;

import com.hotel.config.HibernateUtil;
import com.hotel.model.PrivilegeEntity;
import com.hotel.model.Role;
import com.hotel.model.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class HibernateUserRepository {
    private final SessionFactory sessionFactory = HibernateUtil.getSessionFactory();

    public Optional<User> findByUsername(String username) {
        try (Session session = sessionFactory.openSession()) {
            Query<User> query = session.createQuery("FROM User u LEFT JOIN FETCH u.role LEFT JOIN FETCH u.privileges WHERE u.username = :username", User.class);
            query.setParameter("username", username);
            return query.uniqueResultOptional();
        }
    }

    public Optional<User> findById(long id) {
        try (Session session = sessionFactory.openSession()) {
            User user = session.get(User.class, id);
            if (user != null) {
                // Force loading of associations
                user.getRole();
                user.getPrivileges().size();
            }
            return Optional.ofNullable(user);
        }
    }

    public List<User> findAll() {
        try (Session session = sessionFactory.openSession()) {
            Query<User> query = session.createQuery("FROM User u LEFT JOIN FETCH u.role LEFT JOIN FETCH u.privileges", User.class);
            return query.getResultList();
        }
    }

    public User save(User user) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.persist(user);
            transaction.commit();
            return user;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new IllegalStateException("Unable to save user", e);
        }
    }

    public User update(User user) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.merge(user);
            transaction.commit();
            return user;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new IllegalStateException("Unable to update user", e);
        }
    }

    public void updateRole(long userId, Role role) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            User user = session.get(User.class, userId);
            if (user != null) {
                user.setRole(role);
                session.merge(user);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new IllegalStateException("Unable to update role", e);
        }
    }

    public void updateUserPrivileges(long userId, Set<PrivilegeEntity> privileges) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            User user = session.get(User.class, userId);
            if (user != null) {
                user.setPrivileges(privileges);
                session.merge(user);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new IllegalStateException("Unable to update privileges", e);
        }
    }

    public Role findRoleByName(String roleName) {
        try (Session session = sessionFactory.openSession()) {
            Query<Role> query = session.createQuery("FROM Role WHERE name = :name", Role.class);
            query.setParameter("name", roleName);
            return query.uniqueResult();
        }
    }

    public List<Role> findAllRoles() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM Role", Role.class).getResultList();
        }
    }

    public PrivilegeEntity findPrivilegeByCode(String code) {
        try (Session session = sessionFactory.openSession()) {
            Query<PrivilegeEntity> query = session.createQuery("FROM PrivilegeEntity WHERE code = :code", PrivilegeEntity.class);
            query.setParameter("code", code);
            return query.uniqueResult();
        }
    }

    public List<PrivilegeEntity> findAllPrivileges() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM PrivilegeEntity", PrivilegeEntity.class).getResultList();
        }
    }
    
    public Role findRoleById(long roleId) {
        try (Session session = sessionFactory.openSession()) {
            Role role = session.get(Role.class, roleId);
            if (role != null) {
                // Force loading of privileges
                role.getPrivileges().size();
            }
            return role;
        }
    }
    
    public void updateRole(Role role) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.merge(role);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new IllegalStateException("Unable to update role", e);
        }
    }
}

