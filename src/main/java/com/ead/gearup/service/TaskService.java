package com.ead.gearup.service;

import org.springframework.stereotype.Service;

import com.ead.gearup.dto.task.TaskCreateDTO;
import com.ead.gearup.dto.task.TaskResponseDTO;
import com.ead.gearup.model.Task;
import com.ead.gearup.repository.TaskRepository;
import com.ead.gearup.util.TaskDTOConverter;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskDTOConverter taskDTOConverter;
    private final TaskRepository taskRepository;

    public TaskResponseDTO createTask(TaskCreateDTO taskCreateDTO) {

        Task task = taskDTOConverter.convertToEntity(taskCreateDTO);
        taskRepository.save(task);

        return taskDTOConverter.convertToResponseDto(task);
    }

}
