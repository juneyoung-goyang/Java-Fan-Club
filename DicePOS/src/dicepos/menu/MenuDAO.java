package dicepos.menu;

import dicepos.common.DBUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** menu(메뉴) 테이블 (④ 박준영) */
public class MenuDAO {

    /** 분류별 메뉴 ("전체"면 모두). ⑥ 주문 화면이 메뉴 버튼을 만들 때도 쓴다. */
    public List<MenuDTO> findByCategory(String category) throws SQLException {
        List<MenuDTO> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT * FROM menu WHERE ? = '전체' OR category = ? ORDER BY FIELD(category, '음료', '간식', '식사'), menu_id")) {
            ps.setString(1, category);
            ps.setString(2, category);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(toDTO(rs));
            }
        }
        return list;
    }

    /** 다른 메뉴가 이미 쓰는 이름인지 (수정할 때는 자기 자신 제외) */
    public boolean isNameTaken(String name, int exceptMenuId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM menu WHERE name = ? AND menu_id <> ?")) {
            ps.setString(1, name);
            ps.setInt(2, exceptMenuId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    /** 추가. 재고가 0이면 자동으로 품절 */
    public void insert(MenuDTO m) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO menu (name, category, price, stock, sold_out) VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, m.getName());
            ps.setString(2, m.getCategory());
            ps.setInt(3, m.getPrice());
            ps.setInt(4, m.getStock());
            ps.setBoolean(5, m.isSoldOut() || m.getStock() == 0);
            ps.executeUpdate();
        }
    }

    /** 수정. 재고를 0으로 저장해도 자동으로 품절 */
    public void update(MenuDTO m) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE menu SET name = ?, category = ?, price = ?, stock = ?, sold_out = ? WHERE menu_id = ?")) {
            ps.setString(1, m.getName());
            ps.setString(2, m.getCategory());
            ps.setInt(3, m.getPrice());
            ps.setInt(4, m.getStock());
            ps.setBoolean(5, m.isSoldOut() || m.getStock() == 0);
            ps.setInt(6, m.getMenuId());
            ps.executeUpdate();
        }
    }

    /** 품절 체크박스 */
    public void setSoldOut(int menuId, boolean soldOut) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE menu SET sold_out = ? WHERE menu_id = ?")) {
            ps.setBoolean(1, soldOut);
            ps.setInt(2, menuId);
            ps.executeUpdate();
        }
    }

    /** 주문·결제·발주 기록 수. 0이 아니면 삭제 대신 품절 처리를 안내한다. */
    public int countRecords(int menuId) throws SQLException {
        String sql = "SELECT (SELECT COUNT(*) FROM order_detail WHERE menu_id = ?)"
                   + " + (SELECT COUNT(*) FROM payment_detail WHERE menu_id = ?)"
                   + " + (SELECT COUNT(*) FROM purchase_order_detail WHERE menu_id = ?)";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, menuId);
            ps.setInt(2, menuId);
            ps.setInt(3, menuId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void delete(int menuId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM menu WHERE menu_id = ?")) {
            ps.setInt(1, menuId);
            ps.executeUpdate();
        }
    }

    /** ⑥ 주문 트랜잭션 안에서 부른다. 재고가 모자라면 SQLException, 0이 되면 품절 */
    public void decreaseStock(Connection con, int menuId, int qty) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE menu SET stock = stock - ? WHERE menu_id = ? AND stock >= ? AND sold_out = 0")) {
            ps.setInt(1, qty);
            ps.setInt(2, menuId);
            ps.setInt(3, qty);
            if (ps.executeUpdate() == 0) throw new SQLException("재고가 부족하거나 품절된 메뉴입니다. (메뉴 번호 " + menuId + ")");
        }
        try (PreparedStatement ps = con.prepareStatement("UPDATE menu SET sold_out = 1 WHERE menu_id = ? AND stock = 0")) {
            ps.setInt(1, menuId);
            ps.executeUpdate();
        }
    }

    /** ⑥ 주문 취소, ④ 입고에서 부른다. 재고가 0이어서 품절이었으면 품절을 푼다. */
    public void increaseStock(Connection con, int menuId, int qty) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE menu SET sold_out = IF(stock = 0, 0, sold_out), stock = stock + ? WHERE menu_id = ?")) {
            // SET은 왼쪽부터 처리되므로 sold_out을 정할 때 stock은 아직 늘기 전 값이다
            ps.setInt(1, qty);
            ps.setInt(2, menuId);
            ps.executeUpdate();
        }
    }

    private MenuDTO toDTO(ResultSet rs) throws SQLException {
        return new MenuDTO(rs.getInt("menu_id"), rs.getString("name"), rs.getString("category"),
                rs.getInt("price"), rs.getInt("stock"), rs.getBoolean("sold_out"));
    }
}
