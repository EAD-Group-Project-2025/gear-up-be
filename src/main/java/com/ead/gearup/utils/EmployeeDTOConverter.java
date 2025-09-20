package com.ead.gearup.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.ead.gearup.dto.employee.CreateEmployeeDTO;
import com.ead.gearup.dto.employee.EmployeeResponseDTO;
import com.ead.gearup.dto.employee.UpdateEmployeeDTO;
import com.ead.gearup.model.Employee;

@Component
public class EmployeeDTOConverter {

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Convert CreateEmployeeDTO to Employee entity 
    public Employee convertToEntity(CreateEmployeeDTO dto) {
        Employee employee = new Employee();
        employee.setFirstName(dto.getFirstName());
        employee.setLastName(dto.getLastName());
        employee.setEmail(dto.getEmail());
        employee.setPassword(passwordEncoder.encode(dto.getPassword())); 
        employee.setRole(dto.getRole());
        employee.setSpecialization(dto.getSpecialization());
        employee.setHireDate(dto.getHireDate());

        return employee;
    }
    
    // Convert Employee entity to EmployeeResponseDTO 
    public EmployeeResponseDTO convertToResponseDto(Employee employee) {
        EmployeeResponseDTO dto = new EmployeeResponseDTO();
        dto.setEmployeeId(employee.getEmployeeId());
        dto.setFirstName(employee.getFirstName());
        dto.setLastName(employee.getLastName());
        dto.setEmail(employee.getEmail());
        dto.setRole(employee.getRole());
        dto.setSpecialization(employee.getSpecialization());
        dto.setHireDate(employee.getHireDate());
        dto.setCreatedAt(employee.getCreatedAt());
        dto.setUpdatedAt(employee.getUpdatedAt());
        
        return dto;
    }

    // Update employee entity from UpdateEmployeeDTO
    public void updateEntityFromDto(Employee employee, UpdateEmployeeDTO dto) {
        if (dto.getFirstName() != null) {
            employee.setFirstName(dto.getFirstName());
        }
        if (dto.getLastName() != null) {
            employee.setLastName(dto.getLastName());
        }
        if (dto.getEmail() != null) {
            employee.setEmail(dto.getEmail());
        }
       if (dto.getPassword() != null) {
           employee.setPassword(passwordEncoder.encode(dto.getPassword())); // hash password
        }
       if (dto.getRole() != null) {
           employee.setRole(dto.getRole());
        }
       if (dto.getSpecialization() != null) {
           employee.setSpecialization(dto.getSpecialization());
       }
       if (dto.getHireDate() != null) {
           employee.setHireDate(dto.getHireDate());
       }
   }
}


