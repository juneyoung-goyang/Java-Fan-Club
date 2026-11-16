package dicepos.staff;

import java.time.LocalDate;

/** staff (직원) 한 줄을 담는 그릇 */
public class StaffDTO {
    private int staffId;
    private String loginId;
    private String password;
    private String salt;
    private String name;
    private String phone1;
    private String phone2;
    private String phone3;
    private String role;
    private String empType;
    private Integer hourlyWage;
    private Integer monthlyPay;
    private LocalDate hireDate;
    private LocalDate contractEnd;
    private String status;
    private String address;
    private String workTime;
    private String memo;

    public StaffDTO(int staffId, String loginId, String password, String salt, String name, String phone1, String phone2, String phone3, String role, String empType, Integer hourlyWage, Integer monthlyPay, LocalDate hireDate, LocalDate contractEnd, String status, String address, String workTime, String memo) {
        this.staffId = staffId;
        this.loginId = loginId;
        this.password = password;
        this.salt = salt;
        this.name = name;
        this.phone1 = phone1;
        this.phone2 = phone2;
        this.phone3 = phone3;
        this.role = role;
        this.empType = empType;
        this.hourlyWage = hourlyWage;
        this.monthlyPay = monthlyPay;
        this.hireDate = hireDate;
        this.contractEnd = contractEnd;
        this.status = status;
        this.address = address;
        this.workTime = workTime;
        this.memo = memo;
    }

    public int getStaffId() {
        return staffId;
    }

    public String getLoginId() {
        return loginId;
    }

    public String getPassword() {
        return password;
    }

    public String getSalt() {
        return salt;
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

    public String getRole() {
        return role;
    }

    public String getEmpType() {
        return empType;
    }

    public Integer getHourlyWage() {
        return hourlyWage;
    }

    public Integer getMonthlyPay() {
        return monthlyPay;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public LocalDate getContractEnd() {
        return contractEnd;
    }

    public String getStatus() {
        return status;
    }

    public String getAddress() {
        return address;
    }

    public String getWorkTime() {
        return workTime;
    }

    public String getMemo() {
        return memo;
    }
}
