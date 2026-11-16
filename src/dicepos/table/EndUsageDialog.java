package dicepos.table;

import dicepos.common.FeeCalculator;
import dicepos.common.UIUtil;
import dicepos.game.GameRentalDAO;
import dicepos.order.OrderDAO;
import dicepos.payment.PaymentDialog;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** 이용 종료 팝업: 이용 시간과 요금을 보여 주고 ② 결제 팝업을 연다. (⑤ 서윤채) */
public class EndUsageDialog extends JDialog {
    private final int usageId;

    public static void open(Component parent, CafeTableDTO table, int usageId) {
        EndUsageDialog d;
        try {
            d = new EndUsageDialog(parent, table, usageId);
        } catch (SQLException e) {
            UIUtil.dbError(parent, e);
            return;
        }
        d.setVisible(true);
    }

    private EndUsageDialog(Component parent, CafeTableDTO table, int usageId) throws SQLException {
        super(SwingUtilities.getWindowAncestor(parent), "이용 종료 — " + table.getTableName(), ModalityType.APPLICATION_MODAL);
        this.usageId = usageId;

        TableUsageDTO u = new TableUsageDAO().findById(usageId);
        int minutes = TableUsageDAO.minutesUsed(u);
        int perPerson = FeeCalculator.perPerson(minutes);
        int fee = perPerson * u.getPeople();
        int food = new OrderDAO().totalByUsage(usageId);
        List<String> renting = new GameRentalDAO().rentingGames(usageId);

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("입장 시각"));
        form.add(new JLabel(UIUtil.dateTime(u.getStartTime())));
        form.add(new JLabel("이용 시간"));
        form.add(new JLabel(UIUtil.minutes(minutes)));
        form.add(new JLabel("인원"));
        form.add(new JLabel(u.getPeople() + "명"));
        form.add(new JLabel("이용료"));
        form.add(new JLabel(UIUtil.won(perPerson) + " × " + u.getPeople() + "명 = " + UIUtil.won(fee)));
        form.add(new JLabel("음식 금액"));
        form.add(new JLabel(UIUtil.won(food)));
        form.add(new JLabel("결제할 금액"));
        JLabel totalLabel = new JLabel(UIUtil.won(fee + food));
        totalLabel.setFont(totalLabel.getFont().deriveFont(16f));
        form.add(totalLabel);

        JLabel warn = new JLabel(" ");
        warn.setForeground(Color.RED);
        JButton payButton = new JButton("결제하기");
        if (!renting.isEmpty()) {
            warn.setText("반납하지 않은 게임이 있습니다: " + String.join(", ", renting) + ". 먼저 게임 대여·예약에서 반납하세요.");
            payButton.setEnabled(false);
        }
        JButton close = new JButton("닫기");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(payButton);
        buttons.add(close);

        JPanel south = new JPanel(new BorderLayout());
        south.add(warn, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.SOUTH);

        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(form, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);
        setContentPane(root);

        payButton.addActionListener(e -> {
            if (PaymentDialog.open(this, this.usageId)) dispose();   // 결제가 끝나면 닫는다
        });
        close.addActionListener(e -> dispose());
        pack();
        setLocationRelativeTo(parent);
    }
}
