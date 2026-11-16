package dicepos.menu;

/** purchase_order_detail (발주 상세) 한 줄을 담는 그릇 */
public class PurchaseDetailDTO {
    private int poDetailId;
    private int poId;
    private String itemType;
    private Integer menuId;
    private Integer gameId;
    private int qty;
    private int unitCost;
    private int amount;

    public PurchaseDetailDTO(int poDetailId, int poId, String itemType, Integer menuId, Integer gameId, int qty, int unitCost, int amount) {
        this.poDetailId = poDetailId;
        this.poId = poId;
        this.itemType = itemType;
        this.menuId = menuId;
        this.gameId = gameId;
        this.qty = qty;
        this.unitCost = unitCost;
        this.amount = amount;
    }

    public int getPoDetailId() {
        return poDetailId;
    }

    public int getPoId() {
        return poId;
    }

    public String getItemType() {
        return itemType;
    }

    public Integer getMenuId() {
        return menuId;
    }

    public Integer getGameId() {
        return gameId;
    }

    public int getQty() {
        return qty;
    }

    public int getUnitCost() {
        return unitCost;
    }

    public int getAmount() {
        return amount;
    }
}
