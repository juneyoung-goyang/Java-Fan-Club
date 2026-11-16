package dicepos.payment;

import java.time.LocalDateTime;

/** payment (결제) 한 줄을 담는 그릇 */
public class PaymentDTO {
    private int payId;
    private int usageId;
    private Integer staffId;
    private Integer memberId;
    private LocalDateTime payTime;
    private int totalAmount;
    private int cardAmount;
    private int cashAmount;
    private int pointAmount;
    private int earnPoint;

    public PaymentDTO(int payId, int usageId, Integer staffId, Integer memberId, LocalDateTime payTime, int totalAmount, int cardAmount, int cashAmount, int pointAmount, int earnPoint) {
        this.payId = payId;
        this.usageId = usageId;
        this.staffId = staffId;
        this.memberId = memberId;
        this.payTime = payTime;
        this.totalAmount = totalAmount;
        this.cardAmount = cardAmount;
        this.cashAmount = cashAmount;
        this.pointAmount = pointAmount;
        this.earnPoint = earnPoint;
    }

    public int getPayId() {
        return payId;
    }

    public int getUsageId() {
        return usageId;
    }

    public Integer getStaffId() {
        return staffId;
    }

    public Integer getMemberId() {
        return memberId;
    }

    public LocalDateTime getPayTime() {
        return payTime;
    }

    public int getTotalAmount() {
        return totalAmount;
    }

    public int getCardAmount() {
        return cardAmount;
    }

    public int getCashAmount() {
        return cashAmount;
    }

    public int getPointAmount() {
        return pointAmount;
    }

    public int getEarnPoint() {
        return earnPoint;
    }
}
