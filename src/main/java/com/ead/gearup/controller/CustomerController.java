package com.ead.gearup.controller;

import com.ead.gearup.dto.customer.CustomerRequestDTO;
import com.ead.gearup.dto.customer.CustomerResponseDTO;
import com.ead.gearup.dto.customer.CustomerUpdateDTO;
import com.ead.gearup.dto.response.ApiResponseDTO;
import com.ead.gearup.enums.UserRole;
import com.ead.gearup.service.CustomerService;
import com.ead.gearup.validation.RequiresRole;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Customer API", description = "CRUD operations for customers")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @Operation(summary = "Get all customers")
    @RequiresRole({ UserRole.CUSTOMER, UserRole.ADMIN, UserRole.EMPLOYEE })
    public ResponseEntity<ApiResponseDTO<List<CustomerResponseDTO>>> getAll(HttpServletRequest request) {
        List<CustomerResponseDTO> customers = customerService.getAll();

        ApiResponseDTO<List<CustomerResponseDTO>> response = ApiResponseDTO.<List<CustomerResponseDTO>>builder()
                .status("success")
                .message("Customers retrieved successfully")
                .data(customers)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID")
    @RequiresRole({ UserRole.CUSTOMER, UserRole.ADMIN, UserRole.EMPLOYEE })
    public ResponseEntity<ApiResponseDTO<CustomerResponseDTO>> getById(@PathVariable Long id,
            HttpServletRequest request) {
        CustomerResponseDTO customer = customerService.getById(id);

        ApiResponseDTO<CustomerResponseDTO> response = ApiResponseDTO.<CustomerResponseDTO>builder()
                .status("success")
                .message("Customer retrieved successfully")
                .data(customer)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Create new customer linked to the logged-in user")
    public ResponseEntity<ApiResponseDTO<CustomerResponseDTO>> create(
            @Valid @RequestBody CustomerRequestDTO dto,
            HttpServletRequest request) {

        CustomerResponseDTO createdCustomer = customerService.create(dto);

        ApiResponseDTO<CustomerResponseDTO> response = ApiResponseDTO.<CustomerResponseDTO>builder()
                .status("success")
                .message("Customer created successfully")
                .data(createdCustomer)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update customer by ID")
    @RequiresRole({ UserRole.CUSTOMER })
    public ResponseEntity<ApiResponseDTO<CustomerResponseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody CustomerUpdateDTO dto,
            HttpServletRequest request) {

        CustomerResponseDTO updatedCustomer = customerService.update(id, dto);

        ApiResponseDTO<CustomerResponseDTO> response = ApiResponseDTO.<CustomerResponseDTO>builder()
                .status("success")
                .message("Customer updated successfully")
                .data(updatedCustomer)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete customer by ID")
    @RequiresRole({ UserRole.CUSTOMER })
    public ResponseEntity<ApiResponseDTO<Object>> delete(@PathVariable Long id, HttpServletRequest request) {
        customerService.delete(id);

        ApiResponseDTO<Object> response = ApiResponseDTO.builder()
                .status("success")
                .message("Customer deleted successfully")
                .data(null)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }
}
