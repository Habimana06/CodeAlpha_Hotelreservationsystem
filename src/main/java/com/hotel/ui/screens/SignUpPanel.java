package com.hotel.ui.screens;

import com.hotel.config.DatabaseConfig;
import com.hotel.model.Role;
import com.hotel.model.User;
import com.hotel.repository.HibernateUserRepository;
import com.hotel.service.HibernateAdminService;
import com.hotel.service.PasswordEncoder;
import com.hotel.ui.HerbanetTheme;
import com.hotel.ui.ImageLoader;
import com.hotel.ui.components.RoundedPanel;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class SignUpPanel extends JPanel {
    // Enhanced color scheme for UC Grand Hotel
    private static final Color DEEP_NAVY = new Color(15, 23, 42);
    private static final Color ACCENT_GOLD = new Color(212, 175, 55);
    private static final Color ACCENT_GOLD_HOVER = new Color(184, 153, 48);
    private static final Color SOFT_WHITE = new Color(248, 250, 252);
    private static final Color BORDER_COLOR = new Color(226, 232, 240);
    private static final Color TEXT_PRIMARY = new Color(30, 41, 59);
    private static final Color TEXT_SECONDARY = new Color(100, 116, 139);
    private static final Color ERROR_RED = new Color(239, 68, 68);
    private static final Color SUCCESS_GREEN = new Color(34, 197, 94);
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    public SignUpPanel(HibernateUserRepository userRepository, Consumer<User> signUpCallback, 
                      DatabaseConfig config, Runnable backToLogin) {
        setLayout(new BorderLayout());
        setBackground(SOFT_WHITE);

        // Left side with enhanced branding
        JPanel leftPanel = createBrandingPanel(config);
        add(leftPanel, BorderLayout.WEST);

        // Right side with modern signup form
        JPanel rightContainer = new JPanel(new GridBagLayout());
        rightContainer.setBackground(SOFT_WHITE);
        
        RoundedPanel formPanel = new RoundedPanel(32);
        formPanel.setBackground(Color.WHITE);
        formPanel.setLayout(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createCompoundBorder(
            new ShadowBorder(),
            BorderFactory.createEmptyBorder(50, 60, 50, 60)
        ));
        formPanel.setPreferredSize(new Dimension(540, 700));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0;

        // Modern header
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);
        
        // Icon badge
        JLabel iconBadge = new JLabel("✨");
        iconBadge.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 44));
        iconBadge.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(iconBadge);
        headerPanel.add(Box.createVerticalStrut(12));
        
        JLabel title = new JLabel("Create Account");
        title.setFont(new Font("Segoe UI", Font.BOLD, 34));
        title.setForeground(TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(title);
        
        headerPanel.add(Box.createVerticalStrut(6));
        
        JLabel subtitle = new JLabel("Join UC Grand Hotel");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        subtitle.setForeground(TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(subtitle);
        
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(headerPanel, gbc);

        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridy = 1;
        gbc.insets = new Insets(30, 0, 0, 0);
        formPanel.add(Box.createVerticalStrut(0), gbc);

        // Form fields with modern styling
        JTextField usernameField = createModernTextField("Username");
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 16, 0);
        formPanel.add(createLabeledField("Username", usernameField, true), gbc);

        JPasswordField passwordField = createModernPasswordField("Password");
        JPanel passwordPanel = createPasswordFieldWithToggle(passwordField);
        gbc.gridy = 3;
        formPanel.add(createLabeledField("Password", passwordPanel, true), gbc);
        
        // Password strength indicator
        JPanel strengthPanel = createPasswordStrengthIndicator();
        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 16, 0);
        formPanel.add(strengthPanel, gbc);
        
        // Add password field listener for strength
        passwordField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateStrength(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateStrength(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateStrength(); }
            
            private void updateStrength() {
                String pwd = new String(passwordField.getPassword());
                updatePasswordStrength(strengthPanel, pwd);
            }
        });

        // Name fields in a row
        JPanel namePanel = new JPanel(new GridLayout(1, 2, 12, 0));
        namePanel.setOpaque(false);
        
        JTextField firstNameField = createModernTextField("First Name");
        JPanel firstNamePanel = createLabeledField("First Name", firstNameField, true);
        
        JTextField lastNameField = createModernTextField("Last Name");
        JPanel lastNamePanel = createLabeledField("Last Name", lastNameField, true);
        
        namePanel.add(firstNamePanel);
        namePanel.add(lastNamePanel);
        
        gbc.gridy = 5;
        formPanel.add(namePanel, gbc);

        JTextField emailField = createModernTextField("Email");
        gbc.gridy = 6;
        formPanel.add(createLabeledField("Email", emailField, true), gbc);

        // Terms and conditions checkbox
        JPanel termsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        termsPanel.setOpaque(false);
        
        JCheckBox termsCheckbox = new JCheckBox();
        termsCheckbox.setOpaque(false);
        termsCheckbox.setFocusPainted(false);
        
        JLabel termsLabel = new JLabel("<html>I agree to the <span style='color: rgb(212,175,55);'>Terms & Conditions</span></html>");
        termsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        termsLabel.setForeground(TEXT_SECONDARY);
        
        termsPanel.add(termsCheckbox);
        termsPanel.add(Box.createHorizontalStrut(8));
        termsPanel.add(termsLabel);
        
        gbc.gridy = 7;
        gbc.insets = new Insets(8, 0, 20, 0);
        formPanel.add(termsPanel, gbc);

        // Sign up button with enhanced styling
        JButton signUpBtn = createModernButton("Create Account", ACCENT_GOLD);
        signUpBtn.addActionListener(e -> {
            // Validate all fields
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            String firstName = firstNameField.getText().trim();
            String lastName = lastNameField.getText().trim();
            String email = emailField.getText().trim();

            // Validation checks
            if (username.isEmpty() || password.isEmpty() || firstName.isEmpty() || 
                lastName.isEmpty() || email.isEmpty()) {
                showModernDialog(this, 
                    "Please fill in all required fields", 
                    "Validation Error", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            if (username.length() < 3) {
                showModernDialog(this, 
                    "Username must be at least 3 characters long", 
                    "Validation Error", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            if (password.length() < 6) {
                showModernDialog(this, 
                    "Password must be at least 6 characters long", 
                    "Validation Error", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                showModernDialog(this, 
                    "Please enter a valid email address", 
                    "Validation Error", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            if (!termsCheckbox.isSelected()) {
                showModernDialog(this, 
                    "Please accept the Terms & Conditions to continue", 
                    "Terms Required", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Show loading state
            signUpBtn.setEnabled(false);
            signUpBtn.setText("Creating Account...");
            
            // Process signup
            SwingUtilities.invokeLater(() -> {
                try {
                    HibernateAdminService adminService = new HibernateAdminService(
                        userRepository, 
                        new PasswordEncoder()
                    );
                    User newUser = adminService.createUser(
                        username, 
                        password, 
                        firstName, 
                        lastName, 
                        email, 
                        "CUSTOMER", 
                        null
                    );
                    
                    signUpBtn.setEnabled(true);
                    signUpBtn.setText("Create Account");
                    
                    showSuccessDialog(this, firstName);
                    signUpCallback.accept(newUser);
                    
                } catch (Exception ex) {
                    signUpBtn.setEnabled(true);
                    signUpBtn.setText("Create Account");
                    
                    showModernDialog(this, 
                        "Error creating account: " + ex.getMessage(), 
                        "Sign Up Failed", 
                        JOptionPane.ERROR_MESSAGE);
                }
            });
        });
        
        gbc.gridy = 8;
        gbc.insets = new Insets(0, 0, 16, 0);
        formPanel.add(signUpBtn, gbc);

        // Back to login link
        JPanel backPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        backPanel.setOpaque(false);
        
        JLabel backLabel = new JLabel("Already have an account?");
        backLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        backLabel.setForeground(TEXT_SECONDARY);
        
        JButton backBtn = new JButton("Sign In");
        backBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        backBtn.setForeground(ACCENT_GOLD);
        backBtn.setBorderPainted(false);
        backBtn.setContentAreaFilled(false);
        backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backBtn.setFocusPainted(false);
        addHoverEffect(backBtn, ACCENT_GOLD, ACCENT_GOLD_HOVER);
        backBtn.addActionListener(e -> backToLogin.run());
        
        backPanel.add(backLabel);
        backPanel.add(backBtn);
        
        gbc.gridy = 9;
        gbc.insets = new Insets(0, 0, 0, 0);
        formPanel.add(backPanel, gbc);

        rightContainer.add(formPanel);
        add(rightContainer, BorderLayout.CENTER);
    }
    
    private JPanel createBrandingPanel(DatabaseConfig config) {
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(DEEP_NAVY);
        leftPanel.setPreferredSize(new Dimension(480, 0));
        
        // Gradient overlay panel
        JPanel contentPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                
                GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(15, 23, 42, 255),
                    0, getHeight(), new Color(30, 41, 59, 200)
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        contentPanel.setLayout(new GridBagLayout());
        contentPanel.setOpaque(false);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.insets = new Insets(20, 40, 20, 40);
        
        // Logo/Image container
        JPanel imageContainer = new JPanel(new BorderLayout());
        imageContainer.setOpaque(false);
        imageContainer.setPreferredSize(new Dimension(380, 380));
        
        JLabel hero = new JLabel();
        hero.setHorizontalAlignment(SwingConstants.CENTER);
        hero.setVerticalAlignment(SwingConstants.CENTER);
        hero.setOpaque(false);
        ImageIcon icon = ImageLoader.load("assets/herbanet-lobby.png", 380, 380);
        if (icon != null) {
            hero.setIcon(icon);
        }
        imageContainer.add(hero, BorderLayout.CENTER);
        
        gbc.gridy = 0;
        contentPanel.add(imageContainer, gbc);
        
        gbc.gridy = 1;
        contentPanel.add(Box.createVerticalStrut(30), gbc);
        
        // Brand section
        JPanel brandPanel = new JPanel();
        brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.Y_AXIS));
        brandPanel.setOpaque(false);
        brandPanel.setMaximumSize(new Dimension(380, 200));
        
        JLabel hotelName = new JLabel("UC GRAND HOTEL");
        hotelName.setFont(new Font("Segoe UI", Font.BOLD, 42));
        hotelName.setForeground(Color.WHITE);
        hotelName.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel tagline = new JLabel(config.getTagline());
        tagline.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        tagline.setForeground(ACCENT_GOLD);
        tagline.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel description = new JLabel("<html><div style='text-align: center; color: rgba(248, 250, 252, 0.7);'>" +
            "Begin your journey with us and unlock exclusive benefits and world-class service" +
            "</div></html>");
        description.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        description.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        brandPanel.add(hotelName);
        brandPanel.add(Box.createVerticalStrut(12));
        brandPanel.add(tagline);
        brandPanel.add(Box.createVerticalStrut(16));
        brandPanel.add(description);
        
        gbc.gridy = 2;
        contentPanel.add(brandPanel, gbc);
        
        leftPanel.add(contentPanel, BorderLayout.CENTER);
        return leftPanel;
    }
    
    private JTextField createModernTextField(String placeholder) {
        JTextField field = new JTextField(25);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ACCENT_GOLD, 2, true),
                    BorderFactory.createEmptyBorder(11, 13, 11, 13)
                ));
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                    BorderFactory.createEmptyBorder(12, 14, 12, 14)
                ));
            }
        });
        
        return field;
    }
    
    private JPasswordField createModernPasswordField(String placeholder) {
        JPasswordField field = new JPasswordField(25);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ACCENT_GOLD, 2, true),
                    BorderFactory.createEmptyBorder(11, 13, 11, 13)
                ));
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                    BorderFactory.createEmptyBorder(12, 14, 12, 14)
                ));
            }
        });
        
        return field;
    }
    
    private JPanel createPasswordFieldWithToggle(JPasswordField passwordField) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        
        JButton toggleBtn = new JButton("👁");
        toggleBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        toggleBtn.setBorderPainted(false);
        toggleBtn.setContentAreaFilled(false);
        toggleBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggleBtn.setFocusPainted(false);
        toggleBtn.setPreferredSize(new Dimension(40, 46));
        
        toggleBtn.addActionListener(e -> {
            if (passwordField.getEchoChar() == (char) 0) {
                passwordField.setEchoChar('•');
                toggleBtn.setText("👁");
            } else {
                passwordField.setEchoChar((char) 0);
                toggleBtn.setText("🙈");
            }
        });
        
        panel.add(passwordField, BorderLayout.CENTER);
        panel.add(toggleBtn, BorderLayout.EAST);
        
        return panel;
    }
    
    private JPanel createPasswordStrengthIndicator() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(false);
        
        JPanel barsPanel = new JPanel(new GridLayout(1, 4, 4, 0));
        barsPanel.setOpaque(false);
        barsPanel.setPreferredSize(new Dimension(0, 4));
        
        for (int i = 0; i < 4; i++) {
            JPanel bar = new JPanel();
            bar.setBackground(BORDER_COLOR);
            bar.setName("bar" + i);
            barsPanel.add(bar);
        }
        
        JLabel strengthLabel = new JLabel("");
        strengthLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        strengthLabel.setForeground(TEXT_SECONDARY);
        strengthLabel.setName("strengthLabel");
        
        panel.add(barsPanel, BorderLayout.CENTER);
        panel.add(strengthLabel, BorderLayout.EAST);
        panel.setName("strengthPanel");
        
        return panel;
    }
    
    private void updatePasswordStrength(JPanel strengthPanel, String password) {
        int strength = calculatePasswordStrength(password);
        
        JPanel barsPanel = (JPanel) strengthPanel.getComponent(0);
        JLabel label = (JLabel) strengthPanel.getComponent(1);
        
        Color[] colors = {BORDER_COLOR, ERROR_RED, new Color(251, 146, 60), SUCCESS_GREEN};
        String[] labels = {"", "Weak", "Fair", "Strong"};
        
        for (int i = 0; i < 4; i++) {
            JPanel bar = (JPanel) barsPanel.getComponent(i);
            bar.setBackground(i < strength ? colors[strength] : BORDER_COLOR);
        }
        
        label.setText(labels[strength]);
        label.setForeground(colors[strength]);
    }
    
    private int calculatePasswordStrength(String password) {
        if (password.length() == 0) return 0;
        if (password.length() < 6) return 1;
        
        int score = 1;
        if (password.length() >= 8) score++;
        if (password.matches(".*[A-Z].*") && password.matches(".*[a-z].*")) score++;
        if (password.matches(".*\\d.*") && password.matches(".*[!@#$%^&*].*")) score++;
        
        return Math.min(score, 3);
    }
    
    private JButton createModernButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 16));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(0, 50));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(ACCENT_GOLD_HOVER);
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });
        
        return button;
    }
    
    private void addHoverEffect(JButton button, Color normalColor, Color hoverColor) {
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setForeground(hoverColor);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setForeground(normalColor);
            }
        });
    }
    
    private void showModernDialog(Component parent, String message, String title, int messageType) {
        JOptionPane.showMessageDialog(parent, message, title, messageType);
    }
    
    private void showSuccessDialog(Component parent, String firstName) {
        JOptionPane.showMessageDialog(parent, 
            "Account created successfully! Welcome to UC Grand Hotel, " + firstName + "!", 
            "Welcome! 🎉", 
            JOptionPane.INFORMATION_MESSAGE);
    }

    private JPanel createLabeledField(String label, JComponent component, boolean required) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        
        JLabel lbl = new JLabel(label + (required ? " *" : ""));
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(TEXT_PRIMARY);
        
        panel.add(lbl, BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);
        
        return panel;
    }
    
    // Custom shadow border for depth
    private static class ShadowBorder extends AbstractBorder {
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2d.setColor(new Color(0, 0, 0, 8));
            g2d.fillRoundRect(x + 2, y + 2, width - 4, height - 4, 32, 32);
            
            g2d.dispose();
        }
        
        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(2, 2, 2, 2);
        }
    }
}