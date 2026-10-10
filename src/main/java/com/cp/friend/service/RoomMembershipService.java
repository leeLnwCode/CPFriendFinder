package com.cp.friend.service;
import java.util.UUID;
import com.cp.friend.repository.RoomMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class RoomMembershipService {
 private final RoomMemberRepository members;
 @Transactional(readOnly=true)
 public void requireActive(UUID roomId, UUID userId) {
  members.findActiveMember(roomId,userId).orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN,"You are not a member of this room"));
 }
}
