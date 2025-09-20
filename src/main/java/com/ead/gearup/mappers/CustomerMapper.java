package com.ead.gearup.mappers;

import com.ead.gearup.dto.customer.CustomerRequestDTO;
import com.ead.gearup.dto.customer.CustomerResponseDTO;
import com.ead.gearup.model.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mapping(target = "passwordHash", source = "password")
    Customer toEntity(CustomerRequestDTO dto);

    CustomerResponseDTO toDto(Customer customer);
}
