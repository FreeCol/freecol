/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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

package net.sf.freecol.client.gui.plaf;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.MouseInputAdapter;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalInternalFrameUI;

import net.sf.freecol.client.gui.Canvas.ToolBoxFrame;
import net.sf.freecol.client.gui.panel.FreeColBorder;

public class FreeColInternalFrameUI extends MetalInternalFrameUI {

    public FreeColInternalFrameUI(JInternalFrame b)   {
        super(b);
    }

    public static ComponentUI createUI(JComponent b)    {
        return new FreeColInternalFrameUI((JInternalFrame)b);
    }
    
    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        
        final JInternalFrame f = (JInternalFrame) c;
        final boolean toolBox = (f instanceof ToolBoxFrame);
        
        if (!toolBox) {
            f.getRootPane().setWindowDecorationStyle(JRootPane.NONE);
            setNorthPane(null);
            setSouthPane(null);
            setWestPane(null);
            setEastPane(null);
        } else {
            f.setMaximizable(true);
            f.setClosable(true);
            f.setIconifiable(true);
        }
    }
    
    @Override
    protected MouseInputAdapter createBorderListener(JInternalFrame w) {
        return new FreeColBorderListener();
    }
    
    
    /**
     * Handles areas on the border that should be handled as out-of-bounds for
     * the component. This allows a border to have a non-rectangular shape.
     * 
     * Events on areas that are considered out-of-bounds are delegated to the
     * internal frame's parent.
     */
    private final class FreeColBorderListener extends BorderListener {
        
        private boolean resizing = false;
        
        @Override
        public void mousePressed(MouseEvent e) {
            if (isOutOfBounds(e)) {
                handleOutOfBounds(e);
            } else {
                resizing = true;
                super.mousePressed(e);
            }
        }
        
        @Override
        public void mouseReleased(MouseEvent e) {
            if (!resizing && isOutOfBounds(e)) {
                handleOutOfBounds(e);
            } else {
                resizing = false;
                super.mouseReleased(e);
            }
        }
        
        @Override
        public void mouseDragged(MouseEvent e) {
            if (!resizing && isOutOfBounds(e)) {
                handleOutOfBounds(e);
            } else {
                super.mouseDragged(e);
            }
        }
        
        @Override
        public void mouseMoved(MouseEvent e) {
            if (!resizing && isOutOfBounds(e)) {
                handleOutOfBounds(e);
            } else {
                super.mouseMoved(e);
            }
        }
        
        @Override
        public void mouseClicked(MouseEvent e) {
            if (!resizing && isOutOfBounds(e)) {
                handleOutOfBounds(e);
            } else {
                super.mouseClicked(e);
            }
        }
        
        @Override
        public void mouseEntered(MouseEvent e) {
            if (!resizing && isOutOfBounds(e)) {
                handleOutOfBounds(e);
            } else {
                super.mouseEntered(e);
            }
        }
        
        @Override
        public void mouseExited(MouseEvent e) {
            if (!resizing && isOutOfBounds(e)) {
                handleOutOfBounds(e);
            } else {
                super.mouseExited(e);
            }
        }
        
        private void handleOutOfBounds(MouseEvent e) {
            resetCursor();
            
            if (frame.getParent() == null) {
                return;
            }
            
            dispatchTo(e, frame.getParent());
        }

        private void resetCursor() {
            Cursor lastCursor = frame.getLastCursor();
            if (lastCursor == null) {
                lastCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
            }
            frame.setCursor(lastCursor);
        }
        
        private boolean isOutOfBounds(MouseEvent e) {
            final Border border = frame.getBorder();
            if (!(border instanceof FreeColBorder) ) {
                return false;
            }
            
            final List<Rectangle> bounds = ((FreeColBorder) border).getOpenSpace(frame);
            return bounds.stream().anyMatch(b -> b.contains(e.getPoint()));
        }
        
        private void dispatchTo(MouseEvent e, Component target) {
            final Component source = (Component) e.getSource();
            MouseEvent targetEvent = SwingUtilities.convertMouseEvent(source, e, target);
            target.dispatchEvent(targetEvent);
        }
    }
}
