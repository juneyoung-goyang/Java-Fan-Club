package dicepos.member;

import dicepos.common.Refreshable;
import dicepos.common.UIUtil;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/** [직원] 회원 관리 (② 송하안) */
public class MemberPanel extends JPanel implements Refreshable {
    private final DefaultTableModel model = UIUtil.tableModel("번호", "이름", "전화번호", "성별", "등급", "포인트", "누적 결제", "가입일");
    private final JTable table = UIUtil.table(model);
    private final JTextField searchField = new JTextField(15);
    private final JTextField nameField = new JTextField(10);
    private final JTextField phone1 = new JTextField("010", 3);
    private final JTextField phone2 = new JTextField(4);
    private final JTextField phone3 = new JTextField(4);
    private final JRadioButton maleRadio = new JRadioButton("남", true);
    private final JRadioButton femaleRadio = new JRadioButton("여");
    private final JLabel gradeLabel = new JLabel("-");
    private final MemberDAO dao = new MemberDAO();
    private List<MemberDTO> list = new ArrayList<>();

    public MemberPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("이름 또는 전화번호 뒤 4자리"));
        top.add(searchField);
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                refresh();   // 글자를 칠 때마다 다시 조회
            }
        });

        // 오른쪽 입력 폼
        ButtonGroup gender = new ButtonGroup();
        gender.add(maleRadio);
        gender.add(femaleRadio);
        JPanel genderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        genderPanel.add(maleRadio);
        genderPanel.add(femaleRadio);
        JPanel phonePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        phonePanel.add(phone1);
        phonePanel.add(new JLabel("-"));
        phonePanel.add(phone2);
        phonePanel.add(new JLabel("-"));
        phonePanel.add(phone3);

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 8));
        form.add(new JLabel("이름 *"));
        form.add(nameField);
        form.add(new JLabel("전화번호 *"));
        form.add(phonePanel);
        form.add(new JLabel("성별"));
        form.add(genderPanel);
        form.add(new JLabel("등급 (결제 때 자동)"));
        form.add(gradeLabel);

        JButton addButton = new JButton("등록");
        JButton editButton = new JButton("수정");
        JButton deleteButton = new JButton("삭제");
        JButton clearButton = new JButton("새로 입력");
        JPanel buttons = new JPanel(new GridLayout(2, 2, 5, 5));
        buttons.add(addButton);
        buttons.add(editButton);
        buttons.add(deleteButton);
        buttons.add(clearButton);

        JPanel right = new JPanel(new BorderLayout(0, 10));
        right.setBorder(BorderFactory.createTitledBorder("회원 정보"));
        right.add(form, BorderLayout.NORTH);
        right.add(buttons, BorderLayout.CENTER);
        right.add(new JLabel("<html>등급: 누적 결제 10만 원 이상 실버, 30만 원 이상 골드<br>"
                + "적립: 카드·현금의 일반 1%, 실버 3%, 골드 5%</html>"), BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(right, BorderLayout.EAST);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                fillForm();
            }
        });
        addButton.addActionListener(e -> add());
        editButton.addActionListener(e -> edit());
        deleteButton.addActionListener(e -> delete());
        clearButton.addActionListener(e -> clearForm());
        refresh();
    }

    @Override
    public void refresh() {
        Map<Integer, Integer> totals;
        try {
            list = dao.search(searchField.getText().trim());
            totals = dao.totalPaidMap();
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
            return;
        }
        model.setRowCount(0);
        for (MemberDTO m : list) {
            model.addRow(new Object[] {m.getMemberId(), m.getName(), m.getPhone1() + "-" + m.getPhone2() + "-" + m.getPhone3(),
                    "M".equals(m.getGender()) ? "남" : "여", m.getGrade(), String.format("%,dP", m.getPoint()),
                    UIUtil.won(totals.getOrDefault(m.getMemberId(), 0)), m.getJoinDate()});
        }
    }

    private MemberDTO selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : list.get(row);
    }

    private void fillForm() {
        MemberDTO m = selected();
        if (m == null) return;
        nameField.setText(m.getName());
        phone1.setText(m.getPhone1());
        phone2.setText(m.getPhone2());
        phone3.setText(m.getPhone3());
        maleRadio.setSelected("M".equals(m.getGender()));
        femaleRadio.setSelected("F".equals(m.getGender()));
        gradeLabel.setText(m.getGrade() + " / " + String.format("%,dP", m.getPoint()));
    }

    private void clearForm() {
        table.clearSelection();
        nameField.setText("");
        phone1.setText("010");
        phone2.setText("");
        phone3.setText("");
        maleRadio.setSelected(true);
        gradeLabel.setText("-");
    }

    /** 입력값이 맞으면 true */
    private boolean checkForm() {
        if (nameField.getText().trim().isEmpty()) {
            UIUtil.warn(this, "이름을 입력하세요.");
            return false;
        }
        if (!phone1.getText().trim().matches("[0-9]{2,3}") || !phone2.getText().trim().matches("[0-9]{3,4}")
                || !phone3.getText().trim().matches("[0-9]{4}")) {
            UIUtil.warn(this, "전화번호는 숫자만 입력하세요. (예: 010 - 1234 - 5678)");
            return false;
        }
        return true;
    }

    private String gender() {
        return maleRadio.isSelected() ? "M" : "F";
    }

    private void add() {
        if (!checkForm()) return;
        try {
            dao.insert(nameField.getText().trim(), phone1.getText().trim(), phone2.getText().trim(), phone3.getText().trim(), gender());
            refresh();
            clearForm();
        } catch (SQLIntegrityConstraintViolationException e) {
            UIUtil.warn(this, "이미 등록된 전화번호입니다.");
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }

    private void edit() {
        MemberDTO m = selected();
        if (m == null) {
            UIUtil.warn(this, "표에서 회원을 먼저 고르세요.");
            return;
        }
        if (!checkForm()) return;
        try {
            dao.update(m.getMemberId(), nameField.getText().trim(), phone1.getText().trim(), phone2.getText().trim(),
                    phone3.getText().trim(), gender());
            refresh();
        } catch (SQLIntegrityConstraintViolationException e) {
            UIUtil.warn(this, "다른 회원이 쓰는 전화번호입니다.");
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }

    private void delete() {
        MemberDTO m = selected();
        if (m == null) {
            UIUtil.warn(this, "표에서 회원을 먼저 고르세요.");
            return;
        }
        try {
            if (dao.hasPayments(m.getMemberId())) {
                UIUtil.warn(this, "결제 기록이 있는 회원은 삭제할 수 없습니다.");
                return;
            }
            if (!UIUtil.confirm(this, m.getName() + " 회원을 삭제할까요?")) return;
            dao.delete(m.getMemberId());
            refresh();
            clearForm();
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }
}
