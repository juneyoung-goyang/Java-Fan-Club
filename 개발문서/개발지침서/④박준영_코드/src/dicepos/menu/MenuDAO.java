package dicepos.menu;

import dicepos.common.DBUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** menu 테이블에 일을 시키는 클래스. menu를 바꾸는 SQL은 모두 여기에만 있다. */
public class MenuDAO {

    public List<MenuDTO> findAll() throws SQLException {
        return query("SELECT * FROM menu ORDER BY category, name", null);
    }

    /** ⑥ 김하진도 메뉴 버튼을 만들 때 쓴다. */
    public List<MenuDTO> findByCategory(String category) throws SQLException {
        return query("SELECT * FROM menu WHERE category = ? ORDER BY name", category);
    }

    private List<MenuDTO> query(String sql, String category) throws SQLException {
        List<MenuDTO> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (category != null) ps.setString(1, category);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new MenuDTO(rs.getInt("menu_id"), rs.getString("name"),
                            rs.getString("category"), rs.getInt("price"),
                            rs.getInt("stock"), rs.getBoolean("sold_out")));
                }
            }
        }
        return list;
    }

    /** 새 메뉴를 넣고, DB가 만든 메뉴 번호를 돌려준다. */
    public int insert(MenuDTO m) throws SQLException {
        String sql = "INSERT INTO menu (name, category, price, stock, sold_out) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, m.getName());
            ps.setString(2, m.getCategory());
            ps.setInt(3, m.getPrice());
            ps.setInt(4, m.getStock());
            ps.setBoolean(5, m.isSoldOut());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public void update(MenuDTO m) throws SQLException {
        String sql = "UPDATE menu SET name = ?, category = ?, price = ?, stock = ?, sold_out = ? WHERE menu_id = ?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, m.getName());
            ps.setString(2, m.getCategory());
            ps.setInt(3, m.getPrice());
            ps.setInt(4, m.getStock());
            ps.setBoolean(5, m.isSoldOut());
            ps.setInt(6, m.getMenuId());
            ps.executeUpdate();
        }
    }

    public void setSoldOut(int menuId, boolean soldOut) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE menu SET sold_out = ? WHERE menu_id = ?")) {
            ps.setBoolean(1, soldOut);
            ps.setInt(2, menuId);
            ps.executeUpdate();
        }
    }

    /** 이 메뉴를 가리키는 기록 수 (주문 상세 + 결제 상세 + 발주 상세). 0일 때만 지울 수 있다. */
    public int countUsage(int menuId) throws SQLException {
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

    /**
     * ⑥ 김하진이 주문을 저장할 때 같은 Connection을 넘겨서 부른다.
     * 재고가 모자라면 예외를 던지고, ⑥은 그 예외를 받아 주문 전체를 rollback 한다.
     */
    public void decreaseStock(Connection con, int menuId, int qty) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE menu SET stock = stock - ? WHERE menu_id = ? AND stock >= ?")) {
            ps.setInt(1, qty);
            ps.setInt(2, menuId);
            ps.setInt(3, qty);
            if (ps.executeUpdate() == 0) {
                throw new SQLException("재고가 부족합니다. (메뉴 번호 " + menuId + ")");
            }
        }
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE menu SET sold_out = 1 WHERE menu_id = ? AND stock = 0")) {
            ps.setInt(1, menuId);
            ps.executeUpdate();
        }
    }

    /** 입고(④)와 주문 취소(⑥)에서 부른다. 재고가 생기면 품절을 푼다. */
    public void increaseStock(Connection con, int menuId, int qty) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE menu SET stock = stock + ?, sold_out = 0 WHERE menu_id = ?")) {
            ps.setInt(1, qty);
            ps.setInt(2, menuId);
            ps.executeUpdate();
        }
    }
}
