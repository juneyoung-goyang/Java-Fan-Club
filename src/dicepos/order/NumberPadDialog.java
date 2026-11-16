package dicepos.order;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/** 수량 변경용 숫자 키패드 (⑥ 김하진). 숫자 버튼 12개에 리스너 하나를 같이 쓴다. */
public class NumberPadDialog extends JDialog {
    private final JTextField display = new JTextField(6);
    private Integer result;

    /** 입력한 숫자, 취소하면 null */
    public static Integer ask(Component parent, String title, int start) {
        NumberPadDialog d = new NumberPadDialog(parent, title, start);
        d.setVisible(true);
        return d.result;
    }

    private NumberPadDialog(Component parent, String title, int start) {
        super(SwingUtilities.getWindowAncestor(parent), title, ModalityType.APPLICATION_MODAL);
        display.setText(String.valueOf(start));
        display.setHorizontalAlignment(SwingConstants.RIGHT);
        display.setFont(display.getFont().deriveFont(20f));
        display.setEditable(false);

        JPanel keys = new JPanel(new GridLayout(4, 3, 4, 4));
        String[] labels = {"7", "8", "9", "4", "5", "6", "1", "2", "3", "지움", "0", "확인"};
        for (String label : labels) {
            JButton b = new JButton(label);
            b.addActionListener(e -> press(e.getActionCommand()));
            keys.add(b);
        }
        JPanel root = new JPanel(new BorderLayout(0, 6));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(display, BorderLayout.NORTH);
        root.add(keys, BorderLayout.CENTER);
        setContentPane(root);
        pack();
        setLocationRelativeTo(parent);
    }

    private void press(String key) {
        String text = display.getText();
        if (key.equals("지움")) {
            display.setText("");
        } else if (key.equals("확인")) {
            result = text.isEmpty() ? null : Integer.valueOf(text);
            dispose();
        } else if (text.length() < 3) {   // 999까지
            display.setText(text.equals("0") ? key : text + key);
        }
    }
}
