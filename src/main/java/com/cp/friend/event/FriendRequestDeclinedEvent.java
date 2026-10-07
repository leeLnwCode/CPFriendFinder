package com.cp.friend.event;

import com.cp.friend.model.FriendRequest;

/**
 * Observer Pattern (Spring ApplicationEvent) — event triggered when a friend request is declined.
 * Published by FriendRequestService, consumed by NotificationObserver/NotificationService.
 */
public record FriendRequestDeclinedEvent(FriendRequest request) {
}
