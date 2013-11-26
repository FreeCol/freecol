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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.FoundingFather;


/**
 * This panel displays the different founding fathers the player can work
 * towards recruiting.
 *
 * @see FoundingFather
 */
public final class ChooseFoundingFatherDialog
    extends FreeColDialog<FoundingFather> {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ChooseFoundingFatherDialog.class.getName());

    private final JTabbedPane tb;

    private final List<FoundingFather> possibleFathers;


    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param possibleFoundingFathers The founding fathers which can
     *     be selected.  The length of the array is the same as the
     *     number of <code>FoundingFather</code> categories and the
     *     values identifies a <code>FoundingFather</code> to be
     *     picked in each of those categories.
     */
    public ChooseFoundingFatherDialog(FreeColClient freeColClient,
        List<FoundingFather> possibleFoundingFathers) {
        super(freeColClient);

        this.possibleFathers = possibleFoundingFathers;
        this.tb = new JTabbedPane(JTabbedPane.TOP);
        setFocusCycleRoot(false);

        String nominate = Messages.message("foundingFatherDialog.nominate");
        JLabel header = GUI.getDefaultHeader(nominate);

        JButton helpButton = new JButton(freeColClient.getActionManager()
            .getFreeColAction("colopediaAction.FATHERS"));
        helpButton.setText(Messages.message("help"));

        FatherDetailPanel details = new FatherDetailPanel(freeColClient,
            new ColopediaPanel(freeColClient));
        for (int index = 0; index < possibleFoundingFathers.size(); index++) {
            FoundingFather father = possibleFoundingFathers.get(index);
            JPanel jp = new MigPanel();
            details.buildDetail(father, jp);
            jp.validate();
            tb.addTab(Messages.message(father.getTypeKey()), jp);
        }
        tb.setSelectedIndex(0);

        MigPanel panel = new MigPanel(new MigLayout("wrap 1", "align center"));
        panel.add(header);
        panel.add(helpButton, "tag help");
        panel.add(tb, "width 100%");
        panel.setPreferredSize(panel.getPreferredSize());

        List<ChoiceItem<FoundingFather>> c = choices();
        c.add(new ChoiceItem<FoundingFather>(Messages.message("ok"),
                (FoundingFather)null).okOption().defaultOption());
        initialize(DialogType.QUESTION, false, panel, null, c);
    }


    /**
     * {@inheritDoc}
     */
    public FoundingFather getResponse() {
        Object value = getValue();
        if (options.get(0).equals(value)) {
            return possibleFathers.get(tb.getSelectedIndex());
        }
        return null;
    }


    // Override Component

    @Override
    public void requestFocus() {
        tb.requestFocus();
    }
}
