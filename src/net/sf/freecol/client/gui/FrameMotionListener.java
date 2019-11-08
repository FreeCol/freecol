/**
 *  Copyright (C) 2002-2019   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.client.gui;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.JInternalFrame;
import javax.swing.SwingUtilities;


/**
 * The mouse adapter to handle frame movement.
 */
public class FrameMotionListener extends MouseAdapter {

    private final JInternalFrame f;

    private Point loc = null;


    /**
     * Build a new frame motion listener.
     *
     * @param f The base internal frame.
     */
    FrameMotionListener(JInternalFrame f) {
        this.f = f;
    }

    // Override MouseAdapter

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        if (loc == null || f.getDesktopPane() == null
            || f.getDesktopPane().getDesktopManager() == null) {
            return;
        }

        Point p = SwingUtilities.convertPoint((Component)e.getSource(),
                                              e.getX(), e.getY(), null);
        int moveX = loc.x - p.x;
        int moveY = loc.y - p.y;
        f.getDesktopPane().getDesktopManager()
            .dragFrame(f, f.getX() - moveX, f.getY() - moveY);
        loc = p;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mousePressed(MouseEvent e) {
        if (f.getDesktopPane() == null
            || f.getDesktopPane().getDesktopManager() == null) {
            return;
        }
        loc = SwingUtilities.convertPoint((Component)e.getSource(),
                                          e.getX(), e.getY(), null);
        f.getDesktopPane().getDesktopManager().beginDraggingFrame(f);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        if (loc == null || f.getDesktopPane() == null
            || f.getDesktopPane().getDesktopManager() == null) {
            return;
        }
        f.getDesktopPane().getDesktopManager().endDraggingFrame(f);
    }
}
