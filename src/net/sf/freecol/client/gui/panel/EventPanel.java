/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import net.sf.freecol.client.gui.Canvas.EventType;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * This panel is displayed when an imporantant event in the game has happened.
 */
public final class EventPanel extends FreeColDialog<Boolean> {

    private static final Logger logger = Logger.getLogger(EventPanel.class.getName());


    /**
     * The constructor that will add the items to this panel.
     * @param freeColClient 
     *
     * @param parent The parent of this panel.
     * @param type The type of this panel.
     */
    public EventPanel(FreeColClient freeColClient, GUI gui, EventType type) {

        super(freeColClient, gui);

        setLayout(new MigLayout("wrap 1", "[center]", "[]20"));

        // TODO: simplify this -- event should contain the necessary
        // information; add it to enum or upgrade to class
        String text = null;
        String image = null;
        switch(type) {
        case FIRST_LANDING:
            image = "EventImage.firstLanding";
            text = Messages.message(StringTemplate.template("event.firstLanding")
                                    .addName("%name%", Messages.getNewLandName(getMyPlayer())));
            break;
        case MEETING_EUROPEANS:
            image = "EventImage.meetingEuropeans";
            text = Messages.message("event.meetingEuropeans");
            break;
        case MEETING_NATIVES:
            image = "EventImage.meetingNatives";
            text = Messages.message("event.meetingNatives");
            break;
        case MEETING_AZTEC:
            image = "EventImage.meetingAztec";
            text = Messages.message("event.meetingAztec");
            break;
        case MEETING_INCA:
            image = "EventImage.meetingInca";
            text = Messages.message("event.meetingInca");
            break;
        case DISCOVER_PACIFIC:
            image = "EventImage.discoverPacific";
            text = Messages.message("model.region.pacific.discover");
            break;
        default:
            setResponse(Boolean.FALSE);
        }

        JLabel header = new JLabel(text);
        header.setFont(mediumHeaderFont);

        JLabel imageLabel = new JLabel(new ImageIcon(ResourceManager.getImage(image)));

        add(header);
        add(imageLabel);
        add(okButton, "tag ok");

        setSize(getPreferredSize());

    }

}
