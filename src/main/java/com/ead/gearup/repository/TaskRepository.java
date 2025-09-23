package com.ead.gearup.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ead.gearup.model.Task;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

}
