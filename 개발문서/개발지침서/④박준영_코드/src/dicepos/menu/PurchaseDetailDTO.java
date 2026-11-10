package dicepos.menu;

/** purchase_order_detail 한 줄 (발주한 품목 1개) */
public class PurchaseDetailDTO {
    private String itemType;   // "메뉴" 또는 "보드게임"
    private Integer menuId;    // 보드게임이면 null
    private Integer gameId;    // 메뉴면 null
    private String itemName;
    private int qty;
    private int unitCost;

    public PurchaseDetailDTO(String itemType, Integer menuId, Integer gameId,
                             String itemName, int qty, int unitCost) {
        this.itemType = itemType;
        this.menuId = menuId;
        this.gameId = gameId;
        this.itemName = itemName;
        this.qty = qty;
        this.unitCost = unitCost;
    }

    public String getItemType() { return itemType; }
    public Integer getMenuId() { return menuId; }
    public Integer getGameId() { return gameId; }
    public String getItemName() { return itemName; }
    public int getQty() { return qty; }
    public int getUnitCost() { return unitCost; }
    public int getAmount() { return qty * unitCost; }   // 금액 = 수량 × 단가

    public void addQty(int more) { qty += more; }       // 같은 품목을 또 담으면 수량만 더한다
}
