package com.cp.friend.config;
import java.util.UUID;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import com.cp.friend.service.RoomMembershipService;
import lombok.RequiredArgsConstructor;
@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {
 private final RoomMembershipService membership;
 @Override public Message<?> preSend(Message<?> message, MessageChannel channel) {
  StompHeaderAccessor a=MessageHeaderAccessor.getAccessor(message,StompHeaderAccessor.class);
  if(a==null || a.getCommand()==null || a.getCommand()==StompCommand.DISCONNECT) return message;
  if(a.getUser()==null) throw new AccessDeniedException("Authenticated HTTP session required");
  UUID me;
  try {me=UUID.fromString(a.getUser().getName());}catch(IllegalArgumentException ex){throw new AccessDeniedException("Invalid session principal");}
  if(a.getCommand()==StompCommand.SUBSCRIBE) {
   String destination=a.getDestination();
   if("/topic/rooms/updates".equals(destination)) return message;
   if(destination!=null && (destination.equals("/topic/call/"+me) || destination.equals("/topic/notifications/"+me))) return message;
   if(destination!=null && destination.matches("/topic/rooms/[0-9a-fA-F-]{36}(/(call|members|presence|message-updates))?")) {
    UUID room;
    try{room=UUID.fromString(destination.split("/")[3]);}catch(IllegalArgumentException ex){throw new AccessDeniedException("Invalid room destination");}
    membership.requireActive(room,me);return message;
   }
   throw new AccessDeniedException("Subscription not permitted");
  }
  if(a.getCommand()==StompCommand.SEND) {
   String destination=a.getDestination();
   if(destination==null || !destination.startsWith("/app/"))throw new AccessDeniedException("Only application destinations accept client messages");
  }
  return message;
 }
}
