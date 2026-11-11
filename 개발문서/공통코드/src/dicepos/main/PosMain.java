package dicepos.main;

import javax.swing.SwingUtilities;

/** POS 프로그램을 시작하는 곳. 각자 화면이 완성되면 addScreen 줄을 한 줄씩 추가한다. */
public class PosMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PosMainFrame frame = new PosMainFrame();
            // 직원 화면 (false)
            // frame.addScreen("테이블 현황", new dicepos.table.TableStatusPanel(), false);     // ⑤
            // frame.addScreen("손님 주문받기", new dicepos.order.OrderPanel(), false);         // ⑥
            // frame.addScreen("결제 내역", new dicepos.payment.PaymentHistoryPanel(), false); // ②
            // frame.addScreen("회원 관리", new dicepos.member.MemberPanel(), false);           // ②
            // frame.addScreen("게임 대여·예약", new dicepos.game.GameRentalPanel(), false);    // ③
            // 관리자 화면 (true)
            // frame.addScreen("직원 관리", new dicepos.staff.StaffPanel(), true);              // ①
            // frame.addScreen("근무·급여 관리", new dicepos.staff.WorkSalaryPanel(), true);    // ①
            // frame.addScreen("매출 관리", new dicepos.staff.SalesPanel(), true);              // ①
            // frame.addScreen("게임 관리", new dicepos.game.GamePanel(), true);                // ③
            // frame.addScreen("메뉴 관리", new dicepos.menu.MenuPanel(), true);                // ④
            // frame.addScreen("발주 관리", new dicepos.menu.PurchaseOrderPanel(), true);       // ④
            // frame.addScreen("테이블 설정", new dicepos.table.TableSettingPanel(), true);     // ⑤
            // frame.addScreen("이용 요금 설정", new dicepos.table.FeeSettingPanel(), true);    // ⑤
            frame.setVisible(true);
        });
    }
}
