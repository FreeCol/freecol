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

package net.sf.freecol.client.gui.dialog;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ChoiceItem;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.panel.*;


/**
 * A panel for adjusting the river style.
 *
 * This panel is only used when running in
 * {@link net.sf.freecol.client.FreeColClient#isMapEditor()} map editor mode.
 */
public final class RiverStyleDialog extends FreeColChoiceDialog<String> {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(RiverStyleDialog.class.getName());


    /**
     * Creates a dialog to choose a river style.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param frame The owner frame.
     * @param styles The river styles a choice is made from.
     */
    public RiverStyleDialog(FreeColClient freeColClient, JFrame frame,
                            List<String> styles) {
        super(freeColClient, frame);

        JPanel panel = new JPanel();
        panel.add(Utility.localizedHeader("riverStyleDialog.text", false),
                  "span, align center");

        List<ChoiceItem<String>> c = FreeColDialog.choices();
        for (String style : styles) {
            c.add(new ChoiceItem<>(null, style)
                .setIcon(new ImageIcon(getImageLibrary().getSmallerRiverImage(style))));
        }

        initializeChoiceDialog(frame, true, panel, null, "cancel", c);
    }
}
