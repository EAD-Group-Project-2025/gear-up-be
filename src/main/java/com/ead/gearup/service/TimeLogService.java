package com.ead.gearup.service;

import com.ead.gearup.dto.timelog.*;
import com.ead.gearup.model.Employee;
import com.ead.gearup.model.TimeLog;
import com.ead.gearup.repository.TimeLogRepository;
import com.ead.gearup.util.TimeLogDTOConverter;  
import com.ead.gearup.exception.ResourceNotFoundException;
import com.ead.gearup.repository.EmployeeRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

public interface TimeLogService {
    TimeLogResponseDTO createTimeLog(CreateTimeLogDTO dto);
    TimeLogResponseDTO getTimeLogById(Long id);
    List<TimeLogResponseDTO> getAllTimeLogs();
    TimeLogResponseDTO updateTimeLog(Long id, UpdateTimeLogDTO dto);
    void deleteTimeLog(Long id);

    @Service
    @Transactional
    class TimeLogServiceImpl implements TimeLogService {

        private final TimeLogRepository timeLogRepository;
        private final TimeLogDTOConverter converter;
        private final EmployeeRepository employeeRepository;

        public TimeLogServiceImpl(TimeLogRepository timeLogRepository, TimeLogDTOConverter converter, EmployeeRepository employeeRepository) {
            this.timeLogRepository = timeLogRepository;
            this.converter = converter;
            this.employeeRepository = employeeRepository;
        }

        @Override
        public TimeLogResponseDTO createTimeLog(CreateTimeLogDTO dto) {
            Employee employee = employeeRepository.findById(dto.getEmployeeId())
                  .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id " + dto.getEmployeeId()));

            TimeLog timeLog = converter.convertToEntity(dto, employee);
            TimeLog saved = timeLogRepository.save(timeLog);
            return converter.convertToResponseDTO(saved);
        }

        @Override
        public TimeLogResponseDTO getTimeLogById(Long id) {
            TimeLog timeLog = timeLogRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("TimeLog not found with id " + id));
            return converter.convertToResponseDTO(timeLog);
        }

        @Override
        public List<TimeLogResponseDTO> getAllTimeLogs() {
            return timeLogRepository.findAll().stream()
                    .map(converter::convertToResponseDTO)
                    .collect(Collectors.toList());
        }

        @Override
        public TimeLogResponseDTO updateTimeLog(Long id, UpdateTimeLogDTO dto) {
            TimeLog timeLog = timeLogRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("TimeLog not found with id " + id));
            converter.updateEntityFromDTO(timeLog, dto); 
            return converter.convertToResponseDTO(timeLogRepository.save(timeLog));
        }

        @Override
        public void deleteTimeLog(Long id) {
            if (!timeLogRepository.existsById(id)) {
                throw new ResourceNotFoundException("TimeLog not found with id " + id);
            }
            timeLogRepository.deleteById(id);
        }
    }
}
