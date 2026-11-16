package dicepos.game;

import java.time.LocalDateTime;

/** game_rental (게임 대여·예약) 한 줄을 담는 그릇 */
public class GameRentalDTO {
    private int rentalId;
    private int gameId;
    private int usageId;
    private String status;
    private LocalDateTime requestTime;
    private LocalDateTime startTime;
    private LocalDateTime returnTime;

    public GameRentalDTO(int rentalId, int gameId, int usageId, String status, LocalDateTime requestTime, LocalDateTime startTime, LocalDateTime returnTime) {
        this.rentalId = rentalId;
        this.gameId = gameId;
        this.usageId = usageId;
        this.status = status;
        this.requestTime = requestTime;
        this.startTime = startTime;
        this.returnTime = returnTime;
    }

    public int getRentalId() {
        return rentalId;
    }

    public int getGameId() {
        return gameId;
    }

    public int getUsageId() {
        return usageId;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getRequestTime() {
        return requestTime;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getReturnTime() {
        return returnTime;
    }
}
