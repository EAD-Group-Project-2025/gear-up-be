package com.ead.gearup.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ead.gearup.dto.service.TaskCreateDTO;
import com.ead.gearup.dto.service.TaskResponseDTO;
import com.ead.gearup.enums.UserRole;
import com.ead.gearup.service.TaskService;
import com.ead.gearup.validation.RequiresRole;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;

    // @RequiresRole({ UserRole.EMPLOYEE })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TaskResponseDTO> createTask(@RequestBody @Valid TaskCreateDTO taskCreateDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(taskCreateDTO));
    }
}
