package net.sf.freecol.client.gui;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.JInternalFrame;
import javax.swing.SwingUtilities;

public class FrameMotionListener extends MouseAdapter implements MouseMotionListener {

    public static final String COPYRIGHT = "Copyright (C) 2003-2012 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";
    
    private JInternalFrame f;

    private Point loc = null;


    FrameMotionListener(JInternalFrame f) {
        this.f = f;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (loc == null || f.getDesktopPane() == null || f.getDesktopPane().getDesktopManager() == null) {
            return;
        }

        Point p = SwingUtilities.convertPoint((Component) e.getSource(), e.getX(), e.getY(), null);
        int moveX = loc.x - p.x;
        int moveY = loc.y - p.y;
        f.getDesktopPane().getDesktopManager().dragFrame(f, f.getX() - moveX, f.getY() - moveY);
        loc = p;
    }

    @Override
    public void mouseMoved(MouseEvent arg0) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (f.getDesktopPane() == null || f.getDesktopPane().getDesktopManager() == null) {
            return;
        }
        loc = SwingUtilities.convertPoint((Component) e.getSource(), e.getX(), e.getY(), null);
        f.getDesktopPane().getDesktopManager().beginDraggingFrame(f);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (loc == null || f.getDesktopPane() == null || f.getDesktopPane().getDesktopManager() == null) {
            return;
        }
        f.getDesktopPane().getDesktopManager().endDraggingFrame(f);
    }

}
