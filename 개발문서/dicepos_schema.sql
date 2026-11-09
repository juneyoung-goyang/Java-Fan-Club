-- =========================================================
-- 다이스 POS (dicepos) DB 생성 스크립트 — MySQL 8.0
-- 피드백 반영 수정본 계획서의 17개 테이블과 같은 구조
-- 실행: MySQL Workbench에서 이 파일을 열고 전체 실행(번개 아이콘)
-- =========================================================
DROP DATABASE IF EXISTS dicepos;
CREATE DATABASE dicepos DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE dicepos;

-- staff (직원) : 담당 ①
CREATE TABLE staff (
  staff_id         INT           NOT NULL AUTO_INCREMENT COMMENT '직원 번호',
  login_id         VARCHAR(20)   NOT NULL COMMENT '아이디 (중복 불가)',
  password         VARCHAR(64)   NOT NULL COMMENT '비밀번호 (SHA-256으로 암호화한 값만 저장)',
  name             VARCHAR(20)   NOT NULL COMMENT '이름',
  phone1           VARCHAR(3)    NOT NULL COMMENT '전화번호 1',
  phone2           VARCHAR(4)    NOT NULL COMMENT '전화번호 2',
  phone3           VARCHAR(4)    NOT NULL COMMENT '전화번호 3',
  role             VARCHAR(10)   NOT NULL DEFAULT '직원' COMMENT '권한 (관리자 / 직원). 관리자만 관리자 화면을 열 수 있음',
  emp_type         VARCHAR(10)   NOT NULL COMMENT '고용 형태 (정규직 / 계약직 / 시간제)',
  hourly_wage      INT           COMMENT '시급 (시간제만 입력, 나머지는 NULL)',
  monthly_pay      INT           COMMENT '월 기본급 (정규직·계약직만 입력)',
  hire_date        DATE          NOT NULL COMMENT '입사일',
  contract_end     DATE          COMMENT '계약 종료일 (계약직만 입력)',
  status           VARCHAR(10)   NOT NULL DEFAULT '재직' COMMENT '재직 상태 (재직 / 퇴사)',
  address          VARCHAR(100)  COMMENT '주소 (선택 입력)',
  work_time        VARCHAR(20)   COMMENT '기본 근무 시간대 (예: 09:00~18:00, 선택 입력)',
  memo             VARCHAR(500)  COMMENT '직원 메모 (선택 입력)',
  PRIMARY KEY (staff_id),
  UNIQUE (login_id)
) COMMENT '직원';

-- member (회원) : 담당 ②
CREATE TABLE member (
  member_id        INT           NOT NULL AUTO_INCREMENT COMMENT '회원 번호',
  name             VARCHAR(20)   NOT NULL COMMENT '이름',
  phone1           VARCHAR(3)    NOT NULL COMMENT '전화번호 1',
  phone2           VARCHAR(4)    NOT NULL COMMENT '전화번호 2',
  phone3           VARCHAR(4)    NOT NULL COMMENT '전화번호 3',
  gender           CHAR(1)       NOT NULL COMMENT '성별 (M / F)',
  grade            VARCHAR(10)   NOT NULL DEFAULT '일반' COMMENT '등급 (일반 / 실버 / 골드), 결제할 때 자동 변경',
  point            INT           NOT NULL DEFAULT 0 COMMENT '보유 포인트, 기본값 0',
  join_date        DATE          NOT NULL COMMENT '가입일',
  PRIMARY KEY (member_id),
  UNIQUE (phone1, phone2, phone3)
) COMMENT '회원';

-- cafe_table (카페 테이블) : 담당 ⑤
CREATE TABLE cafe_table (
  table_no         INT           NOT NULL COMMENT '테이블 번호',
  table_name       VARCHAR(10)   NOT NULL COMMENT '표시 이름 (예: T1, 룸A)',
  capacity         INT           NOT NULL COMMENT '최대 좌석 수 (인원 초과 검사)',
  is_active        TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '사용 여부 (0이면 테이블 현황에 표시하지 않음)',
  PRIMARY KEY (table_no),
  UNIQUE (table_name)
) COMMENT '카페 테이블';

