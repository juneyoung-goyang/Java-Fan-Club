package dicepos.game;

/** ※ 임시 파일입니다. ③ 김은별이 진짜 GameDTO를 올리면 지웁니다. */
public class GameDTO {
    private final int gameId;
    private final String name;
    private final int stock;
    private final String status;

    public GameDTO(int gameId, String name, int stock, String status) {
        this.gameId = gameId;
        this.name = name;
        this.stock = stock;
        this.status = status;
    }

    public int getGameId() { return gameId; }
    public String getName() { return name; }
    public int getStock() { return stock; }
    public String getStatus() { return status; }
}
