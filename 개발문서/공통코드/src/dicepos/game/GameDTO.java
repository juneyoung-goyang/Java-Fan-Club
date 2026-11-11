package dicepos.game;

/** ※ 임시 파일 (③ 김은별 것). ③이 진짜를 만들면 지운다. 진짜도 아래 get 메서드는 같은 이름으로 둔다. */
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
