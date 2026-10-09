package com.cp.friend.service;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.CloseStatus;
import org.springframework.messaging.support.GenericMessage;
class RoomRealtimeServiceTest {
 private final SimpMessagingTemplate messaging=mock(SimpMessagingTemplate.class);
 private final RoomRealtimeService service=new RoomRealtimeService(messaging);
 private final UUID room=UUID.randomUUID(),user=UUID.randomUUID();
 private List<?> participants(){return (List<?>)service.snapshot(room).get("participants");}
 @Test void lateObserverReceivesExistingParticipantsAndMedia(){service.signal(room,user,"a","JOIN",null);service.signal(room,user,"a","MEDIA","{\"screenSharing\":true}");assertEquals(1,participants().size());assertEquals("{\"screenSharing\":true}",((Map<?,?>)participants().get(0)).get("payload"));}
 @Test void mediaAloneDoesNotCreateParticipant(){service.signal(room,user,"a","MEDIA","{}");assertTrue(participants().isEmpty());}
 @Test void explicitLeaveRemovesParticipantAndAdvancesRevision(){service.signal(room,user,"a","JOIN",null);long before=(Long)service.snapshot(room).get("revision");service.signal(room,user,"a","LEAVE",null);assertTrue(participants().isEmpty());assertTrue((Long)service.snapshot(room).get("revision")>before);}
 @Test void disconnectKeepsAnotherTabUntilLastSessionCloses(){service.signal(room,user,"a","JOIN",null);service.signal(room,user,"b","JOIN",null);service.disconnected(new SessionDisconnectEvent(this,new GenericMessage<>(new byte[0]),"a",CloseStatus.NORMAL));assertEquals(1,participants().size());service.disconnected(new SessionDisconnectEvent(this,new GenericMessage<>(new byte[0]),"b",CloseStatus.NORMAL));assertTrue(participants().isEmpty());}
 @Test void leavingMembershipRemovesAllCallSessions(){service.signal(room,user,"a","JOIN",null);service.signal(room,user,"b","JOIN",null);service.removeMember(room,user);assertTrue(participants().isEmpty());}
 @Test void membershipBroadcastsToRoomAndHome(){service.membersChanged(room);verify(messaging).convertAndSend("/topic/rooms/"+room+"/members", (Object)Map.of("type","MEMBERS_UPDATED"));verify(messaging).convertAndSend("/topic/rooms/updates", (Object)Map.of("type","ROOMS_UPDATED"));}
}
