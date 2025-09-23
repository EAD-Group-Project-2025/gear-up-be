package com.ead.gearup.controller;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ead.gearup.dto.vehicle.VehicleCreateDTO;
import com.ead.gearup.dto.vehicle.VehicleResponseDTO;
import com.ead.gearup.dto.vehicle.VehicleUpdateDTO;
import com.ead.gearup.enums.UserRole;
import com.ead.gearup.service.VehicleService;
import com.ead.gearup.validation.RequiresRole;
import com.ead.gearup.dto.response.ApiResponseDTO;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(value = "/api/v1/vehicles")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @RequiresRole({ UserRole.CUSTOMER })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseDTO<VehicleResponseDTO>> createVehicle(
            @Valid @RequestBody VehicleCreateDTO vehicleCreateDTO, HttpServletRequest request) {

        VehicleResponseDTO createdVehicle = vehicleService.createVehicle(vehicleCreateDTO);

        ApiResponseDTO<VehicleResponseDTO> response = ApiResponseDTO.<VehicleResponseDTO>builder()
                .status("success")
                .message("Vehicle created successfully")
                .data(createdVehicle)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<VehicleResponseDTO>> getVehicleById(@PathVariable Long id,
            HttpServletRequest request) {

        VehicleResponseDTO vehicle = vehicleService.getVehicleById(id);

        ApiResponseDTO<VehicleResponseDTO> response = ApiResponseDTO.<VehicleResponseDTO>builder()
                .status("success")
                .message("Vehicle retrieved successfully")
                .data(vehicle)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok().body(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<VehicleResponseDTO>>> getAllVehicles(HttpServletRequest request) {

        List<VehicleResponseDTO> vehicles = vehicleService.getAllVehicles();

        ApiResponseDTO<List<VehicleResponseDTO>> response = ApiResponseDTO.<List<VehicleResponseDTO>>builder()
                .status("success")
                .message("Vehicles retrieved successfully")
                .data(vehicles)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteVehicle(@PathVariable Long id, HttpServletRequest request) {

        vehicleService.deleteVehicle(id);

        ApiResponseDTO<Void> response = ApiResponseDTO.<Void>builder()
                .status("success")
                .message("Vehicle deleted successfully")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .data(null)
                .build();

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<VehicleResponseDTO>> updateVehicle(
            @PathVariable Long id,
            @Valid @RequestBody VehicleUpdateDTO updateVehicleDTO,
            HttpServletRequest request) {

        VehicleResponseDTO updatedVehicle = vehicleService.updateVehicle(id, updateVehicleDTO);

        ApiResponseDTO<VehicleResponseDTO> response = ApiResponseDTO.<VehicleResponseDTO>builder()
                .status("success")
                .message("Vehicle updated successfully")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .data(updatedVehicle)
                .build();

        return ResponseEntity.ok(response);
    }
}
