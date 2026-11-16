package dicepos.staff;

import dicepos.common.UIUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * 근무자 확인(출근·퇴근·잠금 해제)과 관리자 확인 팝업. (① 김재민)
 * 비밀번호를 3번 틀리면 30초 동안 입력할 수 없다. 틀린 횟수는 모든 팝업이 같이 센다.
 */
public class CheckDialog extends JDialog {
    public static final int CLOCK_IN = 1;    // 출근
    public static final int CLOCK_OUT = 2;   // 퇴근
    public static final int UNLOCK = 3;      // 근무자 확인 (화면 잠금 해제, 근무자 변경)
    public static final int ADMIN = 4;       // 관리자 확인

    private static int failCount = 0;
    private static long lockedUntil = 0;     // 이 시각(밀리초)까지 입력 금지

    private final int mode;
    private final JTextField idField = new JTextField(15);
    private final JPasswordField pwField = new JPasswordField(15);
    private final JLabel message = new JLabel(" ");
    private final JButton okButton = new JButton("확인");
    private final Timer lockTimer;
    private final StaffDAO staffDAO = new StaffDAO();
    private final WorkLogDAO workLogDAO = new WorkLogDAO();
    private StaffDTO result;

    /** 팝업을 띄우고, 확인에 성공하면 그 직원을, 취소하면 null을 돌려준다. */
    public static StaffDTO show(Component parent, int mode) {
        CheckDialog dialog = new CheckDialog(parent, mode);
        dialog.setVisible(true);   // 모달이라 창이 닫힐 때까지 여기서 기다린다
        return dialog.result;
    }

    private CheckDialog(Component parent, int mode) {
        super(parent == null ? null : SwingUtilities.getWindowAncestor(parent), titleOf(mode), ModalityType.APPLICATION_MODAL);
        this.mode = mode;

        JLabel guide = new JLabel(guideOf(mode));
        guide.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JPanel form = new JPanel(new GridLayout(2, 2, 5, 5));
        form.add(new JLabel(mode == ADMIN ? "관리자 아이디" : "아이디"));
        form.add(idField);
        form.add(new JLabel("비밀번호"));
        form.add(pwField);

        message.setForeground(Color.RED);
        JPanel center = new JPanel(new BorderLayout());
        center.add(form, BorderLayout.CENTER);
        center.add(message, BorderLayout.SOUTH);

        JButton cancel = new JButton("취소");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(okButton);
        buttons.add(cancel);

        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(guide, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);

        okButton.addActionListener(e -> check());
        pwField.addActionListener(e -> check());   // 비밀번호 칸에서 Enter
        idField.addActionListener(e -> pwField.requestFocus());
        cancel.addActionListener(e -> dispose());

        // 잠겨 있는 동안 1초마다 남은 시간을 보여 준다
        lockTimer = new Timer(1000, e -> showLockState());
        showLockState();
        if (isLocked()) lockTimer.start();

        getRootPane().setDefaultButton(okButton);
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    private static String titleOf(int mode) {
        switch (mode) {
            case CLOCK_IN: return "근무자 확인 (출근)";
            case CLOCK_OUT: return "근무자 확인 (퇴근)";
            case ADMIN: return "관리자 확인";
            default: return "근무자 확인";
        }
    }

    private static String guideOf(int mode) {
        switch (mode) {
            case CLOCK_IN: return "출근할 직원의 아이디와 비밀번호를 입력하세요.";
            case CLOCK_OUT: return "퇴근할 직원의 아이디와 비밀번호를 입력하세요.";
            case ADMIN: return "관리자 화면은 관리자만 열 수 있습니다.";
            default: return "근무 중인 직원의 아이디와 비밀번호로 잠금을 풉니다.";
        }
    }

    private static boolean isLocked() {
        return System.currentTimeMillis() < lockedUntil;
    }

    private void showLockState() {
        boolean locked = isLocked();
        idField.setEnabled(!locked);
        pwField.setEnabled(!locked);
        okButton.setEnabled(!locked);
        if (locked) {
            long seconds = (lockedUntil - System.currentTimeMillis() + 999) / 1000;
            message.setText("비밀번호를 3번 틀렸습니다. " + seconds + "초 후에 다시 입력하세요.");
        } else if (lockTimer != null && lockTimer.isRunning()) {
            lockTimer.stop();
            message.setText(" ");
            pwField.requestFocus();
        }
    }

    private void check() {
        if (isLocked()) return;
        String loginId = idField.getText().trim();
        String password = new String(pwField.getPassword());
        if (loginId.isEmpty() || password.isEmpty()) {
            message.setText("아이디와 비밀번호를 모두 입력하세요.");
            return;
        }
        try {
            StaffDTO staff = staffDAO.findByLoginId(loginId);
            if (staff == null || !PasswordUtil.hash(password, staff.getSalt()).equals(staff.getPassword())) {
                fail();
                return;
            }
            failCount = 0;
            if ("퇴사".equals(staff.getStatus())) {
                message.setText("퇴사한 직원은 사용할 수 없습니다.");
                return;
            }
            if (afterPasswordOk(staff)) {
                result = staff;
                dispose();
            }
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }

    /** 비밀번호가 맞은 다음 할 일. 팝업을 닫아도 되면 true */
    private boolean afterPasswordOk(StaffDTO staff) throws SQLException {
        WorkLogDTO open = workLogDAO.findOpen(staff.getStaffId());
        switch (mode) {
            case CLOCK_IN:
                if (open != null) {
                    UIUtil.info(this, staff.getName() + "님은 이미 출근한 상태입니다. (출근 " + UIUtil.dateTime(open.getClockIn()) + ")");
                    return true;
                }
                workLogDAO.clockIn(staff.getStaffId());
                UIUtil.info(this, staff.getName() + "님 출근을 기록했습니다.");
                return true;
            case CLOCK_OUT:
                if (open == null) {
                    message.setText("출근 기록이 없습니다.");
                    return false;
                }
                int minutes = workLogDAO.clockOut(open);
                UIUtil.info(this, staff.getName() + "님 퇴근을 기록했습니다. (실근무 " + UIUtil.minutes(minutes) + ")");
                return true;
            case UNLOCK:
                if (open == null) {
                    message.setText("근무 중인 직원만 POS를 쓸 수 있습니다. 먼저 [출근]을 누르세요.");
                    return false;
                }
                return true;
            default:   // ADMIN
                if (!"관리자".equals(staff.getRole())) {
                    message.setText("관리자 권한이 없는 직원입니다.");
                    return false;
                }
                return true;
        }
    }

    private void fail() {
        failCount++;
        pwField.setText("");
        if (failCount >= 3) {
            failCount = 0;
            lockedUntil = System.currentTimeMillis() + 30_000;
            showLockState();
            lockTimer.start();
        } else {
            message.setText("아이디 또는 비밀번호가 틀렸습니다. (" + failCount + "/3회)");
        }
    }

    @Override
    public void dispose() {
        lockTimer.stop();
        super.dispose();
    }
}
