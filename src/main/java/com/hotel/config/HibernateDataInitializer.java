package com.hotel.config;

import com.hotel.model.*;
import com.hotel.repository.HibernateUserRepository;
import com.hotel.repository.HibernateRoomRepository;
import com.hotel.service.PasswordEncoder;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.HashSet;
import java.util.Set;

public class HibernateDataInitializer {
    private final SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
    private final PasswordEncoder passwordEncoder = new PasswordEncoder();

    public void initializeData() {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();

            // Create roles
            Role customerRole = createOrGetRole(session, "CUSTOMER");
            Role receptionistRole = createOrGetRole(session, "RECEPTIONIST");
            Role managerRole = createOrGetRole(session, "MANAGER");
            Role adminRole = createOrGetRole(session, "ADMIN");

            // Create privileges
            PrivilegeEntity bookRoom = createOrGetPrivilege(session, "BOOK_ROOM", "Create reservations and pay deposits");
            PrivilegeEntity cancelReservation = createOrGetPrivilege(session, "CANCEL_RESERVATION", "Cancel upcoming stays");
            PrivilegeEntity checkIn = createOrGetPrivilege(session, "CHECK_IN", "Mark guests as checked in");
            PrivilegeEntity checkOut = createOrGetPrivilege(session, "CHECK_OUT", "Mark guests as checked out");
            PrivilegeEntity manageRooms = createOrGetPrivilege(session, "MANAGE_ROOMS", "Create and update room inventory");
            PrivilegeEntity viewAnalytics = createOrGetPrivilege(session, "VIEW_ANALYTICS", "See occupancy dashboards");
            PrivilegeEntity manageUsers = createOrGetPrivilege(session, "MANAGE_USERS", "Create users and assign privileges");

            // Assign privileges to roles
            assignPrivilegesToRole(session, customerRole, bookRoom, cancelReservation);
            assignPrivilegesToRole(session, receptionistRole, bookRoom, cancelReservation, checkIn, checkOut);
            assignPrivilegesToRole(session, managerRole, viewAnalytics, manageRooms);
            assignPrivilegesToRole(session, adminRole, bookRoom, cancelReservation, checkIn, checkOut, manageRooms, viewAnalytics, manageUsers);

            // Create room categories
            RoomCategory standard = createOrGetCategory(session, "Standard", "Urban-chic rooms ideal for solo travelers.", 120.00);
            RoomCategory deluxe = createOrGetCategory(session, "Deluxe", "Spacious suites with balcony and lounge.", 210.00);
            RoomCategory suite = createOrGetCategory(session, "Suite", "Panoramic suites with workspace and spa tub.", 340.00);

            // Create 5 rooms with online images
            createOrGetRoom(session, "101", standard, 1, "City Garden", "AVAILABLE", 
                "https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=800", 
                "Standard rooms come with: Free Wi-Fi, Air Conditioning, TV, and basic amenities. Comfortable and well-appointed room for your stay.");
            createOrGetRoom(session, "102", standard, 1, "Atrium View", "AVAILABLE", 
                "https://images.unsplash.com/photo-1611892440504-42a792e24d32?w=800", 
                "Standard rooms come with: Free Wi-Fi, Air Conditioning, TV, and basic amenities. Modern room with atrium view.");
            createOrGetRoom(session, "201", deluxe, 2, "Skyline", "AVAILABLE", 
                "https://images.unsplash.com/photo-1590490360182-c33d57733427?w=800", 
                "Premium rooms come with: Free Wi-Fi, Air Conditioning, TV, Breakfast, and enhanced amenities. Spacious room with stunning skyline views.");
            createOrGetRoom(session, "202", deluxe, 2, "Garden View", "AVAILABLE", 
                "https://images.unsplash.com/photo-1566665797739-1674de7a421a?w=800", 
                "Premium rooms come with: Free Wi-Fi, Air Conditioning, TV, Breakfast, and enhanced amenities. Beautiful room overlooking the garden.");
            createOrGetRoom(session, "301", suite, 3, "Ocean View", "AVAILABLE", 
                "https://images.unsplash.com/photo-1578683010236-d716f9a3f461?w=800", 
                "Deluxe rooms come with: Free Wi-Fi, Air Conditioning, TV, Breakfast, Dinner Service, Pool Access, and luxury amenities. Panoramic suite with ocean views.");

            // Create sample users
            createOrGetUser(session, "customer", "password", "John", "Doe", "customer@hotel.com", customerRole, new HashSet<>());
            createOrGetUser(session, "receptionist", "password", "Jane", "Smith", "receptionist@hotel.com", receptionistRole, new HashSet<>());
            createOrGetUser(session, "manager", "password", "Bob", "Johnson", "manager@hotel.com", managerRole, new HashSet<>());
            createOrGetUser(session, "admin", "password", "Admin", "User", "admin@hotel.com", adminRole, Set.of(manageUsers));

            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            System.err.println("Error initializing data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Role createOrGetRole(Session session, String name) {
        Role role = session.createQuery("FROM Role WHERE name = :name", Role.class)
                .setParameter("name", name)
                .uniqueResult();
        if (role == null) {
            role = new Role(name);
            session.persist(role);
        }
        return role;
    }

    private PrivilegeEntity createOrGetPrivilege(Session session, String code, String description) {
        PrivilegeEntity priv = session.createQuery("FROM PrivilegeEntity WHERE code = :code", PrivilegeEntity.class)
                .setParameter("code", code)
                .uniqueResult();
        if (priv == null) {
            priv = new PrivilegeEntity(code, description);
            session.persist(priv);
        }
        return priv;
    }

    private void assignPrivilegesToRole(Session session, Role role, PrivilegeEntity... privileges) {
        role = session.merge(role);
        for (PrivilegeEntity priv : privileges) {
            priv = session.merge(priv);
            if (!role.getPrivileges().contains(priv)) {
                role.getPrivileges().add(priv);
            }
        }
        session.merge(role);
    }

    private RoomCategory createOrGetCategory(Session session, String name, String description, double baseRate) {
        RoomCategory category = session.createQuery("FROM RoomCategory WHERE name = :name", RoomCategory.class)
                .setParameter("name", name)
                .uniqueResult();
        if (category == null) {
            category = new RoomCategory();
            category.setName(name);
            category.setDescription(description);
            category.setBaseRate(baseRate);
            session.persist(category);
        }
        return category;
    }

    private Room createOrGetRoom(Session session, String roomNumber, RoomCategory category, int floor, 
                                 String viewType, String status, String photoUrl, String description) {
        Room room = session.createQuery("FROM Room WHERE roomNumber = :roomNumber", Room.class)
                .setParameter("roomNumber", roomNumber)
                .uniqueResult();
        if (room == null) {
            room = new Room();
            room.setRoomNumber(roomNumber);
            room.setCategory(category);
            room.setFloor(floor);
            room.setViewType(viewType);
            room.setStatus(status);
            room.setPhotoUrl(photoUrl);
            room.setDescription(description);
            session.persist(room);
        }
        return room;
    }

    private User createOrGetUser(Session session, String username, String password, String firstName, 
                                 String lastName, String email, Role role, Set<PrivilegeEntity> privileges) {
        User user = session.createQuery("FROM User WHERE username = :username", User.class)
                .setParameter("username", username)
                .uniqueResult();
        if (user == null) {
            user = new User();
            user.setUsername(username);
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail(email);
            user.setRole(role);
            user.setActive(true);
            user.setPrivileges(privileges);
            session.persist(user);
        }
        return user;
    }
}

