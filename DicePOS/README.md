# DicePOS — 완성 코드

2차 계획서(`2차계획서.docx`)의 화면과 기능을 모두 구현한 코드다. Java 17 + Swing + MySQL 8.0, 외부 라이브러리는 `mysql-connector-j`만 쓴다.

## 실행 방법

1. [00 시작 가이드](<../개발문서/00_시작가이드.md>) 1~4단계로 Eclipse 프로젝트를 만들고 DB를 만든다. (`개발문서/dicepos_schema.sql`)
2. 이 폴더의 `src` 안 `dicepos` 폴더를 Eclipse 프로젝트의 `src` 위로 끌어다 놓는다. (전에 받은 공통 코드가 있으면 지우고 넣는다)
3. `dicepos.common.DBUtil`의 `"본인 root 비밀번호"`를 자기 비밀번호로 바꾼다.
4. 실행
   - POS: `dicepos.main.PosMain` → Run As → Java Application
   - 키오스크: `dicepos.main.KioskMain` (테이블마다 하나씩, 같은 PC에서 여러 개 띄워도 된다)

### 예시 계정

| 아이디 | 비밀번호 | 이름 | 권한 |
|---|---|---|---|
| admin | admin1234 | 김지우 | 관리자 |
| seoyeon | admin1234 | 한서연 | 관리자 |
| minseo | staff1234 | 박민서 | 직원 |

### 처음 써 보는 순서

1. POS를 켜면 잠겨 있다 → [출근] → `minseo` / `staff1234` (출근 기록 + 잠금 해제)
2. 테이블 현황 → Room 19 카드 → [이용 시작] → 2명, 회원 검색 `5678` (김민수)
3. 키오스크 실행 → `admin` / `admin1234` → Room 19 → 게임 추천, 음식 주문
4. POS 손님 주문받기 → [키오스크 새 주문] 확인 → 메뉴 버튼으로 추가 주문
5. 테이블 현황 → Room 19 → [이용 종료] → [결제하기] → 포인트·카드·현금 나눠 결제
6. 관리자 메뉴(예: 매출 관리)를 누르면 관리자 확인 → `admin` / `admin1234`

## 팀원별 파일

| 담당 | 패키지 | 화면 | DB 작업 |
|---|---|---|---|
| ① 김재민 | `dicepos.staff`, `common.Session`·`AuthDialog` | `StaffPanel`(+`StaffDialog`, `MemoDialog`), `WorkSalaryPanel`, `SalesPanel`(+`BarChart`), `CheckDialog`(근무자·관리자 확인) | `StaffDAO`, `WorkLogDAO`, `SalaryDAO`, `SalesDAO`, `PasswordUtil` |
| ② 송하안 | `dicepos.member`, `dicepos.payment` | `MemberPanel`, `PaymentDialog`, `PaymentHistoryPanel` | `MemberDAO`, `PaymentDAO`(결제 트랜잭션) |
| ③ 김은별 | `dicepos.game`, `main.KioskFrame`·`KioskMain` | `GamePanel`, `GameRentalPanel`, `KioskRecommendPanel`, 키오스크 창 | `GameDAO`, `GameRentalDAO`(반납·호출 트랜잭션), `RecommendDAO` |
| ④ 박준영 | `dicepos.menu`, `common`, `main.PosMainFrame`·`PosMain` | `MenuPanel`, `PurchaseOrderPanel`, POS 메인 창 | `MenuDAO`, `PurchaseOrderDAO`(발주·입고 트랜잭션) |
| ⑤ 서윤채 | `dicepos.table`, `common.FeeCalculator` | `TableStatusPanel`(+`TableCard`, `StartUsageDialog`, `EndUsageDialog`), `TableSettingPanel`, `FeeSettingPanel` | `CafeTableDAO`, `TableUsageDAO`, `FeeSettingDAO` |
| ⑥ 김하진 | `dicepos.order` | `OrderPanel`(+`NumberPadDialog`), `KioskOrderPanel`(+`MenuCard`), `KioskStatusPanel` | `OrderDAO`(주문·수량 변경·취소 트랜잭션) |

## 계획서에 없어서 정한 것

- **회원 등급과 적립률**: 계획서에는 "골드 5%" 예시만 있어서 나머지를 정했다. 누적 결제 10만 원 이상 실버, 30만 원 이상 골드. 적립은 카드·현금으로 낸 금액의 일반 1%, 실버 3%, 골드 5%. `PaymentDAO.gradeOf`, `earnRate`에서 바꿀 수 있다.
- **최저시급**: 2026년 10,320원 (`StaffDialog.MIN_WAGE`).
- **키오스크 룸 지정**: 계획서의 "아직 지정되지 않은 룸만" 고르기는 DB에 칸이 없어 빼고, 사용가능한 룸 중에서 고른다.
- **`FeeCalculator.perPerson` / `total`**: DB에서 요금을 읽으므로 `throws SQLException`이 붙었다. (`개발문서`의 임시 파일과 다른 점)

## 확인한 것

- MySQL 호환 DB에 `dicepos_schema.sql`을 넣고 출근·퇴근, 입장, POS·키오스크 주문, 수량 변경·취소, 게임 대여·예약·반납·호출, 결제, 발주·입고, 급여 확정, 매출 집계를 차례로 실행해 71개 항목을 확인했다. 계획서의 결제 예시(130분 2명 → 1인 7,000원, 골드 적립 1,125P)와 같은 값이 나온다.
- 중간에 실패하면 rollback 되는지(품절 메뉴가 섞인 주문, 이중 결제, 두 번 입고)도 확인했다.
- POS 13개 화면, 팝업, 키오스크 4개 화면을 모두 띄워 오류가 없는지 확인했다.
