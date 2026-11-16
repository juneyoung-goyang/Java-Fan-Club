package dicepos.menu;

import dicepos.common.DBUtil;
import dicepos.game.GameDAO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** purchase_order(발주)·purchase_order_detail(발주 상세) 테이블 (④ 박준영) */
public class PurchaseOrderDAO {
    private final MenuDAO menuDAO = new MenuDAO();
    private final GameDAO gameDAO = new GameDAO();

    /** 같은 품목의 직전 발주 단가 (없으면 0) */
    public int lastUnitCost(String itemType, int itemId) throws SQLException {
        String column = "메뉴".equals(itemType) ? "menu_id" : "game_id";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT unit_cost FROM purchase_order_detail WHERE " + column + " = ? ORDER BY po_detail_id DESC LIMIT 1")) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * 발주 요청 (트랜잭션): 헤더 1줄(상태 '요청') + 품목마다 상세 1줄. 하나라도 실패하면 rollback
     * details의 poDetailId, poId는 쓰지 않는다 (DB가 정한다).
     */
    public void save(Integer staffId, String supplier, List<PurchaseDetailDTO> details) throws SQLException {
        int total = 0;
        for (PurchaseDetailDTO d : details) total += d.getAmount();

        Connection con = DBUtil.getConnection();
        try {
            con.setAutoCommit(false);
            int poId;
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO purchase_order (staff_id, supplier, order_date, total_amount, status) VALUES (?, ?, NOW(), ?, '요청')",
                    Statement.RETURN_GENERATED_KEYS)) {
                if (staffId == null) ps.setNull(1, Types.INTEGER);
                else ps.setInt(1, staffId);
                ps.setString(2, supplier);
                ps.setInt(3, total);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    poId = keys.getInt(1);
                }
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO purchase_order_detail (po_id, item_type, menu_id, game_id, qty, unit_cost, amount) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                for (PurchaseDetailDTO d : details) {
                    ps.setInt(1, poId);
                    ps.setString(2, d.getItemType());
                    if (d.getMenuId() == null) ps.setNull(3, Types.INTEGER);
                    else ps.setInt(3, d.getMenuId());
                    if (d.getGameId() == null) ps.setNull(4, Types.INTEGER);
                    else ps.setInt(4, d.getGameId());
                    ps.setInt(5, d.getQty());
                    ps.setInt(6, d.getUnitCost());
                    ps.setInt(7, d.getAmount());
                    ps.executeUpdate();
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

    /** 발주 내역 (최근 것부터) */
    public List<PurchaseOrderDTO> findAll() throws SQLException {
        List<PurchaseOrderDTO> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM purchase_order ORDER BY po_id DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new PurchaseOrderDTO(rs.getInt("po_id"), rs.getObject("staff_id", Integer.class),
                        rs.getString("supplier"), rs.getObject("order_date", LocalDateTime.class),
                        rs.getInt("total_amount"), rs.getString("status"), rs.getObject("receive_date", LocalDateTime.class)));
            }
        }
        return list;
    }

    /** 발주 상세. 한 줄: {구분, 품목 이름, 수량, 단가, 금액} */
    public List<Object[]> findDetails(int poId) throws SQLException {
        String sql = "SELECT d.item_type, COALESCE(m.name, g.name), d.qty, d.unit_cost, d.amount FROM purchase_order_detail d"
                   + " LEFT JOIN menu m ON m.menu_id = d.menu_id LEFT JOIN game g ON g.game_id = d.game_id"
                   + " WHERE d.po_id = ? ORDER BY d.po_detail_id";
        List<Object[]> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, poId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new Object[] {rs.getString(1), rs.getString(2), rs.getInt(3), rs.getInt(4), rs.getInt(5)});
            }
        }
        return list;
    }

    /** 발주 취소: 기록을 남기기 위해 지우지 않고 상태만 '취소' */
    public void cancel(int poId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE purchase_order SET status = '취소' WHERE po_id = ? AND status = '요청'")) {
            ps.setInt(1, poId);
            if (ps.executeUpdate() == 0) throw new SQLException("'요청' 상태인 발주만 취소할 수 있습니다.");
        }
    }

    /**
     * 입고 처리 (트랜잭션): 발주 상태 '입고' + 메뉴 재고 증가(④) + 보드게임 보유 수량 증가(③ GameDAO)
     * 하나라도 실패하면 rollback
     */
    public void receive(int poId) throws SQLException {
        Connection con = DBUtil.getConnection();
        try {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE purchase_order SET status = '입고', receive_date = NOW() WHERE po_id = ? AND status = '요청'")) {
                ps.setInt(1, poId);
                if (ps.executeUpdate() == 0) throw new SQLException("'요청' 상태인 발주만 입고할 수 있습니다.");
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT item_type, menu_id, game_id, qty FROM purchase_order_detail WHERE po_id = ?")) {
                ps.setInt(1, poId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        if ("메뉴".equals(rs.getString("item_type"))) {
                            menuDAO.increaseStock(con, rs.getInt("menu_id"), rs.getInt("qty"));
                        } else {
                            gameDAO.increaseStock(con, rs.getInt("game_id"), rs.getInt("qty"));
                        }
                    }
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
}
