package dicepos.main;

import dicepos.common.AuthDialog;
import dicepos.common.Session;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** POS 메인 창. 왼쪽은 메뉴 버튼, 가운데는 팀원들의 화면이 카드처럼 바뀐다. */
public class PosMainFrame extends JFrame {
    private final CardLayout cards = new CardLayout();
    private final JPanel center = new JPanel(cards);
    private final JPanel staffMenu = new JPanel(new GridLayout(0, 1, 0, 6));
    private final JPanel adminMenu = new JPanel(new GridLayout(0, 1, 0, 6));
    private final JLabel lockLabel = new JLabel("관리자 화면 (잠김)");

    public PosMainFrame() {
        super("다이스 POS");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1300, 812);
        setLocationRelativeTo(null);   // 화면 가운데

        JLabel home = new JLabel("왼쪽에서 화면을 고르세요", SwingConstants.CENTER);
        home.setFont(new Font("맑은 고딕", Font.PLAIN, 22));
        center.add(home, "처음");

        add(makeSide(), BorderLayout.WEST);
        add(center, BorderLayout.CENTER);
    }

    private JPanel makeSide() {
        JPanel side = new JPanel(new BorderLayout());
        side.setPreferredSize(new Dimension(220, 0));
        side.setBackground(new Color(30, 42, 74));
        side.setBorder(BorderFactory.createEmptyBorder(16, 12, 16, 12));

        JLabel title = new JLabel("다이스 POS");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("맑은 고딕", Font.BOLD, 20));

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);
        staffMenu.setOpaque(false);
        adminMenu.setOpaque(false);
        list.add(sectionLabel(new JLabel("직원 화면")));
        list.add(staffMenu);
        list.add(sectionLabel(lockLabel));
        list.add(adminMenu);

        JLabel who = new JLabel("근무자 " + Session.getName());
        who.setForeground(Color.WHITE);

        side.add(title, BorderLayout.NORTH);
        side.add(list, BorderLayout.CENTER);
        side.add(who, BorderLayout.SOUTH);
        return side;
    }

    private JLabel sectionLabel(JLabel label) {
        label.setForeground(new Color(170, 185, 220));
        label.setBorder(BorderFactory.createEmptyBorder(14, 0, 6, 0));
        return label;
    }

    /** 팀원 화면을 메인 창에 붙인다. admin이 true면 관리자 화면 쪽에 버튼이 생긴다. */
    public void addScreen(String name, JPanel screen, boolean admin) {
        center.add(screen, name);
        JButton button = new JButton(name);
        button.addActionListener(e -> open(name, admin));
        (admin ? adminMenu : staffMenu).add(button);
        revalidate();
    }

    private void open(String name, boolean admin) {
        if (admin && !Session.isAdminUnlocked()) {
            if (!AuthDialog.confirmAdmin(this)) return;   // 관리자 확인 실패
        }
        if (!admin) Session.lockAdmin();                  // 직원 화면으로 오면 다시 잠금
        lockLabel.setText(Session.isAdminUnlocked() ? "관리자 화면 (열림)" : "관리자 화면 (잠김)");
        cards.show(center, name);
    }
}
