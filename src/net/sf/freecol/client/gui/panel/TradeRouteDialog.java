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
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Unit;


/**
 * Allows the user to edit trade routes.
 */
public final class TradeRouteDialog extends FreeColOldDialog<Boolean>
    implements ActionListener {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(TradeRouteDialog.class.getName());

    private static enum Action { OK, CANCEL, DEASSIGN, DELETE }

    /** Compare trade routes by name. */
    private static final Comparator<TradeRoute> tradeRouteComparator
        = new Comparator<TradeRoute>() {
            public int compare(TradeRoute r1, TradeRoute r2) {
                return r1.getName().compareTo(r2.getName());
            }
        };

    private final JButton editRouteButton
        = new JButton(Messages.message("tradeRouteDialog.editRoute"));
    private final JButton newRouteButton
        = new JButton(Messages.message("tradeRoute.newRoute"));
    private final JButton removeRouteButton
        = new JButton(Messages.message("tradeRouteDialog.removeRoute"));
    private final JButton deassignRouteButton
        = new JButton(Messages.message("tradeRouteDialog.deassignRoute"));

    private final DefaultListModel listModel = new DefaultListModel();
    @SuppressWarnings("unchecked") // FIXME in Java7
    private final JList tradeRoutes = new JList(listModel);
    private final JScrollPane tradeRouteView = new JScrollPane(tradeRoutes);

    /** A map of trade route to the number of units using it. */
    private final Map<TradeRoute, Integer> counts
        = new HashMap<TradeRoute, Integer>();

    /** The unit to assign/deassign trade routes for. */
    private Unit unit = null;


    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param unit The optional <code>Unit</code> to operate on.
     */
    @SuppressWarnings("unchecked") // FIXME in Java7
    public TradeRouteDialog(FreeColClient freeColClient, Unit unit) {
        super(freeColClient, new MigLayout("wrap 2", "[fill][fill]"));

        final Player player = getMyPlayer();

        this.unit = unit;

        deassignRouteButton.addActionListener(this);
        deassignRouteButton.setToolTipText(Messages.message("tradeRouteDialog.deassign.tooltip"));
        deassignRouteButton.setActionCommand(Action.DEASSIGN.toString());

        tradeRoutes.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    updateButtons();
                }
            });

        // button for adding new TradeRoute
        newRouteButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    final TradeRoute newRoute
                        = getController().getNewTradeRoute(player);
                    getGUI().showTradeRouteInputPanel(newRoute,
                        new Runnable() {
                            public void run() {
try {
                                if (newRoute.getName() == null) {
                                    return; // cancelled
                                }
                                StringTemplate err = newRoute.verify();
                                if (err == null) {
                                    getController().updateTradeRoute(newRoute);
                                    updateList();
                                    TradeRouteDialog.this.tradeRoutes
                                        .setSelectedValue(newRoute, true);
                                } else {
                                    getGUI().showErrorMessage(err);
                                }
}catch (Exception e) { logger.log(Level.WARNING, "foo", e); }
                            }
                        });
                }
            });

        // button for editing TradeRoute
        editRouteButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    final TradeRoute selected
                        = (TradeRoute)tradeRoutes.getSelectedValue();
                    getGUI().showTradeRouteInputPanel(selected,
                        new Runnable() {
                            public void run() {
                                if (selected.verify() == null) {
                                    TradeRouteDialog.this.tradeRoutes
                                        .setSelectedValue(selected, true);
                                }
                            }
                        });
                }
            });

        // button for deleting TradeRoute
        removeRouteButton.addActionListener(this);
        removeRouteButton.setActionCommand(Action.DELETE.toString());

        tradeRoutes.setCellRenderer(new DefaultListCellRenderer() {
                public Component getListCellRendererComponent(JList list,
                    Object value, int index, boolean selected, boolean focus) {
                    Component ret = super.getListCellRendererComponent(list,
                        value, index, selected, focus);
                    TradeRoute tradeRoute = (TradeRoute)value;
                    String name = tradeRoute.getName();
                    Integer n = counts.get(tradeRoute);
                    if (n == null || n.intValue() <= 0) {
                        setText(name);
                    } else {
                        setText(name + "  (" + n + ")");
                    }
                    return ret;
                }
            });

        updateList();
        if (unit != null && unit.getTradeRoute() != null) {
            tradeRoutes.setSelectedValue(unit.getTradeRoute(), true);
        }
        updateButtons();

        add(GUI.getDefaultHeader(Messages.message("tradeRouteDialog.name")),
            "span, align center");

        add(tradeRouteView, "height 360:400, width 250:");
        add(newRouteButton, "split 4, flowy, growx");
        add(editRouteButton, "growx");
        add(removeRouteButton, "growx");
        add(deassignRouteButton);

        add(okButton, "newline 20, span, split 2, tag ok");
        add(cancelButton, "tag cancel");

        getGUI().restoreSavedSize(this, getPreferredSize());
    }

    public void updateButtons() {
        if (tradeRoutes.getSelectedIndex() < 0) {
            editRouteButton.setEnabled(false);
            removeRouteButton.setEnabled(false);
            deassignRouteButton.setEnabled(false);
        } else {
            editRouteButton.setEnabled(true);
            removeRouteButton.setEnabled(true);
            deassignRouteButton.setEnabled(unit != null);
        }
    }

    @SuppressWarnings("unchecked") // FIXME in Java7
    private void updateList() {
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
    }

    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent event) {
        Action action = Enum.valueOf(Action.class, event.getActionCommand());
        TradeRoute route = (TradeRoute)tradeRoutes.getSelectedValue();
        boolean ret = false;
        if (route != null) {
            switch (action) {
            case OK:
                List<TradeRoute> routes = new ArrayList<TradeRoute>();
                for (int index = 0; index < listModel.getSize(); index++) {
                    routes.add((TradeRoute)listModel.getElementAt(index));
                    //System.err.println(index + " : " + routes.get(index).getName());
                }
                getController().setTradeRoutes(routes);
                if (unit != null) unit.setTradeRoute(route);
                ret = true;
                break;
            case DEASSIGN:
                if (unit != null && route == unit.getTradeRoute()) {
                    unit.setTradeRoute(null);
                    ret = true;
                }
                break;
            case DELETE:
                for (Unit u : route.getAssignedUnits()) {
                    getController().clearOrders(u);
                }
                final Player player = getMyPlayer();
                player.getTradeRoutes().remove(route);
                getController().setTradeRoutes(player.getTradeRoutes());
                updateList();
                return; // Continue, do not set response
            case CANCEL: default:
                ret = false;
                break;
            }
        }
        getGUI().removeFromCanvas(this);
        setResponse(ret);
    }
}
