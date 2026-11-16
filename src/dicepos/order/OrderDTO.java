package dicepos.order;

import java.time.LocalDateTime;

/** orders (주문) 한 줄을 담는 그릇 */
public class OrderDTO {
    private int orderId;
    private int usageId;
    private Integer staffId;
    private String channel;
    private LocalDateTime orderTime;
    private int totalAmount;
    private String status;

    public OrderDTO(int orderId, int usageId, Integer staffId, String channel, LocalDateTime orderTime, int totalAmount, String status) {
        this.orderId = orderId;
        this.usageId = usageId;
        this.staffId = staffId;
        this.channel = channel;
        this.orderTime = orderTime;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    public int getOrderId() {
        return orderId;
    }

    public int getUsageId() {
        return usageId;
    }

    public Integer getStaffId() {
        return staffId;
    }

    public String getChannel() {
        return channel;
    }

    public LocalDateTime getOrderTime() {
        return orderTime;
    }

    public int getTotalAmount() {
        return totalAmount;
    }

    public String getStatus() {
        return status;
    }
}
