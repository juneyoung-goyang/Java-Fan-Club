package dicepos.menu;

import dicepos.common.Session;
import dicepos.common.UIUtil;
import dicepos.game.GameDAO;
import dicepos.game.GameDTO;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
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
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

/** [관리자] 발주 관리 화면 (메뉴·보드게임) */
public class PurchaseOrderPanel extends JPanel {
    private final PurchaseOrderDAO dao = new PurchaseOrderDAO();
    private final MenuDAO menuDAO = new MenuDAO();
    private final GameDAO gameDAO = new GameDAO();   // ③ 김은별의 클래스

    // 위쪽 입력 줄
    private final JToggleButton tgMenu = new JToggleButton("메뉴", true);
    private final JToggleButton tgGame = new JToggleButton("보드게임");
    private final JComboBox<ItemOption> cbItem = new JComboBox<>();
    private final JSpinner spQty = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
    private final JTextField tfCost = new JTextField(7);

    // 왼쪽: 아직 저장하지 않은 발주 품목 (장바구니처럼)
    private final List<PurchaseDetailDTO> cart = new ArrayList<>();
    private final DefaultTableModel cartModel = readOnlyModel("구분", "품목", "수량", "단가", "금액");
    private final JTextField tfSupplier = new JTextField(10);
    private final JLabel lbTotal = new JLabel("총 발주 금액 0원");

    // 오른쪽: 발주 내역과 상세
    private final JComboBox<String> cbStatus = new JComboBox<>(new String[] {"전체", "요청", "입고", "취소"});
    private final DefaultTableModel poModel = readOnlyModel("번호", "발주 일시", "거래처", "관리자", "품목", "총 금액", "상태");
    private final JTable poTable = new JTable(poModel);
    private final DefaultTableModel detailModel = readOnlyModel("구분", "품목", "수량", "단가", "금액");
    private final JButton bReceive = new JButton("입고 처리");
    private final JButton bCancel = new JButton("발주 취소");
    private List<PurchaseOrderDTO> orders = new ArrayList<>();
    private List<PurchaseDetailDTO> details = new ArrayList<>();

