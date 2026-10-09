package com.cp.friend.service;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import com.cp.friend.dto.response.CallSignalResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomRealtimeService {
    private final SimpMessagingTemplate messaging;
    private final Map<UUID, Map<UUID, Map<String,String>>> calls = new HashMap<>();
    private final AtomicLong revision = new AtomicLong(System.currentTimeMillis());

    public void membersChanged(UUID roomId) {
        messaging.convertAndSend("/topic/rooms/"+roomId+"/members", (Object)Map.of("type","MEMBERS_UPDATED"));
        roomsChanged();
    }
    public void roomsChanged() { messaging.convertAndSend("/topic/rooms/updates", (Object)Map.of("type","ROOMS_UPDATED")); }

    public synchronized Map<String,Object> snapshot(UUID roomId) {
        List<Map<String,Object>> participants = new ArrayList<>();
        calls.getOrDefault(roomId,Map.of()).forEach((user,sessions)-> {
            if(!sessions.isEmpty())participants.add(Map.of("userId",user,"payload",new ArrayList<>(sessions.values()).get(sessions.size()-1)));
        });
        return Map.of("revision",revision.get(),"participants",participants);
    }
    private void changed(UUID roomId) { revision.incrementAndGet();messaging.convertAndSend("/topic/rooms/"+roomId+"/presence", (Object)snapshot(roomId)); }

    public synchronized void signal(UUID roomId, UUID userId, String sessionId, String type, String payload) {
        if(sessionId==null)return;
        if("JOIN".equalsIgnoreCase(type)) {
            calls.computeIfAbsent(roomId,id->new HashMap<>()).computeIfAbsent(userId,id->new LinkedHashMap<>()).put(sessionId,"{}");changed(roomId);
        } else if("MEDIA".equalsIgnoreCase(type)) {
            var users=calls.get(roomId);var sessions=users==null?null:users.get(userId);
            if(sessions!=null&&sessions.containsKey(sessionId)){sessions.put(sessionId,payload==null?"{}":payload);changed(roomId);}
        } else if("LEAVE".equalsIgnoreCase(type)) { remove(roomId,userId,sessionId,false); }
    }
    private void remove(UUID roomId,UUID userId,String sessionId,boolean relayLeave) {
        var users=calls.get(roomId);var sessions=users==null?null:users.get(userId);
        if(sessions==null||sessions.remove(sessionId)==null)return;
        if(sessions.isEmpty()) { users.remove(userId);if(relayLeave)messaging.convertAndSend("/topic/rooms/"+roomId+"/call",new CallSignalResponse("LEAVE",userId,null,null)); }
        if(users.isEmpty())calls.remove(roomId);changed(roomId);
    }
    public synchronized void removeMember(UUID roomId,UUID userId) {
        var users=calls.get(roomId);if(users==null||users.remove(userId)==null)return;
        if(users.isEmpty())calls.remove(roomId);
        messaging.convertAndSend("/topic/rooms/"+roomId+"/call",new CallSignalResponse("LEAVE",userId,null,null));changed(roomId);
    }
    @EventListener
    public synchronized void disconnected(SessionDisconnectEvent event) {
        for(UUID roomId:new ArrayList<>(calls.keySet()))for(UUID userId:new ArrayList<>(calls.get(roomId).keySet()))remove(roomId,userId,event.getSessionId(),true);
    }
}
