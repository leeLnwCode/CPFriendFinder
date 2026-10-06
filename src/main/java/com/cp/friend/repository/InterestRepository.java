package com.cp.friend.repository;

import com.cp.friend.model.Interest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Repository ของตาราง interests
public interface InterestRepository extends JpaRepository<Interest, UUID> {

    Optional<Interest> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNameIgnoreCase(String name);

    // Interest ที่เปิดใช้งาน เรียงตามชื่อ (ใช้แสดงให้ user เลือก)
    List<Interest> findByIsActiveTrueOrderByNameAsc();

    List<Interest> findByNameContainingIgnoreCaseAndIsActiveTrueOrderByNameAsc(String name);

    List<Interest> findByIdInAndIsActiveTrue(Collection<UUID> ids);
}
