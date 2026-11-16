package dicepos.staff;

import dicepos.common.DBUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/** salary(급여) 테이블 */
public class SalaryDAO {

    /** 그 직원의 그 달 확정 급여 (없으면 null) */
    public SalaryDTO find(int staffId, String month) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM salary WHERE staff_id = ? AND pay_month = ?")) {
            ps.setInt(1, staffId);
            ps.setString(2, month);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new SalaryDTO(rs.getInt("salary_id"), rs.getInt("staff_id"), rs.getString("pay_month"),
                        rs.getInt("work_minutes"), rs.getInt("base_amount"), rs.getInt("extra_amount"),
                        rs.getInt("total_pay"), rs.getString("status"), rs.getObject("pay_date", LocalDate.class));
            }
        }
    }

    /** 급여 확정. 같은 직원·같은 달은 UNIQUE라서 두 번 넣으면 SQLException이 난다. */
    public void insert(int staffId, String month, int workMinutes, int baseAmount, int extraAmount) throws SQLException {
        String sql = "INSERT INTO salary (staff_id, pay_month, work_minutes, base_amount, extra_amount, total_pay, status)"
                   + " VALUES (?, ?, ?, ?, ?, ?, '미지급')";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, staffId);
            ps.setString(2, month);
            ps.setInt(3, workMinutes);
            ps.setInt(4, baseAmount);
            ps.setInt(5, extraAmount);
            ps.setInt(6, baseAmount + extraAmount);
            ps.executeUpdate();
        }
    }

    public void markPaid(int salaryId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE salary SET status = '지급완료', pay_date = CURDATE() WHERE salary_id = ?")) {
            ps.setInt(1, salaryId);
            ps.executeUpdate();
        }
    }
}
