package com.ead.gearup.controller;

import com.ead.gearup.dto.CreateEmployeeRequest;
import com.ead.gearup.dto.CreateEmployeeResponse;
import com.ead.gearup.dto.EmployeeDTO;
import com.ead.gearup.service.EmployeeManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/employees")
@RequiredArgsConstructor
@Tag(name = "Admin - Employee Management", description = "Endpoints for managing employee accounts (Admin only)")
@SecurityRequirement(name = "bearerAuth")
public class EmployeeManagementController {

    private final EmployeeManagementService employeeManagementService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create new employee account", 
               description = "Creates a new employee account with auto-generated temporary password sent via email")
    public ResponseEntity<ApiResponse<CreateEmployeeResponse>> createEmployee(
            @Valid @RequestBody CreateEmployeeRequest request) {
        
        CreateEmployeeResponse response = employeeManagementService.createEmployee(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.success(
                response, 
                "Employee account created successfully. Temporary password sent to " + request.getEmail()
            )
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all employees", description = "Retrieves list of all employee accounts")
    public ResponseEntity<ApiResponse<List<EmployeeDTO>>> getAllEmployees() {
        List<EmployeeDTO> employees = employeeManagementService.getAllEmployees();
        return ResponseEntity.ok(ApiResponse.success(employees, "Employees retrieved successfully"));
    }

    @PutMapping("/{employeeId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate employee account", description = "Deactivates an employee account")
    public ResponseEntity<ApiResponse<Void>> deactivateEmployee(@PathVariable Long employeeId) {
        employeeManagementService.deactivateEmployee(employeeId);
        return ResponseEntity.ok(ApiResponse.success(null, "Employee account deactivated successfully"));
    }

    @PutMapping("/{employeeId}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reactivate employee account", description = "Reactivates a deactivated employee account")
    public ResponseEntity<ApiResponse<Void>> reactivateEmployee(@PathVariable Long employeeId) {
        employeeManagementService.reactivateEmployee(employeeId);
        return ResponseEntity.ok(ApiResponse.success(null, "Employee account reactivated successfully"));
    }

    @PostMapping("/{employeeId}/resend-password")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Resend temporary password", 
               description = "Generates and sends a new temporary password to employee's email")
    public ResponseEntity<ApiResponse<Void>> resendTemporaryPassword(@PathVariable Long employeeId) {
        employeeManagementService.resendTemporaryPassword(employeeId);
        return ResponseEntity.ok(ApiResponse.success(null, "Temporary password sent successfully"));
    }
}

class ApiResponse<T> {
    private String status;
    private String message;
    private T data;
    private String timestamp;

    public static <T> ApiResponse<T> success(T data, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.status = "success";
        response.message = message;
        response.data = data;
        response.timestamp = java.time.LocalDateTime.now().toString();
        return response;
    }

    // Getters and Setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
