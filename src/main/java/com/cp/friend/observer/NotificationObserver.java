package com.cp.friend.observer;

import com.cp.friend.event.FriendRequestAcceptedEvent;
import com.cp.friend.event.FriendRequestDeclinedEvent;
import com.cp.friend.event.FriendRequestSentEvent;

/**
 * GoF Observer Pattern — Observer interface for domain event notifications.
 * Concrete Observers implement this interface to react to state changes published by domain services.
 */
public interface NotificationObserver {

    void onFriendRequestSent(FriendRequestSentEvent event);

    void onFriendRequestAccepted(FriendRequestAcceptedEvent event);

    void onFriendRequestDeclined(FriendRequestDeclinedEvent event);
}
