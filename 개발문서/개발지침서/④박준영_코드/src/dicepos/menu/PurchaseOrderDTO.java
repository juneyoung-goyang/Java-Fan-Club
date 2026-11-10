package dicepos.menu;

/** purchase_order 한 줄 (발주 1건) */
public class PurchaseOrderDTO {
    private int poId;
    private int staffId;
    private String staffName;
    private String supplier;
    private String orderDate;
    private int totalAmount;
    private String status;
    private int itemCount;

    public PurchaseOrderDTO(int poId, int staffId, String staffName, String supplier,
                            String orderDate, int totalAmount, String status, int itemCount) {
        this.poId = poId;
        this.staffId = staffId;
        this.staffName = staffName;
        this.supplier = supplier;
        this.orderDate = orderDate;
        this.totalAmount = totalAmount;
        this.status = status;
        this.itemCount = itemCount;
    }

    public int getPoId() { return poId; }
    public int getStaffId() { return staffId; }
    public String getStaffName() { return staffName; }
    public String getSupplier() { return supplier; }
    public String getOrderDate() { return orderDate; }
    public int getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }
    public int getItemCount() { return itemCount; }
}
