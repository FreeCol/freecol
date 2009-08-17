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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.plaf.FreeColComboBoxRenderer;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Settlement;

import net.miginfocom.swing.MigLayout;


/**
 * Centers the map on a known settlement or colony.
 */
public final class SelectDestinationDialog extends FreeColDialog<Location> 
    implements ActionListener {

    private static final Logger logger = Logger.getLogger(SelectDestinationDialog.class.getName());

    private final JList destinationList;

    private static Comparator<ChoiceItem<Location>> destinationComparator =
        new Comparator<ChoiceItem<Location>>() {
        public int compare(ChoiceItem<Location> choice1, ChoiceItem<Location> choice2) {
            Location dest1 = choice1.getObject();
            Location dest2 = choice2.getObject();

            int score1 = 100;
            if (dest1 instanceof Europe) {
                score1 = 10;
            } else if (dest1 instanceof Colony) {
                score1 = 20;
            } else if (dest1 instanceof IndianSettlement) {
                score1 = 40;
            }
            int score2 = 100;
            if (dest2 instanceof Europe) {
                score2 = 10;
            } else if (dest2 instanceof Colony) {
                score2 = 20;
            } else if (dest2 instanceof IndianSettlement) {
                score2 = 40;
            }

            if (score1 == score2) {
                String name1 = "";
                if (dest1 instanceof Settlement) {
                    name1 = ((Settlement) dest1).getName();
                } else if (dest1 instanceof Europe) {
                    name1 = ((Europe) dest1).getName();
                }
                String name2 = "";
                if (dest2 instanceof Settlement) {
                    name2 = ((Settlement) dest2).getName();
                } else if (dest2 instanceof Europe) {
                    name2 = ((Europe) dest2).getName();
                }
                return name1.compareTo(name2);
            } else {
                return score1 - score2;
            }
        }
    };


    /**
     * The constructor to use.
     */
    public SelectDestinationDialog(Canvas parent, List<ChoiceItem<Location>> destinations) {
        super(parent);

        MigLayout layout = new MigLayout("wrap 1, fill", "[align center]", "[]30[]30[]");
        setLayout(layout);

        JLabel header = new JLabel(Messages.message("selectDestination.text"));
        header.setFont(smallHeaderFont);
        add(header);

        Collections.sort(destinations, destinationComparator);

        DefaultListModel model = new DefaultListModel();
        for (ChoiceItem<Location> location : destinations) {
            model.addElement(location);
        }
        destinationList = new JList(model);

        destinationList.setCellRenderer(new LocationRenderer());
        destinationList.setFixedCellHeight(48);
        JScrollPane listScroller = new JScrollPane(destinationList);
        listScroller.setPreferredSize(new Dimension(250, 250));

        add(listScroller, "growx, growy");

        cancelButton.setText(Messages.message("selectDestination.cancel"));
        cancelButton.addActionListener(this);
        okButton.addActionListener(this);

        add(okButton, "split 2, tag ok");
        add(cancelButton, "tag cancel");

        setSize(getPreferredSize());
    }

    /**
     * This function analyses an event and calls the right methods to take care
     * of the user's requests.
     * 
     * @param event The incoming ActionEvent.
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (OK.equals(command)) {
            ChoiceItem item = (ChoiceItem) destinationList.getSelectedValue();
            if (item != null) {
                setResponse((Location) item.getObject());
            }
        }
        getCanvas().remove(this);
    }

    private class LocationRenderer extends FreeColComboBoxRenderer {

        @Override
        public void setLabelValues(JLabel label, Object value) {

            ChoiceItem choice = (ChoiceItem) value;
            label.setText(choice.toString());
            if (choice.getObject() instanceof Europe) {
                Europe europe = (Europe) choice.getObject();
                label.setIcon(new ImageIcon(getLibrary().getCoatOfArmsImage(europe.getOwner().getNation())
                                            .getScaledInstance(-1, 48, Image.SCALE_SMOOTH)));
            } else if (choice.getObject() instanceof Settlement) {
                Settlement settlement = (Settlement) choice.getObject();
                label.setIcon(new ImageIcon(getLibrary().getSettlementImage(settlement)
                                            .getScaledInstance(64, -1, Image.SCALE_SMOOTH)));
            }
        }
    }
} 