-- fee_setting (이용 요금 설정) : 담당 ⑤
CREATE TABLE fee_setting (
  fee_id           INT           NOT NULL AUTO_INCREMENT COMMENT '요금 번호',
  base_minutes     INT           NOT NULL COMMENT '기본 시간(분)',
  base_price       INT           NOT NULL COMMENT '1인 기본요금',
  extra_unit       INT           NOT NULL COMMENT '추가 요금 단위(분)',
  extra_price      INT           NOT NULL COMMENT '1인 추가요금',
  apply_date       DATE          NOT NULL COMMENT '적용 시작일',
  PRIMARY KEY (fee_id)
) COMMENT '이용 요금 설정';

-- menu (메뉴) : 담당 ④
CREATE TABLE menu (
  menu_id          INT           NOT NULL AUTO_INCREMENT COMMENT '메뉴 번호',
  name             VARCHAR(30)   NOT NULL COMMENT '메뉴 이름 (중복 불가)',
  category         VARCHAR(10)   NOT NULL COMMENT '분류 (음료 / 간식 / 식사)',
  price            INT           NOT NULL COMMENT '판매 가격',
  stock            INT           NOT NULL DEFAULT 0 COMMENT '재고 수량, 기본값 0',
  sold_out         TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '품절 여부 (1이면 주문 불가)',
  PRIMARY KEY (menu_id),
  UNIQUE (name)
) COMMENT '메뉴';

-- game (보드게임) : 담당 ③
CREATE TABLE game (
  game_id          INT           NOT NULL AUTO_INCREMENT COMMENT '게임 번호',
  name             VARCHAR(50)   NOT NULL COMMENT '게임 이름',
  genre            VARCHAR(10)   NOT NULL COMMENT '장르 (파티 / 가족 / 전략 / 추리 / 카드)',
  min_player       INT           NOT NULL COMMENT '최소 인원',
  max_player       INT           NOT NULL COMMENT '최대 인원',
  difficulty       INT           NOT NULL COMMENT '난이도 1~5 (★로 표시)',
  play_min         INT           NOT NULL COMMENT '평균 플레이 시간(분)',
  stock            INT           NOT NULL DEFAULT 0 COMMENT '보유 수량, 기본값 0 (입고하면 증가, 폐기하면 감소)',
  status           VARCHAR(10)   NOT NULL DEFAULT '정상' COMMENT '상태 (정상 / 부품누락 / 폐기)',
  location         VARCHAR(10)   COMMENT '진열 위치 (예: A-1)',
  PRIMARY KEY (game_id)
) COMMENT '보드게임';

-- table_usage (테이블 이용) : 담당 ⑤
CREATE TABLE table_usage (
  usage_id         INT           NOT NULL AUTO_INCREMENT COMMENT '이용 번호',
  table_no         INT           NOT NULL COMMENT '이용한 테이블',
  member_id        INT           COMMENT '회원 번호 (비회원은 NULL)',
  people           INT           NOT NULL COMMENT '이용 인원',
  start_time       DATETIME      NOT NULL COMMENT '입장 시각',
  end_time         DATETIME      COMMENT '퇴장 시각 (이용 중이면 NULL)',
  status           VARCHAR(10)   NOT NULL DEFAULT '이용중' COMMENT '상태 (이용중 / 결제완료)',
  PRIMARY KEY (usage_id),
  FOREIGN KEY (table_no) REFERENCES cafe_table(table_no),
  FOREIGN KEY (member_id) REFERENCES member(member_id) ON DELETE SET NULL
) COMMENT '테이블 이용';

-- work_log (근무 기록) : 담당 ①
CREATE TABLE work_log (
  log_id           INT           NOT NULL AUTO_INCREMENT COMMENT '근무 기록 번호',
  staff_id         INT           NOT NULL COMMENT '근무한 직원',
  work_date        DATE          NOT NULL COMMENT '근무 날짜',
  clock_in         DATETIME      NOT NULL COMMENT '출근 시각',
  clock_out        DATETIME      COMMENT '퇴근 시각 (근무 중이면 NULL)',
  work_minutes     INT           COMMENT '그날 근무시간(분). 퇴근할 때 계산해서 저장',
  note             VARCHAR(100)  COMMENT '비고 (관리자가 시간을 고친 경우 그 사유)',
  PRIMARY KEY (log_id),
  FOREIGN KEY (staff_id) REFERENCES staff(staff_id)
) COMMENT '근무 기록';

