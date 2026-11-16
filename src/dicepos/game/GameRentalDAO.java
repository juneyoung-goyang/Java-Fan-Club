package dicepos.game;

import dicepos.common.DBUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** game_rental(게임 대여·예약) 테이블 (③ 김은별) */
public class GameRentalDAO {

    /**
     * 상태별 목록 (화면 표에 바로 쓰도록 게임 이름과 룸 이름을 JOIN 한다)
     * 한 줄: {rental_id, 게임 이름, 룸 이름, 상태, 신청 시각, 대여 시작 시각}
     */
    public List<Object[]> findByStatus(String status1, String status2) throws SQLException {
        String sql = "SELECT r.rental_id, g.name, t.table_name, r.status, r.request_time, r.start_time"
                   + " FROM game_rental r JOIN game g ON g.game_id = r.game_id"
                   + " JOIN table_usage u ON u.usage_id = r.usage_id JOIN cafe_table t ON t.table_no = u.table_no"
                   + " WHERE r.status IN (?, ?) ORDER BY r.request_time";
        List<Object[]> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status1);
            ps.setString(2, status2);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Object[] {rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                            rs.getObject(5, LocalDateTime.class), rs.getObject(6, LocalDateTime.class)});
                }
            }
        }
        return list;
    }

    /** 대여 가능 수량 = 보유 수량 − (대여중 + 호출) */
    public int available(Connection con, int gameId) throws SQLException {
        String sql = "SELECT g.stock - (SELECT COUNT(*) FROM game_rental WHERE game_id = ? AND status IN ('대여중', '호출'))"
                   + " FROM game g WHERE g.game_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, gameId);
            ps.setInt(2, gameId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public int available(int gameId) throws SQLException {
        try (Connection con = DBUtil.getConnection()) {
            return available(con, gameId);
        }
    }

    /** 같은 이용 건이 같은 게임을 이미 예약·호출·대여 중인지 */
    public boolean isDuplicate(int usageId, int gameId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT COUNT(*) FROM game_rental WHERE usage_id = ? AND game_id = ? AND status IN ('예약', '호출', '대여중')")) {
            ps.setInt(1, usageId);
            ps.setInt(2, gameId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    /**
     * 대여 신청: 대여 가능하면 '대여중', 모두 나가 있으면 '예약'으로 넣는다.
     * 넣은 기록의 번호를 돌려준다. 상태는 findStatus로 확인한다.
     */
    public int request(int gameId, int usageId) throws SQLException {
        try (Connection con = DBUtil.getConnection()) {
            boolean canRent = available(con, gameId) > 0;
            String sql = canRent
                    ? "INSERT INTO game_rental (game_id, usage_id, status, request_time, start_time) VALUES (?, ?, '대여중', NOW(), NOW())"
                    : "INSERT INTO game_rental (game_id, usage_id, status, request_time) VALUES (?, ?, '예약', NOW())";
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, gameId);
                ps.setInt(2, usageId);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    return keys.getInt(1);
                }
            }
        }
    }

    public String findStatus(int rentalId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT status FROM game_rental WHERE rental_id = ?")) {
            ps.setInt(1, rentalId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    /** 예약 대기 순서 (같은 게임의 '예약' 중 몇 번째인지) */
    public int queuePosition(int rentalId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM game_rental a JOIN game_rental b ON b.game_id = a.game_id"
                   + " WHERE a.rental_id = ? AND b.status = '예약' AND b.request_time <= a.request_time";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, rentalId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /**
     * 반납 (트랜잭션): 그 기록을 '반납'으로 바꾸고, 같은 게임의 가장 먼저 신청한 예약을 '호출'로 바꾼다.
     * 호출한 룸 이름을 돌려준다 (예약이 없으면 null).
     */
    public String returnGame(int rentalId) throws SQLException {
        Connection con = DBUtil.getConnection();
        try {
            con.setAutoCommit(false);
            int gameId;
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE game_rental SET status = '반납', return_time = NOW() WHERE rental_id = ? AND status = '대여중'")) {
                ps.setInt(1, rentalId);
                if (ps.executeUpdate() != 1) throw new SQLException("대여 중인 기록이 아닙니다.");
            }
            try (PreparedStatement ps = con.prepareStatement("SELECT game_id FROM game_rental WHERE rental_id = ?")) {
                ps.setInt(1, rentalId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    gameId = rs.getInt(1);
                }
            }

            String calledRoom = null;
            String sql = "SELECT r.rental_id, t.table_name FROM game_rental r"
                       + " JOIN table_usage u ON u.usage_id = r.usage_id JOIN cafe_table t ON t.table_no = u.table_no"
                       + " WHERE r.game_id = ? AND r.status = '예약' ORDER BY r.request_time LIMIT 1";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, gameId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int nextId = rs.getInt(1);
                        calledRoom = rs.getString(2);
                        try (PreparedStatement up = con.prepareStatement(
                                "UPDATE game_rental SET status = '호출' WHERE rental_id = ?")) {
                            up.setInt(1, nextId);
                            up.executeUpdate();
                        }
                    }
                }
            }
            con.commit();
            return calledRoom;
        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
            con.close();
        }
    }

    /** 호출한 예약 손님에게 게임을 건넸다 → '대여중' */
    public void handOver(int rentalId) throws SQLException {
        updateStatus(rentalId, "UPDATE game_rental SET status = '대여중', start_time = NOW() WHERE rental_id = ? AND status = '호출'");
    }

    /** 예약 취소 */
    public void cancel(int rentalId) throws SQLException {
        updateStatus(rentalId, "UPDATE game_rental SET status = '취소' WHERE rental_id = ? AND status IN ('예약', '호출')");
    }

    private void updateStatus(int rentalId, String sql) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, rentalId);
            if (ps.executeUpdate() == 0) throw new SQLException("이미 상태가 바뀐 기록입니다. 화면을 새로 고치세요.");
        }
    }

    /** 이 이용 건이 아직 반납하지 않은 게임 이름들 (⑤ 이용 종료에서 확인) */
    public List<String> rentingGames(int usageId) throws SQLException {
        List<String> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT g.name FROM game_rental r JOIN game g ON g.game_id = r.game_id WHERE r.usage_id = ? AND r.status = '대여중'")) {
            ps.setInt(1, usageId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getString(1));
            }
        }
        return list;
    }
}
