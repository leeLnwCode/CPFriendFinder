package com.cp.friend.controller;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import com.cp.friend.service.RoomRealtimeService;
import com.cp.friend.service.RoomMembershipService;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class RoomPresenceController extends SessionController {
    private final RoomRealtimeService realtime;
    private final RoomMembershipService members;
    @GetMapping("/{roomId}/call-presence")
    public ResponseEntity<Map<String,Object>> presence(@PathVariable UUID roomId,HttpSession session) {
        UUID userId=currentUserId(session);
        members.requireActive(roomId,userId);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(realtime.snapshot(roomId));
    }
}
