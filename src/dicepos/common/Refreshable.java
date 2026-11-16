package dicepos.common;

/** 메인 창에서 화면을 열 때마다 DB를 다시 읽고 싶은 화면이 구현한다. */
public interface Refreshable {
    void refresh();
}
