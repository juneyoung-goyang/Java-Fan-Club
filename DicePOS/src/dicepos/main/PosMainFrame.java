package dicepos.main;

import dicepos.common.AuthDialog;
import dicepos.common.Refreshable;
import dicepos.common.Session;
import dicepos.staff.CheckDialog;
import dicepos.staff.StaffDTO;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

/**
 * POS 메인 창 (④ 박준영). 왼쪽은 메뉴 버튼, 가운데는 팀원들의 화면이 카드처럼 바뀐다.
 * POS를 켜면 직원 화면이 잠겨 있고, 근무 중인 직원이 자기 비밀번호로 풀어야 쓸 수 있다. (보안 기능은 ① 김재민)
 */
public class PosMainFrame extends JFrame {
    private final CardLayout cards = new CardLayout();
    private final JPanel center = new JPanel(cards);
    private final JPanel staffMenu = new JPanel(new GridLayout(0, 1, 0, 5));
    private final JPanel adminMenu = new JPanel(new GridLayout(0, 1, 0, 5));
    private final TitledBorder adminTitle = BorderFactory.createTitledBorder("관리자 화면 (잠김)");
    private final JLabel workerLabel = new JLabel();
    private final JButton changeButton = new JButton("근무자 변경");
    private final JButton lockButton = new JButton("화면 잠금");
    private final List<JButton> menuButtons = new ArrayList<>();
    private final Map<String, JPanel> screens = new HashMap<>();

    public PosMainFrame() {
        super("다이스 POS");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1300, 812);
        setLocationRelativeTo(null);   // 화면 가운데

        center.add(new JLabel("왼쪽에서 화면을 고르세요", SwingConstants.CENTER), "처음");
        center.add(makeLockScreen(), "잠금");

        add(makeSide(), BorderLayout.WEST);
        add(center, BorderLayout.CENTER);
        lock();   // 켜면 잠긴 상태로 시작
    }

    private JPanel makeSide() {
        staffMenu.setBorder(BorderFactory.createTitledBorder("직원 화면"));
        adminMenu.setBorder(adminTitle);

        JPanel menus = new JPanel(new BorderLayout(0, 10));
        menus.add(staffMenu, BorderLayout.NORTH);
        menus.add(adminMenu, BorderLayout.CENTER);

        JButton inButton = new JButton("출근");
        JButton outButton = new JButton("퇴근");
        JPanel inOut = new JPanel(new GridLayout(1, 2, 5, 0));
        inOut.add(inButton);
        inOut.add(outButton);
        JPanel workButtons = new JPanel(new GridLayout(0, 1, 0, 5));
        workButtons.add(inOut);
        workButtons.add(changeButton);
        workButtons.add(lockButton);
        JPanel worker = new JPanel(new BorderLayout(0, 5));
        worker.setBorder(BorderFactory.createTitledBorder("근무자"));
        worker.add(workerLabel, BorderLayout.NORTH);
        worker.add(workButtons, BorderLayout.CENTER);

        JPanel side = new JPanel(new BorderLayout());
        side.setPreferredSize(new Dimension(200, 0));
        side.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        side.add(menus, BorderLayout.NORTH);
        side.add(worker, BorderLayout.SOUTH);

        inButton.addActionListener(e -> clockIn());
        outButton.addActionListener(e -> clockOut());
        changeButton.addActionListener(e -> unlock());
        lockButton.addActionListener(e -> lock());
        return side;
    }

    /** 잠겨 있을 때 가운데에 보이는 화면 */
    private JPanel makeLockScreen() {
        JLabel text = new JLabel("직원 화면이 잠겨 있습니다. 출근한 직원이 잠금을 풀어 주세요.", SwingConstants.CENTER);
        text.setFont(text.getFont().deriveFont(18f));
        JButton inButton = new JButton("출근");
        JButton unlockButton = new JButton("잠금 해제");
        JPanel buttons = new JPanel(new FlowLayout());
        buttons.add(inButton);
        buttons.add(unlockButton);
        JPanel box = new JPanel(new GridLayout(2, 1, 0, 10));
        box.add(text);
        box.add(buttons);
        JPanel p = new JPanel(new GridBagLayout());   // 가운데에 작게 모은다
        p.add(box);
        inButton.addActionListener(e -> clockIn());
        unlockButton.addActionListener(e -> unlock());
        return p;
    }

    /** 팀원 화면을 메인 창에 붙인다. admin이 true면 관리자 화면 쪽에 버튼이 생긴다. */
    public void addScreen(String name, JPanel screen, boolean admin) {
        center.add(screen, name);
        screens.put(name, screen);
        JButton button = new JButton(name);
        button.addActionListener(e -> open(name, admin));
        (admin ? adminMenu : staffMenu).add(button);
        menuButtons.add(button);
        button.setEnabled(Session.isLoggedIn());
        revalidate();
    }

    private void open(String name, boolean admin) {
        if (!Session.isLoggedIn()) return;
        if (admin && !Session.isAdminUnlocked()) {
            if (!AuthDialog.confirmAdmin(this)) return;   // 관리자 확인 실패
        }
        if (!admin) Session.lockAdmin();                  // 직원 화면으로 오면 관리자 화면은 다시 잠금
        showAdminState();
        JPanel screen = screens.get(name);
        if (screen instanceof Refreshable) ((Refreshable) screen).refresh();   // 열 때마다 DB를 다시 읽는다
        cards.show(center, name);
    }

    private void showAdminState() {
        adminTitle.setTitle(Session.isAdminUnlocked() ? "관리자 화면 (열림)" : "관리자 화면 (잠김)");
        adminMenu.repaint();
    }

    /** 화면 잠금: 직원·관리자 화면을 모두 잠근다 */
    private void lock() {
        Session.logout();
        workerLabel.setText("지금 근무자: 없음 (잠김)");
        setMenusEnabled(false);
        showAdminState();
        cards.show(center, "잠금");
    }

    /** 근무자 확인: 출근한 직원이 자기 비밀번호로 잠금을 푼다 (근무자 변경도 같다) */
    private void unlock() {
        StaffDTO s = CheckDialog.show(this, CheckDialog.UNLOCK);
        if (s == null) return;
        Session.lockAdmin();
        Session.login(s.getStaffId(), s.getName());
        workerLabel.setText("지금 근무자: " + s.getName());
        setMenusEnabled(true);
        showAdminState();
        cards.show(center, "처음");
    }

    /** 출근을 기록하고, 잠겨 있으면 그 직원으로 바로 잠금을 푼다 */
    private void clockIn() {
        StaffDTO s = CheckDialog.show(this, CheckDialog.CLOCK_IN);
        if (s == null || Session.isLoggedIn()) return;
        Session.login(s.getStaffId(), s.getName());
        workerLabel.setText("지금 근무자: " + s.getName());
        setMenusEnabled(true);
        cards.show(center, "처음");
    }

    /** 퇴근을 기록한다. 지금 POS를 쓰던 사람이 퇴근하면 화면을 잠근다. */
    private void clockOut() {
        StaffDTO s = CheckDialog.show(this, CheckDialog.CLOCK_OUT);
        if (s != null && s.getStaffId() == Session.getStaffId()) lock();
    }

    private void setMenusEnabled(boolean enabled) {
        for (JButton b : menuButtons) b.setEnabled(enabled);
        changeButton.setEnabled(enabled);
        lockButton.setEnabled(enabled);
    }
}
