package com.example.UserManagement.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "attendance", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "date"})
})
public class Attendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // This is the correct relationship - many attendances belong to one user
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate date;

    // Existing DB column is named `ispresent` (legacy); map to it so inserts succeed.
    @Column(name = "ispresent", nullable = false)
    private boolean isPresent;

    // Some environments also still have `is_present` as NOT NULL.
    // Keep both in sync to support either schema during transition.
    @Column(name = "is_present", nullable = false)
    private boolean isPresentCompat;
    
    private LocalDateTime checkInTime;
    
    private LocalDateTime checkOutTime;
    
    private String remarks;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        isPresentCompat = isPresent;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        isPresentCompat = isPresent;
    }

}