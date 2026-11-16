package dicepos.common;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** DB 연결 확인용. "연결 성공! 메뉴 7개"가 나오면 된다. */
public class DBTest {
    public static void main(String[] args) {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM menu");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            System.out.println("연결 성공! 메뉴 " + rs.getInt(1) + "개");
        } catch (SQLException ex) {
            System.out.println("연결 실패: " + ex.getMessage());
        }
    }
}
