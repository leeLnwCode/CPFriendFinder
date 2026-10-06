package com.cp.friend.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cp.friend.dto.response.InterestResponse;
import com.cp.friend.model.Interest;

// Mapper Pattern — รวมกฎการแปลง Entity → Response DTO ไว้ที่เดียว
// Presentation layer ไม่เห็น entity เลย
@Component
public class InterestMapper {

    public InterestResponse toResponse(Interest interest) {
        return new InterestResponse(
                interest.getId(),
                interest.getName(),
                interest.isActive()
        );
    }

    public List<InterestResponse> toResponseList(List<Interest> interests) {
        return interests.stream()
                .map(this::toResponse)
                .toList();
    }
}
