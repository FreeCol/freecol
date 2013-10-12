/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

import java.awt.Container;
import java.awt.Graphics;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicOptionPaneUI;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.panel.MigPanel;


/**
 * Draw the "background.FreeColOptionPane" resource as a tiled
 * background image.
 */
public class FreeColOptionPaneUI extends BasicOptionPaneUI {


    private FreeColOptionPaneUI() {}

    public static ComponentUI createUI(JComponent c) {
        return new FreeColOptionPaneUI();
    }


    // Override BasicOptionPaneUI

    /**
     * {@inheritDoc}
     */
    @Override
    protected Container createButtonArea() {
        MigPanel bottom = new MigPanel(new MigLayout("wrap 4"));
        bottom.setOpaque(false);
        bottom.setName("OptionPane.buttonArea");
        addButtonComponents(bottom, getButtons(), getInitialValueIndex());
        return bottom;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addButtonComponents(Container container, Object[] buttons,
                                       int initialIndex) {
        if (buttons == null) return;

        for (int i = 0; i < buttons.length; i++) {
            JButton b;
            if (buttons[i] instanceof Icon) {
                b = new JButton((Icon)buttons[i]);
                b.setName("OptionPane.button.withIcon");
            } else {
                b = new JButton(buttons[i].toString());
                b.setName("OptionPane.button." + buttons[i].toString());
            }
            String tag = (i == 0) ? "tag ok, span, split " + buttons.length
                : (i == buttons.length-1) ? "tag cancel"
                : "";
            container.add(b, tag);
            ActionListener buttonListener = createButtonActionListener(i);
            if (buttonListener != null) b.addActionListener(buttonListener);

            if (i != initialIndex) continue;
            b.addHierarchyListener(new HierarchyListener() {
                    public void hierarchyChanged(HierarchyEvent e) {
                        if ((e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0) {
                            JButton b = (JButton)e.getComponent();
                            JRootPane root = SwingUtilities.getRootPane(b);
                            if (root != null) root.setDefaultButton(b);
                        }
                    }
                });
        }
    }


    // Override ComponentUI

    /**
     * {@inheritDoc}
     */
    @Override
    public void paint(Graphics g, JComponent c) {
        if (c.isOpaque()) {
            ImageLibrary.drawTiledImage("background.FreeColOptionPane",
                                        g, c, null);
        }
    }
}
