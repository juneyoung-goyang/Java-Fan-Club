package dicepos.payment;

import dicepos.common.FeeCalculator;
import dicepos.common.Session;
import dicepos.common.UIUtil;
import dicepos.member.MemberDAO;
import dicepos.member.MemberDTO;
import dicepos.order.OrderDAO;
import dicepos.order.OrderDetailDTO;
import dicepos.table.TableUsageDAO;
import dicepos.table.TableUsageDTO;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/** 결제 팝업 (② 송하안). ⑤ 이용 종료 팝업이 연다. */
public class PaymentDialog extends JDialog {
    private final int usageId;
    private final List<PaymentDetailDTO> items = new ArrayList<>();
    private int total;
    private MemberDTO member;   // 비회원이면 null
    private final Map<Integer, String> menuNames = new LinkedHashMap<>();   // 메뉴 번호 → 이름

    private final SpinnerNumberModel pointModel = new SpinnerNumberModel(0, 0, 0, 100);
    private final SpinnerNumberModel cardModel = new SpinnerNumberModel(0, 0, 0, 1000);
    private final JLabel cashLabel = new JLabel();
    private final JLabel earnLabel = new JLabel();
    private final JLabel checkLabel = new JLabel(" ");
    private final JButton payButton = new JButton("결제하기");
    private boolean paid = false;

    /** 결제 팝업을 열고, 결제가 끝나면 true */
    public static boolean open(Component parent, int usageId) {
        PaymentDialog d;
        try {
            if (new PaymentDAO().isPaid(usageId)) {
                UIUtil.warn(parent, "이미 결제된 이용 건입니다.");
                return false;
            }
            d = new PaymentDialog(parent, usageId);
        } catch (SQLException e) {
            UIUtil.dbError(parent, e);
            return false;
        }
        d.setVisible(true);
        return d.paid;
    }

    private PaymentDialog(Component parent, int usageId) throws SQLException {
        super(SwingUtilities.getWindowAncestor(parent), "결제", ModalityType.APPLICATION_MODAL);
        this.usageId = usageId;

        TableUsageDTO usage = new TableUsageDAO().findById(usageId);
        if (usage.getMemberId() != null) member = new MemberDAO().findById(usage.getMemberId());
        int minutes = TableUsageDAO.minutesUsed(usage);
        makeItems(usage, minutes);

        // 위: 이용 정보
        JPanel info = new JPanel(new GridLayout(0, 2, 6, 4));
        info.add(new JLabel("이용 시간"));
        info.add(new JLabel(UIUtil.minutes(minutes) + " (" + usage.getPeople() + "명)"));
        info.add(new JLabel("회원"));
        info.add(new JLabel(member == null ? "비회원 (포인트 사용·적립 없음)"
                : member.getName() + " · " + member.getGrade() + " · 보유 " + String.format("%,dP", member.getPoint())));

        // 가운데: 결제 항목 표
        DefaultTableModel model = UIUtil.tableModel("구분", "항목", "수량", "단가", "소계");
        for (PaymentDetailDTO d : items) {
            String name = d.getMenuId() == null ? "이용료" : menuNames.get(d.getMenuId());
            model.addRow(new Object[] {d.getItemType(), name, d.getQty(), UIUtil.won(d.getUnitPrice()), UIUtil.won(d.getSubtotal())});
        }
        JScrollPane itemScroll = new JScrollPane(UIUtil.table(model));
        itemScroll.setPreferredSize(new Dimension(460, 160));
        JLabel totalLabel = new JLabel("총 결제 금액  " + UIUtil.won(total));
        totalLabel.setFont(totalLabel.getFont().deriveFont(17f));
        JPanel center = new JPanel(new BorderLayout(0, 4));
        center.add(itemScroll, BorderLayout.CENTER);
        center.add(totalLabel, BorderLayout.SOUTH);

        // 아래: 결제 수단
        int maxPoint = member == null ? 0 : Math.min(member.getPoint(), total);
        pointModel.setMaximum(maxPoint);
        cardModel.setMaximum(total);
        cardModel.setValue(total);   // 처음에는 전부 카드
        JSpinner pointSpinner = new JSpinner(pointModel);
        pointSpinner.setEnabled(member != null);
        JSpinner cardSpinner = new JSpinner(cardModel);
        checkLabel.setForeground(Color.RED);

        JPanel methods = new JPanel(new GridLayout(0, 2, 6, 6));
        methods.setBorder(BorderFactory.createTitledBorder("결제 수단 (카드 + 현금 + 포인트 = 총액)"));
        methods.add(new JLabel("포인트 사용 (최대 " + String.format("%,d", maxPoint) + ")"));
        methods.add(pointSpinner);
        methods.add(new JLabel("카드"));
        methods.add(cardSpinner);
        methods.add(new JLabel("현금 (자동 계산)"));
        methods.add(cashLabel);
        methods.add(new JLabel("이번에 적립할 포인트"));
        methods.add(earnLabel);

        JButton cancel = new JButton("취소");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(checkLabel);
        buttons.add(payButton);
        buttons.add(cancel);

        JPanel south = new JPanel(new BorderLayout());
        south.add(methods, BorderLayout.CENTER);
        south.add(buttons, BorderLayout.SOUTH);

        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(info, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);
        setContentPane(root);

        // 포인트나 카드 금액을 바꾸면 현금 금액을 다시 계산한다
        pointSpinner.addChangeListener(e -> recalc());
        cardSpinner.addChangeListener(e -> recalc());
        payButton.addActionListener(e -> pay());
        cancel.addActionListener(e -> dispose());
        recalc();
        pack();
        setLocationRelativeTo(parent);
    }

