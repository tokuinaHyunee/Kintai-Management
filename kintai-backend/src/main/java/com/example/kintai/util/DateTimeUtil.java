package com.example.kintai.util;

import com.example.kintai.entity.WorkTime;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 日時パース・労働時間計算・集計の共通ユーティリティ。
 * AttendanceController / ImportController / SummaryController / AdminController の重複を集約。
 */
public final class DateTimeUtil {

    private DateTimeUtil() {}

    private static final DateTimeFormatter[] DATE_FORMATS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ofPattern("yyyy/M/d"),
        DateTimeFormatter.ofPattern("yyyyMMdd"),
    };

    private static final DateTimeFormatter[] TIME_FORMATS = {
        DateTimeFormatter.ofPattern("HH:mm"),
        DateTimeFormatter.ofPattern("HH:mm:ss"),
        DateTimeFormatter.ofPattern("H:mm"),
    };

    public static LocalDate parseDate(String s) {
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try { return LocalDate.parse(s, fmt); } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    public static LocalTime parseTime(String s) {
        for (DateTimeFormatter fmt : TIME_FORMATS) {
            try { return LocalTime.parse(s, fmt); } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    /**
     * 労働時間・残業時間を計算して int[3] = {breakMin, workMin, overtimeMin} を返す。
     * breakOverride が null の場合: 6時間超なら60分休憩、それ以外は0分。
     */
    public static int[] calcWorkTime(LocalTime start, LocalTime end, Integer breakOverride) {
        int total = (int) Duration.between(start, end).toMinutes();
        if (total < 0) total += 24 * 60; // 日跨ぎ対応
        int breakMin = breakOverride != null ? breakOverride : (total > 360 ? 60 : 0);
        int workMin  = Math.max(0, total - breakMin);
        int overtime = Math.max(0, workMin - 480);
        return new int[]{ breakMin, workMin, overtime };
    }

    // ----- WorkTime リスト集計ヘルパー -----

    public static int countWorkDays(List<WorkTime> records) {
        return (int) records.stream().filter(r -> r.getWorkMinutes() != null).count();
    }

    public static int sumWorkMinutes(List<WorkTime> records) {
        return records.stream()
                .filter(r -> r.getWorkMinutes() != null)
                .mapToInt(WorkTime::getWorkMinutes).sum();
    }

    public static int sumOvertimeMinutes(List<WorkTime> records) {
        return records.stream()
                .filter(r -> r.getOvertimeMinutes() != null)
                .mapToInt(WorkTime::getOvertimeMinutes).sum();
    }

    /** 分 → 時間（小数点1位まで）*/
    public static double toHours(int minutes) {
        return Math.round(minutes / 60.0 * 10) / 10.0;
    }
}
