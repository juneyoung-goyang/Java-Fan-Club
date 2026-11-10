package dicepos.menu;

import dicepos.common.UIUtil;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/** [입고 처리]를 누르면 뜨는 확인 팝업. 품목을 보여 주고 [입고]를 누르면 true */
public class ReceiveDialog extends JDialog {
    private boolean ok = false;

    private ReceiveDialog(Window owner, PurchaseOrderDTO po, List<PurchaseDetailDTO> items) {
        super(owner, "입고 처리 확인", ModalityType.APPLICATION_MODAL);   // 닫기 전까지 뒤 화면을 못 누름
        DefaultTableModel model = new DefaultTableModel(new String[] {"구분", "품목", "수량"}, 0);
        for (PurchaseDetailDTO d : items) {
            model.addRow(new Object[] {d.getItemType(), d.getItemName(), d.getQty()});
        }
        JLabel title = new JLabel("발주 " + po.getPoId() + "번 (" + UIUtil.won(po.getTotalAmount())
                + ")이 도착했나요? 입고하면 아래 수량만큼 재고가 늘어납니다.");
        title.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton bOk = new JButton("입고");
        JButton bCancel = new JButton("닫기");
        bOk.addActionListener(e -> { ok = true; dispose(); });
        bCancel.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(bOk);
        buttons.add(bCancel);

        add(title, BorderLayout.NORTH);
        add(new JScrollPane(new JTable(model)), BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        setSize(520, 320);
        setLocationRelativeTo(owner);
    }

    public static boolean confirm(Component parent, PurchaseOrderDTO po, List<PurchaseDetailDTO> items) {
        ReceiveDialog dialog = new ReceiveDialog(SwingUtilities.getWindowAncestor(parent), po, items);
        dialog.setVisible(true);   // 팝업이 닫힐 때까지 여기서 기다린다
        return dialog.ok;
    }
}
