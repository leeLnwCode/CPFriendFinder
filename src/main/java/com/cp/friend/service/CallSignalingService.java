package com.cp.friend.service;

import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.cp.friend.dto.request.CallInviteRequest;
import com.cp.friend.dto.request.CallSignalRequest;
import com.cp.friend.dto.response.CallSignalResponse;
import com.cp.friend.model.RoomMember;
import com.cp.friend.model.User;
import com.cp.friend.repository.RoomMemberRepository;
import com.cp.friend.repository.UserRepository;
import lombok.RequiredArgsConstructor;

/** Validates call participants and relays invitations / WebRTC signaling. */
@Service
@RequiredArgsConstructor
public class CallSignalingService {
    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // =========================================================
    // สายเรียกเข้า (call ring) — relay สัญญาณ INVITE/ACCEPT/DECLINE/CANCEL
    // ไปยัง /topic/call/{toUserId} ซึ่งทุกหน้าของผู้รับ subscribe อยู่
    // =========================================================

    public void relayCallInvite(UUID userId, CallInviteRequest req) {
        if (req.getToUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "toUserId is required");
        }
        String type = req.getType() == null ? "" : req.getType().trim().toUpperCase(Locale.ROOT);

        String mode = req.getMode() == null || req.getMode().isBlank()
                ? "VOICE" : req.getMode().trim().toUpperCase(Locale.ROOT);
        if (!"VOICE".equals(mode) && !"VIDEO".equals(mode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid call mode, use VOICE or VIDEO");
        }

        String fromName = null;
        String fromImage = null;
        if ("INVITE".equals(type)) {
            if (req.getRoomId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "roomId is required for INVITE");
            }
            // INVITE ต้องเป็นสมาชิกห้องจริงเท่านั้น กันสแปมข้ามห้อง
            requireActiveMember(req.getRoomId(), userId);
            // ดึง User ตรงจาก repository — RoomMember.user เป็น lazy proxy
            // และ WS handler ทำงานนอก Hibernate session จึงเรียก getter ผ่าน proxy ไม่ได้
            User caller = userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            fromName = (caller.getFirstname() == null ? "" : caller.getFirstname()) + " "
                    + (caller.getLastname() == null ? "" : caller.getLastname());
            fromImage = caller.getImageUrl();
        }

        messagingTemplate.convertAndSend(
                "/topic/call/" + req.getToUserId(),
                new com.cp.friend.dto.response.CallInviteSignal(
                        type, userId, req.getToUserId(), req.getRoomId(), fromName, fromImage, mode));
    }

        public void relayCallSignal(UUID userId, UUID roomId, CallSignalRequest signal) {
            String type = signal.getType() == null
                    ? ""
                    : signal.getType().trim().toUpperCase(Locale.ROOT);

            // LEAVE ต้องส่งได้แม้สมาชิกเพิ่งออกจากห้องแล้ว
            // แต่ต้องเป็น user ที่เคยเป็นสมาชิกของห้องจริง
            if ("LEAVE".equals(type)) {
                roomMemberRepository
                        .findFirstByRoomIdAndUserIdOrderByJoinedAtDesc(roomId, userId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "You are not a member of this room"
                        ));

                messagingTemplate.convertAndSend(
                        "/topic/rooms/" + roomId + "/call",
                        new CallSignalResponse(type, userId, null, null)
                );
                return;
            }

            // Signal อื่นต้องเป็นสมาชิกปัจจุบันเท่านั้น
            requireActiveMember(roomId, userId);

            switch (type) {
                case "JOIN" -> messagingTemplate.convertAndSend(
                        "/topic/rooms/" + roomId + "/call",
                        new CallSignalResponse(type, userId, null, null));

                case "MEDIA" -> messagingTemplate.convertAndSend(
                        "/topic/rooms/" + roomId + "/call",
                        new CallSignalResponse(
                                type,
                                userId,
                                null,
                                signal.getPayload()
                        ));

                case "OFFER", "ANSWER", "ICE" -> {
                    if (signal.getTargetUserId() == null) {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                type + " requires targetUserId"
                        );
                    }

                    messagingTemplate.convertAndSend(
                            "/topic/rooms/" + roomId + "/call",
                            new CallSignalResponse(
                                    type,
                                    userId,
                                    signal.getTargetUserId(),
                                    signal.getPayload()
                            ));
                }

                default -> throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid signal type, use JOIN, LEAVE, MEDIA, OFFER, ANSWER or ICE"
                );
            }
        }

    private RoomMember requireActiveMember(UUID roomId, UUID userId) {
        return roomMemberRepository.findActiveMember(roomId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "You are not a member of this room"));
    }

}
