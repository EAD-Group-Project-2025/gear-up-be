package com.ead.gearup.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ead.gearup.dto.employee.CreateEmployeeDTO;
import com.ead.gearup.dto.employee.EmployeeResponseDTO;
import com.ead.gearup.dto.employee.UpdateEmployeeDTO;
import com.ead.gearup.exception.UserNotFoundException;
import com.ead.gearup.model.Employee;
import com.ead.gearup.repository.EmployeeRepository;
import com.ead.gearup.utils.EmployeeDTOConverter;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmployeeService {
    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EmployeeDTOConverter converter;

    public EmployeeResponseDTO createEmployee(CreateEmployeeDTO createEmployeeDTO) {
        // Convert DTO -> Employee Entity directly
        Employee employee = converter.convertToEntity(createEmployeeDTO);

        // Save Employee
        Employee savedEmployee = employeeRepository.save(employee);

        // Convert to Response DTO
        return converter.convertToResponseDto(savedEmployee);
    }

    public List<EmployeeResponseDTO> getAllEmployees() {
        List<Employee> employees = employeeRepository.findAll();
        return employees.stream()
                .map(employee -> converter.convertToResponseDto(employee))
                .collect(Collectors.toList());
    }

    public EmployeeResponseDTO getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Employee not found with id: " + id));
        return converter.convertToResponseDto(employee);
    }

    public EmployeeResponseDTO updateEmployee(Long id, UpdateEmployeeDTO updateEmployeeDTO) {
    Employee existingEmployee = employeeRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException("Employee not found with id: " + id));

    converter.updateEntityFromDto(existingEmployee, updateEmployeeDTO);

    Employee savedEmployee = employeeRepository.save(existingEmployee);
    return converter.convertToResponseDto(savedEmployee);
}


    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Employee not found with id: " + id));
        
        employeeRepository.delete(employee);
    }
}
