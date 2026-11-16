package dicepos.game;

import dicepos.common.Refreshable;
import dicepos.common.UIUtil;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

/** [관리자] 게임 관리 (③ 김은별) */
public class GamePanel extends JPanel implements Refreshable {
    public static final String[] GENRES = {"파티", "가족", "전략", "추리", "카드"};

    private final DefaultTableModel model = UIUtil.tableModel("번호", "이름", "장르", "인원", "난이도", "시간", "보유", "대여중", "상태", "위치");
    private final JTable table = UIUtil.table(model);
    private final DefaultTableModel topModel = UIUtil.tableModel("순위", "게임", "추천 수", "선택 수");
    private final JTextField searchField = new JTextField(15);

    private final JTextField nameField = new JTextField(12);
    private final JComboBox<String> genreCombo = new JComboBox<>(GENRES);
    private final JSpinner minSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 20, 1));
    private final JSpinner maxSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 20, 1));
    private final JSlider difficultySlider = new JSlider(1, 5, 2);
    private final JLabel starLabel = new JLabel();
    private final JTextField minutesField = new JTextField(5);
    private final JTextField locationField = new JTextField(5);
    private final JRadioButton normalRadio = new JRadioButton("정상", true);
    private final JRadioButton missingRadio = new JRadioButton("부품누락");
    private final JRadioButton discardRadio = new JRadioButton("폐기");
    private final JLabel stockLabel = new JLabel("-");

    private final GameDAO dao = new GameDAO();
    private List<GameDTO> list = new ArrayList<>();
    private Map<Integer, Integer> busy;

    public GamePanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("게임 이름 검색"));
        top.add(searchField);
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                refresh();
            }
        });

        // 오른쪽 입력 폼
        ButtonGroup statusGroup = new ButtonGroup();   // 상태는 하나만 고른다
        statusGroup.add(normalRadio);
        statusGroup.add(missingRadio);
        statusGroup.add(discardRadio);
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        statusPanel.add(normalRadio);
        statusPanel.add(missingRadio);
        statusPanel.add(discardRadio);
        JPanel playerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        playerPanel.add(minSpinner);
        playerPanel.add(new JLabel(" ~ "));
        playerPanel.add(maxSpinner);
        playerPanel.add(new JLabel(" 명"));
        JPanel diffPanel = new JPanel(new BorderLayout());
        diffPanel.add(difficultySlider, BorderLayout.CENTER);
        diffPanel.add(starLabel, BorderLayout.EAST);

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("이름 *"));
        form.add(nameField);
        form.add(new JLabel("장르"));
        form.add(genreCombo);
        form.add(new JLabel("인원 *"));
        form.add(playerPanel);
        form.add(new JLabel("난이도"));
        form.add(diffPanel);
        form.add(new JLabel("플레이 시간(분) *"));
        form.add(minutesField);
        form.add(new JLabel("진열 위치 (예: A-1)"));
        form.add(locationField);
        form.add(new JLabel("상태"));
        form.add(statusPanel);
        form.add(new JLabel("보유 수량"));
        form.add(stockLabel);

        JButton addButton = new JButton("등록");
        JButton editButton = new JButton("수정");
        JButton deleteButton = new JButton("삭제");
        JButton discardButton = new JButton("폐기 처리");
        JButton clearButton = new JButton("새로 입력");
        JPanel buttons = new JPanel(new GridLayout(2, 3, 5, 5));
        buttons.add(addButton);
        buttons.add(editButton);
        buttons.add(deleteButton);
        buttons.add(discardButton);
        buttons.add(clearButton);

        JPanel right = new JPanel(new BorderLayout(0, 8));
        right.setBorder(BorderFactory.createTitledBorder("게임 정보"));
        right.add(form, BorderLayout.NORTH);
        right.add(buttons, BorderLayout.CENTER);
        right.add(new JLabel("<html>새 게임은 보유 0개로 등록되고,<br>발주 관리에서 입고하면 늘어납니다.</html>"), BorderLayout.SOUTH);

        JPanel topBox = new JPanel(new BorderLayout());
        topBox.setBorder(BorderFactory.createTitledBorder("많이 추천된 게임 TOP 5"));
        JScrollPane topScroll = new JScrollPane(UIUtil.table(topModel));
        topScroll.setPreferredSize(new java.awt.Dimension(400, 130));
        topBox.add(topScroll);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(right, BorderLayout.EAST);
        add(topBox, BorderLayout.SOUTH);

        // 슬라이더를 움직이면 ★을 바로 바꾼다
        difficultySlider.addChangeListener(e -> starLabel.setText(stars(difficultySlider.getValue())));
        starLabel.setText(stars(difficultySlider.getValue()));
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) fillForm();
        });
        addButton.addActionListener(e -> add());
        editButton.addActionListener(e -> edit());
        deleteButton.addActionListener(e -> delete());
        discardButton.addActionListener(e -> discard());
        clearButton.addActionListener(e -> clearForm());
        refresh();
    }

    /** 3 → "★★★☆☆" */
    public static String stars(int n) {
        return "★★★★★".substring(0, n) + "☆☆☆☆☆".substring(n);
    }

    @Override
    public void refresh() {
        try {
            list = dao.search(searchField.getText().trim());
            busy = dao.busyMap();
            topModel.setRowCount(0);
            int rank = 1;
            for (Object[] r : dao.top5Recommended()) {
                topModel.addRow(new Object[] {rank++, r[0], r[1] + "번", r[2] + "번"});
            }
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
            return;
        }
        model.setRowCount(0);
        for (GameDTO g : list) {
            model.addRow(new Object[] {g.getGameId(), g.getName(), g.getGenre(), g.getMinPlayer() + "~" + g.getMaxPlayer() + "명",
                    stars(g.getDifficulty()), g.getPlayMin() + "분", g.getStock(), busy.getOrDefault(g.getGameId(), 0),
                    g.getStatus(), g.getLocation() == null ? "" : g.getLocation()});
        }
    }

    private GameDTO selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : list.get(row);
    }

    private void fillForm() {
        GameDTO g = selected();
        if (g == null) return;
        nameField.setText(g.getName());
        genreCombo.setSelectedItem(g.getGenre());
        minSpinner.setValue(g.getMinPlayer());
        maxSpinner.setValue(g.getMaxPlayer());
        difficultySlider.setValue(g.getDifficulty());
        minutesField.setText(String.valueOf(g.getPlayMin()));
        locationField.setText(g.getLocation() == null ? "" : g.getLocation());
        normalRadio.setSelected("정상".equals(g.getStatus()));
        missingRadio.setSelected("부품누락".equals(g.getStatus()));
        discardRadio.setSelected("폐기".equals(g.getStatus()));
        stockLabel.setText(g.getStock() + "개 (대여중 " + busy.getOrDefault(g.getGameId(), 0) + "개)");
    }

    private void clearForm() {
        table.clearSelection();
        nameField.setText("");
        genreCombo.setSelectedIndex(0);
        minSpinner.setValue(2);
        maxSpinner.setValue(4);
        difficultySlider.setValue(2);
        minutesField.setText("");
        locationField.setText("");
        normalRadio.setSelected(true);
        stockLabel.setText("-");
    }

    /** 폼 값 검사 후 DTO로. 틀리면 null */
    private GameDTO readForm(int gameId, int stock) {
        String name = nameField.getText().trim();
        Integer minutes = UIUtil.toInt(minutesField.getText());
        int min = (Integer) minSpinner.getValue();
        int max = (Integer) maxSpinner.getValue();
        if (name.isEmpty()) {
            UIUtil.warn(this, "게임 이름을 입력하세요.");
            return null;
        }
        if (minutes == null || minutes <= 0) {
            UIUtil.warn(this, "플레이 시간을 숫자(분)로 입력하세요.");
            return null;
        }
        if (min > max) {
            UIUtil.warn(this, "최소 인원이 최대 인원보다 큽니다.");
            return null;
        }
        String status = normalRadio.isSelected() ? "정상" : missingRadio.isSelected() ? "부품누락" : "폐기";
        String location = locationField.getText().trim().isEmpty() ? null : locationField.getText().trim();
        return new GameDTO(gameId, name, (String) genreCombo.getSelectedItem(), min, max,
                difficultySlider.getValue(), minutes, stock, status, location);
    }

    private void add() {
        GameDTO g = readForm(0, 0);
        if (g == null) return;
        try {
            dao.insert(g);
            refresh();
            clearForm();
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }

    private void edit() {
        GameDTO old = selected();
        if (old == null) {
            UIUtil.warn(this, "표에서 게임을 먼저 고르세요.");
            return;
        }
        GameDTO g = readForm(old.getGameId(), old.getStock());
        if (g == null) return;
        if ("폐기".equals(g.getStatus()) && busy.getOrDefault(old.getGameId(), 0) > 0) {
            UIUtil.warn(this, "대여 중인 게임은 폐기 상태로 바꿀 수 없습니다.");
            return;
        }
        try {
            dao.update(g);
            refresh();
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }

    private void delete() {
        GameDTO g = selected();
        if (g == null) {
            UIUtil.warn(this, "표에서 게임을 먼저 고르세요.");
            return;
        }
        try {
            if (dao.hasRecords(g.getGameId())) {
                UIUtil.warn(this, "대여·추천·발주 기록이 있는 게임은 삭제할 수 없습니다. 폐기 처리를 쓰세요.");
                return;
            }
            if (!UIUtil.confirm(this, g.getName() + "을(를) 삭제할까요?")) return;
            dao.delete(g.getGameId());
            refresh();
            clearForm();
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }

    /** 폐기 수량을 받아 보유 수량을 줄인다. 대여 중이 아닌 수량까지만 폐기할 수 있다. */
    private void discard() {
        GameDTO g = selected();
        if (g == null) {
            UIUtil.warn(this, "표에서 게임을 먼저 고르세요.");
            return;
        }
        int canDiscard = g.getStock() - busy.getOrDefault(g.getGameId(), 0);
        String input = JOptionPane.showInputDialog(this, g.getName() + " 폐기할 수량 (대여 중이 아닌 수량 " + canDiscard + "개)", "1");
        if (input == null) return;
        Integer qty = UIUtil.toInt(input);
        if (qty == null || qty <= 0) {
            UIUtil.warn(this, "폐기 수량을 1 이상의 숫자로 입력하세요.");
            return;
        }
        if (qty > canDiscard) {
            UIUtil.warn(this, "대여 중이 아닌 수량(" + canDiscard + "개)보다 많이 폐기할 수 없습니다.");
            return;
        }
        try {
            dao.discard(g.getGameId(), qty);
            refresh();
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
    }
}
