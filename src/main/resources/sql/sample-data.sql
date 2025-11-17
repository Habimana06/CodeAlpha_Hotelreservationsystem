USE hotel_reservation_system;

INSERT INTO roles (name) VALUES
    ('CUSTOMER'),
    ('RECEPTIONIST'),
    ('MANAGER'),
    ('ADMIN')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO privileges (code, description) VALUES
    ('BOOK_ROOM', 'Create reservations and pay deposits'),
    ('CANCEL_RESERVATION', 'Cancel upcoming stays'),
    ('CHECK_IN', 'Mark guests as checked in'),
    ('CHECK_OUT', 'Mark guests as checked out'),
    ('MANAGE_ROOMS', 'Create and update room inventory'),
    ('VIEW_ANALYTICS', 'See occupancy dashboards'),
    ('MANAGE_USERS', 'Create users and assign privileges')
ON DUPLICATE KEY UPDATE description = VALUES(description);

INSERT IGNORE INTO role_privileges (role_id, privilege_id)
SELECT r.id, p.id FROM roles r CROSS JOIN privileges p
WHERE (r.name = 'CUSTOMER'     AND p.code IN ('BOOK_ROOM','CANCEL_RESERVATION'))
   OR (r.name = 'RECEPTIONIST' AND p.code IN ('BOOK_ROOM','CANCEL_RESERVATION','CHECK_IN','CHECK_OUT'))
   OR (r.name = 'MANAGER'      AND p.code IN ('VIEW_ANALYTICS','MANAGE_ROOMS'))
   OR (r.name = 'ADMIN');

INSERT INTO room_categories (name, description, base_rate) VALUES
    ('Standard', 'Urban-chic rooms ideal for solo travelers.', 120.00),
    ('Deluxe', 'Spacious suites with balcony and lounge.', 210.00),
    ('Suite', 'Panoramic suites with workspace and spa tub.', 340.00)
ON DUPLICATE KEY UPDATE description = VALUES(description), base_rate = VALUES(base_rate);

INSERT INTO rooms (room_number, category_id, floor, view_type, status, photo_url, description)
VALUES
    ('101', 1, 1, 'City Garden', 'AVAILABLE', 'assets/herbanet-lobby.png', 'Biophilic textures, queen bed'),
    ('102', 1, 1, 'Atrium', 'OCCUPIED', 'assets/herbanet-lobby.png', 'Green wall, queen bed'),
    ('201', 2, 2, 'Skyline', 'AVAILABLE', 'assets/herbanet-lobby.png', 'Balcony, king bed'),
    ('202', 2, 2, 'Garden', 'AVAILABLE', 'assets/herbanet-lobby.png', 'Corner lounge'),
    ('301', 3, 3, 'Ocean', 'AVAILABLE', 'assets/herbanet-lobby.png', 'Panoramic suite')
ON DUPLICATE KEY UPDATE status = VALUES(status), description = VALUES(description);
INSERT IGNORE INTO user_privileges (user_id, privilege_id)
SELECT u.id, p.id FROM users u
JOIN roles r ON u.role_id = r.id
JOIN privileges p ON p.code = 'MANAGE_USERS'
WHERE r.name = 'ADMIN';
