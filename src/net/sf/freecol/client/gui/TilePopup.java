/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

package net.sf.freecol.client.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.action.UnloadAction;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.ChoiceItem;
import net.sf.freecol.client.gui.panel.ReportPanel;
import net.sf.freecol.client.gui.panel.TilePanel;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.CombatModel.CombatOdds;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.LostCityRumour.RumourType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.ai.AIColony;
import net.sf.freecol.server.ai.AIGoods;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.ai.TileImprovementPlan;
import net.sf.freecol.server.ai.Wish;
import net.sf.freecol.server.ai.mission.TransportMission;
import net.sf.freecol.server.model.ServerUnit;


/**
 * Allows the user to obtain more info about a certain tile
 * or to activate a specific unit on the tile.
 */
public final class TilePopup extends JPopupMenu {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(TilePopup.class.getName());

    public static final int UNIT_LINES_IN_FIRST_MENU = 9;
    public static final int UNIT_LINES_IN_OTHER_MENUS = 19;

    private final Canvas canvas;
    private final FreeColClient freeColClient;
    private final GUI gui;

    private boolean hasAnItem = false;

    /**
     * The constructor that will insert the MenuItems.
     *
     * @param tile The <code>Tile</code> to create a popup for.
     *       The popup menu also appears near this <code>Tile</code>.
     * @param freeColClient The main controller object for the client.
     * @param canvas The component containing the map.
     * @param gui An object with methods used for making the popup.
     */
    public TilePopup(final Tile tile, final FreeColClient freeColClient, final Canvas canvas, final GUI gui) {
        super(Messages.message("tile",
                               "%x%", String.valueOf(tile.getX()),
                               "%y%", String.valueOf(tile.getY())));

        this.canvas = canvas;
        this.freeColClient = freeColClient;
        this.gui = gui;

        final Unit activeUnit = gui.getActiveUnit();
        if (activeUnit != null) {
            Tile unitTile = activeUnit.getTile();
            JMenuItem gotoMenuItem = null;
            if (activeUnit.isOffensiveUnit() &&
                unitTile.isAdjacent(tile) &&
                activeUnit.getMoveType(tile) == MoveType.ATTACK) {
                CombatOdds combatOdds = activeUnit.getGame().getCombatModel()
                    .calculateCombatOdds(activeUnit, tile.getDefendingUnit(activeUnit));

                String victoryPercent;
                //If attacking a settlement, the true odds are never known because units may be hidden within
                if (tile.getSettlement() != null || combatOdds.win == CombatOdds.UNKNOWN_ODDS) {
                    victoryPercent = "??";
                } else {
                    victoryPercent = Integer.toString((int)(combatOdds.win * 100));
                }
                gotoMenuItem = new JMenuItem(Messages.message("attackTileOdds", "%chance%", victoryPercent));
            } else if (activeUnit.getSimpleMoveType(unitTile, tile, false).isLegal()) {
                //final Image gotoImage = (Image) UIManager.get("cursor.go.image");
                //JMenuItem gotoMenuItem = new JMenuItem(Messages.message("gotoThisTile"), new ImageIcon(gotoImage));
                gotoMenuItem = new JMenuItem(Messages.message("gotoThisTile"));
            }
            if (gotoMenuItem != null) {
                gotoMenuItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent event) {
                            if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
                            	return;
                            }
                            Tile currTile = activeUnit.getTile();
                        	// just checking we are not already at the destination
                            if (currTile==tile) {
                            	return;
                            }

                            freeColClient.getInGameController().setDestination(activeUnit, tile);
                            freeColClient.getInGameController().moveToDestination(activeUnit);

                            //if unit did not move, we should show the goto path
                            if(activeUnit.getTile() == currTile){
                            	gui.updateGotoPathForActiveUnit();
                            }
                        }
                    });
                add(gotoMenuItem);
                hasAnItem = true;
                addSeparator();
            }
        }

        Settlement settlement = tile.getSettlement();
        if (settlement != null) {
            if (settlement.getOwner() == freeColClient.getMyPlayer()) {
                addColony(((Colony) settlement));
            } else if (settlement instanceof IndianSettlement) {
                addIndianSettlement((IndianSettlement) settlement);
            }
            if (hasItem()) {
                addSeparator();
            }
        }

        addTile(tile);
        addSeparator();

        int lineCount = 0;
        int maxUnits = UNIT_LINES_IN_FIRST_MENU;
        Container currentMenu = this;
        boolean moreUnits = false;
        List<Unit> units = new ArrayList<Unit>(tile.getUnitList());
        Collections.sort(units, ReportPanel.unitTypeComparator);
        for (final Unit currentUnit : units) {

            if (lineCount > maxUnits) {
                JMenu more = new JMenu(Messages.message("more"));
                more.setFont(more.getFont().deriveFont(Font.ITALIC));
                more.setOpaque(false);
                currentMenu.add(more);
                currentMenu = more;
                moreUnits = true;
                lineCount = 0;
                maxUnits = UNIT_LINES_IN_OTHER_MENUS;
            }

            lineCount += addUnit(currentMenu, currentUnit, !currentUnit.isUnderRepair(), false);
        }

        if (tile.getUnitCount() > 1) {
            if (moreUnits) {
                addSeparator();
            }
            JMenuItem activateAllItem = new JMenuItem(Messages.message("activateAllUnits"));
            activateAllItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        Unit lastUnit = null;
                        for (Unit unit: tile.getUnitList()) {
                            freeColClient.getInGameController().clearOrders(unit);
                            lastUnit = unit;
                        }
                        gui.setActiveUnit(lastUnit);
                    }
                });
            add(activateAllItem);
        }

        // START DEBUG
        if (FreeCol.isInDebugMode()
            && freeColClient.getFreeColServer() != null) {
            addSeparator();
            JMenu takeOwnership = new JMenu("Take ownership");
            takeOwnership.setOpaque(false);
            JMenu transportLists = new JMenu("Transport lists");
            transportLists.setOpaque(false);
            boolean notEmpty = false;
            for (final Unit currentUnit : tile.getUnitList()) {
                JMenuItem toMenuItem = new JMenuItem(currentUnit.toString());
                toMenuItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent event) {
                            // Server:
                            final Game serverGame = freeColClient.getFreeColServer().getGame();
                            final Player serverPlayer = (Player) serverGame.getFreeColGameObject(freeColClient.getMyPlayer().getId());
                            final Unit serverUnit = (Unit) serverGame.getFreeColGameObject(currentUnit.getId());
                            serverUnit.setOwner(serverPlayer);
                            for (Unit serverChildUnit : currentUnit.getUnitList()) {
                                serverChildUnit.setOwner(serverPlayer);
                            }
                            freeColClient.getConnectController().reconnect();
                        }
                    });
                takeOwnership.add(toMenuItem);
                notEmpty = true;
                if (currentUnit.isCarrier()) {
                    final AIUnit au = (AIUnit) freeColClient.getFreeColServer().getAIMain().getAIObject(currentUnit);
                    if (au.getMission() != null && au.getMission() instanceof TransportMission) {
                        JMenuItem menuItem = new JMenuItem(currentUnit.toString());
                        menuItem.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent event) {
                                    canvas.showInformationMessage(au.getMission().toString());
                                }
                            });
                        transportLists.add(menuItem);
                    }
                }
            }
            if (transportLists.getItemCount() > 0) {
                add(transportLists);
            }
            if (tile.getColony() != null) {
                if (!notEmpty) {
                    takeOwnership.addSeparator();
                }
                JMenuItem toMenuItem = new JMenuItem(tile.getSettlement().toString());
                toMenuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        // Server:
                        final Game serverGame = freeColClient.getFreeColServer().getGame();
                        final Player serverPlayer = (Player) serverGame.getFreeColGameObject(freeColClient.getMyPlayer().getId());
                        final Tile serverTile = (Tile) serverGame.getFreeColGameObject(tile.getId());
                        serverTile.getSettlement().changeOwner(serverPlayer);
                        freeColClient.getConnectController().reconnect();
                    }
                });
                takeOwnership.add(toMenuItem);
                JMenuItem displayColonyPlan = new JMenuItem("Display Colony Plan");
                displayColonyPlan.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        // Server:
                        final Game serverGame = freeColClient.getFreeColServer().getGame();
                        final Tile serverTile = (Tile) serverGame.getFreeColGameObject(tile.getId());
                        final AIColony ac = (AIColony) freeColClient.getFreeColServer().getAIMain().getAIObject(serverTile.getSettlement());
                        StringBuilder info = new StringBuilder(ac.getColonyPlan().toString());
                        info.append("\n\nTILE IMPROVEMENTS:\n");
                        Iterator<TileImprovementPlan> tipIt = ac.getTileImprovementPlanIterator();
                        while (tipIt.hasNext()) {
                            info.append(tipIt.next().toString());
                            info.append("\n");
                        }
                        info.append("\n\nWISHES:\n");
                        Iterator<Wish> wishIterator = ac.getWishIterator();
                        while (wishIterator.hasNext()) {
                            info.append(wishIterator.next().toString());
                            info.append("\n");
                        }
                        info.append("\n\nEXPORT GOODS:\n");
                        Iterator<AIGoods> goodsIterator = ac.getAIGoodsIterator();
                        while (goodsIterator.hasNext()) {
                            info.append(goodsIterator.next().toString());
                            info.append("\n");
                        }
                        canvas.showInformationMessage(info.toString());
                    }
                });
                add(displayColonyPlan);
                notEmpty = true;
            }
            if (notEmpty) {
                add(takeOwnership);
                hasAnItem = true;
            }
            if (tile.hasLostCityRumour()) {
                JMenuItem rumourItem = new JMenuItem("Set Lost City Rumour type");
                rumourItem.setOpaque(false);
                rumourItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent event) {
                            debugSetRumourType(tile);
                        }
                    });
                add(rumourItem);
            }
            JMenuItem addu = new JMenuItem("Add unit");
            addu.setOpaque(false);
            addu.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        debugAddNewUnitToTile(tile);
                    }
                });
            add(addu);
            JMenuItem dumpItem = new JMenuItem("Dump tile");
            dumpItem.setOpaque(false);
            dumpItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        System.err.println("\nClient side:\n");
                        tile.dumpObject();
                        System.err.println("\nServer side:\n");
                        Game sGame = freeColClient.getFreeColServer().getGame();
                        Tile sTile = (Tile) sGame.getFreeColGameObject(tile.getId());
                        sTile.dumpObject();
                    }
                });
            add(dumpItem);
        }
        // END DEBUG
        Component lastComponent = getComponent(getComponentCount() - 1);
        if (lastComponent instanceof JSeparator) {
            remove(lastComponent);
        }
    }

    /**
     * Adds a unit entry to this popup.
     * @param menu a <code>Container</code> value
     * @param unit The unit that will be represented on the popup.
     * @param enabled The initial state for the menu item.
     * @param indent Should be <code>true</code> if the text should be
     *      indented on the menu.
     * @return an <code>int</code> value
     */
    private int addUnit(Container menu, final Unit unit, boolean enabled, boolean indent) {
        StringTemplate occ;
        TradeRoute tradeRoute = unit.getTradeRoute();

        if (unit.getState() == Unit.UnitState.ACTIVE
            && unit.getMovesLeft() == 0) {
            if (unit.isUnderRepair()) {
                occ = StringTemplate.label(": ")
                    .add("model.unit.occupation.underRepair")
                    .add(Integer.toString(unit.getTurnsForRepair()));
            } else if (tradeRoute != null) {
                occ = StringTemplate.label(": ")
                    .add("model.unit.occupation.inTradeRoute")
                    .addName(tradeRoute.getName());
            } else {
                occ = StringTemplate.key("model.unit.occupation.activeNoMovesLeft");
            }
        } else if (unit.getState() == Unit.UnitState.IMPROVING
                   && unit.getWorkImprovement() != null) {
            occ = StringTemplate.label(": ")
                .add(unit.getWorkImprovement().getType() + ".occupationString")
                .add(Integer.toString(unit.getWorkTurnsLeft()));
        } else if (tradeRoute != null) {
            occ = StringTemplate.label(": ")
                .add("model.unit.occupation.inTradeRoute")
                .add(tradeRoute.getName());
        } else if (unit.getDestination() != null) {
            occ = StringTemplate.key("model.unit.occupation.goingSomewhere");
        } else {
            occ = StringTemplate.key("model.unit.occupation." + unit.getState().toString().toLowerCase());
        }

        String text = (indent ? "    " : "")
            + Messages.message(StringTemplate.template("model.unit.nationUnit")
                               .addStringTemplate("%nation%", unit.getOwner().getNationName())
                               .addStringTemplate("%unit%", Messages.getLabel(unit)))
            + " (" + Messages.message(occ) + ")";
        JMenuItem menuItem = new JMenuItem(text);
        menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    gui.setActiveUnit(unit);
                }
            });
        int lineCount = 1;
        if (indent) {
            menuItem.setFont(menuItem.getFont().deriveFont(Font.ITALIC));
        }
        menuItem.setEnabled(enabled);
        menu.add(menuItem);

        for (Unit passenger : unit.getUnitList()) {
            lineCount += addUnit(menu, passenger, true, true);
        }

        boolean hasGoods = false;
        for (Goods goods: unit.getGoodsList()) {
            text = (indent ? "         " : "     ")
                + Messages.message(goods.getLabel(true));
            menuItem = new JMenuItem(text);
            menuItem.setFont(menuItem.getFont().deriveFont(Font.ITALIC));
            menuItem.setEnabled(false);
            menu.add(menuItem);
            lineCount++;
            hasGoods = true;
        }

        if (hasGoods) {
            JMenuItem dumpItem = new JMenuItem(Messages.message("dumpCargo"));
            dumpItem.setAction(new UnloadAction(freeColClient, unit));
            menu.add(dumpItem);
            lineCount++;
        }
        hasAnItem = true;
        return lineCount;
    }

    /**
     * Adds a colony entry to this popup.
     * @param colony The colony that will be represented on the popup.
     */
    private void addColony(final Colony colony) {
        String name = colony.getNameFor(freeColClient.getMyPlayer());
        JMenuItem menuItem = new JMenuItem(Messages.message(name));
        menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    canvas.showColonyPanel(colony);
                }
            });

        add(menuItem);

        menuItem = new JMenuItem(Messages.message("rename"));
        menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    freeColClient.getInGameController().rename(colony);
                }
            });

        add(menuItem);

        hasAnItem = true;
    }


    /**
     * Adds an indian settlement entry to this popup.
     * @param settlement The Indian settlement that will be represented on the popup.
     */
    private void addIndianSettlement(final IndianSettlement settlement) {
        String name = settlement.getNameFor(freeColClient.getMyPlayer());
        JMenuItem menuItem = new JMenuItem(Messages.message(name));
        menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    canvas.showIndianSettlementPanel(settlement);
                }
            });
        add(menuItem);
        hasAnItem = true;
    }

    /**
     * Adds a tile entry to this popup.
     * @param tile The tile that will be represented on the popup.
     */
    private void addTile(final Tile tile) {
        JMenuItem menuItem = new JMenuItem(Messages.message(tile.getNameKey()));
        menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    canvas.showPanel(new TilePanel(canvas, tile));
                }
            });

        add(menuItem);
        /**
         * Don't set hasAnItem to true, we want the tile panel to open
         * automatically whenever there is no other item on the list.
         */
        // hasAnItem = true;
    }

    /**
     * Returns true if this popup has at least one menuitem so that we know that we can
     * show it to the user. Returns false if there are no menuitems.
     * @return true if this popup has at least one menuitem, false otherwise.
     */
    public boolean hasItem() {
        return hasAnItem || FreeCol.isInDebugMode();
    }

    /**
     * Debug action to set the lost city rumour type on a tile.
     *
     * @param tile The <code>Tile</code> to operate on.
     */
    private void debugSetRumourType(Tile tile) {
        List<ChoiceItem<RumourType>> rumours = new ArrayList<ChoiceItem<RumourType>>();
        for (RumourType rumour : RumourType.values()) {
            if (rumour == RumourType.NO_SUCH_RUMOUR) continue;
            rumours.add(new ChoiceItem<RumourType>(rumour.toString(), rumour));
        }
        RumourType rumourChoice = freeColClient.getCanvas()
            .showChoiceDialog(null, "Select Lost City Rumour", "Cancel", rumours);
        tile.getTileItemContainer().getLostCityRumour().setType(rumourChoice);
        final Tile serverTile = (Tile) freeColClient.getFreeColServer()
            .getGame().getFreeColGameObject(tile.getId());
        serverTile.getTileItemContainer().getLostCityRumour()
            .setType(rumourChoice);
    }

    /**
     * Debug action to add a new unit to a tile.
     *
     * @param tile The <code>Tile</code> to add to.
     */
    private void debugAddNewUnitToTile(Tile tile) {
        List<ChoiceItem<UnitType>> uts = new ArrayList<ChoiceItem<UnitType>>();
        for (UnitType t : tile.getSpecification().getUnitTypeList()) {
            uts.add(new ChoiceItem<UnitType>(Messages.message(t.toString() + ".name"), t));
        }
        UnitType unitChoice = freeColClient.getCanvas()
            .showChoiceDialog(null, "Select Unit Type", "Cancel", uts);
        if (unitChoice != null) {
            Game game = freeColClient.getGame();
            Game sGame = freeColClient.getFreeColServer().getGame();
            Player player = freeColClient.getMyPlayer();
            Player sPlayer = (Player) sGame.getFreeColGameObject(player.getId());
            Tile sTile = (Tile) sGame.getFreeColGameObject(tile.getId());
            ServerUnit sUnit = new ServerUnit(sGame, sTile, sPlayer,
                                              unitChoice, UnitState.ACTIVE,
                                              unitChoice.getDefaultEquipment());
            Unit unit = new Unit(game, sUnit.toXMLElement(Message.createNewDocument()));
            tile.add(unit);
            player.invalidateCanSeeTiles();
            canvas.refresh();
        }
    }

}
