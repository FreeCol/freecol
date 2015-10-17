/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

    private Component initialFocusComponent = null;

    private int okIndex = -1, cancelIndex = -1;

    private JButton[] newButtons = null;


    private FreeColOptionPaneUI() {}

    public static ComponentUI createUI(@SuppressWarnings("unused") JComponent c) {
        return new FreeColOptionPaneUI();
    }

    public Component getInitialFocusComponent() {
        return initialFocusComponent;
    }

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
     * Prepare the new buttons for this component.
     *
     * @param buttons An array of objects provided to define the buttons.
     */
    private void prepareButtons(Object[] buttons) {
        if (this.newButtons != null) return;

        final String okLabel = Messages.message("ok");
        final String cancelLabel = Messages.message("cancel");
        this.newButtons = new JButton[buttons.length];

        int maxWidth = 0, maxHeight = 0;
        for (int i = 0; i < buttons.length; i++) {
            JButton b;
            if (buttons[i] instanceof ChoiceItem) {
                ChoiceItem ci = (ChoiceItem)buttons[i];
                String label = ci.toString();
                Icon icon = ci.getIcon();
                b = (icon == null) ? new JButton(label)
                    : (label == null || label.isEmpty()) ? new JButton(icon)
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
            if (buttonListener != null) b.addActionListener(buttonListener);
            this.newButtons[i] = b;
        }
        if (maxWidth > 0) {
            Dimension dimension = new Dimension(maxWidth, maxHeight);
            for (int i = 0; i < buttons.length; i++) {
                if (buttons[i] instanceof Icon) continue;
                newButtons[i].setPreferredSize(dimension);
                newButtons[i].setMinimumSize(dimension);
            }
        }
    }


    // Override BasicOptionPaneUI

    /**
     * {@inheritDoc}
     */
    @Override
    protected Container createButtonArea() {
        Object[] buttons = getButtons();
        prepareButtons(buttons);

        JPanel bottom;
        if (this.okIndex >= 0) { // Confirm dialog
            bottom = new MigPanel(new MigLayout("insets dialog"));
        } else { // Multi-line choice dialog
            bottom = new MigPanel(new MigLayout("wrap "
                    + getColumns(buttons.length)));
        }
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

        if (0 <= initialIndex && initialIndex < buttons.length) {
            JButton b = newButtons[initialIndex];
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
                container.add(newButtons[cancelIndex], "tag cancel");
            }
            container.add(newButtons[okIndex], "tag ok");
            for (int i = 0; i < buttons.length; i++) {
                if (i == okIndex || i == cancelIndex) continue;
                container.add(newButtons[i]);
            }
        } else {
            // This must be a choice dialog.  The wrap argument to the
            // MigLayout constructor will do the work for us.
            for (int i = 0; i < buttons.length; i++) {
                if (i == cancelIndex) continue;
                container.add(newButtons[i]);
            }
            if (cancelIndex >= 0) {
                container.add(newButtons[cancelIndex],
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
