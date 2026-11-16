package dicepos.game;

import dicepos.common.Refreshable;
import dicepos.common.UIUtil;
import dicepos.table.CafeTableDAO;
import dicepos.table.CafeTableDTO;
import dicepos.table.TableUsageDAO;
import dicepos.table.TableUsageDTO;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

/** [직원] 게임 대여·예약 현황 (③ 김은별). 키오스크에서 들어온 신청도 5초마다 새로 읽는다. */
public class GameRentalPanel extends JPanel implements Refreshable {
    private final DefaultTableModel rentModel = UIUtil.tableModel("번호", "게임", "테이블", "상태", "신청", "받은 시각");
    private final DefaultTableModel waitModel = UIUtil.tableModel("번호", "게임", "테이블", "상태", "신청 시각", "대기 순서");
    private final JTable rentTable = UIUtil.table(rentModel);
    private final JTable waitTable = UIUtil.table(waitModel);
    private final JComboBox<Item> gameCombo = new JComboBox<>();
    private final JComboBox<Item> usageCombo = new JComboBox<>();
    private final GameRentalDAO dao = new GameRentalDAO();
    private List<Object[]> rentRows = new ArrayList<>();
    private List<Object[]> waitRows = new ArrayList<>();

    public GameRentalPanel() {
        super(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton rentButton = new JButton("대여");
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("게임"));
        top.add(gameCombo);
        top.add(new JLabel("  테이블"));
        top.add(usageCombo);
        top.add(rentButton);
        top.add(new JLabel("  (대여 가능하면 바로 대여, 모두 나가 있으면 예약)"));

        JButton returnButton = new JButton("반납");
        JPanel rentBox = new JPanel(new BorderLayout());
        rentBox.setBorder(BorderFactory.createTitledBorder("대여 중 · 호출"));
        rentBox.add(new JScrollPane(rentTable));
        JPanel rentButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton handOverButton = new JButton("전달 완료");
        rentButtons.add(returnButton);
        rentButtons.add(handOverButton);
        rentBox.add(rentButtons, BorderLayout.SOUTH);

        JButton cancelButton = new JButton("예약 취소");
        JPanel waitBox = new JPanel(new BorderLayout());
        waitBox.setBorder(BorderFactory.createTitledBorder("예약 대기 (신청 순서)"));
        waitBox.add(new JScrollPane(waitTable));
        JPanel waitButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        waitButtons.add(cancelButton);
        waitBox.add(waitButtons, BorderLayout.SOUTH);

        JPanel center = new JPanel(new GridLayout(2, 1, 0, 8));
        center.add(rentBox);
        center.add(waitBox);

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(new JLabel("[반납]하면 같은 게임의 첫 번째 예약이 '호출'로 바뀝니다. 게임을 건네고 [전달 완료]를 누르세요."), BorderLayout.SOUTH);

        rentButton.addActionListener(e -> rent());
        returnButton.addActionListener(e -> returnGame());
        handOverButton.addActionListener(e -> handOver());
        cancelButton.addActionListener(e -> cancel());

        // 5초마다 다시 읽는다 (javax.swing.Timer는 Swing 스레드에서 실행된다)
        new Timer(5000, e -> {
            if (isShowing()) loadTables();
        }).start();
        refresh();
    }

    /** 콤보박스 항목 (번호 + 보이는 글자) */
    private static class Item {
        final int id;
        final String text;

