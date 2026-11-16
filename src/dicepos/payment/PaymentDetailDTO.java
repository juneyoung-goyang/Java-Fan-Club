package dicepos.payment;

/** payment_detail (결제 상세) 한 줄을 담는 그릇 */
public class PaymentDetailDTO {
    private int payDetailId;
    private int payId;
    private String itemType;
    private Integer menuId;
    private int qty;
    private int unitPrice;
    private int subtotal;

    public PaymentDetailDTO(int payDetailId, int payId, String itemType, Integer menuId, int qty, int unitPrice, int subtotal) {
        this.payDetailId = payDetailId;
        this.payId = payId;
        this.itemType = itemType;
        this.menuId = menuId;
        this.qty = qty;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
    }

    public int getPayDetailId() {
        return payDetailId;
    }

    public int getPayId() {
        return payId;
    }

    public String getItemType() {
        return itemType;
    }

    public Integer getMenuId() {
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
}
