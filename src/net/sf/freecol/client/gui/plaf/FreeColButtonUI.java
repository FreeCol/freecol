/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalButtonUI;

import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.panel.FreeColImageBorder;
import net.sf.freecol.client.gui.panel.FreeColButton;
import net.sf.freecol.client.gui.panel.FreeColButton.ButtonStyle;
import net.sf.freecol.common.util.ImageUtils;


/**
 * Sets the default opaque attribute to <i>false</i> and
 * uses a 10% black shading on the {@link #paintButtonPressed}.
 */
public class FreeColButtonUI extends MetalButtonUI {

    private boolean paintBackground;
    private PropertyChangeListener pcl;

    public static ComponentUI createUI(@SuppressWarnings("unused") JComponent c) {
        return new FreeColButtonUI();
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        
        pcl = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (FreeColButton.BUTTON_STYLE_PROPERTY_NAME.equals(evt.getPropertyName())) {
                    updateStyle(c, (ButtonStyle) evt.getNewValue());
                }
            }
        };
        c.addPropertyChangeListener(pcl);
        updateStyle(c, getButtonStyle(c));
    }
    
    private ButtonStyle getButtonStyle(JComponent c) {
        if (c instanceof FreeColButton) {
            return ((FreeColButton) c).getButtonStyle();
        } else {
            return (ButtonStyle) c.getClientProperty(FreeColButton.BUTTON_STYLE_PROPERTY_NAME);
        }
    }
    
    @Override
    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);
        c.removePropertyChangeListener(pcl);
    }

    protected void updateStyle(JComponent c, ButtonStyle buttonStyle) {
        final int padding = (int) (5 * FontLibrary.getFontScaling());
        if (buttonStyle == ButtonStyle.IMPORTANT) {
            c.setBorder(BorderFactory.createCompoundBorder(
                    FreeColImageBorder.buttonBorder,
                    BorderFactory.createEmptyBorder(padding, padding, padding, padding)));
            c.setOpaque(true);
            paintBackground = true;
        } else if (buttonStyle == ButtonStyle.TRANSPARENT) { 
            c.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            c.setOpaque(false);
            paintBackground = false;
        } else {
            c.setBorder(BorderFactory.createCompoundBorder(
                    FreeColImageBorder.simpleButtonBorder,
                    BorderFactory.createEmptyBorder(padding, padding, padding, padding)));
            c.setOpaque(false);
            paintBackground = true;
        }
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        LAFUtilities.setProperties(g, c);
        
        if (c.isOpaque() || paintBackground) {
            ImageUtils.drawTiledImage(ImageLibrary.getButtonBackground(),
                                      g, c, null);
        }
        super.paint(g, c);

        AbstractButton a = (AbstractButton) c;
        if (a.isRolloverEnabled()) {
            Point p = MouseInfo.getPointerInfo().getLocation();
            SwingUtilities.convertPointFromScreen(p, c);
            boolean rollover = c.contains(p);
            if (rollover) {
                paintButtonPressed(g, (AbstractButton) c);
            }
        }
    }

    @Override
    protected void paintButtonPressed(Graphics g, AbstractButton c) {
        if (c.isContentAreaFilled()) {
            Graphics2D g2d = (Graphics2D) g;
            Dimension size = c.getSize();
            Composite oldComposite = g2d.getComposite();
            Color oldColor = g2d.getColor();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));
            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, size.width, size.height);
            g2d.setComposite(oldComposite);
            g2d.setColor(oldColor);
        }
    }
}
