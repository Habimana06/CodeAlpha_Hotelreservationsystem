package com.hotel.ui.screens.dashboards;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.AbstractBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import com.hotel.config.DatabaseConfig;
import com.hotel.model.CustomerMessage;
import com.hotel.model.Reservation;
import com.hotel.model.Room;
import com.hotel.model.User;
import com.hotel.repository.HibernateReservationRepository;
import com.hotel.repository.HibernateRoomRepository;
import com.hotel.repository.HibernateUserRepository;
import com.hotel.repository.HibernateCustomerMessageRepository;
import com.hotel.service.HibernateReservationService;
import com.hotel.service.HibernateRoomService;
import com.hotel.service.PaymentService;
import com.hotel.ui.components.RoundedPanel;

public class CustomerDashboard extends JPanel {
    private final User user;
    private final HibernateRoomService roomService;
    private final HibernateReservationService reservationService;
    private final PaymentService paymentService;
    private final DatabaseConfig config;
    private final HibernateCustomerMessageRepository messageRepository;
    
    // Enhanced color scheme - Pure Black and Green
    private static final Color PURE_BLACK = new Color(0, 0, 0);
    private static final Color DARK_BLACK = new Color(10, 10, 10);
    private static final Color ACCENT_GREEN = new Color(34, 197, 94);
    private static final Color ACCENT_GREEN_HOVER = new Color(22, 163, 74);
    private static final Color ACCENT_GREEN_LIGHT = new Color(34, 197, 94, 30);
    private static final Color SOFT_WHITE = new Color(248, 250, 252);
    private static final Color CARD_WHITE = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(226, 232, 240);
    private static final Color TEXT_PRIMARY = new Color(0, 0, 0);
    private static final Color TEXT_SECONDARY = new Color(100, 116, 139);
    private static final Color SUCCESS_GREEN = new Color(34, 197, 94);
    private static final Color WARNING_ORANGE = new Color(251, 146, 60);
    private static final Color ERROR_RED = new Color(239, 68, 68);
    private static final Color DEEP_NAVY = new Color(15, 23, 42);
    private static final Color ACCENT_GOLD = new Color(212, 175, 55);
    private static final Color ACCENT_GOLD_HOVER = new Color(184, 153, 48);
    
    private JTable reservationTable;
    private DefaultTableModel reservationTableModel;
    private JTextField checkInField;
    private JTextField checkOutField;
    private JSpinner guestSpinner;
    private JPanel roomsContainer;
    private List<Room> allAvailableRooms = new ArrayList<>(); // All available rooms
    private List<Room> currentAvailableRooms = new ArrayList<>(); // Current page rooms
    private int currentPage = 0;
    private int roomsPerPage = 9; // 3 columns x 3 rows = 9 rooms per page
    private int currentRoomIndex = 0;
    private JButton prevRoomBtn;
    private JButton nextRoomBtn;
    private JLabel roomCounterLabel;
    private JPanel statsPanel; // Reference to stats panel for updates

    private Runnable onLogout;
    
    public CustomerDashboard(User user, HibernateUserRepository userRepository, DatabaseConfig config) {
        this(user, userRepository, config, null);
    }
    
    public CustomerDashboard(User user, HibernateUserRepository userRepository, DatabaseConfig config, Runnable onLogout) {
        this.user = user;
        this.config = config;
        this.onLogout = onLogout;
        this.messageRepository = new HibernateCustomerMessageRepository();
        this.roomService = new HibernateRoomService(new HibernateRoomRepository());
        this.reservationService = new HibernateReservationService(
                new HibernateReservationRepository(), new HibernateRoomRepository());
        this.paymentService = new PaymentService(
                new com.hotel.repository.HibernatePaymentRepository(),
                new HibernateReservationRepository());
        
        setLayout(new BorderLayout());
        setBackground(SOFT_WHITE);
        initializeUI();
        loadReservations();
    }

