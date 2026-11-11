package dicepos.common;

/**
 * ※ 임시 파일 (⑤ 서윤채 것). ⑤가 진짜를 만들면 지운다.
 * 진짜는 fee_setting에서 요금을 읽지만, 임시 파일은 기본 60분 3,500원 / 10분마다 500원으로 고정 계산한다.
 */
public class FeeCalculator {
    /** 1인 요금 = 기본요금 + 올림(초과 분 ÷ 추가 단위) × 추가요금 */
    public static int perPerson(int minutes) {
        int over = Math.max(0, minutes - 60);
        int units = (over + 10 - 1) / 10;   // 올림
        return 3500 + units * 500;
    }

    /** 이용료 = 1인 요금 × 인원 */
    public static int total(int minutes, int people) {
        return perPerson(minutes) * people;
    }
}
