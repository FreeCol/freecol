/**
 *  Copyright (C) 2002-2014   The FreeCol Team
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
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.PanelUI;
import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.plaf.FreeColSelectedPanelUI;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit;


/**
 * Centers the map on a known settlement or colony.  Pressing ENTER
 * opens a panel if appropriate.
 */
public final class EndTurnDialog extends FreeColConfirmDialog {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(EndTurnDialog.class.getName());

    /**
     * We need to wrap the Unit class in order to make the JList
     * support keystroke navigation.  JList.getNextMatch uses the
     * toString() method, but the toString() method of FreeCol objects
     * provides debugging information rather than a searchable name.
     */
    private static class UnitWrapper {

        public Unit unit;
        public String name;
        public String location;


        public UnitWrapper(Unit unit) {
            this.unit = unit;
            this.name = unit.getDescription(Unit.UnitLabelType.NATIONAL);
            this.location = Messages.message(unit.getLocation()
                .getLocationLabelFor(unit.getOwner()));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return name;
        }
    }

    private class UnitCellRenderer implements ListCellRenderer<UnitWrapper> {

        private JPanel itemPanel = new MigPanel();
        private JPanel selectedPanel = new MigPanel();
        private JLabel imageLabel = new JLabel();
        private JLabel nameLabel = new JLabel();
        private JLabel locationLabel = new JLabel();


        public UnitCellRenderer() {
            itemPanel.setOpaque(false);
            itemPanel.setLayout(new MigLayout("", "[60]"));
            selectedPanel.setOpaque(false);
            selectedPanel.setLayout(new MigLayout("", "[60]"));
            selectedPanel.setUI((PanelUI)FreeColSelectedPanelUI.createUI(selectedPanel));
            locationLabel.setFont(locationLabel.getFont()
                .deriveFont(Font.ITALIC));
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public Component getListCellRendererComponent(JList<? extends UnitWrapper> list,
                                                      UnitWrapper value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            imageLabel.setIcon(getImageLibrary().getUnitImageIcon(value.unit, 0.5));
            nameLabel.setText(value.name);
            locationLabel.setText(value.location);

            JPanel panel = (isSelected) ? selectedPanel : itemPanel;
            panel.removeAll();
            panel.add(imageLabel, "center");
            panel.add(nameLabel, "split 2, flowy");
            panel.add(locationLabel);
            return panel;
        }
    }


    /** The list of units to display. */
    private JList<UnitWrapper> unitList;


    /**
     * The constructor to use.
     */
    public EndTurnDialog(FreeColClient freeColClient, List<Unit> units) {
        super(freeColClient);

        final Player player = getMyPlayer();

        String endName = Messages.message("endTurnDialog.name");
        JLabel header = GUI.getDefaultHeader(endName);
        //header.setFont(GUI.SMALL_HEADER_FONT);

        StringTemplate template
            = StringTemplate.template("endTurnDialog.areYouSure")
                .addAmount("%number%", units.size());
        JTextArea text = GUI.getDefaultTextArea(template);

        DefaultListModel<UnitWrapper> model = new DefaultListModel<UnitWrapper>();
        for (Unit unit : units) {
            model.addElement(new UnitWrapper(unit));
        }

        this.unitList = new JList<UnitWrapper>(model);
        this.unitList.setCellRenderer(new UnitCellRenderer());
        this.unitList.setFixedCellHeight(48);
        this.unitList.getInputMap().put(KeyStroke.getKeyStroke("ENTER"),
                                        "select");
        this.unitList.getActionMap().put("select", new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    selectUnit();
                }
            });
        this.unitList.getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"),
                                        "quit");
        this.unitList.getActionMap().put("quit", new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    EndTurnDialog.this.setValue(options.get(1));
                }
            });
        this.unitList.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    if (e.getValueIsAdjusting()) return;
                    selectUnit();
                }
            });
        JScrollPane listScroller = new JScrollPane(this.unitList);

        MigPanel panel = new MigPanel(new MigLayout("wrap 1, fill",
                                                    "[400, align center]"));
        panel.add(header);
        panel.add(text, "newline 20");
        panel.add(listScroller, "newline 10");
        panel.setSize(panel.getPreferredSize());

        ImageIcon icon = getImageLibrary().getImageIcon(player, false);
        initializeConfirmDialog(false, panel, icon, "ok", "cancel");
    }

    /**
     * Select the current unit in the list.
     */
    private void selectUnit() {
        UnitWrapper wrapper = this.unitList.getSelectedValue();
        if (wrapper != null && wrapper.unit != null) {
            if (wrapper.unit.isInEurope()) {
                getGUI().showEuropePanel();
            } else {
                getGUI().setActiveUnit(wrapper.unit);
                if (wrapper.unit.getColony() != null) {
                    getGUI().showColonyPanel(wrapper.unit.getColony(),
                                             wrapper.unit);
                } else if (wrapper.unit.hasTile()) {
                    getGUI().setFocus(wrapper.unit.getTile());
                }
            }
        }
    }
}
