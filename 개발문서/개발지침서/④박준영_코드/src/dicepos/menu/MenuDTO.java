package dicepos.menu;

/** menu 테이블의 한 줄을 담는 그릇. */
public class MenuDTO {
    private int menuId;
    private String name;
    private String category;
    private int price;
    private int stock;
    private boolean soldOut;

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
