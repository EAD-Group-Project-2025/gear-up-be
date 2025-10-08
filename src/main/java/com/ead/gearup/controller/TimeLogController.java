package com.ead.gearup.controller;

import com.ead.gearup.dto.timelog.*;
import com.ead.gearup.service.TimeLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/timelogs")
public class TimeLogController {

    private final TimeLogService timeLogService;

    public TimeLogController(TimeLogService timeLogService) {
        this.timeLogService = timeLogService;
    }

    @PostMapping
    public ResponseEntity<TimeLogResponseDTO> createTimeLog(@RequestBody CreateTimeLogDTO dto) {
        return ResponseEntity.ok(timeLogService.createTimeLog(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TimeLogResponseDTO> getTimeLogById(@PathVariable Long id) {
        return ResponseEntity.ok(timeLogService.getTimeLogById(id));
    }

    @GetMapping
    public ResponseEntity<List<TimeLogResponseDTO>> getAllTimeLogs() {
        return ResponseEntity.ok(timeLogService.getAllTimeLogs());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TimeLogResponseDTO> updateTimeLog(
            @PathVariable Long id, @RequestBody UpdateTimeLogDTO dto) {
        return ResponseEntity.ok(timeLogService.updateTimeLog(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTimeLog(@PathVariable Long id) {
        timeLogService.deleteTimeLog(id);
        return ResponseEntity.ok("TimeLog deleted successfully with id " + id);
    }
}
