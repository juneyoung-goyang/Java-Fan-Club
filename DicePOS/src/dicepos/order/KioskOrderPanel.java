package dicepos.order;

import dicepos.common.UIUtil;
import dicepos.main.KioskFrame;
import dicepos.main.KioskScreen;
import dicepos.menu.MenuDAO;
import dicepos.menu.MenuDTO;
import dicepos.table.TableUsageDTO;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;

/** [키오스크] 음식·음료 주문 (⑥ 김하진) */
public class KioskOrderPanel extends JPanel implements KioskScreen {
    private final KioskFrame kiosk;
    private final JPanel cardGrid = new JPanel(new GridLayout(0, 3, 10, 10));
    private final List<CartItem> cart = new ArrayList<>();   // 장바구니 (DB에는 [주문하기] 때 저장)
    private final DefaultListModel<CartItem> cartModel = new DefaultListModel<>();
    private final JList<CartItem> cartList = new JList<>(cartModel);
    private final JLabel totalLabel = new JLabel("0원");
    private final JButton orderButton = new JButton("주문하기");
    private TableUsageDTO usage;
    private String category = "음료";

    public KioskOrderPanel(KioskFrame kiosk) {
        super(new BorderLayout(10, 10));
        this.kiosk = kiosk;
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 위: 분류 탭
        JPanel tabs = new JPanel(new GridLayout(1, 3, 6, 0));
        ButtonGroup group = new ButtonGroup();
        for (String c : new String[] {"음료", "간식", "식사"}) {
            JToggleButton b = new JToggleButton(c, c.equals(category));
            b.setFont(b.getFont().deriveFont(16f));
            b.addActionListener(e -> {
                category = c;
                loadCards();
            });
            group.add(b);
            tabs.add(b);
        }
        JButton homeButton = new JButton("처음으로");
        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.add(tabs, BorderLayout.CENTER);
        top.add(homeButton, BorderLayout.EAST);

        JPanel cardHolder = new JPanel(new BorderLayout());
        cardHolder.add(cardGrid, BorderLayout.NORTH);

        // 오른쪽: 장바구니
        cartList.setFont(cartList.getFont().deriveFont(15f));
        JButton minus = new JButton("−");
        JButton plus = new JButton("+");
        JButton remove = new JButton("✕");
        JPanel cartButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        cartButtons.add(minus);
        cartButtons.add(plus);
        cartButtons.add(remove);
        totalLabel.setFont(totalLabel.getFont().deriveFont(Font.BOLD, 18f));
        orderButton.setFont(orderButton.getFont().deriveFont(18f));
        JPanel cartSouth = new JPanel(new GridLayout(3, 1, 0, 4));
        cartSouth.add(cartButtons);
        cartSouth.add(totalLabel);
        cartSouth.add(orderButton);
        JPanel cartBox = new JPanel(new BorderLayout());
        cartBox.setBorder(BorderFactory.createTitledBorder("장바구니"));
        cartBox.add(new JScrollPane(cartList), BorderLayout.CENTER);
        cartBox.add(cartSouth, BorderLayout.SOUTH);
        cartBox.setPreferredSize(new Dimension(300, 0));

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(cardHolder), BorderLayout.CENTER);
        add(cartBox, BorderLayout.EAST);

        homeButton.addActionListener(e -> kiosk.showHome());
        minus.addActionListener(e -> changeSelected(-1));
        plus.addActionListener(e -> changeSelected(1));
        remove.addActionListener(e -> {
            int i = cartList.getSelectedIndex();
            if (i >= 0) {
                cart.remove(i);
                showCart();
            }
        });
        orderButton.addActionListener(e -> order());
    }

    @Override
    public void start(TableUsageDTO usage) {
        this.usage = usage;
        cart.clear();
        showCart();
        loadCards();
    }

    @Override
    public void stop() {
        cart.clear();
        showCart();
    }

    private void loadCards() {
        cardGrid.removeAll();
        try {
            for (MenuDTO m : new MenuDAO().findByCategory(category)) {
                MenuCard card = new MenuCard(m);
                if (!m.isSoldOut()) {
                    card.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            addToCart(card.getMenu());
                        }
                    });
                }
                cardGrid.add(card);
            }
        } catch (SQLException e) {
            UIUtil.dbError(this, e);
        }
        cardGrid.revalidate();
        cardGrid.repaint();
    }

    /** 같은 메뉴는 수량만 늘린다 */
    private void addToCart(MenuDTO m) {
        for (CartItem item : cart) {
            if (item.getMenuId() == m.getMenuId()) {
                if (item.getQty() >= m.getStock()) {
                    UIUtil.warn(this, m.getName() + "은(는) " + m.getStock() + "개까지 주문할 수 있습니다.");
                    return;
                }
                item.setQty(item.getQty() + 1);
                showCart();
                return;
            }
        }
        cart.add(new CartItem(m.getMenuId(), m.getName(), m.getPrice(), 1));
        showCart();
    }

    private void changeSelected(int diff) {
        int i = cartList.getSelectedIndex();
        if (i < 0) return;
        CartItem item = cart.get(i);
        int newQty = item.getQty() + diff;
        if (newQty <= 0) cart.remove(i);
        else item.setQty(newQty);
        showCart();
        if (i < cartModel.size()) cartList.setSelectedIndex(i);
    }

    private void showCart() {
        cartModel.clear();
        int total = 0;
        for (CartItem item : cart) {
            cartModel.addElement(item);
            total += item.getSubtotal();
        }
        totalLabel.setText("합계 " + UIUtil.won(total));
        orderButton.setEnabled(!cart.isEmpty());   // 비어 있으면 주문할 수 없다
    }

    /** 확인 팝업 → 장바구니 전체를 한 주문으로 저장 (경로 '키오스크', 상태 '접수') */
    private void order() {
        StringBuilder sb = new StringBuilder("아래와 같이 주문할까요?\n\n");
        int total = 0;
        for (CartItem item : cart) {
            sb.append(item).append('\n');
            total += item.getSubtotal();
        }
        sb.append("\n합계 ").append(UIUtil.won(total)).append("  (나가실 때 함께 결제합니다)");
        if (JOptionPane.showConfirmDialog(this, sb.toString(), "주문 확인", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            new OrderDAO().saveOrder(usage.getUsageId(), null, "키오스크", "접수", new ArrayList<>(cart));
            UIUtil.info(this, "주문이 접수되었습니다. 잠시만 기다려 주세요!");
            cart.clear();
            showCart();
            kiosk.showHome();
        } catch (SQLException e) {
            UIUtil.warn(this, "주문하지 못했습니다. 직원을 불러 주세요.\n" + e.getMessage());
            loadCards();
        }
    }
}
