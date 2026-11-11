package dicepos.order;

/**
 * ※ 임시 파일 (⑥ 김하진 것). ⑥이 진짜를 만들면 지운다.
 * 진짜도 필드 순서(= 생성자 순서)와 아래 get 메서드 이름은 같게 둔다. 임시 OrderDAO가 이 생성자를 부른다.
 */
public class OrderDetailDTO {
    private int orderDetailId;
    private int orderId;
    private int menuId;
    private int qty;
    private int unitPrice;
    private int subtotal;
    private String menuName;   // DB 칸이 아니라 menu와 JOIN해서 가져온 메뉴 이름

    public OrderDetailDTO(int orderDetailId, int orderId, int menuId, int qty,
                          int unitPrice, int subtotal, String menuName) {
        this.orderDetailId = orderDetailId;
        this.orderId = orderId;
        this.menuId = menuId;
        this.qty = qty;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
        this.menuName = menuName;
    }

    public int getMenuId() { return menuId; }
    public String getMenuName() { return menuName; }
    public int getQty() { return qty; }
    public int getUnitPrice() { return unitPrice; }
    public int getSubtotal() { return subtotal; }
}
