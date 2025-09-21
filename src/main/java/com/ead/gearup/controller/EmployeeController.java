package com.ead.gearup.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ead.gearup.dto.employee.CreateEmployeeDTO;
import com.ead.gearup.dto.employee.EmployeeResponseDTO;
import com.ead.gearup.dto.employee.UpdateEmployeeDTO;
import com.ead.gearup.service.EmployeeService;
import com.ead.gearup.validation.RequiresRole;
import com.ead.gearup.enums.UserRole;

import org.springframework.web.bind.annotation.RequestBody; 

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
    
    @Autowired
    private EmployeeService employeeService;

    //@RequiresRole({UserRole.ADMIN, UserRole.EMPLOYEE, UserRole.PUBLIC})
    @PostMapping
    public ResponseEntity<EmployeeResponseDTO> createEmployee(@Valid @RequestBody CreateEmployeeDTO createEmployeeDTO) {
        EmployeeResponseDTO createdEmployee = employeeService.createEmployee(createEmployeeDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdEmployee);
    }

    //@RequiresRole({UserRole.ADMIN, UserRole.EMPLOYEE, UserRole.CUSTOMER, UserRole.PUBLIC})
    @GetMapping
    public ResponseEntity<List<EmployeeResponseDTO>> getAllEmployees() {
        List<EmployeeResponseDTO> employees = employeeService.getAllEmployees();
        return ResponseEntity.ok(employees);
    }

    //@RequiresRole({UserRole.ADMIN, UserRole.EMPLOYEE, UserRole.CUSTOMER, UserRole.PUBLIC})
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponseDTO> getEmployeeById(@PathVariable Long id) {
        EmployeeResponseDTO employee = employeeService.getEmployeeById(id);
        return ResponseEntity.ok(employee);
    }

    //@RequiresRole({UserRole.ADMIN, UserRole.EMPLOYEE, UserRole.PUBLIC})
    @PatchMapping("/{id}")
    public ResponseEntity<EmployeeResponseDTO> updateEmployee(
            @PathVariable Long id, 
            @Valid @RequestBody UpdateEmployeeDTO updateEmployeeDTO) {
        EmployeeResponseDTO updatedEmployee = employeeService.updateEmployee(id, updateEmployeeDTO);
        return ResponseEntity.ok(updatedEmployee);
    }

    
    //@RequiresRole({UserRole.ADMIN, UserRole.EMPLOYEE, UserRole.PUBLIC})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }


}
