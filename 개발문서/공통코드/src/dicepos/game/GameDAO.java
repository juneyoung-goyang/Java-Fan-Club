package dicepos.game;

import dicepos.common.DBUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** ※ 임시 파일 (③ 김은별 것). ③이 진짜를 만들면 이 파일은 지운다. 메서드 모양은 01_메서드약속표와 같아야 한다. */
public class GameDAO {
    /** ④ 발주 화면이 품목 목록을 만들 때 쓴다. */
    public List<GameDTO> findAll() throws SQLException {
        List<GameDTO> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM game ORDER BY name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new GameDTO(rs.getInt("game_id"), rs.getString("name"), rs.getString("genre"),
                        rs.getInt("min_player"), rs.getInt("max_player"), rs.getInt("difficulty"),
                        rs.getInt("play_min"), rs.getInt("stock"), rs.getString("status"), rs.getString("location")));
            }
        }
        return list;
    }

    /** ④ 입고 트랜잭션 안에서 부른다. 폐기였던 게임은 정상으로 돌린다. */
    public void increaseStock(Connection con, int gameId, int qty) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE game SET stock = stock + ?, status = CASE WHEN status = '폐기' THEN '정상' ELSE status END WHERE game_id = ?")) {
            ps.setInt(1, qty);
            ps.setInt(2, gameId);
            ps.executeUpdate();
        }
    }

    /** ⑤ 이용 종료(결제) 트랜잭션 안에서 부른다. 그 이용 건의 남은 예약·호출을 취소한다. */
    public void cancelReservations(Connection con, int usageId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE game_rental SET status = '취소' WHERE usage_id = ? AND status IN ('예약', '호출')")) {
            ps.setInt(1, usageId);
            ps.executeUpdate();
        }
    }
}
