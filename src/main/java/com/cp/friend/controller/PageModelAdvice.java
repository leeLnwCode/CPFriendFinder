package com.cp.friend.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.cp.friend.dto.response.UpdateProfileResponse;
import com.cp.friend.mapper.UserMapper;
import com.cp.friend.model.User;
import com.cp.friend.service.UserService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@ControllerAdvice(assignableTypes = PageController.class)
@RequiredArgsConstructor
public class PageModelAdvice {

    private final UserService userService;
    private final UserMapper userMapper;

    @ModelAttribute("currentUser")
    public UpdateProfileResponse currentUser(HttpSession session) {
        Object userId = session.getAttribute("userId");

        if (!(userId instanceof UUID id)) {
            return null;
        }

        User user = userService.getProfile(id);
        return userMapper.toProfileResponse(user);
    }
}