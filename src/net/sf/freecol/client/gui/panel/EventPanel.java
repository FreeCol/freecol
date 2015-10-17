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

import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * This panel is displayed when an imporantant event in the game has happened.
 */
public final class EventPanel extends FreeColPanel {

    private static final Logger logger = Logger.getLogger(EventPanel.class.getName());


    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param header The title.
     * @param key A resource key for the image to display.
     * @param footer Optional footer text.
     */
    public EventPanel(FreeColClient freeColClient, String header, String key,
                      String footer) {
        super(freeColClient, new MigLayout("wrap 1", "[center]", "[]20"));

        JLabel headerLabel = new JLabel(header);
        headerLabel.setFont(FontLibrary.createCompatibleFont(header,
            FontLibrary.FontType.HEADER, FontLibrary.FontSize.MEDIUM));

        JLabel imageLabel
            = new JLabel(new ImageIcon(ResourceManager.getImage(key)));

        JLabel footerLabel = (footer == null) ? null : new JLabel(footer);

        add(headerLabel);
        add(imageLabel);
        if (footerLabel != null) add(footerLabel);
        add(okButton, "tag ok");

        setSize(getPreferredSize());
    }
}