        Item(int id, String text) {
            this.id = id;
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    /** 화면을 열 때: 게임·테이블 콤보박스와 표를 모두 새로 읽는다 */
    @Override
    public void refresh() {
        gameCombo.removeAllItems();
        usageCombo.removeAllItems();
        try {
            for (GameDTO g : new GameDAO().findAll()) {
                if ("정상".equals(g.getStatus()) && g.getStock() > 0) {
                    gameCombo.addItem(new Item(g.getGameId(), g.getName() + " (" + g.getLocation() + ")"));
                }
            }
            Map<Integer, String> names = new HashMap<>();
            for (CafeTableDTO t : new CafeTableDAO().findAll()) names.put(t.getTableNo(), t.getTableName());
            for (TableUsageDTO u : new TableUsageDAO().findActive()) {
                usageCombo.addItem(new Item(u.getUsageId(), names.get(u.getTableNo()) + " (" + u.getPeople() + "명)"));
            }
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
        loadTables();
    }

    private void loadTables() {
        int selectedRent = rentTable.getSelectedRow();
        int selectedWait = waitTable.getSelectedRow();
        try {
            rentRows = dao.findByStatus("대여중", "호출");
            waitRows = dao.findByStatus("예약", "예약");
        } catch (SQLException e) {
            return;   // 5초마다 부르므로 오류창을 계속 띄우지 않는다
        }
        rentModel.setRowCount(0);
        for (Object[] r : rentRows) {
            rentModel.addRow(new Object[] {r[0], r[1], r[2], r[3], UIUtil.time((LocalDateTime) r[4]),
                    "호출".equals(r[3]) ? "게임을 건네주세요" : UIUtil.time((LocalDateTime) r[5])});
        }
        waitModel.setRowCount(0);
        Map<Object, Integer> order = new HashMap<>();   // 게임 이름별 대기 순서
        for (Object[] r : waitRows) {
            int n = order.merge(r[1], 1, Integer::sum);
            waitModel.addRow(new Object[] {r[0], r[1], r[2], r[3], UIUtil.time((LocalDateTime) r[4]), n + "번째"});
        }
        if (selectedRent >= 0 && selectedRent < rentModel.getRowCount()) rentTable.setRowSelectionInterval(selectedRent, selectedRent);
        if (selectedWait >= 0 && selectedWait < waitModel.getRowCount()) waitTable.setRowSelectionInterval(selectedWait, selectedWait);
    }

    private void rent() {
        Item game = (Item) gameCombo.getSelectedItem();
        Item usage = (Item) usageCombo.getSelectedItem();
        if (game == null || usage == null) {
            UIUtil.warn(this, "게임과 이용 중인 테이블을 고르세요.");
            return;
        }
        try {
            if (dao.isDuplicate(usage.id, game.id)) {
                UIUtil.warn(this, "이 테이블은 이미 이 게임을 대여 또는 예약했습니다.");
                return;
            }
            int rentalId = dao.request(game.id, usage.id);
            if ("대여중".equals(dao.findStatus(rentalId))) {
                UIUtil.info(this, game.text + "을(를) " + usage.text + "에 대여했습니다.");
            } else {
                UIUtil.info(this, "모두 대여 중이라 예약했습니다. 대기 " + dao.queuePosition(rentalId) + "번째입니다.");
            }
            loadTables();
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }

    private Object[] selectedRent() {
        int row = rentTable.getSelectedRow();
        if (row < 0) {
            UIUtil.warn(this, "'대여 중 · 호출' 표에서 고르세요.");
            return null;
        }
        return rentRows.get(row);
    }

    private void returnGame() {
        Object[] r = selectedRent();
        if (r == null) return;
        if (!"대여중".equals(r[3])) {
            UIUtil.warn(this, "대여 중인 게임만 반납할 수 있습니다.");
            return;
        }
        try {
            String called = dao.returnGame((Integer) r[0]);
            if (called != null) {
                UIUtil.info(this, r[1] + " 반납 완료.\n예약한 " + called + "을(를) 호출하세요!");
            } else {
                UIUtil.info(this, r[1] + " 반납 완료. 진열장에 다시 넣어 주세요.");
            }
            loadTables();
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }

    private void handOver() {
        Object[] r = selectedRent();
        if (r == null) return;
        if (!"호출".equals(r[3])) {
            UIUtil.warn(this, "'호출' 상태인 예약만 전달 완료할 수 있습니다.");
            return;
        }
        try {
            dao.handOver((Integer) r[0]);
            loadTables();
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }

    /** 예약 표에서 고르면 예약을, 대여 표에서 '호출'을 고르면 호출을 취소한다 */
    private void cancel() {
        Object[] r = null;
        if (waitTable.getSelectedRow() >= 0) r = waitRows.get(waitTable.getSelectedRow());
        else if (rentTable.getSelectedRow() >= 0 && "호출".equals(rentRows.get(rentTable.getSelectedRow())[3])) {
            r = rentRows.get(rentTable.getSelectedRow());
        }
        if (r == null) {
            UIUtil.warn(this, "취소할 예약을 고르세요.");
            return;
        }
        if (!UIUtil.confirm(this, r[2] + "의 " + r[1] + " 예약을 취소할까요?")) return;
        try {
            dao.cancel((Integer) r[0]);
            waitTable.clearSelection();
            loadTables();
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }
}
