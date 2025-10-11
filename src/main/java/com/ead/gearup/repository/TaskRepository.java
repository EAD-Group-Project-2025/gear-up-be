package com.ead.gearup.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ead.gearup.enums.TaskStatus;
import com.ead.gearup.model.Employee;
import com.ead.gearup.model.Task;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("SELECT COUNT(t) FROM Task t WHERE t.employee = :employee AND t.status = 'COMPLETED' AND FUNCTION('DATE', t.completedAt) = CURRENT_DATE")
    Long countCompletedToday(@Param("employee") Employee employee);

    Long countByEmployeeAndStatus(Employee employee, TaskStatus status);
}

