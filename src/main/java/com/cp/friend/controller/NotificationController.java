package com.cp.friend.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cp.friend.dto.response.NotificationResponse;
import com.cp.friend.service.NotificationService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController extends SessionController {

    private final NotificationService notificationService;

    // รายการแจ้งเตือน — ?unread=true เฉพาะที่ยังไม่อ่าน, &page= &size=
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> list(
            @RequestParam(required = false) boolean unread,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpSession session
    ) {
        return ResponseEntity.ok(
                notificationService.list(currentUserId(session), unread, page, size)
        );
    }

    // จำนวนที่ยังไม่อ่าน (badge ที่กระดิ่ง)
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(HttpSession session) {
        return ResponseEntity.ok(Map.of("count", notificationService.unreadCount(currentUserId(session))));
    }

    // ทำเครื่องหมายว่าอ่านแล้ว 1 รายการ
    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID notificationId,
            HttpSession session
    ) {
        notificationService.markAsRead(currentUserId(session), notificationId);
        return ResponseEntity.noContent().build();
    }

    // อ่านทั้งหมด
    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(HttpSession session) {
        notificationService.markAllAsRead(currentUserId(session));
        return ResponseEntity.noContent().build();
    }

    
}
