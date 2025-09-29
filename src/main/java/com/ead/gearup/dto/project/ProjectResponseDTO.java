package com.ead.gearup.dto.project;

import com.ead.gearup.enums.ProjectStatus;
import com.ead.gearup.model.Task;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ProjectResponseDTO {
    private long id;
    private String name;
    private String description;
    private ProjectStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
//    private Double totalCost;
    private Long customerId;
    private Long vehicleId;
    private Long appointmentId;
    private List<Long> taskIds;
}
