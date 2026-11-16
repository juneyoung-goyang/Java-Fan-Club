package dicepos.order;

/** 주문 대기 목록·장바구니의 한 줄 (아직 DB에 저장하기 전) */
public class CartItem {
    private final int menuId;
    private final String name;
    private final int price;
    private int qty;

    public CartItem(int menuId, String name, int price, int qty) {
        this.menuId = menuId;
        this.name = name;
        this.price = price;
        this.qty = qty;
    }

    public int getMenuId() { return menuId; }
    public String getName() { return name; }
    public int getPrice() { return price; }
    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }
    public int getSubtotal() { return price * qty; }

    /** JList에 보이는 글자 */
    @Override
    public String toString() {
        return String.format("%s  x %d  =  %,d원", name, qty, getSubtotal());
    }
}
