package com.example.lms_backend.dto;

import com.example.lms_backend.entity.LeaveType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveTypeDTO {

    private Integer id;

    @NotBlank(message = "Leave type name is required")
    private String name;

    @NotNull(message = "Max days per year is required")
    private Integer maxDaysPerYear;

    private Boolean carryForward;

    private String description;

    public static LeaveTypeDTO fromEntity(LeaveType lt) {
        return LeaveTypeDTO.builder()
                .id(lt.getId())
                .name(lt.getName())
                .maxDaysPerYear(lt.getMaxDaysPerYear())
                .carryForward(lt.getCarryForward())
                .description(lt.getDescription())
                .build();
    }
}
