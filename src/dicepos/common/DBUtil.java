package dicepos.common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** DB 연결을 만들어 주는 클래스. 팀원 모두 DB에 접속할 때 이 클래스만 쓴다. */
public class DBUtil {
    private static final String URL =
        "jdbc:mysql://localhost:3306/dicepos?serverTimezone=Asia/Seoul&characterEncoding=UTF-8";
    private static final String USER = "root";
    private static final String PASSWORD = "본인 root 비밀번호";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
