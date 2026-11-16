package dicepos.table;

import dicepos.common.Refreshable;
import dicepos.common.UIUtil;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/** [관리자] 이용 요금 설정 (⑤ 서윤채) */
public class FeeSettingPanel extends JPanel implements Refreshable {
    private final DefaultTableModel model = UIUtil.tableModel("번호", "기본 시간", "1인 기본요금", "추가 단위", "1인 추가요금", "적용 시작일", "상태");
    private final JTable table = UIUtil.table(model);
    private final JTextField baseMinField = new JTextField(8);
    private final JTextField basePriceField = new JTextField(8);
    private final JTextField unitField = new JTextField(8);
    private final JTextField extraPriceField = new JTextField(8);
    private final JTextField dateField = new JTextField(LocalDate.now().plusDays(1).toString(), 8);
    private final JLabel exampleLabel = new JLabel(" ");
    private final FeeSettingDAO dao = new FeeSettingDAO();
    private List<FeeSettingDTO> list = new ArrayList<>();

    public FeeSettingPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 8));
        form.add(new JLabel("기본 시간(분) *"));
        form.add(baseMinField);
        form.add(new JLabel("1인 기본요금 *"));
        form.add(basePriceField);
        form.add(new JLabel("추가 요금 단위(분) *"));
        form.add(unitField);
        form.add(new JLabel("1인 추가요금 *"));
        form.add(extraPriceField);
        form.add(new JLabel("적용 시작일 *"));
        form.add(dateField);

        JButton addButton = new JButton("등록");
        JButton editButton = new JButton("수정");
        JButton deleteButton = new JButton("삭제");
        JPanel buttons = new JPanel(new GridLayout(1, 3, 5, 5));
        buttons.add(addButton);
        buttons.add(editButton);
        buttons.add(deleteButton);

        JPanel left = new JPanel(new BorderLayout(0, 10));
        left.setBorder(BorderFactory.createTitledBorder("요금 입력"));
        left.add(form, BorderLayout.NORTH);
        left.add(buttons, BorderLayout.CENTER);
        left.add(exampleLabel, BorderLayout.SOUTH);

        add(left, BorderLayout.WEST);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(new JLabel("이미 적용이 시작된 요금은 지난 결제에 쓰였으므로 고치지 않고, 새 적용 시작일로 새 요금을 등록합니다."),
                BorderLayout.SOUTH);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) fillForm();
        });
        addButton.addActionListener(e -> add());
        editButton.addActionListener(e -> edit());
        deleteButton.addActionListener(e -> delete());
        refresh();
    }

    @Override
    public void refresh() {
        try {
            list = dao.findAll();
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
            return;
        }
        model.setRowCount(0);
        boolean currentFound = false;   // 목록은 적용 시작일이 늦은 것부터라서 처음 만나는 '시작됨'이 지금 요금
        for (FeeSettingDTO f : list) {
            String state;
            if (f.getApplyDate().isAfter(LocalDate.now())) {
                state = "예정";
            } else if (!currentFound) {
                state = "적용 중";
                currentFound = true;
            } else {
                state = "지난 요금";
            }
            model.addRow(new Object[] {f.getFeeId(), f.getBaseMinutes() + "분", UIUtil.won(f.getBasePrice()),
                    f.getExtraUnit() + "분마다", UIUtil.won(f.getExtraPrice()), f.getApplyDate(), state});
        }
    }

    private FeeSettingDTO selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : list.get(row);
    }

    private void fillForm() {
        FeeSettingDTO f = selected();
        if (f == null) return;
        baseMinField.setText(String.valueOf(f.getBaseMinutes()));
        basePriceField.setText(String.valueOf(f.getBasePrice()));
        unitField.setText(String.valueOf(f.getExtraUnit()));
        extraPriceField.setText(String.valueOf(f.getExtraPrice()));
        dateField.setText(f.getApplyDate().toString());
        showExample(f);
    }

    /** "130분이면 1인 7,000원" 처럼 예시를 보여 준다 */
    private void showExample(FeeSettingDTO f) {
        int over = Math.max(0, 130 - f.getBaseMinutes());
        int units = (over + f.getExtraUnit() - 1) / f.getExtraUnit();
        exampleLabel.setText("예) 130분 이용 → 1인 " + UIUtil.won(f.getBasePrice() + units * f.getExtraPrice()));
    }

    /** 입력값 검사. 틀리면 null */
    private FeeSettingDTO readForm(int feeId) {
        Integer baseMin = UIUtil.toInt(baseMinField.getText());
        Integer basePrice = UIUtil.toInt(basePriceField.getText());
        Integer unit = UIUtil.toInt(unitField.getText());
        Integer extra = UIUtil.toInt(extraPriceField.getText());
        LocalDate date = UIUtil.toDate(dateField.getText());
        if (baseMin == null || basePrice == null || unit == null || extra == null) {
            UIUtil.warn(this, "시간과 요금은 숫자로 입력하세요.");
            return null;
        }
        if (baseMin <= 0 || unit <= 0 || basePrice < 0 || extra < 0) {
            UIUtil.warn(this, "기본 시간과 추가 단위는 1분 이상, 요금은 0원 이상이어야 합니다.");
            return null;
        }
        if (date == null) {
            UIUtil.warn(this, "적용 시작일을 2026-12-01 처럼 입력하세요.");
            return null;
        }
        return new FeeSettingDTO(feeId, baseMin, basePrice, unit, extra, date);
    }

    private void add() {
        FeeSettingDTO f = readForm(0);
        if (f == null) return;
        if (f.getApplyDate().isBefore(LocalDate.now())) {
            UIUtil.warn(this, "적용 시작일은 오늘 이후로 정하세요. 지난 날짜로 넣으면 이미 끝난 결제와 맞지 않습니다.");
            return;
        }
        try {
            dao.insert(f);
            showExample(f);
            refresh();
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }

    private void edit() {
        FeeSettingDTO old = selected();
        if (old == null) {
            UIUtil.warn(this, "표에서 요금을 먼저 고르세요.");
            return;
        }
        if (!old.getApplyDate().isAfter(LocalDate.now())) {
            UIUtil.warn(this, "이미 적용이 시작된 요금은 고칠 수 없습니다. 새 요금을 등록하세요.");
            return;
        }
        FeeSettingDTO f = readForm(old.getFeeId());
        if (f == null) return;
        if (!f.getApplyDate().isAfter(LocalDate.now())) {
            UIUtil.warn(this, "적용 시작일은 내일 이후로 정하세요.");
            return;
        }
        try {
            dao.update(f);
            refresh();
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }

    private void delete() {
        FeeSettingDTO f = selected();
        if (f == null) {
            UIUtil.warn(this, "표에서 요금을 먼저 고르세요.");
            return;
        }
        if (!f.getApplyDate().isAfter(LocalDate.now())) {
            UIUtil.warn(this, "이미 적용이 시작된 요금은 삭제할 수 없습니다.");
            return;
        }
        if (!UIUtil.confirm(this, f.getApplyDate() + "부터 적용할 요금을 삭제할까요?")) return;
        try {
            dao.delete(f.getFeeId());
            refresh();
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }
}
