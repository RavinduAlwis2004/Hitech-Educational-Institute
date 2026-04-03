package com.example.UserManagement.controller;
import com.example.UserManagement.entity.User;
import com.example.UserManagement.service.UserService;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;  
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

    @Autowired
    private UserService userService;

    // Helper method to get current user from session
    private User getCurrentUser(HttpSession session) {
        Object userIdAttribute = session.getAttribute("loggedInUserId");
        if (userIdAttribute == null) {
            throw new RuntimeException("User not authenticated or session invalid.");
        }
        Integer userId;
        if (userIdAttribute instanceof Integer) {
            userId = (Integer) userIdAttribute;
        } else if (userIdAttribute instanceof Long) {
            userId = ((Long) userIdAttribute).intValue();
        } else {
            throw new RuntimeException("User not authenticated or session invalid.");
        }
        return userService.findUserById(userId)
                .orElseThrow(() -> new RuntimeException("Logged-in user (ID: " + userId + ") not found in database."));
    }

    // --- Show Profile Edit Form (GET) ---
    @GetMapping("/edit")
    public String showProfileEditForm(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser(session);
            // Add user object to model for form binding, excluding password
            User userForForm = new User(); // Create a new DTO-like object
            userForForm.setId(currentUser.getId());
            userForForm.setUsername(currentUser.getUsername());
            userForForm.setEmail(currentUser.getEmail());
            userForForm.setFirstName(currentUser.getFirstName());
            userForForm.setLastName(currentUser.getLastName());
            userForForm.setRole(currentUser.getRole()); // Role might be needed for display but shouldn't be editable here

            model.addAttribute("user", userForForm);
            return "profile/profile-edit";
        } catch (RuntimeException e) {
            logger.warn("Failed to show profile edit form: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Please log in to edit your profile.");
            return "redirect:/login";
        }
    }

    // --- Handle Profile Update (POST) ---
    @PostMapping("/update")
    public String updateProfile(@ModelAttribute("user") User userFormData, // Contains updated username, email, names
                                @RequestParam(value = "newPassword", required = false) String newPassword,
                                @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User currentUser;
        try {
            currentUser = getCurrentUser(session);
            // Ensure the ID from the form matches the logged-in user to prevent manipulation
            if (!currentUser.getId().equals(userFormData.getId())) {
                throw new SecurityException("Attempt to update profile for different user.");
            }
        } catch (Exception e) {
            logger.error("Profile update failed - authentication error: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Authentication error. Please log in again.");
            return "redirect:/login";
        }

        try {
            // Call service method to handle update logic
            userService.updateUserProfile(
                    currentUser.getId(),
                    userFormData.getUsername(),
                    userFormData.getEmail(),
                    userFormData.getFirstName(),
                    userFormData.getLastName(),
                    newPassword,
                    confirmPassword
            );
            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");
            logger.info("User {} updated profile successfully.", currentUser.getUsername());

        } catch (IllegalArgumentException e) { // For validation errors like password mismatch
            logger.warn("Profile update validation failed for user {}: {}", currentUser.getUsername(), e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            // Add the submitted data back to the redirect model *except passwords*
            redirectAttributes.addFlashAttribute("user", userFormData);
            return "redirect:/profile/edit"; // Redirect back to edit form with error

        } catch (IllegalStateException e) { // For uniqueness constraint errors (username/email)
            logger.warn("Profile update failed for user {} due to conflict: {}", currentUser.getUsername(), e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("user", userFormData);
            return "redirect:/profile/edit"; // Redirect back to edit form with error

        } catch (Exception e) {
            logger.error("Unexpected error updating profile for user {}: {}", currentUser.getUsername(), e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred while updating profile.");
            return "redirect:/profile/edit"; // Redirect back to edit form with error
        }

        return "redirect:/profile/edit"; // Redirect back to edit form after success
    }


}
