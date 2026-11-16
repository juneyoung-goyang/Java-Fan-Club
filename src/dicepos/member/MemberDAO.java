package dicepos.member;

import dicepos.common.DBUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** member(회원) 테이블 (② 송하안) */
public class MemberDAO {

    /** 이름에 검색어가 들어가거나 전화번호 뒤 4자리가 같은 회원. 검색어가 비면 전체 */
    public List<MemberDTO> search(String keyword) throws SQLException {
        String sql = "SELECT * FROM member WHERE name LIKE ? OR phone3 = ? ORDER BY member_id";
        List<MemberDTO> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            ps.setString(2, keyword);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(toDTO(rs));
            }
        }
        return list;
    }

    public MemberDTO findById(int memberId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM member WHERE member_id = ?")) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? toDTO(rs) : null;
            }
        }
    }

    /** 회원 번호 → 누적 결제 금액 (payment에서 SUM) */
    public Map<Integer, Integer> totalPaidMap() throws SQLException {
        Map<Integer, Integer> map = new HashMap<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT member_id, SUM(total_amount) FROM payment WHERE member_id IS NOT NULL GROUP BY member_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) map.put(rs.getInt(1), rs.getInt(2));
        }
        return map;
    }

    /** 결제 트랜잭션 안에서 누적 결제 금액을 다시 센다 (방금 넣은 결제 포함) */
    public int totalPaid(Connection con, int memberId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT COALESCE(SUM(total_amount), 0) FROM payment WHERE member_id = ?")) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** 결제 트랜잭션 안에서 포인트와 등급을 바꾼다 */
    public void updatePointAndGrade(Connection con, int memberId, int point, String grade) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE member SET point = ?, grade = ? WHERE member_id = ?")) {
            ps.setInt(1, point);
            ps.setString(2, grade);
            ps.setInt(3, memberId);
            ps.executeUpdate();
        }
    }

    /** 등록: 등급 '일반', 포인트 0, 가입일 오늘. 전화번호가 겹치면 SQLIntegrityConstraintViolationException */
    public void insert(String name, String p1, String p2, String p3, String gender) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO member (name, phone1, phone2, phone3, gender, join_date) VALUES (?, ?, ?, ?, ?, CURDATE())")) {
            ps.setString(1, name);
            ps.setString(2, p1);
            ps.setString(3, p2);
            ps.setString(4, p3);
            ps.setString(5, gender);
            ps.executeUpdate();
        }
    }

    /** 수정: 등급과 포인트는 결제할 때만 바뀌므로 여기서는 고치지 않는다 */
    public void update(int memberId, String name, String p1, String p2, String p3, String gender) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE member SET name = ?, phone1 = ?, phone2 = ?, phone3 = ?, gender = ? WHERE member_id = ?")) {
            ps.setString(1, name);
            ps.setString(2, p1);
            ps.setString(3, p2);
            ps.setString(4, p3);
            ps.setString(5, gender);
            ps.setInt(6, memberId);
            ps.executeUpdate();
        }
    }

    public boolean hasPayments(int memberId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM payment WHERE member_id = ?")) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    public void delete(int memberId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM member WHERE member_id = ?")) {
            ps.setInt(1, memberId);
            ps.executeUpdate();
        }
    }

    private MemberDTO toDTO(ResultSet rs) throws SQLException {
        return new MemberDTO(rs.getInt("member_id"), rs.getString("name"), rs.getString("phone1"), rs.getString("phone2"),
                rs.getString("phone3"), rs.getString("gender"), rs.getString("grade"), rs.getInt("point"),
                rs.getObject("join_date", LocalDate.class));
    }
}
