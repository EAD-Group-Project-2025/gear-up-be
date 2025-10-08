package com.ead.gearup.util;

import com.ead.gearup.dto.project.CreateProjectDTO;
import com.ead.gearup.dto.project.ProjectResponseDTO;
import com.ead.gearup.dto.project.UpdateProjectDTO;
import com.ead.gearup.model.Project;
import com.ead.gearup.model.Task;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectDTOConverter {
    public Project convertToEntity(CreateProjectDTO dto){
        Project project = new Project();
        project.setName(dto.getName());
        project.setDescription(dto.getDescription());
        project.setStartDate(dto.getStartDate());
        project.setEndDate(dto.getEndDate());
        return project;
    }

    public void updateEntityFromDto(UpdateProjectDTO dto, Project project){
        if(dto.getName() != null){
            project.setName(dto.getName());
        }
        if(dto.getDescription() != null){
            project.setDescription(dto.getDescription());
        }
        if(dto.getEndDate() != null){
            project.setEndDate(dto.getEndDate());
        }
        if(dto.getStatus() != null){
            project.setStatus(dto.getStatus());
        }
    }

    public ProjectResponseDTO convertToResponseDto(Project project){
        ProjectResponseDTO dto = new ProjectResponseDTO();

        dto.setId(project.getProjectId());
        dto.setName(project.getName());
        dto.setDescription(project.getDescription());
        dto.setStartDate(project.getStartDate());
        dto.setEndDate(project.getEndDate());
        dto.setStatus(project.getStatus());

        if(project.getAppointment() != null){
            dto.setAppointmentId(project.getAppointment().getAppointmentId());
        }
        if(project.getVehicle() != null){
            dto.setVehicleId(project.getVehicle().getVehicleId());
        }
        if(project.getCustomer() != null){
            dto.setCustomerId(project.getCustomer().getCustomerId());
        }
        if (project.getTasks() != null) {
            dto.setTaskIds(project.getTasks().stream()
                    .map(Task::getTaskId)
                    .toList());
        }

        return dto;
    }
}
