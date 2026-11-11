package dicepos.order;

/** ※ 임시 파일 (⑥ 김하진 것). ⑥이 진짜를 만들면 지운다. 진짜도 아래 get 메서드는 같은 이름으로 둔다. */
public class OrderDetailDTO {
    private final int menuId;
    private final String menuName;
    private final int qty;
    private final int unitPrice;

    public OrderDetailDTO(int menuId, String menuName, int qty, int unitPrice) {
        this.menuId = menuId;
        this.menuName = menuName;
        this.qty = qty;
        this.unitPrice = unitPrice;
    }

    public int getMenuId() { return menuId; }
    public String getMenuName() { return menuName; }
    public int getQty() { return qty; }
    public int getUnitPrice() { return unitPrice; }
    public int getSubtotal() { return qty * unitPrice; }
}
