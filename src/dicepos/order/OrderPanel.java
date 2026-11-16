package dicepos.order;

import dicepos.common.Refreshable;
import dicepos.common.Session;
import dicepos.common.UIUtil;
import dicepos.menu.MenuDAO;
import dicepos.menu.MenuDTO;
import dicepos.table.CafeTableDAO;
import dicepos.table.CafeTableDTO;
import dicepos.table.TableUsageDAO;
import dicepos.table.TableUsageDTO;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

/** [직원] 손님 주문받기 (⑥ 김하진). OKPOS 후불 주문 방식 */
public class OrderPanel extends JPanel implements Refreshable {
    private final JComboBox<UsageItem> usageCombo = new JComboBox<>();
    private final DefaultTableModel historyModel = UIUtil.tableModel("메뉴", "수량", "단가", "금액", "경로", "상태", "주문 시각");
    private final JTable historyTable = UIUtil.table(historyModel);
    private final JLabel historyTotal = new JLabel("합계 0원");
    private final JPanel menuButtons = new JPanel(new GridLayout(0, 3, 6, 6));
    private final DefaultListModel<CartItem> pendingModel = new DefaultListModel<>();
    private final JList<CartItem> pendingList = new JList<>(pendingModel);
    private final JLabel pendingTotal = new JLabel("0원");
    private final JButton kioskButton = new JButton("키오스크 주문 0건");
    private final OrderDAO dao = new OrderDAO();
    private List<Object[]> historyRows = new ArrayList<>();
    private String category = "음료";
    private boolean loading = false;

