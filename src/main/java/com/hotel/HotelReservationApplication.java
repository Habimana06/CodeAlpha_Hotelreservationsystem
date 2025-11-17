package com.hotel;

import com.hotel.config.DatabaseConfig;
import com.hotel.config.HibernateUtil;
import com.hotel.model.User;
import com.hotel.repository.HibernateUserRepository;
import com.hotel.service.AuthService;
import com.hotel.service.PasswordEncoder;
import com.hotel.ui.HerbanetTheme;
import com.hotel.ui.screens.ForgotPasswordPanel;
import com.hotel.ui.screens.LoginPanel;
import com.hotel.ui.screens.SignUpPanel;
import com.hotel.ui.screens.dashboards.*;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;

public class HotelReservationApplication extends JFrame {
    private final DatabaseConfig config = new DatabaseConfig();
    private final HibernateUserRepository userRepository = new HibernateUserRepository();
    private final PasswordEncoder passwordEncoder = new PasswordEncoder();
    private final AuthService authService = new AuthService(userRepository, passwordEncoder);
    private User currentUser;

    public HotelReservationApplication() {
        setTitle(config.getHotelName() + " - Reservation System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);
        
        // Set window icon (using emoji as icon since we don't have image file)
        try {
            // Create a simple icon from emoji
            ImageIcon icon = new ImageIcon(createIconImage());
            setIconImage(icon.getImage());
        } catch (Exception e) {
            // If icon creation fails, continue without it
        }
        
        // Initialize Hibernate
        try {
            HibernateUtil.getSessionFactory();
            // Initialize sample data
            new com.hotel.config.HibernateDataInitializer().initializeData();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Database connection failed: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            System.exit(1);
        }

        showLogin();
    }

    private void showLogin() {
        getContentPane().removeAll();
        LoginPanel loginPanel = new LoginPanel(authService, this::onLoginSuccess, config, userRepository);
        loginPanel.setShowSignUpAction(this::showSignUp);
        loginPanel.setShowForgotPasswordAction(this::showForgotPassword);
        add(loginPanel);
        revalidate();
        repaint();
    }
    
    private void showSignUp() {
        getContentPane().removeAll();
        SignUpPanel signUpPanel = new SignUpPanel(userRepository, this::onLoginSuccess, config, this::showLogin);
        add(signUpPanel);
        revalidate();
        repaint();
    }
    
    private void showForgotPassword() {
        getContentPane().removeAll();
        ForgotPasswordPanel forgotPasswordPanel = new ForgotPasswordPanel(userRepository, passwordEncoder, config);
        forgotPasswordPanel.setOnBackToLogin(this::showLogin);
        add(forgotPasswordPanel);
        revalidate();
        repaint();
    }

    private void onLoginSuccess(User user) {
        this.currentUser = user;
        getContentPane().removeAll();
        
        JPanel dashboard = null;
        String roleName = user.getRole().getName().toUpperCase();
        
        switch (roleName) {
            case "CUSTOMER":
                dashboard = new CustomerDashboard(user, userRepository, config, this::showLogin);
                break;
            case "RECEPTIONIST":
                dashboard = new ReceptionistDashboard(user, userRepository, config, this::showLogin);
                break;
            case "MANAGER":
                dashboard = new ManagerDashboard(user, userRepository, config, this::showLogin);
                break;
            case "ADMIN":
                dashboard = new AdminDashboard(user, userRepository, config, this::showLogin);
                break;
            default:
                JOptionPane.showMessageDialog(this, "Unknown role: " + roleName);
                showLogin();
                return;
        }
        
        add(dashboard);
        revalidate();
        repaint();
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
            UIManager.put("Button.arc", 12);
            UIManager.put("Component.arc", 12);
            UIManager.put("TextComponent.arc", 8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            new HotelReservationApplication().setVisible(true);
        });
    }
    
    private java.awt.Image createIconImage() {
        // Create a simple icon image with hotel emoji
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(32, 32, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(new Color(34, 197, 94)); // Green background
        g2d.fillRoundRect(0, 0, 32, 32, 8, 8);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        g2d.drawString("🏨", 6, 24);
        g2d.dispose();
        return img;
    }
}

