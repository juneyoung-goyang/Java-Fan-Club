package dicepos.game;

/**
 * ※ 임시 파일 (③ 김은별 것). ③이 진짜를 만들면 지운다.
 * 진짜도 필드 순서(= 생성자 순서)와 아래 get 메서드 이름은 같게 둔다. 임시 GameDAO가 이 생성자를 부른다.
 */
public class GameDTO {
    private int gameId;
    private String name;
    private String genre;
    private int minPlayer;
    private int maxPlayer;
    private int difficulty;
    private int playMin;
    private int stock;
    private String status;
    private String location;

    public GameDTO(int gameId, String name, String genre, int minPlayer, int maxPlayer,
                   int difficulty, int playMin, int stock, String status, String location) {
        this.gameId = gameId;
        this.name = name;
        this.genre = genre;
        this.minPlayer = minPlayer;
        this.maxPlayer = maxPlayer;
        this.difficulty = difficulty;
        this.playMin = playMin;
        this.stock = stock;
        this.status = status;
        this.location = location;
    }

    public int getGameId() { return gameId; }
    public String getName() { return name; }
    public int getStock() { return stock; }
    public String getStatus() { return status; }
}