-- salary (급여) : 담당 ①
CREATE TABLE salary (
  salary_id        INT           NOT NULL AUTO_INCREMENT COMMENT '급여 번호',
  staff_id         INT           NOT NULL COMMENT '급여를 받는 직원',
  pay_month        CHAR(7)       NOT NULL COMMENT '급여 월 (예: 2026-11). 같은 달 중복 확정 불가',
  work_minutes     INT           NOT NULL COMMENT '그 달 누적 근무시간(분) = work_log 합계',
  base_amount      INT           NOT NULL COMMENT '기본 급여 (시간제: 누적 시간 × 시급, 정규·계약: 월 기본급)',
  extra_amount     INT           NOT NULL DEFAULT 0 COMMENT '추가 수당 (주휴수당 등, 관리자 입력, 기본값 0)',
  total_pay        INT           NOT NULL COMMENT '지급액 (base_amount + extra_amount)',
  status           VARCHAR(10)   NOT NULL DEFAULT '미지급' COMMENT '지급 상태 (미지급 / 지급완료)',
  pay_date         DATE          COMMENT '지급일 (지급 전이면 NULL)',
  PRIMARY KEY (salary_id),
  FOREIGN KEY (staff_id) REFERENCES staff(staff_id),
  UNIQUE (staff_id, pay_month)
) COMMENT '급여';

-- payment (결제) : 담당 ②
CREATE TABLE payment (
  pay_id           INT           NOT NULL AUTO_INCREMENT COMMENT '결제 번호',
  usage_id         INT           NOT NULL COMMENT '결제한 테이블 이용 건 (한 이용 건은 한 번만 결제)',
  staff_id         INT           COMMENT '결제를 처리한 직원',
  member_id        INT           COMMENT '결제한 회원 (비회원은 NULL)',
  pay_time         DATETIME      NOT NULL COMMENT '결제 일시 (연월일 시분초)',
  total_amount     INT           NOT NULL COMMENT '총 결제 금액 (= 결제 상세 소계의 합)',
  card_amount      INT           NOT NULL DEFAULT 0 COMMENT '카드 결제 금액',
  cash_amount      INT           NOT NULL DEFAULT 0 COMMENT '현금 결제 금액',
  point_amount     INT           NOT NULL DEFAULT 0 COMMENT '포인트 결제 금액 (사용한 포인트)',
  earn_point       INT           NOT NULL DEFAULT 0 COMMENT '이번 결제로 적립한 포인트 (비회원은 0)',
  PRIMARY KEY (pay_id),
  FOREIGN KEY (usage_id) REFERENCES table_usage(usage_id),
  UNIQUE (usage_id),
  FOREIGN KEY (staff_id) REFERENCES staff(staff_id) ON DELETE SET NULL,
  FOREIGN KEY (member_id) REFERENCES member(member_id) ON DELETE SET NULL
) COMMENT '결제';

-- payment_detail (결제 상세) : 담당 ②
CREATE TABLE payment_detail (
  pay_detail_id    INT           NOT NULL AUTO_INCREMENT COMMENT '결제 상세 번호',
  pay_id           INT           NOT NULL COMMENT '어느 결제의 상세인지',
  item_type        VARCHAR(10)   NOT NULL COMMENT '항목 구분 (이용료 / 메뉴)',
  menu_id          INT           COMMENT '상품(메뉴) 번호. 이용료 줄은 NULL',
  qty              INT           NOT NULL COMMENT '구매 수량 (이용료 줄은 이용 인원)',
  unit_price       INT           NOT NULL COMMENT '구매 단가 (메뉴는 menu.price에서 불러옴, 이용료는 1인 요금)',
  subtotal         INT           NOT NULL COMMENT '상품별 결제 금액 소계 (qty × unit_price)',
  PRIMARY KEY (pay_detail_id),
  FOREIGN KEY (pay_id) REFERENCES payment(pay_id) ON DELETE CASCADE,
  FOREIGN KEY (menu_id) REFERENCES menu(menu_id)
) COMMENT '결제 상세';

