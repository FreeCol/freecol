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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Unit;

import net.miginfocom.swing.MigLayout;


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
        = new Comparator<TradeRoute>() {
            public int compare(TradeRoute r1, TradeRoute r2) {
                return r1.getName().compareTo(r2.getName());
            }
        };

    /** The unit to assign/deassign trade routes for. */
    private final Unit unit;

    /** The list model describing the players trade routes. */
    private DefaultListModel listModel;

    /** The list of trade routes to display. */
    private JList tradeRoutes;

    /** A map of trade route to the number of units using it. */
    private final Map<TradeRoute, Integer> counts
        = new HashMap<TradeRoute, Integer>();

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
    @SuppressWarnings("unchecked") // FIXME in Java7
    public TradeRoutePanel(FreeColClient freeColClient, Unit unit) {
        super(freeColClient, new MigLayout("wrap 2", "[fill][fill]"));

        final Player player = getMyPlayer();

        this.unit = unit;
        this.listModel = new DefaultListModel();
        this.tradeRoutes = new JList(listModel);
        this.tradeRoutes.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    updateButtons();
                }
            });
        this.tradeRoutes.setCellRenderer(new DefaultListCellRenderer() {
                public Component getListCellRendererComponent(JList list,
                    Object value, int index, boolean selected, boolean focus) {
                    Component ret = super.getListCellRendererComponent(list,
                        value, index, selected, focus);
                    TradeRoute tradeRoute = (TradeRoute)value;
                    String name = tradeRoute.getName();
                    Integer n = TradeRoutePanel.this.counts.get(tradeRoute);
                    if (n == null || n.intValue() <= 0) {
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
        this.newRouteButton
            = new JButton(Messages.message("tradeRoutePanel.newRoute"));
        this.newRouteButton.setToolTipText(Messages
            .message("tradeRoutePanel.new.tooltip"));
        this.newRouteButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    newRoute();
                }
            });

        this.editRouteButton
            = new JButton(Messages.message("tradeRoutePanel.editRoute"));
        this.editRouteButton.setToolTipText(Messages
            .message("tradeRoutePanel.edit.tooltip"));
        this.editRouteButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    final TradeRoute selected
                        = (TradeRoute)tradeRoutes.getSelectedValue();
                    final String name = selected.getName();
                    getGUI().showTradeRouteInputPanel(selected,
                        new Runnable() {
                            public void run() {
                                if (selected.getName() == null) { // Cancelled
                                    selected.setName(name);
                                } else if (selected.verify() == null) {
                                    getController().updateTradeRoute(selected);
                                    updateList(selected);
                                }
                            }
                        });
                }
            });

        this.deleteRouteButton
            = new JButton(Messages.message("tradeRoutePanel.deleteRoute"));
        this.deleteRouteButton.setToolTipText(Messages
            .message("tradeRoutePanel.delete.tooltip"));
        this.deleteRouteButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    TradeRoute route = getRoute();
                    if (route != null) {
                        for (Unit u : route.getAssignedUnits()) {
                            getController().assignTradeRoute(u, null);
                        }
                        deleteTradeRoute(route);
                        updateList(null);
                    }
                }
            });

        this.deassignRouteButton
            = new JButton(Messages.message("tradeRoutePanel.deassignRoute"));
        this.deassignRouteButton.setToolTipText(Messages
            .message("tradeRoutePanel.deassign.tooltip"));
        this.deassignRouteButton.setActionCommand(DEASSIGN);
        this.deassignRouteButton.addActionListener(this);

        JButton cancelButton = new JButton(Messages.message("cancel"));
        cancelButton.setActionCommand(CANCEL);
        cancelButton.addActionListener(this);
        setCancelComponent(cancelButton);

        updateButtons();
        updateList((unit == null || unit.getTradeRoute() == null) ? null
            : unit.getTradeRoute());

        add(GUI.getDefaultHeader(Messages.message("tradeRoutePanel.name")),
            "span, align center");
        if (this.unit != null && this.unit.getLocation() != null) {
            JLabel unitLabel = new JLabel(Messages.message(unit.getLabel()));
            unitLabel.setIcon(getLibrary().getUnitImageIcon(this.unit, 0.5));
            add(unitLabel);
            Location loc = this.unit.getLocation();
            JLabel locLabel
                = new JLabel(Messages.message(loc.getLocationNameFor(player)));
            //locLabel.setIcon(getLibrary().getImageIcon(loc, false));
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
        return (TradeRoute)this.tradeRoutes.getSelectedValue();
    }

    /**
     * Handle a new route request.
     */
    private void newRoute() {
        final Player player = getMyPlayer();
        final Unit u = this.unit;
        final TradeRoute newRoute = getController().getNewTradeRoute(player);
        getGUI().showTradeRouteInputPanel(newRoute,
            new Runnable() {
                public void run() {
                    if (newRoute.getName() == null) { // Cancelled
                        deleteTradeRoute(newRoute);
                        updateList(null);
                    } else if (newRoute.verify() == null) {
                        getController().updateTradeRoute(newRoute);
                        if (u != null) {
                            getController().assignTradeRoute(u, newRoute);
                        }
                        updateList(newRoute);
                    } else {
                        updateList(null);
                    }
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
    @SuppressWarnings("unchecked") // FIXME in Java7
    private void updateList(TradeRoute selectRoute) {
        final Player player = getMyPlayer();

        // First update the counts
        this.counts.clear();
        for (Unit u : player.getUnits()) {
            TradeRoute tradeRoute = u.getTradeRoute();
            if (tradeRoute != null) {
                Integer i = counts.get(tradeRoute);
                int value = (i == null) ? 0 : i.intValue();
                counts.put(tradeRoute, new Integer(value + 1));
            }
        }

        // Now create a sorted list of routes
        List<TradeRoute> routes
            = new ArrayList<TradeRoute>(player.getTradeRoutes());
        Collections.sort(routes, tradeRouteComparator);

        // Then add the routes to the list model.
        this.listModel.clear();
        for (TradeRoute route : routes) {
            this.listModel.addElement(route);
        }

        // Select the route if given.
        if (selectRoute != null) {
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
        getController().setTradeRoutes(routes);
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        final String command = event.getActionCommand();
        final TradeRoute route = getRoute();
        if (DEASSIGN.equals(command)) {
            if (unit != null && route == unit.getTradeRoute()) {
                getController().clearOrders(unit);
            }
            getGUI().removeFromCanvas(this);

        } else if (OK.equals(command)) {
            List<TradeRoute> routes = new ArrayList<TradeRoute>();
            for (int index = 0; index < listModel.getSize(); index++) {
                routes.add((TradeRoute)listModel.getElementAt(index));
            }
            getController().setTradeRoutes(routes);
            if (unit != null && route != null) {
                unit.setTradeRoute(route);
            }
            super.actionPerformed(event);

        } else if (CANCEL.equals(command)) {
            getGUI().removeFromCanvas(this);

        } else {
            super.actionPerformed(event);
        }
    }


    // Override Component

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeNotify() {
        this.listModel.clear();
        this.listModel = null;
        this.tradeRoutes = null;
        this.counts.clear();
        this.newRouteButton = null;
        this.editRouteButton = null;
        this.deleteRouteButton = null;
        this.deassignRouteButton = null;

        super.removeNotify();
    }
}
