package com.ead.gearup.dto.project;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProjectDTO {

    private String projectName;
    private String description;
    private LocalDate endDate;
    private String status;
}
