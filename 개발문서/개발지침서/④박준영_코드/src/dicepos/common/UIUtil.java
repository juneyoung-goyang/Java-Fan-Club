package dicepos.common;

import java.awt.Component;
import java.text.DecimalFormat;
import javax.swing.JOptionPane;

/** 안내창과 금액 표시처럼 모든 화면에서 같이 쓰는 기능. */
public class UIUtil {
    private static final DecimalFormat WON = new DecimalFormat("#,###");

    /** 3000 → "3,000원" */
    public static String won(int amount) {
        return WON.format(amount) + "원";
    }

    public static void warn(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "확인", JOptionPane.WARNING_MESSAGE);
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "알림", JOptionPane.INFORMATION_MESSAGE);
    }

    /** [예]를 누르면 true */
    public static boolean confirm(Component parent, String message) {
        int answer = JOptionPane.showConfirmDialog(parent, message, "확인", JOptionPane.YES_NO_OPTION);
        return answer == JOptionPane.YES_OPTION;
    }

    /** "3,000" → 3000, 숫자가 아니면 null */
    public static Integer toInt(String text) {
        try {
            return Integer.parseInt(text.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
