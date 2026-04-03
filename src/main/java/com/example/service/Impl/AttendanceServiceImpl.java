package com.example.UserManagement.service.Impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.UserManagement.entity.Attendance;
import com.example.UserManagement.entity.User;
import com.example.UserManagement.repository.AttendanceRepository;
import com.example.UserManagement.repository.UserRepository;
import com.example.UserManagement.service.AttendanceService;

@Service
public class AttendanceServiceImpl implements AttendanceService {
    
    @Autowired
    private AttendanceRepository attendanceRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public List<Attendance> getAttendanceByUserId(Long userId) {
        return attendanceRepository.findByUserIdOrderByDateDesc(userId);
    }
    
    @Override
    public List<Attendance> getAttendanceByUserIdAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return attendanceRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
    }
    
    @Override
    public Map<String, Object> getAttendanceAnalytics(Long userId, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> analytics = new HashMap<>();
        
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        
        
        long presentCount = attendanceRepository.countByUserIdAndIsPresentBetweenDates(userId, true, startDate, endDate);
        long absentCount = attendanceRepository.countByUserIdAndIsPresentBetweenDates(userId, false, startDate, endDate);
        
        
        double attendancePercentage = totalDays > 0 ? (presentCount * 100.0 / totalDays) : 0;
        
        
        List<Object[]> monthlyData = attendanceRepository.getMonthlyAttendanceByUserId(userId, startDate, endDate);
        Map<String, Long> monthlyAttendance = new LinkedHashMap<>();
        
        
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            String monthKey = current.getYear() + "-" + String.format("%02d", current.getMonthValue());
            monthlyAttendance.put(monthKey, 0L);
            current = current.plusMonths(1);
        }
        
        // Fill actual data
        for (Object[] data : monthlyData) {
            Integer monthNum = ((Number) data[0]).intValue();
            Long count = ((Number) data[1]).longValue();
            // Format month as YYYY-MM
            String monthKey = startDate.getYear() + "-" + String.format("%02d", monthNum);
            if (monthlyAttendance.containsKey(monthKey)) {
                monthlyAttendance.put(monthKey, count);
            }
        }
        
        // Get weekly trend data
        Map<String, Map<String, Long>> weeklyTrend = getWeeklyTrend(userId, startDate, endDate);
        
        // Get daily attendance data for detailed chart
        List<Object[]> dailyData = attendanceRepository.getDailyAttendanceByUserId(userId, startDate, endDate);
        List<String> dates = new ArrayList<>();
        List<Double> percentages = new ArrayList<>();
        
        for (Object[] data : dailyData) {
            LocalDate date = (LocalDate) data[0];
            boolean isPresent = (boolean) data[1];
            dates.add(date.toString());
            percentages.add(isPresent ? 100.0 : 0.0);
        }
        
        // Prepare analytics data
        analytics.put("userId", userId);
        analytics.put("userName", user.getUsername() != null ? user.getUsername() : user.getUsername());
        analytics.put("userEmail", user.getEmail());
        analytics.put("userRole", user.getRole());
        analytics.put("totalDays", totalDays);
        analytics.put("presentCount", presentCount);
        analytics.put("absentCount", absentCount);
        analytics.put("attendancePercentage", Math.round(attendancePercentage * 100.0) / 100.0);
        analytics.put("monthlyAttendance", monthlyAttendance);
        analytics.put("weeklyTrend", weeklyTrend);
        analytics.put("dates", dates);
        analytics.put("percentages", percentages);
        analytics.put("startDate", startDate);
        analytics.put("endDate", endDate);
        
        return analytics;
    }
    
    private Map<String, Map<String, Long>> getWeeklyTrend(Long userId, LocalDate startDate, LocalDate endDate) {
        Map<String, Map<String, Long>> weeklyTrend = new LinkedHashMap<>();
        LocalDate current = startDate;
        
        while (!current.isAfter(endDate)) {
            // Get week start (Monday)
            LocalDate weekStart = current;
            while (weekStart.getDayOfWeek().getValue() != 1) {
                weekStart = weekStart.minusDays(1);
            }
            if (weekStart.isBefore(startDate)) weekStart = startDate;
            
            LocalDate weekEnd = weekStart.plusDays(6);
            if (weekEnd.isAfter(endDate)) weekEnd = endDate;
            
            String weekKey = "Week of " + weekStart.toString();
            Map<String, Long> weekStats = new HashMap<>();
            
            long presentCount = attendanceRepository.countByUserIdAndIsPresentBetweenDates(
                userId, true, weekStart, weekEnd);
            long absentCount = attendanceRepository.countByUserIdAndIsPresentBetweenDates(
                userId, false, weekStart, weekEnd);
            
            weekStats.put("present", presentCount);
            weekStats.put("absent", absentCount);
            
            weeklyTrend.put(weekKey, weekStats);
            current = weekEnd.plusDays(1);
        }
        
        return weeklyTrend;
    }
    
    @Override
    public Attendance addAttendance(Attendance attendance) {
        return attendanceRepository.save(attendance);
    }
    
    @Override
    @Transactional
    public Attendance markAttendance(Long userId, LocalDate date, boolean isPresent, 
                                     LocalDateTime checkInTime, LocalDateTime checkOutTime, String remarks) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        
        Optional<Attendance> existingAttendance = attendanceRepository.findByUserAndDate(user, date);
        
        if (existingAttendance.isPresent()) {
            Attendance attendance = existingAttendance.get();
            attendance.setPresent(isPresent);
            attendance.setCheckInTime(checkInTime);
            attendance.setCheckOutTime(checkOutTime);
            attendance.setRemarks(remarks);
            return attendanceRepository.save(attendance);
        } else {
            Attendance attendance = new Attendance();
            attendance.setUser(user);
            attendance.setDate(date);
            attendance.setPresent(isPresent);
            attendance.setCheckInTime(checkInTime);
            attendance.setCheckOutTime(checkOutTime);
            attendance.setRemarks(remarks);
            return attendanceRepository.save(attendance);
        }
    }
    
    @Override
    public Attendance updateAttendance(Long id, Attendance attendance) {
        Attendance existingAttendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Attendance not found with id: " + id));
        
        existingAttendance.setDate(attendance.getDate());
        existingAttendance.setPresent(attendance.isPresent());
        existingAttendance.setCheckInTime(attendance.getCheckInTime());
        existingAttendance.setCheckOutTime(attendance.getCheckOutTime());
        existingAttendance.setRemarks(attendance.getRemarks());
        
        return attendanceRepository.save(existingAttendance);
    }
    
    @Override
    public void deleteAttendance(Long id) {
        attendanceRepository.deleteById(id);
    }
    
    @Override
    public double calculateAttendancePercentage(Long userId, LocalDate startDate, LocalDate endDate) {
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        long presentCount = attendanceRepository.countByUserIdAndIsPresentBetweenDates(userId, true, startDate, endDate);
        return totalDays > 0 ? (presentCount * 100.0 / totalDays) : 0;
    }

    @Override
