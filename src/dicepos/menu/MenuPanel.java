package dicepos.menu;

import dicepos.common.Refreshable;
import dicepos.common.UIUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/** [관리자] 메뉴 관리 (④ 박준영) */
public class MenuPanel extends JPanel implements Refreshable {
    private final DefaultTableModel model = UIUtil.tableModel("번호", "이름", "분류", "가격", "재고", "품절");
    private final JTable table = UIUtil.table(model);
    private final JTextField nameField = new JTextField(12);
    private final JComboBox<String> categoryCombo = new JComboBox<>(new String[] {"음료", "간식", "식사"});
    private final JTextField priceField = new JTextField(8);
    private final JSpinner stockSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 9999, 1));
    private final JCheckBox soldOutCheck = new JCheckBox("품절 (주문 불가)");
    private final MenuDAO dao = new MenuDAO();
    private List<MenuDTO> list = new ArrayList<>();
    private String category = "전체";
    private boolean filling = false;   // 폼을 채우는 중에는 체크박스 이벤트를 무시한다

    public MenuPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 위: 분류 버튼
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ButtonGroup group = new ButtonGroup();
        for (String c : new String[] {"전체", "음료", "간식", "식사"}) {
            JToggleButton b = new JToggleButton(c, c.equals("전체"));
            b.addActionListener(e -> {
                category = c;
                refresh();
            });
            group.add(b);
            top.add(b);
        }
        top.add(new JLabel("   품절 메뉴는 회색으로 보입니다."));

        // 품절 메뉴 줄은 회색 글씨
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, selected, focus, row, col);
                if (!selected) c.setForeground(list.get(row).isSoldOut() ? Color.GRAY : Color.BLACK);
                return c;
            }
        });

        // 오른쪽: GridBagLayout 입력 폼
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        addRow(form, gc, 0, "이름 *", nameField);
        addRow(form, gc, 1, "분류", categoryCombo);
        addRow(form, gc, 2, "가격 *", priceField);
        addRow(form, gc, 3, "재고", stockSpinner);
        addRow(form, gc, 4, "", soldOutCheck);

        JButton addButton = new JButton("추가");
        JButton editButton = new JButton("수정");
        JButton deleteButton = new JButton("삭제");
        JButton clearButton = new JButton("새로 입력");
        JPanel buttons = new JPanel(new GridLayout(2, 2, 5, 5));
        buttons.add(addButton);
        buttons.add(editButton);
        buttons.add(deleteButton);
        buttons.add(clearButton);

        JPanel right = new JPanel(new BorderLayout(0, 10));
        right.setBorder(BorderFactory.createTitledBorder("메뉴 정보"));
        right.add(form, BorderLayout.NORTH);
        right.add(buttons, BorderLayout.CENTER);
        right.add(new JLabel("<html>재고가 0이 되면 자동으로 품절됩니다.<br>재고는 발주 관리에서 입고하면 늘어납니다.</html>"),
                BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(right, BorderLayout.EAST);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                fillForm();
            }
        });
        soldOutCheck.addItemListener(e -> {
            if (!filling) changeSoldOut(e.getStateChange() == ItemEvent.SELECTED);
        });
        addButton.addActionListener(e -> add());
        editButton.addActionListener(e -> edit());
        deleteButton.addActionListener(e -> delete());
        clearButton.addActionListener(e -> clearForm());
        refresh();
    }

    private void addRow(JPanel form, GridBagConstraints gc, int row, String label, Component field) {
        gc.gridy = row;
        gc.gridx = 0;
        gc.weightx = 0;
        form.add(new JLabel(label), gc);
        gc.gridx = 1;
        gc.weightx = 1;
        form.add(field, gc);
    }

    @Override
    public void refresh() {
        try {
            list = dao.findByCategory(category);
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
            return;
        }
        model.setRowCount(0);
        for (MenuDTO m : list) {
            model.addRow(new Object[] {m.getMenuId(), m.getName(), m.getCategory(), UIUtil.won(m.getPrice()),
                    m.getStock(), m.isSoldOut() ? "품절" : ""});
        }
    }

    private MenuDTO selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : list.get(row);
    }

    private void fillForm() {
        MenuDTO m = selected();
        if (m == null) return;
        filling = true;
        nameField.setText(m.getName());
        categoryCombo.setSelectedItem(m.getCategory());
        priceField.setText(String.valueOf(m.getPrice()));
        stockSpinner.setValue(m.getStock());
        soldOutCheck.setSelected(m.isSoldOut());
        filling = false;
    }

    private void clearForm() {
        filling = true;
        table.clearSelection();
        nameField.setText("");
        categoryCombo.setSelectedIndex(0);
        priceField.setText("");
        stockSpinner.setValue(0);
        soldOutCheck.setSelected(false);
        filling = false;
    }

    /** 폼 값 검사 후 DTO로. 틀리면 null */
    private MenuDTO readForm(int menuId) throws SQLException {
        String name = nameField.getText().trim();
        Integer price = UIUtil.toInt(priceField.getText());
        if (name.isEmpty()) {
            UIUtil.warn(this, "메뉴 이름을 입력하세요.");
            return null;
        }
        if (price == null || price <= 0) {
            UIUtil.warn(this, "가격은 0보다 큰 숫자로 입력하세요.");
            return null;
        }
        if (dao.isNameTaken(name, menuId)) {
            UIUtil.warn(this, "같은 이름의 메뉴가 이미 있습니다.");
            return null;
        }
        return new MenuDTO(menuId, name, (String) categoryCombo.getSelectedItem(), price,
                (Integer) stockSpinner.getValue(), soldOutCheck.isSelected());
    }

    private void add() {
        try {
            MenuDTO m = readForm(0);
            if (m == null) return;
            dao.insert(m);
            refresh();
            clearForm();
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }

    private void edit() {
        MenuDTO old = selected();
        if (old == null) {
            UIUtil.warn(this, "표에서 메뉴를 먼저 고르세요.");
            return;
        }
        try {
            MenuDTO m = readForm(old.getMenuId());
            if (m == null) return;
            dao.update(m);
            refresh();
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }

    /** 품절 체크박스를 누르면 바로 저장한다 */
    private void changeSoldOut(boolean soldOut) {
        MenuDTO m = selected();
        if (m == null) return;   // 새로 입력하는 중이면 [추가] 때 저장된다
        if (!soldOut && m.getStock() == 0) {
            UIUtil.warn(this, "재고가 0이라 품절을 풀 수 없습니다. 재고를 먼저 늘리세요.");
            filling = true;
            soldOutCheck.setSelected(true);
            filling = false;
            return;
        }
        try {
            dao.setSoldOut(m.getMenuId(), soldOut);
            int row = table.getSelectedRow();
            refresh();
            table.setRowSelectionInterval(row, row);
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }

    private void delete() {
        MenuDTO m = selected();
        if (m == null) {
            UIUtil.warn(this, "표에서 메뉴를 먼저 고르세요.");
            return;
        }
        try {
            if (dao.countRecords(m.getMenuId()) > 0) {
                UIUtil.warn(this, "주문·결제·발주 기록이 있는 메뉴는 삭제할 수 없습니다. 대신 품절 처리하세요.");
                return;
            }
            if (!UIUtil.confirm(this, m.getName() + "을(를) 삭제할까요?")) return;
            dao.delete(m.getMenuId());
            refresh();
            clearForm();
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }
}
