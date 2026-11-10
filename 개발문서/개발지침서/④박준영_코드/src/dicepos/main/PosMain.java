package dicepos.main;

import dicepos.menu.MenuPanel;
import dicepos.menu.PurchaseOrderPanel;
import javax.swing.SwingUtilities;

/** POS 프로그램을 시작하는 곳. 팀원 화면이 완성되면 여기에 한 줄씩 추가한다. */
public class PosMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PosMainFrame frame = new PosMainFrame();
            // 직원 화면 (⑤ ⑥ ② ③이 완성하면 추가)
            // frame.addScreen("테이블 현황", new TableStatusPanel(), false);
            // 관리자 화면
            frame.addScreen("메뉴 관리", new MenuPanel(), true);
            frame.addScreen("발주 관리", new PurchaseOrderPanel(), true);
            frame.setVisible(true);
        });
    }
}
