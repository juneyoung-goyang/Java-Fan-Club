package dicepos.main;

import dicepos.common.AuthDialog;
import dicepos.common.UIUtil;
import dicepos.table.CafeTableDAO;
import dicepos.table.CafeTableDTO;
import java.awt.Component;
import java.sql.SQLException;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * 키오스크 프로그램을 시작하는 곳 (③ 김은별). 테이블마다 한 대씩 실행한다.
 * 설치할 때 관리자가 비밀번호를 넣고 이 키오스크의 룸 번호를 고른다.
 */
public class KioskMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UIUtil.info(null, "키오스크 설치: 관리자 확인 후 이 키오스크의 룸을 고릅니다.");
            if (!AuthDialog.confirmAdmin(null)) System.exit(0);
            CafeTableDTO room = chooseRoom(null);
            if (room == null) System.exit(0);
            new KioskFrame(room).setVisible(true);
        });
    }

    /** 사용가능한 룸 중 하나를 고른다. 취소하면 null */
    static CafeTableDTO chooseRoom(Component parent) {
        List<CafeTableDTO> rooms;
        try {
            rooms = new CafeTableDAO().findActive();
        } catch (SQLException e) {
            UIUtil.dbError(parent, e);
            return null;
        }
        if (rooms.isEmpty()) {
            UIUtil.warn(parent, "사용가능한 룸이 없습니다. POS 관리자 화면의 테이블 설정을 확인하세요.");
            return null;
        }
        String[] names = new String[rooms.size()];
        for (int i = 0; i < rooms.size(); i++) names[i] = rooms.get(i).getTableName();
        Object choice = JOptionPane.showInputDialog(parent, "이 키오스크를 놓을 룸", "룸 지정",
                JOptionPane.QUESTION_MESSAGE, null, names, names[0]);
        if (choice == null) return null;
        for (CafeTableDTO r : rooms) {
            if (r.getTableName().equals(choice)) return r;
        }
        return null;
    }
}
