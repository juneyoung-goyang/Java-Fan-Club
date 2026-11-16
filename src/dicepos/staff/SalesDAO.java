package dicepos.staff;

import dicepos.common.DBUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 매출 집계. ②의 payment(결제)·payment_detail(결제 상세)과 ④의 menu(메뉴)를 조회만 한다.
 * 결과는 표에 바로 넣을 수 있게 Object[] 한 줄씩 돌려준다.
 */
public class SalesDAO {

    /**
     * 일별(monthly = false) 또는 월별(true) 매출
     * 한 줄: {기간, 건수, 매출, 카드, 현금, 포인트}
     */
    public List<Object[]> byPeriod(LocalDate from, LocalDate to, boolean monthly) throws SQLException {
        String format = monthly ? "%Y-%m" : "%Y-%m-%d";
        String sql = "SELECT DATE_FORMAT(pay_time, '" + format + "') AS period, COUNT(*), SUM(total_amount),"
                   + " SUM(card_amount), SUM(cash_amount), SUM(point_amount)"
                   + " FROM payment WHERE DATE(pay_time) BETWEEN ? AND ?"
                   + " GROUP BY period ORDER BY period";
        List<Object[]> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, from);
            ps.setObject(2, to);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Object[] {rs.getString(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getInt(5), rs.getInt(6)});
                }
            }
        }
        return list;
    }

    /** 항목 구분별 합계. 한 줄: {구분(이용료/메뉴), 금액} */
    public List<Object[]> byItemType(LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT d.item_type, SUM(d.subtotal) FROM payment p JOIN payment_detail d ON d.pay_id = p.pay_id"
                   + " WHERE DATE(p.pay_time) BETWEEN ? AND ? GROUP BY d.item_type";
        List<Object[]> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, from);
            ps.setObject(2, to);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new Object[] {rs.getString(1), rs.getInt(2)});
            }
        }
        return list;
    }

    /** 메뉴 판매 순위. 한 줄: {메뉴 이름, 수량, 금액} */
    public List<Object[]> menuRanking(LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT m.name, SUM(d.qty) AS qty, SUM(d.subtotal) FROM payment p"
                   + " JOIN payment_detail d ON d.pay_id = p.pay_id JOIN menu m ON m.menu_id = d.menu_id"
                   + " WHERE d.item_type = '메뉴' AND DATE(p.pay_time) BETWEEN ? AND ?"
                   + " GROUP BY m.menu_id, m.name ORDER BY qty DESC LIMIT 10";
        List<Object[]> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, from);
            ps.setObject(2, to);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new Object[] {rs.getString(1), rs.getInt(2), rs.getInt(3)});
            }
        }
        return list;
    }
}
