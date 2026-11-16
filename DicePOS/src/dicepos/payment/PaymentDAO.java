package dicepos.payment;

import dicepos.common.DBUtil;
import dicepos.game.GameDAO;
import dicepos.member.MemberDAO;
import dicepos.table.TableUsageDAO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** payment(결제)·payment_detail(결제 상세) 테이블 (② 송하안) */
public class PaymentDAO {
    private final MemberDAO memberDAO = new MemberDAO();
    private final TableUsageDAO usageDAO = new TableUsageDAO();
    private final GameDAO gameDAO = new GameDAO();

    /** 누적 결제 금액으로 등급을 정한다 */
    public static String gradeOf(int totalPaid) {
        if (totalPaid >= 300_000) return "골드";
        if (totalPaid >= 100_000) return "실버";
        return "일반";
    }

    /** 등급별 적립률(%) — 카드·현금으로 낸 금액에만 적립한다 (포인트로 낸 금액은 적립 안 함) */
    public static int earnRate(String grade) {
        if ("골드".equals(grade)) return 5;
        if ("실버".equals(grade)) return 3;
        return 1;
    }

    /**
     * 결제하기 (트랜잭션)
     * ① payment 1줄 ② 결제 항목마다 payment_detail ③ 회원 포인트 차감·적립과 등급 갱신
     * ④ 이용 상태 '결제완료'(⑤ TableUsageDAO.finish) + 남은 게임 예약 취소(③ GameDAO.cancelReservations)
     * 하나라도 실패하면 rollback 한다. 결제 번호를 돌려준다.
     */
    public int pay(PaymentDTO p, List<PaymentDetailDTO> details, int memberPointBefore) throws SQLException {
        Connection con = DBUtil.getConnection();
        try {
            con.setAutoCommit(false);

            // ① 결제 헤더
            int payId;
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO payment (usage_id, staff_id, member_id, pay_time, total_amount, card_amount, cash_amount, point_amount, earn_point)"
                    + " VALUES (?, ?, ?, NOW(), ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, p.getUsageId());
                if (p.getStaffId() == null) ps.setNull(2, Types.INTEGER);
                else ps.setInt(2, p.getStaffId());
                if (p.getMemberId() == null) ps.setNull(3, Types.INTEGER);
                else ps.setInt(3, p.getMemberId());
                ps.setInt(4, p.getTotalAmount());
                ps.setInt(5, p.getCardAmount());
                ps.setInt(6, p.getCashAmount());
                ps.setInt(7, p.getPointAmount());
                ps.setInt(8, p.getEarnPoint());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    payId = keys.getInt(1);
                }
            }

            // ② 결제 상세
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO payment_detail (pay_id, item_type, menu_id, qty, unit_price, subtotal) VALUES (?, ?, ?, ?, ?, ?)")) {
                for (PaymentDetailDTO d : details) {
                    ps.setInt(1, payId);
                    ps.setString(2, d.getItemType());
                    if (d.getMenuId() == null) ps.setNull(3, Types.INTEGER);
                    else ps.setInt(3, d.getMenuId());
                    ps.setInt(4, d.getQty());
                    ps.setInt(5, d.getUnitPrice());
                    ps.setInt(6, d.getSubtotal());
                    ps.executeUpdate();
                }
            }

            // ③ 회원 포인트·등급
            if (p.getMemberId() != null) {
                int point = memberPointBefore - p.getPointAmount() + p.getEarnPoint();
                String grade = gradeOf(memberDAO.totalPaid(con, p.getMemberId()));
                memberDAO.updatePointAndGrade(con, p.getMemberId(), point, grade);
            }

            // ④ 이용 상태 결제완료 + 남은 예약 취소 (다른 팀원 DAO에 같은 Connection을 넘긴다)
            usageDAO.finish(con, p.getUsageId());
            gameDAO.cancelReservations(con, p.getUsageId());

            con.commit();
            return payId;
        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
            con.close();
        }
    }

    /** 이미 결제된 이용 건인지 */
    public boolean isPaid(int usageId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM payment WHERE usage_id = ?")) {
            ps.setInt(1, usageId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    /**
     * 결제 내역. method는 "전체", "카드", "현금", "포인트" (그 수단으로 낸 금액이 있는 결제)
     * 한 줄: {결제 번호, 일시, 테이블, 회원, 총액, 카드, 현금, 포인트, 적립, 처리 직원}
     */
    public List<Object[]> findList(LocalDate from, LocalDate to, String method) throws SQLException {
        String where = "";
        if ("카드".equals(method)) where = " AND p.card_amount > 0";
        if ("현금".equals(method)) where = " AND p.cash_amount > 0";
        if ("포인트".equals(method)) where = " AND p.point_amount > 0";
        String sql = "SELECT p.pay_id, p.pay_time, t.table_name, COALESCE(m.name, '비회원'), p.total_amount,"
                   + " p.card_amount, p.cash_amount, p.point_amount, p.earn_point, COALESCE(s.name, '')"
                   + " FROM payment p JOIN table_usage u ON u.usage_id = p.usage_id JOIN cafe_table t ON t.table_no = u.table_no"
                   + " LEFT JOIN member m ON m.member_id = p.member_id LEFT JOIN staff s ON s.staff_id = p.staff_id"
                   + " WHERE DATE(p.pay_time) BETWEEN ? AND ?" + where + " ORDER BY p.pay_id DESC";
        List<Object[]> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, from);
            ps.setObject(2, to);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Object[] {rs.getInt(1), rs.getObject(2, LocalDateTime.class), rs.getString(3), rs.getString(4),
                            rs.getInt(5), rs.getInt(6), rs.getInt(7), rs.getInt(8), rs.getInt(9), rs.getString(10)});
                }
            }
        }
        return list;
    }

    /** 결제 상세 (영수증). 한 줄: {구분, 항목 이름, 수량, 단가, 소계} */
    public List<Object[]> findDetails(int payId) throws SQLException {
        String sql = "SELECT d.item_type, COALESCE(m.name, '이용료'), d.qty, d.unit_price, d.subtotal FROM payment_detail d"
                   + " LEFT JOIN menu m ON m.menu_id = d.menu_id WHERE d.pay_id = ? ORDER BY d.pay_detail_id";
        List<Object[]> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, payId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new Object[] {rs.getString(1), rs.getString(2), rs.getInt(3), rs.getInt(4), rs.getInt(5)});
            }
        }
        return list;
    }
}
