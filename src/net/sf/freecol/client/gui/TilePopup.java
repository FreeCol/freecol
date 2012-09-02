/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.action.UnloadAction;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.ChoiceItem;
import net.sf.freecol.client.gui.panel.ReportPanel;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.CombatModel.CombatOdds;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.LostCityRumour.RumourType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.server.ai.AIColony;
import net.sf.freecol.server.ai.AIGoods;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.ai.TileImprovementPlan;
import net.sf.freecol.server.ai.Wish;
import net.sf.freecol.server.ai.mission.TransportMission;
import net.sf.freecol.server.model.ServerUnit;


/**
 * Allows the user to obtain more info about a certain tile or to
 * activate a specific unit on the tile, or perform various debug mode
 * actions.
 */
public final class TilePopup extends JPopupMenu {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(TilePopup.class.getName());

    public static final int UNIT_LINES_IN_FIRST_MENU = 9;
    public static final int UNIT_LINES_IN_OTHER_MENUS = 19;

    private final FreeColClient freeColClient;
    private final GUI gui;

    private boolean hasAnItem = false;

    /**
     * The constructor that will insert the MenuItems.
     * @param freeColClient The main controller object for the client.
     * @param gui The GUI frontend.
     * @param tile The <code>Tile</code> to create a popup for.
     *       The popup menu also appears near this <code>Tile</code>.
     */
    public TilePopup(final FreeColClient freeColClient, final GUI gui,
                     final Tile tile) {
        super(Messages.message(StringTemplate.template("tile")
                               .addAmount("%x%", tile.getX())
                               .addAmount("%y%", tile.getY())));

        this.freeColClient = freeColClient;
        this.gui = gui;

        final Player player = freeColClient.getMyPlayer();
        final Unit activeUnit = gui.getActiveUnit();
        if (activeUnit != null) {
            Tile unitTile = activeUnit.getTile();
            JMenuItem gotoMenuItem = null;
            if (activeUnit.isOffensiveUnit() &&
                unitTile.isAdjacent(tile) &&
                activeUnit.getMoveType(tile).isAttack()) {
                CombatOdds combatOdds = activeUnit.getGame().getCombatModel()
                    .calculateCombatOdds(activeUnit, tile.getDefendingUnit(activeUnit));

                String victoryPercent;
                // If attacking a settlement, the true odds are never known because units
                // may be hidden within
                if (tile.getSettlement() != null || combatOdds.win == CombatOdds.UNKNOWN_ODDS) {
                    victoryPercent = "??";
                } else {
                    victoryPercent = Integer.toString((int)(combatOdds.win * 100));
                }
                gotoMenuItem = new JMenuItem(Messages.message(StringTemplate.
                         template("attackTileOdds").addName("%chance%", victoryPercent)));
            } else if (activeUnit.getSimpleMoveType(unitTile, tile).isLegal()) {
                //final Image gotoImage = (Image) UIManager.get("cursor.go.image");
                //JMenuItem gotoMenuItem = new JMenuItem(Messages.message("gotoThisTile"), new ImageIcon(gotoImage));
                gotoMenuItem = new JMenuItem(Messages.message("gotoThisTile"));
            }
            if (gotoMenuItem != null) {
                gotoMenuItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent event) {
                            if (!freeColClient.currentPlayerIsMyPlayer()) {
                                return;
                            }
                            Tile currTile = activeUnit.getTile();
                            // already at the destination?
                            if (currTile == tile) return;
                            freeColClient.getInGameController()
                                .goToTile(activeUnit, tile);
                            // if unit did not move, we should show
                            // the goto path
                            if (activeUnit.getTile() == currTile) {
                                gui.getMapViewer().updateGotoPathForActiveUnit();
                            }
                        }
                    });
                add(gotoMenuItem);
                hasAnItem = true;
            }

            // Add move to Europe entry if the unit can do so
            if (unitTile == tile && activeUnit.hasHighSeasMove()) {
                JMenuItem europeMenuItem
                    = new JMenuItem(Messages.message(StringTemplate.template("gotoEurope")));
                europeMenuItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent event) {
                            if (!freeColClient.currentPlayerIsMyPlayer()) {
                                return;
                            }
                            freeColClient.getInGameController()
                                .moveTo(activeUnit, player.getEurope());
                        }
                    });
                add(europeMenuItem);
                hasAnItem = true;
            }
            if (hasAnItem) addSeparator();
        }

        Settlement settlement = tile.getSettlement();
        if (settlement != null) {
            if (settlement.getOwner() == player) {
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
        List<Unit> units = tile.getUnitList();
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
                        for (Unit unit : tile.getUnitList()) {
                            freeColClient.getInGameController().clearOrders(unit);
                            lastUnit = unit;
                        }
                        gui.setActiveUnit(lastUnit);
                    }
                });
            add(activateAllItem);
        }

        // START DEBUG
        if (FreeColDebugger.isInDebugMode()
            && freeColClient.getFreeColServer() != null) {
            final Game serverGame = freeColClient.getFreeColServer().getGame();
            final Player serverPlayer = serverGame.getFreeColGameObject(player.getId(),
                                                                        Player.class);
            boolean notEmpty = false;
            addSeparator();
            JMenu takeOwnership = new JMenu("Take ownership");
            takeOwnership.setOpaque(false);

            JMenu transportLists = new JMenu("Transport lists");
            transportLists.setOpaque(false);
            for (final Unit currentUnit : tile.getUnitList()) {
                JMenuItem toMenuItem = new JMenuItem(currentUnit.toString());
                toMenuItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent event) {
                            // Server:
                            final Unit serverUnit = serverGame.getFreeColGameObject(currentUnit.getId(), Unit.class);
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
                    final AIUnit au = freeColClient.getFreeColServer()
                        .getAIMain().getAIUnit(currentUnit);
                    if (au.getMission() != null && au.getMission() instanceof TransportMission) {
                        JMenuItem menuItem = new JMenuItem(currentUnit.toString());
                        menuItem.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent event) {
                                    gui.showInformationMessage(au.getMission().toString());
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
                        final Tile serverTile = serverGame.getFreeColGameObject(tile.getId(), Tile.class);
                        serverTile.getSettlement().changeOwner(serverPlayer);
                        freeColClient.getConnectController().reconnect();
                    }
                });
                takeOwnership.add(toMenuItem);
                notEmpty = true;

                JMenuItem displayColonyPlan = new JMenuItem("Display Colony Plan");
                displayColonyPlan.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        // Server:
                        final Tile serverTile = serverGame.getFreeColGameObject(tile.getId(), Tile.class);
                        final AIColony ac = freeColClient.getFreeColServer()
                            .getAIMain().getAIColony(serverTile.getColony());
                        StringBuilder info = new StringBuilder(ac.getColonyPlan().toString());
                        info.append("\n\nTILE IMPROVEMENTS:\n");
                        for (TileImprovementPlan tip : ac.getTileImprovementPlans()) {
                            info.append(tip.toString());
                            info.append("\n");
                        }
                        info.append("\n\nWISHES:\n");
                        for (Wish w : ac.getWishes()) {
                            info.append(w.toString());
                            info.append("\n");
                        }
                        info.append("\n\nEXPORT GOODS:\n");
                        for (AIGoods aig : ac.getAIGoods()) {
                            info.append(aig.toString());
                            info.append("\n");
                        }
                        gui.showInformationMessage(info.toString());
                    }
                });
                add(displayColonyPlan);
            }
            if (tile.getIndianSettlement() != null) {
                JMenuItem displayGoods = new JMenuItem("Examine Settlement");
                final IndianSettlement is = tile.getIndianSettlement();
                displayGoods.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent event) {
                            final IndianSettlement sis = serverGame.getFreeColGameObject(is.getId(), IndianSettlement.class);
                            gui.showInformationMessage(
                                debugSummarizeSettlement(serverGame, sis));
                        }
                    });
                add(displayGoods);
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
                            debugSetRumourType(serverGame, tile);
                        }
                    });
                add(rumourItem);
            }

            JMenuItem addu = new JMenuItem("Add unit");
            addu.setOpaque(false);
            addu.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        debugAddNewUnitToTile(serverGame, tile);
                    }
                });
            add(addu);

            if (!tile.isEmpty()) {
                final List<Unit> tileUnits = tile.getUnitList();
                JMenuItem adda = new JMenuItem("Reset moves");
                adda.setOpaque(false);
                adda.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent event) {
                            debugResetUnitsMoves(serverGame, tileUnits);
                        }
                    });
                add(adda);
            }

            for (Unit u : tile.getUnitList()) {
                if (u.canCarryGoods() && u.hasSpaceLeft()) {
                    final Unit unit = u;
                    JMenuItem addg = new JMenuItem("Add goods");
                    addg.setOpaque(false);
                    addg.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent event) {
                                debugAddGoodsToUnit(serverGame, unit);
                            }
                        });
                    add(addg);
                    break;
                }
            }

            JMenuItem dumpItem = new JMenuItem("Dump tile");
            dumpItem.setOpaque(false);
            dumpItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        System.err.println("\nClient side:");
                        tile.dumpObject();
                        System.err.println("\n\nServer side:");
                        serverGame.getFreeColGameObject(tile.getId())
                            .dumpObject();
                        System.err.println("\n");
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
            dumpItem.setAction(new UnloadAction(freeColClient, gui, unit));
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
        StringTemplate name = colony.getLocationNameFor(freeColClient.getMyPlayer());
        JMenuItem menuItem = new JMenuItem(Messages.message(name));
        menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    gui.showColonyPanel(colony);
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
     *
     * @param settlement The Indian settlement that will be
     *     represented on the popup.
     */
    private void addIndianSettlement(final IndianSettlement settlement) {
        StringTemplate name = settlement.getLocationNameFor(freeColClient.getMyPlayer());
        JMenuItem menuItem = new JMenuItem(Messages.message(name));
        menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    gui.showIndianSettlementPanel(settlement);
                }
            });
        add(menuItem);
        hasAnItem = true;
    }

    /**
     * Adds a tile entry to this popup.
     *
     * @param tile The tile that will be represented on the popup.
     */
    private void addTile(final Tile tile) {
        JMenuItem menuItem = new JMenuItem(Messages.message(tile.getNameKey()));
        menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    gui.showTilePanel(tile);
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
     * Returns true if this popup has at least one menuitem so that we
     * know that we can show it to the user. Returns false if there
     * are no menuitems.
     *
     * @return True if this popup has at least one menuitem, false otherwise.
     */
    public boolean hasItem() {
        return hasAnItem || FreeColDebugger.isInDebugMode();
    }

    /**
     * Debug action to set the lost city rumour type on a tile.
     *
     * @param serverGame The server <code>Game</code> containing the tile.
     * @param tile The <code>Tile</code> to operate on.
     */
    private void debugSetRumourType(final Game serverGame, Tile tile) {
        List<ChoiceItem<RumourType>> rumours
            = new ArrayList<ChoiceItem<RumourType>>();
        for (RumourType rumour : RumourType.values()) {
            if (rumour == RumourType.NO_SUCH_RUMOUR) continue;
            rumours.add(new ChoiceItem<RumourType>(rumour.toString(), rumour));
        }

        RumourType rumourChoice = gui.showChoiceDialog(null, "Select Lost City Rumour", "Cancel",
                              rumours);
        tile.getTileItemContainer().getLostCityRumour().setType(rumourChoice);
        final Tile serverTile = serverGame.getFreeColGameObject(tile.getId(),
                                                                Tile.class);
        serverTile.getTileItemContainer().getLostCityRumour()
            .setType(rumourChoice);
    }

    /**
     * Debug action to add a new unit to a tile.
     *
     * @param serverGame The server <code>Game</code> containing the tile.
     * @param tile The <code>Tile</code> to add to.
     */
    private void debugAddNewUnitToTile(final Game serverGame, Tile tile) {
        Specification spec = serverGame.getSpecification();
        List<ChoiceItem<UnitType>> uts = new ArrayList<ChoiceItem<UnitType>>();
        for (UnitType t : spec.getUnitTypeList()) {
            uts.add(new ChoiceItem<UnitType>(Messages.message(t.toString()
                                                              + ".name"), t));
        }
        UnitType unitChoice = gui.showChoiceDialog(null, "Select Unit Type", "Cancel", uts);
        if (unitChoice == null) return;

        Player player = freeColClient.getMyPlayer();
        Player serverPlayer = serverGame.getFreeColGameObject(player.getId(),
                                                              Player.class);
        Tile serverTile = serverGame.getFreeColGameObject(tile.getId(),
                                                          Tile.class);
        Unit carrier = null;
        if (!serverTile.isLand() && !unitChoice.isNaval()) {
            for (Unit u : serverTile.getUnitList()) {
                if (u.isNaval()
                    && u.getSpaceLeft() >= unitChoice.getSpaceTaken()) {
                    carrier = u;
                    break;
                }
            }
        }
        ServerUnit serverUnit = new ServerUnit(serverGame,
            (carrier != null) ? carrier : serverTile,
            serverPlayer, unitChoice);
        serverUnit.setMovesLeft(serverUnit.getInitialMovesLeft());
        Game game = freeColClient.getGame();
        Unit unit = new Unit(game,
            serverUnit.toXMLElement(DOMMessage.createNewDocument()));
        if (carrier == null) {
            tile.add(unit);
        } else {
            game.getFreeColGameObject(carrier.getId(), Unit.class).add(unit);
        }
        gui.setActiveUnit(unit);
        player.invalidateCanSeeTiles();
        gui.refresh();
    }

    /**
     * Debug action to reset the moves left of the units on a tile.
     *
     * @param serverGame The server <code>Game</code> containing the tile.
     * @param units The <code>Unit</code>s to reactivate.
     */
    private void debugResetUnitsMoves(final Game serverGame, List<Unit> units) {
        boolean first = true;
        for (Unit u : units) {
            Unit su = serverGame.getFreeColGameObject(u.getId(), Unit.class);
            u.setMovesLeft(u.getInitialMovesLeft());
            su.setMovesLeft(su.getInitialMovesLeft());
            if (first) {
                gui.setActiveUnit(u);
                first = false;
            }
        }
        gui.refresh();
    }

    /**
     * Debug action to add goods to a unit.
     *
     * @param serverGame The server <code>Game</code> containing the tile.
     * @param unit The <code>Unit</code> to add to.
     */
    private void debugAddGoodsToUnit(final Game serverGame, Unit unit) {
        Specification spec = serverGame.getSpecification();
        List<ChoiceItem<GoodsType>> gtl
            = new ArrayList<ChoiceItem<GoodsType>>();
        for (GoodsType t : spec.getGoodsTypeList()) {
            if (t.isFoodType() && t != spec.getPrimaryFoodType()) continue;
            gtl.add(new ChoiceItem<GoodsType>(Messages.message(t.toString() + ".name"),
                                              t));
        }
        GoodsType goodsType = gui.showChoiceDialog(null, "Select Goods Type", "Cancel", gtl);
        if (goodsType == null) return;
        String amount = gui.showInputDialog(null,
            StringTemplate.name("Select Goods Amount"), "20",
            "ok", "cancel", true);
        if (amount == null) return;
        int a;
        try {
            a = Integer.parseInt(amount);
        } catch (NumberFormatException nfe) {
            return;
        }
        GoodsType sGoodsType = spec.getGoodsType(goodsType.getId());
        GoodsContainer ugc = unit.getGoodsContainer();
        GoodsContainer sgc = serverGame.getFreeColGameObject(ugc.getId(),
                                                             GoodsContainer.class);
        ugc.setAmount(goodsType, a);
        sgc.setAmount(sGoodsType, a);
    }

    /**
     * Debug action to summarize information about a native settlement
     * that is normally hidden.
     *
     * @param serverGame The server <code>Game</code> containing the
     *     settlement.
     * @param sis The server version of the <code>IndianSettlement</code>
     *     to summarize.
     * @return A string summarizing the settlement.
     */
    private String debugSummarizeSettlement(final Game serverGame,
                                            final IndianSettlement sis) {
        Specification spec = serverGame.getSpecification();
        StringBuilder sb = new StringBuilder(sis.getName() + "\n");

        sb.append("\nAlarm\n");
        for (Player p : serverGame.getLiveEuropeanPlayers()) {
            Tension tension = sis.getAlarm(p);
            sb.append(Messages.message(p.getNationName())
                      + " " + ((tension == null) ? "(none)"
                               : Integer.toString(tension.getValue()))
                      + " " + Messages.message(sis.getShortAlarmLevelMessageId(p))
                      + " " + ((sis.hasSpokenToChief(p)) ? "(spoke to chief)"
                               : "")
                      + "\n");
        }

        sb.append("\nGoods Present\n");
        for (Goods goods : sis.getCompactGoods()) {
            sb.append(Messages.message(goods.getLabel(true)) + "\n");
        }

        sb.append("\nGoods Production\n");
        for (GoodsType type : spec.getGoodsTypeList()) {
            int prod = sis.getTotalProductionOf(type);
            if (prod > 0) {
                sb.append(Messages.message(type.getNameKey())
                          + " " + prod + "\n");
            }
        }

        sb.append("\nPrices (buy 1/100 / sell 1/100)\n");
        GoodsType[] wanted = sis.getWantedGoods();
        for (GoodsType type : spec.getGoodsTypeList()) {
            if (!type.isStorable()) continue;
            int i;
            for (i = wanted.length - 1; i >= 0; i--) {
                if (type == wanted[i]) break;
            }
            sb.append(Messages.message(type.getNameKey())
                      + ": " + sis.getPriceToBuy(type, 1)
                      + "/" + sis.getPriceToBuy(type, 100)
                      + " / " + sis.getPriceToSell(type, 1)
                      + "/" + sis.getPriceToSell(type, 100)
                      + ((i < 0) ? "" : " wanted[" + Integer.toString(i) + "]")
                      + "\n");
        }

        sb.append("\nOwned Units\n");
        for (Unit u : sis.getOwnedUnits()) {
            sb.append(u + "\n");
            sb.append("  at " + u.getTile() + "\n");
        }

        sb.append("\nTiles\n");
        for (Tile t : sis.getOwnedTiles()) {
            sb.append(t + "\n");
        }

        return sb.toString();
    }
}
