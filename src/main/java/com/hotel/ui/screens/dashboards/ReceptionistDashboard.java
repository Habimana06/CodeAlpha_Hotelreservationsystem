package com.hotel.ui.screens.dashboards;

import com.hotel.config.DatabaseConfig;
import com.hotel.model.CustomerMessage;
import com.hotel.model.Reservation;
import com.hotel.model.User;
import com.hotel.repository.HibernateCustomerMessageRepository;
import com.hotel.repository.HibernateReservationRepository;
import com.hotel.repository.HibernateRoomRepository;
import com.hotel.repository.HibernateUserRepository;
import com.hotel.service.HibernateReservationService;
import com.hotel.ui.components.RoundedPanel;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReceptionistDashboard extends JPanel {
    private final User user;
    private final HibernateReservationService reservationService;
    private final HibernateReservationRepository reservationRepository;
    private final HibernateRoomRepository roomRepository;
    private final DatabaseConfig config;
    private final HibernateCustomerMessageRepository messageRepository;
    
    private JTable messagesTable;
    private DefaultTableModel messagesModel;
    
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
    private JTextField searchField;
    private JPanel statsPanel; // Reference to stats panel for updates
    private Runnable onLogout;

    public ReceptionistDashboard(User user, HibernateUserRepository userRepository, DatabaseConfig config) {
        this(user, userRepository, config, null);
    }
    
    public ReceptionistDashboard(User user, HibernateUserRepository userRepository, DatabaseConfig config, Runnable onLogout) {
        this.user = user;
        this.config = config;
        this.onLogout = onLogout;
        this.reservationRepository = new HibernateReservationRepository();
        this.roomRepository = new HibernateRoomRepository();
        this.reservationService = new HibernateReservationService(reservationRepository, roomRepository);
        this.messageRepository = new HibernateCustomerMessageRepository();
        
        setLayout(new BorderLayout());
        setBackground(SOFT_WHITE);
        initializeUI();
        loadAllReservations();
    }

    private void initializeUI() {
        // Modern header with gradient
        JPanel header = createModernHeader();
        add(header, BorderLayout.NORTH);

        // Main content area
        JPanel mainContent = new JPanel(new BorderLayout(0, 20));
        mainContent.setBackground(SOFT_WHITE);
        mainContent.setBorder(BorderFactory.createEmptyBorder(20, 30, 30, 30));
        
        // Stats cards
        statsPanel = createStatsPanel();
        mainContent.add(statsPanel, BorderLayout.NORTH);
        
        // Tabbed content
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
        
        tabs.addTab("📋 Reservations", createReservationsPanel());
        tabs.addTab("💬 Customer Messages", createCustomerMessagesPanel());
        
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

        // Left side - Welcome message
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
        JLabel subtitle = new JLabel(hotelName + " - Receptionist Portal");
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
        
        // Hover effect
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
        
        // Calculate stats
        List<Reservation> allReservations = reservationService.findAllReservations();
        long confirmedReservations = allReservations.stream()
            .filter(r -> r.getStatus().equals("CONFIRMED"))
            .count();
        long checkedInReservations = allReservations.stream()
            .filter(r -> r.getStatus().equals("CHECKED_IN"))
            .count();
        
        // Stats cards - improved icon rendering
        panel.add(createStatCard("📋", "Total Reservations", String.valueOf(allReservations.size()), SUCCESS_GREEN));
        panel.add(createStatCard("✅", "Confirmed", String.valueOf(confirmedReservations), ACCENT_GREEN));
        panel.add(createStatCard("🏨", "Checked In", String.valueOf(checkedInReservations), SUCCESS_GREEN));
        
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
    
    private JPanel createReservationsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(SOFT_WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));
        
        JLabel title = new JLabel("📋 All Reservations");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(TEXT_PRIMARY);
        headerPanel.add(title, BorderLayout.WEST);
        
        panel.add(headerPanel, BorderLayout.NORTH);

        // Modern reservations table with improved design - moved to center
        String[] columns = {"ID", "Customer", "Room", "Check-in", "Check-out", "Guests", "Status", "Actions"};
        reservationTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 7;
            }
        };
        reservationTable = new JTable(reservationTableModel);
        styleModernTable(reservationTable);
        
        // Set column widths for better layout
        reservationTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        reservationTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        reservationTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        reservationTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        reservationTable.getColumnModel().getColumn(4).setPreferredWidth(120);
        reservationTable.getColumnModel().getColumn(5).setPreferredWidth(80);
        reservationTable.getColumnModel().getColumn(6).setPreferredWidth(120);
        reservationTable.getColumnModel().getColumn(7).setPreferredWidth(120);
        
        reservationTable.getColumn("Actions").setCellRenderer(new ActionButtonRenderer());
        reservationTable.getColumn("Actions").setCellEditor(new ActionButtonEditor(new JCheckBox()));
        
        // Custom renderer for status column
        reservationTable.getColumn("Status").setCellRenderer(new StatusBadgeRenderer());

        JScrollPane scrollPane = new JScrollPane(reservationTable);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            new ShadowBorder(),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        scrollPane.getViewport().setBackground(CARD_WHITE);
        
        panel.add(scrollPane, BorderLayout.CENTER);

        // Search panel - moved below the table
        RoundedPanel searchPanel = new RoundedPanel(24);
        searchPanel.setBackground(CARD_WHITE);
        searchPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 15));
        searchPanel.setBorder(BorderFactory.createCompoundBorder(
            new ShadowBorder(),
            BorderFactory.createEmptyBorder(20, 25, 20, 25)
        ));
        
        JLabel searchLabel = new JLabel("🔍 Search:");
        searchLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        searchLabel.setForeground(TEXT_PRIMARY);
        searchPanel.add(searchLabel);
        
        searchField = createModernTextField("");
        searchField.setPreferredSize(new Dimension(250, 40));
        searchField.addActionListener(e -> searchReservations());
        searchPanel.add(searchField);
        
        JButton searchBtn = createModernButton("Search", ACCENT_GREEN);
        searchBtn.setPreferredSize(new Dimension(100, 40));
        searchBtn.addActionListener(e -> searchReservations());
        searchPanel.add(searchBtn);
        
        JButton refreshBtn = createModernButton("🔄 Refresh All", TEXT_SECONDARY);
        refreshBtn.setPreferredSize(new Dimension(140, 40));
        refreshBtn.addActionListener(e -> loadAllReservations());
        searchPanel.add(refreshBtn);

        panel.add(searchPanel, BorderLayout.SOUTH);

        return panel;
    }
    
    private JPanel createCustomerMessagesPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(SOFT_WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(25, 0, 0, 0));
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        JLabel title = new JLabel("💬 Customer Support Messages");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(TEXT_PRIMARY);
        
        JButton refreshBtn = createModernButton("🔄 Refresh", TEXT_SECONDARY);
        refreshBtn.setPreferredSize(new Dimension(120, 40));
        refreshBtn.addActionListener(e -> loadMessages());
        
        headerPanel.add(title, BorderLayout.WEST);
        headerPanel.add(refreshBtn, BorderLayout.EAST);
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Messages table
        String[] columns = {"ID", "Customer", "Subject", "Message", "Date", "Status", "Actions"};
        messagesModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6;
            }
        };
        messagesTable = new JTable(messagesModel);
        styleModernTable(messagesTable);
        
        // Set column widths
        messagesTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        messagesTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        messagesTable.getColumnModel().getColumn(2).setPreferredWidth(200);
        messagesTable.getColumnModel().getColumn(3).setPreferredWidth(300);
        messagesTable.getColumnModel().getColumn(4).setPreferredWidth(120);
        messagesTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        messagesTable.getColumnModel().getColumn(6).setPreferredWidth(100);
        
        // Load messages from database
        loadMessages();
        
        // Add reply button editor
        messagesTable.getColumn("Actions").setCellRenderer(new ActionButtonRenderer());
        messagesTable.getColumn("Actions").setCellEditor(new MessageReplyEditor(new JCheckBox(), messagesModel));
        
        JScrollPane scrollPane = new JScrollPane(messagesTable);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            new ShadowBorder(),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        scrollPane.getViewport().setBackground(CARD_WHITE);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void loadMessages() {
        messagesModel.setRowCount(0); // Clear existing rows
        
        try {
            List<CustomerMessage> messages = messageRepository.findAll();
            
            if (messages.isEmpty()) {
                messagesModel.addRow(new Object[]{"", "No messages", "No customer messages yet", "", "", "", ""});
            } else {
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                for (CustomerMessage message : messages) {
                    String customerName = message.getUser().getDisplayName().isEmpty() 
                        ? message.getUser().getUsername() 
                        : message.getUser().getDisplayName();
                    String dateStr = message.getCreatedAt() != null 
                        ? message.getCreatedAt().format(dateFormatter) 
                        : "";
                    String messagePreview = message.getMessage().length() > 50 
                        ? message.getMessage().substring(0, 50) + "..." 
                        : message.getMessage();
                    
                    messagesModel.addRow(new Object[]{
                        String.valueOf(message.getId()),
                        customerName,
                        message.getSubject(),
                        messagePreview,
                        dateStr,
                        message.getStatus(),
                        "Reply"
                    });
                }
            }
        } catch (Exception e) {
            messagesModel.addRow(new Object[]{"", "Error", "Failed to load messages", e.getMessage(), "", "", ""});
        }
    }
    
    private void showReplyDialog(long messageId, String customerName, String subject, String originalMessage) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Reply to Customer", true);
        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(SOFT_WHITE);
        
        RoundedPanel formPanel = new RoundedPanel(20);
        formPanel.setBackground(CARD_WHITE);
        formPanel.setLayout(new BorderLayout(0, 20));
        formPanel.setBorder(BorderFactory.createCompoundBorder(
            new ShadowBorder(),
            BorderFactory.createEmptyBorder(30, 30, 30, 30)
        ));
        
        // Header
        JLabel title = new JLabel("✉️ Reply to: " + customerName);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(TEXT_PRIMARY);
        formPanel.add(title, BorderLayout.NORTH);
        
        // Original message
        JPanel originalPanel = new JPanel(new BorderLayout(0, 10));
        originalPanel.setOpaque(false);
        JLabel originalLabel = new JLabel("Original Message:");
        originalLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        originalLabel.setForeground(TEXT_PRIMARY);
        
        JTextArea originalArea = new JTextArea(originalMessage);
        originalArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        originalArea.setEditable(false);
        originalArea.setBackground(new Color(248, 250, 252));
        originalArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        originalArea.setLineWrap(true);
        originalArea.setWrapStyleWord(true);
        JScrollPane originalScroll = new JScrollPane(originalArea);
        originalScroll.setPreferredSize(new Dimension(0, 100));
        
        originalPanel.add(originalLabel, BorderLayout.NORTH);
        originalPanel.add(originalScroll, BorderLayout.CENTER);
        
        // Reply message
        JPanel replyPanel = new JPanel(new BorderLayout(0, 10));
        replyPanel.setOpaque(false);
        JLabel replyLabel = new JLabel("Your Reply:");
        replyLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        replyLabel.setForeground(TEXT_PRIMARY);
        
        JTextArea replyArea = new JTextArea(8, 30);
        replyArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        replyArea.setLineWrap(true);
        replyArea.setWrapStyleWord(true);
        replyArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        JScrollPane replyScroll = new JScrollPane(replyArea);
        replyScroll.setPreferredSize(new Dimension(0, 150));
        
        replyPanel.add(replyLabel, BorderLayout.NORTH);
        replyPanel.add(replyScroll, BorderLayout.CENTER);
        
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.add(originalPanel);
        contentPanel.add(Box.createVerticalStrut(15));
        contentPanel.add(replyPanel);
        
        formPanel.add(contentPanel, BorderLayout.CENTER);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        
        JButton cancelBtn = createModernButton("Cancel", TEXT_SECONDARY);
        cancelBtn.setPreferredSize(new Dimension(100, 40));
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        JButton sendBtn = createModernButton("📤 Send Reply", ACCENT_GREEN);
        sendBtn.setPreferredSize(new Dimension(140, 40));
        sendBtn.addActionListener(e -> {
            if (replyArea.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please enter a reply message.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Save reply to database
            try {
                java.util.Optional<CustomerMessage> messageOpt = messageRepository.findById(messageId);
                if (messageOpt.isPresent()) {
                    CustomerMessage message = messageOpt.get();
                    message.setReplyMessage(replyArea.getText().trim());
                    message.setStatus("RESOLVED");
                    message.setRepliedAt(java.time.LocalDateTime.now());
                    messageRepository.update(message);
                    
                    // Reload messages
                    loadMessages();
                    
                    JOptionPane.showMessageDialog(dialog, "Reply sent successfully to " + customerName + "!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialog, "Message not found.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Failed to send reply: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        buttonPanel.add(cancelBtn);
        buttonPanel.add(sendBtn);
        formPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.setVisible(true);
    }
    
    private class MessageReplyEditor extends DefaultCellEditor {
        protected JButton button;
        private int selectedRow;
        private DefaultTableModel model;
        
        public MessageReplyEditor(JCheckBox checkBox, DefaultTableModel model) {
            super(checkBox);
            this.model = model;
            button = new JButton();
            button.setOpaque(true);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.addActionListener(e -> fireEditingStopped());
        }
        
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            button.setText("Reply");
            button.setBackground(ACCENT_GREEN);
            button.setForeground(Color.WHITE);
            button.setFont(new Font("Segoe UI", Font.BOLD, 14));
            button.setPreferredSize(new Dimension(100, 35));
            button.setMinimumSize(new Dimension(100, 35));
            button.setMaximumSize(new Dimension(100, 35));
            button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
            selectedRow = row;
            return button;
        }
        
        public Object getCellEditorValue() {
            String messageIdStr = (String) model.getValueAt(selectedRow, 0);
            if (messageIdStr == null || messageIdStr.isEmpty()) {
                return "Reply";
            }
            try {
                long messageId = Long.parseLong(messageIdStr);
                String customer = (String) model.getValueAt(selectedRow, 1);
                String subject = (String) model.getValueAt(selectedRow, 2);
                String message = (String) model.getValueAt(selectedRow, 3);
                showReplyDialog(messageId, customer, subject, message);
            } catch (NumberFormatException e) {
                // Invalid message ID, skip
            }
            return "Reply";
        }
    }

    private void loadAllReservations() {
        List<Reservation> reservations = reservationService.findAllReservations();
        updateTable(reservations);
    }

    private void searchReservations() {
        String search = searchField.getText().trim();
        if (search.isEmpty()) {
            loadAllReservations();
            return;
        }

        List<Reservation> all = reservationService.findAllReservations();
        List<Reservation> filtered = all.stream()
                .filter(r -> String.valueOf(r.getId()).contains(search) ||
                           r.getRoom().getRoomNumber().contains(search) ||
                           r.getUser().getUsername().contains(search))
                .toList();
        updateTable(filtered);
    }

    private void updateTable(List<Reservation> reservations) {
        reservationTableModel.setRowCount(0);
        for (Reservation res : reservations) {
            Object[] row = {
                res.getId(),
                res.getUser().getDisplayName() + " (" + res.getUser().getUsername() + ")",
                res.getRoom().getRoomNumber(),
                res.getCheckIn().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                res.getCheckOut().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                res.getGuestCount(),
                res.getStatus(),
                res.getStatus().equals("CONFIRMED") ? "Check-in" : 
                res.getStatus().equals("CHECKED_IN") ? "Check-out" : "View"
            };
            reservationTableModel.addRow(row);
        }
    }

    private void checkIn(long reservationId) {
        try {
            Reservation res = reservationRepository.findById(reservationId).orElse(null);
            if (res != null) {
                res.setStatus("CHECKED_IN");
                reservationRepository.update(res);
                roomRepository.updateStatus(res.getRoom().getId(), "OCCUPIED");
                JOptionPane.showMessageDialog(this, "Guest checked in successfully! 🎉", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadAllReservations();
                updateStatsPanel(); // Update stats after check-in
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void checkOut(long reservationId) {
        try {
            Reservation res = reservationRepository.findById(reservationId).orElse(null);
            if (res != null) {
                res.setStatus("CHECKED_OUT");
                reservationRepository.update(res);
                roomRepository.updateStatus(res.getRoom().getId(), "AVAILABLE");
                JOptionPane.showMessageDialog(this, "Guest checked out successfully! 🎉", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadAllReservations();
                updateStatsPanel(); // Update stats after check-out
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
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

    private void viewDetails(long reservationId) {
        Reservation res = reservationRepository.findById(reservationId).orElse(null);
        if (res != null) {
            String details = String.format(
                "<html><body style='width: 400px; padding: 20px; font-family: Segoe UI;'>" +
                "<h2 style='color: #1e293b; margin-bottom: 15px; border-bottom: 2px solid #d4af37; padding-bottom: 10px;'>" +
                "📋 Reservation Details</h2>" +
                "<div style='background: #f8fafc; padding: 15px; border-radius: 8px;'>" +
                "<p><b>Reservation ID:</b> %d</p>" +
                "<p><b>Customer:</b> %s (%s)</p>" +
                "<p><b>Room:</b> %s - %s</p>" +
                "<p><b>Check-in:</b> %s</p>" +
                "<p><b>Check-out:</b> %s</p>" +
                "<p><b>Guests:</b> %d</p>" +
                "<p><b>Status:</b> <span style='color: %s; font-weight: bold;'>%s</span></p>" +
                "</div>" +
                "</body></html>",
                res.getId(), 
                res.getUser().getDisplayName(), 
                res.getUser().getUsername(),
                res.getRoom().getRoomNumber(),
                res.getRoom().getCategory().getName(),
                res.getCheckIn().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                res.getCheckOut().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                res.getGuestCount(),
                res.getStatus().equals("CONFIRMED") ? "#22c55e" : 
                res.getStatus().equals("CHECKED_IN") ? "#d4af37" : 
                res.getStatus().equals("CANCELLED") ? "#ef4444" : "#64748b",
                res.getStatus()
            );
            JOptionPane.showMessageDialog(this, details, "Reservation Details", JOptionPane.INFORMATION_MESSAGE);
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
    
    // UI Helper Methods
    
    private JTextField createModernTextField(String text) {
        JTextField field = new JTextField(text, 15);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ACCENT_GOLD, 2, true),
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
        
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(new ModernCellRenderer());
        }
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
                case "CHECKED_OUT":
                    label.setBackground(new Color(TEXT_SECONDARY.getRed(), TEXT_SECONDARY.getGreen(), TEXT_SECONDARY.getBlue(), 30));
                    label.setForeground(TEXT_SECONDARY);
                    label.setText("✓ " + status);
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

    private class ActionButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ActionButtonRenderer() {
            setOpaque(true);
            setBorderPainted(false);
            setFocusPainted(false);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            String action = (value == null) ? "" : value.toString();
            setText(action);
            
            if (action.equals("Check-in")) {
                setBackground(SUCCESS_GREEN);
            } else if (action.equals("Check-out")) {
                setBackground(WARNING_ORANGE);
            } else if (action.equals("Reply")) {
                setBackground(ACCENT_GREEN);
            } else {
                setBackground(new Color(59, 130, 246));
            }
            
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setPreferredSize(new Dimension(100, 35));
            setMinimumSize(new Dimension(100, 35));
            setMaximumSize(new Dimension(100, 35));
            setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            return this;
        }
    }

    private class ActionButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private String label;
        private int selectedRow;

        public ActionButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.addActionListener(e -> fireEditingStopped());
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            label = (value == null) ? "" : value.toString();
            button.setText(label);
            
            if (label.equals("Check-in")) {
                button.setBackground(SUCCESS_GREEN);
            } else if (label.equals("Check-out")) {
                button.setBackground(WARNING_ORANGE);
            } else if (label.equals("Reply")) {
                button.setBackground(ACCENT_GREEN);
            } else {
                button.setBackground(new Color(59, 130, 246));
            }
            
            button.setForeground(Color.WHITE);
            button.setFont(new Font("Segoe UI", Font.BOLD, 14));
            button.setPreferredSize(new Dimension(100, 35));
            button.setMinimumSize(new Dimension(100, 35));
            button.setMaximumSize(new Dimension(100, 35));
            button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
            selectedRow = row;
            return button;
        }

        public Object getCellEditorValue() {
            String action = label;
            long reservationId = (Long) reservationTableModel.getValueAt(selectedRow, 0);
            
            if (action.equals("Check-in")) {
                checkIn(reservationId);
            } else if (action.equals("Check-out")) {
                checkOut(reservationId);
            } else {
                viewDetails(reservationId);
            }
            return label;
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
}
