package com.hotel.ui.screens.dashboards;

import com.hotel.config.DatabaseConfig;
import com.hotel.model.Reservation;
import com.hotel.model.Room;
import com.hotel.model.RoomCategory;
import com.hotel.model.User;
import com.hotel.repository.HibernateReservationRepository;
import com.hotel.repository.HibernateRoomRepository;
import com.hotel.repository.HibernateUserRepository;
import com.hotel.service.HibernateReservationService;
import com.hotel.service.HibernateRoomService;
import com.hotel.ui.components.RoundedPanel;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class ManagerDashboard extends JPanel {
    private final User user;
    private final HibernateReservationService reservationService;
    private final HibernateRoomService roomService;
    private final DatabaseConfig config;
    
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
    private static final Color INFO_BLUE = new Color(59, 130, 246);
    private static final Color DEEP_NAVY = new Color(15, 23, 42);
    private static final Color ACCENT_GOLD = new Color(212, 175, 55);
    private static final Color ACCENT_GOLD_HOVER = new Color(184, 153, 48);
    
    private JTable analyticsTable;
    private DefaultTableModel analyticsTableModel;
    private JTable roomTable;
    private DefaultTableModel roomTableModel;
    private JLabel totalRevenueLabel = new JLabel("$0.00");
    private JLabel occupancyRateLabel = new JLabel("0%");
    private JLabel totalReservationsLabel = new JLabel("0");
    private Runnable onLogout;

    public ManagerDashboard(User user, HibernateUserRepository userRepository, DatabaseConfig config) {
        this(user, userRepository, config, null);
    }
    
    public ManagerDashboard(User user, HibernateUserRepository userRepository, DatabaseConfig config, Runnable onLogout) {
        this.user = user;
        this.config = config;
        this.onLogout = onLogout;
        this.reservationService = new HibernateReservationService(
                new HibernateReservationRepository(), new HibernateRoomRepository());
        this.roomService = new HibernateRoomService(new HibernateRoomRepository());
        
        setLayout(new BorderLayout());
        setBackground(SOFT_WHITE);
        initializeUI();
        loadAnalytics();
    }

    private void initializeUI() {
        JPanel header = createModernHeader();
        add(header, BorderLayout.NORTH);

        JPanel mainContent = new JPanel(new BorderLayout(0, 20));
        mainContent.setBackground(SOFT_WHITE);
        mainContent.setBorder(BorderFactory.createEmptyBorder(20, 30, 30, 30));
        
        JPanel statsPanel = createStatsPanel();
        mainContent.add(statsPanel, BorderLayout.NORTH);
        
        JTabbedPane tabs = createModernTabs();
        mainContent.add(tabs, BorderLayout.CENTER);
        
        add(mainContent, BorderLayout.CENTER);
    }
    
    private JTabbedPane createModernTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 15));
        tabs.setBackground(SOFT_WHITE);
        tabs.setForeground(TEXT_PRIMARY);
        
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
        
        tabs.addTab("📊 Analytics", createAnalyticsPanel());
        tabs.addTab("🏨 Rooms", createRoomManagementPanel());
        
        return tabs;
    }

    private JPanel createModernHeader() {
        JPanel header = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                
                GradientPaint gradient = new GradientPaint(
                    0, 0, PURE_BLACK,
                    getWidth(), 0, DARK_BLACK
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        header.setLayout(new BorderLayout());
        header.setPreferredSize(new Dimension(0, 120));
        header.setBorder(BorderFactory.createEmptyBorder(25, 40, 25, 40));

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);
        
        JLabel greeting = new JLabel("Welcome back,");
        greeting.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        greeting.setForeground(new Color(255, 255, 255, 180));
        greeting.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        String displayName = user.getDisplayName().isEmpty() ? user.getUsername() : user.getDisplayName();
        JLabel userName = new JLabel(displayName + " 👋");
        userName.setFont(new Font("Segoe UI", Font.BOLD, 32));
        userName.setForeground(Color.WHITE);
        userName.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        String hotelName = config.getHotelName();
        JLabel subtitle = new JLabel(hotelName + " - Manager Portal");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(ACCENT_GREEN);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        leftPanel.add(greeting);
        leftPanel.add(Box.createVerticalStrut(4));
        leftPanel.add(userName);
        leftPanel.add(Box.createVerticalStrut(4));
        leftPanel.add(subtitle);
        
        header.add(leftPanel, BorderLayout.WEST);

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
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        if (isPrimary) {
            button.setBackground(ACCENT_GREEN);
            button.setForeground(Color.WHITE);
        } else {
            button.setBackground(new Color(255, 255, 255, 20));
            button.setForeground(Color.WHITE);
        }
        
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(120, 42));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        Color originalBg = button.getBackground();
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isPrimary) {
                    button.setBackground(ACCENT_GREEN_HOVER);
                } else {
                    button.setBackground(new Color(255, 255, 255, 30));
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
        
        panel.add(createStatCard("💰", "Total Revenue", totalRevenueLabel, SUCCESS_GREEN));
        panel.add(createStatCard("📊", "Occupancy Rate", occupancyRateLabel, WARNING_ORANGE));
        panel.add(createStatCard("📋", "Total Reservations", totalReservationsLabel, INFO_BLUE));
        
        return panel;
    }
    
    private JPanel createStatCard(String icon, String label, JLabel valueLabel, Color accentColor) {
        RoundedPanel card = new RoundedPanel(20);
        card.setBackground(CARD_WHITE);
        card.setLayout(new BorderLayout(15, 10));
        card.setBorder(BorderFactory.createCompoundBorder(
            new ShadowBorder(),
            BorderFactory.createEmptyBorder(25, 25, 25, 25)
        ));
        
        // Enhanced icon with circular background - properly sized
        RoundedPanel iconContainer = new RoundedPanel(50);
        iconContainer.setBackground(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 30));
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
        
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        
        JLabel labelText = new JLabel(label);
        labelText.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        labelText.setForeground(TEXT_SECONDARY);
        labelText.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLabel.setForeground(TEXT_PRIMARY);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        textPanel.add(labelText);
        textPanel.add(Box.createVerticalStrut(5));
        textPanel.add(valueLabel);
        
        card.add(textPanel, BorderLayout.CENTER);
        
        return card;
    }
    
    private JPanel createAnalyticsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(SOFT_WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        JLabel title = new JLabel("📊 Analytics & Reports");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(TEXT_PRIMARY);
        
        JButton refreshBtn = createModernButton("🔄 Refresh", TEXT_SECONDARY);
        refreshBtn.setPreferredSize(new Dimension(120, 40));
        refreshBtn.addActionListener(e -> loadAnalytics());
        
        JButton downloadReportBtn = createModernButton("📥 Download", SUCCESS_GREEN);
        downloadReportBtn.setPreferredSize(new Dimension(140, 40));
        downloadReportBtn.addActionListener(e -> downloadReport());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(refreshBtn);
        buttonPanel.add(downloadReportBtn);
        
        headerPanel.add(title, BorderLayout.WEST);
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        
        String[] columns = {"Metric", "Value"};
        analyticsTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        analyticsTable = new JTable(analyticsTableModel);
        styleModernTable(analyticsTable);

        JScrollPane scrollPane = new JScrollPane(analyticsTable);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            new ShadowBorder(),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        scrollPane.getViewport().setBackground(CARD_WHITE);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }

    private void loadAnalytics() {
        SwingUtilities.invokeLater(() -> {
            try {
                List<Reservation> reservations = reservationService.findAllReservations();
                List<Room> rooms = roomService.getAllRooms();

                double totalRevenue = reservations.stream()
                        .filter(r -> r.getStatus().equals("CHECKED_OUT") || r.getStatus().equals("CONFIRMED"))
                        .mapToDouble(r -> reservationService.calculateStayCost(r.getRoom(), r.getCheckIn(), r.getCheckOut()))
                        .sum();

                long occupiedRooms = rooms.stream()
                        .filter(r -> r.getStatus().equals("OCCUPIED") || r.getStatus().equals("RESERVED"))
                        .count();
                double occupancyRate = rooms.isEmpty() ? 0 : (occupiedRooms * 100.0 / rooms.size());

                totalRevenueLabel.setText(String.format("$%.2f", totalRevenue));
                occupancyRateLabel.setText(String.format("%.1f%%", occupancyRate));
                totalReservationsLabel.setText(String.valueOf(reservations.size()));

                analyticsTableModel.setRowCount(0);
                analyticsTableModel.addRow(new Object[]{"Total Reservations", reservations.size()});
                analyticsTableModel.addRow(new Object[]{"Active Reservations", 
                        reservations.stream().filter(r -> r.getStatus().equals("CONFIRMED") || r.getStatus().equals("CHECKED_IN")).count()});
                analyticsTableModel.addRow(new Object[]{"Total Rooms", rooms.size()});
                analyticsTableModel.addRow(new Object[]{"Available Rooms", 
                        rooms.stream().filter(r -> r.getStatus().equals("AVAILABLE")).count()});
                analyticsTableModel.addRow(new Object[]{"Occupied Rooms", occupiedRooms});
                analyticsTableModel.addRow(new Object[]{"Average Revenue per Reservation", 
                        reservations.isEmpty() ? "$0.00" : String.format("$%.2f", totalRevenue / reservations.size())});
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, 
                    "Error loading analytics: " + e.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private JPanel createRoomManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(SOFT_WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(25, 0, 0, 0));
        
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        JLabel title = new JLabel("🏨 Room Management");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(TEXT_PRIMARY);
        
        JButton createRoomBtn = createModernButton("➕ Create Room", ACCENT_GREEN);
        createRoomBtn.setPreferredSize(new Dimension(150, 40));
        createRoomBtn.addActionListener(e -> showCreateRoomDialog());
        
        JButton refreshBtn = createModernButton("🔄 Refresh", TEXT_SECONDARY);
        refreshBtn.setPreferredSize(new Dimension(120, 40));
        refreshBtn.addActionListener(e -> loadRooms());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(createRoomBtn);
        buttonPanel.add(refreshBtn);
        
        headerPanel.add(title, BorderLayout.WEST);
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        
        String[] columns = {"ID", "Room Number", "Category", "Floor", "View", "Status", "Rate/Night", "Actions"};
        roomTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 7;
            }
        };
        roomTable = new JTable(roomTableModel);
        styleModernTable(roomTable);
        
        roomTable.getColumn("Actions").setCellRenderer(new ModernButtonRenderer());
        roomTable.getColumn("Actions").setCellEditor(new RoomActionEditor(new JCheckBox()));
        
        JScrollPane scrollPane = new JScrollPane(roomTable);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            new ShadowBorder(),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        scrollPane.getViewport().setBackground(CARD_WHITE);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        loadRooms();
        
        return panel;
    }
    
    private void loadRooms() {
        SwingUtilities.invokeLater(() -> {
            try {
                List<Room> rooms = roomService.getAllRooms();
                roomTableModel.setRowCount(0);
                
                for (Room room : rooms) {
                    String categoryName = "N/A";
                    String rateStr = "N/A";
                    
                    if (room.getCategory() != null) {
                        categoryName = room.getCategory().getName();
                        // Price display - showing category name as rate is not directly available
                        rateStr = categoryName + " Rate";
                    }
                    
                    double rate = room.getNightlyRate();
                    roomTableModel.addRow(new Object[]{
                        room.getId(),
                        room.getRoomNumber(),
                        categoryName,
                        room.getFloor(),
                        room.getViewType() != null ? room.getViewType() : "Standard",
                        room.getStatus(),
                        String.format("$%.2f", rate),
                        "Edit"
                    });
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, 
                    "Error loading rooms: " + e.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });
    }
    
    private JPanel createStaffManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(SOFT_WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(25, 0, 0, 0));
        
        JLabel title = new JLabel("👥 Staff Management");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(TEXT_PRIMARY);
        
        RoundedPanel infoCard = new RoundedPanel(20);
        infoCard.setBackground(CARD_WHITE);
        infoCard.setBorder(BorderFactory.createCompoundBorder(
            new ShadowBorder(),
            BorderFactory.createEmptyBorder(30, 30, 30, 30)
        ));
        infoCard.setLayout(new BoxLayout(infoCard, BoxLayout.Y_AXIS));
        
        JLabel infoLabel = new JLabel("<html><div style='width: 600px; color: #64748b;'>" +
            "Staff management features allow you to view and manage hotel staff members including receptionists and other employees.<br><br>" +
            "This section will display all staff members with their roles, contact information, and work schedules.</div></html>");
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        infoCard.add(infoLabel);
        
        JPanel mainPanel = new JPanel(new BorderLayout(0, 20));
        mainPanel.setOpaque(false);
        mainPanel.add(title, BorderLayout.NORTH);
        mainPanel.add(infoCard, BorderLayout.CENTER);
        
        panel.add(mainPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void downloadReport() {
        try {
            List<Reservation> reservations = reservationService.findAllReservations();
            List<Room> rooms = roomService.getAllRooms();
            
            StringBuilder report = new StringBuilder();
            report.append("HOTEL ANALYTICS REPORT\n");
            report.append("Generated: ").append(java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
            report.append("=".repeat(50)).append("\n\n");
            
            double totalRevenue = reservations.stream()
                    .filter(r -> r.getStatus().equals("CHECKED_OUT") || r.getStatus().equals("CONFIRMED"))
                    .mapToDouble(r -> reservationService.calculateStayCost(r.getRoom(), r.getCheckIn(), r.getCheckOut()))
                    .sum();
            
            long occupiedRooms = rooms.stream()
                    .filter(r -> r.getStatus().equals("OCCUPIED") || r.getStatus().equals("RESERVED"))
                    .count();
            double occupancyRate = rooms.isEmpty() ? 0 : (occupiedRooms * 100.0 / rooms.size());
            
            report.append("FINANCIAL SUMMARY\n");
            report.append("-".repeat(50)).append("\n");
            report.append("Total Revenue: $").append(String.format("%.2f", totalRevenue)).append("\n");
            report.append("Average Revenue per Reservation: $")
                  .append(reservations.isEmpty() ? "0.00" : String.format("%.2f", totalRevenue / reservations.size())).append("\n\n");
            
            report.append("RESERVATION STATISTICS\n");
            report.append("-".repeat(50)).append("\n");
            report.append("Total Reservations: ").append(reservations.size()).append("\n");
            report.append("Active Reservations: ")
                  .append(reservations.stream().filter(r -> r.getStatus().equals("CONFIRMED") || r.getStatus().equals("CHECKED_IN")).count()).append("\n\n");
            
            report.append("ROOM STATISTICS\n");
            report.append("-".repeat(50)).append("\n");
            report.append("Total Rooms: ").append(rooms.size()).append("\n");
            report.append("Available Rooms: ").append(rooms.stream().filter(r -> r.getStatus().equals("AVAILABLE")).count()).append("\n");
            report.append("Occupied Rooms: ").append(occupiedRooms).append("\n");
            report.append("Occupancy Rate: ").append(String.format("%.1f%%", occupancyRate)).append("\n");
            
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Report");
            fileChooser.setSelectedFile(new java.io.File("hotel_report_" + 
                java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".txt"));
            
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                java.io.File file = fileChooser.getSelectedFile();
                try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
                    writer.write(report.toString());
                    JOptionPane.showMessageDialog(this, 
                        "Report saved successfully to:\n" + file.getAbsolutePath(), 
                        "Success", 
                        JOptionPane.INFORMATION_MESSAGE);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error generating report: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
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
        
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(PURE_BLACK);
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 50));
        header.setBorder(BorderFactory.createEmptyBorder());
        
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(new ModernCellRenderer());
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
            setText("✏️ Edit");
            setBackground(ACCENT_GREEN);
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            return this;
        }
    }
    
    private class RoomActionEditor extends DefaultCellEditor {
        protected JButton button;
        private int selectedRow;

        public RoomActionEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.addActionListener(e -> fireEditingStopped());
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            button.setText("✏️ Edit");
            button.setBackground(ACCENT_GREEN);
            button.setForeground(Color.WHITE);
            button.setFont(new Font("Segoe UI", Font.BOLD, 13));
            selectedRow = row;
            return button;
        }

        public Object getCellEditorValue() {
            long roomId = (Long) roomTableModel.getValueAt(selectedRow, 0);
            showEditRoomDialog(roomId);
            return "Edit";
        }
    }
    
    private void showCreateRoomDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Create New Room", true);
        dialog.setSize(850, 750);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(SOFT_WHITE);
        
        RoundedPanel formPanel = new RoundedPanel(20);
        formPanel.setBackground(CARD_WHITE);
        formPanel.setLayout(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createCompoundBorder(
            new ShadowBorder(),
            BorderFactory.createEmptyBorder(35, 35, 35, 35)
        ));
        
        // Make form scrollable
        JScrollPane formScroll = new JScrollPane(formPanel);
        formScroll.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        formScroll.getViewport().setBackground(CARD_WHITE);
        formScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        formScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        JLabel titleLabel = new JLabel("➕ Create New Room");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(TEXT_PRIMARY);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 0, 25, 0);
        gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(titleLabel, gbc);
        
        gbc.gridwidth = 1;
        gbc.insets = new Insets(12, 12, 12, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        JTextField roomNumberField = createModernTextField("");
        JTextField floorField = createModernTextField("");
        JTextField viewTypeField = createModernTextField("");
        JTextField priceField = createModernTextField("");
        JTextArea descriptionArea = new JTextArea(4, 30);
        descriptionArea.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        descriptionArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        
        JTextArea categoryDescArea = new JTextArea(3, 30);
        categoryDescArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        categoryDescArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        categoryDescArea.setLineWrap(true);
        categoryDescArea.setWrapStyleWord(true);
        
        JComboBox<String> categoryCombo = new JComboBox<>();
        categoryCombo.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        List<RoomCategory> categories = roomService.getAllCategories();
        for (RoomCategory cat : categories) {
            categoryCombo.addItem(cat.getName());
        }
        if (categories.isEmpty()) {
            categoryCombo.addItem("Standard");
            categoryCombo.addItem("Premium");
            categoryCombo.addItem("Deluxe");
            categoryCombo.addItem("Meeting Room");
        }
        
        // Category change listener to show default amenities
        categoryCombo.addActionListener(e -> {
            String selected = (String) categoryCombo.getSelectedItem();
            if (selected != null) {
                if (selected.equalsIgnoreCase("Standard")) {
                    categoryDescArea.setText("Standard rooms come with: Free Wi-Fi, Air Conditioning, TV, and basic amenities.");
                    priceField.setText("100.00");
                } else if (selected.equalsIgnoreCase("Premium")) {
                    categoryDescArea.setText("Premium rooms come with: Free Wi-Fi, Air Conditioning, TV, Breakfast, and enhanced amenities.");
                    priceField.setText("150.00");
                } else if (selected.equalsIgnoreCase("Deluxe")) {
                    categoryDescArea.setText("Deluxe rooms come with: Free Wi-Fi, Air Conditioning, TV, Breakfast, Dinner Service, Pool Access, and luxury amenities.");
                    priceField.setText("200.00");
                } else if (selected.equalsIgnoreCase("Meeting Room")) {
                    categoryDescArea.setText("Meeting rooms come with: Free Wi-Fi, Projector, Whiteboard, Conference facilities, and catering options.");
                    priceField.setText("250.00");
                }
            }
        });
        
        // Set default for first selection
        if (categoryCombo.getItemCount() > 0) {
            categoryCombo.setSelectedIndex(0);
        }
        
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"AVAILABLE", "MAINTENANCE", "CLEANING"});
        statusCombo.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        statusCombo.setSelectedItem("AVAILABLE");
        
        // Room type selection
        JComboBox<String> roomTypeCombo = new JComboBox<>(new String[]{"Guest Room", "Meeting Room", "Conference Room", "Suite"});
        roomTypeCombo.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        
        // Image upload
        JLabel imageLabel = new JLabel("No image selected");
        imageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        imageLabel.setForeground(TEXT_SECONDARY);
        imageLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(0, 120));
        
        JButton uploadImageBtn = createModernButton("📷 Upload Room Image", TEXT_SECONDARY);
        uploadImageBtn.setPreferredSize(new Dimension(200, 40));
        final String[] imagePath = {null};
        uploadImageBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().toLowerCase().endsWith(".jpg") || 
                           f.getName().toLowerCase().endsWith(".jpeg") || 
                           f.getName().toLowerCase().endsWith(".png");
                }
                @Override
                public String getDescription() {
                    return "Image Files (*.jpg, *.jpeg, *.png)";
                }
            });
            if (fileChooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                imagePath[0] = fileChooser.getSelectedFile().getAbsolutePath();
                imageLabel.setText("Image: " + new File(imagePath[0]).getName());
                imageLabel.setForeground(SUCCESS_GREEN);
            }
        });
        
        // Amenities checkboxes
        JCheckBox breakfastCheck = new JCheckBox("Breakfast Included");
        breakfastCheck.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        breakfastCheck.setForeground(TEXT_PRIMARY);
        
        JCheckBox wifiCheck = new JCheckBox("Free Wi-Fi");
        wifiCheck.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        wifiCheck.setForeground(TEXT_PRIMARY);
        wifiCheck.setSelected(true);
        
        JCheckBox poolCheck = new JCheckBox("Swimming Pool Access");
        poolCheck.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        poolCheck.setForeground(TEXT_PRIMARY);
        
        JCheckBox dinnerCheck = new JCheckBox("Dinner Service");
        dinnerCheck.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        dinnerCheck.setForeground(TEXT_PRIMARY);
        
        JCheckBox acCheck = new JCheckBox("Air Conditioning");
        acCheck.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        acCheck.setForeground(TEXT_PRIMARY);
        acCheck.setSelected(true);
        
        JCheckBox tvCheck = new JCheckBox("TV & Entertainment");
        tvCheck.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tvCheck.setForeground(TEXT_PRIMARY);
        tvCheck.setSelected(true);
        
        int row = 1;
        gbc.gridx = 0; gbc.gridy = row++;
        formPanel.add(createFormLabel("Room Number:"), gbc);
        gbc.gridx = 1;
        formPanel.add(roomNumberField, gbc);
        
        gbc.gridx = 0; gbc.gridy = row++;
        formPanel.add(createFormLabel("Room Type:"), gbc);
        gbc.gridx = 1;
        formPanel.add(roomTypeCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = row++;
        formPanel.add(createFormLabel("Category:"), gbc);
        gbc.gridx = 1;
        formPanel.add(categoryCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = row++;
        gbc.gridwidth = 2;
        formPanel.add(createFormLabel("Category Description & Default Amenities:"), gbc);
        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 0.5;
        formPanel.add(new JScrollPane(categoryDescArea), gbc);
        
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0;
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = row++;
        formPanel.add(createFormLabel("Price per Night ($):"), gbc);
        gbc.gridx = 1;
        formPanel.add(priceField, gbc);
        
        gbc.gridx = 0; gbc.gridy = row++;
        formPanel.add(createFormLabel("Floor:"), gbc);
        gbc.gridx = 1;
        formPanel.add(floorField, gbc);
        
        gbc.gridx = 0; gbc.gridy = row++;
        formPanel.add(createFormLabel("View Type:"), gbc);
        gbc.gridx = 1;
        formPanel.add(viewTypeField, gbc);
        
        gbc.gridx = 0; gbc.gridy = row++;
        formPanel.add(createFormLabel("Status:"), gbc);
        gbc.gridx = 1;
        formPanel.add(statusCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = row++;
        gbc.gridwidth = 2;
        JPanel imagePanel = new JPanel(new BorderLayout(10, 10));
        imagePanel.setOpaque(false);
        imagePanel.add(createFormLabel("Room Image:"), BorderLayout.NORTH);
        imagePanel.add(imageLabel, BorderLayout.CENTER);
        imagePanel.add(uploadImageBtn, BorderLayout.SOUTH);
        formPanel.add(imagePanel, gbc);
        
        gbc.gridx = 0; gbc.gridy = row++;
        gbc.gridwidth = 2;
        formPanel.add(createFormLabel("Additional Description:"), gbc);
        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 0.5;
        formPanel.add(new JScrollPane(descriptionArea), gbc);
        
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        JLabel amenitiesLabel = new JLabel("Additional Amenities (check all that apply):");
        amenitiesLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        amenitiesLabel.setForeground(TEXT_PRIMARY);
        formPanel.add(amenitiesLabel, gbc);
        
        JPanel amenitiesPanel = new JPanel(new GridLayout(0, 2, 10, 5));
        amenitiesPanel.setOpaque(false);
        amenitiesPanel.add(breakfastCheck);
        amenitiesPanel.add(wifiCheck);
        amenitiesPanel.add(poolCheck);
        amenitiesPanel.add(dinnerCheck);
        amenitiesPanel.add(acCheck);
        amenitiesPanel.add(tvCheck);
        
        gbc.gridy = row++;
        formPanel.add(amenitiesPanel, gbc);
        
        dialog.add(formScroll, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        buttonPanel.setBackground(SOFT_WHITE);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JButton createBtn = createModernButton("✅ Create Room", ACCENT_GREEN);
        createBtn.setPreferredSize(new Dimension(150, 45));
        createBtn.addActionListener(e -> {
            try {
                if (roomNumberField.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, 
                        "Room number is required!", 
                        "Validation Error", 
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                // Check if room number already exists
                Optional<Room> existing = new HibernateRoomRepository().findByRoomNumber(roomNumberField.getText().trim());
                if (existing.isPresent()) {
                    JOptionPane.showMessageDialog(dialog, 
                        "Room number already exists!", 
                        "Validation Error", 
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                RoomCategory selectedCategory = null;
                String categoryName = (String) categoryCombo.getSelectedItem();
                for (RoomCategory cat : categories) {
                    if (cat.getName().equals(categoryName)) {
                        selectedCategory = cat;
                        break;
                    }
                }
                
                // Validate price
                double price = 100.0;
                try {
                    if (!priceField.getText().trim().isEmpty()) {
                        price = Double.parseDouble(priceField.getText().trim());
                        if (price <= 0) {
                            throw new NumberFormatException("Price must be greater than 0");
                        }
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(dialog, 
                        "Invalid price! Please enter a valid number.", 
                        "Validation Error", 
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                // Create category if it doesn't exist
                if (selectedCategory == null) {
                    selectedCategory = new RoomCategory();
                    selectedCategory.setName(categoryName);
                    selectedCategory.setDescription(categoryDescArea.getText().trim().isEmpty() ? 
                        "Room category: " + categoryName : categoryDescArea.getText().trim());
                    selectedCategory.setBaseRate(price);
                    selectedCategory = new HibernateRoomRepository().saveCategory(selectedCategory);
                } else {
                    // Update category description and price
                    if (!categoryDescArea.getText().trim().isEmpty()) {
                        selectedCategory.setDescription(categoryDescArea.getText().trim());
                    }
                    selectedCategory.setBaseRate(price);
                    new HibernateRoomRepository().saveCategory(selectedCategory);
                }
                
                Room newRoom = new Room();
                newRoom.setRoomNumber(roomNumberField.getText().trim());
                newRoom.setCategory(selectedCategory);
                newRoom.setFloor(Integer.parseInt(floorField.getText().trim().isEmpty() ? "1" : floorField.getText().trim()));
                newRoom.setViewType(viewTypeField.getText().trim().isEmpty() ? "Standard" : viewTypeField.getText().trim());
                newRoom.setStatus((String) statusCombo.getSelectedItem());
                
                // Set image path if uploaded
                if (imagePath[0] != null && !imagePath[0].isEmpty()) {
                    newRoom.setPhotoUrl(imagePath[0]);
                }
                
                // Build description with category info
                StringBuilder fullDescription = new StringBuilder();
                if (!descriptionArea.getText().trim().isEmpty()) {
                    fullDescription.append(descriptionArea.getText().trim());
                }
                
                // Add category description
                if (!categoryDescArea.getText().trim().isEmpty()) {
                    if (fullDescription.length() > 0) fullDescription.append(" ");
                    fullDescription.append(categoryDescArea.getText().trim());
                }
                
                newRoom.setDescription(fullDescription.toString());
                
                // Build amenities description
                StringBuilder amenitiesDesc = new StringBuilder();
                if (breakfastCheck.isSelected()) amenitiesDesc.append("Breakfast, ");
                if (wifiCheck.isSelected()) amenitiesDesc.append("Wi-Fi, ");
                if (poolCheck.isSelected()) amenitiesDesc.append("Pool Access, ");
                if (dinnerCheck.isSelected()) amenitiesDesc.append("Dinner Service, ");
                if (acCheck.isSelected()) amenitiesDesc.append("AC, ");
                if (tvCheck.isSelected()) amenitiesDesc.append("TV");
                
                if (amenitiesDesc.length() > 0 && amenitiesDesc.charAt(amenitiesDesc.length() - 1) == ' ') {
                    amenitiesDesc.setLength(amenitiesDesc.length() - 2);
                }
                
                if (newRoom.getDescription() == null || newRoom.getDescription().isEmpty()) {
                    newRoom.setDescription("Room type: " + roomTypeCombo.getSelectedItem() + 
                                         (amenitiesDesc.length() > 0 ? ". Amenities: " + amenitiesDesc.toString() : ""));
                } else {
                    newRoom.setDescription(newRoom.getDescription() + 
                                         (amenitiesDesc.length() > 0 ? ". Amenities: " + amenitiesDesc.toString() : ""));
                }
                
                new HibernateRoomRepository().save(newRoom);
                JOptionPane.showMessageDialog(dialog, 
                    "Room created successfully! 🎉", 
                    "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                loadRooms();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, 
                    "Invalid floor number!", 
                    "Validation Error", 
                    JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, 
                    "Error: " + ex.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        JButton cancelBtn = createModernButton("Cancel", TEXT_SECONDARY);
        cancelBtn.setPreferredSize(new Dimension(120, 45));
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(cancelBtn);
        buttonPanel.add(createBtn);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setVisible(true);
    }
    
    private JTextField createModernTextField(String text) {
        JTextField field = new JTextField(text, 15);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        return field;
    }
    
    private JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(TEXT_PRIMARY);
        return label;
    }
    
    private void showEditRoomDialog(long roomId) {
        Room room = roomService.getRoomById(roomId);
        if (room == null) {
            JOptionPane.showMessageDialog(this, "Room not found", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Room", true);
        dialog.setSize(850, 750);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(SOFT_WHITE);
        
        RoundedPanel formPanel = new RoundedPanel(20);
        formPanel.setBackground(CARD_WHITE);
        formPanel.setLayout(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createCompoundBorder(
            new ShadowBorder(),
            BorderFactory.createEmptyBorder(35, 35, 35, 35)
        ));
        
        // Make form scrollable
        JScrollPane formScroll = new JScrollPane(formPanel);
        formScroll.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        formScroll.getViewport().setBackground(CARD_WHITE);
        formScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        formScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        JLabel titleLabel = new JLabel("✏️ Edit Room: " + room.getRoomNumber());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(TEXT_PRIMARY);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 0, 25, 0);
        gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(titleLabel, gbc);
        
        gbc.gridwidth = 1;
        gbc.insets = new Insets(12, 12, 12, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        // Pre-populate fields with existing room data
        JTextField roomNumberField = createModernTextField(room.getRoomNumber());
        roomNumberField.setEditable(false);
        JTextField floorField = createModernTextField(String.valueOf(room.getFloor()));
        JTextField viewTypeField = createModernTextField(room.getViewType() != null ? room.getViewType() : "");
        JTextField priceField = createModernTextField(String.valueOf(room.getNightlyRate()));
        JTextArea descriptionArea = new JTextArea(4, 30);
        descriptionArea.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        descriptionArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setText(room.getDescription() != null ? room.getDescription() : "");
        
        JTextArea categoryDescArea = new JTextArea(3, 30);
        categoryDescArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        categoryDescArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        categoryDescArea.setLineWrap(true);
        categoryDescArea.setWrapStyleWord(true);
        if (room.getCategory() != null && room.getCategory().getDescription() != null) {
            categoryDescArea.setText(room.getCategory().getDescription());
        }
        
        JComboBox<String> categoryCombo = new JComboBox<>();
        categoryCombo.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        List<RoomCategory> categories = roomService.getAllCategories();
        for (RoomCategory cat : categories) {
            categoryCombo.addItem(cat.getName());
            if (room.getCategory() != null && cat.getName().equals(room.getCategory().getName())) {
                categoryCombo.setSelectedItem(cat.getName());
            }
        }
        
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"AVAILABLE", "MAINTENANCE", "CLEANING", "OCCUPIED", "RESERVED"});
        statusCombo.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        statusCombo.setSelectedItem(room.getStatus() != null ? room.getStatus() : "AVAILABLE");
        
        JComboBox<String> roomTypeCombo = new JComboBox<>(new String[]{"Guest Room", "Meeting Room", "Conference Room", "Suite"});
        roomTypeCombo.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        if (room.getViewType() != null) {
            for (int i = 0; i < roomTypeCombo.getItemCount(); i++) {
                if (roomTypeCombo.getItemAt(i).equals(room.getViewType())) {
                    roomTypeCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
        
        // Image display/upload
        JLabel imageLabel = new JLabel(room.getPhotoUrl() != null && !room.getPhotoUrl().isEmpty() ? 
            "Image: " + room.getPhotoUrl() : "No image selected");
        imageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        imageLabel.setForeground(room.getPhotoUrl() != null && !room.getPhotoUrl().isEmpty() ? SUCCESS_GREEN : TEXT_SECONDARY);
        imageLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(0, 120));
        
        JButton uploadImageBtn = createModernButton("📷 Upload Room Image", TEXT_SECONDARY);
        uploadImageBtn.setPreferredSize(new Dimension(200, 40));
        final String[] imagePath = {room.getPhotoUrl()};
        uploadImageBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().toLowerCase().endsWith(".jpg") || 
                           f.getName().toLowerCase().endsWith(".jpeg") || 
                           f.getName().toLowerCase().endsWith(".png");
                }
                @Override
                public String getDescription() {
                    return "Image Files (*.jpg, *.jpeg, *.png)";
                }
            });
            if (fileChooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                imagePath[0] = fileChooser.getSelectedFile().getAbsolutePath();
                imageLabel.setText("Image: " + new File(imagePath[0]).getName());
                imageLabel.setForeground(SUCCESS_GREEN);
            }
        });
        
        // Amenities checkboxes - parse from description
        String desc = room.getDescription() != null ? room.getDescription().toLowerCase() : "";
        JCheckBox breakfastCheck = new JCheckBox("Breakfast Included");
        breakfastCheck.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        breakfastCheck.setForeground(TEXT_PRIMARY);
        breakfastCheck.setSelected(desc.contains("breakfast"));
        
        JCheckBox wifiCheck = new JCheckBox("Free Wi-Fi");
        wifiCheck.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        wifiCheck.setForeground(TEXT_PRIMARY);
        wifiCheck.setSelected(desc.contains("wi-fi") || desc.contains("wifi"));
        
        JCheckBox poolCheck = new JCheckBox("Swimming Pool Access");
        poolCheck.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        poolCheck.setForeground(TEXT_PRIMARY);
        poolCheck.setSelected(desc.contains("pool"));
        
        JCheckBox dinnerCheck = new JCheckBox("Dinner Service");
        dinnerCheck.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        dinnerCheck.setForeground(TEXT_PRIMARY);
        dinnerCheck.setSelected(desc.contains("dinner"));
        
        JCheckBox acCheck = new JCheckBox("Air Conditioning");
        acCheck.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        acCheck.setForeground(TEXT_PRIMARY);
        acCheck.setSelected(desc.contains("air conditioning") || desc.contains("ac"));
        
        JCheckBox tvCheck = new JCheckBox("TV & Entertainment");
        tvCheck.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tvCheck.setForeground(TEXT_PRIMARY);
        tvCheck.setSelected(desc.contains("tv"));
        
        int row = 1;
        gbc.gridx = 0; gbc.gridy = row++;
        formPanel.add(createFormLabel("Room Number:"), gbc);
        gbc.gridx = 1;
        formPanel.add(roomNumberField, gbc);
        
        gbc.gridx = 0; gbc.gridy = row++;
        formPanel.add(createFormLabel("Room Type:"), gbc);
        gbc.gridx = 1;
        formPanel.add(roomTypeCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = row++;
        formPanel.add(createFormLabel("Category:"), gbc);
        gbc.gridx = 1;
        formPanel.add(categoryCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = row++;
        gbc.gridwidth = 2;
        formPanel.add(createFormLabel("Category Description & Default Amenities:"), gbc);
        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 0.5;
        formPanel.add(new JScrollPane(categoryDescArea), gbc);
        
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0;
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = row++;
        formPanel.add(createFormLabel("Price per Night ($):"), gbc);
        gbc.gridx = 1;
        formPanel.add(priceField, gbc);
        
        gbc.gridx = 0; gbc.gridy = row++;
        formPanel.add(createFormLabel("Floor:"), gbc);
        gbc.gridx = 1;
        formPanel.add(floorField, gbc);
        
        gbc.gridx = 0; gbc.gridy = row++;
        formPanel.add(createFormLabel("View Type:"), gbc);
        gbc.gridx = 1;
        formPanel.add(viewTypeField, gbc);
        
        gbc.gridx = 0; gbc.gridy = row++;
        formPanel.add(createFormLabel("Status:"), gbc);
        gbc.gridx = 1;
        formPanel.add(statusCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = row++;
        gbc.gridwidth = 2;
        JPanel imagePanel = new JPanel(new BorderLayout(10, 10));
        imagePanel.setOpaque(false);
        imagePanel.add(createFormLabel("Room Image:"), BorderLayout.NORTH);
        imagePanel.add(imageLabel, BorderLayout.CENTER);
        imagePanel.add(uploadImageBtn, BorderLayout.SOUTH);
        formPanel.add(imagePanel, gbc);
        
        gbc.gridx = 0; gbc.gridy = row++;
        gbc.gridwidth = 2;
        formPanel.add(createFormLabel("Additional Description:"), gbc);
        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 0.5;
        formPanel.add(new JScrollPane(descriptionArea), gbc);
        
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        JLabel amenitiesLabel = new JLabel("Additional Amenities (check all that apply):");
        amenitiesLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        amenitiesLabel.setForeground(TEXT_PRIMARY);
        formPanel.add(amenitiesLabel, gbc);
        
        JPanel amenitiesPanel = new JPanel(new GridLayout(0, 2, 10, 5));
        amenitiesPanel.setOpaque(false);
        amenitiesPanel.add(breakfastCheck);
        amenitiesPanel.add(wifiCheck);
        amenitiesPanel.add(poolCheck);
        amenitiesPanel.add(dinnerCheck);
        amenitiesPanel.add(acCheck);
        amenitiesPanel.add(tvCheck);
        
        gbc.gridy = row++;
        formPanel.add(amenitiesPanel, gbc);
        
        dialog.add(formScroll, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        buttonPanel.setBackground(SOFT_WHITE);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JButton saveBtn = createModernButton("💾 Save Changes", ACCENT_GREEN);
        saveBtn.setPreferredSize(new Dimension(150, 45));
        saveBtn.addActionListener(e -> {
            try {
                // Validate price
                double price = Double.parseDouble(priceField.getText().trim());
                if (price <= 0) {
                    JOptionPane.showMessageDialog(dialog, "Invalid price! Please enter a valid number.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                // Update room
                room.setFloor(Integer.parseInt(floorField.getText().trim()));
                room.setViewType(viewTypeField.getText().trim());
                room.setStatus((String) statusCombo.getSelectedItem());
                
                if (imagePath[0] != null && !imagePath[0].isEmpty()) {
                    room.setPhotoUrl(imagePath[0]);
                }
                
                // Update category if changed
                String selectedCategoryName = (String) categoryCombo.getSelectedItem();
                RoomCategory selectedCategory = null;
                for (RoomCategory cat : categories) {
                    if (cat.getName().equals(selectedCategoryName)) {
                        selectedCategory = cat;
                        break;
                    }
                }
                if (selectedCategory != null) {
                    room.setCategory(selectedCategory);
                    if (!categoryDescArea.getText().trim().isEmpty()) {
                        selectedCategory.setDescription(categoryDescArea.getText().trim());
                        new HibernateRoomRepository().saveCategory(selectedCategory);
                    }
                }
                
                // Build description
                StringBuilder fullDescription = new StringBuilder();
                if (!descriptionArea.getText().trim().isEmpty()) {
                    fullDescription.append(descriptionArea.getText().trim());
                }
                if (!categoryDescArea.getText().trim().isEmpty()) {
                    if (fullDescription.length() > 0) fullDescription.append(" ");
                    fullDescription.append(categoryDescArea.getText().trim());
                }
                
                // Build amenities
                StringBuilder amenitiesDesc = new StringBuilder();
                if (breakfastCheck.isSelected()) amenitiesDesc.append("Breakfast, ");
                if (wifiCheck.isSelected()) amenitiesDesc.append("Wi-Fi, ");
                if (poolCheck.isSelected()) amenitiesDesc.append("Pool Access, ");
                if (dinnerCheck.isSelected()) amenitiesDesc.append("Dinner Service, ");
                if (acCheck.isSelected()) amenitiesDesc.append("AC, ");
                if (tvCheck.isSelected()) amenitiesDesc.append("TV");
                
                if (amenitiesDesc.length() > 0 && amenitiesDesc.charAt(amenitiesDesc.length() - 1) == ' ') {
                    amenitiesDesc.setLength(amenitiesDesc.length() - 2);
                }
                
                if (amenitiesDesc.length() > 0) {
                    if (fullDescription.length() > 0) fullDescription.append(". Amenities: ");
                    fullDescription.append(amenitiesDesc.toString());
                }
                
                room.setDescription(fullDescription.toString());
                
                new HibernateRoomRepository().update(room);
                JOptionPane.showMessageDialog(dialog, "Room updated successfully! 🎉", "Success", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                loadRooms();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid floor number or price!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        JButton cancelBtn = createModernButton("Cancel", TEXT_SECONDARY);
        cancelBtn.setPreferredSize(new Dimension(120, 45));
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(cancelBtn);
        buttonPanel.add(saveBtn);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setVisible(true);
    }
    
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
            
            // Add status badges
            if (table.getColumnName(column).equals("Status") && value != null) {
                String status = value.toString();
                String badge = "";
                Color badgeColor = TEXT_SECONDARY;
                
                switch (status) {
                    case "AVAILABLE":
                        badge = "✓ " + status;
                        badgeColor = SUCCESS_GREEN;
                        break;
                    case "OCCUPIED":
                        badge = "● " + status;
                        badgeColor = WARNING_ORANGE;
                        break;
                    case "RESERVED":
                        badge = "⏱ " + status;
                        badgeColor = INFO_BLUE;
                        break;
                    case "MAINTENANCE":
                    case "CLEANING":
                        badge = "⚠ " + status;
                        badgeColor = new Color(234, 179, 8);
                        break;
                    default:
                        badge = status;
                }
                
                setText(badge);
                setForeground(badgeColor);
                setFont(new Font("Segoe UI", Font.BOLD, 13));
            }
            
            return c;
        }
    }
    
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
}