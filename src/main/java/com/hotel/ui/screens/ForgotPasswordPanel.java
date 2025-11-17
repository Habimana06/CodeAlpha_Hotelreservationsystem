package com.hotel.ui.screens;

import com.hotel.config.DatabaseConfig;
import com.hotel.repository.HibernateUserRepository;
import com.hotel.service.PasswordEncoder;
import com.hotel.ui.components.RoundedPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Optional;
import com.hotel.model.User;

public class ForgotPasswordPanel extends JPanel {
    private static final Color ACCENT_GOLD = new Color(212, 175, 55);
    private static final Color ACCENT_GOLD_HOVER = new Color(184, 153, 48);
    private static final Color SOFT_WHITE = new Color(248, 250, 252);
    private static final Color CARD_WHITE = Color.WHITE;
    private static final Color TEXT_PRIMARY = new Color(0, 0, 0);
    private static final Color TEXT_SECONDARY = new Color(100, 116, 139);
    private static final Color BORDER_COLOR = new Color(226, 232, 240);
    
    private final HibernateUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DatabaseConfig config;
    private Runnable onBackToLogin;
    private User verifiedUser;
    private JPanel card;
    private GridBagConstraints cardGbc;
    
    public ForgotPasswordPanel(HibernateUserRepository userRepository, PasswordEncoder passwordEncoder, DatabaseConfig config) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.config = config;
        
