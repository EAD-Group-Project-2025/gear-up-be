package com.ead.gearup.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.ead.gearup.dto.employee.CreateEmployeeDTO;
import com.ead.gearup.dto.employee.EmployeeResponseDTO;
import com.ead.gearup.dto.employee.UpdateEmployeeDTO;
import com.ead.gearup.model.Employee;
import com.ead.gearup.model.User;
import com.ead.gearup.enums.UserRole;

@Component
public class EmployeeDTOConverter {

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Convert CreateEmployeeDTO to Employee entity
    public Employee convertToEntity(CreateEmployeeDTO dto) {
        Employee employee = new Employee();

        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(UserRole.EMPLOYEE); 

        employee.setUser(user);
        employee.setSpecialization(dto.getSpecialization());
        employee.setHireDate(dto.getHireDate());

        return employee;
    }

    // Convert Employee entity to EmployeeResponseDTO
    public EmployeeResponseDTO convertToResponseDto(Employee employee) {
        EmployeeResponseDTO dto = new EmployeeResponseDTO();
        dto.setEmployeeId(employee.getEmployeeId());

        dto.setEmail(employee.getUser().getEmail());
        dto.setName(employee.getUser().getName());
        dto.setSpecialization(employee.getSpecialization());
        dto.setHireDate(employee.getHireDate());
        dto.setCreatedAt(employee.getCreatedAt());
        dto.setUpdatedAt(employee.getUpdatedAt());

        return dto;
    }

    // Update employee entity from UpdateEmployeeDTO
    public void updateEntityFromDto(Employee employee, UpdateEmployeeDTO dto) {
        User user = employee.getUser();

        if (dto.getName() != null) {
            user.setName(dto.getName());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(dto.getPassword())); 
        }

        if (dto.getSpecialization() != null) {
            employee.setSpecialization(dto.getSpecialization());
        }
        if (dto.getHireDate() != null) {
            employee.setHireDate(dto.getHireDate());
        }
    }
}
