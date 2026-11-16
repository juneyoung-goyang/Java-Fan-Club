package dicepos.table;

import dicepos.common.FeeCalculator;
import dicepos.common.Refreshable;
import dicepos.common.UIUtil;
import dicepos.member.MemberDAO;
import dicepos.member.MemberDTO;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/** [직원] 테이블 현황 (⑤ 서윤채) */
public class TableStatusPanel extends JPanel implements Refreshable {
    private static final int COLUMNS = 5;   // 한 줄에 카드 5개

    private final JPanel grid = new JPanel();
    private final JLabel selectedLabel = new JLabel("카드를 눌러 테이블을 고르세요.");
    private final JLabel timeLabel = new JLabel();
    private final JButton startButton = new JButton("이용 시작");
    private final JButton endButton = new JButton("이용 종료");
    private final JButton feeButton = new JButton("요금 계산");
    private final CafeTableDAO tableDAO = new CafeTableDAO();
    private final TableUsageDAO usageDAO = new TableUsageDAO();
    private final MemberDAO memberDAO = new MemberDAO();
    private TableCard selected;
    private int selectedNo = -1;   // 새로 고쳐도 같은 카드를 다시 고르기 위해 번호를 기억한다

    public TableStatusPanel() {
        super(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton refreshButton = new JButton("새로 고침");
        JPanel top = new JPanel(new BorderLayout());
        top.add(new JLabel("초록 = 빈자리, 주황 = 이용 중, 회색 = 사용불가.  1분마다 자동으로 새로 고칩니다."), BorderLayout.WEST);
        top.add(timeLabel, BorderLayout.EAST);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(selectedLabel);
        bottom.add(startButton);
        bottom.add(endButton);
        bottom.add(feeButton);
        bottom.add(refreshButton);

        JPanel gridHolder = new JPanel(new BorderLayout());   // 카드가 위쪽에 모이도록
        gridHolder.add(grid, BorderLayout.NORTH);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(gridHolder), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        startButton.addActionListener(e -> startUsage());
        endButton.addActionListener(e -> endUsage());
        feeButton.addActionListener(e -> showFee());
        refreshButton.addActionListener(e -> refresh());

        refresh();
        startRefreshThread();
    }

    /** 1분마다 화면을 새로 고치는 스레드. 화면 바꾸기는 Swing 스레드에서 해야 하므로 invokeLater를 쓴다. */
    private void startRefreshThread() {
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60_000);
                } catch (InterruptedException e) {
                    return;
                }
                SwingUtilities.invokeLater(this::refresh);
            }
        });
        thread.setDaemon(true);   // 프로그램을 끄면 같이 끝난다
        thread.start();
    }

    @Override
    public void refresh() {
        List<CafeTableDTO> tables;
        Map<Integer, TableUsageDTO> usingMap = new HashMap<>();
        try {
            tables = tableDAO.findAll();
            for (TableUsageDTO u : usageDAO.findActive()) usingMap.put(u.getTableNo(), u);
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
            return;
        }

        grid.removeAll();
        int rows = (tables.size() + COLUMNS - 1) / COLUMNS;   // 테이블 수에 맞춰 행 수 계산
        grid.setLayout(new GridLayout(Math.max(rows, 1), COLUMNS, 8, 8));
        selected = null;
        for (CafeTableDTO t : tables) {
            TableUsageDTO usage = usingMap.get(t.getTableNo());
            TableCard card = makeCard(t, usage);
            card.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    select(card);
                }
            });
            grid.add(card);
            if (t.getTableNo() == selectedNo) {
                selected = card;
                card.setSelected(true);
            }
        }
        grid.revalidate();
        grid.repaint();
        timeLabel.setText("마지막 갱신 " + java.time.LocalTime.now().withNano(0));
        updateButtons();
    }

    private TableCard makeCard(CafeTableDTO t, TableUsageDTO usage) {
        if (usage == null) return new TableCard(t, null, null, 0, 0);
        String memberName = null;
        int minutes = TableUsageDAO.minutesUsed(usage);
        int fee = 0;
        try {
            if (usage.getMemberId() != null) {
                MemberDTO m = memberDAO.findById(usage.getMemberId());
                if (m != null) memberName = m.getName();
            }
            fee = FeeCalculator.total(minutes, usage.getPeople());
        } catch (SQLException e) {
            // 카드 하나 그리기 실패로 전체를 멈추지 않는다. 요금은 0으로 보인다.
        }
        return new TableCard(t, usage, memberName, minutes, fee);
    }

    private void select(TableCard card) {
        if (!card.getTable().isActive()) {
            UIUtil.warn(this, card.getTable().getTableName() + "은(는) 사용불가 룸입니다.");
            return;
        }
        if (selected != null) selected.setSelected(false);
        selected = card;
        selectedNo = card.getTable().getTableNo();
        card.setSelected(true);
        updateButtons();
    }

    private void updateButtons() {
        boolean using = selected != null && selected.getUsage() != null;
        startButton.setEnabled(selected != null && !using);
        endButton.setEnabled(using);
        feeButton.setEnabled(using);
        if (selected == null) {
            selectedLabel.setText("카드를 눌러 테이블을 고르세요.");
            selectedNo = -1;
        } else {
            selectedLabel.setText("고른 테이블: " + selected.getTable().getTableName() + "   ");
        }
    }

    private void startUsage() {
        if (selected == null) return;
        if (StartUsageDialog.open(this, selected.getTable())) refresh();
    }

    private void endUsage() {
        if (selected == null || selected.getUsage() == null) return;
        EndUsageDialog.open(this, selected.getTable(), selected.getUsage().getUsageId());
        refresh();   // 결제가 끝났으면 빈자리로 바뀐다
    }

    /** 지금까지의 요금만 계산한다. DB는 바꾸지 않는다. */
    private void showFee() {
        TableUsageDTO u = selected.getUsage();
        int minutes = TableUsageDAO.minutesUsed(u);
        try {
            int perPerson = FeeCalculator.perPerson(minutes);
            UIUtil.info(this, selected.getTable().getTableName() + "\n이용 시간: " + UIUtil.minutes(minutes)
                    + "\n1인 요금: " + UIUtil.won(perPerson) + " × " + u.getPeople() + "명"
                    + "\n지금까지 이용료: " + UIUtil.won(perPerson * u.getPeople()));
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }
}
