/**
 *  Copyright (C) 2002-2021   The FreeCol Team
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


package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JColorChooser;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;


/**
 * This class represents a panel that holds a JColorChooser and OK
 * and cancel buttons.
 */
public final class ColorChooserPanel extends FreeColPanel {

    private final JColorChooser colorChooser;


    /**
     * The constructor to use.
     *
     * @param freeColClient The top level component that holds all
     *     other components.
     * @param l The ActionListener for the OK and cancel buttons.
     */
    public ColorChooserPanel(FreeColClient freeColClient, ActionListener l) {
        super(freeColClient, null, new MigLayout("", "", ""));

        this.colorChooser = new JColorChooser();
        add(this.colorChooser);

        add(okButton, "newline 20, split 2, tag ok");
        okButton.addActionListener(l);

        JButton cancelButton = Utility.localizedButton("cancel");
        add(cancelButton, "tag cancel");
        cancelButton.setActionCommand(CANCEL);
        cancelButton.addActionListener(l);
        setCancelComponent(cancelButton);

        setOpaque(true);
        setSize(getPreferredSize());
    }


    public Color getColor() {
        return this.colorChooser.getColor();
    }


    // Override Component

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeNotify() {
        super.removeNotify();

        removeAll();
    }
}
