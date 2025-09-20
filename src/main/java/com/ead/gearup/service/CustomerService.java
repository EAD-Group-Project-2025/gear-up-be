package com.ead.gearup.service;

import com.ead.gearup.dto.customer.CustomerRequestDTO;
import com.ead.gearup.dto.customer.CustomerResponseDTO;
import com.ead.gearup.mappers.CustomerMapper;
import com.ead.gearup.model.Customer;
import com.ead.gearup.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final PasswordEncoder passwordEncoder;

    public List<CustomerResponseDTO> getAll() {
        return customerRepository.findAll().stream()
                .map(customerMapper::toDto)
                .collect(Collectors.toList());
    }

    public CustomerResponseDTO getById(Long id) {
        return customerRepository.findById(id)
                .map(customerMapper::toDto)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
    }

    public CustomerResponseDTO create(CustomerRequestDTO dto) {
        Customer customer = customerMapper.toEntity(dto);
        customer.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        customer.setCreatedAt(LocalDateTime.now());
        return customerMapper.toDto(customerRepository.save(customer));
    }

    public CustomerResponseDTO update(Long id, CustomerRequestDTO dto) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        customer.setEmail(dto.getEmail());
        customer.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        customer.setFirstName(dto.getFirstName());
        customer.setLastName(dto.getLastName());
        customer.setPhoneNumber(dto.getPhoneNumber());

        return customerMapper.toDto(customerRepository.save(customer));
    }

    public void delete(Long id) {
        customerRepository.deleteById(id);
    }
}

