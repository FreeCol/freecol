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
import java.awt.event.ActionListener;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.FoundingFather;

/**
 * This panel displays the different founding fathers the player can work
 * towards recruiting.
 *
 * @see FoundingFather
 */
public final class ChooseFoundingFatherDialog extends FreeColDialog<FoundingFather> implements ActionListener {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ChooseFoundingFatherDialog.class.getName());

    private final JTabbedPane tb;

    private final List<FoundingFather> possibleFathers;


    /**
     * The constructor that will add the items to this panel.
     *
     * @param parent The parent of this panel.
     * @param possibleFoundingFathers The founding fathers which can
     *        be selected. The length of the array is the same as the
     *        number of <code>FoundingFather</code> categories and the
     *        values identifies a <code>FoundingFather</code> to be
     *        picked in each of those categories.
     */
    public ChooseFoundingFatherDialog(Canvas parent, List<FoundingFather> possibleFoundingFathers) {
        super(parent);
        this.possibleFathers = possibleFoundingFathers;
        setLayout(new MigLayout("wrap 1", "align center"));

        setFocusCycleRoot(false);

        tb = new JTabbedPane(JTabbedPane.TOP);

        FatherDetailPanel details = new FatherDetailPanel(new ColopediaPanel(getCanvas()));
        boolean hasSelectedTab = false;
        for (int index = 0; index < possibleFoundingFathers.size(); index++) {
            FoundingFather father = possibleFoundingFathers.get(index);
            JPanel panel = new JPanel();
            details.buildDetail(father, panel);
            panel.validate();
            tb.addTab(Messages.message(father.getTypeKey()), panel);
        }
        tb.setSelectedIndex(0);

        JButton helpButton =
            new JButton(getFreeColClient().getActionManager()
                        .getFreeColAction("colopediaAction.FATHERS"));
        helpButton.setText(Messages.message("help"));

        add(getDefaultHeader(Messages.message("foundingFatherDialog.nominate")));
        add(tb, "width 100%");
        add(okButton, "newline 20, split 2, tag ok");
        add(helpButton, "tag help");

        setSize(tb.getPreferredSize());
    }

    @Override
    public void requestFocus() {
        tb.requestFocus();
    }

    /**
     * This function analyses an event and calls the right methods to take care
     * of the user's requests.
     *
     * @param event The incoming ActionEvent.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        setResponse(possibleFathers.get(tb.getSelectedIndex()));
    }


}
