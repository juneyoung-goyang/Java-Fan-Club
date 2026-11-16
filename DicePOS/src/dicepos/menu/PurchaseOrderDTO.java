package dicepos.menu;

import java.time.LocalDateTime;

/** purchase_order (발주) 한 줄을 담는 그릇 */
public class PurchaseOrderDTO {
    private int poId;
    private Integer staffId;
    private String supplier;
    private LocalDateTime orderDate;
    private int totalAmount;
    private String status;
    private LocalDateTime receiveDate;

    public PurchaseOrderDTO(int poId, Integer staffId, String supplier, LocalDateTime orderDate, int totalAmount, String status, LocalDateTime receiveDate) {
        this.poId = poId;
        this.staffId = staffId;
        this.supplier = supplier;
        this.orderDate = orderDate;
        this.totalAmount = totalAmount;
        this.status = status;
        this.receiveDate = receiveDate;
    }

    public int getPoId() {
        return poId;
    }

    public Integer getStaffId() {
        return staffId;
    }

    public String getSupplier() {
        return supplier;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public int getTotalAmount() {
        return totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getReceiveDate() {
        return receiveDate;
    }
}
