package com.example.UserManagement.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.UserManagement.entity.User;
import com.example.UserManagement.entity.Enums.Role;
import com.example.UserManagement.entity.Enums.Status;
import com.example.UserManagement.repository.UserRepository;
import com.example.UserManagement.service.UserService;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;

@Controller
class UserController {
    private final UserService userService;
    private final UserRepository userRepository;

    @Autowired
    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    // Route for the main landing page
    @GetMapping("/")
    public String showLandingPage() {
        return "index";
    }

    // Handles user registration

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }



    @PostMapping("/api/register")
    @ResponseBody
    public String registerUserApi(@RequestBody User user) {
        try {
            if (user.getRole() == null) {
                user.setRole(Role.STUDENT);
            }
            userService.saveUser(user);
            return "User registered successfully: " + user.getUsername();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") User user, Model model) {
        try {
            if (user.getRole() == null) {
                user.setRole(Role.STUDENT);
            }
            if (user.getStatus()==null) {
                user.setStatus(Status.ACTIVE);
            }
            userService.saveUser(user);
            return "redirect:/login?registered";
        } catch (IllegalStateException e) {
            model.addAttribute("registrationError", e.getMessage());
            model.addAttribute("user", user);
            return "register";
        }
    }


    // Handles manual user login

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }



    @PostMapping("/login")
    public String manualLogin(@RequestParam("username") String username,
                              @RequestParam("password") String password,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {

        User user = userService.findByUsername(username).orElse(null);

        if (user != null && user.getPassword().equals(password)) {

            session.setAttribute("loggedInUserId", user.getId());
            session.setAttribute("loggedInUserRole", user.getRole().name());
            session.setAttribute("loggedInUsername", user.getUsername());


            switch (user.getRole()) {
                case STUDENT:
                    return "redirect:/student/dashboard";
                case STAFF:
                    return "redirect:/staff/dashboard";
                case ADMIN:
                     return "redirect:/admin/dashboard";
                default:
                    return "redirect:/admin/dashboard";
            }

        } else {
            redirectAttributes.addFlashAttribute("loginError", "Invalid username or password.");
            return "redirect:/login";
        }
    }

    // Handles manual user logout

    @PostMapping("/logout")
    public String manualLogout(HttpSession session) {

        session.invalidate();

        return "redirect:/login?logout";
    }

    // Handles admin-specific user registration
    @GetMapping("/admin/register")
    public String showAdminRegistrationForm(Model model) {

        if (!model.containsAttribute("user")) {
            model.addAttribute("user", new User());
        }

        model.addAttribute("allRoles", Role.values());
        model.addAttribute("allStatuses", Status.values());
        return "admin/register-form";
    }




    @PostMapping("/admin/register")
    public String adminRegisterUser(@ModelAttribute("user") User user,
                                    RedirectAttributes redirectAttributes) {
        try {
            if (user.getStatus() == null) {
                user.setStatus(Status.ACTIVE);
            }
            if (user.getRole() == null) {
                user.setRole(Role.STUDENT);
            }
            userService.saveUser(user);
            redirectAttributes.addFlashAttribute("successMessage", "User '" + user.getUsername() + "' created successfully with role: " + user.getRole());
            return "redirect:/admin/register";

        } catch (IllegalStateException e) {

            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("user", user);
            return "redirect:/admin/register";
        } catch (Exception e) {

            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred: " + e.getMessage());
            redirectAttributes.addFlashAttribute("user", user);
            return "redirect:/admin/register";
        }
    }


    // Handles admin-level user management (list, edit, update)
    private User getCurrentUser(HttpSession session) {
        Object userIdAttribute = session.getAttribute("loggedInUserId");
        if (userIdAttribute == null) {
            throw new RuntimeException("User not authenticated.");
        }
        Integer userId;
        if (userIdAttribute instanceof Integer) {
            userId = (Integer) userIdAttribute;
        } else if (userIdAttribute instanceof Long) {
            userId = ((Long) userIdAttribute).intValue();
        } else {
            throw new RuntimeException("User not authenticated.");
        }
        return userService.findUserById(userId)
                .orElseThrow(() -> new RuntimeException("Logged-in user is missing from database."));
    }


    private void checkAdminAccess(HttpSession session) throws AccessDeniedException {
        User currentUser = getCurrentUser(session);
        if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Access denied. Admin role required.");
        }
    }


    @GetMapping("/admin/users")
    public String listUsers(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            checkAdminAccess(session);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        }

        List<User> users = userService.findAllUsers();
        model.addAttribute("users", users);
        return "admin/user-list";
    }


    @GetMapping("/admin/users/edit/{id}")
    public String showEditUserForm(@PathVariable Integer id, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            checkAdminAccess(session);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        }

        User user = userService.findUserById(id)
                .orElse(null);

        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "User not found with ID: " + id);
            return "redirect:/admin/users";
        }

        model.addAttribute("user", user);
        model.addAttribute("allRoles", Role.values());
        return "admin/user-edit-form";
    }


    @PostMapping("/admin/users/update")
    public String updateUser(@ModelAttribute("user") User userFormData,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        try {
            checkAdminAccess(session);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        }

        try {

            User existingUser = userService.findUserById(userFormData.getId())
                    .orElseThrow(() -> new RuntimeException("User not found for update."));


            existingUser.setUsername(userFormData.getUsername());
            existingUser.setEmail(userFormData.getEmail());
            existingUser.setFirstName(userFormData.getFirstName());
            existingUser.setLastName(userFormData.getLastName());
            existingUser.setRole(userFormData.getRole());


            userService.saveUser(existingUser);
            redirectAttributes.addFlashAttribute("successMessage", "User '" + existingUser.getUsername() + "' updated successfully.");

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/users/edit/" + userFormData.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating user: " + e.getMessage());
            return "redirect:/admin/users/edit/" + userFormData.getId();
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/delete/{id}")
    public String deleteUser(@PathVariable Integer id,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        try {
            checkAdminAccess(session);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        }

        User current = getCurrentUser(session);
        if (current.getId().equals(id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "You cannot delete your own account.");
            return "redirect:/admin/users/edit/" + id;
        }

        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "User has been deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not delete user: " + e.getMessage());
            return "redirect:/admin/users/edit/" + id;
        }
        return "redirect:/admin/users";
    }

@GetMapping("/student/dashboard")
public String studentDashboard(HttpSession session, Model model) {
    try {
        User user = getCurrentUser(session);
        if (user.getRole() != Role.STUDENT) return "redirect:/login";
        model.addAttribute("user", user);
        return "student/dashboard";
    } catch (Exception e) {
        return "redirect:/login";
    }
}

@GetMapping("/staff/dashboard")
public String staffDashboard(HttpSession session, Model model) {
    try {
        User user = getCurrentUser(session);
        if (user.getRole() != Role.STAFF) return "redirect:/login";
        model.addAttribute("user", user);
        return "staff/dashboard";
    } catch (Exception e) {
        return "redirect:/login";
    }
}

@GetMapping("/admin/dashboard")
public String adminDashboard(HttpSession session, Model model) {
    try {
        User user = getCurrentUser(session);
        if (user.getRole() != Role.ADMIN) return "redirect:/login";
        model.addAttribute("user", user);
        model.addAttribute("totalUsersCount", userRepository.count());
        model.addAttribute("studentsCount", userRepository.countByRole(Role.STUDENT));
        model.addAttribute("staffCount", userRepository.countByRole(Role.STAFF));
        model.addAttribute("adminsCount", userRepository.countByRole(Role.ADMIN));
        return "admin/dashboard";
    } catch (Exception e) {
        return "redirect:/login";
    }
}


}