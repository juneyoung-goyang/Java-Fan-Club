package dicepos.common;

/**
 * 지금 POS를 쓰는 직원과, 관리자 확인을 통과한 관리자 번호를 기억한다. (① 김재민)
 * 프로그램 전체에서 하나만 있으면 되므로 static으로 만든다.
 */
public class Session {
    private static int staffId = 0;       // 0이면 직원 화면이 잠겨 있다는 뜻
    private static String staffName = "";
    private static int adminId = 0;       // 0이면 관리자 화면이 잠겨 있다는 뜻

    /** 근무자 확인을 통과한 직원으로 POS 잠금을 푼다. */
    public static void login(int id, String name) {
        staffId = id;
        staffName = name;
    }

    /** 화면 잠금: 직원 화면과 관리자 화면을 모두 잠근다. */
    public static void logout() {
        staffId = 0;
        staffName = "";
        adminId = 0;
    }

    public static boolean isLoggedIn() { return staffId != 0; }
    public static int getStaffId() { return staffId; }
    public static String getName() { return staffName; }

    public static boolean isAdminUnlocked() { return adminId != 0; }
    public static int getAdminId() { return adminId; }
    public static void unlockAdmin(int id) { adminId = id; }
    public static void lockAdmin() { adminId = 0; }
}
