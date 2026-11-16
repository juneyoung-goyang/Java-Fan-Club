package dicepos.menu;

import dicepos.common.Refreshable;
import dicepos.common.Session;
import dicepos.common.UIUtil;
import dicepos.game.GameDAO;
import dicepos.game.GameDTO;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

/** [관리자] 발주 관리 — 메뉴·보드게임 (④ 박준영) */
public class PurchaseOrderPanel extends JPanel implements Refreshable {
    private final JToggleButton menuToggle = new JToggleButton("메뉴", true);
    private final JToggleButton gameToggle = new JToggleButton("보드게임");
    private final JComboBox<ItemOption> itemCombo = new JComboBox<>();
    private final JSpinner qtySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
    private final JTextField costField = new JTextField(7);
    private final JTextField supplierField = new JTextField(10);

    private final DefaultTableModel cartModel = UIUtil.tableModel("구분", "품목", "수량", "매입 단가", "금액");
    private final JTable cartTable = UIUtil.table(cartModel);
    private final JLabel totalLabel = new JLabel("총 발주 금액 0원");
    private final List<PurchaseDetailDTO> cart = new ArrayList<>();
    private final List<String> cartNames = new ArrayList<>();

    private final DefaultTableModel orderModel = UIUtil.tableModel("발주 번호", "발주 일시", "거래처", "총액", "상태", "입고 일시");
    private final JTable orderTable = UIUtil.table(orderModel);
    private final DefaultTableModel detailModel = UIUtil.tableModel("구분", "품목", "수량", "매입 단가", "금액");
    private final JButton receiveButton = new JButton("입고 처리");
    private final JButton cancelButton = new JButton("발주 취소");
    private List<PurchaseOrderDTO> orders = new ArrayList<>();

    private final PurchaseOrderDAO dao = new PurchaseOrderDAO();

    /** 품목 콤보박스 한 칸 */
    private static class ItemOption {
        final int id;
        final String name;

