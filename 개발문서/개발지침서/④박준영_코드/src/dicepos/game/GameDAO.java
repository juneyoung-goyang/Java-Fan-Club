package dicepos.game;

import dicepos.common.DBUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * ※ 임시 파일입니다. ③ 김은별이 진짜 GameDAO를 올리면 이 파일은 지웁니다.
 * ④ 발주 관리가 쓰는 두 메서드만 들어 있다.
 */
public class GameDAO {
    public List<GameDTO> findAll() throws SQLException {
        String sql = "SELECT game_id, name, stock, status FROM game ORDER BY name";
        List<GameDTO> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new GameDTO(rs.getInt("game_id"), rs.getString("name"),
                                     rs.getInt("stock"), rs.getString("status")));
            }
        }
        return list;
    }

    /** 입고할 때 부른다. 받은 Connection(발주 트랜잭션)을 그대로 쓴다. 폐기였던 게임은 정상으로 돌린다. */
    public void increaseStock(Connection con, int gameId, int qty) throws SQLException {
        String sql = "UPDATE game SET stock = stock + ?, "
                   + "status = CASE WHEN status = '폐기' THEN '정상' ELSE status END WHERE game_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, qty);
            ps.setInt(2, gameId);
            ps.executeUpdate();
        }
    }
}
