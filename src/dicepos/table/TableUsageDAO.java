package dicepos.table;

import dicepos.common.DBUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** table_usage(테이블 이용) 테이블 (⑤ 서윤채) */
public class TableUsageDAO {

    /** 지금까지 이용한 시간(분). 결제가 끝났으면 퇴장 시각까지 */
    public static int minutesUsed(TableUsageDTO usage) {
        LocalDateTime end = usage.getEndTime() == null ? LocalDateTime.now() : usage.getEndTime();
        return (int) Math.max(0, Duration.between(usage.getStartTime(), end).toMinutes());
    }

    /** 이용 중인 모든 테이블 */
    public List<TableUsageDTO> findActive() throws SQLException {
        List<TableUsageDTO> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM table_usage WHERE status = '이용중' ORDER BY table_no");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(toDTO(rs));
        }
        return list;
    }

    /** 이 테이블이 지금 이용 중이면 그 이용 건, 아니면 null (키오스크도 쓴다) */
    public TableUsageDTO findActiveByTable(int tableNo) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT * FROM table_usage WHERE table_no = ? AND status = '이용중' ORDER BY usage_id DESC LIMIT 1")) {
            ps.setInt(1, tableNo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? toDTO(rs) : null;
            }
        }
    }

    public TableUsageDTO findById(int usageId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM table_usage WHERE usage_id = ?")) {
            ps.setInt(1, usageId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? toDTO(rs) : null;
            }
        }
    }

    /** 이용 시작 (입장 시각 = 지금, 상태 '이용중') */
    public void start(int tableNo, Integer memberId, int people) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO table_usage (table_no, member_id, people, start_time, status) VALUES (?, ?, ?, NOW(), '이용중')")) {
            ps.setInt(1, tableNo);
            if (memberId == null) ps.setNull(2, Types.INTEGER);
            else ps.setInt(2, memberId);
            ps.setInt(3, people);
            ps.executeUpdate();
        }
    }

    /** ② 결제 트랜잭션 안에서 부른다. 이용 상태를 결제완료로 바꾸고 퇴장 시각을 적는다. */
    public void finish(Connection con, int usageId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE table_usage SET status = '결제완료', end_time = NOW() WHERE usage_id = ? AND status = '이용중'")) {
            ps.setInt(1, usageId);
            if (ps.executeUpdate() != 1) throw new SQLException("이미 결제된 이용 건입니다.");
        }
    }

    /** 이 룸의 이용 기록 수 (테이블 설정에서 삭제 가능 여부 판단) */
    public int countByTable(int tableNo) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM table_usage WHERE table_no = ?")) {
            ps.setInt(1, tableNo);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private TableUsageDTO toDTO(ResultSet rs) throws SQLException {
        return new TableUsageDTO(rs.getInt("usage_id"), rs.getInt("table_no"), rs.getObject("member_id", Integer.class),
                rs.getInt("people"), rs.getObject("start_time", LocalDateTime.class),
                rs.getObject("end_time", LocalDateTime.class), rs.getString("status"));
    }
}
