/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JSeparator;
import javax.swing.ListCellRenderer;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.Specification;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.server.NationOptions;
import net.sf.freecol.server.NationOptions.Advantages;

import net.miginfocom.swing.MigLayout;

/**
 * Dialog for changing the {@link net.sf.freecol.server.NationOptions}.
 */
public final class NationOptionsDialog extends FreeColDialog<NationOptions> implements ActionListener {

    private static final Logger logger = Logger.getLogger(NationOptionsDialog.class.getName());

    private final JButton ok = new JButton(Messages.message("ok"));
    private final JList europeanPlayers;
    private final JList nativePlayers;
    private final JCheckBox selectColors;
    private final JComboBox nationalAdvantages;

    /**
     * The constructor that will add the items to this panel.
     * 
     * @param parent The parent of this panel.
     */
    public NationOptionsDialog(Canvas parent) {
        super(parent);

        ok.setActionCommand("ok");
        ok.addActionListener(this);
        enterPressesWhenFocused(ok);

        europeanPlayers = new JList(new Vector<Nation>(Specification.getSpecification().getEuropeanNations()));
        europeanPlayers.setSelectionInterval(0, 3);

        nativePlayers = new JList(new Vector<Nation>(Specification.getSpecification().getIndianNations()));
        nativePlayers.setSelectionInterval(0, Specification.getSpecification().getIndianNations().size() - 1);

        selectColors = new JCheckBox(Messages.message("playerOptions.selectColors"));
        selectColors.setSelected(true);
        selectColors.setEnabled(false);

        Advantages[] choices = new Advantages[] {
            Advantages.SELECTABLE,
            Advantages.FIXED,
            Advantages.NONE
        };
        nationalAdvantages = new JComboBox(choices);
        nationalAdvantages.setRenderer(new AdvantageRenderer());

        setLayout(new MigLayout("wrap 3", "", ""));

        add(getDefaultHeader(Messages.message("playerOptions.selectPlayers")),
            "span, align center");
        add(new JLabel(Messages.message("playerOptions.europeanPlayers")),
            "wrap");
        add(new JSeparator(JSeparator.HORIZONTAL), "span, growx");
        add(europeanPlayers, "span 1 2");
        add(new JLabel(Messages.message("playerOptions.nationalAdvantages")));
        add(nationalAdvantages, "wrap");
        add(selectColors, "wrap");

        add(new JLabel(Messages.message("playerOptions.nativePlayers")),
            "newline 20, wrap");
        add(new JSeparator(JSeparator.HORIZONTAL), "span, growx");
        add(nativePlayers);
        add(ok, "newline 20, span, align center, tag ok");

        setSize(getPreferredSize());
    }

    public void requestFocus() {
        ok.requestFocus();
    }

    /**
     * Saves the NationOptions as response.
     * 
     * @param event The incoming ActionEvent.
     */
    public void actionPerformed(ActionEvent event) {
        NationOptions response = new NationOptions();
        response.setSelectColors(selectColors.isSelected());
        response.setNationalAdvantages((Advantages) nationalAdvantages.getSelectedItem());
        List<Nation> europeanNations = new ArrayList<Nation>();
        for (Object o : europeanPlayers.getSelectedValues()) {
            if (o instanceof Nation) {
                europeanNations.add((Nation) o);
            }
        }
        response.setEuropeanNations(europeanNations);
        List<Nation> nativeNations = new ArrayList<Nation>();
        for (Object o : nativePlayers.getSelectedValues()) {
            if (o instanceof Nation) {
                nativeNations.add((Nation) o);
            }
        }
        response.setNativeNations(nativeNations);
        setResponse(response);
    }


    class AdvantageRenderer extends JLabel implements ListCellRenderer {

        public Component getListCellRendererComponent(JList list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            setText(Messages.message("playerOptions." + value.toString()));
            return this;
        }
    }

}
