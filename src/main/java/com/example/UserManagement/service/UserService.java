package com.example.UserManagement.service;
import java.util.List;
import java.util.Optional;
import com.example.UserManagement.entity.User;

public interface UserService {
    User saveUser(User user);


    Optional<User> findUserById(Integer id);

    Optional<User> findByUsername(String username);

    List<User> findAllUsers();


    void deleteUser(Integer id);
 

    void updateUserProfile(Integer userId, User user);


    void updateUserProfile(Integer id, String username, String email, String firstName, String lastName,
            String newPassword, String confirmPassword);


    User findByEmail(String email);


   

   
}
