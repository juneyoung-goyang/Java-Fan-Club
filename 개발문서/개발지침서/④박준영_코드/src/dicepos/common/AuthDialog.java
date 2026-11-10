package dicepos.common;

import java.awt.Component;
import javax.swing.JOptionPane;

/**
 * ※ 임시 파일입니다. ① 김재민이 진짜 관리자 확인 팝업을 올리면 이 파일은 지웁니다.
 * 지금은 확인 없이 1번 관리자(김지우)로 통과시킨다.
 */
public class AuthDialog {
    public static boolean confirmAdmin(Component parent) {
        JOptionPane.showMessageDialog(parent,
            "관리자 확인 팝업은 ① 김재민이 만듭니다.\n지금은 임시로 1번 관리자로 통과합니다.");
        Session.unlockAdmin(1);
        return true;
    }
}