        setLayout(new GridBagLayout());
        setBackground(SOFT_WHITE);
        initializeUI();
    }
    
    public void setOnBackToLogin(Runnable onBackToLogin) {
        this.onBackToLogin = onBackToLogin;
    }
    
    private void initializeUI() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(20, 40, 20, 40);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        // Main card
        card = new RoundedPanel(20);
        card.setBackground(CARD_WHITE);
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(40, 40, 40, 40)
        ));
        
        cardGbc = new GridBagConstraints();
        cardGbc.insets = new Insets(10, 0, 10, 0);
        cardGbc.fill = GridBagConstraints.HORIZONTAL;
        cardGbc.weightx = 1.0;
        
        showUsernameVerificationStep();
        
        // Add card to panel
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weighty = 1.0;
        add(card, gbc);
    }
    
    private void showUsernameVerificationStep() {
        card.removeAll();
        
        // Title
        JLabel title = new JLabel("🔑 Reset Password");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(TEXT_PRIMARY);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        cardGbc.gridy = 0;
        card.add(title, cardGbc);
        
        // Subtitle
        JLabel subtitle = new JLabel("Enter your username to verify your account");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(TEXT_SECONDARY);
        subtitle.setHorizontalAlignment(SwingConstants.CENTER);
        cardGbc.gridy = 1;
        cardGbc.insets = new Insets(5, 0, 20, 0);
        card.add(subtitle, cardGbc);
        
        // Username field
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        usernameLabel.setForeground(TEXT_PRIMARY);
        cardGbc.gridy = 2;
        cardGbc.insets = new Insets(10, 0, 5, 0);
        card.add(usernameLabel, cardGbc);
        
        JTextField usernameField = createModernTextField("Enter your username", "👤");
        usernameField.setPreferredSize(new Dimension(0, 45));
        cardGbc.gridy = 3;
        cardGbc.insets = new Insets(0, 0, 30, 0);
        card.add(usernameField, cardGbc);
        
        // Verify button
        JButton verifyBtn = createModernButton("Verify Username", ACCENT_GOLD);
        verifyBtn.setPreferredSize(new Dimension(0, 50));
        verifyBtn.addActionListener(e -> {
            String username = usernameField.getText().trim();
            
            if (username.isEmpty()) {
                showDialog("Please enter your username.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Verify username exists
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                showDialog("Username not found. Please check your username and try again.", "User Not Found", JOptionPane.ERROR_MESSAGE);
                usernameField.setText("");
                return;
            }
            
            // Username verified, proceed to password reset step
            verifiedUser = userOpt.get();
            showPasswordResetStep();
        });
        cardGbc.gridy = 4;
        cardGbc.insets = new Insets(0, 0, 20, 0);
        card.add(verifyBtn, cardGbc);
        
        // Back to login link
        JButton backBtn = new JButton("← Back to Login");
        backBtn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        backBtn.setForeground(ACCENT_GOLD);
        backBtn.setBorderPainted(false);
        backBtn.setContentAreaFilled(false);
        backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backBtn.setFocusPainted(false);
        backBtn.addActionListener(e -> {
            if (onBackToLogin != null) {
                onBackToLogin.run();
            }
        });
        cardGbc.gridy = 5;
        cardGbc.insets = new Insets(0, 0, 0, 0);
        card.add(backBtn, cardGbc);
        
        card.revalidate();
        card.repaint();
    }
    
    private void showPasswordResetStep() {
        card.removeAll();
        
        // Title
        JLabel title = new JLabel("✅ Username Verified");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(TEXT_PRIMARY);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        cardGbc.gridy = 0;
        card.add(title, cardGbc);
        
        // Subtitle
        JLabel subtitle = new JLabel("Username: " + verifiedUser.getUsername());
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(new Color(34, 197, 94));
        subtitle.setHorizontalAlignment(SwingConstants.CENTER);
        cardGbc.gridy = 1;
        cardGbc.insets = new Insets(5, 0, 20, 0);
        card.add(subtitle, cardGbc);
        
        // New password field
        JLabel newPasswordLabel = new JLabel("New Password:");
        newPasswordLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        newPasswordLabel.setForeground(TEXT_PRIMARY);
        cardGbc.gridy = 2;
        cardGbc.insets = new Insets(10, 0, 5, 0);
        card.add(newPasswordLabel, cardGbc);
        
        JPasswordField newPasswordField = createModernPasswordField("Enter new password", "🔒");
        newPasswordField.setPreferredSize(new Dimension(0, 45));
        cardGbc.gridy = 3;
        cardGbc.insets = new Insets(0, 0, 20, 0);
        card.add(newPasswordField, cardGbc);
        
        // Confirm password field
        JLabel confirmPasswordLabel = new JLabel("Confirm Password:");
        confirmPasswordLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        confirmPasswordLabel.setForeground(TEXT_PRIMARY);
        cardGbc.gridy = 4;
        cardGbc.insets = new Insets(10, 0, 5, 0);
        card.add(confirmPasswordLabel, cardGbc);
        
        JPasswordField confirmPasswordField = createModernPasswordField("Confirm new password", "🔒");
        confirmPasswordField.setPreferredSize(new Dimension(0, 45));
        cardGbc.gridy = 5;
        cardGbc.insets = new Insets(0, 0, 30, 0);
        card.add(confirmPasswordField, cardGbc);
        
        // Reset button
        JButton resetBtn = createModernButton("Update Password", ACCENT_GOLD);
        resetBtn.setPreferredSize(new Dimension(0, 50));
        resetBtn.addActionListener(e -> {
            String newPassword = new String(newPasswordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());
            
            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                showDialog("Please fill in all fields.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            if (!newPassword.equals(confirmPassword)) {
                showDialog("Passwords do not match. Please try again.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            if (newPassword.length() < 6) {
                showDialog("Password must be at least 6 characters long.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Update password
            verifiedUser.setPasswordHash(passwordEncoder.encode(newPassword));
            userRepository.update(verifiedUser);
            
            showDialog("Password updated successfully! You can now login with your new password.", "Success", JOptionPane.INFORMATION_MESSAGE);
            
            // Go back to login
            if (onBackToLogin != null) {
                onBackToLogin.run();
            }
        });
        cardGbc.gridy = 6;
        cardGbc.insets = new Insets(0, 0, 20, 0);
        card.add(resetBtn, cardGbc);
        
        // Back to login link
        JButton backBtn = new JButton("← Back to Login");
        backBtn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        backBtn.setForeground(ACCENT_GOLD);
        backBtn.setBorderPainted(false);
        backBtn.setContentAreaFilled(false);
        backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backBtn.setFocusPainted(false);
        backBtn.addActionListener(e -> {
            if (onBackToLogin != null) {
                onBackToLogin.run();
            }
        });
        cardGbc.gridy = 7;
        cardGbc.insets = new Insets(0, 0, 0, 0);
        card.add(backBtn, cardGbc);
        
        card.revalidate();
        card.repaint();
    }
    
    private JTextField createModernTextField(String placeholder, String icon) {
        JTextField field = new JTextField(placeholder);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setForeground(TEXT_SECONDARY);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(TEXT_PRIMARY);
                }
            }
            
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(TEXT_SECONDARY);
                }
            }
        });
        return field;
    }
    
    private JPasswordField createModernPasswordField(String placeholder, String icon) {
        JPasswordField field = new JPasswordField(placeholder);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setForeground(TEXT_SECONDARY);
        field.setEchoChar((char) 0);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (new String(field.getPassword()).equals(placeholder)) {
                    field.setText("");
                    field.setForeground(TEXT_PRIMARY);
                    field.setEchoChar('•');
                }
            }
            
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (field.getPassword().length == 0) {
                    field.setEchoChar((char) 0);
                    field.setText(placeholder);
                    field.setForeground(TEXT_SECONDARY);
                }
            }
        });
        return field;
    }
    
    private JButton createModernButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 16));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        Color originalBg = button.getBackground();
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(ACCENT_GOLD_HOVER);
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(originalBg);
            }
        });
        
        return button;
    }
    
    private void showDialog(String message, String title, int messageType) {
        JOptionPane.showMessageDialog(this, message, title, messageType);
    }
}