    public PurchaseOrderPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        add(makeInputBar(), BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 10, 0));
        center.add(makeCartPanel());
        center.add(makeHistoryPanel());
        add(center, BorderLayout.CENTER);

        loadItems();
        refreshOrders();
    }

    private static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
    }

    private JPanel makeInputBar() {
        ButtonGroup group = new ButtonGroup();   // 둘 중 하나만 눌리게
        group.add(tgMenu);
        group.add(tgGame);
        tgMenu.addItemListener(e -> loadItems());
        cbItem.addActionListener(e -> fillLastCost());

        JButton bAdd = new JButton("담기");
        bAdd.addActionListener(e -> addToCart());

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        bar.add(new JLabel("품목 구분"));
        bar.add(tgMenu);
        bar.add(tgGame);
        bar.add(cbItem);
        bar.add(new JLabel("수량"));
        bar.add(spQty);
        bar.add(new JLabel("매입 단가"));
        bar.add(tfCost);
        bar.add(bAdd);
        return bar;
    }

    private JPanel makeCartPanel() {
        JButton bClear = new JButton("비우기");
        JButton bRequest = new JButton("발주 요청");
        bClear.addActionListener(e -> { cart.clear(); showCart(); });
        bRequest.addActionListener(e -> requestOrder());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        bottom.add(new JLabel("거래처"));
        bottom.add(tfSupplier);
        bottom.add(lbTotal);
        bottom.add(bClear);
        bottom.add(bRequest);

        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createTitledBorder("발주할 품목 (아직 저장 전)"));
        panel.add(new JScrollPane(new JTable(cartModel)), BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel makeHistoryPanel() {
        cbStatus.addActionListener(e -> refreshOrders());
        poTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        poTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showDetails();
        });
        bReceive.addActionListener(e -> receive());
        bCancel.addActionListener(e -> cancel());
        bReceive.setEnabled(false);
        bCancel.setEnabled(false);

        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.setBorder(BorderFactory.createTitledBorder("발주 내역"));
        JPanel filter = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        filter.add(new JLabel("상태"));
        filter.add(cbStatus);
        top.add(filter, BorderLayout.NORTH);
        top.add(new JScrollPane(poTable), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.add(bReceive);
        buttons.add(bCancel);
        JPanel bottom = new JPanel(new BorderLayout(0, 6));
        bottom.setBorder(BorderFactory.createTitledBorder("고른 발주의 품목"));
        bottom.add(new JScrollPane(new JTable(detailModel)), BorderLayout.CENTER);
        bottom.add(buttons, BorderLayout.SOUTH);

        JPanel panel = new JPanel(new GridLayout(2, 1, 0, 10));
        panel.add(top);
        panel.add(bottom);
        return panel;
    }

    /** [메뉴]/[보드게임]을 누르면 품목 콤보박스를 다시 채운다. */
    private void loadItems() {
        cbItem.removeAllItems();
        try {
            if (tgMenu.isSelected()) {
                for (MenuDTO m : menuDAO.findAll()) cbItem.addItem(new ItemOption("메뉴", m.getMenuId(), m.getName()));
            } else {
                for (GameDTO g : gameDAO.findAll()) cbItem.addItem(new ItemOption("보드게임", g.getGameId(), g.getName()));
            }
        } catch (SQLException ex) {
            UIUtil.warn(this, "품목을 불러오지 못했습니다.\n" + ex.getMessage());
        }
    }

    /** 품목을 고르면 지난번 단가를 불러와 넣는다(바꿀 수 있음). */
    private void fillLastCost() {
        ItemOption item = (ItemOption) cbItem.getSelectedItem();
        if (item == null) return;
        try {
            Integer cost = dao.lastUnitCost(item.getType(), item.getId());
            tfCost.setText(cost == null ? "" : String.valueOf(cost));
        } catch (SQLException ex) {
            tfCost.setText("");
        }
    }

    private void addToCart() {
        ItemOption item = (ItemOption) cbItem.getSelectedItem();
        Integer cost = UIUtil.toInt(tfCost.getText());
        int qty = (Integer) spQty.getValue();
        if (item == null) { UIUtil.warn(this, "품목을 고르세요."); return; }
        if (cost == null || cost <= 0) { UIUtil.warn(this, "매입 단가는 0보다 큰 숫자로 입력하세요."); return; }

        for (PurchaseDetailDTO d : cart) {   // 같은 품목이면 수량만 더한다
            boolean same = d.getItemType().equals(item.getType())
                    && item.getId() == (item.getType().equals("메뉴") ? d.getMenuId() : d.getGameId());
            if (same) { d.addQty(qty); showCart(); return; }
        }
        Integer menuId = item.getType().equals("메뉴") ? Integer.valueOf(item.getId()) : null;
        Integer gameId = item.getType().equals("보드게임") ? Integer.valueOf(item.getId()) : null;
        cart.add(new PurchaseDetailDTO(item.getType(), menuId, gameId, item.getName(), qty, cost));
        showCart();
    }

    private void showCart() {
        cartModel.setRowCount(0);
        int total = 0;
        for (PurchaseDetailDTO d : cart) {
            cartModel.addRow(new Object[] {d.getItemType(), d.getItemName(), d.getQty(),
                    UIUtil.won(d.getUnitCost()), UIUtil.won(d.getAmount())});
            total += d.getAmount();
        }
        lbTotal.setText("총 발주 금액 " + UIUtil.won(total));
    }

    private void requestOrder() {
        if (cart.isEmpty()) { UIUtil.warn(this, "발주할 품목을 먼저 담으세요."); return; }
        try {
            int poId = dao.insert(Session.getAdminId(), tfSupplier.getText().trim(), cart);
            UIUtil.info(this, "발주 " + poId + "번을 요청했습니다.");
            cart.clear();
            showCart();
            tfSupplier.setText("");
            refreshOrders();
        } catch (SQLException ex) {
            UIUtil.warn(this, "발주를 저장하지 못했습니다. (아무것도 저장되지 않았습니다)\n" + ex.getMessage());
        }
    }

    private void refreshOrders() {
        String status = (String) cbStatus.getSelectedItem();
        try {
            orders = dao.findAll("전체".equals(status) ? null : status);
        } catch (SQLException ex) {
            UIUtil.warn(this, "발주 내역을 불러오지 못했습니다.\n" + ex.getMessage());
            return;
        }
        poModel.setRowCount(0);
        for (PurchaseOrderDTO p : orders) {
            poModel.addRow(new Object[] {p.getPoId(), p.getOrderDate(), p.getSupplier(), p.getStaffName(),
                    p.getItemCount() + "건", UIUtil.won(p.getTotalAmount()), p.getStatus()});
        }
        detailModel.setRowCount(0);
        bReceive.setEnabled(false);
        bCancel.setEnabled(false);
    }

    private PurchaseOrderDTO selectedOrder() {
        int row = poTable.getSelectedRow();
        return row == -1 ? null : orders.get(row);
    }

    private void showDetails() {
        PurchaseOrderDTO po = selectedOrder();
        if (po == null) return;
        try {
            details = dao.findDetails(po.getPoId());
        } catch (SQLException ex) {
            UIUtil.warn(this, "품목을 불러오지 못했습니다.\n" + ex.getMessage());
            return;
        }
        detailModel.setRowCount(0);
        for (PurchaseDetailDTO d : details) {
            detailModel.addRow(new Object[] {d.getItemType(), d.getItemName(), d.getQty(),
                    UIUtil.won(d.getUnitCost()), UIUtil.won(d.getAmount())});
        }
        boolean waiting = po.getStatus().equals("요청");   // 요청 상태일 때만 버튼을 켠다
        bReceive.setEnabled(waiting);
        bCancel.setEnabled(waiting);
    }

    private void receive() {
        PurchaseOrderDTO po = selectedOrder();
        if (po == null) return;
        if (!ReceiveDialog.confirm(this, po, details)) return;
        try {
            dao.receive(po.getPoId());
            UIUtil.info(this, "입고했습니다. 재고가 늘어났습니다.");
            refreshOrders();
            loadItems();
        } catch (SQLException ex) {
            UIUtil.warn(this, "입고하지 못했습니다. (재고는 바뀌지 않았습니다)\n" + ex.getMessage());
        }
    }

    private void cancel() {
        PurchaseOrderDTO po = selectedOrder();
        if (po == null || !UIUtil.confirm(this, "발주 " + po.getPoId() + "번을 취소할까요?")) return;
        try {
            if (dao.cancel(po.getPoId()) == 0) UIUtil.warn(this, "이미 입고되었거나 취소된 발주입니다.");
            refreshOrders();
        } catch (SQLException ex) {
            UIUtil.warn(this, "취소하지 못했습니다.\n" + ex.getMessage());
        }
    }
}