public Map<String, Object> getAttendanceAnalytics(Integer userId, LocalDate startDate, LocalDate endDate) {
    Map<String, Object> analytics = new HashMap<>();
    
    
    Long userIdLong = userId.longValue();
    
    
    User user = userRepository.findById(userIdLong)
        .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
    
    
    long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
    
    
    long presentCount = attendanceRepository.countByUserIdAndIsPresentBetweenDates(userIdLong, true, startDate, endDate);
    long absentCount = attendanceRepository.countByUserIdAndIsPresentBetweenDates(userIdLong, false, startDate, endDate);
    
    
    double attendancePercentage = totalDays > 0 ? (presentCount * 100.0 / totalDays) : 0;
    
    
    List<Object[]> monthlyData = attendanceRepository.getMonthlyAttendanceByUserId(userIdLong, startDate, endDate);
    Map<String, Long> monthlyAttendance = new LinkedHashMap<>();
    
    
    LocalDate current = startDate;
    while (!current.isAfter(endDate)) {
        String monthKey = current.getYear() + "-" + String.format("%02d", current.getMonthValue());
        monthlyAttendance.put(monthKey, 0L);
        current = current.plusMonths(1);
    }
    
    
    for (Object[] data : monthlyData) {
        Integer monthNum = ((Number) data[0]).intValue();
        Long count = ((Number) data[1]).longValue();
        
        String monthKey = startDate.getYear() + "-" + String.format("%02d", monthNum);
        if (monthlyAttendance.containsKey(monthKey)) {
            monthlyAttendance.put(monthKey, count);
        }
    }
    
    
    Map<String, Map<String, Long>> weeklyTrend = getWeeklyTrend(userIdLong, startDate, endDate);
    
    
    List<Object[]> dailyData = attendanceRepository.getDailyAttendanceByUserId(userIdLong, startDate, endDate);
    List<String> dates = new ArrayList<>();
    List<Double> percentages = new ArrayList<>();
    
    for (Object[] data : dailyData) {
        LocalDate date = (LocalDate) data[0];
        boolean isPresent = (boolean) data[1];
        dates.add(date.toString());
        percentages.add(isPresent ? 100.0 : 0.0);
    }
    
    
    analytics.put("userId", userId);
    analytics.put("userName", user.getUsername() != null ? user.getUsername() : user.getUsername());
    analytics.put("userEmail", user.getEmail());
    analytics.put("userRole", user.getRole());
    analytics.put("totalDays", totalDays);
    analytics.put("presentCount", presentCount);
    analytics.put("absentCount", absentCount);
    analytics.put("attendancePercentage", Math.round(attendancePercentage * 100.0) / 100.0);
    analytics.put("monthlyAttendance", monthlyAttendance);
    analytics.put("weeklyTrend", weeklyTrend);
    analytics.put("dates", dates);
    analytics.put("percentages", percentages);
    analytics.put("startDate", startDate);
    analytics.put("endDate", endDate);
    
    return analytics;
}

@Override
public List<Attendance> getAttendanceByUserIdAndDateRange(Integer userId, LocalDate startDate, LocalDate endDate) {
    
    Long userIdLong = userId.longValue();
    
    
    return attendanceRepository.findByUserIdAndDateBetween(userIdLong, startDate, endDate);
}

@Override
public Attendance markAttendance(Integer userId, LocalDate date, boolean isPresent,
        LocalDateTime checkInTime, LocalDateTime checkOutTime, String remarks) {
    
    return markAttendance(userId.longValue(), date, isPresent, checkInTime, checkOutTime, remarks);
}

}