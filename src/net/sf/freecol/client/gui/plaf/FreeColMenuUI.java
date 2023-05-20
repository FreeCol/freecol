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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicMenuUI;

import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.panel.FreeColImageBorder;
import net.sf.freecol.common.util.ImageUtils;


/**
 * UI-class for menu items.
 */
public class FreeColMenuUI extends BasicMenuUI {

    private AncestorListener ancestorListener = createAncestorListener();
    private boolean topLevelMenuItem = false;
    
    public static ComponentUI createUI(@SuppressWarnings("unused") JComponent c) {
        return new FreeColMenuUI();
    }


    @Override
    public void installUI(JComponent c) {
        super.installUI(c);

        c.addAncestorListener(ancestorListener);
        
        c.setOpaque(false);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        LAFUtilities.setProperties(g, c);

        if (topLevelMenuItem) {
            final Insets insets = c.getInsets();
            final Graphics2D g2d = (Graphics2D) g;
            
            final Composite oldComposite = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
            
            ImageUtils.fillTexture(g2d,
                    ImageLibrary.getMenuBackground(),
                    insets.left,
                    insets.top,
                    c.getWidth() - insets.right - insets.left,
                    c.getHeight() - insets.top - insets.bottom);
            g2d.setComposite(oldComposite);
        }
        
        super.paint(g, c);
    }
    
    @Override
    protected void paintBackground(Graphics g, JMenuItem menuItem, Color bgColor) {
        if (!topLevelMenuItem) {
            super.paintBackground(g, menuItem, bgColor);
            return;
        }
        
        final ButtonModel model = menuItem.getModel();
        final Color bgColorModified = (model.isSelected()) ? new Color(255, 255, 255, 128) : bgColor;
        super.paintBackground(g, menuItem, bgColorModified);
    }
    
    private AncestorListener createAncestorListener() {
        return new AncestorListener() {
            @Override
            public void ancestorRemoved(AncestorEvent event) {}
            
            @Override
            public void ancestorMoved(AncestorEvent event) {}
            
            @Override
            public void ancestorAdded(AncestorEvent event) {
                final JComponent c = event.getComponent();
                if (c.getParent() instanceof JMenuBar) {
                    final int padding = (int) (2 * FontLibrary.getFontScaling());
                    c.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createEmptyBorder(padding, padding, padding, padding),
                            FreeColImageBorder.simpleButtonBorder));
                    topLevelMenuItem = true;
                } else {
                    c.setBorder(null);
                    topLevelMenuItem = false;
                }
            }
        };
    } 
}
