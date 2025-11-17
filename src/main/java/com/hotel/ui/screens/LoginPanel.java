package com.hotel.ui.screens;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.AbstractBorder;

import com.hotel.config.DatabaseConfig;
import com.hotel.model.User;
import com.hotel.repository.HibernateUserRepository;
import com.hotel.service.AuthService;
import com.hotel.ui.ImageLoader;
import com.hotel.ui.components.RoundedPanel;

public class LoginPanel extends JPanel {
    private Consumer<User> signUpCallback;
    private Runnable showSignUp;
    private Runnable showForgotPassword;
    
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
    
    public LoginPanel(AuthService authService, Consumer<User> loginCallback, 
                     DatabaseConfig config, HibernateUserRepository userRepository) {
        setLayout(new BorderLayout());
        setBackground(SOFT_WHITE);

        // Left side with enhanced branding and image
        JPanel leftPanel = createBrandingPanel(config);
        add(leftPanel, BorderLayout.WEST);

        // Right side with modern login form
        JPanel rightContainer = new JPanel(new GridBagLayout());
        rightContainer.setBackground(SOFT_WHITE);
        
        RoundedPanel formPanel = new RoundedPanel(32);
        formPanel.setBackground(Color.WHITE);
        formPanel.setLayout(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createCompoundBorder(
            new ShadowBorder(),
            BorderFactory.createEmptyBorder(60, 70, 60, 70)
        ));
        formPanel.setPreferredSize(new Dimension(520, 600));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0;

        // Modern header with icon
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);
        
        // Icon badge
        JLabel iconBadge = new JLabel("🏨");
        iconBadge.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        iconBadge.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(iconBadge);
        headerPanel.add(Box.createVerticalStrut(16));
        
        JLabel title = new JLabel("Welcome Back");
        title.setFont(new Font("Segoe UI", Font.BOLD, 36));
        title.setForeground(TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(title);
        
        headerPanel.add(Box.createVerticalStrut(8));
        
        JLabel subtitle = new JLabel("Sign in to access your account");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        subtitle.setForeground(TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(subtitle);
        
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(headerPanel, gbc);

        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridy = 1;
        gbc.insets = new Insets(40, 0, 0, 0);
        formPanel.add(Box.createVerticalStrut(0), gbc);

        // Enhanced username field with icon
        JTextField usernameField = createModernTextField("Username", "👤");
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 20, 0);
        formPanel.add(createLabeledField("Username", usernameField), gbc);

        // Enhanced password field with icon and show/hide toggle
        JPasswordField passwordField = createModernPasswordField("Password", "🔒");
        JPanel passwordPanel = createPasswordFieldWithToggle(passwordField);
        gbc.gridy = 3;
        formPanel.add(createLabeledField("Password", passwordPanel), gbc);

        // Remember me and forgot password row
        JPanel optionsPanel = new JPanel(new BorderLayout());
        optionsPanel.setOpaque(false);
        
        JCheckBox rememberMe = new JCheckBox("Remember me");
        rememberMe.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        rememberMe.setForeground(TEXT_SECONDARY);
        rememberMe.setOpaque(false);
        rememberMe.setFocusPainted(false);
        
        JButton forgotBtn = new JButton("Forgot password?");
        forgotBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        forgotBtn.setForeground(ACCENT_GOLD);
        forgotBtn.setBorderPainted(false);
        forgotBtn.setContentAreaFilled(false);
        forgotBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        forgotBtn.setFocusPainted(false);
        forgotBtn.addActionListener(e -> {
            if (showForgotPassword != null) {
                showForgotPassword.run();
            }
        });
        
        optionsPanel.add(rememberMe, BorderLayout.WEST);
        optionsPanel.add(forgotBtn, BorderLayout.EAST);
        
        gbc.gridy = 4;
        gbc.insets = new Insets(8, 0, 24, 0);
        formPanel.add(optionsPanel, gbc);

        // Modern login button with hover effect
        JButton loginBtn = createModernButton("Sign In", ACCENT_GOLD);
        loginBtn.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            
            if (username.isEmpty() || password.isEmpty()) {
                showModernDialog(this, "Please enter both username and password", 
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Add loading state
            loginBtn.setEnabled(false);
            loginBtn.setText("Signing in...");
            
            // Simulate async operation
            SwingUtilities.invokeLater(() -> {
                authService.authenticate(username, password)
                    .ifPresentOrElse(
                        user -> {
                            loginBtn.setEnabled(true);
                            loginBtn.setText("Sign In");
                            loginCallback.accept(user);
                        },
                        () -> {
                            loginBtn.setEnabled(true);
                            loginBtn.setText("Sign In");
                            showModernDialog(this, 
                                "Invalid username or password. Please try again.", 
                                "Login Failed", 
                                JOptionPane.ERROR_MESSAGE);
                        }
                    );
            });
        });
        
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 20, 0);
        formPanel.add(loginBtn, gbc);

