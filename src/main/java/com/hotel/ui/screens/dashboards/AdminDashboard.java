package com.hotel.ui.screens.dashboards;

import com.hotel.config.DatabaseConfig;
import com.hotel.model.PrivilegeEntity;
import com.hotel.model.Role;
import com.hotel.model.User;
import com.hotel.repository.HibernateUserRepository;
import com.hotel.service.HibernateAdminService;
import com.hotel.service.PasswordEncoder;
import com.hotel.ui.components.RoundedPanel;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AdminDashboard extends JPanel {
    private final User user;
    private final HibernateAdminService adminService;
    private final HibernateUserRepository userRepository;
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
    private static final Color ERROR_RED = new Color(239, 68, 68);
    private static final Color DEEP_NAVY = new Color(15, 23, 42);
    private static final Color ACCENT_GOLD = new Color(212, 175, 55);
    private static final Color ACCENT_GOLD_HOVER = new Color(184, 153, 48);
    
    private JTable userTable;
    private DefaultTableModel userTableModel;
    private JTable roleTable;
    private JTable privilegeTable;
    private Runnable onLogout;

    public AdminDashboard(User user, HibernateUserRepository userRepository, DatabaseConfig config) {
        this(user, userRepository, config, null);
    }
    
    public AdminDashboard(User user, HibernateUserRepository userRepository, DatabaseConfig config, Runnable onLogout) {
        this.user = user;
        this.config = config;
        this.onLogout = onLogout;
        this.userRepository = userRepository;
        this.adminService = new HibernateAdminService(userRepository, new PasswordEncoder());
        
        setLayout(new BorderLayout());
        setBackground(SOFT_WHITE);
        initializeUI();
        loadUsers();
        loadRoles();
        loadPrivileges();
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
        JPanel statsPanel = createStatsPanel();
        mainContent.add(statsPanel, BorderLayout.NORTH);
        
        // Tabbed content with modern styling
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
        JLabel subtitle = new JLabel(hotelName + " - Admin Portal");
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
        List<User> allUsers = adminService.listUsers();
        long activeUsers = allUsers.stream().filter(User::isActive).count();
        long totalRoles = adminService.listRoles().size();
        
        // Stats cards - improved icon rendering
        panel.add(createStatCard("👥", "Total Users", String.valueOf(allUsers.size()), SUCCESS_GREEN));
        panel.add(createStatCard("✅", "Active Users", String.valueOf(activeUsers), ACCENT_GREEN));
        panel.add(createStatCard("🔐", "Total Roles", String.valueOf(totalRoles), SUCCESS_GREEN));
        
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
        
        tabs.addTab("👥 User Management", createUserManagementPanel());
        tabs.addTab("🔐 Role & Privileges", createRolePrivilegePanel());
        tabs.addTab("📊 Analytics", createAnalyticsPanel());
        
        return tabs;
    }

    private JPanel createUserManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(SOFT_WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(25, 0, 0, 0));

        // Header with buttons
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        JLabel title = new JLabel("👥 User Management");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(TEXT_PRIMARY);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        
        JButton createUserBtn = createModernButton("➕ Create User", ACCENT_GREEN);
        createUserBtn.setPreferredSize(new Dimension(140, 40));
        createUserBtn.addActionListener(e -> showCreateUserDialog());
        
        JButton refreshBtn = createModernButton("🔄 Refresh", TEXT_SECONDARY);
        refreshBtn.setPreferredSize(new Dimension(120, 40));
        refreshBtn.addActionListener(e -> loadUsers());
        
        buttonPanel.add(createUserBtn);
        buttonPanel.add(refreshBtn);
        
        headerPanel.add(title, BorderLayout.WEST);
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        
        panel.add(headerPanel, BorderLayout.NORTH);

        // Modern user table
        String[] columns = {"ID", "Username", "Name", "Email", "Role", "Active", "Actions"};
        userTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6;
            }
        };
        userTable = new JTable(userTableModel);
        styleModernTable(userTable);
        
        userTable.getColumn("Actions").setCellRenderer(new ModernButtonRenderer());
        userTable.getColumn("Actions").setCellEditor(new UserActionEditor(new JCheckBox()));

        JScrollPane scrollPane = new JScrollPane(userTable);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            new ShadowBorder(),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        scrollPane.getViewport().setBackground(CARD_WHITE);
        
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createRolePrivilegePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(SOFT_WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(25, 0, 0, 0));
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        JLabel title = new JLabel("🔐 Role & Privilege Management");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(TEXT_PRIMARY);
        
        JButton managePrivilegesBtn = createModernButton("⚙️ Manage Privileges", ACCENT_GREEN);
        managePrivilegesBtn.setPreferredSize(new Dimension(180, 40));
        managePrivilegesBtn.addActionListener(e -> showPrivilegeManagementDialog());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(managePrivilegesBtn);
        
        headerPanel.add(title, BorderLayout.WEST);
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Content with two tables side by side
        JPanel contentPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        contentPanel.setBackground(SOFT_WHITE);

        // Roles table
        String[] roleColumns = {"ID", "Role Name", "Privileges", "Actions"};
        DefaultTableModel roleModel = new DefaultTableModel(roleColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3;
            }
        };
        roleTable = new JTable(roleModel);
        styleModernTable(roleTable);
        
        roleTable.getColumn("Actions").setCellRenderer(new ModernButtonRenderer());
        roleTable.getColumn("Actions").setCellEditor(new RoleActionEditor(new JCheckBox(), roleModel));
        
        JScrollPane roleScroll = new JScrollPane(roleTable);
        roleScroll.setBorder(BorderFactory.createCompoundBorder(
            new ShadowBorder(),
            BorderFactory.createTitledBorder("Roles")
        ));
        roleScroll.getViewport().setBackground(CARD_WHITE);
        contentPanel.add(roleScroll);

        // Privileges table
        String[] privColumns = {"ID", "Code", "Description"};
        DefaultTableModel privModel = new DefaultTableModel(privColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        privilegeTable = new JTable(privModel);
        styleModernTable(privilegeTable);
        
        JScrollPane privScroll = new JScrollPane(privilegeTable);
        privScroll.setBorder(BorderFactory.createCompoundBorder(
            new ShadowBorder(),
            BorderFactory.createTitledBorder("Available Privileges")
        ));
        privScroll.getViewport().setBackground(CARD_WHITE);
        contentPanel.add(privScroll);
        
        panel.add(contentPanel, BorderLayout.CENTER);

        return panel;
    }
    
    private void showPrivilegeManagementDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Manage Role Privileges", true);
        dialog.setSize(700, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        
        RoundedPanel mainPanel = new RoundedPanel(20);
        mainPanel.setBackground(CARD_WHITE);
        mainPanel.setLayout(new BorderLayout(0, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        
        JLabel title = new JLabel("⚙️ Assign Privileges to Roles");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(TEXT_PRIMARY);
        mainPanel.add(title, BorderLayout.NORTH);
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        
        JComboBox<String> roleCombo = new JComboBox<>();
        roleCombo.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        List<Role> roles = adminService.listRoles();
        for (Role role : roles) {
            roleCombo.addItem(role.getName());
        }
        
        JList<String> privilegeList = new JList<>();
        List<PrivilegeEntity> allPrivileges = adminService.listPrivileges();
        DefaultListModel<String> privModel = new DefaultListModel<>();
        for (PrivilegeEntity priv : allPrivileges) {
            privModel.addElement(priv.getCode() + " - " + priv.getDescription());
        }
        privilegeList.setModel(privModel);
        privilegeList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        privilegeList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        roleCombo.addActionListener(e -> {
            String selectedRole = (String) roleCombo.getSelectedItem();
            if (selectedRole != null) {
                Role role = roles.stream().filter(r -> r.getName().equals(selectedRole)).findFirst().orElse(null);
                if (role != null) {
                    privilegeList.clearSelection();
                    Set<String> rolePrivs = role.getPrivileges().stream()
                            .map(PrivilegeEntity::getCode)
                            .collect(Collectors.toSet());
                    for (int i = 0; i < allPrivileges.size(); i++) {
                        if (rolePrivs.contains(allPrivileges.get(i).getCode())) {
                            privilegeList.addSelectionInterval(i, i);
                        }
                    }
                }
            }
        });
        
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(createFormLabel("Select Role:"), gbc);
        gbc.gridx = 1;
        formPanel.add(roleCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 2;
        formPanel.add(createFormLabel("Select Privileges:"), gbc);
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        formPanel.add(new JScrollPane(privilegeList), gbc);
        
        mainPanel.add(formPanel, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setOpaque(false);
        JButton saveBtn = createModernButton("💾 Save Privileges", ACCENT_GREEN);
        saveBtn.addActionListener(e -> {
            try {
                String selectedRole = (String) roleCombo.getSelectedItem();
                if (selectedRole == null) {
                    JOptionPane.showMessageDialog(dialog, "Please select a role", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                Role role = roles.stream().filter(r -> r.getName().equals(selectedRole)).findFirst().orElse(null);
                if (role == null) return;
                
                Set<String> selectedPrivs = privilegeList.getSelectedValuesList().stream()
                        .map(s -> s.split(" - ")[0])
                        .collect(Collectors.toSet());
                
                adminService.assignPrivilegesToRole(role.getId(), selectedPrivs);
                JOptionPane.showMessageDialog(dialog, "Privileges updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadRoles();
                dialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        JButton cancelBtn = createModernButton("Cancel", TEXT_SECONDARY);
        cancelBtn.addActionListener(e -> dialog.dispose());
        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(mainPanel, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private void loadUsers() {
        List<User> users = adminService.listUsers();
        userTableModel.setRowCount(0);
        
        for (User u : users) {
            Object[] row = {
                u.getId(),
                u.getUsername(),
                u.getDisplayName(),
                u.getEmail(),
                u.getRole().getName(),
                u.isActive() ? "Yes" : "No",
                "Edit"
            };
            userTableModel.addRow(row);
        }
    }

    private void loadRoles() {
        List<Role> roles = adminService.listRoles();
        DefaultTableModel model = (DefaultTableModel) roleTable.getModel();
        model.setRowCount(0);
        
        for (Role role : roles) {
            String privileges = role.getPrivileges().stream()
                    .map(PrivilegeEntity::getCode)
                    .collect(Collectors.joining(", "));
            model.addRow(new Object[]{
                role.getId(),
                role.getName(),
                privileges.isEmpty() ? "None" : privileges,
                "Edit"
            });
        }
    }

    private void loadPrivileges() {
        List<PrivilegeEntity> privileges = adminService.listPrivileges();
        DefaultTableModel model = (DefaultTableModel) privilegeTable.getModel();
        model.setRowCount(0);
        
        for (PrivilegeEntity priv : privileges) {
            model.addRow(new Object[]{
                priv.getId(),
                priv.getCode(),
                priv.getDescription()
            });
        }
    }

    private void showCreateUserDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Create New User", true);
        dialog.setSize(650, 700);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(SOFT_WHITE);

        RoundedPanel formPanel = new RoundedPanel(20);
        formPanel.setBackground(CARD_WHITE);
        formPanel.setLayout(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createCompoundBorder(
            new ShadowBorder(),
            BorderFactory.createEmptyBorder(40, 40, 40, 40)
        ));
        
        // Enhanced title with properly sized icon
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titlePanel.setOpaque(false);
        
        RoundedPanel iconPanel = new RoundedPanel(30);
        iconPanel.setBackground(ACCENT_GREEN_LIGHT);
        iconPanel.setPreferredSize(new Dimension(50, 50));
        iconPanel.setLayout(new BorderLayout());
        
        JLabel iconLabel = new JLabel("👤");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setVerticalAlignment(SwingConstants.CENTER);
        iconLabel.setForeground(ACCENT_GREEN);
        iconPanel.add(iconLabel, BorderLayout.CENTER);
        
        JLabel titleLabel = new JLabel("Create New User");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        titleLabel.setForeground(TEXT_PRIMARY);
        
        titlePanel.add(iconPanel);
        titlePanel.add(titleLabel);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 0, 30, 0);
        gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(titlePanel, gbc);
        
        gbc.gridwidth = 1;
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JTextField usernameField = createModernTextField("");
        JPasswordField passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        JTextField firstNameField = createModernTextField("");
        JTextField lastNameField = createModernTextField("");
        JTextField emailField = createModernTextField("");
        JComboBox<String> roleCombo = new JComboBox<>();
        roleCombo.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        List<Role> roles = adminService.listRoles();
        for (Role role : roles) {
            roleCombo.addItem(role.getName());
        }

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(createFormLabel("Username:"), gbc);
        gbc.gridx = 1;
        formPanel.add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(createFormLabel("Password:"), gbc);
        gbc.gridx = 1;
        formPanel.add(passwordField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(createFormLabel("First Name:"), gbc);
        gbc.gridx = 1;
        formPanel.add(firstNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(createFormLabel("Last Name:"), gbc);
        gbc.gridx = 1;
        formPanel.add(lastNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 5;
        formPanel.add(createFormLabel("Email:"), gbc);
        gbc.gridx = 1;
        formPanel.add(emailField, gbc);

        gbc.gridx = 0; gbc.gridy = 6;
        formPanel.add(createFormLabel("Role:"), gbc);
        gbc.gridx = 1;
        formPanel.add(roleCombo, gbc);

        dialog.add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        buttonPanel.setBackground(SOFT_WHITE);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JButton createBtn = createModernButton("✅ Create User", ACCENT_GREEN);
        createBtn.setPreferredSize(new Dimension(160, 48));
        createBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        createBtn.addActionListener(e -> {
            try {
                if (usernameField.getText().trim().isEmpty() || 
                    new String(passwordField.getPassword()).trim().isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, 
                        "Username and password are required!", 
                        "Validation Error", 
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }
                adminService.createUser(
                    usernameField.getText().trim(),
                    new String(passwordField.getPassword()),
                    firstNameField.getText().trim(),
                    lastNameField.getText().trim(),
                    emailField.getText().trim(),
                    (String) roleCombo.getSelectedItem(),
                    new HashSet<>()
                );
                JOptionPane.showMessageDialog(dialog, 
                    "User created successfully! 🎉", 
                    "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                loadUsers();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, 
                    "Error: " + ex.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        JButton cancelBtn = createModernButton("Cancel", TEXT_SECONDARY);
        cancelBtn.setPreferredSize(new Dimension(120, 42));
        cancelBtn.addActionListener(e -> dialog.dispose());
        buttonPanel.add(cancelBtn);
        buttonPanel.add(createBtn);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void showEditUserDialog(long userId) {
        User userToEdit = userRepository.findById(userId).orElse(null);
        if (userToEdit == null) return;

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit User", true);
        dialog.setSize(800, 850);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(SOFT_WHITE);

        RoundedPanel formPanel = new RoundedPanel(20);
        formPanel.setBackground(CARD_WHITE);
        formPanel.setLayout(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createCompoundBorder(
            new ShadowBorder(),
            BorderFactory.createEmptyBorder(40, 40, 40, 40)
        ));
        
        // Enhanced title with properly sized icon
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titlePanel.setOpaque(false);
        
        RoundedPanel iconPanel = new RoundedPanel(30);
        iconPanel.setBackground(ACCENT_GREEN_LIGHT);
        iconPanel.setPreferredSize(new Dimension(50, 50));
        iconPanel.setLayout(new BorderLayout());
        
        JLabel iconLabel = new JLabel("✏️");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setVerticalAlignment(SwingConstants.CENTER);
        iconLabel.setForeground(ACCENT_GREEN);
        iconPanel.add(iconLabel, BorderLayout.CENTER);
        
        JLabel titleLabel = new JLabel("Edit User: " + userToEdit.getUsername());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        titleLabel.setForeground(TEXT_PRIMARY);
        
        titlePanel.add(iconPanel);
        titlePanel.add(titleLabel);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 0, 30, 0);
        gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(titlePanel, gbc);
        
        gbc.gridwidth = 1;
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JTextField usernameField = createModernTextField(userToEdit.getUsername());
        usernameField.setPreferredSize(new Dimension(0, 42));
        JTextField firstNameField = createModernTextField(userToEdit.getFirstName());
        firstNameField.setPreferredSize(new Dimension(0, 42));
        JTextField lastNameField = createModernTextField(userToEdit.getLastName());
        lastNameField.setPreferredSize(new Dimension(0, 42));
        JTextField emailField = createModernTextField(userToEdit.getEmail());
        emailField.setPreferredSize(new Dimension(0, 42));
        JComboBox<String> roleCombo = new JComboBox<>();
        roleCombo.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        roleCombo.setPreferredSize(new Dimension(0, 42));
        List<Role> roles = adminService.listRoles();
        for (Role role : roles) {
            roleCombo.addItem(role.getName());
            if (role.getName().equals(userToEdit.getRole().getName())) {
                roleCombo.setSelectedItem(role.getName());
            }
        }

        JCheckBox activeCheck = new JCheckBox("Active", userToEdit.isActive());
        activeCheck.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        activeCheck.setForeground(TEXT_PRIMARY);

        JList<String> privilegeList = new JList<>();
        List<PrivilegeEntity> allPrivileges = adminService.listPrivileges();
        DefaultListModel<String> privModel = new DefaultListModel<>();
        for (PrivilegeEntity priv : allPrivileges) {
            privModel.addElement(priv.getCode() + " - " + priv.getDescription());
        }
        privilegeList.setModel(privModel);
        privilegeList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        privilegeList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        Set<String> userPrivileges = userToEdit.getPrivileges().stream()
                .map(PrivilegeEntity::getCode)
                .collect(Collectors.toSet());
        for (int i = 0; i < allPrivileges.size(); i++) {
            if (userPrivileges.contains(allPrivileges.get(i).getCode())) {
                privilegeList.addSelectionInterval(i, i);
            }
        }

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(createFormLabel("Username:"), gbc);
        gbc.gridx = 1;
        formPanel.add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(createFormLabel("First Name:"), gbc);
        gbc.gridx = 1;
        formPanel.add(firstNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(createFormLabel("Last Name:"), gbc);
        gbc.gridx = 1;
        formPanel.add(lastNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(createFormLabel("Email:"), gbc);
        gbc.gridx = 1;
        formPanel.add(emailField, gbc);

        gbc.gridx = 0; gbc.gridy = 5;
        formPanel.add(createFormLabel("Role:"), gbc);
        gbc.gridx = 1;
        formPanel.add(roleCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 6;
        gbc.gridwidth = 2;
        formPanel.add(activeCheck, gbc);

        gbc.gridx = 0; gbc.gridy = 7;
        gbc.gridwidth = 2;
        formPanel.add(createFormLabel("User Privileges:"), gbc);
        gbc.gridy = 8;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        JScrollPane privScroll = new JScrollPane(privilegeList);
        privScroll.setPreferredSize(new Dimension(400, 180));
        privScroll.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        formPanel.add(privScroll, gbc);

        dialog.add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        buttonPanel.setBackground(SOFT_WHITE);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JButton saveBtn = createModernButton("💾 Save Changes", ACCENT_GREEN);
        saveBtn.setPreferredSize(new Dimension(150, 42));
        saveBtn.addActionListener(e -> {
            try {
                if (usernameField.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, 
                        "Username cannot be empty!", 
                        "Validation Error", 
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }
                userToEdit.setUsername(usernameField.getText().trim());
                userToEdit.setFirstName(firstNameField.getText().trim());
                userToEdit.setLastName(lastNameField.getText().trim());
                userToEdit.setEmail(emailField.getText().trim());
                userToEdit.setActive(activeCheck.isSelected());
                
                Role newRole = adminService.listRoles().stream()
                        .filter(r -> r.getName().equals(roleCombo.getSelectedItem()))
                        .findFirst().orElse(null);
                if (newRole != null) {
                    userToEdit.setRole(newRole);
                }
                
                Set<String> selectedPrivs = privilegeList.getSelectedValuesList().stream()
                        .map(s -> s.split(" - ")[0])
                        .collect(Collectors.toSet());
                adminService.assignPrivileges(userId, selectedPrivs);
                
                adminService.updateUser(userToEdit);
                JOptionPane.showMessageDialog(dialog, 
                    "User updated successfully! 🎉", 
                    "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                loadUsers();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, 
                    "Error: " + ex.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        JButton cancelBtn = createModernButton("Cancel", TEXT_SECONDARY);
        cancelBtn.setPreferredSize(new Dimension(120, 42));
        cancelBtn.addActionListener(e -> dialog.dispose());
        buttonPanel.add(cancelBtn);
        buttonPanel.add(saveBtn);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }
    
    private JPanel createAnalyticsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(SOFT_WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(25, 0, 0, 0));
        
        JLabel title = new JLabel("📊 System Analytics");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(TEXT_PRIMARY);
        
        JButton downloadReportBtn = createModernButton("📥 Download Report", ACCENT_GREEN);
        downloadReportBtn.setPreferredSize(new Dimension(180, 40));
        downloadReportBtn.addActionListener(e -> downloadReport());
        
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.add(title, BorderLayout.WEST);
        headerPanel.add(downloadReportBtn, BorderLayout.EAST);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Analytics content with graphs
        JPanel analyticsContent = new JPanel(new BorderLayout(0, 20));
        analyticsContent.setBackground(SOFT_WHITE);
        
        // Statistics overview
        List<User> users = adminService.listUsers();
        List<Role> roles = adminService.listRoles();
        long activeUsers = users.stream().filter(User::isActive).count();
        
        JPanel statsGrid = new JPanel(new GridLayout(2, 2, 15, 15));
        statsGrid.setOpaque(false);
        
        statsGrid.add(createStatBox("Total Users", String.valueOf(users.size()), ACCENT_GREEN));
        statsGrid.add(createStatBox("Active Users", String.valueOf(activeUsers), SUCCESS_GREEN));
        statsGrid.add(createStatBox("Total Roles", String.valueOf(roles.size()), ACCENT_GREEN));
        statsGrid.add(createStatBox("Total Privileges", String.valueOf(adminService.listPrivileges().size()), SUCCESS_GREEN));
        
        analyticsContent.add(statsGrid, BorderLayout.NORTH);
        
        // Chart panel
        JPanel chartPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int width = getWidth();
                int height = getHeight();
                int padding = 40;
                int chartWidth = width - 2 * padding;
                int chartHeight = height - 2 * padding;
                
                // Draw background
                g2d.setColor(CARD_WHITE);
                g2d.fillRoundRect(padding, padding, chartWidth, chartHeight, 10, 10);
                
                // Draw title
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 16));
                g2d.setColor(TEXT_PRIMARY);
                g2d.drawString("User Activity Overview", padding + 10, padding + 25);
                
                // Draw bar chart
                int barCount = 4;
                int barWidth = chartWidth / (barCount + 1);
                int maxValue = Math.max(users.size(), 10);
                
                String[] labels = {"Total", "Active", "Inactive", "Roles"};
                int[] values = {users.size(), (int)activeUsers, users.size() - (int)activeUsers, roles.size()};
                
                for (int i = 0; i < barCount; i++) {
                    int x = padding + (i + 1) * barWidth;
                    int barHeight = (int)((values[i] / (double)maxValue) * (chartHeight - 60));
                    int y = padding + chartHeight - barHeight - 30;
                    
                    // Draw bar
                    g2d.setColor(ACCENT_GREEN);
                    g2d.fillRoundRect(x - barWidth/2 + 10, y, barWidth - 20, barHeight, 5, 5);
                    
                    // Draw value
                    g2d.setColor(TEXT_PRIMARY);
                    g2d.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    g2d.drawString(String.valueOf(values[i]), x - 5, y - 5);
                    
                    // Draw label
                    g2d.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                    g2d.setColor(TEXT_SECONDARY);
                    FontMetrics fm = g2d.getFontMetrics();
                    int labelWidth = fm.stringWidth(labels[i]);
                    g2d.drawString(labels[i], x - labelWidth/2, padding + chartHeight - 10);
                }
            }
        };
        chartPanel.setPreferredSize(new Dimension(0, 300));
        chartPanel.setBackground(SOFT_WHITE);
        chartPanel.setBorder(BorderFactory.createCompoundBorder(
            new ShadowBorder(),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        analyticsContent.add(chartPanel, BorderLayout.CENTER);
        
        panel.add(analyticsContent, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createStatBox(String label, String value, Color color) {
        RoundedPanel box = new RoundedPanel(15);
        box.setBackground(CARD_WHITE);
        box.setLayout(new BorderLayout(0, 10));
        box.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 2, true),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        valueLabel.setForeground(color);
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        JLabel labelText = new JLabel(label);
        labelText.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        labelText.setForeground(TEXT_SECONDARY);
        labelText.setHorizontalAlignment(SwingConstants.CENTER);
        
        box.add(valueLabel, BorderLayout.CENTER);
        box.add(labelText, BorderLayout.SOUTH);
        
        return box;
    }
    
    private void downloadReport() {
        try {
            List<User> users = adminService.listUsers();
            List<Role> roles = adminService.listRoles();
            List<PrivilegeEntity> privileges = adminService.listPrivileges();
            
            StringBuilder report = new StringBuilder();
            report.append("ADMIN SYSTEM REPORT\n");
            report.append("Generated: ").append(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
            report.append("=".repeat(50)).append("\n\n");
            
            report.append("USER STATISTICS\n");
            report.append("-".repeat(50)).append("\n");
            report.append("Total Users: ").append(users.size()).append("\n");
            report.append("Active Users: ").append(users.stream().filter(User::isActive).count()).append("\n");
            report.append("Inactive Users: ").append(users.stream().filter(u -> !u.isActive()).count()).append("\n\n");
            
            report.append("ROLE STATISTICS\n");
            report.append("-".repeat(50)).append("\n");
            for (Role role : roles) {
                report.append("Role: ").append(role.getName()).append(" - ");
                report.append("Privileges: ").append(role.getPrivileges().size()).append("\n");
            }
            report.append("\n");
            
            report.append("PRIVILEGES\n");
            report.append("-".repeat(50)).append("\n");
            for (PrivilegeEntity priv : privileges) {
                report.append(priv.getCode()).append(": ").append(priv.getDescription()).append("\n");
            }
            
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Report");
            fileChooser.setSelectedFile(new java.io.File("admin_report_" + 
                java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".txt"));
            
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
    
    private JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(TEXT_PRIMARY);
        return label;
    }
    
    private JButton createModernButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 15));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(120, 40));
        
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
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setPreferredSize(new Dimension(100, 35));
            setMinimumSize(new Dimension(100, 35));
            setMaximumSize(new Dimension(100, 35));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
            return this;
        }
    }

    private class UserActionEditor extends DefaultCellEditor {
        protected JButton button;
        private int selectedRow;

        public UserActionEditor(JCheckBox checkBox) {
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
            long userId = (Long) userTableModel.getValueAt(selectedRow, 0);
            showEditUserDialog(userId);
            return "Edit";
        }
    }
    
    private class RoleActionEditor extends DefaultCellEditor {
        protected JButton button;
        private int selectedRow;
        private DefaultTableModel model;

        public RoleActionEditor(JCheckBox checkBox, DefaultTableModel model) {
            super(checkBox);
            this.model = model;
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
            button.setFont(new Font("Segoe UI", Font.BOLD, 14));
            button.setPreferredSize(new Dimension(100, 35));
            button.setMinimumSize(new Dimension(100, 35));
            button.setMaximumSize(new Dimension(100, 35));
            button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
            selectedRow = row;
            return button;
        }

        public Object getCellEditorValue() {
            long roleId = (Long) model.getValueAt(selectedRow, 0);
            showEditRolePrivilegesDialog(roleId);
            return "Edit";
        }
    }
    
    private void showEditRolePrivilegesDialog(long roleId) {
        Role role = userRepository.findRoleById(roleId);
        if (role == null) {
            JOptionPane.showMessageDialog(this, "Role not found", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        showPrivilegeManagementDialogForRole(role);
    }
    
    private void showPrivilegeManagementDialogForRole(Role role) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Manage Privileges for " + role.getName(), true);
        dialog.setSize(600, 550);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        
        RoundedPanel mainPanel = new RoundedPanel(20);
        mainPanel.setBackground(CARD_WHITE);
        mainPanel.setLayout(new BorderLayout(0, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        
        JLabel title = new JLabel("⚙️ Assign Privileges to " + role.getName());
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(TEXT_PRIMARY);
        mainPanel.add(title, BorderLayout.NORTH);
        
        JList<String> privilegeList = new JList<>();
        List<PrivilegeEntity> allPrivileges = adminService.listPrivileges();
        DefaultListModel<String> privModel = new DefaultListModel<>();
        for (PrivilegeEntity priv : allPrivileges) {
            privModel.addElement(priv.getCode() + " - " + priv.getDescription());
        }
        privilegeList.setModel(privModel);
        privilegeList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        privilegeList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        // Pre-select current privileges
        Set<String> rolePrivs = role.getPrivileges().stream()
                .map(PrivilegeEntity::getCode)
                .collect(Collectors.toSet());
        for (int i = 0; i < allPrivileges.size(); i++) {
            if (rolePrivs.contains(allPrivileges.get(i).getCode())) {
                privilegeList.addSelectionInterval(i, i);
            }
        }
        
        mainPanel.add(new JScrollPane(privilegeList), BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setOpaque(false);
        JButton saveBtn = createModernButton("💾 Save Privileges", ACCENT_GREEN);
        saveBtn.addActionListener(e -> {
            try {
                Set<String> selectedPrivs = privilegeList.getSelectedValuesList().stream()
                        .map(s -> s.split(" - ")[0])
                        .collect(Collectors.toSet());
                
                adminService.assignPrivilegesToRole(role.getId(), selectedPrivs);
                JOptionPane.showMessageDialog(dialog, "Privileges updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadRoles();
                dialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        JButton cancelBtn = createModernButton("Cancel", TEXT_SECONDARY);
        cancelBtn.addActionListener(e -> dialog.dispose());
        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(mainPanel, BorderLayout.CENTER);
        dialog.setVisible(true);
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
