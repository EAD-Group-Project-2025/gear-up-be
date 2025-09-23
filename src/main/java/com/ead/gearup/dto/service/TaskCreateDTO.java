package com.ead.gearup.dto.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskCreateDTO {

    private String name;
    private String description;
    private Integer estimatedHours;
    private Double cost;
    private Long appointmentId;
}
