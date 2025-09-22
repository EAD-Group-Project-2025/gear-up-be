package com.ead.gearup.util;

import org.springframework.stereotype.Component;

import com.ead.gearup.dto.vehicle.VehicleCreateDTO;
import com.ead.gearup.dto.vehicle.VehicleResponseDTO;
import com.ead.gearup.model.Vehicle;

@Component
public class VehicleDTOConverter {

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
}
