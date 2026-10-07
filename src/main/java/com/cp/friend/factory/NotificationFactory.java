package com.cp.friend.factory;

import com.cp.friend.model.ChatRoom;
import com.cp.friend.model.FriendRequest;
import com.cp.friend.model.Message;
import com.cp.friend.model.Notification;
import com.cp.friend.model.User;
import org.springframework.stereotype.Component;

// Factory Method Pattern — รวม "กฎการสร้าง Notification" ไว้ที่เดียว
// แต่ละเมธอดคือ Factory Method ของ notification แต่ละชนิด (title/message/relations ต่างกัน)
// เพิ่มชนิดแจ้งเตือนใหม่ = เพิ่ม factory method ใหม่ ไม่กระทบผู้ใช้ (Open/Closed)
@Component
public class NotificationFactory {

    // มีคนส่งคำขอเป็นเพื่อน → แจ้งผู้รับ
    public Notification friendRequestReceived(FriendRequest request) {
        return build(
                request.getReceiver(),
                request.getSender(),
                Notification.Type.FRIEND_REQUEST,
                "New friend request",
                "%s sent you a friend request".formatted(fullName(request.getSender())),
                request,
                null
        );
    }

    // คำขอถูกยอมรับ → แจ้งผู้ส่ง
    public Notification friendRequestAccepted(FriendRequest request) {
        return build(
                request.getSender(),
                request.getReceiver(),
                Notification.Type.FRIEND_REQUEST,
                "Friend request accepted",
                "%s accepted your friend request".formatted(fullName(request.getReceiver())),
                request,
                null
        );
    }

    // มีข้อความใหม่ในห้อง → แจ้งสมาชิกคนนั้น
    public Notification newMessage(Message message, User recipient) {
        return build(
                recipient,
                message.getSender(),
                Notification.Type.NEW_MESSAGE,
                "New message",
                "%s sent a message in %s".formatted(fullName(message.getSender()), message.getRoom().getRoomName()),
                null,
                message.getRoom()
        );
    }

    private Notification build(User user, User actor, Notification.Type type,
                               String title, String message,
                               FriendRequest friendRequest, ChatRoom room) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setActor(actor);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setFriendRequest(friendRequest);
        notification.setRoom(room);
        return notification;
    }

    private String fullName(User user) {
        return "%s %s".formatted(user.getFirstname(), user.getLastname());
    }
}
