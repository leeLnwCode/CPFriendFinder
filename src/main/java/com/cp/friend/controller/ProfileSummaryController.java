package com.cp.friend.controller;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.cp.friend.model.*;
import com.cp.friend.repository.*;
import com.cp.friend.service.UserInterestService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class ProfileSummaryController extends SessionController {
 private final UserRepository users;
 private final UserInterestService interests;
 private final FriendshipRepository friends;
 private final FriendRequestRepository requests;
 public record Summary(UUID userId,String firstname,String lastname,String imageUrl,Short year,String department,String bio,List<String> interests,String friendStatus,UUID requestId) {}
 @GetMapping("/{userId}/profile")
 @Transactional(readOnly=true)
 public Summary profile(@PathVariable UUID userId,HttpSession session) {
  UUID me=currentUserId(session);
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
  return new Summary(user.getId(),user.getFirstname(),user.getLastname(),user.getImageUrl(),user.getYear(),user.getDepartment(),user.getBio(),interests.getInterests(userId).stream().map(Interest::getName).toList(),status,requestId);
 }
}
