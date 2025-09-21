package com.ead.gearup.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ead.gearup.model.Employee;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
}
