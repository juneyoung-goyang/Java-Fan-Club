package dicepos.main;

import dicepos.table.TableUsageDTO;

/** 키오스크 창(KioskFrame)에 붙는 화면이 구현한다. (③ 김은별이 정하고 ⑥ 김하진도 쓴다) */
public interface KioskScreen {
    /** 화면을 열 때. usage는 지금 이 룸의 이용 건 */
    void start(TableUsageDTO usage);

    /** 처음 화면으로 돌아갈 때 (스레드 멈추기 등) */
    void stop();
}
