package dicepos.menu;

/** ※ 임시 파일 (④ 박준영 것). ④가 진짜를 만들면 지운다. 진짜도 아래 get 메서드는 같은 이름으로 둔다. */
public class MenuDTO {
    private final int menuId;
    private final String name;
    private final String category;
    private final int price;
    private final int stock;
    private final boolean soldOut;

    public MenuDTO(int menuId, String name, String category, int price, int stock, boolean soldOut) {
        this.menuId = menuId;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.soldOut = soldOut;
    }

    public int getMenuId() { return menuId; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public int getPrice() { return price; }
    public int getStock() { return stock; }
    public boolean isSoldOut() { return soldOut; }
}
