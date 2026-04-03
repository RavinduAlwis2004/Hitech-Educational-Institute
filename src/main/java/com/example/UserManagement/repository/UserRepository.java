package com.example.UserManagement.repository;

import com.example.UserManagement.entity.User;
import com.example.UserManagement.entity.Enums.Role;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    
    // Find by email - correct method name
    Optional<User> findByEmail(String email);
    
    // Find by username
    Optional<User> findByUsername(String username);
    
    // Find by role
    List<User> findByRole(Role role);

    long countByRole(Role role);
    
    // Check if email exists
    boolean existsByEmail(String email);
    
    // Custom query to find by ID (this is already provided by JpaRepository)
    // Optional<User> findById(Long id) - this exists by default

    Optional<User> findById(Integer id);

    



    
    // If you need to find active users by role
    List<User> findByRoleAndStatus(String role, String status);

    void deleteById(Integer id);
}