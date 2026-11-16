package dicepos.staff;

import dicepos.common.Refreshable;
import dicepos.common.UIUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

/** [관리자] 근무·급여 관리 (① 김재민) */
public class WorkSalaryPanel extends JPanel implements Refreshable {
    private static final DateTimeFormatter INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JComboBox<StaffDTO> staffCombo = new JComboBox<>();
    private final JTextField monthField = new JTextField(YearMonth.now().toString(), 7);
    private final DefaultTableModel model = UIUtil.tableModel("날짜", "출근", "퇴근", "근무시간", "비고");
    private final JTable table = UIUtil.table(model);

    private final JLabel minutesLabel = new JLabel("-");
    private final JLabel rateLabel = new JLabel("-");
    private final JLabel baseLabel = new JLabel("-");
    private final JSpinner extraSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 10_000_000, 10_000));
    private final JLabel totalLabel = new JLabel("-");
    private final JLabel statusLabel = new JLabel("-");
    private final JLabel warnLabel = new JLabel(" ");
    private final JButton confirmButton = new JButton("급여 확정");
    private final JButton paidButton = new JButton("지급 완료 처리");
    private final JButton editButton = new JButton("시간 수정");
    private final JButton deleteButton = new JButton("기록 삭제");

    private final StaffDAO staffDAO = new StaffDAO();
    private final WorkLogDAO logDAO = new WorkLogDAO();
    private final SalaryDAO salaryDAO = new SalaryDAO();
    private List<WorkLogDTO> logs = new ArrayList<>();
    private StaffDTO staff;      // 조회한 직원
    private String month;        // 조회한 달
    private SalaryDTO salary;    // 확정된 급여 (없으면 null)
    private int workMinutes;
    private int baseAmount;

    public WorkSalaryPanel() {
        super(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton searchButton = new JButton("조회");
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("직원"));
        top.add(staffCombo);
        top.add(new JLabel("  월 (예: 2026-11)"));
        top.add(monthField);
        top.add(searchButton);

        // 왼쪽: 근무 기록
        JPanel logButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        logButtons.add(editButton);
        logButtons.add(deleteButton);
        JPanel left = new JPanel(new BorderLayout());
        left.setBorder(BorderFactory.createTitledBorder("근무 기록 (work_log)"));
        left.add(new JScrollPane(table), BorderLayout.CENTER);
        left.add(logButtons, BorderLayout.SOUTH);

        // 오른쪽: 급여 계산
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 8));
        form.add(new JLabel("누적 근무시간"));
        form.add(minutesLabel);
        form.add(new JLabel("시급 / 월 기본급"));
        form.add(rateLabel);
        form.add(new JLabel("기본 급여"));
        form.add(baseLabel);
        form.add(new JLabel("추가 수당 (주휴수당 등)"));
        form.add(extraSpinner);
        form.add(new JLabel("지급액"));
        form.add(totalLabel);
        form.add(new JLabel("지급 상태"));
        form.add(statusLabel);
        warnLabel.setForeground(Color.RED);
        JPanel salaryButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        salaryButtons.add(confirmButton);
        salaryButtons.add(paidButton);
        JPanel right = new JPanel(new BorderLayout(0, 8));
        right.setBorder(BorderFactory.createTitledBorder("급여 계산"));
        right.add(form, BorderLayout.NORTH);
        right.add(warnLabel, BorderLayout.CENTER);
        right.add(salaryButtons, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.6);

        add(top, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        add(new JLabel("휴게시간: 4시간 이상 30분, 8시간 이상 1시간을 빼고 저장합니다. 급여는 달이 끝난 뒤 한 번만 확정할 수 있습니다."),
                BorderLayout.SOUTH);

        searchButton.addActionListener(e -> search());
        extraSpinner.addChangeListener(e -> showTotal());
        editButton.addActionListener(e -> editTime());
        deleteButton.addActionListener(e -> deleteLog());
        confirmButton.addActionListener(e -> confirmSalary());
        paidButton.addActionListener(e -> markPaid());
        setButtons();
        refresh();
    }

    /** 화면을 열 때 직원 목록을 다시 읽는다. */
    @Override
    public void refresh() {
        Object before = staffCombo.getSelectedItem();
        staffCombo.removeAllItems();
        try {
            for (StaffDTO s : staffDAO.findWorking()) {
                staffCombo.addItem(s);
            }
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
        // JComboBox는 toString()으로 글자를 보여 주므로 이름만 보이게 렌더러를 쓴다
        staffCombo.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index,
                                                                   boolean selected, boolean focus) {
                String text = value == null ? "" : ((StaffDTO) value).getName() + " (" + ((StaffDTO) value).getEmpType() + ")";
                return super.getListCellRendererComponent(list, text, index, selected, focus);
            }
        });
        if (before != null) {
            for (int i = 0; i < staffCombo.getItemCount(); i++) {
                if (staffCombo.getItemAt(i).getStaffId() == ((StaffDTO) before).getStaffId()) staffCombo.setSelectedIndex(i);
            }
        }
    }

    private void search() {
        StaffDTO s = (StaffDTO) staffCombo.getSelectedItem();
        if (s == null) return;
        YearMonth ym;
        try {
            ym = YearMonth.parse(monthField.getText().trim());
        } catch (DateTimeParseException e) {
            UIUtil.warn(this, "월을 2026-11 처럼 입력하세요.");
            return;
        }
        staff = s;
        month = ym.toString();
        load();
    }

    /** 고른 직원·달의 근무 기록과 급여를 다시 읽는다. */
    private void load() {
        try {
            logs = logDAO.findByMonth(staff.getStaffId(), month);
            salary = salaryDAO.find(staff.getStaffId(), month);
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
            return;
        }
        model.setRowCount(0);
        workMinutes = 0;
        int missing = 0;
        for (WorkLogDTO log : logs) {
            Integer m = log.getWorkMinutes();
            if (log.getClockOut() == null) missing++;
            else if (m != null) workMinutes += m;
            model.addRow(new Object[] {log.getWorkDate(), UIUtil.time(log.getClockIn()),
                    log.getClockOut() == null ? "(퇴근 안 찍음)" : UIUtil.dateTime(log.getClockOut()),
                    m == null ? "" : UIUtil.minutes(m), log.getNote() == null ? "" : log.getNote()});
        }

        if (salary != null) {
            // 확정된 달은 저장된 값을 그대로 보여 준다 (시급이 바뀌어도 지난달 급여는 그대로)
            minutesLabel.setText(UIUtil.minutes(salary.getWorkMinutes()));
            baseAmount = salary.getBaseAmount();
            extraSpinner.setValue(salary.getExtraAmount());
            statusLabel.setText(salary.getStatus() + (salary.getPayDate() == null ? "" : " (" + salary.getPayDate() + ")"));
        } else {
            minutesLabel.setText(UIUtil.minutes(workMinutes));
            if ("시간제".equals(staff.getEmpType())) {
                baseAmount = workMinutes * staff.getHourlyWage() / 60;
            } else {
                baseAmount = staff.getMonthlyPay() == null ? 0 : staff.getMonthlyPay();
            }
            extraSpinner.setValue(0);
            statusLabel.setText("확정 전");
        }
        rateLabel.setText(staff.getHourlyWage() != null ? "시급 " + UIUtil.won(staff.getHourlyWage())
                : "월 " + UIUtil.won(staff.getMonthlyPay() == null ? 0 : staff.getMonthlyPay()));
        baseLabel.setText(UIUtil.won(baseAmount));
        warnLabel.setText(missing > 0 ? "퇴근을 찍지 않은 날이 " + missing + "일 있습니다. 시간을 고치기 전까지 급여 계산에서 뺍니다." : " ");
        showTotal();
        setButtons();
    }

    private void showTotal() {
        totalLabel.setText(UIUtil.won(baseAmount + (Integer) extraSpinner.getValue()));
    }

    private void setButtons() {
        boolean loaded = staff != null;
        boolean confirmed = salary != null;
        editButton.setEnabled(loaded && !confirmed);
        deleteButton.setEnabled(loaded && !confirmed);
        extraSpinner.setEnabled(loaded && !confirmed);
        confirmButton.setEnabled(loaded && !confirmed);
        paidButton.setEnabled(confirmed && "미지급".equals(salary.getStatus()));
    }

    private WorkLogDTO selectedLog() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UIUtil.warn(this, "근무 기록 표에서 날짜를 먼저 고르세요.");
            return null;
        }
        return logs.get(row);
    }

    private void editTime() {
        WorkLogDTO log = selectedLog();
        if (log == null) return;
        JTextField inField = new JTextField(UIUtil.dateTime(log.getClockIn()));
        JTextField outField = new JTextField(log.getClockOut() == null
                ? log.getWorkDate() + " " : UIUtil.dateTime(log.getClockOut()));
        JTextField noteField = new JTextField(log.getNote() == null ? "" : log.getNote());
        JPanel p = new JPanel(new GridLayout(0, 2, 5, 5));
        p.add(new JLabel("출근 (yyyy-MM-dd HH:mm)"));
        p.add(inField);
        p.add(new JLabel("퇴근 (yyyy-MM-dd HH:mm)"));
        p.add(outField);
        p.add(new JLabel("고친 사유 *"));
        p.add(noteField);
        if (JOptionPane.showConfirmDialog(this, p, "근무 시간 수정", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        LocalDateTime in;
        LocalDateTime out;
        try {
            in = LocalDateTime.parse(inField.getText().trim(), INPUT);
            out = LocalDateTime.parse(outField.getText().trim(), INPUT);
        } catch (DateTimeParseException e) {
            UIUtil.warn(this, "시각을 2026-11-03 18:00 처럼 입력하세요.");
            return;
        }
        if (!out.isAfter(in)) {
            UIUtil.warn(this, "퇴근 시각이 출근 시각보다 빠릅니다.");
            return;
        }
        if (noteField.getText().trim().isEmpty()) {
            UIUtil.warn(this, "고친 사유를 적어 주세요.");
            return;
        }
        try {
            logDAO.updateTime(log.getLogId(), in, out, noteField.getText().trim());
            load();
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }

    private void deleteLog() {
        WorkLogDTO log = selectedLog();
        if (log == null) return;
        if (!UIUtil.confirm(this, log.getWorkDate() + " 근무 기록을 삭제할까요?")) return;
        try {
            logDAO.delete(log.getLogId());
            load();
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }

    private void confirmSalary() {
        if (!YearMonth.parse(month).isBefore(YearMonth.now())) {
            UIUtil.warn(this, "급여는 달이 끝난 뒤에 확정할 수 있습니다.");
            return;
        }
        int extra = (Integer) extraSpinner.getValue();
        if (!UIUtil.confirm(this, staff.getName() + "의 " + month + " 급여를 " + UIUtil.won(baseAmount + extra)
                + "으로 확정할까요?\n확정하면 그달 근무 기록은 고칠 수 없습니다.")) return;
        try {
            salaryDAO.insert(staff.getStaffId(), month, workMinutes, baseAmount, extra);
            load();
        } catch (java.sql.SQLIntegrityConstraintViolationException e) {
            UIUtil.warn(this, "이미 확정된 달입니다.");
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }

    private void markPaid() {
        if (!UIUtil.confirm(this, "오늘 날짜로 지급 완료 처리할까요?")) return;
        try {
            salaryDAO.markPaid(salary.getSalaryId());
            load();
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }
}
