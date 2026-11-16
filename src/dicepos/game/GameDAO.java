package dicepos.game;

import dicepos.common.DBUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** game(보드게임) 테이블 (③ 김은별) */
public class GameDAO {

    /** ④ 발주 화면이 품목 목록을 만들 때 쓴다. */
    public List<GameDTO> findAll() throws SQLException {
        return search("");
    }

    /** 이름에 검색어가 들어간 게임 */
    public List<GameDTO> search(String keyword) throws SQLException {
        List<GameDTO> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM game WHERE name LIKE ? ORDER BY name")) {
            ps.setString(1, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(toDTO(rs));
            }
        }
        return list;
    }

    public GameDTO findById(int gameId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM game WHERE game_id = ?")) {
            ps.setInt(1, gameId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? toDTO(rs) : null;
            }
        }
    }

    /** 게임 번호 → 지금 손님에게 나가 있는 수량 (대여중 + 호출) */
    public Map<Integer, Integer> busyMap() throws SQLException {
        Map<Integer, Integer> map = new HashMap<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT game_id, COUNT(*) FROM game_rental WHERE status IN ('대여중', '호출') GROUP BY game_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) map.put(rs.getInt(1), rs.getInt(2));
        }
        return map;
    }

    /** 새 게임은 보유 수량 0으로 등록한다. 수량은 ④ 발주에서 입고하면 늘어난다. */
    public void insert(GameDTO g) throws SQLException {
        String sql = "INSERT INTO game (name, genre, min_player, max_player, difficulty, play_min, stock, status, location)"
                   + " VALUES (?, ?, ?, ?, ?, ?, 0, ?, ?)";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            fill(ps, g);
            ps.executeUpdate();
        }
    }

    /** 정보 수정 (보유 수량은 입고·폐기로만 바뀐다) */
    public void update(GameDTO g) throws SQLException {
        String sql = "UPDATE game SET name = ?, genre = ?, min_player = ?, max_player = ?, difficulty = ?, play_min = ?,"
                   + " status = ?, location = ? WHERE game_id = ?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            fill(ps, g);
            ps.setInt(9, g.getGameId());
            ps.executeUpdate();
        }
    }

    private void fill(PreparedStatement ps, GameDTO g) throws SQLException {
        ps.setString(1, g.getName());
        ps.setString(2, g.getGenre());
        ps.setInt(3, g.getMinPlayer());
        ps.setInt(4, g.getMaxPlayer());
        ps.setInt(5, g.getDifficulty());
        ps.setInt(6, g.getPlayMin());
        ps.setString(7, g.getStatus());
        ps.setString(8, g.getLocation());
    }

    /** 대여·추천·발주 기록이 하나라도 있으면 true (그러면 삭제하지 않는다) */
    public boolean hasRecords(int gameId) throws SQLException {
        String sql = "SELECT (SELECT COUNT(*) FROM game_rental WHERE game_id = ?)"
                   + " + (SELECT COUNT(*) FROM recommend WHERE game_id = ?)"
                   + " + (SELECT COUNT(*) FROM purchase_order_detail WHERE game_id = ?)";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, gameId);
            ps.setInt(2, gameId);
            ps.setInt(3, gameId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    public void delete(int gameId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM game WHERE game_id = ?")) {
            ps.setInt(1, gameId);
            ps.executeUpdate();
        }
    }

    /** 폐기: 보유 수량을 줄이고, 0이 되면 상태를 '폐기'로 바꾼다. */
    public void discard(int gameId, int qty) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE game SET stock = stock - ?, status = IF(stock = 0, '폐기', status) WHERE game_id = ? AND stock >= ?")) {
            // MySQL은 SET을 왼쪽부터 처리하므로 status를 볼 때 stock은 이미 줄어든 값이다
            ps.setInt(1, qty);
            ps.setInt(2, gameId);
            ps.setInt(3, qty);
            if (ps.executeUpdate() == 0) throw new SQLException("보유 수량보다 많이 폐기할 수 없습니다.");
        }
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

    /** ② 결제(⑤ 이용 종료) 트랜잭션 안에서 부른다. 그 이용 건의 남은 예약·호출을 취소한다. */
    public void cancelReservations(Connection con, int usageId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE game_rental SET status = '취소' WHERE usage_id = ? AND status IN ('예약', '호출')")) {
            ps.setInt(1, usageId);
            ps.executeUpdate();
        }
    }

    /**
     * 키오스크 게임 추천: 인원이 맞고, 난이도·시간이 조건 이하이고, 상태 '정상'에 보유 1개 이상인 게임
     * maxMinutes가 0이면 시간 상관없음, genre가 "전체"면 장르 상관없음
     */
    public List<GameDTO> recommend(int people, int maxDifficulty, int maxMinutes, String genre) throws SQLException {
        String sql = "SELECT * FROM game WHERE status = '정상' AND stock >= 1 AND min_player <= ? AND max_player >= ?"
                   + " AND difficulty <= ? AND (? = 0 OR play_min <= ?) AND (? = '전체' OR genre = ?)"
                   + " ORDER BY difficulty DESC, name LIMIT 6";
        List<GameDTO> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, people);
            ps.setInt(2, people);
            ps.setInt(3, maxDifficulty);
            ps.setInt(4, maxMinutes);
            ps.setInt(5, maxMinutes);
            ps.setString(6, genre);
            ps.setString(7, genre);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(toDTO(rs));
            }
        }
        return list;
    }

    /** 많이 추천된 게임 TOP 5. 한 줄: {게임 이름, 추천 수, 선택 수} */
    public List<Object[]> top5Recommended() throws SQLException {
        String sql = "SELECT g.name, COUNT(*) AS cnt, SUM(r.chosen) FROM recommend r JOIN game g ON g.game_id = r.game_id"
                   + " GROUP BY g.game_id, g.name ORDER BY cnt DESC LIMIT 5";
        List<Object[]> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(new Object[] {rs.getString(1), rs.getInt(2), rs.getInt(3)});
        }
        return list;
    }

    private GameDTO toDTO(ResultSet rs) throws SQLException {
        return new GameDTO(rs.getInt("game_id"), rs.getString("name"), rs.getString("genre"),
                rs.getInt("min_player"), rs.getInt("max_player"), rs.getInt("difficulty"),
                rs.getInt("play_min"), rs.getInt("stock"), rs.getString("status"), rs.getString("location"));
    }
}
