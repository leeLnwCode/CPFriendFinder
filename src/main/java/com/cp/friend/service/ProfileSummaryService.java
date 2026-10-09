package com.cp.friend.service;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.cp.friend.model.*;
import com.cp.friend.repository.*;
import com.cp.friend.dto.response.ProfileSummaryResponse;
import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class ProfileSummaryService {
 private final UserRepository users;
 private final UserInterests interests;
 private final FriendshipRepository friends;
 private final FriendRequestRepository requests;
 @Transactional(readOnly=true)
 public ProfileSummaryResponse profile(UUID userId, UUID me) {
  User user=users.findById(userId).filter(u->u.getStatus()==User.Status.ACTIVE).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found"));
  String status="none"; UUID requestId=null;
  if(me.equals(userId)) status="self";
  else if(friends.existsBetween(me,userId)) status="friend";
  else {
   var incoming=requests.findBySenderIdAndReceiverIdAndStatus(userId,me,FriendRequest.Status.PENDING);
   var outgoing=requests.findBySenderIdAndReceiverIdAndStatus(me,userId,FriendRequest.Status.PENDING);
   if(incoming.isPresent()){status="incoming";requestId=incoming.get().getId();}
   else if(outgoing.isPresent()){status="pending";requestId=outgoing.get().getId();}
  }
  return new ProfileSummaryResponse(user.getId(),user.getFirstname(),user.getLastname(),user.getImageUrl(),user.getYear(),user.getDepartment(),user.getBio(),interests.getInterests(userId).stream().map(Interest::getName).toList(),status,requestId);
 }
}
