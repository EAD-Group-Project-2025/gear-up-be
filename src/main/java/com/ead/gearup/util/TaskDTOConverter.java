package com.ead.gearup.util;

import org.springframework.stereotype.Component;

import com.ead.gearup.dto.task.TaskCreateDTO;
import com.ead.gearup.dto.task.TaskResponseDTO;
import com.ead.gearup.model.Task;
import com.ead.gearup.repository.AppointmentRepository;

import lombok.RequiredArgsConstructor;

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

    public TaskResponseDTO convertToResponseDto(Task task) {
        TaskResponseDTO dto = new TaskResponseDTO();

        dto.setServiceId(task.getServiceId());
        dto.setName(task.getName());
        dto.setDescription(task.getDescription());
        dto.setEstimatedHours(task.getEstimatedHours());
        dto.setCost(task.getCost());
        dto.setAppointmentId(task.getAppointment().getAppointmentId());
        dto.setStatus(task.getStatus().name());
        dto.setAssignedProject(task.isAssignedProject());
        dto.setAppointmentId(task.getAppointment().getAppointmentId());

        return dto;
    }
}
