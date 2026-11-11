package dicepos.order;

import dicepos.common.DBUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** ※ 임시 파일 (⑥ 김하진 것). ⑥이 진짜를 만들면 지운다. ② 결제가 쓰는 메서드만 들어 있다. */
public class OrderDAO {
    /** 한 이용 건에서 주문한 메뉴들 (취소된 주문 제외). ② 결제 팝업이 결제 항목을 만들 때 쓴다. */
    public List<OrderDetailDTO> findDetailsByUsage(int usageId) throws SQLException {
        String sql = "SELECT d.order_detail_id, d.order_id, d.menu_id, d.qty, d.unit_price, d.subtotal, m.name FROM orders o"
                   + " JOIN order_detail d ON d.order_id = o.order_id"
                   + " JOIN menu m ON m.menu_id = d.menu_id"
                   + " WHERE o.usage_id = ? AND o.status <> '취소' ORDER BY d.order_detail_id";
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
}
