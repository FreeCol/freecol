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

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalFileChooserUI;


/**
 * UI for the file chooser.
 */
public class FreeColFileChooserUI extends MetalFileChooserUI {

    public static ComponentUI createUI(JComponent c) {
        return new FreeColFileChooserUI((JFileChooser) c);
    }


    public FreeColFileChooserUI(JFileChooser filechooser) {
        super(filechooser);
    }

    @Override
    protected void addControlButtons() {
        JPanel buttonPanel = getButtonPanel();
        Component[] buttons = buttonPanel.getComponents();
        buttonPanel.removeAll();
        for (int i=buttons.length-1; i>=0; i--) {
            buttonPanel.add(buttons[i]);
        }
        super.addControlButtons();
    }
}
