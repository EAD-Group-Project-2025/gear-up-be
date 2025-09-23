package com.ead.gearup.dto.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskResponseDTO {

    private Long serviceId;
    private String name;
    private String description;
    private Integer estimatedHours;
    private Double cost;
    private String status;
    private boolean isAssignedProject;
    private Long appointmentId;

}