        // Divider with "OR"
        JPanel dividerPanel = createDividerWithText("OR");
        gbc.gridy = 6;
        gbc.insets = new Insets(0, 0, 20, 0);
        formPanel.add(dividerPanel, gbc);

        // Sign up link with modern styling
        JPanel signUpPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        signUpPanel.setOpaque(false);
        
        JLabel signUpLabel = new JLabel("Don't have an account?");
        signUpLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        signUpLabel.setForeground(TEXT_SECONDARY);
        
        JButton signUpBtn = new JButton("Create Account");
        signUpBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        signUpBtn.setForeground(ACCENT_GOLD);
        signUpBtn.setBorderPainted(false);
        signUpBtn.setContentAreaFilled(false);
        signUpBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        signUpBtn.setFocusPainted(false);
        addHoverEffect(signUpBtn, ACCENT_GOLD, ACCENT_GOLD_HOVER);
        signUpBtn.addActionListener(e -> {
            if (showSignUp != null) {
                showSignUp.run();
            }
        });
        
        signUpPanel.add(signUpLabel);
        signUpPanel.add(signUpBtn);
        
        gbc.gridy = 7;
        gbc.insets = new Insets(0, 0, 0, 0);
        formPanel.add(signUpPanel, gbc);

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
                
                // Subtle gradient overlay
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
        
        // Brand section with enhanced typography
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
            "Experience luxury and comfort with our world-class hospitality services" +
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
    
    private JTextField createModernTextField(String placeholder, String icon) {
        JTextField field = new JTextField(25);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(14, 15, 14, 15)
        ));
        
        // Add focus effects
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ACCENT_GOLD, 2, true),
                    BorderFactory.createEmptyBorder(13, 14, 13, 14)
                ));
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                    BorderFactory.createEmptyBorder(14, 15, 14, 15)
                ));
            }
        });
        
        return field;
    }
    
    private JPasswordField createModernPasswordField(String placeholder, String icon) {
        JPasswordField field = new JPasswordField(25);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(14, 15, 14, 15)
        ));
        
        // Add focus effects
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ACCENT_GOLD, 2, true),
                    BorderFactory.createEmptyBorder(13, 14, 13, 14)
                ));
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                    BorderFactory.createEmptyBorder(14, 15, 14, 15)
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
        toggleBtn.setPreferredSize(new Dimension(40, 48));
        
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
    
    private JButton createModernButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 16));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(0, 52));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Add hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(ACCENT_GOLD_HOVER);
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
    
    private JPanel createDividerWithText(String text) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 0, 12);
        
        JSeparator leftSep = new JSeparator();
        leftSep.setForeground(BORDER_COLOR);
        panel.add(leftSep, gbc);
        
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        JLabel orLabel = new JLabel(text);
        orLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        orLabel.setForeground(TEXT_SECONDARY);
        panel.add(orLabel, gbc);
        
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 12, 0, 0);
        JSeparator rightSep = new JSeparator();
        rightSep.setForeground(BORDER_COLOR);
        panel.add(rightSep, gbc);
        
        return panel;
    }
    
    private void showModernDialog(Component parent, String message, String title, int messageType) {
        JOptionPane.showMessageDialog(parent, message, title, messageType);
    }
    
    public void setSignUpCallback(Consumer<User> callback) {
        this.signUpCallback = callback;
    }
    
    public void setShowSignUpAction(Runnable action) {
        this.showSignUp = action;
    }
    
    public void setShowForgotPasswordAction(Runnable action) {
        this.showForgotPassword = action;
    }

    private JPanel createLabeledField(String label, JComponent component) {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
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
            
            // Subtle shadow effect
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