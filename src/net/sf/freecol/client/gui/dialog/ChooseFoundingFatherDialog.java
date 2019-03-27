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

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ChoiceItem;
import net.sf.freecol.client.gui.panel.*;
import net.sf.freecol.client.gui.panel.colopedia.*;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.FoundingFather;


/**
 * This panel displays the different Founding Fathers the player can work
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
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param frame The owner frame.
     * @param possibleFoundingFathers The {@code FoundingFather}s
     *     which can be selected. The length of the array is the same
     *     as the number of {@code FoundingFather} categories and the
     *     values identifies a {@code FoundingFather} to be picked in
     *     each of those categories.
     */
    public ChooseFoundingFatherDialog(FreeColClient freeColClient, JFrame frame,
            List<FoundingFather> possibleFoundingFathers) {
        super(freeColClient, frame);

        this.possibleFathers = possibleFoundingFathers;
        this.tb = new JTabbedPane(JTabbedPane.TOP);

        JButton helpButton = new JButton(freeColClient.getActionManager()
            .getFreeColAction("colopediaAction.fathers"));
        helpButton.setText(Messages.message("help"));

        FatherDetailPanel details = new FatherDetailPanel(freeColClient,
            new ColopediaPanel(freeColClient));
        for (FoundingFather father : possibleFoundingFathers) {
            JPanel jp = new MigPanel(new MigLayout());
            details.buildDetail(father, jp);
            jp.validate();
            tb.addTab(Messages.message(father.getTypeKey()), jp);
        }
        tb.setSelectedIndex(0);

        JPanel panel = new MigPanel(new MigLayout("wrap 1", "align center"));
        panel.add(Utility.localizedHeader("chooseFoundingFatherDialog.title", false));
        panel.add(helpButton, "tag help");
        panel.add(tb, "width 100%");

        List<ChoiceItem<FoundingFather>> c = choices();
        c.add(new ChoiceItem<>(Messages.message("ok"), (FoundingFather)null)
            .okOption().defaultOption());
        initializeDialog(frame, DialogType.QUESTION, false, panel, null, c);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public FoundingFather getResponse() {
        Object value = getValue();
        if (options.get(0).equals(value)) {
            return possibleFathers.get(tb.getSelectedIndex());
        }
        return null;
    }
}
