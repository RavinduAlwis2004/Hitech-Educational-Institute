package com.example.UserManagement.controller;

import com.example.UserManagement.entity.User;
import com.example.UserManagement.entity.Enums.Role;
import com.example.UserManagement.service.AttendanceService;
import com.example.UserManagement.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/attendance")
public class AttendanceController {

    @Autowired private AttendanceService attendanceService;
    @Autowired private UserRepository    userRepository;

    // ── SHOW MARK FORM ──────────────────────────────────────────────
    @GetMapping("/mark")
    public String showMarkForm(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String success,
            Model model) {

        if (date == null) date = LocalDate.now();
        List<User> students = userRepository.findByRole(Role.STUDENT);
        model.addAttribute("date",     date);
        model.addAttribute("students", students);
        if (success != null) model.addAttribute("success", success);
        return "mark_attendance";
    }

    // ── SAVE BULK ATTENDANCE ─────────────────────────────────────────
    @PostMapping("/markBulk")
    public String saveBulk(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("studentIds")  List<Long>   studentIds,
            @RequestParam("statuses")    List<String> statuses,
            @RequestParam(value="checkIns",  required=false) List<String> checkIns,
            @RequestParam(value="checkOuts", required=false) List<String> checkOuts,
            @RequestParam(value="remarks",   required=false) List<String> remarks,
            Model model) {

        try {
            for (int i = 0; i < studentIds.size(); i++) {
                Long    sid  = studentIds.get(i);
                boolean pres = "true".equalsIgnoreCase(statuses.get(i));

                LocalDateTime ci = null, co = null;
                if (checkIns  != null && i < checkIns.size()
                        && checkIns.get(i)  != null && !checkIns.get(i).isBlank())
                    ci = LocalDateTime.of(date, LocalTime.parse(checkIns.get(i)));
                if (checkOuts != null && i < checkOuts.size()
                        && checkOuts.get(i) != null && !checkOuts.get(i).isBlank())
                    co = LocalDateTime.of(date, LocalTime.parse(checkOuts.get(i)));

                String rem = (remarks != null && i < remarks.size()) ? remarks.get(i) : null;
                attendanceService.markAttendance(sid, date, pres, ci, co, rem);
            }
            return "redirect:/attendance/mark?date=" + date + "&success=Attendance+saved!";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("date",     date);
            model.addAttribute("students", userRepository.findByRole(Role.STUDENT));
            model.addAttribute("error",    "Error saving: " + e.getMessage());
            return "mark_attendance";
        }
    }

    // ── ANALYTICS PAGE ───────────────────────────────────────────────
    @GetMapping("/analysis")
    public String analysis(
            @RequestParam(required=false) Integer userId,
            @RequestParam(required=false)
            @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required=false)
            @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpSession session,
            Model model) {

        try {
            Integer sessionUserIdInt = null;
            Object sessionUserId = session.getAttribute("loggedInUserId");
            if (sessionUserId instanceof Integer i) {
                sessionUserIdInt = i;
            } else if (sessionUserId instanceof Long l) {
                sessionUserIdInt = l.intValue();
            }

            if (sessionUserIdInt != null) {
                User currentUser = userRepository.findById(sessionUserIdInt).orElse(null);
                if (currentUser != null) {
                    // Students must only see their own attendance, never any provided userId param.
                    if (currentUser.getRole() == Role.STUDENT) {
                        userId = currentUser.getId();
                    } else if (userId == null) {
                        // For staff/admin default to their own profile unless explicitly filtering.
                        userId = currentUser.getId();
                    }
                }
            }

            if (userId == null) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated()
                        && !auth.getPrincipal().equals("anonymousUser")) {
                    Object p = auth.getPrincipal();
                    if (p instanceof org.springframework.security.core.userdetails.UserDetails ud) {
                        String loginId = ud.getUsername();
                        User u = userRepository.findByUsername(loginId)
                                .or(() -> userRepository.findByEmail(loginId))
                                .orElse(null);
                        if (u != null) userId = u.getId();
                    }
                }
            }

            if (userId == null) {
                model.addAttribute("error","Please log in again.");
                return "redirect:/login";
            }

            if (startDate == null) startDate = LocalDate.now().minusDays(30);
            if (endDate   == null) endDate   = LocalDate.now();

            model.addAttribute("analytics",
                    attendanceService.getAttendanceAnalytics(userId, startDate, endDate));
            model.addAttribute("attendanceRecords",
                    attendanceService.getAttendanceByUserIdAndDateRange(userId, startDate, endDate));
            model.addAttribute("userId",    userId);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate",   endDate);
            return "attendance_visualization";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    // ── STUDENT VIEW ─────────────────────────────────────────────────
    @GetMapping("/student/{studentId}")
    public String studentView(
            @PathVariable Integer studentId,
            @RequestParam(required=false)
            @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required=false)
            @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {

        if (startDate == null) startDate = LocalDate.now().minusMonths(3);
        if (endDate   == null) endDate   = LocalDate.now();
        model.addAttribute("analytics",
                attendanceService.getAttendanceAnalytics(studentId, startDate, endDate));
        model.addAttribute("attendanceRecords",
                attendanceService.getAttendanceByUserIdAndDateRange(studentId, startDate, endDate));
        model.addAttribute("studentId",  studentId);
        model.addAttribute("startDate",  startDate);
        model.addAttribute("endDate",    endDate);
        return "student_attendance_view";
    }

    // ── DEBUG / SEED ─────────────────────────────────────────────────
    @GetMapping("/test")
    @ResponseBody
    public String test() { return "OK"; }

    @GetMapping("/debug")
    @ResponseBody
    public String debug() {
        StringBuilder sb = new StringBuilder("<h3>Debug</h3>");
        userRepository.findByRole(Role.STUDENT).forEach(s ->
            sb.append("<p>ID=").append(s.getId())
              .append(" | ").append(s.getUsername()).append("</p>"));
        return sb.toString();
    }

    @GetMapping("/seed")
    @ResponseBody
    public String seed(@RequestParam(defaultValue="1") Long studentId) {
        boolean[] pat = {true,true,false,true,true,true,false,true,true,true,
                         false,true,true,true,false,true,true,true,false,true};
        int ok = 0;
        for (int i = 0; i < pat.length; i++) {
            LocalDate d  = LocalDate.now().minusDays(i+1);
            boolean   p  = pat[i];
            LocalDateTime ci = p ? LocalDateTime.of(d,LocalTime.of(8,0))  : null;
            LocalDateTime co = p ? LocalDateTime.of(d,LocalTime.of(16,0)) : null;
            try { attendanceService.markAttendance(studentId,d,p,ci,co,p?null:"Absent"); ok++; }
            catch(Exception ignored){}
        }
        return "Seeded "+ok+" records. <a href='/attendance/analysis'>View Analytics</a>";
    }
}