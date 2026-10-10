package com.cp.friend.config;

import com.cp.friend.model.Interest;
import com.cp.friend.repository.InterestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seed ความสนใจเริ่มต้นของระบบตอน startup (เฉพาะเมื่อตารางว่าง)
 *
 * เดิมทำผ่าน data.sql ซึ่ง Spring รันเฉพาะ embedded database — พอ deploy ขึ้น
 * PostgreSQL จริงตาราง interests จะว่าง ทำให้ register/matching ไม่มีข้อมูลให้เลือก
 * ย้ายมาเป็น seeder จึงทำงานได้ทุก environment และ idempotent (ข้ามถ้ามีข้อมูลแล้ว)
 */
@Component
@RequiredArgsConstructor
public class InterestSeeder implements ApplicationRunner {

    private static final List<String> DEFAULT_INTERESTS = List.of(
            "Gaming", "Music", "Coding", "Sports",
            "Movies", "Reading", "Travel", "Art");

    private final InterestRepository interestRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (interestRepository.count() > 0) {
            return;
        }
        List<Interest> interests = DEFAULT_INTERESTS.stream()
                .map(name -> {
                    Interest interest = new Interest();
                    interest.setName(name);
                    interest.setActive(true);
                    return interest;
                })
                .toList();
        interestRepository.saveAll(interests);
    }
}
