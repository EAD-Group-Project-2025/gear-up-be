package com.ead.gearup.dto.task;

import com.ead.gearup.enums.TaskStatus;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskUpdateDTO {
    private String name;
    private String description;

    @Positive
    private Integer estimatedHours;

    @Positive
    public Double cost;

    public TaskStatus status;
}
