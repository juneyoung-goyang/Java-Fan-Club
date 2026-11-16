package dicepos.common;

import dicepos.staff.CheckDialog;
import dicepos.staff.StaffDTO;
import java.awt.Component;

/** 관리자 확인 팝업 (① 김재민). 실제 팝업은 dicepos.staff.CheckDialog가 그린다. */
public class AuthDialog {
    /** 관리자 아이디·비밀번호가 맞으면 그 번호를 Session에 기억하고 true */
    public static boolean confirmAdmin(Component parent) {
        StaffDTO admin = CheckDialog.show(parent, CheckDialog.ADMIN);
        if (admin == null) return false;
        Session.unlockAdmin(admin.getStaffId());
        return true;
    }
}
