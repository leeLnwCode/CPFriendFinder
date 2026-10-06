package com.cp.friend.service;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.cp.friend.dto.request.CreateChatRoomRequest;
import com.cp.friend.dto.response.ChatRoomDetailResponse;
import com.cp.friend.dto.response.ChatRoomSummaryResponse;
import com.cp.friend.model.ChatRoom;
import com.cp.friend.model.Friendship;
import com.cp.friend.model.Interest;
import com.cp.friend.model.RoomInterest;
import com.cp.friend.model.RoomMember;
import com.cp.friend.model.User;
import com.cp.friend.repository.ChatRoomRepository;
import com.cp.friend.repository.FriendshipRepository;
import com.cp.friend.repository.InterestRepository;
import com.cp.friend.repository.RoomInterestRepository;
import com.cp.friend.repository.RoomMemberRepository;
import com.cp.friend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomInterestRepository roomInterestRepository;
    private final InterestRepository interestRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // =========================================================
    // สร้างห้อง GROUP
    // =========================================================

    @Transactional
    public ChatRoomSummaryResponse createGroupRoom(UUID userId, CreateChatRoomRequest request) {
        if (request.isPrivate() && (request.getPassword() == null || request.getPassword().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required for private rooms");
        }

        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        ChatRoom room = new ChatRoom();
        room.setRoomName(request.getRoomName().trim());
        room.setDescription(request.getDescription() == null ? null : request.getDescription().trim());
        room.setRoomType(ChatRoom.RoomType.GROUP);
        room.setCreatedBy(creator);
        room.setMaxMembers(request.getMaxMembers() == null ? (short) 10 : request.getMaxMembers());
        room.setPrivate(request.isPrivate());
        if (request.isPrivate()) {
            room.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        room = chatRoomRepository.save(room);

        addOwner(room, creator);
        saveRoomInterests(room.getId(), request.getInterestIds());

        return toSummary(room, 1);
    }

    // =========================================================
    // ห้องที่ตัวเองเป็นสมาชิกอยู่
    // =========================================================

    @Transactional(readOnly = true)
    public List<ChatRoomSummaryResponse> myRooms(UUID userId) {
        List<ChatRoom> rooms = chatRoomRepository.findJoinedRooms(userId);
        return toSummaries(rooms);
    }

    // =========================================================
    // รายการห้อง GROUP สาธารณะ (หน้า Home) + filter
    // =========================================================

    @Transactional(readOnly = true)
    public List<ChatRoomSummaryResponse> discoverRooms(String search, Set<UUID> interestIds, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));

        List<ChatRoom> rooms;
        boolean hasSearch = search != null && !search.isBlank();
        boolean hasInterests = interestIds != null && !interestIds.isEmpty();

        if (hasSearch && hasInterests) {
            // หาจากชื่อก่อนแล้วค่อยเอามาตัดกับ interest
            Set<UUID> matchedIds = chatRoomRepository.searchGroupRoomsByName(search.trim())
                    .stream()
                    .map(ChatRoom::getId)
                    .collect(Collectors.toSet());
            rooms = chatRoomRepository.findGroupRoomsByInterestIds(interestIds).stream()
                    .filter(r -> matchedIds.contains(r.getId()))
                    .toList();
        } else if (hasSearch) {
            rooms = chatRoomRepository.searchGroupRoomsByName(search.trim());
        } else if (hasInterests) {
            rooms = chatRoomRepository.findGroupRoomsByInterestIds(interestIds);
        } else {
            rooms = chatRoomRepository.findByRoomType(ChatRoom.RoomType.GROUP, pageable).getContent();
        }

        return toSummaries(rooms.stream()
                .skip((long) Math.max(page, 0) * Math.min(Math.max(size, 1), 50))
                .limit(Math.min(Math.max(size, 1), 50))
                .toList());
    }

    // =========================================================
    // รายละเอียดห้อง
    // =========================================================

    @Transactional(readOnly = true)
    public ChatRoomDetailResponse getRoom(UUID roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        List<RoomMember> members = roomMemberRepository.findActiveMembers(roomId);
        return new ChatRoomDetailResponse(
                room.getId(),
                room.getRoomName(),
                room.getDescription(),
                room.getRoomType(),
                room.isPrivate(),
                room.getMaxMembers(),
                members.size(),
                toInterestDtos(roomId),
                members.stream()
                        .map(m -> new ChatRoomDetailResponse.MemberDto(
                                m.getUser().getId(),
                                m.getUser().getFirstname(),
                                m.getUser().getLastname(),
                                m.getUser().getImageUrl(),
                                m.getRole()))
                        .toList(),
                room.getCreatedAt());
    }

    // =========================================================
    // เข้า / ออกห้อง
    // =========================================================

    @Transactional
    public ChatRoomDetailResponse joinRoom(UUID userId, UUID roomId, String password) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (roomMemberRepository.isActiveMember(roomId, userId)) {
            return getRoom(roomId);
        }

        if (room.isPrivate() && room.getPasswordHash() != null) {
            if (password == null || !passwordEncoder.matches(password, room.getPasswordHash())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid room password");
            }
        }

        if (roomMemberRepository.countActiveMembers(roomId) >= room.getMaxMembers()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room is full");
        }

        // เคยเข้าห้องนี้แล้วออกไป → เปิดแถวเดิมกลับมา ไม่งั้นสร้างใหม่
        roomMemberRepository.findFirstByRoomIdAndUserIdOrderByJoinedAtDesc(roomId, userId)
                .ifPresentOrElse(
                        member -> member.setLeftAt(null),
                        () -> roomMemberRepository.save(newMember(room, user, RoomMember.Role.MEMBER)));

        return getRoom(roomId);
    }

    @Transactional
    public void leaveRoom(UUID userId, UUID roomId) {
        RoomMember member = roomMemberRepository.findActiveMember(roomId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "You are not a member of this room"));
        member.setLeftAt(Instant.now());

        roomMemberRepository.save(member);
    }

    // =========================================================
    // ห้อง DIRECT (แชทส่วนตัวกับเพื่อน) — หาถ้ามีอยู่แล้ว ไม่มีก็สร้างใหม่
    // =========================================================

    @Transactional
    public ChatRoomSummaryResponse getOrCreateDirectRoom(UUID userId, UUID friendId) {
        if (userId.equals(friendId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot create a direct chat with yourself");
        }

        ensureFriendship(userId, friendId);

        ChatRoom existing = chatRoomRepository.findDirectRoomBetween(userId, friendId)
                .orElse(null);
        if (existing != null) {
            return toSummary(existing, roomMemberRepository.countActiveMembers(existing.getId()));
        }

        User me = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        User friend = userRepository.findById(friendId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        ChatRoom room = new ChatRoom();
        room.setRoomName("DIRECT");
        room.setRoomType(ChatRoom.RoomType.DIRECT);
        room.setCreatedBy(me);
        room.setMaxMembers((short) 2);
        room.setPrivate(true);
        room = chatRoomRepository.save(room);

        roomMemberRepository.save(newMember(room, me, RoomMember.Role.MEMBER));
        roomMemberRepository.save(newMember(room, friend, RoomMember.Role.MEMBER));

        return toSummary(room, 2);
    }

    private void ensureFriendship(UUID userId, UUID friendId) {
        Friendship friendship = friendshipRepository.findBetween(userId, friendId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "You can only chat directly with friends"));
    }

    // =========================================================
    // helpers
    // =========================================================

    private void addOwner(ChatRoom room, User owner) {
        roomMemberRepository.save(newMember(room, owner, RoomMember.Role.OWNER));
    }

    private RoomMember newMember(ChatRoom room, User user, RoomMember.Role role) {
        RoomMember member = new RoomMember();
        member.setRoom(room);
        member.setUser(user);
        member.setRole(role);
        return member;
    }

    private void saveRoomInterests(UUID roomId, List<UUID> interestIds) {
        if (interestIds == null || interestIds.isEmpty()) {
            return;
        }
        Set<UUID> uniqueIds = new LinkedHashSet<>(interestIds);
        List<Interest> interests = interestRepository.findByIdInAndIsActiveTrue(uniqueIds);
        if (interests.size() != uniqueIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "One or more interests do not exist or are inactive");
        }
        for (Interest interest : interests) {
            RoomInterest roomInterest = new RoomInterest();
            roomInterest.setRoomId(roomId);
            roomInterest.setInterestId(interest.getId());
            roomInterestRepository.save(roomInterest);
        }
    }

    private List<ChatRoomSummaryResponse> toSummaries(List<ChatRoom> rooms) {
        List<UUID> roomIds = rooms.stream().map(ChatRoom::getId).toList();
        Map<UUID, List<RoomInterest>> interestsByRoom = roomInterestRepository.findByRoomIdInWithInterest(roomIds)
                .stream()
                .collect(Collectors.groupingBy(RoomInterest::getRoomId));

        return rooms.stream()
                .map(room -> toSummary(
                        room,
                        roomMemberRepository.countActiveMembers(room.getId()),
                        interestsByRoom.getOrDefault(room.getId(), List.of())))
                .toList();
    }

    private ChatRoomSummaryResponse toSummary(ChatRoom room, long memberCount) {
        return toSummary(room, memberCount, roomInterestRepository.findByRoomIdWithInterest(room.getId()));
    }

    private ChatRoomSummaryResponse toSummary(ChatRoom room, long memberCount, List<RoomInterest> roomInterests) {
        return new ChatRoomSummaryResponse(
                room.getId(),
                room.getRoomName(),
                room.getRoomType(),
                room.isPrivate(),
                room.getMaxMembers(),
                memberCount,
                roomInterests.stream()
                        .map(ri -> new ChatRoomSummaryResponse.InterestDto(
                                ri.getInterest().getId(),
                                ri.getInterest().getName()))
                        .toList(),
                room.getCreatedAt());
    }

    private List<ChatRoomSummaryResponse.InterestDto> toInterestDtos(UUID roomId) {
        return roomInterestRepository.findByRoomIdWithInterest(roomId).stream()
                .map(ri -> new ChatRoomSummaryResponse.InterestDto(
                        ri.getInterest().getId(),
                        ri.getInterest().getName()))
                .toList();
    }
}
