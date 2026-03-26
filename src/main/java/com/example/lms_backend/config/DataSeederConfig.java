package com.example.lms_backend.config;

import com.example.lms_backend.entity.LeaveType;
import com.example.lms_backend.repository.LeaveTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSeederConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSeederConfig.class);

    @Bean
    public CommandLineRunner seedLeaveTypes(LeaveTypeRepository leaveTypeRepository) {
        return args -> {
            if (leaveTypeRepository.count() == 0) {
                leaveTypeRepository.save(LeaveType.builder()
                        .name("SICK")
                        .maxDaysPerYear(10)
                        .carryForward(false)
                        .description("Sick leave for medical reasons")
                        .build());

                leaveTypeRepository.save(LeaveType.builder()
                        .name("CASUAL")
                        .maxDaysPerYear(12)
                        .carryForward(false)
                        .description("Casual leave for personal reasons")
                        .build());

                leaveTypeRepository.save(LeaveType.builder()
                        .name("EARNED")
                        .maxDaysPerYear(15)
                        .carryForward(true)
                        .description("Earned leave that can be carried forward")
                        .build());

                log.info("✅ Seed data: 3 leave types created (SICK=10, CASUAL=12, EARNED=15)");
            } else {
                log.info("✅ Leave types already exist, skipping seed.");
            }
        };
    }
}
