package dicepos.order;

import dicepos.common.DBUtil;
import dicepos.menu.MenuDAO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** orders(주문)·order_detail(주문 상세) 테이블 (⑥ 김하진) */
public class OrderDAO {
    private final MenuDAO menuDAO = new MenuDAO();

    /** 한 이용 건에서 주문한 메뉴들. ② 결제 팝업이 결제 항목을 만들 때 쓴다. */
    public List<OrderDetailDTO> findDetailsByUsage(int usageId) throws SQLException {
        String sql = "SELECT d.order_detail_id, d.order_id, d.menu_id, d.qty, d.unit_price, d.subtotal, m.name FROM orders o"
                   + " JOIN order_detail d ON d.order_id = o.order_id"
                   + " JOIN menu m ON m.menu_id = d.menu_id"
                   + " WHERE o.usage_id = ? ORDER BY d.order_detail_id";
        List<OrderDetailDTO> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usageId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new OrderDetailDTO(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4),
                            rs.getInt(5), rs.getInt(6), rs.getString(7)));
                }
            }
        }
        return list;
    }

    /**
     * 주문 내역 표. 한 줄: {order_detail_id, 메뉴, 수량, 단가, 금액, 경로, 상태, 주문 시각}
     */
    public List<Object[]> findLinesByUsage(int usageId) throws SQLException {
        String sql = "SELECT d.order_detail_id, m.name, d.qty, d.unit_price, d.subtotal, o.channel, o.status, o.order_time"
                   + " FROM orders o JOIN order_detail d ON d.order_id = o.order_id JOIN menu m ON m.menu_id = d.menu_id"
                   + " WHERE o.usage_id = ? ORDER BY d.order_detail_id";
        List<Object[]> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usageId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Object[] {rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getInt(4), rs.getInt(5),
                            rs.getString(6), rs.getString(7), rs.getObject(8, LocalDateTime.class)});
                }
            }
        }
        return list;
    }

    /** 이 이용 건의 음식 금액 합계 */
    public int totalByUsage(int usageId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE usage_id = ?")) {
            ps.setInt(1, usageId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /**
     * 주문 저장 (트랜잭션): orders 1줄 + 메뉴마다 order_detail 1줄 + 메뉴 재고 감소(④ MenuDAO)
     * 단가는 주문하는 순간의 menu.price를 다시 읽어서 저장한다. 하나라도 실패하면 rollback
     * staffId는 키오스크 주문이면 null, status는 POS '확인' / 키오스크 '접수'
     */
    public void saveOrder(int usageId, Integer staffId, String channel, String status, List<CartItem> items) throws SQLException {
        Connection con = DBUtil.getConnection();
        try {
            con.setAutoCommit(false);
            checkUsing(con, usageId);
            int orderId;
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO orders (usage_id, staff_id, channel, order_time, total_amount, status) VALUES (?, ?, ?, NOW(), 0, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, usageId);
                if (staffId == null) ps.setNull(2, Types.INTEGER);
                else ps.setInt(2, staffId);
                ps.setString(3, channel);
                ps.setString(4, status);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    orderId = keys.getInt(1);
                }
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO order_detail (order_id, menu_id, qty, unit_price, subtotal)"
                    + " SELECT ?, menu_id, ?, price, price * ? FROM menu WHERE menu_id = ?")) {
                for (CartItem item : items) {
                    ps.setInt(1, orderId);
                    ps.setInt(2, item.getQty());
                    ps.setInt(3, item.getQty());
                    ps.setInt(4, item.getMenuId());
                    ps.executeUpdate();
                    menuDAO.decreaseStock(con, item.getMenuId(), item.getQty());
                }
            }
            updateOrderTotal(con, orderId);
            con.commit();
        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
            con.close();
        }
    }

    /** 이용 중이 아닌 테이블(이미 결제됨)에는 주문을 넣지 않는다 */
    private void checkUsing(Connection con, int usageId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT status FROM table_usage WHERE usage_id = ?")) {
            ps.setInt(1, usageId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !"이용중".equals(rs.getString(1))) {
                    throw new SQLException("이용 중인 테이블이 아닙니다. 결제가 끝났는지 확인하세요.");
                }
            }
        }
    }

    /**
     * 수량 변경 (트랜잭션): 주문 상세의 qty·subtotal을 바꾸고, 늘거나 준 만큼 재고를 조정하고, 주문 합계를 다시 계산
     */
    public void changeQty(int orderDetailId, int newQty) throws SQLException {
        Connection con = DBUtil.getConnection();
        try {
            con.setAutoCommit(false);
            int[] d = findDetail(con, orderDetailId);   // {order_id, menu_id, qty, usage_id}
            checkUsing(con, d[3]);
            int diff = newQty - d[2];
            if (diff > 0) menuDAO.decreaseStock(con, d[1], diff);
            if (diff < 0) menuDAO.increaseStock(con, d[1], -diff);
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE order_detail SET qty = ?, subtotal = unit_price * ? WHERE order_detail_id = ?")) {
                ps.setInt(1, newQty);
                ps.setInt(2, newQty);
                ps.setInt(3, orderDetailId);
                ps.executeUpdate();
            }
            updateOrderTotal(con, d[0]);
            con.commit();
        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
            con.close();
        }
    }

    /** 선택 취소 (트랜잭션): 상세를 지우고 재고를 되돌린다. 상세가 하나도 없어진 주문(헤더)도 지운다. */
    public void cancelDetail(int orderDetailId) throws SQLException {
        Connection con = DBUtil.getConnection();
        try {
            con.setAutoCommit(false);
            int[] d = findDetail(con, orderDetailId);
            checkUsing(con, d[3]);
            deleteDetail(con, orderDetailId, d);
            con.commit();
        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
            con.close();
        }
    }

    /** 전체 취소 (트랜잭션): 그 이용 건의 모든 주문을 지우고 재고를 되돌린다. */
    public void cancelAll(int usageId) throws SQLException {
        Connection con = DBUtil.getConnection();
        try {
            con.setAutoCommit(false);
            checkUsing(con, usageId);
            List<Integer> detailIds = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT d.order_detail_id FROM order_detail d JOIN orders o ON o.order_id = d.order_id WHERE o.usage_id = ?")) {
                ps.setInt(1, usageId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) detailIds.add(rs.getInt(1));
                }
            }
            for (int id : detailIds) {
                deleteDetail(con, id, findDetail(con, id));
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

    /** {order_id, menu_id, qty, usage_id} */
    private int[] findDetail(Connection con, int orderDetailId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT d.order_id, d.menu_id, d.qty, o.usage_id FROM order_detail d JOIN orders o ON o.order_id = d.order_id"
                + " WHERE d.order_detail_id = ?")) {
            ps.setInt(1, orderDetailId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("이미 취소된 주문입니다. 화면을 새로 고치세요.");
                return new int[] {rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4)};
            }
        }
    }

    private void deleteDetail(Connection con, int orderDetailId, int[] d) throws SQLException {
        menuDAO.increaseStock(con, d[1], d[2]);
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM order_detail WHERE order_detail_id = ?")) {
            ps.setInt(1, orderDetailId);
            ps.executeUpdate();
        }
        // 상세가 남아 있으면 합계만 다시 계산, 없으면 헤더도 지운다
        try (PreparedStatement ps = con.prepareStatement(
                "DELETE FROM orders WHERE order_id = ? AND NOT EXISTS (SELECT 1 FROM order_detail WHERE order_id = ?)")) {
            ps.setInt(1, d[0]);
            ps.setInt(2, d[0]);
            ps.executeUpdate();
        }
        updateOrderTotal(con, d[0]);
    }

    /** orders.total_amount = 주문 상세 소계의 합 */
    private void updateOrderTotal(Connection con, int orderId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE orders SET total_amount = (SELECT COALESCE(SUM(subtotal), 0) FROM order_detail WHERE order_id = ?)"
                + " WHERE order_id = ?")) {
            ps.setInt(1, orderId);
            ps.setInt(2, orderId);
            ps.executeUpdate();
        }
    }

    /** 직원이 아직 확인하지 않은 키오스크 주문 수 */
    public int countNewKioskOrders() throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM orders WHERE channel = '키오스크' AND status = '접수'");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /** 확인 안 한 키오스크 주문 내용. 한 줄: "Room 19  21:05  오렌지주스 x2, 라면 x1" */
    public List<String> findNewKioskOrders() throws SQLException {
        String sql = "SELECT t.table_name, o.order_time, GROUP_CONCAT(CONCAT(m.name, ' x', d.qty) SEPARATOR ', ')"
                   + " FROM orders o JOIN table_usage u ON u.usage_id = o.usage_id JOIN cafe_table t ON t.table_no = u.table_no"
                   + " JOIN order_detail d ON d.order_id = o.order_id JOIN menu m ON m.menu_id = d.menu_id"
                   + " WHERE o.channel = '키오스크' AND o.status = '접수' GROUP BY o.order_id, t.table_name, o.order_time ORDER BY o.order_id";
        List<String> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rs.getString(1) + "   " + rs.getObject(2, LocalDateTime.class).toLocalTime().withNano(0) + "   " + rs.getString(3));
            }
        }
        return list;
    }

    /** 키오스크 주문을 모두 '확인'으로 */
    public void confirmKioskOrders() throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE orders SET status = '확인' WHERE channel = '키오스크' AND status = '접수'")) {
            ps.executeUpdate();
        }
    }
}
