package dicepos.common;

import java.awt.Component;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/** 안내창과 금액 표시처럼 모든 화면에서 같이 쓰는 기능. */
public class UIUtil {
    private static final DecimalFormat WON = new DecimalFormat("#,###");
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

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

    /** "2026-11-03" → LocalDate, 형식이 틀리면 null */
    public static LocalDate toDate(String text) {
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** 2026-11-03T21:20 → "2026-11-03 21:20", null이면 "" */
    public static String dateTime(LocalDateTime t) {
        return t == null ? "" : t.format(DT);
    }

    /** 21:20 처럼 시각만, null이면 "" */
    public static String time(LocalDateTime t) {
        return t == null ? "" : t.format(TIME);
    }

    /** 130 → "2시간 10분" */
    public static String minutes(int minutes) {
        if (minutes < 60) return minutes + "분";
        return (minutes / 60) + "시간 " + (minutes % 60) + "분";
    }

    /** DB 오류를 안내창으로 보여 준다. */
    public static void dbError(Component parent, Exception e) {
        warn(parent, "DB 작업 중 오류가 났습니다.\n" + e.getMessage());
    }

    /** 사용자가 직접 고칠 수 없는 표 모델 */
    public static DefaultTableModel tableModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    /** 한 줄만 고를 수 있는 표 */
    public static JTable table(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(22);
        table.getTableHeader().setReorderingAllowed(false);
        return table;
    }
}
