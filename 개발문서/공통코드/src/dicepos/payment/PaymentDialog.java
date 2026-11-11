package dicepos.payment;

import dicepos.common.UIUtil;
import java.awt.Component;

/** ※ 임시 파일 (② 송하안 것). ②가 진짜를 만들면 지운다. ⑤ 이용 종료 팝업이 부른다. */
public class PaymentDialog {
    /** 결제 팝업을 열고, 결제가 끝나면 true */
    public static boolean open(Component parent, int usageId) {
        UIUtil.info(parent, "결제 팝업은 ② 송하안이 만듭니다. (이용 번호 " + usageId + ")");
        return false;
    }
}
