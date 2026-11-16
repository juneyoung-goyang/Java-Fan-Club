package dicepos.staff;

import dicepos.common.Refreshable;
import dicepos.common.UIUtil;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.table.DefaultTableModel;

/** [관리자] 직원 관리 (① 김재민) */
public class StaffPanel extends JPanel implements Refreshable {
    private final DefaultTableModel model = UIUtil.tableModel(
            "번호", "이름", "아이디", "연락처", "권한", "고용 형태", "시급 / 월 기본급", "입사일", "기본 근무", "상태");
    private final JTable table = UIUtil.table(model);
    private final JTextField searchField = new JTextField(15);
    private final StaffDAO dao = new StaffDAO();
    private List<StaffDTO> list = new ArrayList<>();
    private String typeFilter = "전체";

    public StaffPanel() {
        super(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 위: 고용 형태 버튼과 검색창
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ButtonGroup group = new ButtonGroup();
        for (String type : new String[] {"전체", "정규직", "계약직", "시간제"}) {
            JToggleButton b = new JToggleButton(type, type.equals("전체"));
            b.addActionListener(e -> {
                typeFilter = type;
                refresh();
            });
            group.add(b);
            top.add(b);
        }
        top.add(new JLabel("   이름·아이디 검색"));
        top.add(searchField);
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                refresh();   // 글자를 칠 때마다 다시 조회
            }
        });

        // 아래: 버튼 줄
        JButton addButton = new JButton("추가");
        JButton editButton = new JButton("변경");
        JButton retireButton = new JButton("퇴사 처리");
        JButton memoButton = new JButton("메모 보기");
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(addButton);
        bottom.add(editButton);
        bottom.add(retireButton);
        bottom.add(memoButton);
        bottom.add(new JLabel("   비밀번호는 해시로만 저장되어 표에 나오지 않습니다."));

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        addButton.addActionListener(e -> {
            if (StaffDialog.open(this, null)) refresh();
        });
        editButton.addActionListener(e -> {
            StaffDTO s = selected();
            if (s != null && StaffDialog.open(this, s)) refresh();
        });
        retireButton.addActionListener(e -> retire());
        memoButton.addActionListener(e -> {
            StaffDTO s = selected();
            if (s == null) return;
            MemoDialog.open(this, s);
            refresh();
        });
        refresh();
    }

    @Override
    public void refresh() {
        try {
            list = dao.find(typeFilter, searchField.getText().trim());
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
            return;
        }
        model.setRowCount(0);
        for (StaffDTO s : list) {
            String pay = s.getHourlyWage() != null ? "시급 " + UIUtil.won(s.getHourlyWage())
                       : s.getMonthlyPay() != null ? "월 " + UIUtil.won(s.getMonthlyPay()) : "";
            model.addRow(new Object[] {s.getStaffId(), s.getName(), s.getLoginId(),
                    s.getPhone1() + "-" + s.getPhone2() + "-" + s.getPhone3(), s.getRole(), s.getEmpType(), pay,
                    s.getHireDate(), s.getWorkTime() == null ? "" : s.getWorkTime(), s.getStatus()});
        }
    }

    private StaffDTO selected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UIUtil.warn(this, "표에서 직원을 먼저 고르세요.");
            return null;
        }
        return list.get(row);
    }

    private void retire() {
        StaffDTO s = selected();
        if (s == null) return;
        if ("퇴사".equals(s.getStatus())) {
            UIUtil.info(this, "이미 퇴사 처리된 직원입니다.");
            return;
        }
        if (!UIUtil.confirm(this, s.getName() + " 직원을 퇴사 처리할까요?\n기록은 지우지 않고 남겨 둡니다.")) return;
        try {
            dao.retire(s.getStaffId());
            refresh();
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }
}
