package com.cp.friend.dto.response;
import java.util.*;
 public record ProfileSummaryResponse(UUID userId,String firstname,String lastname,String imageUrl,Short year,String department,String bio,List<String> interests,String friendStatus,UUID requestId) {}
