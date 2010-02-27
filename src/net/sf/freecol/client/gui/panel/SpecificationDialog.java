/**
 *  Copyright (C) 2002-2010  The FreeCol Team
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
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.util.logging.Logger;

import javax.swing.JComboBox;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Specification;

import net.miginfocom.swing.MigLayout;

/**
 * A panel for selecting the specification and difficulty.
 */
public final class SpecificationDialog extends FreeColDialog<Boolean> implements ActionListener {

    private static final Logger logger = Logger.getLogger(SpecificationDialog.class.getName());

    private final String[] files =
        new String[] {
        "data/freecol/specification.xml",
        "data/classic/specification.xml"
    };

    private final JComboBox specificationBox =
        new JComboBox(new String[] { "FreeCol", "Colonization" });

    private final JComboBox difficultyBox =
        new JComboBox(new String[] {
                Messages.message("model.difficulty.veryEasy"),
                Messages.message("model.difficulty.easy"),
                Messages.message("model.difficulty.medium"),
                Messages.message("model.difficulty.hard"),
                Messages.message("model.difficulty.veryHard")
            });


    /**
     * The constructor that will add the items to this panel.
     * 
     * @param parent The parent of this panel.
     */
    public SpecificationDialog(Canvas parent) {
        super(parent);
        setLayout(new MigLayout("wrap 1", "fill"));
        add(specificationBox);
        add(difficultyBox);
        add(okButton, "tag ok");

        setSize(getPreferredSize());
    }

    /**
     * This function analyses an event and calls the right methods to take care
     * of the user's requests.
     * 
     * @param event The incoming ActionEvent.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (OK.equals(command)) {
            try {
                Specification.createSpecification(new FileInputStream(files[specificationBox.getSelectedIndex()]));
                Specification.getSpecification().applyDifficultyLevel(difficultyBox.getSelectedIndex());
                setResponse(true);
            } catch(Exception e) {
                logger.warning(e.toString());
            }
        }
    }


}