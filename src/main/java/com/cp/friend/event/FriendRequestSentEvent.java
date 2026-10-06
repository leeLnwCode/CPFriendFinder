package com.cp.friend.event;

import com.cp.friend.model.FriendRequest;

// Observer Pattern (Spring ApplicationEvent) — event "มีคนส่งคำขอเป็นเพื่อน"
// FriendRequestService publish แล้วไม่สนใจว่าใครฟัง — NotificationService เป็น Listener
public record FriendRequestSentEvent(FriendRequest request) {
}
