package com.ead.gearup.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ead.gearup.dto.employee.CreateEmployeeDTO;
import com.ead.gearup.dto.employee.EmployeeResponseDTO;
import com.ead.gearup.dto.employee.UpdateEmployeeDTO;
import com.ead.gearup.enums.UserRole;
import com.ead.gearup.exception.EmployeeNotFoundException;
import com.ead.gearup.exception.UnauthorizedCustomerAccessException;
import com.ead.gearup.exception.UserNotFoundException;
import com.ead.gearup.model.Employee;
import com.ead.gearup.model.User;
import com.ead.gearup.repository.EmployeeRepository;
import com.ead.gearup.repository.UserRepository;
import com.ead.gearup.service.auth.CurrentUserService;
import com.ead.gearup.util.EmployeeDTOConverter;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeService {
    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EmployeeDTOConverter converter;

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    @Transactional
    public EmployeeResponseDTO createEmployee(CreateEmployeeDTO createEmployeeDTO) {
        User currentUser = currentUserService.getCurrentUser();

        if (currentUser == null) {
            throw new UserNotFoundException("No authenticated user found");
        }

        if (currentUser.getRole() != UserRole.PUBLIC) {
            throw new UnauthorizedCustomerAccessException(
                    "Only public users can create an employee profile");
        }

        currentUser.setRole(UserRole.EMPLOYEE);
        userRepository.save(currentUser);

        // Convert DTO -> Employee Entity directly
        Employee employee = converter.convertToEntity(createEmployeeDTO);

        employee.setUser(currentUser);

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
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid employee ID");
        }

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with id: " + id));
        return converter.convertToResponseDto(employee);
    }

    @Transactional
    public EmployeeResponseDTO updateEmployee(Long id, UpdateEmployeeDTO updateEmployeeDTO) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid employee ID");
        }

        Employee existingEmployee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with id: " + id));

        converter.updateEntityFromDto(existingEmployee, updateEmployeeDTO);

        Employee savedEmployee = employeeRepository.save(existingEmployee);
        return converter.convertToResponseDto(savedEmployee);
    }

    @Transactional
    public void deleteEmployee(Long employeeId) {
        if (employeeId == null || employeeId <= 0) {
            throw new IllegalArgumentException("Invalid employee ID");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with id: " + employeeId));

        // Handle linked User
        User linkedUser = employee.getUser();
        if (linkedUser != null) {
            linkedUser.setRole(UserRole.PUBLIC);
            employee.setUser(null);
            userRepository.save(linkedUser);
        }

        // Delete the employee safely
        employeeRepository.delete(employee);
    }
}
