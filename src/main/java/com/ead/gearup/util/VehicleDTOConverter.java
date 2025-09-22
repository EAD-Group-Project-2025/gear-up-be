package com.ead.gearup.util;

import org.springframework.stereotype.Component;

import com.ead.gearup.dto.vehicle.VehicleCreateDTO;
import com.ead.gearup.dto.vehicle.VehicleResponseDTO;
import com.ead.gearup.dto.vehicle.VehicleUpdateDTO;
import com.ead.gearup.model.Vehicle;
import com.ead.gearup.repository.VehicleRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class VehicleDTOConverter {

    private final VehicleRepository vehicleRepository;

    // Convert VehicleDTO to Vehicle entity
    public Vehicle convertToEntity(VehicleCreateDTO dto) {
        Vehicle vehicle = new Vehicle();

        vehicle.setLicensePlate(dto.getLicensePlate());
        vehicle.setMake(dto.getMake());
        vehicle.setModel(dto.getModel());
        vehicle.setVin(dto.getVin());
        vehicle.setYear(dto.getYear());

        return vehicle;
    }

    public VehicleResponseDTO convertToResponseDto(Vehicle savedVehicle) {
        VehicleResponseDTO dto = new VehicleResponseDTO();

        dto.setId(savedVehicle.getVehicleId());
        dto.setLicensePlate(savedVehicle.getLicensePlate());
        dto.setMake(savedVehicle.getMake());
        dto.setModel(savedVehicle.getModel());
        dto.setVin(savedVehicle.getVin());
        dto.setYear(savedVehicle.getYear());

        return dto;
    }

    public void updateEntityFromDto(Vehicle vehicle, VehicleUpdateDTO dto) {

        if (dto.getLicensePlate() != null) {
            if (vehicleRepository.existsByLicensePlateAndVehicleIdNot(dto.getLicensePlate(), vehicle.getVehicleId())) {
                throw new IllegalArgumentException("Vehicle with this license plate already exists");
            }
            vehicle.setLicensePlate(dto.getLicensePlate());
        }

        if (dto.getMake() != null) {
            vehicle.setMake(dto.getMake());
        }

        if (dto.getModel() != null) {
            vehicle.setModel(dto.getModel());
        }

        if (dto.getVin() != null) {
            if (vehicleRepository.existsByVinAndVehicleIdNot(dto.getVin(), vehicle.getVehicleId())) {
                throw new IllegalArgumentException("Vehicle with this VIN already exists");
            }
            vehicle.setVin(dto.getVin());
        }

        if (dto.getYear() != null) {
            vehicle.setYear(dto.getYear());
        }
    }
}
