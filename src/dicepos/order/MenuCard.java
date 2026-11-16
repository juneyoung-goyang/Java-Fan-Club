package dicepos.order;

import dicepos.common.UIUtil;
import dicepos.menu.MenuDTO;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** 키오스크 메뉴 카드 (직접 만든 JPanel). 누르는 동작은 MouseListener로 밖에서 붙인다. (⑥ 김하진) */
public class MenuCard extends JPanel {
    private final MenuDTO menu;

    public MenuCard(MenuDTO menu) {
        super(new BorderLayout());
        this.menu = menu;
        setPreferredSize(new Dimension(150, 90));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY), BorderFactory.createEmptyBorder(8, 8, 8, 8)));

        JLabel name = new JLabel(menu.getName(), SwingConstants.CENTER);
        name.setFont(name.getFont().deriveFont(Font.BOLD, 16f));
        JLabel price = new JLabel(menu.isSoldOut() ? "품절" : UIUtil.won(menu.getPrice()), SwingConstants.CENTER);
        add(name, BorderLayout.CENTER);
        add(price, BorderLayout.SOUTH);

        if (menu.isSoldOut()) {
            setBackground(new Color(220, 220, 220));
            name.setForeground(Color.GRAY);
            price.setForeground(Color.RED);
        } else {
            setBackground(Color.WHITE);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }

    public MenuDTO getMenu() {
        return menu;
    }
}
