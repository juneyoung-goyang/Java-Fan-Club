package dicepos.table;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** 테이블 현황의 카드 하나 (직접 만든 JPanel). (⑤ 서윤채) */
public class TableCard extends JPanel {
    private static final Color EMPTY = new Color(235, 245, 235);   // 빈자리
    private static final Color USING = new Color(255, 240, 210);   // 이용 중
    private static final Color OFF = new Color(210, 210, 210);     // 사용불가

    private final CafeTableDTO table;
    private final TableUsageDTO usage;   // 비어 있으면 null

    /** memberName은 비회원이면 null, minutes·fee는 이용 중일 때만 쓴다 */
    public TableCard(CafeTableDTO table, TableUsageDTO usage, String memberName, int minutes, int fee) {
        super(new BorderLayout());
        this.table = table;
        this.usage = usage;

        JLabel name = new JLabel(table.getTableName() + "  (" + table.getCapacity() + "인실)");
        name.setFont(name.getFont().deriveFont(Font.BOLD, 15f));

        JPanel info = new JPanel(new GridLayout(3, 1));
        info.setOpaque(false);
        if (!table.isActive()) {
            setBackground(OFF);
            info.add(new JLabel("사용불가"));
        } else if (usage == null) {
            setBackground(EMPTY);
            info.add(new JLabel("빈자리"));
        } else {
            setBackground(USING);
            info.add(new JLabel(usage.getPeople() + "명 · " + (memberName == null ? "비회원" : memberName + " 회원")));
            info.add(new JLabel("이용 " + dicepos.common.UIUtil.minutes(minutes)
                    + " (" + dicepos.common.UIUtil.time(usage.getStartTime()) + " 입장)"));
            info.add(new JLabel("현재 이용료 " + dicepos.common.UIUtil.won(fee)));
        }
        add(name, BorderLayout.NORTH);
        add(info, BorderLayout.CENTER);
        setSelected(false);
    }

    public CafeTableDTO getTable() { return table; }
    public TableUsageDTO getUsage() { return usage; }

    /** 고른 카드는 두꺼운 파란 테두리 */
    public void setSelected(boolean selected) {
        if (selected) {
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.BLUE, 3), BorderFactory.createEmptyBorder(6, 6, 6, 6)));
        } else {
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.GRAY, 1), BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        }
    }
}
