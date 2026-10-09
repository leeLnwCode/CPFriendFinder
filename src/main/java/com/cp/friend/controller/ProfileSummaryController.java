package com.cp.friend.controller;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import com.cp.friend.dto.response.ProfileSummaryResponse;
import com.cp.friend.service.ProfileSummaryService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class ProfileSummaryController extends SessionController {
 private final ProfileSummaryService profiles;
 @GetMapping("/{userId}/profile")
 public ProfileSummaryResponse profile(@PathVariable UUID userId,HttpSession session) {
  return profiles.profile(userId,currentUserId(session));
 }
}
