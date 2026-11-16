package dicepos.staff;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

/** 매출 막대그래프. paintComponent에서 직접 그린다. (① 김재민) */
public class BarChart extends JPanel {
    private List<String> labels = new ArrayList<>();
    private List<Integer> values = new ArrayList<>();

    public BarChart() {
        setPreferredSize(new Dimension(400, 220));
        setBackground(Color.WHITE);
    }

    public void setData(List<String> labels, List<Integer> values) {
        this.labels = labels;
        this.values = values;
        repaint();   // 다시 그리기
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth();
        int h = getHeight();
        int left = 10;
        int bottom = h - 25;   // 아래 글자 자리
        int top = 20;          // 위 금액 자리

        g.setColor(Color.GRAY);
        g.drawLine(left, bottom, w - 10, bottom);
        if (values.isEmpty()) {
            g.drawString("조회한 기간에 결제가 없습니다.", w / 2 - 80, h / 2);
            return;
        }

        int max = 1;
        for (int v : values) max = Math.max(max, v);

        int slot = (w - 20) / values.size();     // 막대 하나가 쓰는 너비
        int barWidth = Math.max(4, slot * 6 / 10);
        for (int i = 0; i < values.size(); i++) {
            int barHeight = (int) ((long) values.get(i) * (bottom - top) / max);
            int x = left + i * slot + (slot - barWidth) / 2;
            int y = bottom - barHeight;
            g.setColor(new Color(70, 110, 180));
            g.fillRect(x, y, barWidth, barHeight);

            g.setColor(Color.DARK_GRAY);
            String label = labels.get(i);
            if (label.length() > 5) label = label.substring(label.length() - 5);   // 2026-11-03 → 11-03
            g.drawString(label, x, bottom + 15);
            if (slot > 45) g.drawString(String.format("%,d", values.get(i)), x, y - 4);
        }
    }
}
