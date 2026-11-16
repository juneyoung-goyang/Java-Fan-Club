package dicepos.game;

import dicepos.common.UIUtil;
import dicepos.main.KioskFrame;
import dicepos.main.KioskScreen;
import dicepos.table.TableUsageDTO;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;

/** [키오스크] 게임 추천 · 대여 신청 · 예약 (③ 김은별) */
public class KioskRecommendPanel extends JPanel implements KioskScreen {
    private final KioskFrame kiosk;
    private final JSpinner peopleSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 12, 1));
    private final JSlider difficultySlider = new JSlider(1, 5, 3);
    private final JLabel starLabel = new JLabel();
    private final ButtonGroup timeGroup = new ButtonGroup();
    private final ButtonGroup genreGroup = new ButtonGroup();
    private final JPanel resultGrid = new JPanel(new GridLayout(0, 3, 10, 10));
    private final JLabel pastLabel = new JLabel(" ");
    private final GameDAO gameDAO = new GameDAO();
    private final GameRentalDAO rentalDAO = new GameRentalDAO();
    private final RecommendDAO recommendDAO = new RecommendDAO();
    private TableUsageDTO usage;
    private int maxMinutes = 0;      // 0 = 상관없음
    private String genre = "전체";

    public KioskRecommendPanel(KioskFrame kiosk) {
        super(new BorderLayout(10, 10));
        this.kiosk = kiosk;
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 조건 고르기
        JPanel timeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        addToggle(timeRow, timeGroup, "상관없음", true, () -> maxMinutes = 0);
        addToggle(timeRow, timeGroup, "30분 이하", false, () -> maxMinutes = 30);
        addToggle(timeRow, timeGroup, "60분 이하", false, () -> maxMinutes = 60);
        JPanel genreRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        addToggle(genreRow, genreGroup, "전체", true, () -> genre = "전체");
        for (String g : GamePanel.GENRES) addToggle(genreRow, genreGroup, g, false, () -> genre = g);
        JPanel diffRow = new JPanel(new BorderLayout());
        diffRow.add(difficultySlider, BorderLayout.CENTER);
        diffRow.add(starLabel, BorderLayout.EAST);
        difficultySlider.addChangeListener(e -> starLabel.setText(GamePanel.stars(difficultySlider.getValue()) + " 이하"));
        starLabel.setText(GamePanel.stars(difficultySlider.getValue()) + " 이하");

        JPanel conditions = new JPanel(new GridLayout(0, 2, 8, 8));
        conditions.setBorder(BorderFactory.createTitledBorder("어떤 게임을 찾으세요?"));
        conditions.add(new JLabel("인원"));
        conditions.add(peopleSpinner);
        conditions.add(new JLabel("난이도"));
        conditions.add(diffRow);
        conditions.add(new JLabel("플레이 시간"));
        conditions.add(timeRow);
        conditions.add(new JLabel("장르"));
        conditions.add(genreRow);

        JButton searchButton = new JButton("추천받기");
        searchButton.setFont(searchButton.getFont().deriveFont(18f));
        JButton homeButton = new JButton("처음으로");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(searchButton);
        buttons.add(homeButton);

        JPanel north = new JPanel(new BorderLayout());
        north.add(conditions, BorderLayout.CENTER);
        north.add(buttons, BorderLayout.SOUTH);

        JPanel resultHolder = new JPanel(new BorderLayout());
        resultHolder.add(resultGrid, BorderLayout.NORTH);

        add(north, BorderLayout.NORTH);
        add(new JScrollPane(resultHolder), BorderLayout.CENTER);
        add(pastLabel, BorderLayout.SOUTH);

        searchButton.addActionListener(e -> recommend());
        homeButton.addActionListener(e -> kiosk.showHome());
    }

    private void addToggle(JPanel row, ButtonGroup group, String text, boolean selected, Runnable onSelect) {
        JToggleButton b = new JToggleButton(text, selected);
        b.addActionListener(e -> onSelect.run());
        group.add(b);
        row.add(b);
    }

    @Override
    public void start(TableUsageDTO usage) {
        this.usage = usage;
        peopleSpinner.setValue(usage.getPeople());
        resultGrid.removeAll();
        resultGrid.add(new JLabel("조건을 고르고 [추천받기]를 누르세요."));
        resultGrid.revalidate();
        resultGrid.repaint();
        // 회원이면 지난 방문 때 추천받은 게임도 보여 준다
        pastLabel.setText(" ");
        if (usage.getMemberId() != null) {
            try {
                List<String> past = recommendDAO.pastGames(usage.getMemberId(), usage.getUsageId());
                if (!past.isEmpty()) pastLabel.setText("지난 방문 때 추천받은 게임: " + String.join(", ", past));
            } catch (SQLException e) {
                // 지난 기록은 못 불러와도 추천은 할 수 있다
            }
        }
    }

    @Override
    public void stop() {
    }

    private void recommend() {
        resultGrid.removeAll();
        try {
            List<GameDTO> games = gameDAO.recommend((Integer) peopleSpinner.getValue(), difficultySlider.getValue(), maxMinutes, genre);
            if (games.isEmpty()) {
                JLabel none = new JLabel("조건에 맞는 게임이 없습니다. 난이도나 시간 조건을 바꿔 보세요.");
                none.setFont(none.getFont().deriveFont(16f));
                resultGrid.add(none);
            }
            for (GameDTO g : games) {
                recommendDAO.insert(usage.getUsageId(), usage.getMemberId(), g.getGameId());   // 추천 기록 남기기
                resultGrid.add(makeCard(g, rentalDAO.available(g.getGameId())));
            }
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
        resultGrid.revalidate();
        resultGrid.repaint();
    }

    /** 추천 결과 카드 */
    private JPanel makeCard(GameDTO g, int available) {
        JLabel name = new JLabel(g.getName());
        name.setFont(name.getFont().deriveFont(Font.BOLD, 17f));
        JPanel info = new JPanel(new GridLayout(0, 1));
        info.setOpaque(false);
        info.add(name);
        info.add(new JLabel(g.getGenre() + " · " + g.getMinPlayer() + "~" + g.getMaxPlayer() + "명 · " + g.getPlayMin() + "분"));
        info.add(new JLabel("난이도 " + GamePanel.stars(g.getDifficulty())));
        info.add(new JLabel(available > 0 ? "지금 빌릴 수 있어요 (" + available + "개)" : "모두 대여 중 (예약 가능)"));

        JButton choose = new JButton(available > 0 ? "이 게임 할래요" : "예약하기");
        choose.addActionListener(e -> choose(g));

        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(available > 0 ? new Color(235, 245, 235) : new Color(255, 245, 225));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY), BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        card.add(info, BorderLayout.CENTER);
        card.add(choose, BorderLayout.SOUTH);
        return card;
    }

    /** 대여 가능하면 '대여중', 모두 나가 있으면 '예약'으로 신청한다 */
    private void choose(GameDTO g) {
        try {
            if (rentalDAO.isDuplicate(usage.getUsageId(), g.getGameId())) {
                UIUtil.warn(this, "이미 대여했거나 예약한 게임입니다.");
                return;
            }
            int rentalId = rentalDAO.request(g.getGameId(), usage.getUsageId());
            recommendDAO.markChosen(usage.getUsageId(), g.getGameId());
            if ("대여중".equals(rentalDAO.findStatus(rentalId))) {
                UIUtil.info(this, g.getName() + " 대여 신청 완료!\n진열장 " + g.getLocation() + " 칸에서 가져가세요.");
            } else {
                UIUtil.info(this, g.getName() + " 예약 완료!\n대기 " + rentalDAO.queuePosition(rentalId)
                        + "번째입니다. 반납되면 직원이 불러 드립니다.");
            }
            kiosk.showHome();
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }
}
