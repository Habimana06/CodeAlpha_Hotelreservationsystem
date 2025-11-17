package com.hotel.service;

import com.hotel.model.PrivilegeEntity;
import com.hotel.model.Role;
import com.hotel.model.User;
import com.hotel.repository.HibernateUserRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class HibernateAdminService {
    private final HibernateUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public HibernateAdminService(HibernateUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(String username, String rawPassword, String firstName, String lastName, 
                          String email, String roleName, Set<String> privilegeCodes) {
        Role role = userRepository.findRoleByName(roleName);
        if (role == null) {
            throw new IllegalArgumentException("Role not found: " + roleName);
        }
        
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setRole(role);
        user.setActive(true);
        
        if (privilegeCodes != null && !privilegeCodes.isEmpty()) {
            Set<PrivilegeEntity> privileges = privilegeCodes.stream()
                    .map(code -> userRepository.findPrivilegeByCode(code))
                    .filter(p -> p != null)
                    .collect(Collectors.toSet());
            user.setPrivileges(privileges);
        }
        
        return userRepository.save(user);
    }

    public void updateRole(long userId, String roleName) {
        Role role = userRepository.findRoleByName(roleName);
        if (role == null) {
            throw new IllegalArgumentException("Role not found: " + roleName);
        }
        userRepository.updateRole(userId, role);
    }

    public void assignPrivileges(long userId, Set<String> privilegeCodes) {
        Set<PrivilegeEntity> privileges = privilegeCodes.stream()
                .map(code -> userRepository.findPrivilegeByCode(code))
                .filter(p -> p != null)
                .collect(Collectors.toSet());
        userRepository.updateUserPrivileges(userId, privileges);
    }

    public List<User> listUsers() {
        return userRepository.findAll();
    }

    public List<Role> listRoles() {
        return userRepository.findAllRoles();
    }

    public List<PrivilegeEntity> listPrivileges() {
        return userRepository.findAllPrivileges();
    }

    public User updateUser(User user) {
        return userRepository.update(user);
    }
    
    public void assignPrivilegesToRole(long roleId, Set<String> privilegeCodes) {
        Role role = userRepository.findRoleById(roleId);
        if (role == null) {
            throw new IllegalArgumentException("Role not found with ID: " + roleId);
        }
        
        Set<PrivilegeEntity> privileges = privilegeCodes.stream()
                .map(code -> userRepository.findPrivilegeByCode(code))
                .filter(p -> p != null)
                .collect(Collectors.toSet());
        
        role.setPrivileges(privileges);
        userRepository.updateRole(role);
    }
}

