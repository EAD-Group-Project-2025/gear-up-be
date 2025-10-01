package com.ead.gearup.util;

import org.springframework.stereotype.Component;

import com.ead.gearup.dto.employee.CreateEmployeeDTO;
import com.ead.gearup.dto.employee.EmployeeResponseDTO;
import com.ead.gearup.dto.employee.UpdateEmployeeDTO;
import com.ead.gearup.model.Employee;
import com.ead.gearup.model.User;

@Component
public class EmployeeDTOConverter {

    // Convert CreateEmployeeDTO to Employee entity
    public Employee convertToEntity(CreateEmployeeDTO dto) {
        Employee employee = new Employee();

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

        if (dto.getSpecialization() != null) {
            employee.setSpecialization(dto.getSpecialization());
        }
        if (dto.getHireDate() != null) {
            employee.setHireDate(dto.getHireDate());
        }
    }
}
