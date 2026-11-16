package dicepos.order;

/** order_detail (주문 상세) + 메뉴 이름(JOIN) 한 줄을 담는 그릇 */
public class OrderDetailDTO {
    private int orderDetailId;
    private int orderId;
    private int menuId;
    private int qty;
    private int unitPrice;
    private int subtotal;
    private String menuName;

    public OrderDetailDTO(int orderDetailId, int orderId, int menuId, int qty, int unitPrice, int subtotal, String menuName) {
        this.orderDetailId = orderDetailId;
        this.orderId = orderId;
        this.menuId = menuId;
        this.qty = qty;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
        this.menuName = menuName;
    }

    public int getOrderDetailId() {
        return orderDetailId;
    }

    public int getOrderId() {
        return orderId;
    }

    public int getMenuId() {
        return menuId;
    }

    public int getQty() {
        return qty;
    }

    public int getUnitPrice() {
        return unitPrice;
    }

    public int getSubtotal() {
        return subtotal;
    }

    public String getMenuName() {
        return menuName;
    }
}
