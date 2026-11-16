package dicepos.table;

import dicepos.common.DBUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** fee_setting(이용 요금 설정) 테이블 (⑤ 서윤채). 요금 계산은 dicepos.common.FeeCalculator가 한다. */
public class FeeSettingDAO {

    public List<FeeSettingDTO> findAll() throws SQLException {
        List<FeeSettingDTO> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM fee_setting ORDER BY apply_date DESC, fee_id DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new FeeSettingDTO(rs.getInt("fee_id"), rs.getInt("base_minutes"), rs.getInt("base_price"),
                        rs.getInt("extra_unit"), rs.getInt("extra_price"), rs.getObject("apply_date", LocalDate.class)));
            }
        }
        return list;
    }

    public void insert(FeeSettingDTO f) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO fee_setting (base_minutes, base_price, extra_unit, extra_price, apply_date) VALUES (?, ?, ?, ?, ?)")) {
            fill(ps, f);
            ps.executeUpdate();
        }
    }

    public void update(FeeSettingDTO f) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE fee_setting SET base_minutes = ?, base_price = ?, extra_unit = ?, extra_price = ?, apply_date = ? WHERE fee_id = ?")) {
            fill(ps, f);
            ps.setInt(6, f.getFeeId());
            ps.executeUpdate();
        }
    }

    private void fill(PreparedStatement ps, FeeSettingDTO f) throws SQLException {
        ps.setInt(1, f.getBaseMinutes());
        ps.setInt(2, f.getBasePrice());
        ps.setInt(3, f.getExtraUnit());
        ps.setInt(4, f.getExtraPrice());
        ps.setObject(5, f.getApplyDate());
    }

    public void delete(int feeId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM fee_setting WHERE fee_id = ?")) {
            ps.setInt(1, feeId);
            ps.executeUpdate();
        }
    }
}
