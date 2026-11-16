package dicepos.staff;

import dicepos.common.UIUtil;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** 직원 상세 정보 팝업. 추가와 변경에 같이 쓴다. (① 김재민) */
public class StaffDialog extends JDialog {
    public static final int MIN_WAGE = 10320;   // 2026년 최저시급

    private final StaffDTO old;   // 변경이면 원래 정보, 추가면 null
    private final JTextField nameField = new JTextField(12);
    private final JTextField idField = new JTextField(12);
    private final JTextField phone1 = new JTextField("010", 3);
    private final JTextField phone2 = new JTextField(4);
    private final JTextField phone3 = new JTextField(4);
    private final JPasswordField pwField = new JPasswordField(12);
    private final JPasswordField pwCheckField = new JPasswordField(12);
    private final JRadioButton adminRadio = new JRadioButton("관리자");
    private final JRadioButton staffRadio = new JRadioButton("직원", true);
    private final JComboBox<String> typeCombo = new JComboBox<>(new String[] {"시간제", "정규직", "계약직"});
    private final JTextField wageField = new JTextField(12);
    private final JTextField monthlyField = new JTextField(12);
    private final JTextField hireField = new JTextField(LocalDate.now().toString(), 12);
    private final JTextField contractEndField = new JTextField(12);
    private final JTextField workTimeField = new JTextField(12);
    private final JComboBox<String> statusCombo = new JComboBox<>(new String[] {"재직", "퇴사"});
    private final JTextField addressField = new JTextField(20);
    private final StaffDAO dao = new StaffDAO();
    private boolean saved = false;

    /** 팝업을 띄우고 저장했으면 true. old가 null이면 추가, 아니면 변경 */
    public static boolean open(Component parent, StaffDTO old) {
        StaffDialog dialog = new StaffDialog(parent, old);
        dialog.setVisible(true);
        return dialog.saved;
    }

