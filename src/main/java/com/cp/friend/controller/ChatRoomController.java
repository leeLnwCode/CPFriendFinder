package com.cp.friend.controller;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cp.friend.dto.request.CreateChatRoomRequest;
import com.cp.friend.dto.request.JoinChatRoomRequest;
import com.cp.friend.dto.request.UpdateChatRoomRequest;
import com.cp.friend.dto.request.UpdateMemberRoleRequest;
import com.cp.friend.dto.response.ChatRoomDetailResponse;
import com.cp.friend.dto.response.ChatRoomSummaryResponse;
import com.cp.friend.service.ChatRoomService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatRoomController extends SessionController {

    private final ChatRoomService chatRoomService;

    // ห้องที่ตัวเองเป็นสมาชิกอยู่
    @GetMapping
    public ResponseEntity<List<ChatRoomSummaryResponse>> myRooms(HttpSession session) {
        return ResponseEntity.ok(chatRoomService.myRooms(currentUserId(session)));
    }

    // สร้างห้อง GROUP
    @PostMapping
    public ResponseEntity<ChatRoomSummaryResponse> createRoom(
            @Valid @RequestBody CreateChatRoomRequest request,
            HttpSession session
    ) {
        ChatRoomSummaryResponse response = chatRoomService.createGroupRoom(currentUserId(session), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // รายการห้อง GROUP สำหรับหน้า Home — filter: ?search= &interestId= (ใส่ซ้ำได้) &page= &size=
    @GetMapping("/discover")
    public ResponseEntity<List<ChatRoomSummaryResponse>> discoverRooms(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Set<UUID> interestId,
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(chatRoomService.discoverRooms(search, interestId, page, size, year));
    }

    // รายละเอียดห้อง + สมาชิก
    @GetMapping("/{roomId}")
    public ResponseEntity<ChatRoomDetailResponse> getRoom(@PathVariable UUID roomId) {
        return ResponseEntity.ok(chatRoomService.getRoom(roomId));
    }

    // แก้ไขห้อง (OWNER / MODERATOR) — ส่งมาแค่ field ที่ต้องการแก้
    @PutMapping("/{roomId}")
    public ResponseEntity<ChatRoomDetailResponse> updateRoom(
            @PathVariable UUID roomId,
            @Valid @RequestBody UpdateChatRoomRequest request,
            HttpSession session
    ) {
        return ResponseEntity.ok(chatRoomService.updateRoom(currentUserId(session), roomId, request));
    }

    // เปลี่ยน role ของสมาชิก (OWNER เท่านั้น) — body: {"role": "MODERATOR"} หรือ "MEMBER"
    @PutMapping("/{roomId}/members/{memberUserId}/role")
    public ResponseEntity<ChatRoomDetailResponse> updateMemberRole(
            @PathVariable UUID roomId,
            @PathVariable UUID memberUserId,
            @Valid @RequestBody UpdateMemberRoleRequest request,
            HttpSession session
    ) {
        return ResponseEntity.ok(chatRoomService.updateMemberRole(
                currentUserId(session), roomId, memberUserId, request.getRole()));
    }

    // เข้าห้อง (ห้อง private ต้องส่ง password)
    @PostMapping("/{roomId}/join")
    public ResponseEntity<ChatRoomDetailResponse> joinRoom(
            @PathVariable UUID roomId,
            @Valid @RequestBody(required = false) JoinChatRoomRequest request,
            HttpSession session
    ) {
        String password = request == null ? null : request.getPassword();
        return ResponseEntity.ok(chatRoomService.joinRoom(currentUserId(session), roomId, password));
    }

    // ออกจากห้อง
    @PostMapping("/{roomId}/leave")
    public ResponseEntity<Void> leaveRoom(@PathVariable UUID roomId, HttpSession session) {
        chatRoomService.leaveRoom(currentUserId(session), roomId);
        return ResponseEntity.noContent().build();
    }

    // ห้องแชทส่วนตัวกับเพื่อน — ได้ห้องเดิมถ้ามีอยู่แล้ว
    @PostMapping("/direct/{friendId}")
    public ResponseEntity<ChatRoomSummaryResponse> getOrCreateDirectRoom(
            @PathVariable UUID friendId,
            HttpSession session
    ) {
        ChatRoomSummaryResponse response =
                chatRoomService.getOrCreateDirectRoom(currentUserId(session), friendId);
        return ResponseEntity.ok(response);
    }
}
