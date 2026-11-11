package dicepos.table;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/** ※ 임시 파일 (⑤ 서윤채 것). ⑤가 진짜를 만들면 지운다. ② 결제가 쓰는 메서드만 들어 있다. */
public class TableUsageDAO {
    /** ② 결제 트랜잭션 안에서 부른다. 이용 상태를 결제완료로 바꾸고 퇴장 시각을 적는다. */
    public void finish(Connection con, int usageId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE table_usage SET status = '결제완료', end_time = NOW() WHERE usage_id = ? AND status = '이용중'")) {
            ps.setInt(1, usageId);
            if (ps.executeUpdate() != 1) throw new SQLException("이미 결제된 이용 건입니다.");
        }
    }
}
