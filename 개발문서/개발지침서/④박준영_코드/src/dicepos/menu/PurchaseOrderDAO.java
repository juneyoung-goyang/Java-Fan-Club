package dicepos.menu;

import dicepos.common.DBUtil;
import dicepos.game.GameDAO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** purchase_order(발주)와 purchase_order_detail(발주 상세)에 일을 시키는 클래스. */
public class PurchaseOrderDAO {

    /** [발주 요청] 발주 1줄 + 품목 N줄을 한 트랜잭션으로 저장하고, 새 발주 번호를 돌려준다. */
    public int insert(int adminId, String supplier, List<PurchaseDetailDTO> items) throws SQLException {
        int total = 0;
        for (PurchaseDetailDTO d : items) total += d.getAmount();

        Connection con = DBUtil.getConnection();
        try {
            con.setAutoCommit(false);                       // 1. 묶기 시작
            int poId;
            String head = "INSERT INTO purchase_order (staff_id, supplier, order_date, total_amount, status)"
                        + " VALUES (?, ?, NOW(), ?, '요청')";
            try (PreparedStatement ps = con.prepareStatement(head, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, adminId);
                ps.setString(2, supplier);
                ps.setInt(3, total);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {   // 2. 새 발주 번호 받기
                    keys.next();
                    poId = keys.getInt(1);
                }
            }
            String detail = "INSERT INTO purchase_order_detail"
                          + " (po_id, item_type, menu_id, game_id, qty, unit_cost, amount) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(detail)) {
                for (PurchaseDetailDTO d : items) {         // 3. 품목마다 1줄
                    ps.setInt(1, poId);
                    ps.setString(2, d.getItemType());
                    ps.setObject(3, d.getMenuId(), Types.INTEGER);   // null이면 NULL로 저장
                    ps.setObject(4, d.getGameId(), Types.INTEGER);
                    ps.setInt(5, d.getQty());
                    ps.setInt(6, d.getUnitCost());
                    ps.setInt(7, d.getAmount());
                    ps.executeUpdate();
                }
            }
            con.commit();                                   // 4. 전부 확정
            return poId;
        } catch (SQLException e) {
            con.rollback();                                 // 하나라도 실패하면 전부 취소
            throw e;
        } finally {
            con.setAutoCommit(true);
            con.close();
        }
    }

    /** 발주 내역. status가 null이면 전체 */
    public List<PurchaseOrderDTO> findAll(String status) throws SQLException {
        String sql = "SELECT p.po_id, p.staff_id, s.name AS staff_name, p.supplier,"
                   + " DATE_FORMAT(p.order_date, '%m-%d %H:%i') AS order_date, p.total_amount, p.status,"
                   + " COUNT(d.po_detail_id) AS item_count"
                   + " FROM purchase_order p"
                   + " LEFT JOIN staff s ON p.staff_id = s.staff_id"
                   + " LEFT JOIN purchase_order_detail d ON d.po_id = p.po_id"
                   + (status == null ? "" : " WHERE p.status = ?")
                   + " GROUP BY p.po_id, p.staff_id, s.name, p.supplier, p.order_date, p.total_amount, p.status"
                   + " ORDER BY p.po_id DESC";
        List<PurchaseOrderDTO> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (status != null) ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new PurchaseOrderDTO(rs.getInt("po_id"), rs.getInt("staff_id"),
                            rs.getString("staff_name"), rs.getString("supplier"), rs.getString("order_date"),
                            rs.getInt("total_amount"), rs.getString("status"), rs.getInt("item_count")));
                }
            }
        }
        return list;
    }

    public List<PurchaseDetailDTO> findDetails(int poId) throws SQLException {
        try (Connection con = DBUtil.getConnection()) {
            return findDetails(con, poId);
        }
    }

    private List<PurchaseDetailDTO> findDetails(Connection con, int poId) throws SQLException {
        String sql = "SELECT d.item_type, d.menu_id, d.game_id, COALESCE(m.name, g.name) AS item_name,"
                   + " d.qty, d.unit_cost"
                   + " FROM purchase_order_detail d"
                   + " LEFT JOIN menu m ON d.menu_id = m.menu_id"
                   + " LEFT JOIN game g ON d.game_id = g.game_id"
                   + " WHERE d.po_id = ? ORDER BY d.po_detail_id";
        List<PurchaseDetailDTO> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, poId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new PurchaseDetailDTO(rs.getString("item_type"),
                            (Integer) rs.getObject("menu_id"), (Integer) rs.getObject("game_id"),
                            rs.getString("item_name"), rs.getInt("qty"), rs.getInt("unit_cost")));
                }
            }
        }
        return list;
    }

    /** 같은 품목을 가장 최근에 발주한 단가. 처음 발주하는 품목이면 null */
    public Integer lastUnitCost(String itemType, int id) throws SQLException {
        String column = itemType.equals("메뉴") ? "menu_id" : "game_id";
        String sql = "SELECT d.unit_cost FROM purchase_order_detail d"
                   + " JOIN purchase_order p ON d.po_id = p.po_id"
                   + " WHERE d." + column + " = ? AND p.status <> '취소'"
                   + " ORDER BY d.po_detail_id DESC LIMIT 1";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Integer.valueOf(rs.getInt(1)) : null;
            }
        }
    }

    /** [입고 처리] 발주 상태 + 메뉴 재고 + 게임 재고를 한 트랜잭션으로 바꾼다. */
    public void receive(int poId) throws SQLException {
        MenuDAO menuDAO = new MenuDAO();
        GameDAO gameDAO = new GameDAO();   // ③ 김은별의 클래스
        Connection con = DBUtil.getConnection();
        try {
            con.setAutoCommit(false);
            String sql = "UPDATE purchase_order SET status = '입고', receive_date = NOW()"
                       + " WHERE po_id = ? AND status = '요청'";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, poId);
                if (ps.executeUpdate() != 1) {   // 이미 입고됐거나 취소된 발주
                    throw new SQLException("요청 상태인 발주만 입고할 수 있습니다.");
                }
            }
            for (PurchaseDetailDTO d : findDetails(con, poId)) {
                if (d.getItemType().equals("메뉴")) {
                    menuDAO.increaseStock(con, d.getMenuId(), d.getQty());
                } else {
                    gameDAO.increaseStock(con, d.getGameId(), d.getQty());   // 같은 Connection을 넘긴다
                }
            }
            con.commit();
        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
            con.close();
        }
    }

    /** [발주 취소] 지우지 않고 상태만 '취소'로. 바뀐 줄 수를 돌려준다(0이면 이미 입고·취소됨). */
    public int cancel(int poId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE purchase_order SET status = '취소' WHERE po_id = ? AND status = '요청'")) {
            ps.setInt(1, poId);
            return ps.executeUpdate();
        }
    }
}
