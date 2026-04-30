package com.example.kintai.controller;

import com.example.kintai.dto.AttendanceImportRequest;
import com.example.kintai.dto.AttendanceRecordDto;
import com.example.kintai.dto.WorkMemoRequest;
import com.example.kintai.entity.Employee;
import com.example.kintai.service.AttendanceService;
import com.example.kintai.util.EmployeeResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final EmployeeResolver employeeResolver;

    @GetMapping
    public ResponseEntity<List<AttendanceRecordDto>> getList(@RequestParam String month) {
        Employee employee = employeeResolver.resolve();
        return ResponseEntity.ok(attendanceService.getList(employee, YearMonth.parse(month)));
    }

    @PostMapping("/clock-in")
    public ResponseEntity<?> clockIn() {
        attendanceService.clockIn(employeeResolver.resolve());
        return ResponseEntity.ok(Map.of("message", "出勤打刻が完了しました"));
    }

    @PostMapping("/clock-out")
    public ResponseEntity<?> clockOut() {
        attendanceService.clockOut(employeeResolver.resolve());
        return ResponseEntity.ok(Map.of("message", "退勤打刻が完了しました"));
    }

    @PostMapping("/go-out")
    public ResponseEntity<?> goOut() {
        attendanceService.goOut(employeeResolver.resolve());
        return ResponseEntity.ok(Map.of("message", "外出打刻が完了しました"));
    }

    @PostMapping("/go-out-return")
    public ResponseEntity<?> goOutReturn() {
        attendanceService.goOutReturn(employeeResolver.resolve());
        return ResponseEntity.ok(Map.of("message", "外出戻り打刻が完了しました"));
    }

    @PostMapping("/work-memo")
    public ResponseEntity<?> saveMemo(@RequestBody WorkMemoRequest request) {
        attendanceService.saveMemo(
                employeeResolver.resolve(),
                request.getWork_date(),
                request.getMemo());
        return ResponseEntity.ok(Map.of("message", "保存しました"));
    }

    @PostMapping("/submit-csv")
    public ResponseEntity<?> submitCsv(@RequestBody AttendanceImportRequest req) {
        int count = attendanceService.submitCsv(
                employeeResolver.resolve(),
                req.getFileName(),
                req.getCsvContent());
        return ResponseEntity.ok(Map.of("message", "管理者に送信しました", "count", count));
    }

    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importAttendance(@RequestBody AttendanceImportRequest req) {
        return ResponseEntity.ok(
                attendanceService.importLegacy(employeeResolver.resolve(), req.getRecords()));
    }
}
