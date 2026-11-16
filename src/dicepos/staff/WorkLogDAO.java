package dicepos.staff;

import dicepos.common.DBUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** work_log(근무 기록) 테이블 */
public class WorkLogDAO {

    /**
     * 실근무 시간(분) = 퇴근 − 출근 − 휴게.
     * 휴게시간은 근로기준법 기준으로 4시간 이상 30분, 8시간 이상 1시간을 뺀다.
     */
    public static int workMinutes(LocalDateTime clockIn, LocalDateTime clockOut) {
        int minutes = (int) Duration.between(clockIn, clockOut).toMinutes();
        if (minutes >= 8 * 60) return minutes - 60;
        if (minutes >= 4 * 60) return minutes - 30;
        return minutes;
    }

    /** 아직 퇴근하지 않은 기록 (없으면 null) */
    public WorkLogDTO findOpen(int staffId) throws SQLException {
        String sql = "SELECT * FROM work_log WHERE staff_id = ? AND clock_out IS NULL ORDER BY log_id DESC LIMIT 1";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, staffId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? toDTO(rs) : null;
            }
        }
    }

    /** 출근: 오늘 날짜와 지금 시각으로 기록을 만든다. */
    public void clockIn(int staffId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO work_log (staff_id, work_date, clock_in) VALUES (?, CURDATE(), NOW())")) {
            ps.setInt(1, staffId);
            ps.executeUpdate();
        }
    }

    /** 퇴근: 퇴근 시각과 실근무 시간을 적는다. 실근무 시간(분)을 돌려준다. */
    public int clockOut(WorkLogDTO log) throws SQLException {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        int minutes = workMinutes(log.getClockIn(), now);
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE work_log SET clock_out = ?, work_minutes = ? WHERE log_id = ?")) {
            ps.setTimestamp(1, Timestamp.valueOf(now));
            ps.setInt(2, minutes);
            ps.setInt(3, log.getLogId());
            ps.executeUpdate();
        }
        return minutes;
    }

    /** 한 직원의 한 달 기록. month는 "2026-11" */
    public List<WorkLogDTO> findByMonth(int staffId, String month) throws SQLException {
        String sql = "SELECT * FROM work_log WHERE staff_id = ? AND DATE_FORMAT(work_date, '%Y-%m') = ? ORDER BY work_date, clock_in";
        List<WorkLogDTO> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, staffId);
            ps.setString(2, month);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(toDTO(rs));
            }
        }
        return list;
    }

    /** 관리자가 출근·퇴근 시각을 고친다. 사유는 note에 남긴다. */
    public void updateTime(int logId, LocalDateTime clockIn, LocalDateTime clockOut, String note) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE work_log SET work_date = ?, clock_in = ?, clock_out = ?, work_minutes = ?, note = ? WHERE log_id = ?")) {
            ps.setObject(1, clockIn.toLocalDate());
            ps.setTimestamp(2, Timestamp.valueOf(clockIn));
            ps.setTimestamp(3, Timestamp.valueOf(clockOut));
            ps.setInt(4, workMinutes(clockIn, clockOut));
            ps.setString(5, note);
            ps.setInt(6, logId);
            ps.executeUpdate();
        }
    }

    public void delete(int logId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM work_log WHERE log_id = ?")) {
            ps.setInt(1, logId);
            ps.executeUpdate();
        }
    }

    private WorkLogDTO toDTO(ResultSet rs) throws SQLException {
        return new WorkLogDTO(rs.getInt("log_id"), rs.getInt("staff_id"), rs.getObject("work_date", LocalDate.class),
                rs.getObject("clock_in", LocalDateTime.class), rs.getObject("clock_out", LocalDateTime.class),
                rs.getObject("work_minutes", Integer.class), rs.getString("note"));
    }
}
