package com.cp.friend.event;

import com.cp.friend.model.FriendRequest;

// Observer Pattern (Spring ApplicationEvent) — event "คำขอเป็นเพื่อนถูกยอมรับ"
public record FriendRequestAcceptedEvent(FriendRequest request) {
}
