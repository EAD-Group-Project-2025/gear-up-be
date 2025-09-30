package com.ead.gearup.service;

import com.ead.gearup.dto.timelog.*;
import com.ead.gearup.exception.ResourceNotFoundException;
import com.ead.gearup.model.Employee;
import com.ead.gearup.model.Task;
import com.ead.gearup.model.TimeLog;
import com.ead.gearup.repository.EmployeeRepository;
import com.ead.gearup.repository.TaskRepository;
import com.ead.gearup.repository.TimeLogRepository;
import com.ead.gearup.util.TimeLogDTOConverter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TimeLogService {

    private final TimeLogRepository timeLogRepository;
    private final TimeLogDTOConverter converter;
    private final EmployeeRepository employeeRepository;
    private final TaskRepository taskRepository;

    public TimeLogService(TimeLogRepository timeLogRepository,
                          TimeLogDTOConverter converter,
                          EmployeeRepository employeeRepository,
                          TaskRepository taskRepository) {
        this.timeLogRepository = timeLogRepository;
        this.converter = converter;
        this.employeeRepository = employeeRepository;
        this.taskRepository = taskRepository;
    }

    public TimeLogResponseDTO createTimeLog(CreateTimeLogDTO dto) {
        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id " + dto.getEmployeeId()));

        Task task = taskRepository.findById(dto.getTaskId())
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + dto.getTaskId()));

        TimeLog timeLog = converter.convertToEntity(dto, employee, task);
        TimeLog saved = timeLogRepository.save(timeLog);
        return converter.convertToResponseDTO(saved);
    }

    public TimeLogResponseDTO getTimeLogById(Long id) {
        TimeLog timeLog = timeLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TimeLog not found with id " + id));
        return converter.convertToResponseDTO(timeLog);
    }

    public List<TimeLogResponseDTO> getAllTimeLogs() {
        return timeLogRepository.findAll().stream()
                .map(converter::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public TimeLogResponseDTO updateTimeLog(Long id, UpdateTimeLogDTO dto) {
        TimeLog timeLog = timeLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TimeLog not found with id " + id));
        converter.updateEntityFromDTO(timeLog, dto);
        return converter.convertToResponseDTO(timeLogRepository.save(timeLog));
    }

    public void deleteTimeLog(Long id) {
        if (!timeLogRepository.existsById(id)) {
            throw new ResourceNotFoundException("TimeLog not found with id " + id);
        }
        timeLogRepository.deleteById(id);
    }
}
