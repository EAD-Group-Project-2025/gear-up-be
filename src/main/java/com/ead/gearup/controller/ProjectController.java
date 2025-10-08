package com.ead.gearup.controller;

import com.ead.gearup.dto.project.CreateProjectDTO;
import com.ead.gearup.dto.project.UpdateProjectDTO;
import com.ead.gearup.dto.response.ApiResponseDTO;
import com.ead.gearup.dto.project.ProjectResponseDTO;
import com.ead.gearup.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseDTO<ProjectResponseDTO>> createProject(
            @RequestBody @Valid CreateProjectDTO projectCreateDTO,
            HttpServletRequest request
    ) {
        ProjectResponseDTO projectResponseDTO = projectService.createProject(projectCreateDTO);

        ApiResponseDTO<ProjectResponseDTO> response = ApiResponseDTO.<ProjectResponseDTO>builder()
                .status("success")
                .message("Project created successfully")
                .data(projectResponseDTO)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseDTO<ProjectResponseDTO>> updateProject(
            @PathVariable Long id,
            @RequestBody @Valid UpdateProjectDTO projectUpdateDTO,
            HttpServletRequest request
    ) {
        ProjectResponseDTO projectResponseDTO = projectService.updateProject(id, projectUpdateDTO);

        ApiResponseDTO<ProjectResponseDTO> response = ApiResponseDTO.<ProjectResponseDTO>builder()
                .status("success")
                .message("Project updated successfully")
                .data(projectResponseDTO)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<ProjectResponseDTO>> getProjectById(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        ProjectResponseDTO projectResponseDTO = projectService.getProjectById(id);

        ApiResponseDTO<ProjectResponseDTO> response = ApiResponseDTO.<ProjectResponseDTO>builder()
                .status("success")
                .message("Project retrieved successfully")
                .data(projectResponseDTO)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseDTO<List<ProjectResponseDTO>>> getAllProjects(HttpServletRequest request) {
        List<ProjectResponseDTO> projects = projectService.getAllProjects();

        ApiResponseDTO<List<ProjectResponseDTO>> response = ApiResponseDTO.<List<ProjectResponseDTO>>builder()
                .status("success")
                .message("Projects retrieved successfully")
                .data(projects)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteProject(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        projectService.deleteProject(id);

        ApiResponseDTO<Void> response = ApiResponseDTO.<Void>builder()
                .status("success")
                .message("Project deleted successfully")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(response);
    }
}
