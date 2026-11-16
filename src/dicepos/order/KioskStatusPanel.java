package dicepos.order;

import dicepos.common.FeeCalculator;
import dicepos.common.UIUtil;
import dicepos.main.KioskFrame;
import dicepos.main.KioskScreen;
import dicepos.table.TableUsageDAO;
import dicepos.table.TableUsageDTO;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.time.LocalDateTime;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/** [키오스크] 이용 현황: 이용 시간, 이용 요금, 주문 내역, 예상 요금. 30초마다 다시 계산한다. (⑥ 김하진) */
public class KioskStatusPanel extends JPanel implements KioskScreen {
    private final KioskFrame kiosk;
    private final JLabel startLabel = new JLabel();
    private final JLabel minutesLabel = new JLabel();
    private final JLabel peopleLabel = new JLabel();
    private final JLabel feeLabel = new JLabel();
    private final JLabel foodLabel = new JLabel();
    private final JLabel totalLabel = new JLabel();
    private final JLabel updatedLabel = new JLabel(" ");
    private final DefaultTableModel orderModel = UIUtil.tableModel("메뉴", "수량", "금액", "주문 시각", "상태");
    private TableUsageDTO usage;
    private Thread thread;

    public KioskStatusPanel(KioskFrame kiosk) {
        super(new BorderLayout(10, 10));
        this.kiosk = kiosk;
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        Font big = startLabel.getFont().deriveFont(18f);
        JPanel info = new JPanel(new GridLayout(0, 2, 10, 14));
        info.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        addRow(info, "입장 시각", startLabel, big);
        addRow(info, "이용 시간", minutesLabel, big);
        addRow(info, "인원", peopleLabel, big);
        addRow(info, "이용 요금", feeLabel, big);
        addRow(info, "음식 금액", foodLabel, big);
        addRow(info, "예상 결제 금액", totalLabel, big.deriveFont(Font.BOLD, 22f));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("이용 정보", info);
        tabs.addTab("주문 정보", new JScrollPane(UIUtil.table(orderModel)));

        JButton homeButton = new JButton("처음으로");
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(updatedLabel, BorderLayout.WEST);
        bottom.add(homeButton, BorderLayout.EAST);

        add(tabs, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        homeButton.addActionListener(e -> kiosk.showHome());
    }

    private void addRow(JPanel p, String title, JLabel value, Font font) {
        JLabel t = new JLabel(title);
        t.setFont(font);
        value.setFont(font);
        p.add(t);
        p.add(value);
    }

    /** 화면을 열면 바로 계산하고, 30초마다 다시 계산하는 스레드를 시작한다 */
    @Override
    public void start(TableUsageDTO usage) {
        this.usage = usage;
        update();
        stop();
        thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(30_000);
                } catch (InterruptedException e) {
                    return;   // stop()이 부르면 끝난다
                }
                SwingUtilities.invokeLater(this::update);
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void stop() {
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
    }

    private void update() {
        try {
            TableUsageDTO now = new TableUsageDAO().findById(usage.getUsageId());
            if (!"이용중".equals(now.getStatus())) {   // 그새 결제가 끝났으면 처음 화면으로
                kiosk.showHome();
                return;
            }
            int minutes = TableUsageDAO.minutesUsed(now);
            int perPerson = FeeCalculator.perPerson(minutes);
            int fee = perPerson * now.getPeople();
            OrderDAO orderDAO = new OrderDAO();
            int food = orderDAO.totalByUsage(now.getUsageId());

            startLabel.setText(UIUtil.time(now.getStartTime()));
            minutesLabel.setText(UIUtil.minutes(minutes));
            peopleLabel.setText(now.getPeople() + "명");
            feeLabel.setText(UIUtil.won(perPerson) + " × " + now.getPeople() + "명 = " + UIUtil.won(fee));
            foodLabel.setText(UIUtil.won(food));
            totalLabel.setText(UIUtil.won(fee + food));

            orderModel.setRowCount(0);
            for (Object[] r : orderDAO.findLinesByUsage(now.getUsageId())) {
                orderModel.addRow(new Object[] {r[1], r[2], UIUtil.won((Integer) r[4]), UIUtil.time((LocalDateTime) r[7]),
                        "접수".equals(r[6]) ? "준비 중" : "확인됨"});
            }
            updatedLabel.setText("30초마다 다시 계산합니다. (마지막 " + java.time.LocalTime.now().withNano(0) + ")");
        } catch (SQLException e) {
            updatedLabel.setText("요금을 불러오지 못했습니다: " + e.getMessage());
        }
    }
}
