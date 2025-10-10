package com.ead.gearup.service;

import com.ead.gearup.dto.customer.*;
import com.ead.gearup.dto.vehicle.VehicleCreateDTO;
import com.ead.gearup.dto.vehicle.VehicleResponseDTO;
import com.ead.gearup.enums.AppointmentStatus;
import com.ead.gearup.exception.CustomerNotFoundException;
import com.ead.gearup.exception.UnauthorizedCustomerAccessException;
import com.ead.gearup.model.Customer;
import com.ead.gearup.model.User;
import com.ead.gearup.enums.UserRole;
import com.ead.gearup.model.Vehicle;
import com.ead.gearup.repository.CustomerRepository;
import com.ead.gearup.repository.UserRepository;
import com.ead.gearup.service.auth.CurrentUserService;
import com.ead.gearup.util.CustomerMapper;
import com.ead.gearup.model.Appointment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
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
    public CustomerResponseDTO create(CustomerRequestDTO dto) {
        User currentUser = currentUserService.getCurrentUser();

        if (currentUser == null) {
            throw new IllegalStateException("No authenticated user found");
        }

        if (currentUser.getRole() != UserRole.PUBLIC) {
            throw new UnauthorizedCustomerAccessException(
                    "Only public users can create a customer profile");
        }

        currentUser.setRole(UserRole.CUSTOMER);
        userRepository.save(currentUser);

        Customer customer = customerMapper.toEntity(dto);
        if (customer == null) {
            throw new IllegalStateException("Failed to map customer request");
        }

        customer.setUser(currentUser);

        return customerMapper.toDto(customerRepository.save(customer));
    }

    @Transactional
    public CustomerResponseDTO update(Long id, @Valid CustomerUpdateDTO dto) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid customer ID");
        }

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + id));

        if (dto.getName() != null) {
            User currentUser = currentUserService.getCurrentUser();
            currentUser.setName(dto.getName());
            userRepository.save(currentUser);
        }

        if (dto.getPhoneNumber() != null) {
            customer.setPhoneNumber(dto.getPhoneNumber());
        }

        return customerMapper.toDto(customerRepository.save(customer));
    }

    @Transactional
    public void delete(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid customer ID");
        }

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + id));

        // Handle linked User
        User linkedUser = customer.getUser();
        if (linkedUser != null) {
            linkedUser.setRole(UserRole.PUBLIC);
            customer.setUser(null);
            userRepository.save(linkedUser);
        }

        customerRepository.delete(customer);
    }

    @Transactional(readOnly = true)
    public CustomerHeaderDTO getHeaderInfo(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + id));

        return CustomerHeaderDTO.builder()
                .name(customer.getUser().getName())
                .profileImage(customer.getProfileImage())
                .build();
    }

//    @Transactional(readOnly = true)
//    public List<NotificationDTO> getNotifications(Long id) {
//        Customer customer = customerRepository.findById(id)
//                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + id));
//
//        List<Notification> notifications = notificationRepository.findByCustomerOrderByCreatedAtDesc(customer);
//
//        return notifications.stream()
//                .map(n -> NotificationDTO.builder()
//                        .id(n.getId())
//                        .message(n.getMessage())
//                        .type(n.getType())
//                        .time(formatTimeAgo(n.getCreatedAt()))
//                        .build())
//                .collect(Collectors.toList());
//    }

    // helper
    private String formatTimeAgo(LocalDateTime dateTime) {
        Duration duration = Duration.between(dateTime, LocalDateTime.now());
        if (duration.toMinutes() < 60) return duration.toMinutes() + " minutes ago";
        else if (duration.toHours() < 24) return duration.toHours() + " hours ago";
        else return duration.toDays() + " days ago";
    }


    // Customer Dashboard
    @Transactional(readOnly = true)
    public CustomerDashboardDTO getDashboard(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + id));

        // --- Profile ---
        CustomerProfileDTO profile = CustomerProfileDTO.builder()
                .name(customer.getUser().getName())
                .email(customer.getUser().getEmail())
                .profileImage(customer.getProfileImage())
                .build();

        LocalDate today = LocalDate.now();

        // --- Summary counts ---
        long upcomingAppointmentsCount = customer.getAppointments().stream()
                .map(Appointment::getDate)
                .filter(d -> d.isAfter(today))
                .count();

        String nextAppointmentDate = customer.getAppointments().stream()
                .map(Appointment::getDate)
                .filter(d -> d.isAfter(today))
                .min(Comparator.naturalOrder())
                .map(LocalDate::toString)
                .orElse("No upcoming appointments");

        long ongoingProjectsCount = customer.getAppointments().stream()
                .filter(a -> a.getStatus() == AppointmentStatus.PENDING) // or IN_PROGRESS depending on your enum
                .count();

        String ongoingProjectStatus = ongoingProjectsCount > 0 ? "In Progress" : "None";

        long completedServicesCount = customer.getAppointments().stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .count();

        long pendingRequestsCount = customer.getAppointments().stream()
                .filter(a -> a.getStatus() == AppointmentStatus.PENDING)
                .count();

        CustomerSummaryDTO summary = CustomerSummaryDTO.builder()
                .upcomingAppointmentsCount(upcomingAppointmentsCount)
                .nextAppointmentDate(nextAppointmentDate)
                .ongoingProjectsCount(ongoingProjectsCount)
                .ongoingProjectStatus(ongoingProjectStatus)
                .completedServicesCount(completedServicesCount)
                .pendingRequestsCount(pendingRequestsCount)
                .build();

        // --- Recent Activities (mock for now) ---
        List<CustomerActivityDTO> activities = List.of(
                CustomerActivityDTO.builder()
                        .id(1L)
                        .action("Appointment Booked")
                        .description("Service appointment for Toyota Axio confirmed.")
                        .time(formatTimeAgo(LocalDateTime.now().minusHours(4)))
                        .icon("calendar")
                        .build(),
                CustomerActivityDTO.builder()
                        .id(2L)
                        .action("Payment Completed")
                        .description("Payment for last service has been processed.")
                        .time(formatTimeAgo(LocalDateTime.now().minusDays(2)))
                        .icon("credit-card")
                        .build()
        );

        // --- Vehicles list ---
        List<CustomerVehicleDTO> vehicles = customer.getVehicles().stream()
                .map(v -> CustomerVehicleDTO.builder()
                        .id(v.getVehicleId())
                        .make(v.getMake())
                        .model(v.getModel())
                        .year(v.getYear())
                        .licensePlate(v.getLicensePlate())
                        .nextService("2025-11-10") // mock for now
                        .build())
                .collect(Collectors.toList());

        // --- Combine all ---
        return CustomerDashboardDTO.builder()
                .profile(profile)
                .summary(summary)
                .recentActivities(activities)
                .vehicles(vehicles)
                .build();
    }


}