-- orders (주문) : 담당 ⑥
CREATE TABLE orders (
  order_id         INT           NOT NULL AUTO_INCREMENT COMMENT '주문 번호',
  usage_id         INT           NOT NULL COMMENT '어느 테이블 이용 건의 주문인지 (후불제)',
  staff_id         INT           COMMENT '주문을 받은 직원 (키오스크 주문은 NULL)',
  channel          VARCHAR(10)   NOT NULL COMMENT '주문 경로 (POS / 키오스크)',
  order_time       DATETIME      NOT NULL COMMENT '주문 일시',
  total_amount     INT           NOT NULL DEFAULT 0 COMMENT '주문 금액 합계 (= 주문 상세 소계의 합)',
  status           VARCHAR(10)   NOT NULL DEFAULT '확인' COMMENT '상태 (접수 / 확인). 키오스크 주문은 ''접수''로 들어오고 직원이 확인하면 ''확인''',
  PRIMARY KEY (order_id),
  FOREIGN KEY (usage_id) REFERENCES table_usage(usage_id),
  FOREIGN KEY (staff_id) REFERENCES staff(staff_id) ON DELETE SET NULL
) COMMENT '주문';

-- order_detail (주문 상세) : 담당 ⑥
CREATE TABLE order_detail (
  order_detail_id  INT           NOT NULL AUTO_INCREMENT COMMENT '주문 상세 번호',
  order_id         INT           NOT NULL COMMENT '어느 주문의 상세인지',
  menu_id          INT           NOT NULL COMMENT '주문한 메뉴',
  qty              INT           NOT NULL COMMENT '주문 수량',
  unit_price       INT           NOT NULL COMMENT '주문 당시 단가 (menu.price에서 불러옴)',
  subtotal         INT           NOT NULL COMMENT '소계 (qty × unit_price)',
  PRIMARY KEY (order_detail_id),
  FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
  FOREIGN KEY (menu_id) REFERENCES menu(menu_id)
) COMMENT '주문 상세';

-- purchase_order (발주) : 담당 ④
CREATE TABLE purchase_order (
  po_id            INT           NOT NULL AUTO_INCREMENT COMMENT '발주 번호',
  staff_id         INT           COMMENT '발주한 직원',
  supplier         VARCHAR(30)   COMMENT '거래처 (선택 입력)',
  order_date       DATETIME      NOT NULL COMMENT '발주 일시',
  total_amount     INT           NOT NULL DEFAULT 0 COMMENT '총 발주 금액 (= 발주 상세 금액의 합)',
  status           VARCHAR(10)   NOT NULL DEFAULT '요청' COMMENT '상태 (요청 / 입고 / 취소)',
  receive_date     DATETIME      COMMENT '입고 일시 (입고 전이면 NULL)',
  PRIMARY KEY (po_id),
  FOREIGN KEY (staff_id) REFERENCES staff(staff_id) ON DELETE SET NULL
) COMMENT '발주';

-- purchase_order_detail (발주 상세) : 담당 ④
CREATE TABLE purchase_order_detail (
  po_detail_id     INT           NOT NULL AUTO_INCREMENT COMMENT '발주 상세 번호',
  po_id            INT           NOT NULL COMMENT '어느 발주의 상세인지',
  item_type        VARCHAR(10)   NOT NULL COMMENT '품목 구분 (메뉴 / 보드게임)',
  menu_id          INT           COMMENT '발주한 메뉴 (보드게임이면 NULL)',
  game_id          INT           COMMENT '발주한 보드게임 (메뉴면 NULL)',
  qty              INT           NOT NULL COMMENT '발주 수량',
  unit_cost        INT           NOT NULL COMMENT '매입 단가 (같은 품목의 직전 발주 단가를 불러오고 수정 가능)',
  amount           INT           NOT NULL COMMENT '발주 금액 (qty × unit_cost)',
  PRIMARY KEY (po_detail_id),
  FOREIGN KEY (po_id) REFERENCES purchase_order(po_id) ON DELETE CASCADE,
  FOREIGN KEY (menu_id) REFERENCES menu(menu_id),
  FOREIGN KEY (game_id) REFERENCES game(game_id)
) COMMENT '발주 상세';

