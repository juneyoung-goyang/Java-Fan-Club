package dicepos.staff;

import java.time.LocalDate;

/** salary (급여) 한 줄을 담는 그릇 */
public class SalaryDTO {
    private int salaryId;
    private int staffId;
    private String payMonth;
    private int workMinutes;
    private int baseAmount;
    private int extraAmount;
    private int totalPay;
    private String status;
    private LocalDate payDate;

    public SalaryDTO(int salaryId, int staffId, String payMonth, int workMinutes, int baseAmount, int extraAmount, int totalPay, String status, LocalDate payDate) {
        this.salaryId = salaryId;
        this.staffId = staffId;
        this.payMonth = payMonth;
        this.workMinutes = workMinutes;
        this.baseAmount = baseAmount;
        this.extraAmount = extraAmount;
        this.totalPay = totalPay;
        this.status = status;
        this.payDate = payDate;
    }

    public int getSalaryId() {
        return salaryId;
    }

    public int getStaffId() {
        return staffId;
    }

    public String getPayMonth() {
        return payMonth;
    }

    public int getWorkMinutes() {
        return workMinutes;
    }

    public int getBaseAmount() {
        return baseAmount;
    }

    public int getExtraAmount() {
        return extraAmount;
    }

    public int getTotalPay() {
        return totalPay;
    }

    public String getStatus() {
        return status;
    }

    public LocalDate getPayDate() {
        return payDate;
    }
}
