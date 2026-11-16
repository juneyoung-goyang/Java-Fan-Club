package dicepos.table;

/** cafe_table (카페 테이블) 한 줄을 담는 그릇 */
public class CafeTableDTO {
    private int tableNo;
    private String tableName;
    private int capacity;
    private boolean isActive;

    public CafeTableDTO(int tableNo, String tableName, int capacity, boolean isActive) {
        this.tableNo = tableNo;
        this.tableName = tableName;
        this.capacity = capacity;
        this.isActive = isActive;
    }

    public int getTableNo() {
        return tableNo;
    }

    public String getTableName() {
        return tableName;
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean isActive() {
        return isActive;
    }
}
