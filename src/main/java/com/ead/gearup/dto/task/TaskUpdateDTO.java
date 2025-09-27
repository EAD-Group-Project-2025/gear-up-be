package com.ead.gearup.dto.task;

import com.ead.gearup.enums.TaskStatus;
import jakarta.validation.constraints.Positive;

public class TaskUpdateDTO {
    private String name;
    private String description;

    @Positive
    private Integer estimatedHours;

    @Positive
    public Double cost;

    public TaskStatus status;
}
