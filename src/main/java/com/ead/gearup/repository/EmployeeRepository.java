package com.ead.gearup.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.ead.gearup.model.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
}
