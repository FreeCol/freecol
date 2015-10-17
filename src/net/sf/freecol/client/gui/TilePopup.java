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

package net.sf.freecol.client.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.gui.action.UnloadAction;
import net.sf.freecol.client.gui.panel.ReportPanel;
import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.common.debug.DebugUtils;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.util.LogBuilder;


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
    private final SwingGUI gui;
    private final Canvas canvas;
    private boolean hasAnItem = false;


    /**
     * The constructor that will insert the MenuItems.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param canvas The Canvas.
     * @param tile The <code>Tile</code> to create a popup for.
     *       The popup menu also appears near this <code>Tile</code>.
     */
    public TilePopup(final FreeColClient freeColClient, final Canvas canvas,
                     final Tile tile) {
        super(Messages.message(tile.getSimpleLabel()));

        this.freeColClient = freeColClient;
        this.gui = (SwingGUI)freeColClient.getGUI();
        this.canvas = canvas;

        final Player player = freeColClient.getMyPlayer();
        final Unit activeUnit = gui.getActiveUnit();
        final boolean owned = player != null && activeUnit != null
            && player.owns(activeUnit);
        Tile unitTile;
        if (activeUnit != null && owned
            && (unitTile = activeUnit.getTile()) != null) {
            JMenuItem gotoMenuItem = null;
            if (activeUnit.isOffensiveUnit()
                && unitTile.isAdjacent(tile)
                && activeUnit.getMoveType(tile).isAttack()) {
                gotoMenuItem = Utility.localizedMenuItem(activeUnit
                    .getCombatLabel(tile));
            } else if (activeUnit.getSimpleMoveType(unitTile, tile).isLegal()) {
                gotoMenuItem = Utility.localizedMenuItem("goToThisTile");
            }
            if (gotoMenuItem != null) {
                gotoMenuItem.addActionListener((ActionEvent ae) -> {
                        if (!freeColClient.currentPlayerIsMyPlayer()) return;
                        Tile currTile = activeUnit.getTile();
                        if (currTile == tile) return; // already at destination
                        freeColClient.getInGameController()
                            .goToTile(activeUnit, tile);
                        // if unit did not move, we should show the goto path
                        if (activeUnit.getTile() == currTile) {
                            canvas.updateCurrentPathForActiveUnit();
                        }
                    });
                add(gotoMenuItem);
            }

            // Add move to Europe entry if the unit can do so
            final InGameController igc = freeColClient.getInGameController();
            if (unitTile == tile && activeUnit.hasHighSeasMove()) {
                JMenuItem europeMenuItem = Utility.localizedMenuItem(StringTemplate
                    .template("goToEurope"));
                europeMenuItem.addActionListener((ActionEvent ae) -> {
                        if (!freeColClient.currentPlayerIsMyPlayer()) return;
                        igc.moveTo(activeUnit, player.getEurope());
                    });
                add(europeMenuItem);
                hasAnItem = true;
            }

            // Add state changes if present
            if (unitTile == tile) {
                JMenuItem ji = null;
                if (activeUnit.checkSetState(UnitState.ACTIVE)) {
                    ji = Utility.localizedMenuItem("activateUnit");
                    ji.addActionListener((ActionEvent ae) -> {
                            igc.changeState(activeUnit, Unit.UnitState.ACTIVE);
                        });
                    add(ji);
                    hasAnItem = true;
                }
                if (activeUnit.checkSetState(UnitState.FORTIFYING)) {
                    ji = Utility.localizedMenuItem("fortify");
                    ji.addActionListener((ActionEvent ae) -> {
                            igc.changeState(activeUnit, Unit.UnitState.FORTIFYING);
                        });
                    add(ji);
                    hasAnItem = true;
                }
                if (activeUnit.checkSetState(UnitState.SKIPPED)) {
                    ji = Utility.localizedMenuItem("skip");
                    ji.addActionListener((ActionEvent ae) -> {
                            igc.changeState(activeUnit, Unit.UnitState.SKIPPED);
                        });
                    add(ji);
                    hasAnItem = true;
                }
                if (activeUnit.canCarryTreasure()
                    && activeUnit.canCashInTreasureTrain()) {
                    ji = Utility.localizedMenuItem("cashInTreasureTrain");
                    ji.addActionListener((ActionEvent ae) -> {
                            igc.checkCashInTreasureTrain(activeUnit);
                        });
                    ji.setEnabled(true);
                    add(ji);
                    hasAnItem = true;
                }

                if (activeUnit.getDestination() != null) {
                    ji = Utility.localizedMenuItem("clearOrders");
                    ji.addActionListener((ActionEvent ae) -> {
                            igc.clearOrders(activeUnit);
                        });
                    add(ji);
                    hasAnItem = true;
                }
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

        if (tile.isExplored()) addTile(tile);
        addSeparator();

        int lineCount = 0;
        int maxUnits = UNIT_LINES_IN_FIRST_MENU;
        Container currentMenu = this;
        boolean moreUnits = false;
        Unit firstUnit = tile.getFirstUnit();
        for (Unit u : tile.getUnitList().stream()
                 .sorted(Unit.typeRoleComparator)
                 .collect(Collectors.toList())) {
            if (lineCount > maxUnits) {
                JMenu more = Utility.localizedMenu("more");
                more.setFont(more.getFont().deriveFont(Font.ITALIC));
                more.setOpaque(false);
                currentMenu.add(more);
                currentMenu = more;
                moreUnits = true;
                lineCount = 0;
                maxUnits = UNIT_LINES_IN_OTHER_MENUS;
            }
            lineCount += addUnit(currentMenu, u, !u.isDamaged(), false);
        }

        if (tile.getUnitCount() > 1 && player.owns(firstUnit)) {
            if (moreUnits) addSeparator();
            JMenuItem activateAllItem = Utility.localizedMenuItem(StringTemplate
                .template("activateAllUnits"));
            activateAllItem.addActionListener((ActionEvent ae) -> {
                    Unit lastUnit = null;
                    for (Unit unit : tile.getUnitList()) {
                        freeColClient.getInGameController().clearOrders(unit);
                        lastUnit = unit;
                    }
                    gui.setActiveUnit(lastUnit);
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
     * @param freeColClient The <code>FreeColClient</code> for the game.
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
            toMenuItem.addActionListener((ActionEvent ae) -> {
                    DebugUtils.changeOwnership(freeColClient, unit);
                });
            changeOwnership.add(toMenuItem);

            if (unit.isCarrier()) {
                JMenuItem menuItem = new JMenuItem(unit.toString());
                menuItem.addActionListener((ActionEvent ae) -> {
                        DebugUtils.displayMission(freeColClient, unit);
                    });
                transportLists.add(menuItem);
            }

            if (unit.isPerson()) {
                JMenuItem roleMenuItem = new JMenuItem(unit.toString());
                roleMenuItem.addActionListener((ActionEvent ae) -> {
                        DebugUtils.changeRole(freeColClient, unit);
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
            toMenuItem.addActionListener((ActionEvent ae) -> {
                    DebugUtils.changeOwnership(freeColClient, colony);
                });
            changeOwnership.add(toMenuItem);

            JMenuItem displayColonyPlan = new JMenuItem("Display Colony Plan");
            displayColonyPlan.addActionListener((ActionEvent ae) -> {
                    DebugUtils.displayColonyPlan(freeColClient, colony);
                });
            add(displayColonyPlan);

            JMenuItem applyDisaster = new JMenuItem("Apply Disaster");
            applyDisaster.addActionListener((ActionEvent ae) -> {
                    DebugUtils.applyDisaster(freeColClient, colony);
                });
            add(applyDisaster);
        }
        if (tile.getIndianSettlement() != null) {
            JMenuItem displayGoods = new JMenuItem("Examine Settlement");
            final IndianSettlement is = tile.getIndianSettlement();
            displayGoods.addActionListener((ActionEvent ae) -> {
                    DebugUtils.summarizeSettlement(freeColClient, is);
                });
            add(displayGoods);
        }
        if (changeOwnership.getItemCount() > 0) add(changeOwnership);
        if (changeRole.getItemCount() > 0) add(changeRole);

        if (tile.hasLostCityRumour()) {
            JMenuItem rumourItem = new JMenuItem("Set Lost City Rumour type");
            rumourItem.setOpaque(false);
            rumourItem.addActionListener((ActionEvent ae) -> {
                    DebugUtils.setRumourType(freeColClient, tile);
                });
            add(rumourItem);
        }

        JMenuItem addu = new JMenuItem("Add unit");
        addu.setOpaque(false);
        addu.addActionListener((ActionEvent ae) -> {
                DebugUtils.addNewUnitToTile(freeColClient, tile);
            });
        add(addu);

        if (!tile.isEmpty()) {
            JMenuItem adda = new JMenuItem("Reset moves");
            adda.setOpaque(false);
            final List<Unit> tileUnits = tile.getUnitList();
            adda.addActionListener((ActionEvent ae) -> {
                    DebugUtils.resetMoves(freeColClient, tileUnits);
                });
            add(adda);
        }

        final Unit activeUnit = gui.getActiveUnit();
        if (activeUnit != null && activeUnit.getTile() != null) {
            JMenuItem menuItem = new JMenuItem("Show search");
            menuItem.addActionListener((ActionEvent ae) -> {
                    if (!freeColClient.currentPlayerIsMyPlayer()) return;
                    Tile currTile = activeUnit.getTile();
                    if (currTile == tile) return;
                    final Map map = activeUnit.getGame().getMap();
                    LogBuilder lb = new LogBuilder(512);
                    PathNode path = map.findPath(activeUnit,
                        activeUnit.getTile(), tile, activeUnit.getCarrier(),
                        null, lb);
                    gui.showInformationMessage(lb.toString());
                    canvas.setCurrentPath(path);
                    gui.refresh();                        
                });
            add(menuItem);
        }

        for (Unit u : tile.getUnitList()) {
            if (u.canCarryGoods() && u.hasSpaceLeft()) {
                JMenuItem addg = new JMenuItem("Add goods");
                addg.setOpaque(false);
                final Unit unit = u;
                addg.addActionListener((ActionEvent ae) -> {
                        DebugUtils.addUnitGoods(freeColClient, unit);
                    });
                add(addg);
                break;
            }
        }

        JMenuItem dumpItem = new JMenuItem("Dump tile");
        dumpItem.setOpaque(false);
        dumpItem.addActionListener((ActionEvent ae) -> {
                DebugUtils.dumpTile(freeColClient, tile);
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
        StringTemplate occ
            = unit.getOccupationLabel(freeColClient.getMyPlayer(), true);
        String text = (indent ? "    " : "")
            + unit.getDescription(Unit.UnitLabelType.NATIONAL)
            + " (" + Messages.message(occ) + ")";
        JMenuItem menuItem = new JMenuItem(text);
        menuItem.setFont(FontLibrary.createFont(FontLibrary.FontType.NORMAL,
            FontLibrary.FontSize.TINY, Font.BOLD,
            gui.getImageLibrary().getScaleFactor()));
        menuItem.addActionListener((ActionEvent ae) -> {
                gui.setActiveUnit(unit);
            });
        if (indent) {
            menuItem.setFont(menuItem.getFont().deriveFont(Font.ITALIC));
        }
        menuItem.setEnabled(enabled);
        menu.add(menuItem);

        int lineCount = 1 + unit.getUnitList().stream()
            .mapToInt(u -> addUnit(menu, u, true, true)).sum();
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
            JMenuItem dumpItem = Utility.localizedMenuItem("dumpCargo");
            dumpItem.setAction(new UnloadAction(freeColClient, unit));
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
        StringTemplate name
            = colony.getLocationLabelFor(freeColClient.getMyPlayer());
        JMenuItem menuItem = Utility.localizedMenuItem(name);
        menuItem.setFont(FontLibrary.createFont(FontLibrary.FontType.NORMAL,
            FontLibrary.FontSize.TINY, Font.BOLD,
            gui.getImageLibrary().getScaleFactor()));
        menuItem.addActionListener((ActionEvent ae) -> {
                gui.showColonyPanel(colony, null);
            });

        add(menuItem);

        menuItem = Utility.localizedMenuItem("rename");
        menuItem.addActionListener((ActionEvent ae) -> {
                freeColClient.getInGameController().rename(colony);
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
        StringTemplate name
            = settlement.getLocationLabelFor(freeColClient.getMyPlayer());
        JMenuItem menuItem = Utility.localizedMenuItem(name);
        menuItem.setFont(FontLibrary.createFont(FontLibrary.FontType.NORMAL,
            FontLibrary.FontSize.TINY, Font.BOLD,
            gui.getImageLibrary().getScaleFactor()));
        menuItem.addActionListener((ActionEvent ae) -> {
                gui.showIndianSettlementPanel(settlement);
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
        JMenuItem menuItem = new JMenuItem(Messages.getName(tile));
        menuItem.addActionListener((ActionEvent ae) -> {
                gui.showTilePanel(tile);
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
