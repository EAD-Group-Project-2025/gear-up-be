package com.ead.gearup.service;

import com.ead.gearup.dto.project.CreateProjectDTO;
import com.ead.gearup.dto.project.UpdateProjectDTO;
import com.ead.gearup.dto.project.ProjectResponseDTO;
import com.ead.gearup.dto.employee.EmployeeProjectDetailResponseDTO;
import com.ead.gearup.dto.employee.EmployeeProjectResponseDTO;
import com.ead.gearup.enums.ProjectStatus;
import com.ead.gearup.enums.UserRole;
import com.ead.gearup.exception.*;
import com.ead.gearup.model.*;
import com.ead.gearup.repository.*;
import com.ead.gearup.service.auth.CurrentUserService;
import com.ead.gearup.validation.RequiresRole;

import com.ead.gearup.util.ProjectDTOConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final CurrentUserService currentUserService;
    private final ProjectRepository projectRepository;
    private final AppointmentRepository appointmentRepository;
    private final VehicleRepository vehicleRepository;
    private final TaskRepository taskRepository;
    private final ProjectDTOConverter projectDTOConverter;


    @RequiresRole({UserRole.EMPLOYEE, UserRole.ADMIN})
    public ProjectResponseDTO createProject(CreateProjectDTO dto) {
        Appointment appointment = appointmentRepository.findById(dto.getAppointmentId())
                .orElseThrow(() -> new AppointmentNotFoundException(
                        "Appointment not found: " + dto.getAppointmentId()));

        Vehicle vehicle = vehicleRepository.findById(dto.getVehicleId())
                .orElseThrow(() -> new VehicleNotFoundException(
                        "Vehicle not found: " + dto.getVehicleId()));

        List<Task> tasks = taskRepository.findAllById(dto.getTaskIds());
        if (tasks.isEmpty()) {
            throw new TaskNotFoundException("No valid tasks found for project");
        }

        Project project = projectDTOConverter.convertToEntity(dto);
        project.setAppointment(appointment);
        project.setVehicle(vehicle);
        project.setTasks(tasks);
        project.setStatus(ProjectStatus.CREATED);

        projectRepository.save(project);

        return projectDTOConverter.convertToResponseDto(project);
    }

    @RequiresRole({UserRole.EMPLOYEE, UserRole.ADMIN})
    public ProjectResponseDTO updateProject(Long projectId, UpdateProjectDTO dto) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));

        projectDTOConverter.updateEntityFromDto(dto, project);
        projectRepository.save(project);

        return projectDTOConverter.convertToResponseDto(project);
    }

    @RequiresRole({UserRole.CUSTOMER, UserRole.EMPLOYEE, UserRole.ADMIN})
    public ProjectResponseDTO getProjectById(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));

        UserRole role = currentUserService.getCurrentUserRole();

        if (role == UserRole.CUSTOMER) {
            Long customerId = currentUserService.getCurrentEntityId();
            if (!project.getCustomer().getCustomerId().equals(customerId)) {
                throw new UnauthorizedProjectAccessException("You cannot access this project.");
            }
        }

        if (role == UserRole.EMPLOYEE) {
            Long employeeId = currentUserService.getCurrentEntityId();
            boolean assigned = project.getAssignedEmployees().stream()
                    .anyMatch(e -> e.getEmployeeId().equals(employeeId));
            if (!assigned) {
                throw new UnauthorizedProjectAccessException("You are not assigned to this project.");
            }
        }

        return projectDTOConverter.convertToResponseDto(project);
    }

    @RequiresRole({UserRole.CUSTOMER, UserRole.EMPLOYEE, UserRole.ADMIN})
    public List<ProjectResponseDTO> getAllProjects() {
        UserRole role = currentUserService.getCurrentUserRole();

        if (role == UserRole.CUSTOMER) {
            Long customerId = currentUserService.getCurrentEntityId();
            return projectRepository.findAll().stream()
                    .filter(p -> p.getCustomer().getCustomerId().equals(customerId))
                    .map(projectDTOConverter::convertToResponseDto)
                    .toList();
        }

        if (role == UserRole.EMPLOYEE) {
            Long employeeId = currentUserService.getCurrentEntityId();
            return projectRepository.findAll().stream()
                    .filter(p -> p.getAssignedEmployees().stream()
                            .anyMatch(e -> e.getEmployeeId().equals(employeeId)))
                    .map(projectDTOConverter::convertToResponseDto)
                    .toList();
        }

        // ADMIN → all projects
        return projectRepository.findAll().stream()
                .map(projectDTOConverter::convertToResponseDto)
                .toList();
    }

    @RequiresRole(UserRole.ADMIN)
    public void deleteProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));

        projectRepository.delete(project);
    }

    @RequiresRole({UserRole.EMPLOYEE, UserRole.ADMIN})
    public Map<String, Long> getProjectCountByStatus(Long employeeId) {
        List<Object[]> results = projectRepository.countProjectsByStatusForEmployee(employeeId);
        Map<String, Long> response = new HashMap<>();
    
        for (Object[] row : results) {
            String status = row[0].toString();
            Long count = (Long) row[1];
            response.put(status, count);
        }
    
        return response;
    }

    @RequiresRole({UserRole.EMPLOYEE, UserRole.ADMIN})
    public List<EmployeeProjectResponseDTO> getAssignedProjectsForCurrentEmployee() {
        Long employeeId = currentUserService.getCurrentUserId();
        List<Project> projects = projectRepository.findByAssignedEmployeesEmployeeId(employeeId);

        return projects.stream()
                .map(p -> new EmployeeProjectResponseDTO(
                        p.getProjectId(),
                        p.getName()
                ))
                .toList();
    }

    @RequiresRole({UserRole.EMPLOYEE, UserRole.ADMIN})
    public EmployeeProjectDetailResponseDTO getAssignedProjectDetail(Long projectId) {
        Long employeeId = currentUserService.getCurrentUserId();

        Project project = projectRepository.findByProjectIdAndAssignedEmployeesEmployeeId(projectId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found or not assigned to you: " + projectId));

        return new EmployeeProjectDetailResponseDTO(
                project.getCustomer().getUser().getName(),
                project.getVehicle().getModel(),
                project.getEndDate(),
                project.getStatus()
        );
    }
}
