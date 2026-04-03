package com.example.UserManagement.service;

import com.example.UserManagement.entity.Attendance;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AttendanceService {

    List<Attendance> getAttendanceByUserId(Long userId);
    
    List<Attendance> getAttendanceByUserIdAndDateRange(Long userId, LocalDate startDate, LocalDate endDate);
    
    Map<String, Object> getAttendanceAnalytics(Long userId, LocalDate startDate, LocalDate endDate);

    Attendance addAttendance(Attendance attendance);
    
    Attendance markAttendance(Integer userId, LocalDate date, boolean isPresent, 
                             LocalDateTime checkInTime, LocalDateTime checkOutTime, String remarks);

    Attendance updateAttendance(Long id, Attendance attendance);

    void deleteAttendance(Long id);
    
    double calculateAttendancePercentage(Long userId, LocalDate startDate, LocalDate endDate);

    Map<String, Object> getAttendanceAnalytics(Integer userId, LocalDate startDate, LocalDate endDate);

    List<Attendance> getAttendanceByUserIdAndDateRange(Integer userId, LocalDate startDate, LocalDate endDate);

    Attendance markAttendance(Long userId, LocalDate date, boolean isPresent, LocalDateTime checkInTime,
            LocalDateTime checkOutTime, String remarks);
}