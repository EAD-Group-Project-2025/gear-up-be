package com.ead.gearup.controller;

import com.ead.gearup.dto.customer.CustomerResponseDTO;
import com.ead.gearup.dto.customer.CustomerRequestDTO;
import com.ead.gearup.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customer API", description = "CRUD operations for customers")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @Operation(summary = "Get all customers")
    public List<CustomerResponseDTO> getAll() {
        return customerService.getAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID")
    public CustomerResponseDTO getById(@PathVariable Long id) {
        return customerService.getById(id);
    }

    @PostMapping("/{userId}")
    @Operation(summary = "Create new customer linked to an existing user")
    public CustomerResponseDTO create(
            @PathVariable Long userId,
            @RequestBody CustomerRequestDTO dto
    ) {
        return customerService.create(userId, dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update customer by ID")
    public CustomerResponseDTO update(
            @PathVariable Long id,
            @RequestBody CustomerRequestDTO dto
    ) {
        return customerService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete customer by ID")
    public void delete(@PathVariable Long id) {
        customerService.delete(id);
    }
}
