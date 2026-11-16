package dicepos.game;

import dicepos.common.DBUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** recommend(게임 추천 기록) 테이블 (③ 김은별) */
public class RecommendDAO {

    /** 추천한 게임마다 한 줄씩 남긴다 */
    public void insert(int usageId, Integer memberId, int gameId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO recommend (usage_id, member_id, game_id, rec_time, chosen) VALUES (?, ?, ?, NOW(), 0)")) {
            ps.setInt(1, usageId);
            if (memberId == null) ps.setNull(2, Types.INTEGER);
            else ps.setInt(2, memberId);
            ps.setInt(3, gameId);
            ps.executeUpdate();
        }
    }

    /** 손님이 추천받은 게임을 골랐다 (대여 신청·예약) */
    public void markChosen(int usageId, int gameId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE recommend SET chosen = 1 WHERE usage_id = ? AND game_id = ? ORDER BY rec_id DESC LIMIT 1")) {
            ps.setInt(1, usageId);
            ps.setInt(2, gameId);
            ps.executeUpdate();
        }
    }

    /** 회원이 지난 방문 때 추천받은 게임 이름 (이번 이용 건 제외, 최근 것부터 5개) */
    public List<String> pastGames(int memberId, int exceptUsageId) throws SQLException {
        String sql = "SELECT g.name FROM recommend r JOIN game g ON g.game_id = r.game_id"
                   + " WHERE r.member_id = ? AND r.usage_id <> ? GROUP BY g.game_id, g.name"
                   + " ORDER BY MAX(r.rec_time) DESC LIMIT 5";
        List<String> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            ps.setInt(2, exceptUsageId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getString(1));
            }
        }
        return list;
    }
}
