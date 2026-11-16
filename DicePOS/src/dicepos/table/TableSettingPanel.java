package dicepos.table;

import dicepos.common.Refreshable;
import dicepos.common.UIUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

/** [관리자] 테이블 설정: 룸 등록·정원, 사용가능/사용불가 (⑤ 서윤채) */
public class TableSettingPanel extends JPanel implements Refreshable {
    private final DefaultTableModel model = UIUtil.tableModel("번호", "표시 이름", "정원", "사용 여부", "이용 기록 수");
    private final JTable table = UIUtil.table(model);
    private final JTextField noField = new JTextField(6);
    private final JTextField nameField = new JTextField(10);
    private final JSpinner capacitySpinner = new JSpinner(new SpinnerNumberModel(4, 1, 20, 1));
    private final JCheckBox activeCheck = new JCheckBox("사용가능", true);
    private final JPanel preview = new JPanel();
    private final CafeTableDAO dao = new CafeTableDAO();
    private final TableUsageDAO usageDAO = new TableUsageDAO();
    private List<CafeTableDTO> list = new ArrayList<>();
    private List<Integer> usageCounts = new ArrayList<>();
    private boolean filling = false;   // 폼을 채우는 중에는 체크박스 이벤트를 무시한다

    public TableSettingPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 8));
        form.add(new JLabel("번호 *"));
        form.add(noField);
        form.add(new JLabel("표시 이름 * (예: Room 17)"));
        form.add(nameField);
        form.add(new JLabel("정원 *"));
        form.add(capacitySpinner);
        form.add(new JLabel("사용 여부"));
        form.add(activeCheck);

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
        right.setBorder(BorderFactory.createTitledBorder("룸 정보"));
        right.add(form, BorderLayout.NORTH);
        right.add(buttons, BorderLayout.CENTER);
        right.add(new JLabel("<html>정원은 룸 크기라서 처음 등록하거나<br>공사했을 때만 바꿉니다.<br>"
                + "사용 여부 체크를 풀면 바로 사용불가가 됩니다.</html>"), BorderLayout.SOUTH);

        preview.setBorder(BorderFactory.createTitledBorder("테이블 현황 미리보기"));

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(right, BorderLayout.EAST);
        add(preview, BorderLayout.SOUTH);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) fillForm();
        });
        activeCheck.addItemListener(e -> {
            if (!filling) changeActive(e.getStateChange() == ItemEvent.SELECTED);
        });
        addButton.addActionListener(e -> add());
        editButton.addActionListener(e -> edit());
        deleteButton.addActionListener(e -> delete());
        clearButton.addActionListener(e -> clearForm());
        refresh();
    }

    @Override
    public void refresh() {
        try {
            list = dao.findAll();
            usageCounts = new ArrayList<>();
            for (CafeTableDTO t : list) usageCounts.add(usageDAO.countByTable(t.getTableNo()));
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
            return;
        }
        model.setRowCount(0);
        for (int i = 0; i < list.size(); i++) {
            CafeTableDTO t = list.get(i);
            model.addRow(new Object[] {t.getTableNo(), t.getTableName(), t.getCapacity() + "명",
                    t.isActive() ? "사용가능" : "사용불가", usageCounts.get(i) + "건"});
        }
        showPreview();
    }

    /** 아래쪽에 룸 배치를 작게 보여 준다 */
    private void showPreview() {
        preview.removeAll();
        preview.setLayout(new GridLayout(0, 10, 4, 4));
        Set<Integer> using = new HashSet<>();
        try {
            for (TableUsageDTO u : usageDAO.findActive()) using.add(u.getTableNo());
        } catch (SQLException e) {
            // 미리보기는 실패해도 넘어간다
        }
        for (CafeTableDTO t : list) {
            JLabel l = new JLabel(t.getTableName(), SwingConstants.CENTER);
            l.setOpaque(true);
            l.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            l.setBackground(!t.isActive() ? new Color(210, 210, 210)
                    : using.contains(t.getTableNo()) ? new Color(255, 240, 210) : new Color(235, 245, 235));
            preview.add(l);
        }
        preview.revalidate();
        preview.repaint();
    }

    private CafeTableDTO selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : list.get(row);
    }

    private void fillForm() {
        CafeTableDTO t = selected();
        if (t == null) return;
        filling = true;
        noField.setText(String.valueOf(t.getTableNo()));
        nameField.setText(t.getTableName());
        capacitySpinner.setValue(t.getCapacity());
        activeCheck.setSelected(t.isActive());
        filling = false;
    }

    private void clearForm() {
        filling = true;
        table.clearSelection();
        noField.setText("");
        nameField.setText("");
        capacitySpinner.setValue(4);
        activeCheck.setSelected(true);
        filling = false;
    }

    /** 폼 값 검사 후 DTO로 만든다. 틀리면 null */
    private CafeTableDTO readForm(int exceptNo) throws SQLException {
        Integer no = UIUtil.toInt(noField.getText());
        String name = nameField.getText().trim();
        if (no == null || no <= 0 || name.isEmpty()) {
            UIUtil.warn(this, "번호(숫자)와 표시 이름을 입력하세요.");
            return null;
        }
        if (dao.isDuplicate(no, name, exceptNo)) {
            UIUtil.warn(this, "같은 번호나 이름의 룸이 이미 있습니다.");
            return null;
        }
        return new CafeTableDTO(no, name, (Integer) capacitySpinner.getValue(), activeCheck.isSelected());
    }

    private void add() {
        try {
            CafeTableDTO t = readForm(-1);
            if (t == null) return;
            dao.insert(t);
            refresh();
            clearForm();
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }

    private void edit() {
        CafeTableDTO old = selected();
        if (old == null) {
            UIUtil.warn(this, "표에서 룸을 먼저 고르세요.");
            return;
        }
        try {
            CafeTableDTO t = readForm(old.getTableNo());
            if (t == null) return;
            if (t.getTableNo() != old.getTableNo() && usageDAO.countByTable(old.getTableNo()) > 0) {
                UIUtil.warn(this, "이용 기록이 있는 룸은 번호를 바꿀 수 없습니다.");
                return;
            }
            dao.update(old.getTableNo(), t);
            refresh();
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }

    /** 사용 여부 체크박스: 고른 룸에 바로 저장한다 */
    private void changeActive(boolean active) {
        CafeTableDTO t = selected();
        if (t == null) return;   // 새로 입력하는 중이면 [추가] 때 저장된다
        try {
            if (!active && usageDAO.findActiveByTable(t.getTableNo()) != null) {
                UIUtil.warn(this, "이용 중인 룸은 사용불가로 바꿀 수 없습니다.");
                filling = true;
                activeCheck.setSelected(true);
                filling = false;
                return;
            }
            dao.setActive(t.getTableNo(), active);
            int row = table.getSelectedRow();
            refresh();
            table.setRowSelectionInterval(row, row);
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }

    private void delete() {
        CafeTableDTO t = selected();
        if (t == null) {
            UIUtil.warn(this, "표에서 룸을 먼저 고르세요.");
            return;
        }
        try {
            if (usageDAO.countByTable(t.getTableNo()) > 0) {
                UIUtil.warn(this, "이용 기록이 있는 룸은 삭제할 수 없습니다. 대신 사용 여부 체크를 풀어 사용불가로 바꾸세요.");
                return;
            }
            if (!UIUtil.confirm(this, t.getTableName() + "을(를) 삭제할까요?")) return;
            dao.delete(t.getTableNo());
            refresh();
            clearForm();
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }
}
