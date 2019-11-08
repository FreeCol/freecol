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

package net.sf.freecol.client.gui.plaf;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicOptionPaneUI;

import java.util.ArrayList;
import java.util.List;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.ChoiceItem;
import net.sf.freecol.client.gui.panel.MigPanel;
import net.sf.freecol.common.i18n.Messages;


/**
 * Draw the "image.background.FreeColOptionPane" resource as a tiled
 * background image.
 */
public class FreeColOptionPaneUI extends BasicOptionPaneUI {

    private int okIndex = -1, cancelIndex = -1;

    private List<JButton> newButtons = null;


    /** Trivial internal constructor. */
    private FreeColOptionPaneUI() {}


    /**
     * Choose the number of columns for the OptionPane buttons.
     *
     * @param nButtons The number of buttons.
     * @return A suitable number of columns.
     */
    private int getColumns(int nButtons) {
        return (nButtons > 21) ? 4
            :  ((nButtons % 4) == 0 && nButtons > 12) ? 4
            :  ((nButtons % 3) == 0 && nButtons > 6)  ? 3
            :  ((nButtons % 2) == 0 && nButtons > 4)  ? 2
            :  (nButtons > 5)  ? 2
            :  1;
    }

    /**
     * Prepare the new buttons for this component and cache.
     *
     * @return A list of buttons.
     */
    private List<JButton> prepareButtons() {
        if (this.newButtons != null) return this.newButtons;

        final String okLabel = Messages.message("ok");
        final String cancelLabel = Messages.message("cancel");
        Object[] buttons = getButtons();
        List<JButton> newButtons = new ArrayList<>(buttons.length);

        int maxWidth = 0, maxHeight = 0;
        for (int i = 0; i < buttons.length; i++) {
            JButton b;
            if (buttons[i] instanceof ChoiceItem) {
                ChoiceItem ci = (ChoiceItem)buttons[i];
                String label = ci.toString();
                Icon icon = ci.getIcon();
                b = (label.isEmpty()) ? new JButton(icon)
                    : new JButton(label, icon);
                b.setName("OptionPane.button." + label);
                if (ci.isOK()) this.okIndex = i;
                else if (ci.isCancel()) this.cancelIndex = i;
            } else if (buttons[i] instanceof Icon) {
                b = new JButton((Icon)buttons[i]);
                b.setName("OptionPane.button.withIcon");
            } else {
                String label = buttons[i].toString();
                b = new JButton(label);
                b.setName("OptionPane.button." + label);
                if (okLabel.equals(label)) this.okIndex = i;
                else if (cancelLabel.equals(label)) this.cancelIndex = i;
            }
            maxWidth = Math.max(maxWidth, b.getMinimumSize().width);
            maxHeight = Math.max(maxHeight, b.getMinimumSize().height);
            ActionListener buttonListener = createButtonActionListener(i);
            b.addActionListener(buttonListener);
            newButtons.add(b);
        }
        if (maxWidth > 0) {
            Dimension dimension = new Dimension(maxWidth, maxHeight);
            for (int i = 0; i < buttons.length; i++) {
                if (buttons[i] instanceof Icon) continue;
                JButton newb = newButtons.get(i);
                newb.setPreferredSize(dimension);
                newb.setMinimumSize(dimension);
            }
        }
        return newButtons;
    }

    /**
     * Get a button by index.
     *
     * @param index The index to look for.
     * @return The corresponding button.
     */
    private JButton getButton(int index) {
        return this.newButtons.get(index);
    }

    
    // Override BasicOptionPaneUI

    /**
     * {@inheritDoc}
     */
    public static ComponentUI createUI(@SuppressWarnings("unused")JComponent c) {
        return new FreeColOptionPaneUI();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Container createButtonArea() {
        Object[] buttons = getButtons();
        JPanel bottom = new MigPanel(new MigLayout((this.okIndex < 0)
                // Multi-line choice dialog
                ? "wrap " + getColumns(buttons.length)
                // Confirm dialog
                : "insets dialog"));
        bottom.setOpaque(false);
        bottom.setName("OptionPane.buttonArea");
        addButtonComponents(bottom, buttons, getInitialValueIndex());
        bottom.setSize(bottom.getPreferredSize());
        return bottom;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addButtonComponents(Container container, Object[] buttons,
                                       int initialIndex) {
        if (buttons == null) return;
        this.newButtons = prepareButtons();
        final int nButtons = this.newButtons.size();
        
        if (0 <= initialIndex && initialIndex < nButtons) {
            JButton b = getButton(initialIndex);
            this.initialFocusComponent = b;
            b.addHierarchyListener((HierarchyEvent e) -> {
                    if ((e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0) {
                        JButton button = (JButton)e.getComponent();
                        JRootPane root = SwingUtilities.getRootPane(button);
                        if (root != null) root.setDefaultButton(button);
                    }
                });
        }

        if (okIndex >= 0) {
            // If OK is present this is a confirm-dialog.  Put everything
            // in the same span especially the Cancel.
            if (cancelIndex >= 0) {
                container.add(getButton(cancelIndex), "tag cancel");
            }
            container.add(getButton(okIndex), "tag ok");
            for (int i = 0; i < nButtons; i++) {
                if (i == okIndex || i == cancelIndex) continue;
                container.add(getButton(i));
            }
        } else {
            // This must be a choice dialog.  The wrap argument to the
            // MigLayout constructor will do the work for us.
            for (int i = 0; i < nButtons; i++) {
                if (i == cancelIndex) continue;
                container.add(getButton(i));
            }
            if (cancelIndex >= 0) {
                container.add(getButton(cancelIndex),
                              "newline 20, tag cancel");
            }
        }
        // The righthand button gets truncated, so add some extra space.
        Dimension prefer = container.getPreferredSize();
        prefer = new Dimension((int)(prefer.getWidth() + 100),
                               (int)prefer.getHeight());
        container.setMinimumSize(prefer);
        container.setPreferredSize(prefer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selectInitialValue(JOptionPane op) {
        if (initialFocusComponent != null) {
            initialFocusComponent.requestFocus();
 
            if (initialFocusComponent instanceof JButton) {
                JRootPane root = SwingUtilities.getRootPane(initialFocusComponent);
                if (root != null) {
                    root.setDefaultButton((JButton)initialFocusComponent);
                }
            }
        }
    }


    // Override ComponentUI

    /**
     * {@inheritDoc}
     */
    @Override
    public void paint(Graphics g, JComponent c) {
        if (c.isOpaque()) {
            ImageLibrary.drawTiledImage("image.background.FreeColOptionPane",
                                        g, c, null);
        }
    }
}
