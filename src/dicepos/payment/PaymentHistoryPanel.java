package dicepos.payment;

import dicepos.common.Refreshable;
import dicepos.common.UIUtil;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/** [직원] 결제 내역 조회 (② 송하안) */
public class PaymentHistoryPanel extends JPanel implements Refreshable {
    private final JTextField fromField = new JTextField(LocalDate.now().toString(), 9);
    private final JTextField toField = new JTextField(LocalDate.now().toString(), 9);
    private final JComboBox<String> methodCombo = new JComboBox<>(new String[] {"전체", "카드", "현금", "포인트"});
    private final DefaultTableModel model = UIUtil.tableModel(
            "결제 번호", "일시", "테이블", "회원", "총액", "카드", "현금", "포인트", "처리 직원");
    private final JTable table = UIUtil.table(model);
    private final JTextArea receipt = new JTextArea();
    private final PaymentDAO dao = new PaymentDAO();
    private List<Object[]> rows = new ArrayList<>();

    public PaymentHistoryPanel() {
        super(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton searchButton = new JButton("조회");
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("기간"));
        top.add(fromField);
        top.add(new JLabel("~"));
        top.add(toField);
        top.add(new JLabel("  결제 수단"));
        top.add(methodCombo);
        top.add(searchButton);
        top.add(new JLabel("   표의 결제를 누르면 아래에 영수증이 나옵니다."));

        receipt.setEditable(false);
        receipt.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(table), new JScrollPane(receipt));
        split.setResizeWeight(0.55);

        add(top, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);

        searchButton.addActionListener(e -> refresh());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showReceipt();
            }
        });
        refresh();
    }

    @Override
    public void refresh() {
        LocalDate from = UIUtil.toDate(fromField.getText());
        LocalDate to = UIUtil.toDate(toField.getText());
        if (from == null || to == null || from.isAfter(to)) {
            UIUtil.warn(this, "기간을 2026-11-01 ~ 2026-11-30 처럼 바르게 입력하세요.");
            return;
        }
        try {
            rows = dao.findList(from, to, (String) methodCombo.getSelectedItem());
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
            return;
        }
        model.setRowCount(0);
        for (Object[] r : rows) {
            model.addRow(new Object[] {r[0], UIUtil.dateTime((LocalDateTime) r[1]), r[2], r[3], UIUtil.won((Integer) r[4]),
                    UIUtil.won((Integer) r[5]), UIUtil.won((Integer) r[6]), String.format("%,dP", (Integer) r[7]), r[9]});
        }
        receipt.setText("");
    }

    /** 고른 결제의 상세를 영수증처럼 보여 준다 */
    private void showReceipt() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        Object[] r = rows.get(row);
        StringBuilder sb = new StringBuilder();
        sb.append("========== 다이스 POS 영수증 ==========\n");
        sb.append("결제 번호 ").append(r[0]).append("   ").append(UIUtil.dateTime((LocalDateTime) r[1])).append('\n');
        sb.append("테이블 ").append(r[2]).append("   회원 ").append(r[3]).append("   직원 ").append(r[9]).append("\n");
        sb.append("---------------------------------------\n");
        try {
            for (Object[] d : dao.findDetails((Integer) r[0])) {
                sb.append(String.format("%-12s %3d x %,7d = %,8d%n", d[1], (Integer) d[2], (Integer) d[3], (Integer) d[4]));
            }
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
            return;
        }
        sb.append("---------------------------------------\n");
        sb.append(String.format("합계                         %,8d원%n", (Integer) r[4]));
        sb.append(String.format("  카드 %,d원 / 현금 %,d원 / 포인트 %,dP%n", (Integer) r[5], (Integer) r[6], (Integer) r[7]));
        sb.append(String.format("  적립 포인트 %,dP%n", (Integer) r[8]));
        receipt.setText(sb.toString());
    }
}
