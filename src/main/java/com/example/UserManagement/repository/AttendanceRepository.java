package com.example.UserManagement.repository;

import com.example.UserManagement.entity.Attendance;
import com.example.UserManagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    
    // Find attendance by user ID
    List<Attendance> findByUserId(Long userId);
    
    // Find attendance by user ID and date range
    List<Attendance> findByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
    
    // Find attendance by user and date
    Optional<Attendance> findByUserAndDate(User user, LocalDate date);
    
    // Find attendance by user ID ordered by date descending
    List<Attendance> findByUserIdOrderByDateDesc(Long userId);
    
    // Count attendance by status and date range
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.user.id = :userId AND a.date BETWEEN :startDate AND :endDate AND a.isPresent = :isPresent")
    long countByUserIdAndIsPresentBetweenDates(@Param("userId") Long userId, 
                                               @Param("isPresent") boolean isPresent,
                                               @Param("startDate") LocalDate startDate, 
                                               @Param("endDate") LocalDate endDate);
    
    // Get monthly attendance statistics
    @Query("SELECT FUNCTION('MONTH', a.date), COUNT(a) FROM Attendance a WHERE a.user.id = :userId AND a.isPresent = true AND a.date BETWEEN :startDate AND :endDate GROUP BY FUNCTION('MONTH', a.date)")
    List<Object[]> getMonthlyAttendanceByUserId(@Param("userId") Long userId,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);
    
    // Get weekly attendance statistics
    @Query("SELECT a.date, a.isPresent FROM Attendance a WHERE a.user.id = :userId AND a.date BETWEEN :startDate AND :endDate ORDER BY a.date")
    List<Object[]> getDailyAttendanceByUserId(@Param("userId") Long userId,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);
}