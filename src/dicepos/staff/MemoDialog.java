package dicepos.staff;

import dicepos.common.UIUtil;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/** 직원 메모 팝업 (① 김재민) */
public class MemoDialog extends JDialog {
    private final JTextArea area = new JTextArea(10, 36);
    private final JLabel countLabel = new JLabel();

    public static void open(Component parent, StaffDTO staff) {
        new MemoDialog(parent, staff).setVisible(true);
    }

    private MemoDialog(Component parent, StaffDTO staff) {
        super(SwingUtilities.getWindowAncestor(parent), "직원 메모 — " + staff.getName(), ModalityType.APPLICATION_MODAL);
        area.setLineWrap(true);
        area.setText(staff.getMemo() == null ? "" : staff.getMemo());
        updateCount();
        area.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                updateCount();
            }
        });

        JButton save = new JButton("저장");
        JButton close = new JButton("닫기");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(countLabel);
        buttons.add(save);
        buttons.add(close);

        JPanel root = new JPanel(new BorderLayout(0, 6));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(new JLabel("근무 특이사항, 연락 메모 등을 적습니다. (500자까지)"), BorderLayout.NORTH);
        root.add(new JScrollPane(area), BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);

        save.addActionListener(e -> {
            String memo = area.getText().trim();
            if (memo.length() > 500) {
                UIUtil.warn(this, "메모는 500자까지 쓸 수 있습니다.");
                return;
            }
            try {
                new StaffDAO().updateMemo(staff.getStaffId(), memo.isEmpty() ? null : memo);
                UIUtil.info(this, "메모를 저장했습니다.");
                dispose();
            } catch (SQLException ex) {
                UIUtil.dbError(this, ex);
            }
        });
        close.addActionListener(e -> dispose());
        pack();
        setLocationRelativeTo(parent);
    }

    private void updateCount() {
        countLabel.setText(area.getText().length() + " / 500자");
    }
}
