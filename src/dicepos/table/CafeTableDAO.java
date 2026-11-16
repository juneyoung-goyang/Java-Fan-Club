package dicepos.table;

import dicepos.common.DBUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** cafe_table(카페 테이블) 테이블 (⑤ 서윤채) */
public class CafeTableDAO {

    public List<CafeTableDTO> findAll() throws SQLException {
        List<CafeTableDTO> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM cafe_table ORDER BY table_no");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(toDTO(rs));
        }
        return list;
    }

    /** 사용가능한 룸만 (키오스크 설치할 때 고르는 목록) */
    public List<CafeTableDTO> findActive() throws SQLException {
        List<CafeTableDTO> list = new ArrayList<>();
        for (CafeTableDTO t : findAll()) {
            if (t.isActive()) list.add(t);
        }
        return list;
    }

    public CafeTableDTO findByNo(int tableNo) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM cafe_table WHERE table_no = ?")) {
            ps.setInt(1, tableNo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? toDTO(rs) : null;
            }
        }
    }

    /** 번호 또는 이름이 이미 있는지 (수정할 때는 자기 번호 제외) */
    public boolean isDuplicate(int tableNo, String tableName, int exceptNo) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT COUNT(*) FROM cafe_table WHERE (table_no = ? OR table_name = ?) AND table_no <> ?")) {
            ps.setInt(1, tableNo);
            ps.setString(2, tableName);
            ps.setInt(3, exceptNo);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    public void insert(CafeTableDTO t) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO cafe_table (table_no, table_name, capacity, is_active) VALUES (?, ?, ?, ?)")) {
            ps.setInt(1, t.getTableNo());
            ps.setString(2, t.getTableName());
            ps.setInt(3, t.getCapacity());
            ps.setBoolean(4, t.isActive());
            ps.executeUpdate();
        }
    }

    /** oldNo 룸을 t의 내용으로 바꾼다 (이용 기록이 있는 룸은 번호를 바꿀 수 없다) */
    public void update(int oldNo, CafeTableDTO t) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE cafe_table SET table_no = ?, table_name = ?, capacity = ? WHERE table_no = ?")) {
            ps.setInt(1, t.getTableNo());
            ps.setString(2, t.getTableName());
            ps.setInt(3, t.getCapacity());
            ps.setInt(4, oldNo);
            ps.executeUpdate();
        }
    }

    public void setActive(int tableNo, boolean active) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE cafe_table SET is_active = ? WHERE table_no = ?")) {
            ps.setBoolean(1, active);
            ps.setInt(2, tableNo);
            ps.executeUpdate();
        }
    }

    public void delete(int tableNo) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM cafe_table WHERE table_no = ?")) {
            ps.setInt(1, tableNo);
            ps.executeUpdate();
        }
    }

    private CafeTableDTO toDTO(ResultSet rs) throws SQLException {
        return new CafeTableDTO(rs.getInt("table_no"), rs.getString("table_name"), rs.getInt("capacity"),
                rs.getBoolean("is_active"));
    }
}
