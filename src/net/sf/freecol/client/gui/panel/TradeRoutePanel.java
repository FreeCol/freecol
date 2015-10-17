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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Unit;


/**
 * Allows the user to edit trade routes.
 */
public final class TradeRoutePanel extends FreeColPanel {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(TradeRoutePanel.class.getName());

    /** Deassign command string constant. */
    private static final String DEASSIGN = "deassign";

    /** Compare trade routes by name. */
    private static final Comparator<TradeRoute> tradeRouteComparator
        = Comparator.comparing(TradeRoute::getName);

    /** The unit to assign/deassign trade routes for. */
    private final Unit unit;

    /** The list model describing the players trade routes. */
    private final DefaultListModel<TradeRoute> listModel
        = new DefaultListModel<>();

    /** The list of trade routes to display. */
    private JList<TradeRoute> tradeRoutes;

    /** A map of trade route to the number of units using it. */
    private final Map<TradeRoute, Integer> counts = new HashMap<>();

    /** The button to create a new trade route. */
    private JButton newRouteButton;
    /** The button to edit an existing trade route. */
    private JButton editRouteButton;
    /** The button to delete a trade route. */
    private JButton deleteRouteButton;
    /** The button to deassing the unit from a trade route. */
    private JButton deassignRouteButton;


    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param unit The optional <code>Unit</code> to operate on.
     */
    public TradeRoutePanel(FreeColClient freeColClient, Unit unit) {
        super(freeColClient, new MigLayout("wrap 2", "[fill][fill]"));

        final Player player = getMyPlayer();

        this.unit = unit;
        this.tradeRoutes = new JList<>(listModel);
        this.tradeRoutes.addListSelectionListener((ListSelectionEvent e) -> {
                updateButtons();
            });
        this.tradeRoutes.setCellRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList list,
                    Object value, int index, boolean selected, boolean focus) {
                    Component ret = super.getListCellRendererComponent(list,
                        value, index, selected, focus);
                    TradeRoute tradeRoute = (TradeRoute)value;
                    String name = tradeRoute.getName();
                    Integer n = TradeRoutePanel.this.counts.get(tradeRoute);
                    if (n == null || n <= 0) {
                        setText(name);
                    } else {
                        setText(name + "  (" + n + ")");
                    }
                    return ret;
                }
            });

        JScrollPane tradeRouteView = new JScrollPane(tradeRoutes);

        // Buttons.  New route, edit and delete route actions do not
        // close the dialog by default, so they have dedicated action
        // listeners.  The ok, cancel and deassign actions do close
        // the dialog, so they are handled in the class-level action
        // listener below.
        this.newRouteButton = Utility.localizedButton("tradeRoutePanel.newRoute");
        Utility.localizeToolTip(this.newRouteButton, "tradeRoutePanel.new.tooltip");
        this.newRouteButton.addActionListener((ActionEvent ae) -> {
                newRoute();
            });

        this.editRouteButton = Utility.localizedButton("tradeRoutePanel.editRoute");
        Utility.localizeToolTip(this.editRouteButton, "tradeRoutePanel.edit.tooltip");
        this.editRouteButton.addActionListener((ActionEvent ae) -> {
                final TradeRoute selected = tradeRoutes.getSelectedValue();
                final String name = selected.getName();
                getGUI().showTradeRouteInputPanel(selected, () -> {
                        StringTemplate template = null;
                        if (selected.getName() == null) { // Cancelled
                            selected.setName(name);
                        } else if ((template = selected.verify()) == null) {
                            igc().updateTradeRoute(selected);
                            updateList(selected);
                        } else {
                            getGUI().showInformationMessage(template);
                        }
                    });
            });

        this.deleteRouteButton = Utility.localizedButton("tradeRoutePanel.deleteRoute");
        Utility.localizeToolTip(this.deleteRouteButton, "tradeRoutePanel.delete.tooltip");
        this.deleteRouteButton.addActionListener((ActionEvent ae) -> {
                TradeRoute route = getRoute();
                if (route != null) {
                    for (Unit u : route.getAssignedUnits()) {
                        igc().assignTradeRoute(u, null);
                    }
                    deleteTradeRoute(route);
                    updateList(null);
                }
            });

        this.deassignRouteButton = Utility.localizedButton("tradeRoutePanel.deassignRoute");
        Utility.localizeToolTip(this.deassignRouteButton, "tradeRoutePanel.deassign.tooltip");
        this.deassignRouteButton.setActionCommand(DEASSIGN);
        this.deassignRouteButton.addActionListener(this);

        JButton cancelButton = Utility.localizedButton("cancel");
        cancelButton.setActionCommand(CANCEL);
        cancelButton.addActionListener(this);
        setCancelComponent(cancelButton);

        updateButtons();
        updateList((unit == null || unit.getTradeRoute() == null) ? null
            : unit.getTradeRoute());

        add(Utility.localizedHeader(Messages.nameKey("tradeRoutePanel"), false),
            "span, align center");
        if (this.unit != null && this.unit.getLocation() != null) {
            JLabel unitLabel
                = new JLabel(unit.getDescription(Unit.UnitLabelType.NATIONAL));
            unitLabel.setIcon(new ImageIcon(
                getImageLibrary().getSmallerUnitImage(this.unit)));
            add(unitLabel);
            Location loc = this.unit.getLocation();
            JLabel locLabel = Utility.localizedLabel(loc.getLocationLabelFor(player));
            add(locLabel);
        }
        add(tradeRouteView, "height 360:400, width 250:");
        add(this.newRouteButton, "split 4, flowy, growx");
        add(this.editRouteButton, "growx");
        add(this.deleteRouteButton, "growx");
        add(this.deassignRouteButton);
        add(okButton, "newline 20, span, split 2, tag ok");
        add(cancelButton, "tag cancel");

        getGUI().restoreSavedSize(this, getPreferredSize());
    }


    /**
     * Gets the currently selected route.
     */
    private TradeRoute getRoute() {
        return this.tradeRoutes.getSelectedValue();
    }

    /**
     * Handle a new route request.
     */
    private void newRoute() {
        final Player player = getMyPlayer();
        final Unit u = this.unit;
        final TradeRoute newRoute = igc().getNewTradeRoute(player);
        getGUI().showTradeRouteInputPanel(newRoute, () -> {
                StringTemplate template = null;
                if (newRoute.getName() == null) { // Cancelled
                    deleteTradeRoute(newRoute);
                    updateList(null);
                } else if ((template = newRoute.verify()) == null) {
                    igc().updateTradeRoute(newRoute);
                    if (u != null) igc().assignTradeRoute(u, newRoute);
                    updateList(newRoute);
                } else {
                    updateList(null);
                    getGUI().showInformationMessage(template);
                }
            });
    }

    /**
     * Update the buttons on the panel.
     */
    private void updateButtons() {
        newRouteButton.setEnabled(true);
        if (tradeRoutes.getSelectedIndex() < 0) {
            editRouteButton.setEnabled(false);
            deleteRouteButton.setEnabled(false);
            deassignRouteButton.setEnabled(false);
        } else {
            editRouteButton.setEnabled(true);
            deleteRouteButton.setEnabled(true);
            deassignRouteButton.setEnabled(unit != null
                && unit.getTradeRoute() != null);
        }
    }

    /**
     * Update the list of routes displayed.
     *
     * @param selectRoute An optional <code>TradeRoute</code> to select.
     */
    private void updateList(TradeRoute selectRoute) {
        final Player player = getMyPlayer();

        // First update the counts
        this.counts.clear();
        for (Unit u : player.getUnits()) {
            TradeRoute tradeRoute = u.getTradeRoute();
            if (tradeRoute != null) {
                Integer i = counts.get(tradeRoute);
                int value = (i == null) ? 0 : i;
                counts.put(tradeRoute, value + 1);
            }
        }

        // Now create a sorted list of routes
        List<TradeRoute> routes = new ArrayList<>(player.getTradeRoutes());
        Collections.sort(routes, tradeRouteComparator);

        // Then add the routes to the list model.
        this.listModel.clear();
        for (TradeRoute route : routes) {
            this.listModel.addElement(route);
        }

        // Select the route if given.
        if (selectRoute != null && this.tradeRoutes != null) {
            this.tradeRoutes.setSelectedValue(selectRoute, true);
        }
    }

    /**
     * Delete a player trade route.
     *
     * @param route The <code>TradeRoute</code> to delete.
     */
    private void deleteTradeRoute(TradeRoute route) {
        List<TradeRoute> routes = getMyPlayer().getTradeRoutes();
        routes.remove(route);
        igc().setTradeRoutes(routes);
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        final String command = ae.getActionCommand();
        if (null == command) return;
        final TradeRoute route = getRoute();
        switch (command) {
        case DEASSIGN:
            if (unit != null && route == unit.getTradeRoute()) {
                igc().clearOrders(unit);
            }
            getGUI().removeFromCanvas(this);
            break;
        case OK:
            List<TradeRoute> routes = new ArrayList<>();
            for (int index = 0; index < listModel.getSize(); index++) {
                routes.add(listModel.getElementAt(index));
            }
            igc().setTradeRoutes(routes);
            if (unit != null && route != null) {
                igc().assignTradeRoute(unit, route);
            }
            super.actionPerformed(ae);
            break;
        case CANCEL:
            getGUI().removeTradeRoutePanel(this);
            break;
        default:
            super.actionPerformed(ae);
            break;
        }
    }


    // Override Component

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeNotify() {
        this.listModel.clear();
        this.tradeRoutes = null;
        this.counts.clear();
        this.newRouteButton = null;
        this.editRouteButton = null;
        this.deleteRouteButton = null;
        this.deassignRouteButton = null;

        super.removeNotify();
    }
}
