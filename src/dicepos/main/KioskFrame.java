package dicepos.main;

import dicepos.common.AuthDialog;
import dicepos.common.UIUtil;
import dicepos.game.KioskRecommendPanel;
import dicepos.order.KioskOrderPanel;
import dicepos.order.KioskStatusPanel;
import dicepos.table.CafeTableDTO;
import dicepos.table.TableUsageDAO;
import dicepos.table.TableUsageDTO;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * 손님용 키오스크 창 (③ 김은별). POS 메뉴가 아예 없는 별도 창이다.
 * 가운데 판을 CardLayout으로 바꿔 끼운다: 메인 → 게임 추천(③) / 음식 주문(⑥) / 이용 현황(⑥)
 */
public class KioskFrame extends JFrame {
    private final CardLayout cards = new CardLayout();
    private final JPanel center = new JPanel(cards);
    private final JLabel roomLabel = new JLabel();
    private final KioskRecommendPanel recommendPanel = new KioskRecommendPanel(this);
    private final KioskOrderPanel orderPanel = new KioskOrderPanel(this);
    private final KioskStatusPanel statusPanel = new KioskStatusPanel(this);
    private KioskScreen current;
    private CafeTableDTO table;

    public KioskFrame(CafeTableDTO table) {
        super("다이스 키오스크");
        this.table = table;
        setSize(1000, 700);
        setLocationRelativeTo(null);

        roomLabel.setFont(roomLabel.getFont().deriveFont(Font.BOLD, 20f));
        JButton changeRoom = new JButton("룸 변경 (관리자)");
        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        top.add(roomLabel, BorderLayout.WEST);
        top.add(changeRoom, BorderLayout.EAST);

        center.add(makeHome(), "메인");
        center.add(recommendPanel, "추천");
        center.add(orderPanel, "주문");
        center.add(statusPanel, "현황");

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        showRoom();

        changeRoom.addActionListener(e -> changeRoom());
        // 키오스크를 끌 때도 관리자 비밀번호가 필요하다
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (AuthDialog.confirmAdmin(KioskFrame.this)) System.exit(0);
            }
        });
    }

    private void showRoom() {
        roomLabel.setText(table.getTableName() + " 키오스크");
    }

    /** 메인: 서비스 카드 3개 */
    private JPanel makeHome() {
        JPanel home = new JPanel(new BorderLayout());
        JLabel hello = new JLabel("어서 오세요! 원하는 서비스를 눌러 주세요.", SwingConstants.CENTER);
        hello.setFont(hello.getFont().deriveFont(22f));
        hello.setBorder(BorderFactory.createEmptyBorder(30, 0, 20, 0));

        JPanel cardRow = new JPanel(new GridLayout(1, 3, 20, 0));
        cardRow.setBorder(BorderFactory.createEmptyBorder(20, 40, 80, 40));
        cardRow.add(serviceCard("게임 추천", "인원·난이도·시간으로 골라 드려요", "추천"));
        cardRow.add(serviceCard("음식·음료 주문", "자리에서 바로 주문하세요", "주문"));
        cardRow.add(serviceCard("이용 현황", "이용 시간과 예상 요금", "현황"));

        home.add(hello, BorderLayout.NORTH);
        home.add(cardRow, BorderLayout.CENTER);
        return home;
    }

    /** 누를 수 있는 서비스 카드 (MouseListener) */
    private JPanel serviceCard(String title, String desc, String screen) {
        JLabel t = new JLabel(title, SwingConstants.CENTER);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 24f));
        JLabel d = new JLabel(desc, SwingConstants.CENTER);
        JPanel card = new JPanel(new GridLayout(2, 1));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.add(t);
        card.add(d);
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                open(screen);
            }
        });
        return card;
    }

    /** 이 룸이 이용 중일 때만 서비스 화면을 연다 */
    private void open(String screen) {
        TableUsageDTO usage;
        try {
            usage = new TableUsageDAO().findActiveByTable(table.getTableNo());
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
            return;
        }
        if (usage == null) {
            UIUtil.info(this, "직원에게 입장 등록을 요청해 주세요.");
            return;
        }
        current = screen.equals("추천") ? recommendPanel : screen.equals("주문") ? orderPanel : statusPanel;
        current.start(usage);
        cards.show(center, screen);
    }

    /** 각 화면의 [처음으로] 버튼이 부른다 */
    public void showHome() {
        if (current != null) current.stop();
        current = null;
        cards.show(center, "메인");
    }

    private void changeRoom() {
        if (!AuthDialog.confirmAdmin(this)) return;
        CafeTableDTO chosen = KioskMain.chooseRoom(this);
        if (chosen != null) {
            table = chosen;
            showRoom();
            showHome();
        }
    }
}