    /** 모든 메뉴 버튼이 같이 쓰는 리스너 하나: 누른 메뉴를 주문 대기 목록에 담는다 */
    private final Map<JButton, MenuDTO> buttonMenu = new HashMap<>();
    private final ActionListener menuListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            addPending(buttonMenu.get((JButton) e.getSource()));
        }
    };

    /** 콤보박스 한 칸: 이용 건 번호 + "Room 19 (2명)" */
    private static class UsageItem {
        final int usageId;
        final String text;

        UsageItem(int usageId, String text) {
            this.usageId = usageId;
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    public OrderPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 위: 테이블 선택과 키오스크 주문 알림
        JButton reloadButton = new JButton("새로 고침");
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("이용 중인 테이블"));
        top.add(usageCombo);
        top.add(reloadButton);
        top.add(new JLabel("     "));
        top.add(kioskButton);

        // 왼쪽: 주문 내역
        JButton plusButton = new JButton("+");
        JButton minusButton = new JButton("−");
        JButton qtyButton = new JButton("수량 변경");
        JButton cancelOneButton = new JButton("선택 취소");
        JButton cancelAllButton = new JButton("전체 취소");
        JPanel historyButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        historyButtons.add(plusButton);
        historyButtons.add(minusButton);
        historyButtons.add(qtyButton);
        historyButtons.add(cancelOneButton);
        historyButtons.add(cancelAllButton);
        historyButtons.add(historyTotal);
        JPanel left = new JPanel(new BorderLayout());
        left.setBorder(BorderFactory.createTitledBorder("주문 내역"));
        left.add(new JScrollPane(historyTable), BorderLayout.CENTER);
        left.add(historyButtons, BorderLayout.SOUTH);

        // 오른쪽: 분류 버튼 + 메뉴 버튼 + 주문 대기 목록
        JPanel categoryRow = new JPanel(new GridLayout(1, 3, 4, 0));
        ButtonGroup group = new ButtonGroup();
        for (String c : new String[] {"음료", "간식", "식사"}) {
            JToggleButton b = new JToggleButton(c, c.equals(category));
            b.addActionListener(e -> {
                category = c;
                loadMenuButtons();
            });
            group.add(b);
            categoryRow.add(b);
        }
        JPanel menuHolder = new JPanel(new BorderLayout());
        menuHolder.add(menuButtons, BorderLayout.NORTH);
        JButton orderButton = new JButton("주문");
        JButton removeButton = new JButton("빼기");
        JButton clearButton = new JButton("비우기");
        JPanel pendingButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pendingButtons.add(removeButton);
        pendingButtons.add(clearButton);
        pendingButtons.add(pendingTotal);
        pendingButtons.add(orderButton);
        JPanel pendingBox = new JPanel(new BorderLayout());
        pendingBox.setBorder(BorderFactory.createTitledBorder("주문할 메뉴 (같은 메뉴는 수량 +1)"));
        pendingBox.add(new JScrollPane(pendingList), BorderLayout.CENTER);
        pendingBox.add(pendingButtons, BorderLayout.SOUTH);
        pendingBox.setPreferredSize(new java.awt.Dimension(380, 220));

        JPanel right = new JPanel(new BorderLayout(0, 6));
        right.add(categoryRow, BorderLayout.NORTH);
        right.add(new JScrollPane(menuHolder), BorderLayout.CENTER);
        right.add(pendingBox, BorderLayout.SOUTH);
        right.setPreferredSize(new java.awt.Dimension(400, 0));

        add(top, BorderLayout.NORTH);
        add(left, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);

        usageCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && !loading) loadHistory();
        });
        reloadButton.addActionListener(e -> refresh());
        plusButton.addActionListener(e -> changeBy(1));
        minusButton.addActionListener(e -> changeBy(-1));
        qtyButton.addActionListener(e -> changeByPad());
        cancelOneButton.addActionListener(e -> cancelOne());
        cancelAllButton.addActionListener(e -> cancelAll());
        orderButton.addActionListener(e -> order());
        removeButton.addActionListener(e -> {
            int i = pendingList.getSelectedIndex();
            if (i >= 0) pendingModel.remove(i);
            showPendingTotal();
        });
        clearButton.addActionListener(e -> {
            pendingModel.clear();
            showPendingTotal();
        });
        kioskButton.addActionListener(e -> showKioskOrders());

        // 5초마다 확인 안 한 키오스크 주문 수를 센다
        new Timer(5000, e -> countKioskOrders()).start();
        refresh();
    }

    @Override
    public void refresh() {
        Object before = usageCombo.getSelectedItem();
        loading = true;
        usageCombo.removeAllItems();
        try {
            Map<Integer, String> names = new HashMap<>();
            for (CafeTableDTO t : new CafeTableDAO().findAll()) names.put(t.getTableNo(), t.getTableName());
            for (TableUsageDTO u : new TableUsageDAO().findActive()) {
                UsageItem item = new UsageItem(u.getUsageId(), names.get(u.getTableNo()) + " (" + u.getPeople() + "명)");
                usageCombo.addItem(item);
                if (before != null && ((UsageItem) before).usageId == u.getUsageId()) usageCombo.setSelectedItem(item);
            }
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
        loading = false;
        loadMenuButtons();
        loadHistory();
        countKioskOrders();
    }

    /** 분류별 메뉴 버튼을 새로 만든다. 품절 메뉴 버튼은 누를 수 없다. */
    private void loadMenuButtons() {
        menuButtons.removeAll();
        buttonMenu.clear();
        try {
            for (MenuDTO m : new MenuDAO().findByCategory(category)) {
                String text = "<html><center>" + m.getName() + "<br>" + UIUtil.won(m.getPrice())
                        + (m.isSoldOut() ? "<br>(품절)" : "<br>재고 " + m.getStock()) + "</center></html>";
                JButton b = new JButton(text);
                b.setEnabled(!m.isSoldOut());
                b.addActionListener(menuListener);
                buttonMenu.put(b, m);
                menuButtons.add(b);
            }
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
        menuButtons.revalidate();
        menuButtons.repaint();
    }

    private void addPending(MenuDTO m) {
        for (int i = 0; i < pendingModel.size(); i++) {
            CartItem item = pendingModel.get(i);
            if (item.getMenuId() == m.getMenuId()) {
                item.setQty(item.getQty() + 1);
                pendingModel.set(i, item);   // 목록을 다시 그리게 한다
                showPendingTotal();
                return;
            }
        }
        pendingModel.addElement(new CartItem(m.getMenuId(), m.getName(), m.getPrice(), 1));
        showPendingTotal();
    }

    private void showPendingTotal() {
        int total = 0;
        for (int i = 0; i < pendingModel.size(); i++) total += pendingModel.get(i).getSubtotal();
        pendingTotal.setText(UIUtil.won(total));
    }

    private UsageItem currentUsage() {
        UsageItem u = (UsageItem) usageCombo.getSelectedItem();
        if (u == null) UIUtil.warn(this, "이용 중인 테이블이 없습니다. 테이블 현황에서 먼저 입장을 등록하세요.");
        return u;
    }

    private void loadHistory() {
        historyModel.setRowCount(0);
        UsageItem u = (UsageItem) usageCombo.getSelectedItem();
        if (u == null) {
            historyRows = new ArrayList<>();
            historyTotal.setText("합계 0원");
            return;
        }
        try {
            historyRows = dao.findLinesByUsage(u.usageId);
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
            return;
        }
        int total = 0;
        for (Object[] r : historyRows) {
            historyModel.addRow(new Object[] {r[1], r[2], UIUtil.won((Integer) r[3]), UIUtil.won((Integer) r[4]), r[5], r[6],
                    UIUtil.time((java.time.LocalDateTime) r[7])});
            total += (Integer) r[4];
        }
        historyTotal.setText("합계 " + UIUtil.won(total));
    }

    /** [주문]: 주문 대기 목록을 한 주문으로 저장한다 (경로 POS, 상태 확인, 받은 직원) */
    private void order() {
        UsageItem u = currentUsage();
        if (u == null) return;
        if (pendingModel.isEmpty()) {
            UIUtil.warn(this, "메뉴 버튼을 눌러 주문할 메뉴를 담으세요.");
            return;
        }
        List<CartItem> items = new ArrayList<>();
        for (int i = 0; i < pendingModel.size(); i++) items.add(pendingModel.get(i));
        try {
            dao.saveOrder(u.usageId, Session.getStaffId() == 0 ? null : Session.getStaffId(), "POS", "확인", items);
            pendingModel.clear();
            showPendingTotal();
            loadHistory();
            loadMenuButtons();   // 재고·품절이 바뀌었을 수 있다
        } catch (SQLException e) {
            UIUtil.warn(this, "주문하지 못했습니다. 아무것도 저장되지 않았습니다.\n" + e.getMessage());
            loadMenuButtons();
        }
    }

    private Object[] selectedHistory() {
        int row = historyTable.getSelectedRow();
        if (row < 0) {
            UIUtil.warn(this, "주문 내역에서 메뉴를 먼저 고르세요.");
            return null;
        }
        return historyRows.get(row);
    }

    private void changeBy(int diff) {
        Object[] r = selectedHistory();
        if (r == null) return;
        int newQty = (Integer) r[2] + diff;
        if (newQty <= 0) {
            UIUtil.warn(this, "수량은 1 이상이어야 합니다. 빼려면 [선택 취소]를 누르세요.");
            return;
        }
        changeQty((Integer) r[0], newQty);
    }

    private void changeByPad() {
        Object[] r = selectedHistory();
        if (r == null) return;
        Integer newQty = NumberPadDialog.ask(this, r[1] + " 수량", (Integer) r[2]);
        if (newQty == null) return;
        if (newQty <= 0) {
            UIUtil.warn(this, "수량은 1 이상이어야 합니다. 빼려면 [선택 취소]를 누르세요.");
            return;
        }
        changeQty((Integer) r[0], newQty);
    }

    private void changeQty(int detailId, int newQty) {
        int row = historyTable.getSelectedRow();
        try {
            dao.changeQty(detailId, newQty);
        } catch (SQLException e) {
            UIUtil.warn(this, "수량을 바꾸지 못했습니다.\n" + e.getMessage());
        }
        loadHistory();
        loadMenuButtons();
        if (row >= 0 && row < historyModel.getRowCount()) historyTable.setRowSelectionInterval(row, row);
    }

    private void cancelOne() {
        Object[] r = selectedHistory();
        if (r == null) return;
        if (!UIUtil.confirm(this, r[1] + " " + r[2] + "개 주문을 취소할까요?")) return;
        try {
            dao.cancelDetail((Integer) r[0]);
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
        loadHistory();
        loadMenuButtons();
    }

    private void cancelAll() {
        UsageItem u = currentUsage();
        if (u == null || historyRows.isEmpty()) return;
        if (!UIUtil.confirm(this, u.text + "의 주문을 모두 취소할까요?")) return;
        try {
            dao.cancelAll(u.usageId);
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
        loadHistory();
        loadMenuButtons();
    }

    private void countKioskOrders() {
        try {
            int n = dao.countNewKioskOrders();
            kioskButton.setText(n > 0 ? "키오스크 새 주문 " + n + "건!" : "키오스크 주문 0건");
        } catch (SQLException e) {
            // 5초마다 부르므로 오류창은 띄우지 않는다
        }
    }

    /** 키오스크에서 들어온 주문을 보여 주고, 확인하면 '확인'으로 바꾼다 */
    private void showKioskOrders() {
        try {
            List<String> lines = dao.findNewKioskOrders();
            if (lines.isEmpty()) {
                UIUtil.info(this, "확인할 키오스크 주문이 없습니다.");
                return;
            }
            String text = String.join("\n", lines) + "\n\n음식을 준비하고 [예]를 누르면 모두 '확인'으로 바뀝니다.";
            if (JOptionPane.showConfirmDialog(this, text, "키오스크 주문", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                dao.confirmKioskOrders();
            }
            countKioskOrders();
            loadHistory();
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }
}
