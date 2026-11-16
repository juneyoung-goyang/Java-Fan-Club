package dicepos.staff;

import dicepos.common.DBUtil;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** staff(직원) 테이블 */
public class StaffDAO {

    /** 고용 형태("전체"면 모두)와 검색어(이름·아이디)로 직원 목록 */
    public List<StaffDTO> find(String empType, String keyword) throws SQLException {
        String sql = "SELECT * FROM staff WHERE (? = '전체' OR emp_type = ?) AND (name LIKE ? OR login_id LIKE ?)"
                   + " ORDER BY status DESC, staff_id";   // '재직'이 '퇴사'보다 먼저
        List<StaffDTO> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, empType);
            ps.setString(2, empType);
            ps.setString(3, "%" + keyword + "%");
            ps.setString(4, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(toDTO(rs));
            }
        }
        return list;
    }

    /** 재직 중인 직원 목록 (근무·급여 관리의 직원 콤보박스) */
    public List<StaffDTO> findWorking() throws SQLException {
        List<StaffDTO> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM staff WHERE status = '재직' ORDER BY staff_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(toDTO(rs));
        }
        return list;
    }

    public StaffDTO findByLoginId(String loginId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM staff WHERE login_id = ?")) {
            ps.setString(1, loginId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? toDTO(rs) : null;
            }
        }
    }

    public StaffDTO findById(int staffId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM staff WHERE staff_id = ?")) {
            ps.setInt(1, staffId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? toDTO(rs) : null;
            }
        }
    }

    /** 다른 직원이 이미 쓰는 아이디인지 (변경할 때는 자기 자신 제외) */
    public boolean isLoginIdTaken(String loginId, int exceptStaffId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM staff WHERE login_id = ? AND staff_id <> ?")) {
            ps.setString(1, loginId);
            ps.setInt(2, exceptStaffId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    public void insert(StaffDTO s) throws SQLException {
        String sql = "INSERT INTO staff (login_id, password, salt, name, phone1, phone2, phone3, role, emp_type,"
                   + " hourly_wage, monthly_pay, hire_date, contract_end, status, address, work_time, memo)"
                   + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '재직', ?, ?, ?)";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, s.getLoginId());
            ps.setString(2, s.getPassword());
            ps.setString(3, s.getSalt());
            setCommon(ps, 4, s);
            ps.setString(14, s.getAddress());
            ps.setString(15, s.getWorkTime());
            ps.setString(16, s.getMemo());
            ps.executeUpdate();
        }
    }

    /** 정보 변경. 비밀번호가 null이면 기존 비밀번호를 그대로 둔다. */
    public void update(StaffDTO s) throws SQLException {
        boolean newPassword = s.getPassword() != null;
        String sql = "UPDATE staff SET login_id = ?, name = ?, phone1 = ?, phone2 = ?, phone3 = ?, role = ?, emp_type = ?,"
                   + " hourly_wage = ?, monthly_pay = ?, hire_date = ?, contract_end = ?, status = ?, address = ?, work_time = ?"
                   + (newPassword ? ", password = ?, salt = ?" : "")
                   + " WHERE staff_id = ?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, s.getLoginId());
            setCommon(ps, 2, s);
            ps.setString(12, s.getStatus());
            ps.setString(13, s.getAddress());
            ps.setString(14, s.getWorkTime());
            int i = 15;
            if (newPassword) {
                ps.setString(i++, s.getPassword());
                ps.setString(i++, s.getSalt());
            }
            ps.setInt(i, s.getStaffId());
            ps.executeUpdate();
        }
    }

    /** name ~ contract_end (10칸)을 start 번째 물음표부터 채운다 */
    private void setCommon(PreparedStatement ps, int start, StaffDTO s) throws SQLException {
        ps.setString(start, s.getName());
        ps.setString(start + 1, s.getPhone1());
        ps.setString(start + 2, s.getPhone2());
        ps.setString(start + 3, s.getPhone3());
        ps.setString(start + 4, s.getRole());
        ps.setString(start + 5, s.getEmpType());
        setInteger(ps, start + 6, s.getHourlyWage());
        setInteger(ps, start + 7, s.getMonthlyPay());
        ps.setDate(start + 8, Date.valueOf(s.getHireDate()));
        setDate(ps, start + 9, s.getContractEnd());
    }

    /** 퇴사 처리: 기록을 남기기 위해 지우지 않고 상태만 바꾼다. */
    public void retire(int staffId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE staff SET status = '퇴사' WHERE staff_id = ?")) {
            ps.setInt(1, staffId);
            ps.executeUpdate();
        }
    }

    public void updateMemo(int staffId, String memo) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE staff SET memo = ? WHERE staff_id = ?")) {
            ps.setString(1, memo);
            ps.setInt(2, staffId);
            ps.executeUpdate();
        }
    }

    private static void setInteger(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) ps.setNull(index, Types.INTEGER);
        else ps.setInt(index, value);
    }

    private static void setDate(PreparedStatement ps, int index, LocalDate value) throws SQLException {
        if (value == null) ps.setNull(index, Types.DATE);
        else ps.setDate(index, Date.valueOf(value));
    }

    private StaffDTO toDTO(ResultSet rs) throws SQLException {
        return new StaffDTO(rs.getInt("staff_id"), rs.getString("login_id"), rs.getString("password"), rs.getString("salt"),
                rs.getString("name"), rs.getString("phone1"), rs.getString("phone2"), rs.getString("phone3"),
                rs.getString("role"), rs.getString("emp_type"),
                rs.getObject("hourly_wage", Integer.class), rs.getObject("monthly_pay", Integer.class),
                rs.getObject("hire_date", LocalDate.class), rs.getObject("contract_end", LocalDate.class),
                rs.getString("status"), rs.getString("address"), rs.getString("work_time"), rs.getString("memo"));
    }
}
