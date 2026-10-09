package com.cp.friend.service;
import java.util.*;
import com.cp.friend.model.Interest;
public interface UserInterests {
 List<Interest> getInterests(UUID userId);
 List<Interest> replaceInterests(UUID userId,List<UUID> ids);
 List<Interest> replaceInterestsByNames(UUID userId,List<String> names);
}
