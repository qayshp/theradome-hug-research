/*
 * Decompiled with CFR 0.152.
 */
package com.screens;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicProgressBarUI;

public class FancyProgressBar
extends BasicProgressBarUI {
    @Override
    protected Dimension getPreferredInnerVertical() {
        return new Dimension(40, 146);
    }

    @Override
    protected Dimension getPreferredInnerHorizontal() {
        return new Dimension(146, 40);
    }

    @Override
    protected void paintDeterminate(Graphics g, JComponent c) {
        Graphics2D g2d = (Graphics2D)g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int iStrokWidth = 3;
        g2d.setStroke(new BasicStroke(iStrokWidth, 1, 1));
        g2d.setColor(Color.white);
        g2d.setBackground(Color.white);
        int width = this.progressBar.getWidth();
        int height = this.progressBar.getHeight();
        RoundRectangle2D.Double outline = new RoundRectangle2D.Double(iStrokWidth / 2, iStrokWidth / 2, width - iStrokWidth, height - iStrokWidth, height, height);
        g2d.draw(outline);
        int iInnerHeight = height - iStrokWidth * 4;
        int iInnerWidth = width - iStrokWidth * 4;
        double dProgress = this.progressBar.getPercentComplete();
        if (dProgress < 0.0) {
            dProgress = 0.0;
        } else if (dProgress > 1.0) {
            dProgress = 1.0;
        }
        iInnerWidth = (int)Math.round((double)iInnerWidth * dProgress);
        int x = iStrokWidth * 2;
        int y = iStrokWidth * 2;
        Point2D.Double start = new Point2D.Double(x, y);
        Point2D.Double end = new Point2D.Double(x, y + iInnerHeight);
        float[] dist = new float[]{0.0f, 0.25f, 1.0f};
        Color myLightblue = new Color(25, 164, 219);
        Color[] colors = new Color[]{myLightblue, myLightblue.brighter(), myLightblue.darker()};
        LinearGradientPaint p = new LinearGradientPaint(start, end, dist, colors);
        g2d.setPaint(p);
        RoundRectangle2D.Double fill = new RoundRectangle2D.Double(iStrokWidth * 2, iStrokWidth * 2, iInnerWidth, iInnerHeight, iInnerHeight, iInnerHeight);
        g2d.fill(fill);
        g2d.dispose();
    }

    @Override
    protected void paintIndeterminate(Graphics g, JComponent c) {
        super.paintIndeterminate(g, c);
    }
}

