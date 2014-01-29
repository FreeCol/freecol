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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.Graphics;
import java.awt.Graphics2D;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.plaf.FreeColComboBoxRenderer;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.TypeCountMap;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.UnitType;


/**
 * This panel displays the Labour Report.
 */
public final class ReportLabourPanel extends ReportPanel {

    private Map<UnitType, Map<Location, Integer>> data;
    private TypeCountMap<UnitType> unitCount;
    private List<Colony> colonies;
    private JList list;

    /**
     * The constructor that will add the items to this panel.
     */
    public ReportLabourPanel(FreeColClient freeColClient) {
        super(freeColClient, Messages.message("reportLabourAction.name"));

        colonies = freeColClient.getMySortedColonies();
        gatherData();
        displayData();
    }

    public void gatherData() {
        data = new HashMap<UnitType, Map<Location, Integer>>();
        unitCount = new TypeCountMap<UnitType>();

        for (Unit unit : getMyPlayer().getUnits()) {
            UnitType type = unit.getType();
            unitCount.incrementCount(type, 1);
            Map<Location, Integer> unitMap = data.get(type);
            if (unitMap == null) {
                unitMap = new HashMap<Location, Integer>();
                data.put(type, unitMap);
            }

            Location location = unit.getLocation();
            if (location == null) {
                logger.warning("Unit has null location: " + unit.toString());
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
    }

    @SuppressWarnings("unchecked") // FIXME in Java7
    public void displayData() {

        DefaultListModel model = new DefaultListModel();

        for (UnitType unitType : getSpecification().getUnitTypeList()) {
            if (unitType.isPerson() && unitType.isAvailableTo(getMyPlayer())) {
                String roleId = Role.DEFAULT_ID;
                if (unitType.hasAbility(Ability.EXPERT_PIONEER)) {
                    roleId = "model.role.pioneer";
                } else if (unitType.hasAbility(Ability.EXPERT_MISSIONARY)) {
                    roleId = "model.role.missionary";
                }
                int count = unitCount.getCount(unitType);
                model.addElement(new UnitPanel(unitType, roleId, count));
            }
        }
        list = new JList(model);

        Action selectAction = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    showDetails();
                }
            };

        Action quitAction = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    getGUI().removeFromCanvas(ReportLabourPanel.this);
                }
            };

        list.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "select");
        list.getActionMap().put("select", selectAction);
        list.getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "quit");
        list.getActionMap().put("quit", quitAction);

        MouseListener mouseListener = new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        showDetails();
                    }
                }
            };
        list.addMouseListener(mouseListener);
        list.setOpaque(false);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        list.setVisibleRowCount(-1);
        list.setCellRenderer(new UnitRenderer());
        scrollPane.setViewportView(list);
    }

    public void showDetails() {
        UnitType unitType = ((UnitPanel) list.getSelectedValue()).unitType;
        getGUI().showReportLabourDetailPanel(unitType, data, unitCount, colonies);
    }

    private class UnitRenderer implements ListCellRenderer {

        public Component getListCellRendererComponent(JList list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            UnitPanel panel = (UnitPanel) value;
            panel.selected = isSelected;
            return panel;
        }

    }


    private class UnitPanel extends JPanel {

        boolean selected;
        UnitType unitType;

        public UnitPanel(UnitType unitType, String roleId, int count) {
            this.unitType = unitType;
            setOpaque(false);
            setLayout(new MigLayout("wrap 2", "[60, right][left]"));
            add(new JLabel(getLibrary().getUnitImageIcon(unitType, roleId,
                                                         (count == 0), 0.8)),
                "spany 2");
            add(new JLabel(Messages.getName(unitType)));
            add(new JLabel(Integer.toString(count)));
        }

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

    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (OK.equals(command)) {
            super.actionPerformed(event);
        } else {
            UnitType unitType = getSpecification().getUnitType(command);
            getGUI().showReportLabourDetailPanel(unitType, data, unitCount, colonies);

        }
    }
}
