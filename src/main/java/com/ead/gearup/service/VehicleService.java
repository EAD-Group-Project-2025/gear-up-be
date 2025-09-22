package com.ead.gearup.service;

import org.springframework.stereotype.Service;

import com.ead.gearup.dto.vehicle.VehicleCreateDTO;
import com.ead.gearup.dto.vehicle.VehicleResponseDTO;
import com.ead.gearup.exception.CustomerNotFoundException;
import com.ead.gearup.model.Customer;
import com.ead.gearup.model.Vehicle;
import com.ead.gearup.repository.CustomerRepository;
import com.ead.gearup.repository.VehicleRepository;
import com.ead.gearup.service.auth.CurrentUserService;
import com.ead.gearup.util.VehicleDTOConverter;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final CurrentUserService currentUserService;
    private final VehicleDTOConverter converter;
    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;

    @Transactional
    public VehicleResponseDTO createVehicle(VehicleCreateDTO createVehicleDTO) {

        Customer customer = customerRepository.findById(currentUserService.getCurrentEntityId())
                .orElseThrow(() -> new CustomerNotFoundException(
                        "Customer not found: " + currentUserService.getCurrentEntityId()));

        // Check if vehicle already exists by license plate
        vehicleRepository.findByLicensePlate(createVehicleDTO.getLicensePlate())
                .ifPresent(v -> {
                    throw new IllegalArgumentException("Vehicle with this license plate already exists");
                });

        // Check if vehicle already exists by vin
        vehicleRepository.findByVin(createVehicleDTO.getVin())
                .ifPresent(v -> {
                    throw new IllegalArgumentException("Vehicle with this VIN already exists");
                });

        Vehicle vehicle = converter.convertToEntity(createVehicleDTO);

        vehicle.setCustomer(customer);
        Vehicle savedVehicle = vehicleRepository.save(vehicle);

        return converter.convertToResponseDto(savedVehicle);
    }

}
