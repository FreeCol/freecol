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
import net.sf.freecol.common.model.Europe;
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

    private class LocationRenderer extends JLabel implements ListCellRenderer {

        public Component getListCellRendererComponent(JList list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {

            ChoiceItem choice = (ChoiceItem) value;
            if (choice.getObject() instanceof Europe) {
                Europe europe = (Europe) choice.getObject();
                setText(choice.toString());
                setIcon(new ImageIcon(getLibrary().getCoatOfArmsImage(europe.getOwner().getNation())
                                      .getScaledInstance(-1, 48, Image.SCALE_SMOOTH)));
            } else if (choice.getObject() instanceof Settlement) {
                Settlement settlement = (Settlement) choice.getObject();
                setText(choice.toString());
                setIcon(new ImageIcon(getLibrary().getSettlementImage(settlement)
                                      .getScaledInstance(64, -1, Image.SCALE_SMOOTH)));
            }
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

