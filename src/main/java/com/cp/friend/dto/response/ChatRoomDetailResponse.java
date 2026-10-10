package com.cp.friend.dto.response;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.cp.friend.model.ChatRoom;
import com.cp.friend.model.RoomMember;

// Builder Pattern — response ที่มีหลาย field + สอง list ประกอบยาก ใช้ builder อ่านง่ายกว่า
// constructor 10 ตำแหน่ง (โดยเฉพาะ field ชนิด list ที่ต้องกัน null)
public record ChatRoomDetailResponse(
        UUID id,
        String roomName,
        String description,
        ChatRoom.RoomType roomType,
        boolean isPrivate,
        short maxMembers,
        long memberCount,
        List<ChatRoomSummaryResponse.InterestDto> interests,
        List<MemberDto> members,
        Instant createdAt
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private String roomName;
        private String description;
        private ChatRoom.RoomType roomType;
        private boolean isPrivate;
        private short maxMembers;
        private long memberCount;
        private List<ChatRoomSummaryResponse.InterestDto> interests = new ArrayList<>();
        private List<MemberDto> members = new ArrayList<>();
        private Instant createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder roomName(String roomName) { this.roomName = roomName; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder roomType(ChatRoom.RoomType roomType) { this.roomType = roomType; return this; }
        public Builder isPrivate(boolean isPrivate) { this.isPrivate = isPrivate; return this; }
        public Builder maxMembers(short maxMembers) { this.maxMembers = maxMembers; return this; }
        public Builder memberCount(long memberCount) { this.memberCount = memberCount; return this; }
        public Builder interests(List<ChatRoomSummaryResponse.InterestDto> interests) {
            this.interests = interests == null ? new ArrayList<>() : interests;
            return this;
        }
        public Builder members(List<MemberDto> members) {
            this.members = members == null ? new ArrayList<>() : members;
            return this;
        }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public ChatRoomDetailResponse build() {
            return new ChatRoomDetailResponse(
                    id, roomName, description, roomType, isPrivate,
                    maxMembers, memberCount,
                    List.copyOf(interests), List.copyOf(members), createdAt
            );
        }
    }

    public record MemberDto(
            UUID userId,
            String firstname,
            String lastname,
            String imageUrl,
            RoomMember.Role role
    ) {
    }
}
