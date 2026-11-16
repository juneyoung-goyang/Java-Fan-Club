package dicepos.staff;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** work_log (근무 기록) 한 줄을 담는 그릇 */
public class WorkLogDTO {
    private int logId;
    private int staffId;
    private LocalDate workDate;
    private LocalDateTime clockIn;
    private LocalDateTime clockOut;
    private Integer workMinutes;
    private String note;

    public WorkLogDTO(int logId, int staffId, LocalDate workDate, LocalDateTime clockIn, LocalDateTime clockOut, Integer workMinutes, String note) {
        this.logId = logId;
        this.staffId = staffId;
        this.workDate = workDate;
        this.clockIn = clockIn;
        this.clockOut = clockOut;
        this.workMinutes = workMinutes;
        this.note = note;
    }

    public int getLogId() {
        return logId;
    }

    public int getStaffId() {
        return staffId;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public LocalDateTime getClockIn() {
        return clockIn;
    }

    public LocalDateTime getClockOut() {
        return clockOut;
    }

    public Integer getWorkMinutes() {
        return workMinutes;
    }

    public String getNote() {
        return note;
    }
}
