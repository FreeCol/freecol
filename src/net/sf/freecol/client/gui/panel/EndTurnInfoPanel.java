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

package net.sf.freecol.client.gui.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.action.EndTurnAction;
import net.sf.freecol.common.i18n.Messages;
import static net.sf.freecol.common.util.StringUtils.*;


/** Panel for ending the turn. */
public final class EndTurnInfoPanel extends FreeColPanel {

    /**
     * Build a new end panel.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public EndTurnInfoPanel(FreeColClient freeColClient) {
        super(freeColClient, null,
              new MigLayout("wrap 1, center", "[center]", ""));

        final ImageLibrary lib = getGUI().getTileImageLibrary();
        Font font = FontLibrary.createFont(FontLibrary.FontType.NORMAL,
            FontLibrary.FontSize.TINY, lib.getScaleFactor());

        String labelString = Messages.message("infoPanel.endTurn");
        for (String s : splitText(labelString, " /",
                                  getFontMetrics(font), 150)) {
            JLabel label = new JLabel(s);
            label.setFont(font);
            this.add(label);
        }

        JButton button = new JButton(getFreeColClient().getActionManager()
            .getFreeColAction(EndTurnAction.id));
        button.setFont(font);
        this.add(button);
        setBorder(null);
        setOpaque(false);
        setSize(getPreferredSize());
    }
}
