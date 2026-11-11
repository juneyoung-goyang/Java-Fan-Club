package dicepos.main;

import dicepos.common.AuthDialog;
import dicepos.common.Session;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

/** POS 메인 창. 왼쪽은 메뉴 버튼, 가운데는 팀원들의 화면이 카드처럼 바뀐다. */
public class PosMainFrame extends JFrame {
    private final CardLayout cards = new CardLayout();
    private final JPanel center = new JPanel(cards);
    private final JPanel staffMenu = new JPanel(new GridLayout(0, 1, 0, 5));
    private final JPanel adminMenu = new JPanel(new GridLayout(0, 1, 0, 5));
    private final TitledBorder adminTitle = BorderFactory.createTitledBorder("관리자 화면 (잠김)");

    public PosMainFrame() {
        super("다이스 POS");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1300, 812);
        setLocationRelativeTo(null);   // 화면 가운데

        center.add(new JLabel("왼쪽에서 화면을 고르세요", SwingConstants.CENTER), "처음");

        add(makeSide(), BorderLayout.WEST);
        add(center, BorderLayout.CENTER);
    }

    private JPanel makeSide() {
        staffMenu.setBorder(BorderFactory.createTitledBorder("직원 화면"));
        adminMenu.setBorder(adminTitle);

        JPanel menus = new JPanel(new BorderLayout(0, 10));
        menus.add(staffMenu, BorderLayout.NORTH);
        menus.add(adminMenu, BorderLayout.CENTER);

        JPanel side = new JPanel(new BorderLayout());
        side.setPreferredSize(new Dimension(200, 0));
        side.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        side.add(menus, BorderLayout.NORTH);
        side.add(new JLabel("근무자: " + Session.getName()), BorderLayout.SOUTH);
        return side;
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
        adminTitle.setTitle(Session.isAdminUnlocked() ? "관리자 화면 (열림)" : "관리자 화면 (잠김)");
        adminMenu.repaint();
        cards.show(center, name);
    }
}
