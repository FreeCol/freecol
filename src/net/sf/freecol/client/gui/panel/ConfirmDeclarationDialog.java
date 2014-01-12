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

package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.MigPanel;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;


/**
 * A dialog used to confirm the declaration of independence.
 */
public class ConfirmDeclarationDialog extends FreeColDialog<List<String>> {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ConfirmDeclarationDialog.class.getName());

    private final JTextField nationField;

    private final JTextField countryField;


    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public ConfirmDeclarationDialog(FreeColClient freeColClient) {
        super(freeColClient);

        final Player player = freeColClient.getMyPlayer();

        // Create the main panel
        MigPanel panel = new MigPanel(new MigLayout("wrap 1", "", ""));

        StringTemplate sure
            = StringTemplate.template("declareIndependence.areYouSure.text")
                .add("%monarch%", player.getMonarch().getNameKey());

        StringTemplate country
            = StringTemplate.template("declareIndependence.defaultCountry")
                .add("%nation%", player.getNewLandName());
        countryField = new JTextField(Messages.message(country), 20);
        String cPrompt = Messages.message("declareIndependence.enterCountry");

        StringTemplate nation
            = StringTemplate.template("declareIndependence.defaultNation")
                .addStringTemplate("%nation%", player.getNationName());
        nationField = new JTextField(Messages.message(nation), 20);
        String nPrompt = Messages.message("declareIndependence.enterNation");

        panel.add(GUI.getDefaultTextArea(Messages.message(sure)));
        panel.add(GUI.getDefaultTextArea(cPrompt));
        panel.add(countryField);
        panel.add(GUI.getDefaultTextArea(nPrompt));
        panel.add(nationField);
        panel.setPreferredSize(panel.getPreferredSize()); // Prevent NPE

        // Use the coat of arms image icon.  Is there something better?
        ImageIcon icon = getGUI().getImageLibrary().getImageIcon(player, true);

        final List<String> fake = null;
        List<ChoiceItem<List<String>>> c = choices();
        c.add(new ChoiceItem<List<String>>(Messages.message("declareIndependence.areYouSure.yes"),
                fake).okOption());
        c.add(new ChoiceItem<List<String>>(Messages.message("declareIndependence.areYouSure.no"),
                fake).cancelOption().defaultOption());
        initialize(DialogType.QUESTION, true, panel, icon, c);
    }


    /**
     * {@inheritDoc}
     */
    public List<String> getResponse() {
        Object value = getValue();
        if (options.get(0).equals(value)) {
            List<String> result = new ArrayList<String>();
            // Sanitize user input, used in save file name
            result.add(nationField.getText().replaceAll("[^\\s\\w]", ""));
            result.add(countryField.getText());
            return result;
        }
        return null;
    }
}