        ItemOption(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public PurchaseOrderPanel() {
        super(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 위: 발주 입력 줄 (FlowLayout)
        ButtonGroup typeGroup = new ButtonGroup();
        typeGroup.add(menuToggle);
        typeGroup.add(gameToggle);
        JButton putButton = new JButton("담기");
        JPanel inputRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inputRow.add(menuToggle);
        inputRow.add(gameToggle);
        inputRow.add(new JLabel(" 품목"));
        inputRow.add(itemCombo);
        inputRow.add(new JLabel(" 수량"));
        inputRow.add(qtySpinner);
        inputRow.add(new JLabel(" 매입 단가"));
        inputRow.add(costField);
        inputRow.add(putButton);

        JButton removeButton = new JButton("빼기");
        JButton orderButton = new JButton("발주 요청");
        JPanel cartButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        cartButtons.add(new JLabel("거래처"));
        cartButtons.add(supplierField);
        cartButtons.add(removeButton);
        cartButtons.add(totalLabel);
        cartButtons.add(orderButton);

        JPanel cartBox = new JPanel(new BorderLayout());
        cartBox.setBorder(BorderFactory.createTitledBorder("발주할 품목"));
        cartBox.add(inputRow, BorderLayout.NORTH);
        cartBox.add(new JScrollPane(cartTable), BorderLayout.CENTER);
        cartBox.add(cartButtons, BorderLayout.SOUTH);

        // 아래: 발주 내역과 상세
        JPanel orderButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        orderButtons.add(receiveButton);
        orderButtons.add(cancelButton);
        orderButtons.add(new JLabel("  '요청' 상태일 때만 입고·취소할 수 있습니다. 입고하면 메뉴 재고와 게임 보유 수량이 늘어납니다."));
        JPanel lists = new JPanel(new GridLayout(1, 2, 8, 0));
        lists.add(new JScrollPane(orderTable));
        lists.add(new JScrollPane(UIUtil.table(detailModel)));
        JPanel historyBox = new JPanel(new BorderLayout());
        historyBox.setBorder(BorderFactory.createTitledBorder("발주 내역 (왼쪽을 고르면 오른쪽에 상세)"));
        historyBox.add(lists, BorderLayout.CENTER);
        historyBox.add(orderButtons, BorderLayout.SOUTH);

        JPanel center = new JPanel(new GridLayout(2, 1, 0, 8));
        center.add(cartBox);
        center.add(historyBox);
        add(center, BorderLayout.CENTER);

        // 품목 구분을 바꾸면 품목 목록을 바꾼다
        menuToggle.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) loadItems();
        });
        gameToggle.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) loadItems();
        });
        // 품목을 고르면 직전 발주 단가를 불러온다
        itemCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) loadLastCost();
        });
        // 발주를 고르면 상세를 보여 주고 버튼을 켠다
        orderTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showDetails();
        });
        putButton.addActionListener(e -> put());
        removeButton.addActionListener(e -> removeFromCart());
        orderButton.addActionListener(e -> order());
        receiveButton.addActionListener(e -> receive());
        cancelButton.addActionListener(e -> cancelOrder());
        refresh();
    }

    private String itemType() {
        return menuToggle.isSelected() ? "메뉴" : "보드게임";
    }

    @Override
    public void refresh() {
        loadItems();
        loadOrders();
    }

    private void loadItems() {
        itemCombo.removeAllItems();
        try {
            if (menuToggle.isSelected()) {
                for (MenuDTO m : new MenuDAO().findByCategory("전체")) itemCombo.addItem(new ItemOption(m.getMenuId(), m.getName()));
            } else {
                for (GameDTO g : new GameDAO().findAll()) itemCombo.addItem(new ItemOption(g.getGameId(), g.getName()));
            }
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
        loadLastCost();
    }

    private void loadLastCost() {
        ItemOption item = (ItemOption) itemCombo.getSelectedItem();
        if (item == null) return;
        try {
            int cost = dao.lastUnitCost(itemType(), item.id);
            costField.setText(cost == 0 ? "" : String.valueOf(cost));
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }

    /** [담기]: 같은 품목은 수량만 합친다 */
    private void put() {
        ItemOption item = (ItemOption) itemCombo.getSelectedItem();
        Integer cost = UIUtil.toInt(costField.getText());
        int qty = (Integer) qtySpinner.getValue();
        if (item == null) return;
        if (cost == null || cost <= 0 || qty <= 0) {
            UIUtil.warn(this, "수량과 매입 단가는 0보다 커야 합니다.");
            return;
        }
        String type = itemType();
        Integer menuId = "메뉴".equals(type) ? item.id : null;
        Integer gameId = "메뉴".equals(type) ? null : item.id;
        for (int i = 0; i < cart.size(); i++) {
            PurchaseDetailDTO d = cart.get(i);
            if (d.getItemType().equals(type) && java.util.Objects.equals(d.getMenuId(), menuId)
                    && java.util.Objects.equals(d.getGameId(), gameId) && d.getUnitCost() == cost) {
                int newQty = d.getQty() + qty;
                cart.set(i, new PurchaseDetailDTO(0, 0, type, menuId, gameId, newQty, cost, newQty * cost));
                showCart();
                return;
            }
        }
        cart.add(new PurchaseDetailDTO(0, 0, type, menuId, gameId, qty, cost, qty * cost));
        cartNames.add(item.name);
        showCart();
    }

    private void removeFromCart() {
        int row = cartTable.getSelectedRow();
        if (row < 0) return;
        cart.remove(row);
        cartNames.remove(row);
        showCart();
    }

    private void showCart() {
        cartModel.setRowCount(0);
        int total = 0;
        for (int i = 0; i < cart.size(); i++) {
            PurchaseDetailDTO d = cart.get(i);
            cartModel.addRow(new Object[] {d.getItemType(), cartNames.get(i), d.getQty(), UIUtil.won(d.getUnitCost()), UIUtil.won(d.getAmount())});
            total += d.getAmount();
        }
        totalLabel.setText("총 발주 금액 " + UIUtil.won(total));
    }

    /** [발주 요청]: 발주 직원은 관리자 확인을 통과한 관리자 */
    private void order() {
        if (cart.isEmpty()) {
            UIUtil.warn(this, "발주할 품목을 먼저 담으세요.");
            return;
        }
        String supplier = supplierField.getText().trim().isEmpty() ? null : supplierField.getText().trim();
        Integer staffId = Session.getAdminId() == 0 ? null : Session.getAdminId();
        try {
            dao.save(staffId, supplier, cart);
            cart.clear();
            cartNames.clear();
            showCart();
            loadOrders();
            UIUtil.info(this, "발주를 요청했습니다.");
        } catch (SQLException e) {
            UIUtil.warn(this, "발주를 저장하지 못했습니다. 아무것도 저장되지 않았습니다.\n" + e.getMessage());
        }
    }

    private void loadOrders() {
        try {
            orders = dao.findAll();
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
            return;
        }
        orderModel.setRowCount(0);
        for (PurchaseOrderDTO o : orders) {
            orderModel.addRow(new Object[] {o.getPoId(), UIUtil.dateTime(o.getOrderDate()), o.getSupplier() == null ? "" : o.getSupplier(),
                    UIUtil.won(o.getTotalAmount()), o.getStatus(), UIUtil.dateTime(o.getReceiveDate())});
        }
        detailModel.setRowCount(0);
        receiveButton.setEnabled(false);
        cancelButton.setEnabled(false);
    }

    private PurchaseOrderDTO selectedOrder() {
        int row = orderTable.getSelectedRow();
        return row < 0 ? null : orders.get(row);
    }

    private void showDetails() {
        PurchaseOrderDTO o = selectedOrder();
        detailModel.setRowCount(0);
        boolean requested = o != null && "요청".equals(o.getStatus());
        receiveButton.setEnabled(requested);
        cancelButton.setEnabled(requested);
        if (o == null) return;
        try {
            for (Object[] d : dao.findDetails(o.getPoId())) {
                detailModel.addRow(new Object[] {d[0], d[1], d[2], UIUtil.won((Integer) d[3]), UIUtil.won((Integer) d[4])});
            }
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }

    private void receive() {
        PurchaseOrderDTO o = selectedOrder();
        if (o == null) return;
        if (!UIUtil.confirm(this, "발주 " + o.getPoId() + "번을 입고 처리할까요?\n메뉴 재고와 게임 보유 수량이 늘어납니다.")) return;
        try {
            dao.receive(o.getPoId());
            loadOrders();
            UIUtil.info(this, "입고 처리했습니다.");
        } catch (SQLException e) {
            UIUtil.warn(this, "입고하지 못했습니다. 아무것도 바뀌지 않았습니다.\n" + e.getMessage());
        }
    }

    private void cancelOrder() {
        PurchaseOrderDTO o = selectedOrder();
        if (o == null) return;
        if (!UIUtil.confirm(this, "발주 " + o.getPoId() + "번을 취소할까요? 기록은 '취소'로 남습니다.")) return;
        try {
            dao.cancel(o.getPoId());
            loadOrders();
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }
}
