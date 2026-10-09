package com.cp.friend.controller;
import com.cp.friend.model.*;
import com.cp.friend.repository.*;
import com.cp.friend.service.UserInterestService;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ProfileSummaryControllerTest {
 UserRepository users=mock(UserRepository.class);UserInterestService interests=mock(UserInterestService.class);FriendshipRepository friends=mock(FriendshipRepository.class);FriendRequestRepository requests=mock(FriendRequestRepository.class);
 ProfileSummaryController controller=new ProfileSummaryController(new com.cp.friend.service.ProfileSummaryService(users,interests,friends,requests));
 UUID me=UUID.randomUUID(),other=UUID.randomUUID();MockHttpSession session=new MockHttpSession();
 @BeforeEach void setup(){session.setAttribute("userId",me);}
 void user(UUID id){User u=new User();org.springframework.test.util.ReflectionTestUtils.setField(u,"id",id);u.setStatus(User.Status.ACTIVE);u.setFirstname("Person");u.setBio("Biography");when(users.findById(id)).thenReturn(Optional.of(u));Interest i=new Interest();i.setName("Coding");when(interests.getInterests(id)).thenReturn(List.of(i));}
 @Test void requiresSession(){assertThrows(ResponseStatusException.class,()->controller.profile(other,new MockHttpSession()));verifyNoInteractions(users);}
 @Test void loadsInterestsAndSelf(){user(me);var r=controller.profile(me,session);assertEquals("self",r.friendStatus());assertEquals(List.of("Coding"),r.interests());assertEquals("Biography",r.bio());verifyNoInteractions(requests,friends);}
 @Test void existingFriendTakesPriority(){user(other);when(friends.existsBetween(me,other)).thenReturn(true);assertEquals("friend",controller.profile(other,session).friendStatus());verifyNoInteractions(requests);}
 @Test void incomingReturnsActualRequestId(){user(other);FriendRequest r=new FriendRequest();r.setId(UUID.randomUUID());when(requests.findBySenderIdAndReceiverIdAndStatus(other,me,FriendRequest.Status.PENDING)).thenReturn(Optional.of(r));var p=controller.profile(other,session);assertEquals("incoming",p.friendStatus());assertEquals(r.getId(),p.requestId());}
 @Test void outgoingReturnsPending(){user(other);FriendRequest r=new FriendRequest();r.setId(UUID.randomUUID());when(requests.findBySenderIdAndReceiverIdAndStatus(me,other,FriendRequest.Status.PENDING)).thenReturn(Optional.of(r));assertEquals("pending",controller.profile(other,session).friendStatus());}
 @Test void inactiveProfileHidden(){User u=new User();u.setStatus(null);when(users.findById(other)).thenReturn(Optional.of(u));assertThrows(ResponseStatusException.class,()->controller.profile(other,session));}
}


