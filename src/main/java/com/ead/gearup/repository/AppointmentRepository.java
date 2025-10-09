package com.ead.gearup.repository;

import com.ead.gearup.model.Appointment;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("SELECT a FROM Appointment a " +
            "JOIN FETCH a.customer c " +
            "JOIN FETCH c.user u " +
            "WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Appointment> findByCustomerUserName(@Param("name") String name);

}
