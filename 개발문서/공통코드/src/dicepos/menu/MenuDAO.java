package dicepos.menu;

import dicepos.common.DBUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** ※ 임시 파일 (④ 박준영 것). ④가 진짜를 만들면 지운다. ⑥ 주문이 쓰는 메서드만 들어 있다. */
public class MenuDAO {
    public List<MenuDTO> findByCategory(String category) throws SQLException {
        List<MenuDTO> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM menu WHERE category = ? ORDER BY name")) {
            ps.setString(1, category);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new MenuDTO(rs.getInt("menu_id"), rs.getString("name"), rs.getString("category"),
                            rs.getInt("price"), rs.getInt("stock"), rs.getBoolean("sold_out")));
                }
            }
        }
        return list;
    }

    /** ⑥ 주문 트랜잭션 안에서 부른다. 재고가 모자라면 SQLException */
    public void decreaseStock(Connection con, int menuId, int qty) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE menu SET stock = stock - ? WHERE menu_id = ? AND stock >= ?")) {
            ps.setInt(1, qty);
            ps.setInt(2, menuId);
            ps.setInt(3, qty);
            if (ps.executeUpdate() == 0) throw new SQLException("재고가 부족합니다. (메뉴 번호 " + menuId + ")");
        }
        try (PreparedStatement ps = con.prepareStatement("UPDATE menu SET sold_out = 1 WHERE menu_id = ? AND stock = 0")) {
            ps.setInt(1, menuId);
            ps.executeUpdate();
        }
    }

    /** ⑥ 주문 취소, ④ 입고에서 부른다. */
    public void increaseStock(Connection con, int menuId, int qty) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE menu SET stock = stock + ?, sold_out = 0 WHERE menu_id = ?")) {
            ps.setInt(1, qty);
            ps.setInt(2, menuId);
            ps.executeUpdate();
        }
    }
}