    private StaffDialog(Component parent, StaffDTO old) {
        super(SwingUtilities.getWindowAncestor(parent), old == null ? "직원 추가" : "직원 정보 변경", ModalityType.APPLICATION_MODAL);
        this.old = old;

        ButtonGroup roleGroup = new ButtonGroup();
        roleGroup.add(adminRadio);
        roleGroup.add(staffRadio);
        JPanel rolePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        rolePanel.add(adminRadio);
        rolePanel.add(staffRadio);

        JPanel phonePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        phonePanel.add(phone1);
        phonePanel.add(new JLabel(" - "));
        phonePanel.add(phone2);
        phonePanel.add(new JLabel(" - "));
        phonePanel.add(phone3);

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        addRow(form, "이름 *", nameField);
        addRow(form, "아이디 *", idField);
        addRow(form, "연락처 *", phonePanel);
        addRow(form, old == null ? "비밀번호 *" : "새 비밀번호 (비우면 그대로)", pwField);
        addRow(form, "비밀번호 확인", pwCheckField);
        addRow(form, "권한 *", rolePanel);
        addRow(form, "고용 형태 *", typeCombo);
        addRow(form, "시급 (시간제)", wageField);
        addRow(form, "월 기본급 (정규·계약)", monthlyField);
        addRow(form, "입사일 * (예: 2026-08-15)", hireField);
        addRow(form, "계약 종료일 (계약직)", contractEndField);
        addRow(form, "기본 근무 시간대", workTimeField);
        if (old != null) addRow(form, "재직 상태", statusCombo);
        addRow(form, "주소", addressField);

        JLabel note = new JLabel("* 필수 입력. 고용 형태를 고르면 시급 / 월 기본급 칸 중 하나만 켜집니다. 시급이 최저시급보다 낮으면 저장할 수 없습니다.");

        JButton saveButton = new JButton("저장");
        JButton cancelButton = new JButton("취소");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(saveButton);
        buttons.add(cancelButton);

        JPanel south = new JPanel(new BorderLayout());
        south.add(note, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.SOUTH);

        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(form, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);
        setContentPane(root);

        // 고용 형태에 따라 시급 / 월 기본급 / 계약 종료일 칸을 켜고 끈다
        typeCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) updateTypeFields();
        });
        saveButton.addActionListener(e -> save());
        cancelButton.addActionListener(e -> dispose());

        if (old != null) fill(old);
        updateTypeFields();
        pack();
        setLocationRelativeTo(parent);
    }

    private void addRow(JPanel form, String label, Component field) {
        form.add(new JLabel(label));
        form.add(field);
    }

    private void fill(StaffDTO s) {
        nameField.setText(s.getName());
        idField.setText(s.getLoginId());
        phone1.setText(s.getPhone1());
        phone2.setText(s.getPhone2());
        phone3.setText(s.getPhone3());
        adminRadio.setSelected("관리자".equals(s.getRole()));
        staffRadio.setSelected(!"관리자".equals(s.getRole()));
        typeCombo.setSelectedItem(s.getEmpType());
        wageField.setText(s.getHourlyWage() == null ? "" : String.valueOf(s.getHourlyWage()));
        monthlyField.setText(s.getMonthlyPay() == null ? "" : String.valueOf(s.getMonthlyPay()));
        hireField.setText(s.getHireDate().toString());
        contractEndField.setText(s.getContractEnd() == null ? "" : s.getContractEnd().toString());
        workTimeField.setText(s.getWorkTime() == null ? "" : s.getWorkTime());
        statusCombo.setSelectedItem(s.getStatus());
        addressField.setText(s.getAddress() == null ? "" : s.getAddress());
    }

    private void updateTypeFields() {
        String type = (String) typeCombo.getSelectedItem();
        boolean hourly = "시간제".equals(type);
        wageField.setEnabled(hourly);
        monthlyField.setEnabled(!hourly);
        contractEndField.setEnabled("계약직".equals(type));
        if (hourly) monthlyField.setText("");
        else wageField.setText("");
        if (!"계약직".equals(type)) contractEndField.setText("");
    }

    private boolean isDigits(String text, int minLength, int maxLength) {
        return text.matches("[0-9]{" + minLength + "," + maxLength + "}");
    }

    private void save() {
        String name = nameField.getText().trim();
        String loginId = idField.getText().trim();
        String p1 = phone1.getText().trim();
        String p2 = phone2.getText().trim();
        String p3 = phone3.getText().trim();
        String pw = new String(pwField.getPassword());
        String pwCheck = new String(pwCheckField.getPassword());
        String type = (String) typeCombo.getSelectedItem();

        if (name.isEmpty() || loginId.isEmpty()) {
            UIUtil.warn(this, "이름과 아이디를 입력하세요.");
            return;
        }
        if (!isDigits(p1, 2, 3) || !isDigits(p2, 3, 4) || !isDigits(p3, 4, 4)) {
            UIUtil.warn(this, "전화번호는 숫자만 입력하세요. (예: 010 - 1234 - 5678)");
            return;
        }
        if (old == null && pw.isEmpty()) {
            UIUtil.warn(this, "비밀번호를 입력하세요.");
            return;
        }
        if (!pw.equals(pwCheck)) {
            UIUtil.warn(this, "비밀번호와 비밀번호 확인이 다릅니다.");
            return;
        }
        if (!pw.isEmpty() && pw.length() < 4) {
            UIUtil.warn(this, "비밀번호는 4자 이상으로 정하세요.");
            return;
        }

        Integer wage = null;
        Integer monthly = null;
        if ("시간제".equals(type)) {
            wage = UIUtil.toInt(wageField.getText());
            if (wage == null) {
                UIUtil.warn(this, "시급을 숫자로 입력하세요.");
                return;
            }
            if (wage < MIN_WAGE) {
                UIUtil.warn(this, "시급이 최저시급(" + UIUtil.won(MIN_WAGE) + ")보다 낮습니다.");
                return;
            }
        } else {
            monthly = UIUtil.toInt(monthlyField.getText());
            if (monthly == null || monthly <= 0) {
                UIUtil.warn(this, "월 기본급을 숫자로 입력하세요.");
                return;
            }
        }

        LocalDate hireDate = UIUtil.toDate(hireField.getText());
        if (hireDate == null) {
            UIUtil.warn(this, "입사일을 2026-08-15 처럼 입력하세요.");
            return;
        }
        LocalDate contractEnd = null;
        if ("계약직".equals(type)) {
            contractEnd = UIUtil.toDate(contractEndField.getText());
            if (contractEnd == null || !contractEnd.isAfter(hireDate)) {
                UIUtil.warn(this, "계약 종료일을 입사일보다 뒤의 날짜로 입력하세요.");
                return;
            }
        }

        try {
            int myId = old == null ? 0 : old.getStaffId();
            if (dao.isLoginIdTaken(loginId, myId)) {
                UIUtil.warn(this, "이미 쓰고 있는 아이디입니다.");
                return;
            }
            String salt = null;
            String hash = null;
            if (!pw.isEmpty()) {
                salt = PasswordUtil.newSalt();
                hash = PasswordUtil.hash(pw, salt);
            }
            String role = adminRadio.isSelected() ? "관리자" : "직원";
            String status = old == null ? "재직" : (String) statusCombo.getSelectedItem();
            String workTime = emptyToNull(workTimeField.getText());
            String address = emptyToNull(addressField.getText());
            StaffDTO s = new StaffDTO(myId, loginId, hash, salt, name, p1, p2, p3, role, type, wage, monthly,
                    hireDate, contractEnd, status, address, workTime, old == null ? null : old.getMemo());
            if (old == null) dao.insert(s);
            else dao.update(s);
            saved = true;
            dispose();
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }

    private String emptyToNull(String text) {
        return text.trim().isEmpty() ? null : text.trim();
    }
}
