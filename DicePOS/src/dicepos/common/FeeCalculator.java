package dicepos.common;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 이용 요금 계산 (⑤ 서윤채). ② 결제, ⑤ 테이블 현황, ⑥ 이용 현황이 같이 쓴다.
 * 적용 시작일이 오늘까지인 요금 중 가장 최근 것으로 계산한다.
 * 1인 요금 = 기본요금 + 올림(기본 시간을 넘은 분 ÷ 추가 단위) × 추가요금
 * 예) 기본 60분 3,500원, 10분마다 500원, 130분 → 3,500 + 7 × 500 = 7,000원
 */
public class FeeCalculator {

    /** 1인 요금 */
    public static int perPerson(int minutes) throws SQLException {
        String sql = "SELECT base_minutes, base_price, extra_unit, extra_price FROM fee_setting"
                   + " WHERE apply_date <= CURDATE() ORDER BY apply_date DESC, fee_id DESC LIMIT 1";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new SQLException("적용 중인 이용 요금이 없습니다. 관리자 화면의 이용 요금 설정을 확인하세요.");
            int baseMinutes = rs.getInt("base_minutes");
            int basePrice = rs.getInt("base_price");
            int extraUnit = rs.getInt("extra_unit");
            int extraPrice = rs.getInt("extra_price");

            int over = Math.max(0, minutes - baseMinutes);
            int units = (over + extraUnit - 1) / extraUnit;   // 올림
            return basePrice + units * extraPrice;
        }
    }

    /** 이용료 = 1인 요금 × 인원 */
    public static int total(int minutes, int people) throws SQLException {
        return perPerson(minutes) * people;
    }
}
