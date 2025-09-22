package com.ead.gearup.controller;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ead.gearup.dto.vehicle.VehicleCreateDTO;
import com.ead.gearup.dto.vehicle.VehicleResponseDTO;
import com.ead.gearup.enums.UserRole;
import com.ead.gearup.service.VehicleService;
import com.ead.gearup.validation.RequiresRole;
import com.ead.gearup.dto.response.ApiResponseDTO;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(value = "/api/vehicles")
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
}
