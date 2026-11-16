package dicepos.table;

import java.time.LocalDate;

/** fee_setting (이용 요금 설정) 한 줄을 담는 그릇 */
public class FeeSettingDTO {
    private int feeId;
    private int baseMinutes;
    private int basePrice;
    private int extraUnit;
    private int extraPrice;
    private LocalDate applyDate;

    public FeeSettingDTO(int feeId, int baseMinutes, int basePrice, int extraUnit, int extraPrice, LocalDate applyDate) {
        this.feeId = feeId;
        this.baseMinutes = baseMinutes;
        this.basePrice = basePrice;
        this.extraUnit = extraUnit;
        this.extraPrice = extraPrice;
        this.applyDate = applyDate;
    }

    public int getFeeId() {
        return feeId;
    }

    public int getBaseMinutes() {
        return baseMinutes;
    }

    public int getBasePrice() {
        return basePrice;
    }

    public int getExtraUnit() {
        return extraUnit;
    }

    public int getExtraPrice() {
        return extraPrice;
    }

    public LocalDate getApplyDate() {
        return applyDate;
    }
}
