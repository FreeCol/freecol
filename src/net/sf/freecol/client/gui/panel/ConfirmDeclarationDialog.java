/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

import javax.swing.JTextField;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;

import net.miginfocom.swing.MigLayout;


public class ConfirmDeclarationDialog extends FreeColDialog<List<String>> {

    private JTextField nationField;
    private JTextField countryField;

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ConfirmDeclarationDialog.class.getName());

    /**
     * The constructor that will add the items to this panel.
     *
     * @param parent The parent of this panel.
     */
    public ConfirmDeclarationDialog(final Canvas parent) {
        super(parent);
        Player player = getMyPlayer();

        StringTemplate nation
            = StringTemplate.template("declareIndependence.defaultNation")
                .addStringTemplate("%nation%", player.getNationName());
        nationField = new JTextField(Messages.message(nation), 20);
        StringTemplate country
            = StringTemplate.template("declareIndependence.defaultCountry")
                .add("%nation%", player.getNewLandName());
        countryField = new JTextField(Messages.message(country), 20);

        okButton.setText(Messages.message("declareIndependence.areYouSure.yes"));
        cancelButton.setText(Messages.message("declareIndependence.areYouSure.no"));

        setLayout(new MigLayout("wrap 1", "", ""));

        StringTemplate sure
            = StringTemplate.template("declareIndependence.areYouSure.text")
                .add("%monarch%", player.getMonarch().getNameKey());
        add(getDefaultTextArea(Messages.message(sure)));
        add(getDefaultTextArea(Messages.message("declareIndependence.enterCountry")));
        add(countryField);
        add(getDefaultTextArea(Messages.message("declareIndependence.enterNation")));
        add(nationField);
        add(okButton, "newline 20, split 2, tag ok");
        add(cancelButton, "tag cancel");

    }


    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (OK.equals(command)) {
            List<String> result = new ArrayList<String>();
            // Sanitize user input, used in save file name
            result.add(nationField.getText().replaceAll("[^\\s\\w]", ""));
            result.add(countryField.getText());
            setResponse(result);
        } else {
            super.actionPerformed(event);
        }
    }

}