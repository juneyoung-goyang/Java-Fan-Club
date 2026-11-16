package dicepos.table;

import dicepos.common.UIUtil;
import dicepos.member.MemberDAO;
import dicepos.member.MemberDTO;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

/** 이용 시작 팝업: 인원과 회원을 입력해 입장을 등록한다. (⑤ 서윤채) */
public class StartUsageDialog extends JDialog {
    private final CafeTableDTO table;
    private final JSpinner peopleSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 20, 1));
    private final JTextField searchField = new JTextField(10);
    private final DefaultComboBoxModel<Object> memberModel = new DefaultComboBoxModel<>();
    private final JComboBox<Object> memberCombo = new JComboBox<>(memberModel);
    private final MemberDAO memberDAO = new MemberDAO();
    private final TableUsageDAO usageDAO = new TableUsageDAO();
    private boolean started = false;

    /** 입장을 등록했으면 true */
    public static boolean open(Component parent, CafeTableDTO table) {
        StartUsageDialog d = new StartUsageDialog(parent, table);
        d.setVisible(true);
        return d.started;
    }

    private StartUsageDialog(Component parent, CafeTableDTO table) {
        super(SwingUtilities.getWindowAncestor(parent), "이용 시작 — " + table.getTableName(), ModalityType.APPLICATION_MODAL);
        this.table = table;
        memberModel.addElement("비회원");

        JButton searchButton = new JButton("회원 검색");
        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        searchRow.add(searchField);
        searchRow.add(searchButton);

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("정원"));
        form.add(new JLabel(table.getCapacity() + "명"));
        form.add(new JLabel("인원 *"));
        form.add(peopleSpinner);
        form.add(new JLabel("이름 또는 전화번호 뒤 4자리"));
        form.add(searchRow);
        form.add(new JLabel("회원"));
        form.add(memberCombo);

        JButton ok = new JButton("이용 시작");
        JButton cancel = new JButton("취소");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(ok);
        buttons.add(cancel);

        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(form, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);

        searchButton.addActionListener(e -> searchMember());
        searchField.addActionListener(e -> searchMember());
        ok.addActionListener(e -> start());
        cancel.addActionListener(e -> dispose());
        pack();
        setLocationRelativeTo(parent);
    }

    private void searchMember() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) return;
        memberModel.removeAllElements();
        memberModel.addElement("비회원");
        try {
            List<MemberDTO> found = memberDAO.search(keyword);
            for (MemberDTO m : found) {
                memberModel.addElement(new MemberItem(m));
            }
            if (found.isEmpty()) UIUtil.info(this, "찾는 회원이 없습니다.");
            else memberCombo.setSelectedIndex(1);
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }

    private void start() {
        int people = (Integer) peopleSpinner.getValue();
        if (people > table.getCapacity()) {
            UIUtil.warn(this, "인원(" + people + "명)이 정원(" + table.getCapacity() + "명)보다 많습니다.");
            return;
        }
        Object choice = memberCombo.getSelectedItem();
        Integer memberId = choice instanceof MemberItem ? ((MemberItem) choice).member.getMemberId() : null;
        try {
            CafeTableDTO now = new CafeTableDAO().findByNo(table.getTableNo());
            if (now == null || !now.isActive()) {
                UIUtil.warn(this, "사용불가 룸에는 입장할 수 없습니다.");
                return;
            }
            if (usageDAO.findActiveByTable(table.getTableNo()) != null) {
                UIUtil.warn(this, "이미 이용 중인 테이블입니다.");
                return;
            }
            usageDAO.start(table.getTableNo(), memberId, people);
            started = true;
            dispose();
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }

    /** 콤보박스에 "김민수 (010-****-5678)"로 보이게 감싼다 */
    private static class MemberItem {
        final MemberDTO member;

        MemberItem(MemberDTO member) {
            this.member = member;
        }

        @Override
        public String toString() {
            return member.getName() + " (" + member.getPhone1() + "-****-" + member.getPhone3() + ", " + member.getGrade() + ")";
        }
    }
}
