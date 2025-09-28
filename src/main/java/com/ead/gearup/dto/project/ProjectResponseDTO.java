package com.ead.gearup.dto.project;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ProjectResponseDTO {
    private long id;
    private String projectName;
    private String description;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
//    private Double totalCost;
    private Long vehicleId;
    private Long appointmentId;
    private List<Long> taskIds;
}
