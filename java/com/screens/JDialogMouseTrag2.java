/*
 * Decompiled with CFR 0.152.
 */
package com.screens;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JDialog;

public class JDialogMouseTrag2
extends MouseAdapter {
    private JDialog jdialg;
    private Point mouseDownCompCoords = null;

    public JDialogMouseTrag2(JDialog jd) {
        this.jdialg = jd;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        this.mouseDownCompCoords = null;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        this.mouseDownCompCoords = e.getPoint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point currCoords = e.getLocationOnScreen();
        this.jdialg.setLocation(currCoords.x - this.mouseDownCompCoords.x, currCoords.y - this.mouseDownCompCoords.y);
    }
}

