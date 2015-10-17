/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Graphics;
import java.awt.Graphics2D;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.TypeCountMap;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;


/**
 * This panel displays the Labour Report.
 */
public final class ReportLabourPanel extends ReportPanel {

    /** An individual unit type panel. */
    private class LabourUnitPanel extends JPanel {

        public boolean selected;
        public final UnitType unitType;


        public LabourUnitPanel(UnitType unitType, int count) {
            this.unitType = unitType;
            setOpaque(false);
            setLayout(new MigLayout("wrap 2", "[60, right][left]"));
            add(new JLabel(new ImageIcon(getImageLibrary().getSmallUnitImage(
                    unitType, (count == 0)))),
                "spany 2");
            add(new JLabel(Messages.getName(unitType)));
            add(new JLabel(Integer.toString(count)));
        }


        @Override
        public void paint(Graphics g) {
            if (selected) {
                Graphics2D g2d = (Graphics2D) g;
                Composite oldComposite = g2d.getComposite();
                Color oldColor = g2d.getColor();
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));
                g2d.setColor(Color.BLACK);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setComposite(oldComposite);
                g2d.setColor(oldColor);
            }
            super.paint(g);
        }
    }

    /** A renderer for the labour unit panels. */
    private static class LabourUnitPanelRenderer
        implements ListCellRenderer<LabourUnitPanel> {

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getListCellRendererComponent(JList<? extends LabourUnitPanel> list,
                                                      LabourUnitPanel value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            value.selected = isSelected;
            return value;
        }
    }


    /** The map of unit type to location and count. */
    private final Map<UnitType, Map<Location, Integer>> data;

    /** A map of count by unit type. */
    private final TypeCountMap<UnitType> unitCount;

    /** The player colonies. */
    private final List<Colony> colonies;

    /** A list of panels for the unit types. */
    private JList<LabourUnitPanel> panelList = null;


    /**
     * The constructor that will add the items to this panel.
     */
    public ReportLabourPanel(FreeColClient freeColClient) {
        super(freeColClient, "reportLabourAction");

        this.data = new HashMap<>();
        this.unitCount = new TypeCountMap<>();
        for (Unit unit : getMyPlayer().getUnits()) {
            UnitType type = unit.getType();
            this.unitCount.incrementCount(type, 1);
            Map<Location, Integer> unitMap = this.data.get(type);
            if (unitMap == null) {
                unitMap = new HashMap<>();
                this.data.put(type, unitMap);
            }

            Location location = unit.getLocation();
            if (location == null) {
                logger.warning("Unit has null location: " + unit);
            } else if (location.getSettlement() != null) {
                location = location.getSettlement();
            } else if (unit.isInEurope()) {
                location = getMyPlayer().getEurope();
            } else if (location.getTile() != null) {
                location = location.getTile();
            }
            Integer count = unitMap.get(location);
            if (count == null) {
                unitMap.put(location, 1);
            } else {
                unitMap.put(location, count + 1);
            }
        }

        this.colonies = freeColClient.getMySortedColonies();

        DefaultListModel<LabourUnitPanel> model
            = new DefaultListModel<>();
        for (UnitType unitType : getSpecification().getUnitTypeList()) {
            if (unitType.isPerson() && unitType.isAvailableTo(getMyPlayer())) {
                int count = this.unitCount.getCount(unitType);
                model.addElement(new LabourUnitPanel(unitType, count));
            }
        }
        Action selectAction = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    showDetails();
                }
            };
        Action quitAction = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    getGUI().removeFromCanvas(ReportLabourPanel.this);
                }
            };

        // Add all the components
        this.panelList = new JList<>(model);
        this.panelList.getInputMap()
            .put(KeyStroke.getKeyStroke("ENTER"), "select");
        this.panelList.getActionMap().put("select", selectAction);
        this.panelList.getInputMap()
            .put(KeyStroke.getKeyStroke("ESCAPE"), "quit");
        this.panelList.getActionMap().put("quit", quitAction);
        this.panelList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        showDetails();
                    }
                }
            });
        this.panelList.setOpaque(false);
        this.panelList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.panelList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        this.panelList.setVisibleRowCount(-1);
        this.panelList.setCellRenderer(new LabourUnitPanelRenderer());

        this.scrollPane.setViewportView(this.panelList);
    }

    private void showDetails() {
        UnitType unitType = panelList.getSelectedValue()
            .unitType;
        getGUI().showReportLabourDetailPanel(unitType, this.data,
                                             this.unitCount, this.colonies);
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        final String command = ae.getActionCommand();
        if (OK.equals(command)) {
            super.actionPerformed(ae);
        } else {
            UnitType unitType = getSpecification().getUnitType(command);
            getGUI().showReportLabourDetailPanel(unitType, this.data,
                this.unitCount, this.colonies);
        }
    }
}
