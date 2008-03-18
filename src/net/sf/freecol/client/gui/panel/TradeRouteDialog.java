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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.TradeRoute;
import cz.autel.dmi.HIGLayout;

/**
 * Allows the user to edit trade routes.
 */
public final class TradeRouteDialog extends FreeColDialog implements ActionListener {

    private static final Logger logger = Logger.getLogger(TradeRouteDialog.class.getName());
    
    private static enum Action { OK, CANCEL, DEASSIGN };

    private final JButton ok = new JButton(Messages.message("ok"));
    private final JButton cancel = new JButton(Messages.message("cancel"));

    private final JButton editRouteButton = new JButton(Messages.message("traderouteDialog.editRoute"));
    private final JButton newRouteButton = new JButton(Messages.message("traderouteDialog.newRoute"));
    private final JButton removeRouteButton = new JButton(Messages.message("traderouteDialog.removeRoute"));
    private final JButton deassignRouteButton = new JButton(Messages.message("traderouteDialog.deassignRoute"));

    private final JPanel tradeRoutePanel = new JPanel();
    private final JPanel buttonPanel = new JPanel();
    
    private final DefaultListModel listModel = new DefaultListModel();
    private final JList tradeRoutes = new JList(listModel);
    private final JScrollPane tradeRouteView = new JScrollPane(tradeRoutes);

    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public TradeRouteDialog(final Canvas parent) {
        super(parent);

        tradeRoutePanel.setOpaque(false);

        ok.setActionCommand(Action.OK.toString());
        cancel.setActionCommand(Action.CANCEL.toString());
        deassignRouteButton.setActionCommand(Action.DEASSIGN.toString());
        
        ok.addActionListener(this);
        cancel.addActionListener(this);
        deassignRouteButton.addActionListener(this);

        ok.setMnemonic('y');
        cancel.setMnemonic('n');

        deassignRouteButton.setToolTipText(Messages.message("traderouteDialog.deassign.tooltip"));

        FreeColPanel.enterPressesWhenFocused(cancel);
        FreeColPanel.enterPressesWhenFocused(ok);
        FreeColPanel.enterPressesWhenFocused(deassignRouteButton);

        setCancelComponent(cancel);
        //ok.setActionCommand(String.valueOf(OK));

        tradeRoutes.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    updateButtons();
                }
            });

        // button for adding new TradeRoute
        newRouteButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Player player = parent.getClient().getMyPlayer();
                    TradeRoute newRoute = parent.getClient().getModelController().getNewTradeRoute(player);
                    newRoute.setName(Messages.message("traderouteDialog.newRoute"));
                    if (parent.showTradeRouteInputDialog(newRoute)) {
                        listModel.addElement(newRoute);
                        //tradeRoutes.revalidate();
                    }
                }
            });

        // button for editing TradeRoute
        editRouteButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    parent.showTradeRouteInputDialog((TradeRoute) tradeRoutes.getSelectedValue());
                }
            });

        // button for deleting TradeRoute
        removeRouteButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    listModel.removeElementAt(tradeRoutes.getSelectedIndex());
                }
            });

        int[] widths = {0};
        int[] heights = {0, margin, 0, margin, 0};
        setLayout(new HIGLayout(widths, heights));

        buttonPanel.add(ok);
        buttonPanel.add(cancel);
        buttonPanel.add(deassignRouteButton);
        buttonPanel.setOpaque(false);

        add(getDefaultHeader(Messages.message("traderouteDialog.name")),
            higConst.rc(1, 1));
        add(tradeRoutePanel, higConst.rc(3, 1));
        add(buttonPanel, higConst.rc(5, 1));

    }

    public void initialize() {

        Player player = getCanvas().getClient().getMyPlayer();

        tradeRoutePanel.removeAll();

        listModel.removeAllElements();
        for (TradeRoute route : player.getTradeRoutes()) {
            listModel.addElement(route);
        }

        updateButtons();

        int widths[] = {240, margin, 0};
        int heights[] = {120, 0, margin, 0, margin, 0, 120};
        int listColumn = 1;
        int buttonColumn = 3;

        tradeRoutePanel.setLayout(new HIGLayout(widths, heights));

        int row = 1;
        tradeRoutePanel.add(tradeRouteView, higConst.rcwh(row, listColumn, 1, heights.length));
        row ++;

        tradeRoutePanel.add(newRouteButton, higConst.rc(row, buttonColumn));
        row += 2;

        tradeRoutePanel.add(editRouteButton, higConst.rc(row, buttonColumn));
        row += 2;

        tradeRoutePanel.add(removeRouteButton, higConst.rc(row, buttonColumn));

        setSize(getPreferredSize());

    }
    
    public void setSelectedRoute(TradeRoute selectedRoute) {
        tradeRoutes.setSelectedValue(selectedRoute, true);
    }

    public void requestFocus() {
        ok.requestFocus();
    }

    public void updateButtons() {
        if (tradeRoutes.getSelectedIndex() == -1) {
            editRouteButton.setEnabled(false);
            removeRouteButton.setEnabled(false);
        } else {
            editRouteButton.setEnabled(true);
            removeRouteButton.setEnabled(true);
        }
    }

    
    /**
    * This function analyses an event and calls the right methods to take
    * care of the user's requests.
    * @param event The incoming ActionEvent.
    */
    public void actionPerformed(ActionEvent event) {
        Action action = Enum.valueOf(Action.class, event.getActionCommand());
        try {
            switch (action) {
            case OK:
                getCanvas().remove(this);
                ArrayList<TradeRoute> routes = new ArrayList<TradeRoute>();
                for (int index = 0; index < listModel.getSize(); index++) {
                    routes.add((TradeRoute) listModel.getElementAt(index));
                }
                getCanvas().getClient().getInGameController().setTradeRoutes(routes);
                setResponse(tradeRoutes.getSelectedValue());
                break;
            case CANCEL:
                getCanvas().remove(this);
                setResponse(null);
                break;
            case DEASSIGN:
                getCanvas().remove(this);
                setResponse(TradeRoute.NO_TRADE_ROUTE);
                break;
            default:
                logger.warning("Invalid ActionCommand: invalid number.");
            }
        }
        catch (NumberFormatException e) {
            logger.warning("Invalid Actioncommand: not a number.");
        }
    }
}
