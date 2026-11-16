package dicepos.table;

import java.time.LocalDateTime;

/** table_usage (테이블 이용) 한 줄을 담는 그릇 */
public class TableUsageDTO {
    private int usageId;
    private int tableNo;
    private Integer memberId;
    private int people;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;

    public TableUsageDTO(int usageId, int tableNo, Integer memberId, int people, LocalDateTime startTime, LocalDateTime endTime, String status) {
        this.usageId = usageId;
        this.tableNo = tableNo;
        this.memberId = memberId;
        this.people = people;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    public int getUsageId() {
        return usageId;
    }

    public int getTableNo() {
        return tableNo;
    }

    public Integer getMemberId() {
        return memberId;
    }

    public int getPeople() {
        return people;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public String getStatus() {
        return status;
    }
}
