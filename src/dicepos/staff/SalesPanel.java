package dicepos.staff;

import dicepos.common.Refreshable;
import dicepos.common.UIUtil;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/** [관리자] 매출 관리 (① 김재민) */
public class SalesPanel extends JPanel implements Refreshable {
    private final JRadioButton dailyRadio = new JRadioButton("일별", true);
    private final JRadioButton monthlyRadio = new JRadioButton("월별");
    private final JTextField fromField = new JTextField(LocalDate.now().withDayOfMonth(1).toString(), 9);
    private final JTextField toField = new JTextField(LocalDate.now().toString(), 9);

    private final JLabel countLabel = new JLabel("0건");
    private final JLabel totalLabel = new JLabel("0원");
    private final JLabel avgLabel = new JLabel("0원");
    private final JLabel methodLabel = new JLabel("-");
    private final JLabel ratioLabel = new JLabel("이용료 / 메뉴 비율: -");

    private final DefaultTableModel periodModel = UIUtil.tableModel("기간", "결제 건수", "매출", "카드", "현금", "포인트");
    private final DefaultTableModel rankModel = UIUtil.tableModel("순위", "메뉴", "판매 수량", "판매 금액");
    private final BarChart chart = new BarChart();
    private final SalesDAO dao = new SalesDAO();

    public SalesPanel() {
        super(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        ButtonGroup group = new ButtonGroup();
        group.add(dailyRadio);
        group.add(monthlyRadio);
        JButton searchButton = new JButton("조회");
        JPanel condition = new JPanel(new FlowLayout(FlowLayout.LEFT));
        condition.add(dailyRadio);
        condition.add(monthlyRadio);
        condition.add(new JLabel("   기간"));
        condition.add(fromField);
        condition.add(new JLabel("~"));
        condition.add(toField);
        condition.add(searchButton);

        // 위쪽 숫자 카드 4개
        JPanel cards = new JPanel(new GridLayout(1, 4, 8, 0));
        cards.add(card("결제 건수", countLabel));
        cards.add(card("총 매출", totalLabel));
        cards.add(card("평균 결제 금액", avgLabel));
        cards.add(card("카드 / 현금 / 포인트", methodLabel));

        JPanel north = new JPanel(new BorderLayout(0, 6));
        north.add(condition, BorderLayout.NORTH);
        north.add(cards, BorderLayout.CENTER);

        // 가운데: 표 + 그래프
        JPanel chartBox = new JPanel(new BorderLayout());
        chartBox.setBorder(BorderFactory.createTitledBorder("매출 그래프"));
        chartBox.add(chart);
        JPanel periodBox = new JPanel(new BorderLayout());
        periodBox.setBorder(BorderFactory.createTitledBorder("기간별 매출"));
        periodBox.add(new JScrollPane(UIUtil.table(periodModel)));
        JPanel center = new JPanel(new GridLayout(1, 2, 8, 0));
        center.add(periodBox);
        center.add(chartBox);

        // 아래: 이용료/메뉴 비율과 메뉴 판매 순위
        JPanel rankBox = new JPanel(new BorderLayout());
        rankBox.setBorder(BorderFactory.createTitledBorder("메뉴 판매 순위"));
        rankBox.add(ratioLabel, BorderLayout.NORTH);
        JTable rankTable = UIUtil.table(rankModel);
        JScrollPane rankScroll = new JScrollPane(rankTable);
        rankScroll.setPreferredSize(new java.awt.Dimension(400, 170));
        rankBox.add(rankScroll, BorderLayout.CENTER);

        add(north, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(rankBox, BorderLayout.SOUTH);

        searchButton.addActionListener(e -> refresh());
        // 일별 / 월별을 바꾸면 바로 다시 조회
        dailyRadio.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) refresh();
        });
        monthlyRadio.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) refresh();
        });
        refresh();
    }

    private JPanel card(String title, JLabel value) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        value.setFont(value.getFont().deriveFont(16f));
        p.add(value);
        return p;
    }

    @Override
    public void refresh() {
        LocalDate from = UIUtil.toDate(fromField.getText());
        LocalDate to = UIUtil.toDate(toField.getText());
        if (from == null || to == null) {
            UIUtil.warn(this, "기간을 2026-11-01 처럼 입력하세요.");
            return;
        }
        if (from.isAfter(to)) {
            UIUtil.warn(this, "시작일이 종료일보다 늦습니다.");
            return;
        }
        try {
            showPeriod(dao.byPeriod(from, to, monthlyRadio.isSelected()));
            showRatio(dao.byItemType(from, to));
            showRanking(dao.menuRanking(from, to));
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }

    private void showPeriod(List<Object[]> rows) {
        periodModel.setRowCount(0);
        int count = 0, total = 0, card = 0, cash = 0, point = 0;
        List<String> labels = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        for (Object[] r : rows) {
            count += (Integer) r[1];
            total += (Integer) r[2];
            card += (Integer) r[3];
            cash += (Integer) r[4];
            point += (Integer) r[5];
            periodModel.addRow(new Object[] {r[0], r[1] + "건", UIUtil.won((Integer) r[2]),
                    UIUtil.won((Integer) r[3]), UIUtil.won((Integer) r[4]), UIUtil.won((Integer) r[5])});
            labels.add((String) r[0]);
            values.add((Integer) r[2]);
        }
        countLabel.setText(count + "건");
        totalLabel.setText(UIUtil.won(total));
        avgLabel.setText(UIUtil.won(count == 0 ? 0 : total / count));
        methodLabel.setText(String.format("%,d / %,d / %,d", card, cash, point));
        chart.setData(labels, values);
    }

    private void showRatio(List<Object[]> rows) {
        int fee = 0, menu = 0;
        for (Object[] r : rows) {
            if ("이용료".equals(r[0])) fee = (Integer) r[1];
            else menu = (Integer) r[1];
        }
        int sum = fee + menu;
        if (sum == 0) {
            ratioLabel.setText("이용료 / 메뉴 비율: -");
        } else {
            ratioLabel.setText(String.format("이용료 %s (%d%%)  /  메뉴 %s (%d%%)",
                    UIUtil.won(fee), fee * 100 / sum, UIUtil.won(menu), 100 - fee * 100 / sum));
        }
    }

    private void showRanking(List<Object[]> rows) {
        rankModel.setRowCount(0);
        int rank = 1;
        for (Object[] r : rows) {
            rankModel.addRow(new Object[] {rank++, r[0], r[1] + "개", UIUtil.won((Integer) r[2])});
        }
    }
}
