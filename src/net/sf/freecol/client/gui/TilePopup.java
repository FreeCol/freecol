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
import net.sf.freecol.client.gui.panel.ReportPanel;
import net.sf.freecol.common.debug.DebugUtils;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.CombatModel.CombatOdds;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Unit;


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
        Tile unitTile;
        if (activeUnit != null && (unitTile = activeUnit.getTile()) != null) {
            JMenuItem gotoMenuItem = null;
            if (activeUnit.isOffensiveUnit()
                && unitTile.isAdjacent(tile)
                && activeUnit.getMoveType(tile).isAttack()) {
                CombatOdds combatOdds = activeUnit.getGame().getCombatModel()
                    .calculateCombatOdds(activeUnit, tile.getDefendingUnit(activeUnit));

                String victoryPercent;
                // If attacking a settlement, the true odds are never
                // known because units may be hidden within
                if (tile.getSettlement() != null
                    || combatOdds.win == CombatOdds.UNKNOWN_ODDS) {
                    victoryPercent = "??";
                } else {
                    victoryPercent = Integer.toString((int)(combatOdds.win * 100));
                }
                gotoMenuItem = new JMenuItem(Messages.message(StringTemplate.
                         template("attackTileOdds").addName("%chance%", victoryPercent)));
            } else if (activeUnit.getSimpleMoveType(unitTile, tile).isLegal()) {
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
            }

            // Add move to Europe entry if the unit can do so
            if (unitTile == tile && activeUnit.hasHighSeasMove()) {
                JMenuItem europeMenuItem
                    = new JMenuItem(Messages.message(StringTemplate
                            .template("gotoEurope")));
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
            if (hasAnItem) addSeparator();
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

            lineCount += addUnit(currentMenu, currentUnit,
                !currentUnit.isUnderRepair(), false);
        }

        if (tile.getUnitCount() > 1) {
            if (moreUnits) addSeparator();
            JMenuItem activateAllItem = new JMenuItem(Messages
                .message(StringTemplate.template("activateAllUnits")));
            activateAllItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        Unit lastUnit = null;
                        for (Unit unit : tile.getUnitList()) {
                            freeColClient.getInGameController()
                                .clearOrders(unit);
                            lastUnit = unit;
                        }
                        gui.setActiveUnit(lastUnit);
                    }
                });
            add(activateAllItem);
        }

        if (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.MENUS)
            && freeColClient.getFreeColServer() != null) {
            addDebugItems(freeColClient, tile);
        }

        Component lastComponent = getComponent(getComponentCount() - 1);
        if (lastComponent instanceof JSeparator) {
            remove(lastComponent);
        }
    }

    /**
     * Build the debug entries for the TilePopup.
     *
     * @param freeColClient The <code>FreeColClient</code> where the menu
     *     is being created.
     * @param tile The <code>Tile</code> to build menu items for.
     */
    public void addDebugItems(final FreeColClient freeColClient, 
                              final Tile tile) {
        addSeparator();

        JMenu changeOwnership = new JMenu("Change ownership");
        changeOwnership.setOpaque(false);
        JMenu transportLists = new JMenu("Transport lists");
        transportLists.setOpaque(false);
        JMenu changeRole = new JMenu("Change role");
        changeRole.setOpaque(false);

        for (final Unit unit : tile.getUnitList()) {
            JMenuItem toMenuItem = new JMenuItem(unit.toString());
            toMenuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        DebugUtils.changeOwnership(freeColClient, unit);
                    }
                });
            changeOwnership.add(toMenuItem);

            if (unit.isCarrier()) {
                JMenuItem menuItem = new JMenuItem(unit.toString());
                menuItem.addActionListener(new ActionListener() {
                       public void actionPerformed(ActionEvent event) {
                           DebugUtils.displayMission(freeColClient, unit);
                       }
                    });
                transportLists.add(menuItem);
            }

            if (unit.isPerson()) {
                JMenuItem roleMenuItem = new JMenuItem(unit.toString());
                roleMenuItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent event) {
                            DebugUtils.changeRole(freeColClient, unit);
                        }
                    });
                changeRole.add(roleMenuItem);
            }
        }
        if (transportLists.getItemCount() > 0) add(transportLists);

        if (tile.getColony() != null) {
            if (changeOwnership.getItemCount() > 0) {
                changeOwnership.addSeparator();
            }
            JMenuItem toMenuItem = new JMenuItem(tile.getColony().toString());
            final Colony colony = tile.getColony();
            toMenuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        DebugUtils.changeOwnership(freeColClient, colony);
                    }
                });
            changeOwnership.add(toMenuItem);

            JMenuItem displayColonyPlan = new JMenuItem("Display Colony Plan");
            displayColonyPlan.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        DebugUtils.displayColonyPlan(freeColClient, colony);
                    }
                });
            add(displayColonyPlan);
        }
        if (tile.getIndianSettlement() != null) {
            JMenuItem displayGoods = new JMenuItem("Examine Settlement");
            final IndianSettlement is = tile.getIndianSettlement();
            displayGoods.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        DebugUtils.summarizeSettlement(freeColClient, is);
                    }
                });
            add(displayGoods);
        }
        if (changeOwnership.getItemCount() > 0) add(changeOwnership);
        if (changeRole.getItemCount() > 0) add(changeRole);

        if (tile.hasLostCityRumour()) {
            JMenuItem rumourItem = new JMenuItem("Set Lost City Rumour type");
            rumourItem.setOpaque(false);
            rumourItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        DebugUtils.setRumourType(freeColClient, tile);
                    }
                });
            add(rumourItem);
        }

        JMenuItem addu = new JMenuItem("Add unit");
        addu.setOpaque(false);
        addu.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    DebugUtils.addNewUnitToTile(freeColClient, tile);
                }
            });
        add(addu);

        if (!tile.isEmpty()) {
            JMenuItem adda = new JMenuItem("Reset moves");
            adda.setOpaque(false);
            final List<Unit> tileUnits = tile.getUnitList();
            adda.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        DebugUtils.resetMoves(freeColClient, tileUnits);
                    }
                });
            add(adda);
        }

        for (Unit u : tile.getUnitList()) {
            if (u.canCarryGoods() && u.hasSpaceLeft()) {
                JMenuItem addg = new JMenuItem("Add goods");
                addg.setOpaque(false);
                final Unit unit = u;
                addg.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent event) {
                            DebugUtils.addUnitGoods(freeColClient, unit);
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
                    DebugUtils.dumpTile(freeColClient, tile);
                }
            });
        add(dumpItem);
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
    private int addUnit(Container menu, final Unit unit, boolean enabled,
                        boolean indent) {
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
            occ = StringTemplate.key("model.unit.occupation."
                + unit.getState().toString().toLowerCase());
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
            dumpItem.setAction(new UnloadAction(freeColClient,
                    freeColClient.getInGameController(), gui, unit));
            menu.add(dumpItem);
            lineCount++;
        }
        hasAnItem = true;
        return lineCount;
    }

    /**
     * Adds a colony entry to this popup.
     *
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
        return getComponentCount() > 0;
    }
}
