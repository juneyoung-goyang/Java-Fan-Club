package dicepos.menu;

/** 발주 화면의 품목 콤보박스 한 칸 (메뉴 또는 보드게임). */
public class ItemOption {
    private final String type;   // "메뉴" 또는 "보드게임"
    private final int id;        // menu_id 또는 game_id
    private final String name;

    public ItemOption(String type, int id, String name) {
        this.type = type;
        this.id = id;
        this.name = name;
    }

    public String getType() { return type; }
    public int getId() { return id; }
    public String getName() { return name; }

    @Override
    public String toString() { return name; }   // 콤보박스에 보이는 글자
}
