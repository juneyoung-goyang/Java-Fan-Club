package dicepos.common;

/**
 * ※ 임시 파일입니다. ① 김재민이 진짜 Session을 올리면 이 파일은 지우고 그것을 씁니다.
 * 지금 POS를 쓰는 직원과, 관리자 확인을 통과한 관리자 번호를 기억한다.
 */
public class Session {
    private static int staffId = 3;          // 임시: 박민서 (직원)
    private static String staffName = "박민서";
    private static int adminId = 0;           // 0이면 관리자 화면이 잠겨 있다는 뜻

    public static int getStaffId() { return staffId; }
    public static String getName() { return staffName; }

    public static boolean isAdminUnlocked() { return adminId != 0; }
    public static int getAdminId() { return adminId; }
    public static void unlockAdmin(int id) { adminId = id; }
    public static void lockAdmin() { adminId = 0; }
}
