package com.ead.gearup.util;

import com.ead.gearup.dto.task.TaskCreateDTO;
import com.ead.gearup.dto.task.TaskResponseDTO;
import com.ead.gearup.dto.task.TaskUpdateDTO;
import com.ead.gearup.model.Task;
import com.ead.gearup.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TaskDTOConverter {

    private final AppointmentRepository appointmentRepository;

    // Convert TaskCreateDTO to Task entity
    public Task convertToEntity(TaskCreateDTO dto) {
        Task task = new Task();

        task.setName(dto.getName());
        task.setDescription(dto.getDescription());
        task.setEstimatedHours(dto.getEstimatedHours());
        task.setCost(dto.getCost());
        task.setAppointment(appointmentRepository.findById(dto.getAppointmentId())
                .orElseThrow(() -> {
                    throw new IllegalArgumentException("Invalid appointment ID: " + dto.getAppointmentId());
                }));

        return task;
    }

    // Convert Task entity to Response DTO
    public TaskResponseDTO convertToResponseDto(Task task) {
        TaskResponseDTO dto = new TaskResponseDTO();

        dto.setTaskId(task.getTaskId());
        dto.setName(task.getName());
        dto.setDescription(task.getDescription());
        dto.setEstimatedHours(task.getEstimatedHours());
        dto.setCost(task.getCost());
        dto.setAppointmentId(task.getAppointment().getAppointmentId());
        dto.setStatus(task.getStatus().name());
        dto.setAssignedProject(task.isAssignedProject());

        return dto;
    }

    // Update Task entity from TaskUpdateDTO (partial update)
    public Task updateEntityFromDto(Task task, TaskUpdateDTO dto) {
        if (dto.getName() != null) {
            task.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            task.setDescription(dto.getDescription());
        }
        if (dto.getEstimatedHours() != null) {
            task.setEstimatedHours(dto.getEstimatedHours());
        }
        if (dto.getCost() != null) {
            task.setCost(dto.getCost());
        }
        if (dto.getStatus() != null) {
            task.setStatus(dto.getStatus());
        }
        return task;
    }
}