-- recommend (게임 추천 기록) : 담당 ③
CREATE TABLE recommend (
  rec_id           INT           NOT NULL AUTO_INCREMENT COMMENT '추천 번호',
  usage_id         INT           NOT NULL COMMENT '추천을 받은 테이블 이용 건',
  member_id        INT           COMMENT '추천받은 회원 (비회원은 NULL)',
  game_id          INT           NOT NULL COMMENT '추천한 게임',
  rec_time         DATETIME      NOT NULL COMMENT '추천한 시각',
  chosen           TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '손님이 이 게임을 골랐는지 (대여 신청·예약하면 1)',
  PRIMARY KEY (rec_id),
  FOREIGN KEY (usage_id) REFERENCES table_usage(usage_id),
  FOREIGN KEY (member_id) REFERENCES member(member_id) ON DELETE SET NULL,
  FOREIGN KEY (game_id) REFERENCES game(game_id)
) COMMENT '게임 추천 기록';

-- game_rental (게임 대여·예약) : 담당 ③
CREATE TABLE game_rental (
  rental_id        INT           NOT NULL AUTO_INCREMENT COMMENT '대여 번호',
  game_id          INT           NOT NULL COMMENT '대여·예약한 게임',
  usage_id         INT           NOT NULL COMMENT '어느 테이블 이용 건인지',
  status           VARCHAR(10)   NOT NULL COMMENT '상태 (예약 / 대여중 / 반납 / 취소)',
  request_time     DATETIME      NOT NULL COMMENT '신청 시각 (예약 대기 순서의 기준)',
  start_time       DATETIME      COMMENT '대여 시작 시각 (게임을 받은 시각)',
  return_time      DATETIME      COMMENT '반납 시각 (대여 중이면 NULL)',
  PRIMARY KEY (rental_id),
  FOREIGN KEY (game_id) REFERENCES game(game_id),
  FOREIGN KEY (usage_id) REFERENCES table_usage(usage_id)
) COMMENT '게임 대여·예약';

-- =========================================================
-- 예시 데이터 (개발·시연용, 필요하면 각자 추가)
-- 비밀번호는 SHA-256 값: admin1234 / staff1234
-- =========================================================
INSERT INTO staff (login_id, password, name, phone1, phone2, phone3, role, emp_type, hourly_wage, monthly_pay, hire_date, status) VALUES
 ('admin', 'ac9689e2272427085e35b9d3e3e8bed88cb3434828b43b86fc0596cad4c6e270', '김지우', '010', '1234', '5678', '관리자', '정규직', NULL, 2600000, '2024-03-02', '재직'),
 ('minseo', '02defbfb8190f9d0719ef7a23da2049bd2e61442bc14021a6d8a4ae35ca334b7', '박민서', '010', '9876', '5432', '직원', '시간제', 10320, NULL, '2026-08-15', '재직');
INSERT INTO cafe_table (table_no, table_name, capacity) VALUES
 (1,'T1',4),(2,'T2',4),(3,'T3',4),(4,'T4',2),(5,'T5',6),(6,'T6',6),(7,'T7',4),(8,'T8',4);
INSERT INTO fee_setting (base_minutes, base_price, extra_unit, extra_price, apply_date) VALUES (60, 3500, 10, 500, '2026-11-01');
INSERT INTO menu (name, category, price, stock) VALUES
 ('아메리카노','음료',3000,50),('오렌지주스','음료',3000,30),('콜라','음료',2000,24),
 ('감자칩','간식',2500,20),('츄러스','간식',3500,0),('라면','식사',4500,15),('떡볶이','식사',6000,10);
UPDATE menu SET sold_out = 1 WHERE stock = 0;
INSERT INTO game (name, genre, min_player, max_player, difficulty, play_min, stock, location) VALUES
 ('스플렌더','전략',2,4,2,30,2,'A-2'),('딕싯','가족',3,6,2,30,2,'C-2'),('우노','카드',2,10,1,20,4,'B-2'),
 ('젠가','파티',2,8,1,15,2,'C-1'),('할리갈리','카드',2,6,1,20,2,'B-1'),('카탄','전략',3,4,3,60,1,'A-1');
INSERT INTO member (name, phone1, phone2, phone3, gender, join_date) VALUES
 ('김민수','010','1234','5678','M','2026-03-01'),('이서연','010','2345','1122','F','2026-05-10');
