package com.example.UserManagement.service.Impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.UserManagement.entity.User;
import com.example.UserManagement.repository.UserRepository;
import com.example.UserManagement.service.UserService;

@Service
public class UserServiseImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiseImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User saveUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        // Check for duplicate username when creating new user (id is null)
        if (user.getId() == null) {
            if (user.getUsername() != null && userRepository.findByUsername(user.getUsername()).isPresent()) {
                throw new IllegalStateException("Username already exists: " + user.getUsername());
            }
            if (user.getEmail() != null && userRepository.findByEmail(user.getEmail()).isPresent()) {
                throw new IllegalStateException("Email already registered: " + user.getEmail());
            }
        }
        return userRepository.save(user);
    }

    @Override
    public Optional<User> findUserById(Integer id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public void deleteUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("User not found with ID: " + id));
        userRepository.delete(user);
    }

    @Override
    public void updateUserProfile(Integer userId, User user) {
        User existing = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found with ID: " + userId));
        existing.setUsername(user.getUsername());
        existing.setEmail(user.getEmail());
        existing.setFirstName(user.getFirstName());
        existing.setLastName(user.getLastName());
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existing.setPassword(user.getPassword());
        }
        userRepository.save(existing);
    }

    @Override
    public void updateUserProfile(Integer id, String username, String email, String firstName, String lastName,
            String newPassword, String confirmPassword) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("User not found with ID: " + id));
        existing.setUsername(username);
        existing.setEmail(email);
        existing.setFirstName(firstName);
        existing.setLastName(lastName);
        if (newPassword != null && !newPassword.isEmpty() && newPassword.equals(confirmPassword)) {
            existing.setPassword(newPassword);
        }
        userRepository.save(existing);
    }

    @Override
    public User findByEmail(String email) {
        throw new UnsupportedOperationException("Unimplemented method 'findByEmail'");
    }

}
