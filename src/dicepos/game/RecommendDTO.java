package dicepos.game;

import java.time.LocalDateTime;

/** recommend (게임 추천 기록) 한 줄을 담는 그릇 */
public class RecommendDTO {
    private int recId;
    private int usageId;
    private Integer memberId;
    private int gameId;
    private LocalDateTime recTime;
    private boolean chosen;

    public RecommendDTO(int recId, int usageId, Integer memberId, int gameId, LocalDateTime recTime, boolean chosen) {
        this.recId = recId;
        this.usageId = usageId;
        this.memberId = memberId;
        this.gameId = gameId;
        this.recTime = recTime;
        this.chosen = chosen;
    }

    public int getRecId() {
        return recId;
    }

    public int getUsageId() {
        return usageId;
    }

    public Integer getMemberId() {
        return memberId;
    }

    public int getGameId() {
        return gameId;
    }

    public LocalDateTime getRecTime() {
        return recTime;
    }

    public boolean isChosen() {
        return chosen;
    }
}
