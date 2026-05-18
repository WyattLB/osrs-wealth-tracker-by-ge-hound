package com.wealthtracker;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

public class WealthChartPanel extends JPanel
{
	private static final int PADDING_LEFT = 50;
	private static final int PADDING_RIGHT = 8;
	private static final int PADDING_TOP = 8;
	private static final int PADDING_BOTTOM = 20;
	private static final Color LINE_COLOR = new Color(0, 220, 100);
	private static final Color FILL_COLOR = new Color(0, 220, 100, 35);
	private static final Color DOT_COLOR = new Color(0, 255, 120);
	private static final Color GRID_COLOR = new Color(255, 255, 255, 25);

	private List<WealthSnapshot> snapshots = new ArrayList<>();

	public WealthChartPanel()
	{
		setPreferredSize(new Dimension(0, 150));
		setMinimumSize(new Dimension(0, 150));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setOpaque(true);
	}

	public void setData(List<WealthSnapshot> data)
	{
		this.snapshots = (data != null) ? new ArrayList<>(data) : new ArrayList<>();
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		int w = getWidth();
		int h = getHeight();

		if (snapshots == null || snapshots.size() < 2)
		{
			g2.setFont(FontManager.getRunescapeSmallFont());
			FontMetrics fm = g2.getFontMetrics();
			String msg1 = "Open your bank to";
			String msg2 = "record a snapshot";
			int lineH = fm.getHeight() + 2;
			int startY = h / 2 - lineH / 2;
			g2.setColor(new Color(100, 100, 100));
			g2.drawString(msg1, (w - fm.stringWidth(msg1)) / 2, startY);
			g2.setColor(new Color(80, 80, 80));
			g2.drawString(msg2, (w - fm.stringWidth(msg2)) / 2, startY + lineH);
			return;
		}

		int n = snapshots.size();
		long minVal = Long.MAX_VALUE;
		long maxVal = Long.MIN_VALUE;
		for (WealthSnapshot s : snapshots)
		{
			if (s.getTotalNetWorth() < minVal) minVal = s.getTotalNetWorth();
			if (s.getTotalNetWorth() > maxVal) maxVal = s.getTotalNetWorth();
		}
		if (minVal == maxVal)
		{
			minVal = Math.max(0, minVal - 1);
			maxVal = maxVal + 1;
		}

		int chartW = w - PADDING_LEFT - PADDING_RIGHT;
		int chartH = h - PADDING_TOP - PADDING_BOTTOM;
		double valueRange = (double) (maxVal - minVal);

		int[] px = new int[n];
		int[] py = new int[n];
		for (int i = 0; i < n; i++)
		{
			px[i] = PADDING_LEFT + (int) ((double) i / (n - 1) * chartW);
			double norm = (snapshots.get(i).getTotalNetWorth() - minVal) / valueRange;
			py[i] = PADDING_TOP + chartH - (int) (norm * chartH);
		}

		g2.setFont(FontManager.getRunescapeSmallFont());
		FontMetrics fm = g2.getFontMetrics();
		for (int level = 0; level <= 3; level++)
		{
			double frac = (double) level / 3;
			long labelVal = minVal + (long) (frac * valueRange);
			int lineY = PADDING_TOP + chartH - (int) (frac * chartH);

			g2.setColor(GRID_COLOR);
			g2.drawLine(PADDING_LEFT, lineY, w - PADDING_RIGHT, lineY);

			String shortLabel = compactGp(labelVal);
			g2.setColor(new Color(140, 140, 140));
			int labelX = Math.max(0, PADDING_LEFT - fm.stringWidth(shortLabel) - 4);
			g2.drawString(shortLabel, labelX, lineY + fm.getAscent() / 2);
		}

		GeneralPath fillPath = new GeneralPath();
		fillPath.moveTo(px[0], PADDING_TOP + chartH);
		fillPath.lineTo(px[0], py[0]);
		for (int i = 1; i < n; i++) fillPath.lineTo(px[i], py[i]);
		fillPath.lineTo(px[n - 1], PADDING_TOP + chartH);
		fillPath.closePath();
		g2.setColor(FILL_COLOR);
		g2.fill(fillPath);

		g2.setColor(LINE_COLOR);
		g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		for (int i = 0; i < n - 1; i++) g2.drawLine(px[i], py[i], px[i + 1], py[i + 1]);

		int dotRadius = 4;
		g2.setColor(new Color(0, 255, 120, 60));
		g2.fillOval(px[n - 1] - dotRadius - 2, py[n - 1] - dotRadius - 2,
			(dotRadius + 2) * 2, (dotRadius + 2) * 2);
		g2.setColor(DOT_COLOR);
		g2.fillOval(px[n - 1] - dotRadius, py[n - 1] - dotRadius,
			dotRadius * 2, dotRadius * 2);
		g2.setColor(ColorScheme.DARKER_GRAY_COLOR);
		g2.setStroke(new BasicStroke(1.5f));
		g2.drawOval(px[n - 1] - dotRadius, py[n - 1] - dotRadius,
			dotRadius * 2, dotRadius * 2);
	}

	private String compactGp(long value)
	{
		if (value < 0) return "-" + compactGp(-value);
		if (value >= 1_000_000_000L) return String.format("%.1fb", value / 1_000_000_000.0);
		if (value >= 1_000_000L) return String.format("%.1fm", value / 1_000_000.0);
		if (value >= 1_000L) return String.format("%dk", value / 1_000);
		return value + "";
	}
}