    private void bookRoomDirectly(Room room, LocalDate checkIn, LocalDate checkOut) {
        try {
            // Default to 1 guest if spinner is not available
            int guests = 1;
            if (guestSpinner != null) {
                guests = (Integer) guestSpinner.getValue();
            }
            
            double amount = reservationService.calculateStayCost(room, checkIn, checkOut);
            long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
            
            String message = String.format(
                "<html><body style='width: 350px; padding: 10px;'>" +
                "<h2 style='color: #1e293b; margin-bottom: 15px;'>Booking Confirmation</h2>" +
                "<p><b>Room:</b> %s - %s</p>" +
                "<p><b>Check-in:</b> %s</p>" +
                "<p><b>Check-out:</b> %s</p>" +
                "<p><b>Guests:</b> %d</p>" +
                "<p><b>Duration:</b> %d night%s</p>" +
                "<p style='font-size: 18px; margin-top: 15px;'><b>Total Cost:</b> <span style='color: %s;'>$%.2f</span></p>" +
                "</body></html>",
                room.getRoomNumber(), room.getCategory().getName(),
                checkIn.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                checkOut.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                guests, nights, nights == 1 ? "" : "s", 
                String.format("#%02x%02x%02x", ACCENT_GREEN.getRed(), ACCENT_GREEN.getGreen(), ACCENT_GREEN.getBlue()),
                amount
            );
            
            int confirm = JOptionPane.showConfirmDialog(this, message, 
                "Confirm Booking", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            
            if (confirm != JOptionPane.YES_OPTION) return;
            
            Reservation reservation = new Reservation();
            reservation.setCheckIn(checkIn);
            reservation.setCheckOut(checkOut);
            reservation.setGuestCount(guests);
            reservation.setStatus("CONFIRMED");
            reservation.setUser(user);
            reservation.setRoom(room);

            Reservation saved = new HibernateReservationRepository().save(reservation);
            new HibernateRoomRepository().updateStatus(room.getId(), "RESERVED");

            paymentService.processPayment(saved.getId(), amount, "CREDIT_CARD");

            showSuccessDialog("Room booked successfully! Booking confirmation #" + saved.getId());
            loadReservations();
            displayAllRooms(); // Refresh room cards instead of searching
            updateStatsPanel(); // Update stats panel after booking
        } catch (Exception e) {
            showModernDialog("Error booking room: " + e.getMessage(), "Booking Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadReservations() {
        List<Reservation> reservations = reservationService.findCustomerReservations(user.getId());
        reservationTableModel.setRowCount(0);

        for (Reservation res : reservations) {
            long nights = ChronoUnit.DAYS.between(res.getCheckIn(), res.getCheckOut());
            double cost = reservationService.calculateStayCost(res.getRoom(), res.getCheckIn(), res.getCheckOut());
            Object[] row = {
                "#" + res.getId(),
                res.getRoom().getRoomNumber(),
                res.getCheckIn().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                res.getCheckOut().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                res.getGuestCount(),
                nights + (nights == 1 ? " night" : " nights"),
                String.format("$%.2f", cost),
                res.getStatus(),
                res.getId(), // Store ID for view details
                res.getId() // Store ID for cancellation
            };
            reservationTableModel.addRow(row);
        }
        
        // Update stats
        updateStats();
    }
    
    private void updateStats() {
        SwingUtilities.invokeLater(() -> {
            // Refresh stats panel if needed
            revalidate();
            repaint();
        });
    }
    
    private void updateStatsPanel() {
        if (statsPanel != null) {
            // Remove old stats panel
            Component parent = statsPanel.getParent();
            if (parent != null) {
                ((Container) parent).remove(statsPanel);
            }
            
            // Create new stats panel with updated data
            statsPanel = createStatsPanel();
            
            // Add it back to the main content
            if (parent instanceof JPanel) {
                ((JPanel) parent).add(statsPanel, BorderLayout.NORTH);
                parent.revalidate();
                parent.repaint();
            }
        }
    }


    private void cancelReservation(int row) {
        try {
            long reservationId = (Long) reservationTableModel.getValueAt(row, 9);
            
            int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to cancel this reservation?",
                "Cancel Reservation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            
            if (confirm != JOptionPane.YES_OPTION) return;
            
            Reservation res = new HibernateReservationRepository().findById(reservationId).orElse(null);
            if (res != null) {
                reservationService.cancel(reservationId, res.getRoom().getId());
                showSuccessDialog("Reservation cancelled successfully");
                loadReservations();
            }
        } catch (Exception e) {
            showModernDialog("Error cancelling reservation: " + e.getMessage(), "Cancellation Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void viewReservationDetails(int row) {
        try {
            long reservationId = (Long) reservationTableModel.getValueAt(row, 8);
            Reservation res = new HibernateReservationRepository().findById(reservationId).orElse(null);
            if (res == null) {
                showModernDialog("Reservation not found", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            Room room = res.getRoom();
            long nights = ChronoUnit.DAYS.between(res.getCheckIn(), res.getCheckOut());
            double cost = reservationService.calculateStayCost(room, res.getCheckIn(), res.getCheckOut());
            
            String roomDescription = room.getDescription() != null && !room.getDescription().isEmpty() 
                ? room.getDescription() 
                : "A comfortable and well-appointed room for your stay.";
            
            String message = String.format(
                "<html><body style='width: 500px; padding: 20px; font-family: Segoe UI;'>" +
                "<h2 style='color: #1e293b; margin-bottom: 20px; border-bottom: 2px solid #22c55e; padding-bottom: 10px;'>" +
                "📋 Reservation Details</h2>" +
                "<div style='background: #f8fafc; padding: 15px; border-radius: 8px; margin-bottom: 15px;'>" +
                "<h3 style='color: #0f172a; margin-top: 0;'>Reservation Information</h3>" +
                "<p><b>Reservation #:</b> %d</p>" +
                "<p><b>Status:</b> <span style='color: %s; font-weight: bold;'>%s</span></p>" +
                "<p><b>Check-in:</b> %s</p>" +
                "<p><b>Check-out:</b> %s</p>" +
                "<p><b>Duration:</b> %d night%s</p>" +
                "<p><b>Number of Guests:</b> %d</p>" +
                "<p><b>Total Cost:</b> <span style='color: #22c55e; font-size: 18px; font-weight: bold;'>$%.2f</span></p>" +
                "</div>" +
                "<div style='background: #f8fafc; padding: 15px; border-radius: 8px;'>" +
                "<h3 style='color: #0f172a; margin-top: 0;'>🏨 Room Information</h3>" +
                "<p><b>Room Number:</b> %s</p>" +
                "<p><b>Category:</b> %s</p>" +
                "<p><b>Floor:</b> Floor %d</p>" +
                "<p><b>View:</b> %s</p>" +
                "<p><b>Nightly Rate:</b> $%.2f</p>" +
                "<p><b>Description:</b> %s</p>" +
                "<p><b>Room Status:</b> %s</p>" +
                "</div>" +
                "</body></html>",
                res.getId(),
                res.getStatus().equals("CONFIRMED") ? "#22c55e" : 
                res.getStatus().equals("CHECKED_IN") ? "#22c55e" : 
                res.getStatus().equals("CANCELLED") ? "#ef4444" : "#64748b",
                res.getStatus(),
                res.getCheckIn().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                res.getCheckOut().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                nights, nights == 1 ? "" : "s",
                res.getGuestCount(),
                cost,
                room.getRoomNumber(),
                room.getCategory().getName(),
                room.getFloor(),
                room.getViewType() != null ? room.getViewType() : "Standard",
                room.getNightlyRate(),
                roomDescription,
                room.getStatus()
            );
            
            JOptionPane.showMessageDialog(this, message, "Reservation & Room Details", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            showModernDialog("Error loading reservation details: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private JPanel createCustomerSupportPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(SOFT_WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        
        // Header
        JLabel title = new JLabel("💬 Customer Support");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(TEXT_PRIMARY);
        panel.add(title, BorderLayout.NORTH);
        
        // Scrollable main content with two columns
        JPanel contentPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        contentPanel.setBackground(SOFT_WHITE);
        
        // Left: Contact Information
        RoundedPanel contactCard = new RoundedPanel(20);
        contactCard.setBackground(CARD_WHITE);
        contactCard.setLayout(new BorderLayout(0, 20));
        contactCard.setBorder(BorderFactory.createCompoundBorder(
            new ShadowBorder(),
            BorderFactory.createEmptyBorder(30, 30, 30, 30)
        ));
        
        JLabel contactTitle = new JLabel("📞 Contact Information");
        contactTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        contactTitle.setForeground(TEXT_PRIMARY);
        contactCard.add(contactTitle, BorderLayout.NORTH);
        
        JPanel contactInfo = new JPanel();
        contactInfo.setLayout(new BoxLayout(contactInfo, BoxLayout.Y_AXIS));
        contactInfo.setOpaque(false);
        
        addContactItem(contactInfo, "🏨 Receptionist", "Receptionist", "+1 (555) 123-4567", "Available 24/7");
        addContactItem(contactInfo, "🧹 Housekeeping", "Housekeeping", "+1 (555) 123-4568", "Available 6 AM - 10 PM");
        addContactItem(contactInfo, "🍽️ Room Service", "Waiter", "+1 (555) 123-4569", "Available 6 AM - 11 PM");
        addContactItem(contactInfo, "🚗 Concierge", "Driver", "+1 (555) 123-4570", "Available 7 AM - 9 PM");
        addContactItem(contactInfo, "🆘 Emergency", "Emergency", "+1 (555) 911", "Available 24/7");
        
        contactCard.add(contactInfo, BorderLayout.CENTER);
        contentPanel.add(contactCard);
        
        // Right: Message Support
        RoundedPanel messageCard = new RoundedPanel(20);
        messageCard.setBackground(CARD_WHITE);
        messageCard.setLayout(new BorderLayout(0, 20));
        messageCard.setBorder(BorderFactory.createCompoundBorder(
            new ShadowBorder(),
            BorderFactory.createEmptyBorder(30, 30, 30, 30)
        ));
        
        JLabel messageTitle = new JLabel("✉️ Send Message");
        messageTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        messageTitle.setForeground(TEXT_PRIMARY);
        messageCard.add(messageTitle, BorderLayout.NORTH);
        
        JPanel messageForm = new JPanel();
        messageForm.setLayout(new BoxLayout(messageForm, BoxLayout.Y_AXIS));
        messageForm.setOpaque(false);
        
        JLabel subjectLabel = new JLabel("Subject:");
        subjectLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        subjectLabel.setForeground(TEXT_PRIMARY);
        subjectLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JTextField subjectField = createModernTextField("");
        subjectField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        subjectField.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel messageLabel = new JLabel("Message:");
        messageLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        messageLabel.setForeground(TEXT_PRIMARY);
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JTextArea messageArea = new JTextArea(8, 30);
        messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        JScrollPane messageScroll = new JScrollPane(messageArea);
        messageScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        messageScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JButton sendBtn = createModernButton("📤 Send Message", ACCENT_GREEN);
        sendBtn.setPreferredSize(new Dimension(0, 45));
        sendBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        sendBtn.addActionListener(e -> {
            if (subjectField.getText().trim().isEmpty() || messageArea.getText().trim().isEmpty()) {
                showModernDialog("Please fill in both subject and message fields.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            // Save message to database
            try {
                CustomerMessage message = new CustomerMessage();
                message.setUser(user);
                message.setSubject(subjectField.getText().trim());
                message.setMessage(messageArea.getText().trim());
                message.setStatus("NEW");
                messageRepository.save(message);
                
                showSuccessDialog("Your message has been sent to customer support. We'll respond within 24 hours.");
                subjectField.setText("");
                messageArea.setText("");
            } catch (Exception ex) {
                showModernDialog("Failed to send message. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        messageForm.add(subjectLabel);
        messageForm.add(Box.createVerticalStrut(8));
        messageForm.add(subjectField);
        messageForm.add(Box.createVerticalStrut(15));
        messageForm.add(messageLabel);
        messageForm.add(Box.createVerticalStrut(8));
        messageForm.add(messageScroll);
        messageForm.add(Box.createVerticalStrut(15));
        messageForm.add(sendBtn);
        
        messageCard.add(messageForm, BorderLayout.CENTER);
        contentPanel.add(messageCard);
        
        // Make content scrollable
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        scrollPane.getViewport().setBackground(SOFT_WHITE);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void addContactItem(JPanel panel, String icon, String title, String number, String hours) {
        JPanel item = new JPanel(new BorderLayout(15, 0));
        item.setOpaque(false);
        item.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        iconLabel.setPreferredSize(new Dimension(40, 40));
        
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(ACCENT_GREEN);
        
        JLabel numberLabel = new JLabel(number);
        numberLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        numberLabel.setForeground(TEXT_PRIMARY);
        
        JLabel hoursLabel = new JLabel(hours);
        hoursLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        hoursLabel.setForeground(TEXT_SECONDARY);
        
        textPanel.add(titleLabel);
        textPanel.add(numberLabel);
        textPanel.add(hoursLabel);
        
        item.add(iconLabel, BorderLayout.WEST);
        item.add(textPanel, BorderLayout.CENTER);
        
        panel.add(item);
        panel.add(Box.createVerticalStrut(5));
    }
    
    private JPanel createHotelInfoPanel() {
        // Main container panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(SOFT_WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        
        // Hotel profile card
        RoundedPanel hotelCard = new RoundedPanel(24);
        hotelCard.setBackground(CARD_WHITE);
        hotelCard.setLayout(new BorderLayout(0, 20));
        hotelCard.setBorder(BorderFactory.createCompoundBorder(
            new ShadowBorder(),
            BorderFactory.createEmptyBorder(40, 40, 40, 40)
        ));
        hotelCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        
        // Hotel header with improved icon design
        JPanel headerPanel = new JPanel(new BorderLayout(0, 15));
        headerPanel.setOpaque(false);
        
        // Enhanced icon with background circle
        RoundedPanel iconContainer = new RoundedPanel(50);
        iconContainer.setBackground(new Color(ACCENT_GOLD.getRed(), ACCENT_GOLD.getGreen(), ACCENT_GOLD.getBlue(), 20));
        iconContainer.setLayout(new BorderLayout());
        iconContainer.setPreferredSize(new Dimension(80, 80));
        iconContainer.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JLabel hotelIcon = new JLabel("🏨");
        hotelIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 50));
        hotelIcon.setHorizontalAlignment(SwingConstants.CENTER);
        iconContainer.add(hotelIcon, BorderLayout.CENTER);
        
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        
        String hotelName = config.getHotelName();
        JLabel hotelNameLabel = new JLabel(hotelName);
        hotelNameLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        hotelNameLabel.setForeground(TEXT_PRIMARY);
        
        String tagline = config.getTagline();
        JLabel taglineLabel = new JLabel(tagline);
        taglineLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        taglineLabel.setForeground(ACCENT_GOLD);
        
        titlePanel.add(hotelNameLabel);
        titlePanel.add(Box.createVerticalStrut(5));
        titlePanel.add(taglineLabel);
        
        headerPanel.add(iconContainer, BorderLayout.WEST);
        headerPanel.add(titlePanel, BorderLayout.CENTER);
        
        hotelCard.add(headerPanel, BorderLayout.NORTH);
        
        // Hotel description
        JLabel description = new JLabel("<html><div style='width: 650px; line-height: 1.8; color: #64748b; font-size: 15px;'>" +
            "Welcome to " + hotelName + "! We are committed to providing exceptional hospitality and unforgettable experiences. " +
            "Our hotel offers luxurious accommodations, world-class amenities, and personalized service to ensure your stay is nothing short of perfect. " +
            "Whether you're traveling for business or leisure, we strive to exceed your expectations and create lasting memories.<br><br>" +
            "<b style='color: #1e293b;'>Our Features:</b><br>" +
            "✨ Elegant and spacious rooms with modern amenities<br>" +
            "✨ Stunning views and premium locations<br>" +
            "✨ 24/7 concierge service<br>" +
            "✨ State-of-the-art facilities<br>" +
            "✨ Fine dining and room service<br>" +
            "✨ Business center and meeting rooms<br>" +
            "✨ Fitness center and spa facilities</div></html>");
        description.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        hotelCard.add(description, BorderLayout.CENTER);
        
        mainPanel.add(hotelCard);
        mainPanel.add(Box.createVerticalStrut(25));
        
        // System description card
        RoundedPanel systemCard = new RoundedPanel(24);
        systemCard.setBackground(CARD_WHITE);
        systemCard.setLayout(new BorderLayout(0, 20));
        systemCard.setBorder(BorderFactory.createCompoundBorder(
            new ShadowBorder(),
            BorderFactory.createEmptyBorder(40, 40, 40, 40)
        ));
        systemCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        
        // Enhanced system icon
        JPanel systemHeader = new JPanel(new BorderLayout(0, 10));
        systemHeader.setOpaque(false);
        
        RoundedPanel systemIconContainer = new RoundedPanel(40);
        systemIconContainer.setBackground(new Color(59, 130, 246, 20));
        systemIconContainer.setLayout(new BorderLayout());
        systemIconContainer.setPreferredSize(new Dimension(60, 60));
        systemIconContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel systemIcon = new JLabel("💻");
        systemIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 35));
        systemIcon.setHorizontalAlignment(SwingConstants.CENTER);
        systemIconContainer.add(systemIcon, BorderLayout.CENTER);
        
        JLabel systemTitle = new JLabel("Hotel Reservation System");
        systemTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        systemTitle.setForeground(TEXT_PRIMARY);
        
        systemHeader.add(systemIconContainer, BorderLayout.WEST);
        systemHeader.add(systemTitle, BorderLayout.CENTER);
        systemCard.add(systemHeader, BorderLayout.NORTH);
        
        JLabel systemDescription = new JLabel("<html><div style='width: 650px; line-height: 1.8; color: #64748b; font-size: 15px;'>" +
            "Our Hotel Reservation System is a comprehensive platform designed to streamline your booking experience. " +
            "This system allows you to:<br><br>" +
            "<b style='color: #1e293b;'>🔍 Search & Book:</b> Easily search for available rooms by date and guest count, view detailed room information, and make instant reservations.<br><br>" +
            "<b style='color: #1e293b;'>📋 Manage Reservations:</b> View all your bookings, check reservation details including room information, check-in/check-out dates, and manage your upcoming stays.<br><br>" +
            "<b style='color: #1e293b;'>💰 Transparent Pricing:</b> See real-time pricing, calculate total costs based on your stay duration, and make secure payments.<br><br>" +
            "<b style='color: #1e293b;'>📊 Booking History:</b> Track all your past and current reservations, view booking statistics, and monitor your spending.<br><br>" +
            "<b style='color: #1e293b;'>🎯 User-Friendly Interface:</b> Enjoy a modern, intuitive interface that makes booking and managing reservations effortless.<br><br>" +
            "Our system ensures a seamless experience from search to checkout, giving you complete control over your hotel reservations.</div></html>");
        systemDescription.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        systemCard.add(systemDescription, BorderLayout.CENTER);
        
        mainPanel.add(systemCard);
        mainPanel.add(Box.createVerticalGlue());
        
        // Wrap in scroll pane
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(SOFT_WHITE);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        // Style the scrollbar
        scrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(200, 200, 200);
                this.trackColor = SOFT_WHITE;
            }
            
            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }
            
            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }
            
            private JButton createZeroButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                button.setMinimumSize(new Dimension(0, 0));
                button.setMaximumSize(new Dimension(0, 0));
                return button;
            }
        });
        
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(SOFT_WHITE);
        wrapper.add(scrollPane, BorderLayout.CENTER);
        
        return wrapper;
    }
    
    private void showProfileDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "My Profile", true);
        dialog.setSize(450, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(SOFT_WHITE);
        
        RoundedPanel profilePanel = new RoundedPanel(20);
        profilePanel.setBackground(CARD_WHITE);
        profilePanel.setLayout(new BorderLayout(0, 25));
        profilePanel.setBorder(BorderFactory.createCompoundBorder(
            new ShadowBorder(),
            BorderFactory.createEmptyBorder(40, 40, 40, 40)
        ));
        
        // Header with icon
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        headerPanel.setOpaque(false);
        
        RoundedPanel iconPanel = new RoundedPanel(50);
        iconPanel.setBackground(ACCENT_GREEN_LIGHT);
        iconPanel.setPreferredSize(new Dimension(80, 80));
        iconPanel.setLayout(new BorderLayout());
        
        JLabel iconLabel = new JLabel("👤");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setForeground(ACCENT_GREEN);
        iconPanel.add(iconLabel, BorderLayout.CENTER);
        
        JLabel title = new JLabel("Profile Information");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(TEXT_PRIMARY);
        
        headerPanel.add(iconPanel);
        headerPanel.add(title);
        profilePanel.add(headerPanel, BorderLayout.NORTH);
        
        // Profile details
        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setOpaque(false);
        
        addProfileField(detailsPanel, "Name", user.getDisplayName().isEmpty() ? user.getFirstName() + " " + user.getLastName() : user.getDisplayName());
        addProfileField(detailsPanel, "Username", user.getUsername());
        addProfileField(detailsPanel, "Email", user.getEmail());
        addProfileField(detailsPanel, "Role", user.getRole().getName());
        addProfileField(detailsPanel, "Status", user.isActive() ? "Active" : "Inactive");
        
        profilePanel.add(detailsPanel, BorderLayout.CENTER);
        
        // Close button
        JButton closeBtn = createModernButton("Close", TEXT_SECONDARY);
        closeBtn.setPreferredSize(new Dimension(120, 42));
        closeBtn.addActionListener(e -> dialog.dispose());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(closeBtn);
        profilePanel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(profilePanel, BorderLayout.CENTER);
        dialog.setVisible(true);
    }
    
    private void addProfileField(JPanel panel, String label, String value) {
        JPanel fieldPanel = new JPanel(new BorderLayout(0, 5));
        fieldPanel.setOpaque(false);
        fieldPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        
        JLabel labelText = new JLabel(label + ":");
        labelText.setFont(new Font("Segoe UI", Font.BOLD, 14));
        labelText.setForeground(TEXT_SECONDARY);
        
        JLabel valueText = new JLabel(value);
        valueText.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        valueText.setForeground(TEXT_PRIMARY);
        
        fieldPanel.add(labelText, BorderLayout.NORTH);
        fieldPanel.add(valueText, BorderLayout.CENTER);
        
        panel.add(fieldPanel);
    }
    
    // UI Helper Methods
    
    private JTextField createModernTextField(String placeholder) {
        JTextField field = new JTextField(15);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ACCENT_GREEN, 2, true),
                    BorderFactory.createEmptyBorder(11, 14, 11, 14)
                ));
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                    BorderFactory.createEmptyBorder(12, 15, 12, 15)
                ));
            }
        });
        
        return field;
    }
    
    private void styleSpinner(JSpinner spinner) {
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
            textField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)
            ));
        }
    }
    
    private JPanel createModernFormField(String label, JComponent component) {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbl.setForeground(TEXT_PRIMARY);
        
        panel.add(lbl, BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JButton createModernButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 15));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        Color hoverColor = bgColor.equals(ACCENT_GREEN) ? ACCENT_GREEN_HOVER : bgColor.darker();
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(hoverColor);
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });
        
        return button;
    }
    
    private void styleModernTable(JTable table) {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setRowHeight(60);
        table.setGridColor(BORDER_COLOR);
        table.setSelectionBackground(ACCENT_GREEN_LIGHT);
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        
        // Header styling
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(PURE_BLACK);
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 50));
        header.setBorder(BorderFactory.createEmptyBorder());
        
        // Center align numeric columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(new ModernCellRenderer());
        }
    }
    
    private void showModernDialog(String message, String title, int messageType) {
        JOptionPane.showMessageDialog(this, message, title, messageType);
    }
    
    private void showSuccessDialog(String message) {
        JOptionPane.showMessageDialog(this, message, "Success 🎉", JOptionPane.INFORMATION_MESSAGE);
    }
    
    // Custom Renderers
    
    private class ModernCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            if (!isSelected) {
                c.setBackground(row % 2 == 0 ? CARD_WHITE : SOFT_WHITE);
            }
            
            setFont(new Font("Segoe UI", Font.PLAIN, 14));
            setForeground(TEXT_PRIMARY);
            setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
            
            return c;
        }
    }
    
    private class StatusBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            JLabel label = new JLabel(value != null ? value.toString() : "");
            label.setOpaque(true);
            label.setFont(new Font("Segoe UI", Font.BOLD, 12));
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
            
            String status = value != null ? value.toString() : "";
            
            switch (status) {
                case "CONFIRMED":
                    label.setBackground(new Color(SUCCESS_GREEN.getRed(), SUCCESS_GREEN.getGreen(), SUCCESS_GREEN.getBlue(), 30));
                    label.setForeground(SUCCESS_GREEN);
                    label.setText("✓ " + status);
                    break;
                case "CHECKED_IN":
                    label.setBackground(ACCENT_GREEN_LIGHT);
                    label.setForeground(ACCENT_GREEN);
                    label.setText("🏨 " + status);
                    break;
                case "CANCELLED":
                    label.setBackground(new Color(ERROR_RED.getRed(), ERROR_RED.getGreen(), ERROR_RED.getBlue(), 30));
                    label.setForeground(ERROR_RED);
                    label.setText("✗ " + status);
                    break;
                default:
                    label.setBackground(new Color(TEXT_SECONDARY.getRed(), TEXT_SECONDARY.getGreen(), TEXT_SECONDARY.getBlue(), 30));
                    label.setForeground(TEXT_SECONDARY);
            }
            
            if (!isSelected) {
                JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
                wrapper.setBackground(row % 2 == 0 ? CARD_WHITE : SOFT_WHITE);
                wrapper.add(label);
                return wrapper;
            }
            
            return label;
        }
    }
    
    private class ModernButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ModernButtonRenderer() {
            setOpaque(true);
            setBorderPainted(false);
            setFocusPainted(false);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            
            setText("✗ Cancel");
            setBackground(ERROR_RED);
            
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            return this;
        }
    }

    private class ViewDetailsButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ViewDetailsButtonRenderer() {
            setOpaque(true);
            setBorderPainted(false);
            setFocusPainted(false);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            setText("👁️ View Details");
            setBackground(new Color(59, 130, 246));
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            return this;
        }
    }
    
    private class ViewDetailsButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private boolean isPushed;
        private int selectedRow;

        public ViewDetailsButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.addActionListener(e -> fireEditingStopped());
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            button.setText("👁️ View Details");
            button.setBackground(new Color(59, 130, 246));
            button.setForeground(Color.WHITE);
            button.setFont(new Font("Segoe UI", Font.BOLD, 13));
            
            isPushed = true;
            selectedRow = row;
            return button;
        }

        public Object getCellEditorValue() {
            if (isPushed) {
                viewReservationDetails(selectedRow);
            }
            isPushed = false;
            return "";
        }

        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }
    
    private class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private boolean isPushed;
        private int selectedRow;

        public ButtonEditor(JCheckBox checkBox, boolean isBooking) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.addActionListener(e -> fireEditingStopped());
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            button.setText("✗ Cancel");
            button.setBackground(ERROR_RED);
            button.setForeground(Color.WHITE);
            button.setFont(new Font("Segoe UI", Font.BOLD, 13));
            
            isPushed = true;
            selectedRow = row;
            return button;
        }

        public Object getCellEditorValue() {
            if (isPushed) {
                cancelReservation(selectedRow);
            }
            isPushed = false;
            return "";
        }

        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }
    
    // Shadow border for cards
    private static class ShadowBorder extends AbstractBorder {
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2d.setColor(new Color(0, 0, 0, 8));
            g2d.fillRoundRect(x + 2, y + 2, width - 4, height - 4, 20, 20);
            
            g2d.dispose();
        }
        
        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(2, 2, 2, 2);
        }
    }

    private void initializeUI() {
        // Modern header with gradient
        JPanel header = createModernHeader();
        add(header, BorderLayout.NORTH);

        // Main content area
        JPanel mainContent = new JPanel(new BorderLayout(0, 20));
        mainContent.setBackground(SOFT_WHITE);
        mainContent.setBorder(BorderFactory.createEmptyBorder(20, 30, 30, 30));
        
        // Tabbed content with modern styling (stats removed for more space)
        JTabbedPane tabs = createModernTabs();
        mainContent.add(tabs, BorderLayout.CENTER);
        
        add(mainContent, BorderLayout.CENTER);
    }

    private JPanel createModernHeader() {
        JPanel header = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Solid black background like in the image
                g2d.setColor(PURE_BLACK);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        header.setOpaque(true);
        header.setBackground(PURE_BLACK);
        header.setLayout(new BorderLayout());
        header.setPreferredSize(new Dimension(0, 130));
        header.setBorder(BorderFactory.createEmptyBorder(25, 40, 25, 40));

        // Left side - Welcome message
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);
        
        JLabel greeting = new JLabel("Welcome back,");
        greeting.setFont(new Font("Segoe UI", Font.PLAIN, 17));
        greeting.setForeground(new Color(200, 200, 200)); // Light grey - more visible
        greeting.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        String displayName = user.getDisplayName().isEmpty() ? user.getUsername() : user.getDisplayName();
        JLabel userName = new JLabel(displayName);
        userName.setFont(new Font("Segoe UI", Font.BOLD, 36));
        userName.setForeground(new Color(230, 230, 230)); // Bright light grey - clearly visible
        userName.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        String hotelName = config.getHotelName();
        JLabel subtitle = new JLabel(hotelName + " - Guest Portal");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(ACCENT_GREEN);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        leftPanel.add(greeting);
        leftPanel.add(Box.createVerticalStrut(4));
        leftPanel.add(userName);
        leftPanel.add(Box.createVerticalStrut(4));
        leftPanel.add(subtitle);
        
        header.add(leftPanel, BorderLayout.WEST);

        // Right side - Action buttons
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        rightPanel.setOpaque(false);
        
        JButton profileBtn = createHeaderButton("👤 Profile", false);
        profileBtn.addActionListener(e -> showProfileDialog());
        
        JButton logoutBtn = createHeaderButton("🚪 Logout", true);
        logoutBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to logout?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (onLogout != null) {
                    onLogout.run();
                } else {
                    // Fallback to System.exit if no callback provided
                    System.exit(0);
                }
            }
        });
        
        rightPanel.add(profileBtn);
        rightPanel.add(logoutBtn);
        
        header.add(rightPanel, BorderLayout.EAST);

        return header;
    }
    
    private JButton createHeaderButton(String text, boolean isPrimary) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 15));
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        if (isPrimary) {
            // Logout button - bright green background
            button.setBackground(ACCENT_GREEN);
            button.setForeground(Color.WHITE);
            button.setPreferredSize(new Dimension(140, 45));
            button.setMinimumSize(new Dimension(140, 45));
        } else {
            // Profile button - dark grey background with white text
            button.setBackground(new Color(60, 60, 60)); // Dark grey
            button.setForeground(Color.WHITE);
            button.setPreferredSize(new Dimension(130, 45));
            button.setMinimumSize(new Dimension(130, 45));
        }
        
        // Hover effect
        Color originalBg = button.getBackground();
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isPrimary) {
                    button.setBackground(ACCENT_GREEN_HOVER);
                } else {
                    button.setBackground(new Color(80, 80, 80)); // Lighter grey on hover
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(originalBg);
            }
        });
        
        return button;
    }
    
    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 20, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        
        // Calculate stats
        List<Reservation> allReservations = reservationService.findCustomerReservations(user.getId());
        int activeReservations = (int) allReservations.stream()
            .filter(r -> r.getStatus().equals("CONFIRMED") || r.getStatus().equals("CHECKED_IN"))
            .count();
        
        double totalSpent = allReservations.stream()
            .mapToDouble(r -> reservationService.calculateStayCost(r.getRoom(), r.getCheckIn(), r.getCheckOut()))
            .sum();
        
        // Stats cards - using proper icon rendering
        panel.add(createStatCard("📊", "Total Bookings", String.valueOf(allReservations.size()), SUCCESS_GREEN));
        panel.add(createStatCard("✅", "Active Reservations", String.valueOf(activeReservations), ACCENT_GREEN));
        panel.add(createStatCard("💰", "Total Spent", String.format("$%.2f", totalSpent), SUCCESS_GREEN));
        
        return panel;
    }
    
    private JPanel createStatCard(String icon, String label, String value, Color accentColor) {
        RoundedPanel card = new RoundedPanel(20);
        card.setBackground(CARD_WHITE);
        card.setLayout(new BorderLayout(15, 10));
        card.setBorder(BorderFactory.createCompoundBorder(
            new ShadowBorder(),
            BorderFactory.createEmptyBorder(25, 25, 25, 25)
        ));
        
        // Enhanced icon with circular background - properly sized
        RoundedPanel iconContainer = new RoundedPanel(50);
        iconContainer.setBackground(ACCENT_GREEN_LIGHT);
        iconContainer.setLayout(new BorderLayout());
        iconContainer.setPreferredSize(new Dimension(80, 80));
        iconContainer.setMinimumSize(new Dimension(80, 80));
        iconContainer.setMaximumSize(new Dimension(80, 80));
        iconContainer.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        
        JLabel iconLabel = new JLabel(icon) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(80, 80);
            }
        };
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setVerticalAlignment(SwingConstants.CENTER);
        iconLabel.setForeground(accentColor);
        iconLabel.setOpaque(false);
        iconContainer.add(iconLabel, BorderLayout.CENTER);
        
        card.add(iconContainer, BorderLayout.WEST);
        
        // Text content
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        
        JLabel labelText = new JLabel(label);
        labelText.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        labelText.setForeground(TEXT_SECONDARY);
        labelText.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel valueText = new JLabel(value);
        valueText.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueText.setForeground(TEXT_PRIMARY);
        valueText.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        textPanel.add(labelText);
        textPanel.add(Box.createVerticalStrut(5));
        textPanel.add(valueText);
        
        card.add(textPanel, BorderLayout.CENTER);
        
        return card;
    }

    private JTabbedPane createModernTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 15));
        tabs.setBackground(SOFT_WHITE);
        tabs.setForeground(TEXT_PRIMARY);
        tabs.setPreferredSize(new Dimension(0, Integer.MAX_VALUE)); // Allow tabs to expand
        
        // Custom tab styling
        tabs.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
            @Override
            protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                    int x, int y, int w, int h, boolean isSelected) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (isSelected) {
                    g2d.setColor(ACCENT_GREEN);
                } else {
                    g2d.setColor(CARD_WHITE);
                }
                g2d.fillRoundRect(x, y, w, h - 5, 12, 12);
            }
            
            @Override
            protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics,
                    int tabIndex, String title, Rectangle textRect, boolean isSelected) {
                g.setFont(font);
                g.setColor(isSelected ? Color.WHITE : TEXT_PRIMARY);
                g.drawString(title, textRect.x, textRect.y + metrics.getAscent());
            }
        });
        
        tabs.addTab("🏨 Hotel Info", createHotelInfoPanel());
        tabs.addTab("🔍 Search & Book", createSearchPanel());
        tabs.addTab("📋 My Reservations", createReservationsPanel());
        tabs.addTab("💬 Customer Support", createCustomerSupportPanel());
        
        return tabs;
    }

    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(SOFT_WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        // Title panel
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titlePanel.setOpaque(false);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        
        JLabel titleIcon = new JLabel("🏨");
        titleIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        
        JLabel formTitle = new JLabel(" Find Your Perfect Room");
        formTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        formTitle.setForeground(TEXT_PRIMARY);
        
        titlePanel.add(titleIcon);
        titlePanel.add(formTitle);
        
        panel.add(titlePanel, BorderLayout.NORTH);

        // Room cards container
        JPanel roomsContainer = new JPanel();
        roomsContainer.setLayout(new BoxLayout(roomsContainer, BoxLayout.Y_AXIS));
        roomsContainer.setBackground(SOFT_WHITE);
        roomsContainer.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        
        // Create a scrollable panel for room cards with increased height
        JScrollPane scrollPane = new JScrollPane(roomsContainer);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            new ShadowBorder(),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        scrollPane.getViewport().setBackground(SOFT_WHITE);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(0, 700)); // Increased height for Search & Book tab
        
        // Store reference to rooms container for updating
        this.roomsContainer = roomsContainer;
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Footer with navigation controls
        JPanel footerPanel = createRoomsNavigationFooter();
        panel.add(footerPanel, BorderLayout.SOUTH);
        

        displayAllRooms();

        return panel;
    }
    
    private JPanel createRoomsNavigationFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(CARD_WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR),
            BorderFactory.createEmptyBorder(15, 30, 15, 30)
        ));
        
        // Navigation buttons panel
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        navPanel.setOpaque(false);
        
        prevRoomBtn = createModernButton("← Previous", TEXT_SECONDARY);
        prevRoomBtn.setPreferredSize(new Dimension(140, 45));
        prevRoomBtn.setEnabled(false);
        prevRoomBtn.addActionListener(e -> navigateToPreviousRoom());
        
        roomCounterLabel = new JLabel("0 / 0");
        roomCounterLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        roomCounterLabel.setForeground(TEXT_PRIMARY);
        
        nextRoomBtn = createModernButton("Next →", TEXT_SECONDARY);
        nextRoomBtn.setPreferredSize(new Dimension(140, 45));
        nextRoomBtn.setEnabled(false);
        nextRoomBtn.addActionListener(e -> navigateToNextRoom());
        
        navPanel.add(prevRoomBtn);
        navPanel.add(roomCounterLabel);
        navPanel.add(nextRoomBtn);
        
        footer.add(navPanel, BorderLayout.CENTER);
        
        return footer;
    }
    
    private void navigateToPreviousRoom() {
        if (currentPage > 0) {
            currentPage--;
            currentRoomIndex = 0;
            displayCurrentPage();
            // Scroll to top
            SwingUtilities.invokeLater(() -> {
                if (roomsContainer.getParent() instanceof JScrollPane) {
                    JScrollPane scrollPane = (JScrollPane) roomsContainer.getParent();
                    scrollPane.getVerticalScrollBar().setValue(0);
                }
            });
        }
    }
    
    private void navigateToNextRoom() {
        int totalPages = (int) Math.ceil((double) allAvailableRooms.size() / roomsPerPage);
        if (currentPage < totalPages - 1) {
            currentPage++;
            currentRoomIndex = 0;
            displayCurrentPage();
            // Scroll to top
            SwingUtilities.invokeLater(() -> {
                if (roomsContainer.getParent() instanceof JScrollPane) {
                    JScrollPane scrollPane = (JScrollPane) roomsContainer.getParent();
                    scrollPane.getVerticalScrollBar().setValue(0);
                }
            });
        }
    }
    
    private void updateRoomNavigation() {
        if (roomCounterLabel != null && prevRoomBtn != null && nextRoomBtn != null) {
            int totalPages = (int) Math.ceil((double) allAvailableRooms.size() / roomsPerPage);
            int totalRooms = allAvailableRooms.size();
            int currentPageDisplay = currentPage + 1;
            
            // Show page info: "Page 1 / 3 (7 rooms)"
            if (totalPages > 0) {
                roomCounterLabel.setText("Page " + currentPageDisplay + " / " + totalPages + 
                    " (" + currentAvailableRooms.size() + " rooms)");
            } else {
                roomCounterLabel.setText("0 / 0");
            }
            
            prevRoomBtn.setEnabled(currentPage > 0);
            nextRoomBtn.setEnabled(currentPage < totalPages - 1);
        }
    }
    
    private void displayAllRooms() {
        try {
            // Get all available rooms
            List<Room> allRooms = roomService.getAllRooms();
            // Filter to only show available rooms (no limit - get all)
            allAvailableRooms = allRooms.stream()
                .filter(room -> "AVAILABLE".equals(room.getStatus()))
                .collect(Collectors.toList());
            
            currentPage = 0; // Reset to first page
            currentRoomIndex = 0;
            
            // Display current page
            displayCurrentPage();
        } catch (Exception e) {
            showModernDialog("Error loading rooms: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void displayCurrentPage() {
        // Clear existing cards
        roomsContainer.removeAll();
        roomsContainer.revalidate();
        roomsContainer.repaint();

        if (allAvailableRooms.isEmpty()) {
            JLabel noRoomsLabel = new JLabel("<html><div style='text-align: center; padding: 40px; color: #64748b; font-size: 16px;'>" +
                "No rooms available at the moment.<br>Please check back later.</div></html>");
            noRoomsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            roomsContainer.add(noRoomsLabel);
            roomsContainer.revalidate();
            updateRoomNavigation();
            return;
        }

        // Calculate pagination
        int totalPages = (int) Math.ceil((double) allAvailableRooms.size() / roomsPerPage);
        int startIndex = currentPage * roomsPerPage;
        int endIndex = Math.min(startIndex + roomsPerPage, allAvailableRooms.size());
        
        // Get rooms for current page
        currentAvailableRooms = allAvailableRooms.subList(startIndex, endIndex);

        // Create room cards in a responsive grid layout (3 columns for better use of space)
        int cols = 3;
        int rows = (int) Math.ceil((double) currentAvailableRooms.size() / cols);
        JPanel gridPanel = new JPanel(new GridLayout(rows, cols, 20, 20));
        gridPanel.setBackground(SOFT_WHITE);
        gridPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        
        // Use default dates for display (today and tomorrow)
        LocalDate defaultCheckIn = LocalDate.now();
        LocalDate defaultCheckOut = LocalDate.now().plusDays(1);
        
        for (Room room : currentAvailableRooms) {
            JPanel roomCard = createRoomCard(room, defaultCheckIn, defaultCheckOut);
            gridPanel.add(roomCard);
        }
        
        // Add empty cells if needed to maintain grid layout
        int totalCells = rows * cols;
        int emptyCells = totalCells - currentAvailableRooms.size();
        for (int i = 0; i < emptyCells; i++) {
            JPanel emptyPanel = new JPanel();
            emptyPanel.setOpaque(false);
            gridPanel.add(emptyPanel);
        }
        
        roomsContainer.add(gridPanel);
        roomsContainer.revalidate();
        roomsContainer.repaint();
        
        // Update navigation footer
        updateRoomNavigation();
    }

    private JPanel createReservationsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(SOFT_WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(25, 0, 0, 0));

        // Header with refresh button
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        JLabel title = new JLabel("📋 My Reservations");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(TEXT_PRIMARY);
        
        JButton refreshBtn = createModernButton("🔄 Refresh", new Color(100, 116, 139));
        refreshBtn.setPreferredSize(new Dimension(120, 40));
        refreshBtn.addActionListener(e -> loadReservations());
        
        headerPanel.add(title, BorderLayout.WEST);
        headerPanel.add(refreshBtn, BorderLayout.EAST);
        
        panel.add(headerPanel, BorderLayout.NORTH);

        // Modern reservations table
        String[] columns = {"Reservation #", "Room", "Check-in", "Check-out", "Guests", "Nights", "Total Cost", "Status", "View Details", "Action"};
        reservationTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 8 || column == 9;
            }
        };
        reservationTable = new JTable(reservationTableModel);
        styleModernTable(reservationTable);
        
        reservationTable.getColumn("View Details").setCellRenderer(new ViewDetailsButtonRenderer());
        reservationTable.getColumn("View Details").setCellEditor(new ViewDetailsButtonEditor(new JCheckBox()));
        
        reservationTable.getColumn("Action").setCellRenderer(new ModernButtonRenderer());
        reservationTable.getColumn("Action").setCellEditor(new ButtonEditor(new JCheckBox(), false));
        
        // Custom renderer for status column
        reservationTable.getColumn("Status").setCellRenderer(new StatusBadgeRenderer());

        JScrollPane scrollPane = new JScrollPane(reservationTable);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            new ShadowBorder(),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        scrollPane.getViewport().setBackground(CARD_WHITE);
        
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void searchRooms() {
        try {
            String checkInText = checkInField.getText().trim();
            String checkOutText = checkOutField.getText().trim();
            
            if (checkInText.isEmpty() || checkOutText.isEmpty()) {
                showModernDialog("Please enter both check-in and check-out dates.\nFormat: YYYY-MM-DD (e.g., 2024-12-25)", 
                    "Missing Dates", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            LocalDate checkIn = LocalDate.parse(checkInText);
            LocalDate checkOut = LocalDate.parse(checkOutText);
            
            if (checkIn.isBefore(LocalDate.now())) {
                showModernDialog("Check-in date cannot be in the past.", "Invalid Date", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            if (!checkOut.isAfter(checkIn)) {
                showModernDialog("Check-out date must be after check-in date", "Invalid Dates", JOptionPane.WARNING_MESSAGE);
                return;
            }

            List<Room> rooms = roomService.searchAvailableRooms(checkIn, checkOut);
            currentAvailableRooms = rooms;
            
            // Clear existing cards
            roomsContainer.removeAll();
            roomsContainer.revalidate();
            roomsContainer.repaint();

            if (rooms.isEmpty()) {
                JLabel noRoomsLabel = new JLabel("<html><div style='text-align: center; padding: 40px; color: #64748b; font-size: 16px;'>" +
                    "No rooms available for the selected dates.<br>Please try different dates.</div></html>");
                noRoomsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                roomsContainer.add(noRoomsLabel);
                roomsContainer.revalidate();
                return;
            }

            // Create room cards in a responsive grid layout (3 columns)
            int cols = Math.min(3, rooms.size());
            JPanel gridPanel = new JPanel(new GridLayout(0, cols, 20, 20));
            gridPanel.setBackground(SOFT_WHITE);
            gridPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
            
            for (Room room : rooms) {
                JPanel roomCard = createRoomCard(room, checkIn, checkOut);
                gridPanel.add(roomCard);
            }
            
            roomsContainer.add(gridPanel);
            roomsContainer.revalidate();
            roomsContainer.repaint();
        } catch (java.time.format.DateTimeParseException e) {
            showModernDialog("Invalid date format. Please use YYYY-MM-DD format.\nExample: 2024-12-25", 
                "Invalid Date Format", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            showModernDialog("Error searching rooms: " + e.getMessage(), "Search Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private JPanel createRoomCard(Room room, LocalDate checkIn, LocalDate checkOut) {
        RoundedPanel card = new RoundedPanel(20);
        card.setBackground(CARD_WHITE);
        card.setLayout(new BorderLayout(0, 0));
        card.setBorder(BorderFactory.createCompoundBorder(
            new ShadowBorder(),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        // Smaller, resizable cards - flexible sizing
        card.setPreferredSize(new Dimension(280, 300));
        card.setMinimumSize(new Dimension(250, 280));
        // No maximum size constraint to allow resizing
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Room image panel - load from URL if available with enhanced styling
        JPanel imagePanel = new JPanel() {
            private Image roomImage = null;
            private boolean imageLoaded = false;
            
            {
                // Load image asynchronously
                if (room.getPhotoUrl() != null && !room.getPhotoUrl().isEmpty()) {
                    new Thread(() -> {
                        try {
                            if (room.getPhotoUrl().startsWith("http")) {
                                java.net.URL url = new java.net.URL(room.getPhotoUrl());
                                roomImage = javax.imageio.ImageIO.read(url);
                                imageLoaded = true;
                                SwingUtilities.invokeLater(() -> repaint());
                            } else {
                                // Local file
                                java.io.File file = new java.io.File(room.getPhotoUrl());
                                if (file.exists()) {
                                    roomImage = javax.imageio.ImageIO.read(file);
                                    imageLoaded = true;
                                    SwingUtilities.invokeLater(() -> repaint());
                                }
                            }
                        } catch (Exception e) {
                            // If image loading fails, use placeholder
                            roomImage = null;
                            imageLoaded = false;
                        }
                    }).start();
                }
            }
            
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                
                if (roomImage != null && imageLoaded) {
                    // Draw loaded image with smooth scaling
                    int imgWidth = roomImage.getWidth(this);
                    int imgHeight = roomImage.getHeight(this);
                    int panelWidth = getWidth();
                    int panelHeight = getHeight();
                    
                    // Scale to cover while maintaining aspect ratio
                    double scale = Math.max((double)panelWidth / imgWidth, (double)panelHeight / imgHeight);
                    int scaledWidth = (int)(imgWidth * scale);
                    int scaledHeight = (int)(imgHeight * scale);
                    int x = (panelWidth - scaledWidth) / 2;
                    int y = (panelHeight - scaledHeight) / 2;
                    
                    g2d.drawImage(roomImage, x, y, scaledWidth, scaledHeight, this);
                    
                    // Add subtle overlay
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
                    g2d.setColor(PURE_BLACK);
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                } else {
                    // Enhanced gradient background placeholder
                    GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(34, 197, 94, 40),
                        getWidth(), getHeight(), new Color(34, 197, 94, 80)
                    );
                    g2d.setPaint(gradient);
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                    
                    // Room icon with shadow
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
                    g2d.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 70));
                    FontMetrics fm = g2d.getFontMetrics();
                    String icon = "🏨";
                    int x = (getWidth() - fm.stringWidth(icon)) / 2;
                    int y = (getHeight() + fm.getAscent()) / 2;
                    g2d.setColor(PURE_BLACK);
                    g2d.drawString(icon, x + 2, y + 2);
                    
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                    g2d.setColor(ACCENT_GREEN);
                    g2d.drawString(icon, x, y);
                }
            }
        };
        imagePanel.setPreferredSize(new Dimension(0, 140));
        imagePanel.setBackground(ACCENT_GREEN_LIGHT);
        
        // Add overlay with room number badge
        JPanel imageWrapper = new JPanel(new BorderLayout());
        imageWrapper.setOpaque(false);
        imageWrapper.add(imagePanel, BorderLayout.CENTER);
        
        // Room number badge
        RoundedPanel badge = new RoundedPanel(15);
        badge.setBackground(new Color(0, 0, 0, 180));
        badge.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        JLabel badgeLabel = new JLabel("Room " + room.getRoomNumber());
        badgeLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        badgeLabel.setForeground(Color.WHITE);
        badge.add(badgeLabel);
        imageWrapper.add(badge, BorderLayout.SOUTH);
        
        card.add(imageWrapper, BorderLayout.NORTH);
        
        // Room info panel with enhanced styling
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        
        // Category badge with enhanced design
        String categoryName = room.getCategory() != null ? room.getCategory().getName() : "Standard";
        RoundedPanel categoryBadge = new RoundedPanel(12);
        categoryBadge.setBackground(ACCENT_GREEN_LIGHT);
        categoryBadge.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        categoryBadge.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        categoryBadge.setMaximumSize(new Dimension(150, 30));
        categoryBadge.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel categoryLabel = new JLabel(categoryName);
        categoryLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        categoryLabel.setForeground(ACCENT_GREEN);
        categoryBadge.add(categoryLabel);
        
        infoPanel.add(categoryBadge);
        infoPanel.add(Box.createVerticalStrut(12));
        
        // Room title
        JLabel roomTitle = new JLabel("Room " + room.getRoomNumber());
        roomTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        roomTitle.setForeground(TEXT_PRIMARY);
        roomTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(roomTitle);
        infoPanel.add(Box.createVerticalStrut(8));
        
        // Description with line clamping
        String description = room.getDescription();
        if (description == null || description.isEmpty()) {
            description = categoryName.equalsIgnoreCase("Premium") || categoryName.equalsIgnoreCase("Deluxe") 
                ? "Luxurious room with premium amenities and stunning views." 
                : "Comfortable and well-appointed room for your stay.";
        }
        if (description.length() > 120) {
            description = description.substring(0, 117) + "...";
        }
        
        JLabel descLabel = new JLabel("<html><div style='width: 350px; color: #64748b; font-size: 13px; line-height: 1.6;'>" + 
            description + "</div></html>");
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(descLabel);
        infoPanel.add(Box.createVerticalStrut(15));
        
        // Features row
        JPanel featuresPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        featuresPanel.setOpaque(false);
        featuresPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        if (room.getViewType() != null && !room.getViewType().isEmpty()) {
            addFeatureTag(featuresPanel, "🌄 " + room.getViewType());
        }
        addFeatureTag(featuresPanel, "📍 Floor " + room.getFloor());
        
        infoPanel.add(featuresPanel);
        infoPanel.add(Box.createVerticalStrut(18));
        
        // Price section with divider
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setForeground(BORDER_COLOR);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(separator);
        infoPanel.add(Box.createVerticalStrut(18));
        
        JPanel pricePanel = new JPanel(new BorderLayout());
        pricePanel.setOpaque(false);
        pricePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel priceLeft = new JPanel();
        priceLeft.setLayout(new BoxLayout(priceLeft, BoxLayout.Y_AXIS));
        priceLeft.setOpaque(false);
        
        JLabel priceLabel = new JLabel("$" + String.format("%.0f", room.getNightlyRate()));
        priceLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        priceLabel.setForeground(ACCENT_GREEN);
        priceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel perNightLabel = new JLabel("per night");
        perNightLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        perNightLabel.setForeground(TEXT_SECONDARY);
        perNightLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        priceLeft.add(priceLabel);
        priceLeft.add(perNightLabel);
        
        pricePanel.add(priceLeft, BorderLayout.WEST);
        infoPanel.add(pricePanel);
        
        card.add(infoPanel, BorderLayout.CENTER);
        
        // View Details button with enhanced hover effect
        JButton viewBtn = createModernButton("View Full Details", ACCENT_GREEN);
        viewBtn.setPreferredSize(new Dimension(0, 50));
        viewBtn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        viewBtn.addActionListener(e -> showRoomDetailsModal(room, checkIn, checkOut, 
            currentAvailableRooms.indexOf(room), currentAvailableRooms.size()));
        
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
        buttonPanel.add(viewBtn, BorderLayout.CENTER);
        
        card.add(buttonPanel, BorderLayout.SOUTH);
        
        // Enhanced card hover effect
        MouseAdapter cardHoverListener = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ACCENT_GREEN, 2, true),
                    BorderFactory.createEmptyBorder(0, 0, 0, 0)
                ));
                card.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                card.setBorder(BorderFactory.createCompoundBorder(
                    new ShadowBorder(),
                    BorderFactory.createEmptyBorder(0, 0, 0, 0)
                ));
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                // Update current room index when card is clicked
                int roomIndex = currentAvailableRooms.indexOf(room);
                if (roomIndex >= 0) {
                    currentRoomIndex = roomIndex;
                    updateRoomNavigation();
                }
                showRoomDetailsModal(room, checkIn, checkOut, 
                    roomIndex, currentAvailableRooms.size());
            }
        };
        
        card.addMouseListener(cardHoverListener);
        
        return card;
    }
    
    private void addFeatureTag(JPanel panel, String text) {
        RoundedPanel tag = new RoundedPanel(8);
        tag.setBackground(new Color(248, 250, 252));
        tag.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(TEXT_SECONDARY);
        tag.add(label);
        
        panel.add(tag);
    }
    
    private void showRoomDetailsModal(Room room, LocalDate checkIn, LocalDate checkOut, int currentIndex, int totalRooms) {
        JFrame frame = new JFrame("Room Details - " + room.getRoomNumber());
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(1000, 900);
        frame.setLocationRelativeTo(this);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(SOFT_WHITE);
        frame.setResizable(true);
        frame.setMinimumSize(new Dimension(800, 600));
        
        // Container for current room details
        final JPanel[] contentContainer = new JPanel[1];
        final int[] currentIndexRef = {currentIndex};
        
        // Create content panel
        contentContainer[0] = createRoomDetailsContent(room, checkIn, checkOut);
        
        // Scrollable content
        JScrollPane scrollPane = new JScrollPane(contentContainer[0]);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        scrollPane.getViewport().setBackground(SOFT_WHITE);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        frame.add(scrollPane, BorderLayout.CENTER);
        
        // Book button panel at bottom
        JPanel bookPanel = new JPanel(new BorderLayout());
        bookPanel.setBackground(CARD_WHITE);
        bookPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR),
            BorderFactory.createEmptyBorder(20, 30, 20, 30)
        ));
        
        JButton bookBtn = createModernButton("📅 Book This Room", ACCENT_GREEN);
        bookBtn.setPreferredSize(new Dimension(250, 50));
        bookBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        bookBtn.addActionListener(e -> {
            frame.dispose();
            bookRoomDirectly(room, checkIn, checkOut);
        });
        
        JPanel buttonWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonWrapper.setOpaque(false);
        buttonWrapper.add(bookBtn);
        bookPanel.add(buttonWrapper, BorderLayout.CENTER);
        
        frame.add(bookPanel, BorderLayout.SOUTH);
        
        frame.setVisible(true);
    }
    
    private JPanel createRoomDetailsContent(Room room, LocalDate checkIn, LocalDate checkOut) {
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setBackground(SOFT_WHITE);
        mainContent.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        
        // Room image section - load from URL if available with enhanced full-width design
        JPanel imageSection = new JPanel() {
            private Image roomImage = null;
            private boolean imageLoaded = false;
            
            {
                // Load image asynchronously
                if (room.getPhotoUrl() != null && !room.getPhotoUrl().isEmpty()) {
                    new Thread(() -> {
                        try {
                            if (room.getPhotoUrl().startsWith("http")) {
                                java.net.URL url = new java.net.URL(room.getPhotoUrl());
                                roomImage = javax.imageio.ImageIO.read(url);
                                imageLoaded = true;
                                SwingUtilities.invokeLater(() -> repaint());
                            } else {
                                // Local file
                                java.io.File file = new java.io.File(room.getPhotoUrl());
                                if (file.exists()) {
                                    roomImage = javax.imageio.ImageIO.read(file);
                                    imageLoaded = true;
                                    SwingUtilities.invokeLater(() -> repaint());
                                }
                            }
                        } catch (Exception e) {
                            roomImage = null;
                            imageLoaded = false;
                        }
                    }).start();
                }
            }
            
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                
                if (roomImage != null && imageLoaded) {
                    // Draw loaded image with cover scaling
                    int imgWidth = roomImage.getWidth(this);
                    int imgHeight = roomImage.getHeight(this);
                    int panelWidth = getWidth();
                    int panelHeight = getHeight();
                    
                    double scale = Math.max((double)panelWidth / imgWidth, (double)panelHeight / imgHeight);
                    int scaledWidth = (int)(imgWidth * scale);
                    int scaledHeight = (int)(imgHeight * scale);
                    int x = (panelWidth - scaledWidth) / 2;
                    int y = (panelHeight - scaledHeight) / 2;
                    
                    g2d.drawImage(roomImage, x, y, scaledWidth, scaledHeight, this);
                    
                    // Dark overlay at bottom for text readability
                    GradientPaint overlayGradient = new GradientPaint(
                        0, getHeight() - 150, new Color(0, 0, 0, 0),
                        0, getHeight(), new Color(0, 0, 0, 180)
                    );
                    g2d.setPaint(overlayGradient);
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                } else {
                    // Enhanced gradient with pattern
                    GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(34, 197, 94, 50),
                        getWidth(), getHeight(), new Color(34, 197, 94, 100)
                    );
                    g2d.setPaint(gradient);
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                    
                    // Pattern overlay
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));
                    g2d.setColor(PURE_BLACK);
                    for (int i = 0; i < getWidth(); i += 30) {
                        for (int j = 0; j < getHeight(); j += 30) {
                            g2d.fillOval(i, j, 3, 3);
                        }
                    }
                    
                    // Large centered icon
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                    g2d.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 120));
                    FontMetrics fm = g2d.getFontMetrics();
                    String icon = "🏨";
                    int iconX = (getWidth() - fm.stringWidth(icon)) / 2;
                    int iconY = (getHeight() + fm.getAscent()) / 2;
                    
                    // Shadow
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
                    g2d.setColor(PURE_BLACK);
                    g2d.drawString(icon, iconX + 3, iconY + 3);
                    
                    // Icon
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                    g2d.setColor(ACCENT_GREEN);
                    g2d.drawString(icon, iconX, iconY);
                }
            }
        };
        imageSection.setPreferredSize(new Dimension(0, 400));
        imageSection.setMaximumSize(new Dimension(Integer.MAX_VALUE, 400));
        imageSection.setBackground(ACCENT_GREEN_LIGHT);
        mainContent.add(imageSection);
        
        // Content wrapper with padding
        JPanel contentWrapper = new JPanel();
        contentWrapper.setLayout(new BoxLayout(contentWrapper, BoxLayout.Y_AXIS));
        contentWrapper.setBackground(SOFT_WHITE);
        contentWrapper.setBorder(BorderFactory.createEmptyBorder(35, 50, 35, 50));
        
        // Header section with room info - improved design
        JPanel headerSection = new JPanel(new BorderLayout(0, 12));
        headerSection.setOpaque(false);
        headerSection.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        
        // Room number and category
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        topRow.setOpaque(false);
        
        JLabel roomNumberLabel = new JLabel("Room " + room.getRoomNumber());
        roomNumberLabel.setFont(new Font("Segoe UI", Font.BOLD, 40));
        roomNumberLabel.setForeground(TEXT_PRIMARY);
        
        String categoryName = room.getCategory() != null ? room.getCategory().getName() : "Standard";
        RoundedPanel categoryBadge = new RoundedPanel(18);
        categoryBadge.setBackground(ACCENT_GREEN_LIGHT);
        categoryBadge.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        JLabel categoryLabel = new JLabel(categoryName);
        categoryLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        categoryLabel.setForeground(ACCENT_GREEN);
        categoryBadge.add(categoryLabel);
        
        topRow.add(roomNumberLabel);
        topRow.add(categoryBadge);
        titlePanel.add(topRow);
        titlePanel.add(Box.createVerticalStrut(10));
        
        // Room ID and status
        JLabel roomIdLabel = new JLabel("Room ID: #" + room.getId() + " • " + room.getStatus());
        roomIdLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        roomIdLabel.setForeground(TEXT_SECONDARY);
        titlePanel.add(roomIdLabel);
        
        headerSection.add(titlePanel, BorderLayout.WEST);
        
        // Price on the right - improved styling
        JPanel pricePanel = new JPanel();
        pricePanel.setLayout(new BoxLayout(pricePanel, BoxLayout.Y_AXIS));
        pricePanel.setOpaque(false);
        
        JLabel priceLabel = new JLabel("$" + String.format("%.0f", room.getNightlyRate()));
        priceLabel.setFont(new Font("Segoe UI", Font.BOLD, 48));
        priceLabel.setForeground(ACCENT_GREEN);
        priceLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        
        JLabel perNightLabel = new JLabel("per night");
        perNightLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        perNightLabel.setForeground(TEXT_SECONDARY);
        perNightLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        
        pricePanel.add(priceLabel);
        pricePanel.add(perNightLabel);
        headerSection.add(pricePanel, BorderLayout.EAST);
        
        contentWrapper.add(headerSection);
        contentWrapper.add(Box.createVerticalStrut(30));
        
        // Description section - improved design
        RoundedPanel descriptionCard = new RoundedPanel(18);
        descriptionCard.setBackground(CARD_WHITE);
        descriptionCard.setLayout(new BorderLayout(0, 18));
        descriptionCard.setBorder(BorderFactory.createCompoundBorder(
            new ShadowBorder(),
            BorderFactory.createEmptyBorder(28, 30, 28, 30)
        ));
        descriptionCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        
        JLabel descTitle = new JLabel("📝 Description");
        descTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        descTitle.setForeground(TEXT_PRIMARY);
        
        String description = room.getDescription();
        if (description == null || description.isEmpty()) {
            description = "A comfortable and well-appointed room designed for your ultimate relaxation and convenience.";
        }
        
        JLabel descLabel = new JLabel("<html><div style='color: #64748b; font-size: 16px; line-height: 1.9;'>" + 
            description + "</div></html>");
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        
        descriptionCard.add(descTitle, BorderLayout.NORTH);
        descriptionCard.add(descLabel, BorderLayout.CENTER);
        contentWrapper.add(descriptionCard);
        contentWrapper.add(Box.createVerticalStrut(30));
        
        // Room specifications in grid - improved spacing (3 columns, 2 rows)
        JPanel specsGrid = new JPanel(new GridLayout(2, 3, 25, 25));
        specsGrid.setOpaque(false);
        specsGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));
        
        addEnhancedSpecCard(specsGrid, "🏢", "Floor", "Floor " + room.getFloor());
        addEnhancedSpecCard(specsGrid, "🛏️", "Room Status", room.getStatus());
        addEnhancedSpecCard(specsGrid, room.getViewType() != null ? "🌄" : "🪟", "View Type", 
            room.getViewType() != null ? room.getViewType() : "Standard");
        addEnhancedSpecCard(specsGrid, "💰", "Nightly Rate", "$" + String.format("%.2f", room.getNightlyRate()));
        if (room.getCategory() != null) {
            addEnhancedSpecCard(specsGrid, "⭐", "Category Rate", "$" + String.format("%.2f", room.getCategory().getBaseRate()));
        } else {
            addEnhancedSpecCard(specsGrid, "📋", "Room Type", categoryName);
        }
        addEnhancedSpecCard(specsGrid, "🏨", "Category", categoryName);
        
        contentWrapper.add(specsGrid);
        contentWrapper.add(Box.createVerticalStrut(25));
        
        // Amenities section - improved design
        RoundedPanel amenitiesCard = new RoundedPanel(18);
        amenitiesCard.setBackground(CARD_WHITE);
        amenitiesCard.setLayout(new BorderLayout(0, 22));
        amenitiesCard.setBorder(BorderFactory.createCompoundBorder(
            new ShadowBorder(),
            BorderFactory.createEmptyBorder(28, 30, 28, 30)
        ));
        amenitiesCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 350));
        
        JLabel amenitiesTitle = new JLabel("✨ Amenities & Features");
        amenitiesTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        amenitiesTitle.setForeground(TEXT_PRIMARY);
        
        // Parse amenities from description
        String desc = description.toLowerCase();
        JPanel amenitiesGrid = new JPanel(new GridLayout(0, 2, 15, 15));
        amenitiesGrid.setOpaque(false);
        
        if (desc.contains("wi-fi") || desc.contains("wifi") || categoryName.toLowerCase().contains("premium")) {
            addEnhancedAmenityItem(amenitiesGrid, "📶", "Free High-Speed Wi-Fi");
        }
        if (desc.contains("breakfast") || categoryName.toLowerCase().contains("deluxe")) {
            addEnhancedAmenityItem(amenitiesGrid, "🍳", "Complimentary Breakfast");
        }
        if (desc.contains("air conditioning") || desc.contains("ac")) {
            addEnhancedAmenityItem(amenitiesGrid, "❄️", "Air Conditioning");
        } else {
            addEnhancedAmenityItem(amenitiesGrid, "❄️", "Climate Control");
        }
        if (desc.contains("tv")) {
            addEnhancedAmenityItem(amenitiesGrid, "📺", "Smart TV");
        } else {
            addEnhancedAmenityItem(amenitiesGrid, "📺", "HD Television");
        }
        if (desc.contains("mini") || desc.contains("fridge")) {
            addEnhancedAmenityItem(amenitiesGrid, "🧊", "Mini Refrigerator");
        }
        if (desc.contains("safe")) {
            addEnhancedAmenityItem(amenitiesGrid, "🔐", "In-Room Safe");
        }
        addEnhancedAmenityItem(amenitiesGrid, "🛁", "Private Bathroom");
        addEnhancedAmenityItem(amenitiesGrid, "☕", "Coffee/Tea Maker");
        if (desc.contains("room service")) {
            addEnhancedAmenityItem(amenitiesGrid, "🍽️", "24/7 Room Service");
        }
        if (desc.contains("balcony") || desc.contains("terrace")) {
            addEnhancedAmenityItem(amenitiesGrid, "🌿", "Private Balcony");
        }
        
        // Add default amenities if none were found
        if (amenitiesGrid.getComponentCount() < 4) {
            addEnhancedAmenityItem(amenitiesGrid, "🧴", "Premium Toiletries");
            addEnhancedAmenityItem(amenitiesGrid, "👔", "Iron & Board");
        }
        
        amenitiesCard.add(amenitiesTitle, BorderLayout.NORTH);
        amenitiesCard.add(amenitiesGrid, BorderLayout.CENTER);
        contentWrapper.add(amenitiesCard);
        contentWrapper.add(Box.createVerticalStrut(25));
        
        // Booking summary
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        double totalCost = room.getNightlyRate() * nights;
        
        RoundedPanel summaryCard = new RoundedPanel(16);
        summaryCard.setBackground(new Color(34, 197, 94, 10));
        summaryCard.setLayout(new BorderLayout(0, 20));
        summaryCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_GREEN, 2, true),
            BorderFactory.createEmptyBorder(30, 30, 30, 30)
        ));
        summaryCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        
        JLabel summaryTitle = new JLabel("📊 Booking Summary");
        summaryTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        summaryTitle.setForeground(TEXT_PRIMARY);
        
        JPanel summaryContent = new JPanel();
        summaryContent.setLayout(new BoxLayout(summaryContent, BoxLayout.Y_AXIS));
        summaryContent.setOpaque(false);
        
        addSummaryRow(summaryContent, "Check-in:", checkIn.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")));
        addSummaryRow(summaryContent, "Check-out:", checkOut.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")));
        addSummaryRow(summaryContent, "Duration:", nights + " night" + (nights != 1 ? "s" : ""));
        addSummaryRow(summaryContent, "Rate per night:", "$" + String.format("%.2f", room.getNightlyRate()));
        
        summaryContent.add(Box.createVerticalStrut(15));
        
        // Total with emphasis
        JPanel totalPanel = new JPanel(new BorderLayout());
        totalPanel.setOpaque(false);
        totalPanel.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, ACCENT_GREEN));
        totalPanel.add(Box.createVerticalStrut(15), BorderLayout.NORTH);
        
        JPanel totalRow = new JPanel(new BorderLayout());
        totalRow.setOpaque(false);
        
        JLabel totalLabel = new JLabel("Total Cost:");
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        totalLabel.setForeground(TEXT_PRIMARY);
        
        JLabel totalAmount = new JLabel("$" + String.format("%.2f", totalCost));
        totalAmount.setFont(new Font("Segoe UI", Font.BOLD, 32));
        totalAmount.setForeground(ACCENT_GREEN);
        
        totalRow.add(totalLabel, BorderLayout.WEST);
        totalRow.add(totalAmount, BorderLayout.EAST);
        totalPanel.add(totalRow, BorderLayout.SOUTH);
        
        summaryContent.add(totalPanel);
        
        summaryCard.add(summaryTitle, BorderLayout.NORTH);
        summaryCard.add(summaryContent, BorderLayout.CENTER);
        contentWrapper.add(summaryCard);
        
        mainContent.add(contentWrapper);
        
        return mainContent;
    }
    
    private void addEnhancedSpecCard(JPanel panel, String icon, String label, String value) {
        RoundedPanel card = new RoundedPanel(12);
        card.setBackground(CARD_WHITE);
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(BorderFactory.createCompoundBorder(
            new ShadowBorder(),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        // Icon
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
        iconLabel.setPreferredSize(new Dimension(40, 40));
        
        // Text content
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        
        JLabel labelText = new JLabel(label);
        labelText.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        labelText.setForeground(TEXT_SECONDARY);
        labelText.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel valueText = new JLabel(value);
        valueText.setFont(new Font("Segoe UI", Font.BOLD, 18));
        valueText.setForeground(TEXT_PRIMARY);
        valueText.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        textPanel.add(labelText);
        textPanel.add(Box.createVerticalStrut(5));
        textPanel.add(valueText);
        
        card.add(iconLabel, BorderLayout.WEST);
        card.add(textPanel, BorderLayout.CENTER);
        
        panel.add(card);
    }
    
    private void addEnhancedAmenityItem(JPanel panel, String icon, String amenity) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        item.setOpaque(false);
        item.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        
        JLabel text = new JLabel(amenity);
        text.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        text.setForeground(TEXT_PRIMARY);
        
        item.add(iconLabel);
        item.add(text);
        panel.add(item);
    }
    
    private void addSummaryRow(JPanel panel, String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        
        JLabel labelText = new JLabel(label);
        labelText.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        labelText.setForeground(TEXT_SECONDARY);
        
        JLabel valueText = new JLabel(value);
        valueText.setFont(new Font("Segoe UI", Font.BOLD, 16));
        valueText.setForeground(TEXT_PRIMARY);
        
        row.add(labelText, BorderLayout.WEST);
        row.add(valueText, BorderLayout.EAST);
        
        panel.add(row);
    }
}