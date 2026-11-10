package dicepos.menu;

import dicepos.common.UIUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/** [관리자] 메뉴 관리 화면 */
public class MenuPanel extends JPanel {
    private final MenuDAO dao = new MenuDAO();
    private final DefaultTableModel model = new DefaultTableModel(
            new String[] {"번호", "메뉴", "분류", "가격", "재고", "품절"}, 0) {
        @Override
        public boolean isCellEditable(int row, int col) { return false; }   // 표에서 직접 고치지 못하게
    };
    private final JTable table = new JTable(model);
    private final JTextField tfName = new JTextField(12);
    private final JComboBox<String> cbCategory = new JComboBox<>(new String[] {"음료", "간식", "식사"});
    private final JTextField tfPrice = new JTextField(12);
    private final JTextField tfStock = new JTextField(12);
    private final JCheckBox chkSoldOut = new JCheckBox("품절 (주문 불가)");

    private List<MenuDTO> rows = new ArrayList<>();   // 표에 보이는 메뉴들
    private String filter = null;                     // null이면 전체
    private int selectedId = 0;                       // 0이면 고른 메뉴 없음
    private boolean filling = false;                  // 폼을 채우는 중에는 품절 이벤트를 무시

    public MenuPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(26);
        table.setDefaultRenderer(Object.class, new SoldOutRenderer());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) fillForm();
        });
        chkSoldOut.addItemListener(e -> changeSoldOut());

        add(makeFilterBar(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(makeForm(), BorderLayout.EAST);
        refresh();
    }

    private JPanel makeFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        for (String name : new String[] {"전체", "음료", "간식", "식사"}) {
            JButton button = new JButton(name);
            button.addActionListener(e -> {
                filter = name.equals("전체") ? null : name;
                refresh();
            });
            bar.add(button);
        }
        return bar;
    }

    private JPanel makeForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("메뉴 정보"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.fill = GridBagConstraints.HORIZONTAL;

        String[] labels = {"메뉴 이름 *", "분류 *", "판매 가격 *", "재고"};
        JComponent[] inputs = {tfName, cbCategory, tfPrice, tfStock};
        for (int i = 0; i < labels.length; i++) {
            g.gridx = 0; g.gridy = i; g.gridwidth = 1;
            form.add(new JLabel(labels[i]), g);
            g.gridx = 1;
            form.add(inputs[i], g);
        }
        g.gridx = 1; g.gridy = 4;
        form.add(chkSoldOut, g);

        JPanel buttons = new JPanel(new GridLayout(2, 2, 6, 6));
        JButton bAdd = new JButton("추가");
        JButton bEdit = new JButton("수정");
        JButton bDelete = new JButton("삭제");
        JButton bClear = new JButton("새로 입력");
        bAdd.addActionListener(e -> save(true));
        bEdit.addActionListener(e -> save(false));
        bDelete.addActionListener(e -> delete());
        bClear.addActionListener(e -> clearForm());
        buttons.add(bAdd);
        buttons.add(bEdit);
        buttons.add(bDelete);
        buttons.add(bClear);
        g.gridx = 0; g.gridy = 5; g.gridwidth = 2;
        form.add(buttons, g);

        JPanel wrap = new JPanel(new BorderLayout());   // 폼을 오른쪽 위에 붙인다
        wrap.add(form, BorderLayout.NORTH);
        return wrap;
    }

    /** DB에서 다시 읽어 표를 새로 그린다. */
    private void refresh() {
        try {
            rows = (filter == null) ? dao.findAll() : dao.findByCategory(filter);
        } catch (SQLException ex) {
            UIUtil.warn(this, "메뉴를 불러오지 못했습니다.\n" + ex.getMessage());
            return;
        }
        model.setRowCount(0);
        for (MenuDTO m : rows) {
            model.addRow(new Object[] {m.getMenuId(), m.getName(), m.getCategory(),
                    UIUtil.won(m.getPrice()), m.getStock(), m.isSoldOut() ? "품절" : ""});
        }
        clearForm();
    }

    private void fillForm() {
        int row = table.getSelectedRow();
        if (row == -1) return;
        MenuDTO m = rows.get(row);
        filling = true;
        selectedId = m.getMenuId();
        tfName.setText(m.getName());
        cbCategory.setSelectedItem(m.getCategory());
        tfPrice.setText(String.valueOf(m.getPrice()));
        tfStock.setText(String.valueOf(m.getStock()));
        chkSoldOut.setSelected(m.isSoldOut());
        filling = false;
    }

    private void clearForm() {
        filling = true;
        selectedId = 0;
        table.clearSelection();
        tfName.setText("");
        cbCategory.setSelectedIndex(0);
        tfPrice.setText("");
        tfStock.setText("0");
        chkSoldOut.setSelected(false);
        filling = false;
    }

    /** 품절 체크박스를 누르면 바로 저장한다. */
    private void changeSoldOut() {
        if (filling || selectedId == 0) return;
        try {
            dao.setSoldOut(selectedId, chkSoldOut.isSelected());
            refresh();
        } catch (SQLException ex) {
            UIUtil.warn(this, "품절을 바꾸지 못했습니다.\n" + ex.getMessage());
        }
    }

    /** isNew가 true면 [추가], false면 [수정] */
    private void save(boolean isNew) {
        String name = tfName.getText().trim();
        Integer price = UIUtil.toInt(tfPrice.getText());
        Integer stock = tfStock.getText().trim().isEmpty() ? Integer.valueOf(0) : UIUtil.toInt(tfStock.getText());
        if (name.isEmpty()) { UIUtil.warn(this, "메뉴 이름을 입력하세요."); return; }
        if (price == null || price <= 0) { UIUtil.warn(this, "가격은 0보다 큰 숫자로 입력하세요."); return; }
        if (stock == null || stock < 0) { UIUtil.warn(this, "재고는 0 이상의 숫자로 입력하세요."); return; }
        if (!isNew && selectedId == 0) { UIUtil.warn(this, "수정할 메뉴를 표에서 먼저 고르세요."); return; }

        boolean soldOut = chkSoldOut.isSelected() || stock == 0;   // 재고 0이면 자동 품절
        MenuDTO m = new MenuDTO(selectedId, name, (String) cbCategory.getSelectedItem(), price, stock, soldOut);
        try {
            if (isNew) dao.insert(m); else dao.update(m);
            UIUtil.info(this, isNew ? "메뉴를 추가했습니다." : "메뉴를 수정했습니다.");
            refresh();
        } catch (SQLIntegrityConstraintViolationException ex) {
            UIUtil.warn(this, "이미 있는 메뉴 이름입니다.");
        } catch (SQLException ex) {
            UIUtil.warn(this, "저장하지 못했습니다.\n" + ex.getMessage());
        }
    }

    private void delete() {
        if (selectedId == 0) { UIUtil.warn(this, "삭제할 메뉴를 표에서 먼저 고르세요."); return; }
        try {
            if (dao.countUsage(selectedId) > 0) {
                UIUtil.warn(this, "주문·결제·발주 기록이 있는 메뉴는 삭제할 수 없습니다.\n판매를 멈추려면 품절로 바꾸세요.");
                return;
            }
            if (!UIUtil.confirm(this, tfName.getText() + " 메뉴를 삭제할까요?")) return;
            dao.delete(selectedId);
            refresh();
        } catch (SQLException ex) {
            UIUtil.warn(this, "삭제하지 못했습니다.\n" + ex.getMessage());
        }
    }

    /** 품절 메뉴 줄을 회색 글씨로 그린다. */
    private static class SoldOutRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                       boolean focus, int row, int col) {
            Component c = super.getTableCellRendererComponent(t, value, selected, focus, row, col);
            if (!selected) {
                c.setForeground("품절".equals(t.getValueAt(row, 5)) ? Color.GRAY : Color.BLACK);
            }
            return c;
        }
    }
}
