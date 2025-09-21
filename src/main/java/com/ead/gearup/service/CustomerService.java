package com.ead.gearup.service;

import com.ead.gearup.dto.customer.CustomerRequestDTO;
import com.ead.gearup.dto.customer.CustomerResponseDTO;
import com.ead.gearup.model.Customer;
import com.ead.gearup.model.User;
import com.ead.gearup.enums.UserRole;
import com.ead.gearup.exception.CustomerNotFoundException;
import com.ead.gearup.exception.UnauthorizedCustomerAccessException;
import com.ead.gearup.repository.CustomerRepository;
import com.ead.gearup.repository.UserRepository;
import com.ead.gearup.service.auth.CurrentUserService;
import com.ead.gearup.util.CustomerMapper;
import com.ead.gearup.validation.RequiresRole;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final CustomerMapper customerMapper;
    private final CurrentUserService currentUserService;

    public List<CustomerResponseDTO> getAll() {
        return customerRepository.findAll().stream()
                .map(customerMapper::toDto)
                .collect(Collectors.toList());
    }

    public CustomerResponseDTO getById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid customer ID");
        }

        return customerRepository.findById(id)
                .map(customerMapper::toDto)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + id));
    }

    @Transactional
    public CustomerResponseDTO create(@Valid CustomerRequestDTO dto) {
        User user = currentUserService.getCurrentUser();

        if (user == null) {
            throw new IllegalStateException("No authenticated user found");
        }

        if (user.getRole() != UserRole.CUSTOMER) {
            throw new UnauthorizedCustomerAccessException("Only customers can create customer profiles");
        }

        user.setRole(UserRole.CUSTOMER);
        userRepository.save(user);

        Customer customer = customerMapper.toEntity(dto);
        if (customer == null) {
            throw new IllegalStateException("Failed to map customer request");
        }

        customer.setUser(user);

        return customerMapper.toDto(customerRepository.save(customer));
    }

    @Transactional
    @RequiresRole({ UserRole.CUSTOMER })
    public CustomerResponseDTO update(Long id, @Valid CustomerRequestDTO dto) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid customer ID");
        }

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + id));

        customer.setPhoneNumber(dto.getPhoneNumber());

        return customerMapper.toDto(customerRepository.save(customer));
    }

    @Transactional
    @RequiresRole({ UserRole.CUSTOMER })
    public void delete(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid customer ID");
        }

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + id));

        customerRepository.delete(customer);
    }
}
