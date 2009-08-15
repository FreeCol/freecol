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
import java.awt.Dimension;
import java.awt.Image;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Player;

import net.miginfocom.swing.MigLayout;


/**
 * Centers the map on a known settlement or colony.
 */
public final class FindSettlementDialog extends FreeColDialog implements ListSelectionListener {

    private static final Logger logger = Logger.getLogger(FindSettlementDialog.class.getName());

    private List<Settlement> knownSettlements = new ArrayList<Settlement>();

    private JList settlementList;


    private static Comparator<Settlement> settlementComparator = new Comparator<Settlement>() {
        public int compare(Settlement s1, Settlement s2) {
            return s1.getName().compareTo(s2.getName());
        }
    };


    /**
     * The constructor to use.
     */
    public FindSettlementDialog(Canvas parent) {
        super(parent);

        for (Player player : getGame().getPlayers()) {
            knownSettlements.addAll(player.getSettlements());
        }

        Collections.sort(knownSettlements, settlementComparator);

        MigLayout layout = new MigLayout("wrap 1, fill", "[align center]", "[]30[]30[]");
        setLayout(layout);

        JLabel header = new JLabel(Messages.message("menuBar.view.findSettlement"));
        header.setFont(smallHeaderFont);
        add(header);

        settlementList = new JList(knownSettlements.toArray(new Settlement[knownSettlements.size()]));
        settlementList.setCellRenderer(new SettlementRenderer());
        settlementList.setFixedCellHeight(48);
        JScrollPane listScroller = new JScrollPane(settlementList);
        listScroller.setPreferredSize(new Dimension(250, 250));
        settlementList.addListSelectionListener(this);
        add(listScroller, "growx, growy");

        add(okButton, "tag ok");

        setSize(getPreferredSize());
    }

    /**
     * This function analyses an event and calls the right methods to take care
     * of the user's requests.
     * 
     * @param event The incoming ActionEvent.
     */
    public void valueChanged(ListSelectionEvent e) {
        Settlement settlement = (Settlement) settlementList.getSelectedValue();
        getCanvas().getGUI().setFocus(settlement.getTile().getPosition());
    }

    private class SettlementRenderer extends JLabel implements ListCellRenderer {

        public Component getListCellRendererComponent(JList list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            Settlement settlement = (Settlement) value;
            setText(settlement.getName()
                    + (settlement.isCapital() ? "* (" : " (")
                    + settlement.getOwner().getNationAsString() + ")");
            setIcon(new ImageIcon(getLibrary().getSettlementImage(settlement)
                                  .getScaledInstance(64, -1, Image.SCALE_SMOOTH)));
            if (isSelected) {
                setOpaque(true);
                setBackground(LIST_SELECT_COLOR);
            } else {
                setOpaque(false);
            }
            return this;
        }
    }

} 

