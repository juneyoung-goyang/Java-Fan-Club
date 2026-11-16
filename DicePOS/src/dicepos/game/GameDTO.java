package dicepos.game;

/** game (보드게임) 한 줄을 담는 그릇 */
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

    public GameDTO(int gameId, String name, String genre, int minPlayer, int maxPlayer, int difficulty, int playMin, int stock, String status, String location) {
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

    public int getGameId() {
        return gameId;
    }

    public String getName() {
        return name;
    }

    public String getGenre() {
        return genre;
    }

    public int getMinPlayer() {
        return minPlayer;
    }

    public int getMaxPlayer() {
        return maxPlayer;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public int getPlayMin() {
        return playMin;
    }

    public int getStock() {
        return stock;
    }

    public String getStatus() {
        return status;
    }

    public String getLocation() {
        return location;
    }
}
