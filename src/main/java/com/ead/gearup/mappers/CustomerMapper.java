package com.ead.gearup.mappers;

import com.ead.gearup.dto.customer.CustomerRequestDTO;
import com.ead.gearup.dto.customer.CustomerResponseDTO;
import com.ead.gearup.model.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    // Map only profile-related fields
    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "user", ignore = true) // User will be set manually in the service
    @Mapping(target = "createdAt", ignore = true)
    Customer toEntity(CustomerRequestDTO dto);

    // Convert Customer -> ResponseDTO
    @Mapping(source = "user.email", target = "email")  // take email from linked User
    CustomerResponseDTO toDto(Customer customer);
}