    /** 결제 항목 만들기: 이용료 1줄 + 같은 메뉴(같은 단가)끼리 합친 메뉴 줄 */
    private void makeItems(TableUsageDTO usage, int minutes) throws SQLException {
        int perPerson = FeeCalculator.perPerson(minutes);
        items.add(new PaymentDetailDTO(0, 0, "이용료", null, usage.getPeople(), perPerson, perPerson * usage.getPeople()));

        Map<String, int[]> merged = new LinkedHashMap<>();   // "메뉴번호/단가" → {메뉴번호, 수량, 단가}
        for (OrderDetailDTO d : new OrderDAO().findDetailsByUsage(usageId)) {
            String key = d.getMenuId() + "/" + d.getUnitPrice();
            int[] v = merged.get(key);
            if (v == null) merged.put(key, new int[] {d.getMenuId(), d.getQty(), d.getUnitPrice()});
            else v[1] += d.getQty();
            menuNames.put(d.getMenuId(), d.getMenuName());
        }
        for (int[] v : merged.values()) {
            items.add(new PaymentDetailDTO(0, 0, "메뉴", v[0], v[1], v[2], v[1] * v[2]));
        }
        total = 0;
        for (PaymentDetailDTO d : items) total += d.getSubtotal();
    }

    private int point() { return (Integer) pointModel.getValue(); }
    private int card() { return (Integer) cardModel.getValue(); }
    private int cash() { return total - point() - card(); }

    private int earnPoint() {
        if (member == null) return 0;
        return (card() + cash()) * PaymentDAO.earnRate(member.getGrade()) / 100;
    }

    private void recalc() {
        int cash = cash();
        cashLabel.setText(UIUtil.won(Math.max(cash, 0)));
        if (cash < 0) {
            checkLabel.setText("포인트 + 카드가 총액보다 많습니다.");
            payButton.setEnabled(false);
        } else {
            checkLabel.setText(" ");
            payButton.setEnabled(true);
        }
        earnLabel.setText(member == null ? "0P (비회원)"
                : String.format("%,dP (%s %d%%)", earnPoint(), member.getGrade(), PaymentDAO.earnRate(member.getGrade())));
    }

    private void pay() {
        if (point() + card() + cash() != total || cash() < 0) {
            UIUtil.warn(this, "결제 금액 합계가 총액과 다릅니다.");
            return;
        }
        Integer staffId = Session.getStaffId() == 0 ? null : Session.getStaffId();
        Integer memberId = member == null ? null : member.getMemberId();
        PaymentDTO p = new PaymentDTO(0, usageId, staffId, memberId, null, total, card(), cash(), point(), earnPoint());
        try {
            int payId = new PaymentDAO().pay(p, items, member == null ? 0 : member.getPoint());
            UIUtil.info(this, "결제를 마쳤습니다. (결제 번호 " + payId + ")\n총 " + UIUtil.won(total)
                    + "\n카드 " + UIUtil.won(card()) + " / 현금 " + UIUtil.won(cash()) + " / 포인트 " + String.format("%,dP", point())
                    + (member == null ? "" : "\n적립 " + String.format("%,dP", earnPoint())));
            paid = true;
            dispose();
        } catch (SQLException e) {
            UIUtil.warn(this, "결제하지 못했습니다. 아무것도 저장되지 않았습니다.\n" + e.getMessage());
        }
    }
}
