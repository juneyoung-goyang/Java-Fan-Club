package dicepos.member;

import java.time.LocalDate;

/** member (회원) 한 줄을 담는 그릇 */
public class MemberDTO {
    private int memberId;
    private String name;
    private String phone1;
    private String phone2;
    private String phone3;
    private String gender;
    private String grade;
    private int point;
    private LocalDate joinDate;

    public MemberDTO(int memberId, String name, String phone1, String phone2, String phone3, String gender, String grade, int point, LocalDate joinDate) {
        this.memberId = memberId;
        this.name = name;
        this.phone1 = phone1;
        this.phone2 = phone2;
        this.phone3 = phone3;
        this.gender = gender;
        this.grade = grade;
        this.point = point;
        this.joinDate = joinDate;
    }

    public int getMemberId() {
        return memberId;
    }

    public String getName() {
        return name;
    }

    public String getPhone1() {
        return phone1;
    }

    public String getPhone2() {
        return phone2;
    }

    public String getPhone3() {
        return phone3;
    }

    public String getGender() {
        return gender;
    }

    public String getGrade() {
        return grade;
    }

    public int getPoint() {
        return point;
    }

    public LocalDate getJoinDate() {
        return joinDate;
    }
}
