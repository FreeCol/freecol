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

package net.sf.freecol.client.gui.panel;

import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * This dialog is displayed to a player who has won the game.
 */
public final class VictoryDialog extends FreeColConfirmDialog {

    /**
     * Create a Victory dialog.
     * 
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param frame The owner frame.
     */
    public VictoryDialog(FreeColClient freeColClient, JFrame frame) {
        super(freeColClient, frame);

        MigPanel panel = new MigPanel(new MigLayout("wrap 1", "", ""));
        panel.add(Utility.localizedHeader(Messages.message("victory.text"),
                                          false),
                  "align center, wrap 20");
        Image image = ResourceManager.getImage("image.flavor.Victory");
        panel.add(new JLabel(new ImageIcon(image)),
                  "align center");

        initializeConfirmDialog(frame, false, panel, null,
                                "victory.yes", "victory.continue");
    }
}
