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

package net.sf.freecol.client.control;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.ChoiceItem;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.ColonyWas;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.DiplomaticTrade.TradeStatus;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.EuropeWas;
import net.sf.freecol.common.model.Event;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.HighScore;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Limit;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.LostCityRumour;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.ModelMessage.MessageType;
import net.sf.freecol.common.model.Nameable;
import net.sf.freecol.common.model.NationSummary;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.NoClaimReason;
import net.sf.freecol.common.model.Player.PlayerType;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.TradeRoute.Stop;
import net.sf.freecol.common.model.TransactionListener;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;
import net.sf.freecol.common.model.UnitWas;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.networking.NetworkConstants;
import net.sf.freecol.common.networking.ServerAPI;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.server.FreeColServer;


/**
 * The controller that will be used while the game is played.
 */
public final class InGameController implements NetworkConstants {

    private static final Logger logger = Logger.getLogger(InGameController.class.getName());

    private final FreeColClient freeColClient;

    private final short UNIT_LAST_MOVE_DELAY = 300;

    // Selecting next unit depends on mode--- either from the active list,
    // from the going-to list, or flush going-to and end the turn.
    private final int MODE_NEXT_ACTIVE_UNIT = 0;
    private final int MODE_EXECUTE_GOTO_ORDERS = 1;
    private final int MODE_END_TURN = 2;
    private int moveMode = MODE_NEXT_ACTIVE_UNIT;

    private int turnsPlayed = 0;

    private static FileFilter FSG_FILTER = new FileFilter() {
            public boolean accept(File file) {
                return file.isFile() && file.getName().endsWith(".fsg");
            }
        };

    /** A map of messages to be ignored. */
    private HashMap<String, Integer> messagesToIgnore
        = new HashMap<String, Integer>();

    /** The messages in the last turn report. */
    private ModelMessage[] turnReportMessages;

    private GUI gui;


    /**
     * The constructor to use.
     *
     * @param freeColClient The main controller.
     */
    public InGameController(FreeColClient freeColClient, GUI gui) {
        this.freeColClient = freeColClient;
        this.gui = gui;
        // TODO: fetch value of lastSaveGameFile from a persistent client value
        // lastSaveGameFile = new File(freeColClient.getClientOptions().getString(null));
    }


    // Public configuration.

    /**
     * Informs this controller that a game has been newly loaded.
     */
    public void setGameConnected () {
        turnsPlayed = 0;
        Player player = freeColClient.getMyPlayer();
        if (player != null) {
            player.refilterModelMessages(freeColClient.getClientOptions());
        }
        gui.updateMenuBar();
    }

    /**
     * Sets the "debug mode" to be active or not. Calls
     * {@link FreeColDebugger#setInDebugMode(boolean)} and reinitialize the
     * <code>FreeColMenuBar</code>.
     *
     * @param debug Set to <code>true</code> to enable debug mode.
     */
    public void setInDebugMode(boolean debug) {
        FreeColDebugger.setInDebugMode(debug);
        logger.info("Debug mode set to " + debug);
        gui.updateMenuBar();
    }


    // Private utilities

    /**
     * Meaningfully named access to the ServerAPI.
     *
     * @return The ServerAPI.
     */
    private ServerAPI askServer() {
        return freeColClient.askServer();
    }

    /**
     * Gets the specification for the current game.
     *
     * @return The current game specification.
     */
    private Specification getSpecification() {
        return freeColClient.getGame().getSpecification();
    }

    /**
     * Require that it is this client's player's turn.
     * Put up the notYourTurn message if not.
     *
     * @return True if it is our turn.
     */
    private boolean requireOurTurn() {
        if (!freeColClient.currentPlayerIsMyPlayer()) {
            if (freeColClient.isInGame()) {
                gui.showInformationMessage("notYourTurn");
            }
            return false;
        }
        return true;
    }

    /**
     * Check if an attack results in a transition from peace or cease fire to
     * war and, if so, warn the player.
     *
     * @param attacker The potential attacking <code>Unit</code>.
     * @param target The target <code>Tile</code>.
     * @return True to attack, false to abort.
     */
    private boolean confirmHostileAction(Unit attacker, Tile target) {
        if (attacker.hasAbility(Ability.PIRACY)) {
            // Privateers can attack and remain at peace
            return true;
        }

        Player enemy;
        if (target.getSettlement() != null) {
            enemy = target.getSettlement().getOwner();
        } else if (target == attacker.getTile()) {
            // Fortify on tile owned by another nation
            enemy = target.getOwner();
            if (enemy == null) return true;
        } else {
            Unit defender = target.getDefendingUnit(attacker);
            if (defender == null) {
                logger.warning("Attacking, but no defender - will try!");
                return true;
            }
            if (defender.hasAbility(Ability.PIRACY)) {
                // Privateers can be attacked and remain at peace
                return true;
            }
            enemy = defender.getOwner();
        }

        String messageID = null;
        switch (attacker.getOwner().getStance(enemy)) {
        case WAR:
            logger.finest("Player at war, no confirmation needed");
            return true;
        case UNCONTACTED: case PEACE:
            messageID = "model.diplomacy.attack.peace";
            break;
        case CEASE_FIRE:
            messageID = "model.diplomacy.attack.ceaseFire";
            break;
        case ALLIANCE:
            messageID = "model.diplomacy.attack.alliance";
            break;
        }
        return gui.showConfirmDialog(attacker.getTile(),
            StringTemplate.template(messageID)
            .addStringTemplate("%nation%", enemy.getNationName()),
            "model.diplomacy.attack.confirm", "cancel");
    }

    /**
     * Shows the pre-combat dialog if enabled, allowing the user to
     * view the odds and possibly cancel the attack.
     *
     * @param attacker The attacking <code>Unit</code>.
     * @param tile The target <code>Tile</code>.
     * @return True to attack, false to abort.
     */
    private boolean confirmPreCombat(Unit attacker, Tile tile) {
        if (freeColClient.getClientOptions()
            .getBoolean(ClientOptions.SHOW_PRECOMBAT)) {
            Settlement settlement = tile.getSettlement();
            // Don't tell the player how a settlement is defended!
            FreeColGameObject defender = (settlement != null) ? settlement
                : tile.getDefendingUnit(attacker);
            return gui.showPreCombatDialog(attacker, defender,
                                                       tile);
        }
        return true;
    }

    /**
     * Convenience function to find an adjacent settlement.  Intended
     * to be called in contexts where we are expecting a settlement to
     * be there, such as when handling a particular move type.
     *
     * @param tile The <code>Tile</code> to start at.
     * @param direction The <code>Direction</code> to step.
     * @return A settlement on the adjacent tile if any.
     */
    private Settlement getSettlementAt(Tile tile, Direction direction) {
        return tile.getNeighbourOrNull(direction).getSettlement();
    }

    /**
     * Convenience function to find the nation controlling an adjacent
     * settlement.  Intended to be called in contexts where we are
     * expecting a settlement or unit to be there, such as when
     * handling a particular move type.
     *
     * @param tile The <code>Tile</code> to start at.
     * @param direction The <code>Direction</code> to step.
     * @return The name of the nation controlling a settlement on the
     *         adjacent tile if any.
     */
    private StringTemplate getNationAt(Tile tile, Direction direction) {
        Tile newTile = tile.getNeighbourOrNull(direction);
        Player player = null;
        if (newTile.getSettlement() != null) {
            player = newTile.getSettlement().getOwner();
        } else if (newTile.getFirstUnit() != null) {
            player = newTile.getFirstUnit().getOwner();
        } else { // should not happen
            player = freeColClient.getGame().getUnknownEnemy();
        }
        return player.getNationName();
    }


    // Utilities to handle the transitions between the active unit,
    // execute-orders and end-turn controller states.

    /**
     * Actually do the goto orders operation.
     *
     * @return True if all goto orders have been performed and no units
     *     reached their destination and are free to move again.
     */
    private boolean doExecuteGotoOrders() {
        Player player = freeColClient.getMyPlayer();
        Unit active = gui.getActiveUnit();

        // Ensure the goto mode sticks.
        if (moveMode < MODE_EXECUTE_GOTO_ORDERS) {
            moveMode = MODE_EXECUTE_GOTO_ORDERS;
        }

        // Process all units.
        Unit stillActive = null;
        while (player.hasNextGoingToUnit()) {
            Unit unit = player.getNextGoingToUnit();
            gui.setActiveUnit(unit);

            // Give the player a chance to deal with any problems
            // shown in a popup before pressing on with more moves.
            if (gui.isShowingSubPanel()) {
                gui.requestFocusForSubPanel();
                return false;
            }

            // Move the unit as much as possible
            if (moveToDestination(unit)) stillActive = unit;
            nextModelMessage();
        }
        gui.setActiveUnit((stillActive != null) ? stillActive : active);
        return stillActive == null;
    }

    /**
     * Really end the turn.
     */
    private void doEndTurn() {
        // Clear active unit if any.
        gui.setActiveUnit(null);

        // Unskip all skipped, some may have been faked in-client.
        // Server-side skipped units are set active in csNewTurn.
        for (Unit unit : freeColClient.getMyPlayer().getUnits()) {
            if (unit.getState() == UnitState.SKIPPED) {
                unit.setState(UnitState.ACTIVE);
            }
        }

        // Restart the selection cycle.
        moveMode = MODE_NEXT_ACTIVE_UNIT;
        turnsPlayed++;

        // Clear outdated turn report messages.
        turnReportMessages = null;

        // Inform the server of end of turn.
        askServer().endTurn();
    }


    // Trade route support.

    /**
     * Follows a trade route, doing load/unload actions, moving the unit,
     * and updating the stop and destination.
     *
     * @param unit The <code>Unit</code> on the route.
     * @return True if the unit should keep moving, which can only
     *     happen if the trade route is found to be broken and the
     *     unit is thrown off it.
     */
    private boolean followTradeRoute(Unit unit) {
        Player player = unit.getOwner();
        TradeRoute tr = unit.getTradeRoute();
        String name = tr.getName();
        List<ModelMessage> messages = new ArrayList<ModelMessage>();
        boolean detailed = freeColClient.getClientOptions()
            .getBoolean(ClientOptions.SHOW_GOODS_MOVEMENT);
        final List<Stop> stops = tr.getStops();
        Stop stop;
        boolean result = false;
        int tries = stops.size();

        for (;;) {
            stop = unit.getStop();
            // Complain and return if the stop is no longer valid.
            if (!TradeRoute.isStopValid(unit, stop)) {
                messages.add(new ModelMessage(ModelMessage.MessageType.GOODS_MOVEMENT,
                        "traderoute.broken", unit)
                    .addName("%route%", name));
                clearOrders(unit);
                result = true;
                break;
            }

            // Is the unit at the stop already?
            boolean atStop;
            if (stop.getLocation() instanceof Europe) {
                atStop = unit.isInEurope();
            } else if (stop.getLocation() instanceof Colony) {
                atStop = unit.getTile() == stop.getLocation().getTile();
            } else {
                throw new IllegalStateException("Bogus stop location: "
                    + (FreeColGameObject) stop.getLocation());
            }
            if (atStop) {
                // Anything to unload?
                unloadUnitAtStop(unit, (detailed) ? messages : null);

                // Anything to load?
                loadUnitAtStop(unit, (detailed) ? messages : null);

                // If the un/load consumed the moves, break now before
                // updating the stop.  This allows next move to arrive
                // here again having taken a second shot at
                // un/loading, but this time should not have consumed
                // the moves.
                if (unit.getMovesLeft() <= 0) break;

                // Update the stop.
                int index = unit.validateCurrentStop();
                askServer().updateCurrentStop(unit);
                
                // Check if we have tried all the stops.  If so, this
                // means there is no work to do anywhere in the whole
                // trade route.  Skip the unit if so.
                // Otherwise optionally add messages notifying that a
                // stop has been skipped.
                int next = unit.validateCurrentStop();
                for (;;) {
                    if (++index >= stops.size()) index = 0;
                    tries--;
                    if (index == next) break;
                    if (detailed) {
                        Location loc = stops.get(index).getLocation();
                        messages.add(new ModelMessage(ModelMessage.MessageType.GOODS_MOVEMENT,
                                "traderoute.skipStop", unit)
                            .addName("%route%", name)
                            .addStringTemplate("%unit%",
                                Messages.getLabel(unit))
                            .addStringTemplate("%location%",
                                loc.getLocationNameFor(player)));
                    }
                }
                if (tries < 0) {
                    Location loc = stop.getLocation();
                    if (detailed) { 
                        int i = 0; // Drop skip-messages if skipping them all.
                        while (i < messages.size()) {
                            ModelMessage m = messages.get(i);
                            if (m.getId().equals("traderoute.skipStop")) {
                                messages.remove(i);
                            } else {
                                i++;
                            }
                        }
                        messages.add(new ModelMessage(ModelMessage.MessageType.GOODS_MOVEMENT,
                                "traderoute.noWork", unit)
                            .addName("%route%", name)
                            .addStringTemplate("%unit%",
                                Messages.getLabel(unit))
                            .addStringTemplate("%location%",
                                loc.getLocationNameFor(player)));
                    }
                    unit.setState(UnitState.SKIPPED);
                    break;
                }

                continue; // Stop was updated, loop.
            }

            // Not at stop, give up if no moves left.
            if (unit.getMovesLeft() <= 0
                || unit.getState() == UnitState.SKIPPED) {
                break;
            }

            // Find a path to the stop.  Skip if none.
            Location destination = stop.getLocation();
            PathNode path = unit.findFullPath(destination);
            if (path == null) {
                StringTemplate dest = destination.getLocationNameFor(player);
                messages.add(new ModelMessage(ModelMessage.MessageType.GOODS_MOVEMENT,
                        "traderoute.noPath", unit)
                    .addName("%route%", name)
                    .addStringTemplate("%unit%", Messages.getLabel(unit))
                    .addStringTemplate("%location%", dest));
                unit.setState(UnitState.SKIPPED);
                break;
            }

            // Try to follow the path.
            // Ignore the result, check for unload before returning.
            followPath(unit, path);
        }

        for (ModelMessage m : messages) player.addModelMessage(m);
        return result;
    }

    /**
     * Work out what goods to load onto a unit at a stop, and load them.
     *
     * @param unit The <code>Unit</code> to load.
     * @param messages An optional list of messages to update.
     * @return True if goods were loaded.
     */
    private boolean loadUnitAtStop(Unit unit, List<ModelMessage> messages) {
        // Copy the list of goods types to load at this stop.
        Stop stop = unit.getStop();
        List<GoodsType> goodsTypesToLoad
            = new ArrayList<GoodsType>(stop.getCargo());
        boolean ret = false;

        // First handle partial loads.
        // For each cargo the unit is already carrying, and which is
        // not to be unloaded at this stop, check if the cargo is
        // completely full and if not, try to fill to capacity.
        Colony colony = unit.getColony();
        Location loc = (unit.isInEurope()) ? unit.getOwner().getEurope()
            : colony;
        Game game = freeColClient.getGame();
        for (Goods goods : unit.getGoodsList()) {
            GoodsType type = goods.getType();
            int index, toLoad;
            if ((toLoad = GoodsContainer.CARGO_SIZE - goods.getAmount()) > 0
                && (index = goodsTypesToLoad.indexOf(type)) >= 0) {
                int present, atStop;
                if (unit.isInEurope()) {
                    present = atStop = Integer.MAX_VALUE;
                } else {
                    present = colony.getGoodsContainer().getGoodsCount(type);
                    atStop = colony.getExportAmount(type);
                }
                if (atStop > 0) {
                    Goods cargo = new Goods(game, loc, type,
                        Math.min(toLoad, atStop));
                    if (loadGoods(cargo, unit)) {
                        if (messages != null) {
                            messages.add(getLoadGoodsMessage(unit, type,
                                    cargo.getAmount(), present,
                                    atStop, toLoad));
                        }
                        ret = true;
                    }
                } else if (present > 0) {
                    if (messages != null) {
                        messages.add(getLoadGoodsMessage(unit, type,
                                0, present, 0, toLoad));
                    }
                }
                // Do not try to load this goods type again.  Either
                // it has already succeeded, or it can not ever
                // succeed because there is nothing available.
                goodsTypesToLoad.remove(index);
            }
        }

        // Then fill any remaining empty cargo slots.
        for (GoodsType type : goodsTypesToLoad) {
            if (unit.getSpaceLeft() <= 0) break; // Full
            int toLoad = GoodsContainer.CARGO_SIZE;
            int present, atStop;
            if (unit.isInEurope()) {
                present = atStop = Integer.MAX_VALUE;
            } else {
                present = colony.getGoodsContainer().getGoodsCount(type);
                atStop = colony.getExportAmount(type);
            }
            if (atStop > 0) {
                Goods cargo = new Goods(game, loc, type,
                    Math.min(toLoad, atStop));
                if (loadGoods(cargo, unit)) {
                    if (messages != null) {
                        messages.add(getLoadGoodsMessage(unit, type,
                                cargo.getAmount(), present, atStop, toLoad));
                    }
                    ret = true;
                }
            } else if (present > 0) {
                if (messages != null) {
                    messages.add(getLoadGoodsMessage(unit, type,
                            0, present, 0, toLoad));
                }
            }
        }

        return ret;
    }

    /**
     * Gets a message describing a goods loading.
     *
     * @param unit The <code>Unit</code> that is loading.
     * @param type The <code>GoodsType</code> the type of goods being loaded.
     * @param amount The amount of goods loaded.
     * @param present The amount of goods already at the location.
     * @param atStop The amount of goods available to load.
     * @param toLoad The amount of goods the unit could load.
     * @return A model message describing the load.
     */
    private ModelMessage getLoadGoodsMessage(Unit unit, GoodsType type,
                                             int amount, int present,
                                             int atStop, int toLoad) {
        Player player = unit.getOwner();
        Location loc = unit.getLocation();
        String route = unit.getTradeRoute().getName();
        String key = null;
        int more = 0;

        if (toLoad < atStop) {
            key = "traderoute.loadImportLimited";
            more = atStop - toLoad;
        } else if (present > atStop && toLoad > atStop) {
            key = "traderoute.loadExportLimited";
            more = present - atStop;
        } else {
            key = "traderoute.load";
        }
        return new ModelMessage(ModelMessage.MessageType.GOODS_MOVEMENT, key, unit)
            .addName("%route%", route)
            .addStringTemplate("%unit%", Messages.getLabel(unit))
            .addStringTemplate("%location%", loc.getLocationNameFor(player))
            .addAmount("%amount%", amount)
            .add("%goods%", type.getNameKey())
            .addAmount("%more%", more);
    }

    /**
     * Work out what goods to unload from a unit at a stop, and unload them.
     *
     * @param unit The <code>Unit</code> to unload.
     * @param messages A list of messages to update.
     * @return True if something was unloaded.
     */
    private boolean unloadUnitAtStop(Unit unit, List<ModelMessage> messages) {
        Colony colony = unit.getColony();
        Stop stop = unit.getStop();
        final List<GoodsType> goodsTypesToLoad = stop.getCargo();
        boolean ret = false;

        // Unload everything that is on the carrier but not listed to
        // be loaded at this stop.
        Game game = freeColClient.getGame();
        for (Goods goods : new ArrayList<Goods>(unit.getGoodsList())) {
            GoodsType type = goods.getType();
            if (goodsTypesToLoad.contains(type)) continue; // Keep this cargo.

            int atStop = (colony == null) ? Integer.MAX_VALUE // Europe
                : colony.getImportAmount(type);
            int toUnload = goods.getAmount();
            if (toUnload > atStop) {
                String locName = colony.getName();
                String overflow = Integer.toString(toUnload - atStop);
                int option = freeColClient.getClientOptions()
                    .getInteger(ClientOptions.UNLOAD_OVERFLOW_RESPONSE);
                switch (option) {
                case ClientOptions.UNLOAD_OVERFLOW_RESPONSE_ASK:
                    StringTemplate template
                        = StringTemplate.template("traderoute.warehouseCapacity")
                        .addStringTemplate("%unit%", Messages.getLabel(unit))
                        .addName("%colony%", locName)
                        .addName("%amount%", overflow)
                        .add("%goods%", goods.getNameKey());
                    if (!gui.showConfirmDialog(colony.getTile(), template,
                            "yes", "no")) {
                        toUnload = atStop;
                    }
                    break;
                case ClientOptions.UNLOAD_OVERFLOW_RESPONSE_NEVER:
                    toUnload = atStop;
                    break;
                case ClientOptions.UNLOAD_OVERFLOW_RESPONSE_ALWAYS:
                    break;
                default:
                    logger.warning("Illegal UNLOAD_OVERFLOW_RESPONSE: "
                        + Integer.toString(option));
                    break;
                }
            }

            // Try to unload.
            Goods cargo = (goods.getAmount() == toUnload) ? goods
                : new Goods(game, unit, type, toUnload);
            if (unloadGoods(cargo, unit, colony)) {
                if (messages != null) {
                    messages.add(getUnloadGoodsMessage(unit, type,
                            cargo.getAmount(), atStop, goods.getAmount(),
                            toUnload));
                }
                ret = true;
            }
        }

        return ret;
    }

    /**
     * Gets a message describing a goods unloading.
     *
     * @param unit The <code>Unit</code> that is unloading.
     * @param type The <code>GoodsType</code> the type of goods being unloaded.
     * @param amount The amount of goods unloaded.
     * @param present The amount of goods already carried by the unit.
     * @param atStop The amount of goods available to unload.
     * @param toUnload The amount of goods actually unloaded.
     * @return A model message describing the unload.
     */
    private ModelMessage getUnloadGoodsMessage(Unit unit, GoodsType type,
                                               int amount, int atStop,
                                               int present, int toUnload) {
        String key = null;
        int overflow = 0;

        if (present == toUnload) {
            key = "traderoute.unload";
        } else if (toUnload > atStop) {
            key = "traderoute.overflow";
            overflow = toUnload - atStop;
        } else {
            key = "traderoute.nounload";
            overflow = present - atStop;
        }

        StringTemplate loc = unit.getLocation().getLocationNameFor(unit.getOwner());
        return new ModelMessage(ModelMessage.MessageType.GOODS_MOVEMENT, key,
            unit)
            .addName("%route%", unit.getTradeRoute().getName())
            .addStringTemplate("%unit%", Messages.getLabel(unit))
            .addStringTemplate("%location%", loc)
            .addAmount("%amount%", amount)
            .addAmount("%overflow%", overflow)
            .add("%goods%", type.getNameKey());
    }


    // Server access routines called from multiple places.

    /**
     * Claim a tile.
     *
     * @param player The <code>Player</code> that is claiming.
     * @param tile The <code>Tile</code> to claim.
     * @param claimant The <code>Unit</code> or <code>Colony</code> claiming.
     * @param price The price required.
     * @param offer An offer to pay.
     * @return True if the claim succeeded.
     */
    private boolean claimTile(Player player, Tile tile,
                              FreeColGameObject claimant,
                              int price, int offer) {
        Player owner = tile.getOwner();
        if (price < 0) return false; // not for sale
        if (price > 0) { // for sale by natives
            if (offer >= price) { // offered more than enough
                price = offer;
            } else if (offer < 0) { // plan to steal
                price = NetworkConstants.STEAL_LAND;
            } else {
                boolean canAccept = player.checkGold(price);
                switch (gui.showClaimDialog(tile, player, price,
                        owner, canAccept)) {
                case CANCEL:
                    return false;
                case ACCEPT: // accepted price
                    break;
                case STEAL:
                    price = NetworkConstants.STEAL_LAND;
                    break;
                default:
                    throw new IllegalStateException("showClaimDialog fail");
                }
            }
        } // else price == 0 and we can just proceed

        // Ask the server
        if (askServer().claimLand(tile, claimant, price)
            && player.owns(tile)) {
            gui.updateMenuBar();
            return true;
        }
        return false;
    }

    /**
     * Emigrate a unit from Europe.
     *
     * @param player The <code>Player</code> that owns the unit.
     * @param slot The slot to emigrate from.
     */
    private void emigrate(Player player, int slot) {
        Europe europe = player.getEurope();
        EuropeWas europeWas = new EuropeWas(europe);
        if (askServer().emigrate(slot)) {
            europeWas.fireChanges();
            gui.updateMenuBar();
        }
    }

    /**
     * Follow a path.
     *
     * @param unit The <code>Unit</code> to move.
     * @param path The path to follow.
     * @return True if the unit has completed the path and can move further.
     */
    private boolean followPath(Unit unit, PathNode path) {
        // Traverse the path to the destination.
        for (; path != null; path = path.next) {
            if (path.getLocation() == unit.getLocation()) continue;

            if (path.getLocation() instanceof Europe) {
                if (unit.getTile() != null
                    && unit.getTile().isDirectlyHighSeasConnected()) {
                    moveTo(unit, path.getLocation());
                    return false;
                } else {
                    logger.warning("Can not move to Europe from "
                        + unit.getLocation()
                        + " on path: " + path.fullPathToString());
                }
            } else {
                if (path.getDirection() != null) {
                    if (!moveDirection(unit, path.getDirection(), false)) {
                        return false;
                    }
                } else {
                    logger.warning("Can not move to tile from "
                        + unit.getLocation()
                        + " on path: " + path.fullPathToString());
                }
            }
        }
        return true;
    }

    /**
     * Load some goods onto a carrier.
     *
     * @param goods The <code>Goods</code> to load.
     * @param carrier The <code>Unit</code> to load onto.
     * @return True if the load succeeded.
     */
    private boolean loadGoods(Goods goods, Unit carrier) {
        if (carrier.isInEurope() && goods.getLocation() instanceof Europe) {
            if (!carrier.getOwner().canTrade(goods)) return false;
            return buyGoods(goods.getType(), goods.getAmount(), carrier);
        }
        GoodsType type = goods.getType();
        GoodsContainer container = carrier.getGoodsContainer();
        int oldAmount = container.getGoodsCount(type);
        UnitWas unitWas = new UnitWas(carrier);
        Colony colony = carrier.getColony();
        ColonyWas colonyWas = (colony == null) ? null : new ColonyWas(colony);
        if (askServer().loadCargo(goods, carrier)
            && container.getGoodsCount(type) != oldAmount) {
            if (colonyWas != null) colonyWas.fireChanges();
            unitWas.fireChanges();
            return true;
        }
        return false;
    }

    /**
     * Unload some goods from a carrier.
     *
     * @param goods The <code>Goods</code> to unload.
     * @param carrier The <code>Unit</code> carrying the goods.
     * @param colony The <code>Colony</code> to unload to,
     *               or null if unloading in Europe.
     * @return True if the unload succeeded.
     */
    private boolean unloadGoods(Goods goods, Unit carrier, Colony colony) {
        if (colony == null && carrier.isInEurope()
            && carrier.getOwner().canTrade(goods)) return sellGoods(goods);

        GoodsType type = goods.getType();
        GoodsContainer container = carrier.getGoodsContainer();
        int oldAmount = container.getGoodsCount(type);
        ColonyWas colonyWas = (colony == null) ? null : new ColonyWas(colony);
        UnitWas unitWas = new UnitWas(carrier);
        if (askServer().unloadCargo(goods)
            && container.getGoodsCount(type) != oldAmount) {
            if (colonyWas != null) colonyWas.fireChanges();
            unitWas.fireChanges();
            return true;
        }
        return false;
    }


    // Utilities connected with saving the game

    /**
     * Creates at least one autosave game file of the currently played
     * game in the autosave directory. Does nothing if there is no
     * game running.
     *
     */
    private void autosave_game () {
        Game game = freeColClient.getGame();
        if (game == null) return;

        // unconditional save per round (fix file "last-turn")
        String autosave_text
            = Messages.message("clientOptions.savegames.autosave.fileprefix");
        String filename = autosave_text + "-"
            + Messages.message("clientOptions.savegames.autosave.lastturn")
            + ".fsg";
        String beforeFilename = autosave_text + "-"
            + Messages.message("clientOptions.savegames.autosave.beforelastturn")
            + ".fsg";
        File autosaveDir = FreeColDirectories.getAutosaveDirectory();
        File saveGameFile = new File(autosaveDir, filename);
        File beforeSaveFile = new File(autosaveDir, beforeFilename);

        // if "last-turn" file exists, shift it to "before-last-turn" file
        if (saveGameFile.exists()) {
           beforeSaveFile.delete();
           saveGameFile.renameTo(beforeSaveFile);
        }
        saveGame(saveGameFile);

        // conditional save after user-set period
        ClientOptions options = freeColClient.getClientOptions();
        int savegamePeriod = options.getInteger(ClientOptions.AUTOSAVE_PERIOD);
        int turnNumber = game.getTurn().getNumber();
        if (savegamePeriod <= 1
            || (savegamePeriod != 0 && turnNumber % savegamePeriod == 0)) {
            Player player = game.getCurrentPlayer();
            String playerNation = player == null ? ""
                : Messages.message(player.getNation().getNameKey());
            String gid = Integer.toHexString(game.getUUID().hashCode());
            filename = Messages.message("clientOptions.savegames.autosave.fileprefix")
                + '-' + gid  + "_" + playerNation
                + "_" + getSaveGameString(game.getTurn()) + ".fsg";
            saveGameFile = new File(autosaveDir, filename);
            saveGame(saveGameFile);
        }
    }

    /**
     * Returns a string representation of the given turn suitable for
     * savegame files.
     *
     * @param turn a <code>Turn</code> value
     * @return A string with the format: "<i>[season] year</i>".
     *         Examples: "1602_1_Spring", "1503"...
     */
    private String getSaveGameString(Turn turn) {
        int year = turn.getYear();
        switch (turn.getSeason()) {
        case SPRING:
            return Integer.toString(year) + "_1_" + Messages.message("spring");
        case AUTUMN:
            return Integer.toString(year) + "_2_" + Messages.message("autumn");
        case YEAR:
        default:
            return Integer.toString(year);
        }
    }

    /**
     * Gets the most recently saved game file, or <b>null</b>.  (This
     * may be either from a recent arbitrary user operation or an
     * autosave function.)
     *
     *  @return The recent save game file
     */
    public File getLastSaveGameFile() {
        File lastSave = null;
        for (File directory : new File[] {
                FreeColDirectories.getSaveDirectory(), FreeColDirectories.getAutosaveDirectory() }) {
            for (File savegame : directory.listFiles(FSG_FILTER)) {
                if (lastSave == null
                    || savegame.lastModified() > lastSave.lastModified()) {
                    lastSave = savegame;
                }
            }
        }
        return lastSave;
    }

    /**
     * Opens a dialog where the user should specify the filename and
     * loads the game.
     */
    public void loadGame() {
        File file = gui.showLoadDialog(FreeColDirectories.getSaveDirectory());
        if (file == null) return;
        if (!file.isFile()) {
            gui.errorMessage("fileNotFound");
            return;
        }
        if (freeColClient.isInGame()
            && !gui.showConfirmDialog("stopCurrentGame.text",
                "stopCurrentGame.yes", "stopCurrentGame.no")) {
            return;
        }

        freeColClient.getConnectController().quitGame(true);
        gui.setActiveUnit(null);
        gui.removeInGameComponents();
        freeColClient.getConnectController().loadGame(file);
    }

    /**
     * Reloads a game state which was previously saved via
     * <code>quicksaveGame</code>.
     *
     * @return boolean <b>true</b> if and only if a game was loaded
     */
    public boolean quickReload () {
        Game game = freeColClient.getGame();
        if (game != null) {
            String gid = Integer.toHexString(game.getUUID().hashCode());
            String filename = "quicksave-" + gid + ".fsg";
            File file = new File(FreeColDirectories.getAutosaveDirectory(), filename);
            if (file.isFile()) {
                // ask user to confirm reload action
                boolean ok = true; // canvas.showConfirmDialog(gid, gid, filename);

                // perform loading game state if answer == ok
                if (ok) {
                    freeColClient.getConnectController().quitGame(true);
                    gui.removeInGameComponents();
                    freeColClient.getConnectController().loadGame(file);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Opens a dialog where the user should specify the filename and
     * saves the game.
     *
     * @return True if the game was saved.
     */
    public boolean saveGame() {
        Player player = freeColClient.getMyPlayer();
        Game game = freeColClient.getGame();
        String gid = Integer.toHexString(game.getUUID().hashCode());
        String fileName = /* player.getName() + "_" */ gid + "_"
            + Messages.message(player.getNationName()) + "_"
            + getSaveGameString(game.getTurn());
        fileName = fileName.replaceAll(" ", "_");

        if (freeColClient.canSaveCurrentGame()) {
            final File file
                = gui.showSaveDialog(FreeColDirectories.getSaveDirectory(), fileName);
            if (file != null) {
                FreeColDirectories.setSaveDirectory(file.getParentFile());
                return saveGame(file);
            }
        }
        return false;
    }

    /**
     * Saves the game to the given file.
     *
     * @param file The <code>File</code>.
     * @return True if the game was saved.
     */
    public boolean saveGame(final File file) {
        FreeColServer server = freeColClient.getFreeColServer();
        boolean result = false;
        gui.showStatusPanel(Messages.message("status.savingGame"));
        try {
            server.setActiveUnit(gui.getActiveUnit());
            server.saveGame(file, freeColClient.getMyPlayer().getName(),
                freeColClient.getClientOptions());
            gui.closeStatusPanel();
            result = true;
        } catch (IOException e) {
            gui.errorMessage("couldNotSaveGame");
        }
        gui.requestFocusInWindow();
        return result;
    }

    /**
     * Saves the game to a fix-named file in the autosave directory, which may
     * be used for quick-reload.
     *
     * @return boolean <b>true</b> if and only if the game was saved
     */
    public boolean quicksaveGame () {
        Game game = freeColClient.getGame();
        if (game != null) {
            String gid = Integer.toHexString(game.getUUID().hashCode());
            String filename = "quicksave-" + gid + ".fsg";
            File file = new File(FreeColDirectories.getAutosaveDirectory(), filename);
            return saveGame(file);
        }
        return false;
    }


    // Utilities for message handling.

    /**
     * Provides an opportunity to filter the messages delivered to the canvas.
     *
     * @param message the message that is candidate for delivery to the canvas
     * @return true if the message should be delivered
     */
    private boolean shouldAllowMessage(ModelMessage message) {
        BooleanOption option = freeColClient.getClientOptions()
            .getBooleanOption(message);
        return (option == null) ? true : option.getValue();
    }

    private synchronized void startIgnoringMessage(String key, int turn) {
        logger.finer("Ignoring model message with key " + key);
        messagesToIgnore.put(key, new Integer(turn));
    }

    private synchronized void stopIgnoringMessage(String key) {
        logger.finer("Removing model message with key " + key
            + " from ignored messages.");
        messagesToIgnore.remove(key);
    }

    private synchronized Integer getTurnForMessageIgnored(String key) {
        return messagesToIgnore.get(key);
    }

    /**
     * Ignore this ModelMessage from now on until it is not generated
     * in a turn.
     *
     * @param message a <code>ModelMessage</code> value
     * @param flag whether to ignore the ModelMessage or not
     */
    public synchronized void ignoreMessage(ModelMessage message, boolean flag) {
        String key = message.getSourceId();
        if (message.getTemplateType() == StringTemplate.TemplateType.TEMPLATE) {
            for (String otherkey : message.getKeys()) {
                if ("%goods%".equals(otherkey)) {
                    key += otherkey;
                }
                break;
            }
        }
        if (flag) {
            startIgnoringMessage(key,
                freeColClient.getGame().getTurn().getNumber());
        } else {
            stopIgnoringMessage(key);
        }
    }

    /**
     * Displays the messages in the current turn report.
     */
    public void displayTurnReportMessages() {
        gui.showReportTurnPanel(turnReportMessages);
    }

    /**
     * Displays pending <code>ModelMessage</code>s.
     *
     * @param allMessages Display all messages or just the undisplayed ones.
     */
    public void displayModelMessages(boolean allMessages) {
        displayModelMessages(allMessages, false);
    }

    /**
     * Displays pending <code>ModelMessage</code>s.
     *
     * @param allMessages Display all messages or just the undisplayed ones.
     * @param endOfTurn Use a turn report panel if necessary.
     */
    public void displayModelMessages(final boolean allMessages,
                                     final boolean endOfTurn) {
        Player player = freeColClient.getMyPlayer();
        int thisTurn = freeColClient.getGame().getTurn().getNumber();
        final ArrayList<ModelMessage> messages = new ArrayList<ModelMessage>();
        for (ModelMessage m : ((allMessages) ? player.getModelMessages()
                : player.getNewModelMessages())) {
            if (shouldAllowMessage(m)) {
                if (m.getMessageType() == MessageType.WAREHOUSE_CAPACITY) {
                    String key = m.getSourceId();
                    switch (m.getTemplateType()) {
                    case TEMPLATE:
                        for (String otherkey : m.getKeys()) {
                            if ("%goods%".equals(otherkey)) {
                                key += otherkey;
                                break;
                            }
                        }
                        break;
                    default:
                        break;
                    }

                    Integer turn = getTurnForMessageIgnored(key);
                    if (turn != null && turn.intValue() == thisTurn - 1) {
                        startIgnoringMessage(key, thisTurn);
                        m.setBeenDisplayed(true);
                        continue;
                    }
                }
                messages.add(m);
            }

            // flag all messages delivered as "beenDisplayed".
            m.setBeenDisplayed(true);
        }

        for (Entry<String, Integer> entry : messagesToIgnore.entrySet()) {
            if (entry.getValue().intValue() < thisTurn - 1) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer("Removing old model message with key "
                        + entry.getKey() + " from ignored messages.");
                }
                stopIgnoringMessage(entry.getKey());
            }
        }

        if (messages.size() > 0) {
            Runnable uiTask;
            if (endOfTurn) {
                turnReportMessages = messages.toArray(new ModelMessage[0]);
                uiTask = new Runnable() {
                        public void run() {
                            displayTurnReportMessages();
                        }
                    };
            } else {
                final ModelMessage[] a = messages.toArray(new ModelMessage[0]);
                uiTask = new Runnable() {
                        public void run() {
                            gui.showModelMessages(a);
                        }
                    };
            }
            freeColClient.updateActions();
            if (SwingUtilities.isEventDispatchThread()) {
                uiTask.run();
            } else {
                try {
                    SwingUtilities.invokeAndWait(uiTask);
                } catch (InterruptedException e) {
                    logger.log(Level.WARNING, "Message display", e);
                } catch (InvocationTargetException e) {
                    logger.log(Level.WARNING, "Message display", e);
                }
            }
        }
    }

    /**
     * Displays the next <code>ModelMessage</code>.
     */
    public void nextModelMessage() {
        displayModelMessages(false);
    }


    // All the routines from here on are called from actions.
    // They (usually) then make requests to the server.

    /**
     * Abandon a colony with no units.
     *
     * @param colony The <code>Colony</code> to be abandoned.
     */
    public void abandonColony(Colony colony) {
        if (!requireOurTurn()) return;
        Player player = freeColClient.getMyPlayer();

        // Sanity check
        if (colony == null || !player.owns(colony)
            || colony.getUnitCount() > 0) {
            throw new IllegalStateException("Abandon bogus colony");
        }

        // Proceed to abandon
        Tile tile = colony.getTile();
        if (askServer().abandonColony(colony)
            && tile.getSettlement() == null) {
            player.invalidateCanSeeTiles();
            gui.setActiveUnit(null);
            gui.setSelectedTile(tile, false);
        }
    }

    /**
     * Assigns a unit to a teacher <code>Unit</code>.
     *
     * @param student an <code>Unit</code> value
     * @param teacher an <code>Unit</code> value
     */
    public void assignTeacher(Unit student, Unit teacher) {
        Player player = freeColClient.getMyPlayer();
        if (!requireOurTurn()
            || student == null
            || !player.owns(student)
            || student.getColony() == null
            || !(student.getLocation() instanceof WorkLocation)
            || teacher == null
            || !player.owns(teacher)
            || !student.canBeStudent(teacher)
            || teacher.getColony() == null
            || student.getColony() != teacher.getColony()
            || !teacher.getColony().canTrain(teacher)) {
            return;
        }

        askServer().assignTeacher(student, teacher);
    }

    /**
     * Assigns a trade route to a unit using the trade route dialog.
     *
     * @param unit The <code>Unit</code> to assign a trade route to.
     */
    public void assignTradeRoute(Unit unit) {
        TradeRoute oldRoute = unit.getTradeRoute();
        if (!gui.showTradeRouteDialog(unit)) return; // Cancelled

        // Delete or deassign of trade route removes the route from the unit
        TradeRoute route = unit.getTradeRoute();
        if (oldRoute != route) assignTradeRoute(unit, route);
    }

    /**
     * Assigns a trade route to a unit.
     *
     * @param unit The <code>Unit</code> to assign a trade route to.
     * @param tradeRoute The <code>TradeRoute</code> to assign.
     */
    public void assignTradeRoute(Unit unit, TradeRoute tradeRoute) {
        if (askServer().assignTradeRoute(unit, tradeRoute)) {
            if ((tradeRoute = unit.getTradeRoute()) != null
                && freeColClient.currentPlayerIsMyPlayer()) {
                moveToDestination(unit);
            }
        }
    }

    /**
     * Boards a specified unit onto a carrier.
     * The carrier must be at the same location as the boarding unit.
     *
     * @param unit The <code>Unit</code> which is to board the carrier.
     * @param carrier The carrier to board.
     * @return True if the unit boards the carrier.
     */
    public boolean boardShip(Unit unit, Unit carrier) {
        if (!requireOurTurn()) return false;

        // Sanity checks.
        if (unit == null) {
            logger.warning("unit == null");
            return false;
        }
        if (carrier == null) {
            logger.warning("Trying to load onto a non-existent carrier.");
            return false;
        }
        if (unit.isNaval()) {
            logger.warning("Trying to load a ship onto another carrier.");
            return false;
        }
        if (unit.isInEurope() != carrier.isInEurope()
            || unit.getTile() != carrier.getTile()) {
            logger.warning("Unit and carrier are not co-located.");
            return false;
        }

        // Proceed to board
        UnitWas unitWas = new UnitWas(unit);
        if (askServer().embark(unit, carrier, null)
            && unit.getLocation() == carrier) {
            gui.playSound("sound.event.loadCargo");
            unitWas.fireChanges();
            nextActiveUnit();
            return true;
        }
        return false;
    }

    /**
     * Use the active unit to build a colony.
     */
    public void buildColony() {
        if (!requireOurTurn()) return;
        Player player = freeColClient.getMyPlayer();

        // Check unit can build, and is on the map.
        // Show the colony warnings if required.
        Unit unit = gui.getActiveUnit();
        if (unit == null) {
            return;
        } else if (!unit.canBuildColony()) {
            gui.showInformationMessage(unit,
                StringTemplate.template("buildColony.badUnit")
                .addName("%unit%", unit.getName()));
            return;
        }
        Tile tile = unit.getTile();
        if (tile.getColony() != null) {
            askServer().joinColony(unit, tile.getColony());
            return;
        }
        NoClaimReason reason = player.canClaimToFoundSettlementReason(tile);
        if (reason != NoClaimReason.NONE
            && reason != NoClaimReason.NATIVES) {
            gui.showInformationMessage("noClaimReason."
                + reason.toString().toLowerCase(Locale.US));
            return;
        }

        if (freeColClient.getClientOptions()
            .getBoolean(ClientOptions.SHOW_COLONY_WARNINGS)
            && !buildColonyShowWarnings(tile, unit)) {
            return;
        }

        if (tile.getOwner() != null && !player.owns(tile)) {
            // Claim tile from other owners before founding a settlement.
            // Only native owners that we can steal, buy from, or use a
            // bonus center tile exception should be possible by this point.
            if (!claimTile(player, tile, unit, player.getLandPrice(tile), 0))
                return;
            // One more check that founding can now proceed.
            if (!player.canClaimToFoundSettlement(tile)) return;
        }

        // Get and check the name.
        String name = player.getSettlementName(null);
        name = gui.showInputDialog(tile,
            StringTemplate.key("nameColony.text"), name,
            "nameColony.yes", "nameColony.no", true);
        if (name == null) return; // User cancelled, 0-length invalid.

        if (player.getSettlement(name) != null) {
            // Colony name must be unique.
            gui.showInformationMessage(tile,
                StringTemplate.template("nameColony.notUnique")
                .addName("%name%", name));
            return;
        }

        if (askServer().buildColony(name, unit)
            && tile.getSettlement() != null) {
            player.invalidateCanSeeTiles();
            gui.playSound("sound.event.buildingComplete");
            gui.setActiveUnit(null);
            gui.setSelectedTile(tile, false);

            // Check units present for treasure cash-in as they are now
            // suddenly in-colony.
            for (Unit unitInTile : tile.getUnitList()) {
                checkCashInTreasureTrain(unitInTile);
            }
        }
    }

    /**
     * A colony is proposed to be built.  Show warnings if this has
     * disadvantages.
     *
     * @param tile The <code>Tile</code> on which the colony is to be built.
     * @param unit The <code>Unit</code> which is to build the colony.
     */
    private boolean buildColonyShowWarnings(Tile tile, Unit unit) {
        boolean landLocked = true;
        boolean ownedByEuropeans = false;
        boolean ownedBySelf = false;
        boolean ownedByIndians = false;

        java.util.Map<GoodsType, Integer> goodsMap
            = new HashMap<GoodsType, Integer>();
        for (GoodsType goodsType : getSpecification().getGoodsTypeList()) {
            if (goodsType.isFoodType()) {
                int potential = 0;
                if (tile.getType().isPrimaryGoodsType(goodsType)) {
                    potential = tile.potential(goodsType, null);
                }
                goodsMap.put(goodsType, new Integer(potential));
            } else if (goodsType.isBuildingMaterial()) {
                while (goodsType.isRefined()) {
                    goodsType = goodsType.getRawMaterial();
                }
                int potential = 0;
                if (tile.getType().isSecondaryGoodsType(goodsType)) {
                    potential = tile.potential(goodsType, null);
                }
                goodsMap.put(goodsType, new Integer(potential));
            }
        }

        for (Tile newTile: tile.getSurroundingTiles(1)) {
            if (!newTile.isLand()) {
                landLocked = false;
            }
            for (Entry<GoodsType, Integer> entry : goodsMap.entrySet()) {
                entry.setValue(entry.getValue().intValue()
                    + newTile.potential(entry.getKey(), null));
            }
            Player tileOwner = newTile.getOwner();
            if (unit.getOwner() == tileOwner) {
                if (newTile.getOwningSettlement() != null) {
                    // we are using newTile
                    ownedBySelf = true;
                } else {
                    for (Tile ownTile: newTile.getSurroundingTiles(1)) {
                        Colony colony = ownTile.getColony();
                        if (colony != null
                            && colony.getOwner() == unit.getOwner()) {
                            // newTile can be used from an own colony
                            ownedBySelf = true;
                            break;
                        }
                    }
                }
            } else if (tileOwner != null && tileOwner.isEuropean()) {
                ownedByEuropeans = true;
            } else if (tileOwner != null) {
                ownedByIndians = true;
            }
        }

        int food = 0;
        for (Entry<GoodsType, Integer> entry : goodsMap.entrySet()) {
            if (entry.getKey().isFoodType()) {
                food += entry.getValue().intValue();
            }
        }

        ArrayList<ModelMessage> messages = new ArrayList<ModelMessage>();
        if (landLocked) {
            messages.add(new ModelMessage(ModelMessage.MessageType.MISSING_GOODS,
                    "buildColony.landLocked", unit,
                    getSpecification().getGoodsType("model.goods.fish")));
        }
        if (food < 8) {
            messages.add(new ModelMessage(ModelMessage.MessageType.MISSING_GOODS,
                    "buildColony.noFood", unit,
                    getSpecification().getPrimaryFoodType()));
        }
        for (Entry<GoodsType, Integer> entry : goodsMap.entrySet()) {
            if (!entry.getKey().isFoodType() && entry.getValue().intValue() < 4) {
                messages.add(new ModelMessage(ModelMessage.MessageType.MISSING_GOODS,
                        "buildColony.noBuildingMaterials",
                        unit, entry.getKey())
                    .add("%goods%", entry.getKey().getNameKey()));
            }
        }

        if (ownedBySelf) {
            messages.add(new ModelMessage(ModelMessage.MessageType.WARNING,
                    "buildColony.ownLand", unit));
        }
        if (ownedByEuropeans) {
            messages.add(new ModelMessage(ModelMessage.MessageType.WARNING,
                    "buildColony.EuropeanLand", unit));
        }
        if (ownedByIndians) {
            messages.add(new ModelMessage(ModelMessage.MessageType.WARNING,
                    "buildColony.IndianLand", unit));
        }

        if (messages.isEmpty()) return true;
        ModelMessage[] modelMessages
            = messages.toArray(new ModelMessage[messages.size()]);
        return gui.showConfirmDialog(unit.getTile(),
            modelMessages, "buildColony.yes", "buildColony.no");
    }


    /**
     * Buy goods in Europe.
     * The amount of goods is adjusted to the space in the carrier.
     *
     * @param type The type of goods to buy.
     * @param amount The amount of goods to buy.
     * @param carrier The <code>Unit</code> acting as carrier.
     * @return True if the purchase succeeds.
     */
    public boolean buyGoods(GoodsType type, int amount, Unit carrier) {
        if (!requireOurTurn()) return false;

        // Sanity checks.  Should not happen!
        Player player = freeColClient.getMyPlayer();
        if (type == null) {
            throw new NullPointerException("Goods type must not be null.");
        } else if (carrier == null) {
            throw new NullPointerException("Carrier must not be null.");
        } else if (!player.owns(carrier)) {
            throw new IllegalArgumentException("Carrier owned by someone else.");
        } else if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        } else if (!player.canTrade(type)) {
            throw new IllegalArgumentException("Goods are boycotted.");
        }

        // Size check, if there are spare holds they can be filled, but...
        int toBuy = GoodsContainer.CARGO_SIZE;
        if (carrier.getSpaceLeft() <= 0) {
            // ...if there are no spare holds, we can only fill a hold
            // already partially filled with this type, otherwise fail.
            int partial = carrier.getGoodsContainer().getGoodsCount(type)
                % GoodsContainer.CARGO_SIZE;
            if (partial == 0) return false;
            toBuy -= partial;
        }
        if (amount < toBuy) toBuy = amount;

        // Check that the purchase is funded.
        Market market = player.getMarket();
        if (!player.checkGold(market.getBidPrice(type, toBuy))) {
            gui.errorMessage("notEnoughGold");
            return false;
        }

        // Try to purchase.
        int oldAmount = carrier.getGoodsContainer().getGoodsCount(type);
        int price = market.getCostToBuy(type);
        UnitWas unitWas = new UnitWas(carrier);
        if (askServer().buyGoods(carrier, type, toBuy)
            && carrier.getGoodsContainer().getGoodsCount(type) != oldAmount) {
            gui.playSound("sound.event.loadCargo");
            unitWas.fireChanges();
            for (TransactionListener l : market.getTransactionListener()) {
                l.logPurchase(type, toBuy, price);
            }
            gui.updateMenuBar();
            nextModelMessage();
            return true;
        }

        // Purchase failed for some reason.
        return false;
    }

    /**
     * Changes the state of this <code>Unit</code>.
     *
     * @param unit The <code>Unit</code>
     * @param state The state of the unit.
     */
    public void changeState(Unit unit, UnitState state) {
        if (!requireOurTurn()) return;

        if (!unit.checkSetState(state)) {
            return; // Don't bother (and don't log, this is not exceptional)
        }

        // Check if this is a hostile fortification, and give the player
        // a chance to confirm.
        Player player = freeColClient.getMyPlayer();
        if (state == UnitState.FORTIFYING && unit.isOffensiveUnit()
            && !unit.hasAbility(Ability.PIRACY)) {
            Tile tile = unit.getTile();
            if (tile != null && tile.getOwningSettlement() != null) {
                Player enemy = tile.getOwningSettlement().getOwner();
                if (player != enemy
                    && player.getStance(enemy) != Stance.ALLIANCE) {
                    if (!confirmHostileAction(unit, tile)) return; // Aborted
                }
            }
        }

        if (askServer().changeState(unit, state)) {
            if (!gui.isShowingSubPanel()
                && (unit.getMovesLeft() == 0
                    || unit.getState() == UnitState.SENTRY
                    || unit.getState() == UnitState.SKIPPED)) {
                nextActiveUnit();
            } else {
                gui.refresh();
            }
        }
    }

    /**
     * Changes the work type of this <code>Unit</code>.
     *
     * @param unit The <code>Unit</code>
     * @param improvementType a <code>TileImprovementType</code> value
     */
    public void changeWorkImprovementType(Unit unit,
                                          TileImprovementType improvementType) {
        if (!requireOurTurn()) return;

        if (!unit.checkSetState(UnitState.IMPROVING)
            || improvementType.isNatural()) {
            return; // Don't bother (and don't log, this is not exceptional)
        }

        Player player = freeColClient.getMyPlayer();
        Tile tile = unit.getTile();
        if (!player.owns(tile)) {
            if (!claimTile(player, tile, unit, player.getLandPrice(tile), 0)
                || !player.owns(tile)) return;
        }

        if (askServer().changeWorkImprovementType(unit, improvementType)) {
            ;// Redisplay should work
        }
        nextActiveUnit();
    }

    /**
     * Changes the work type of this <code>Unit</code>.
     *
     * @param unit The <code>Unit</code>
     * @param workType The new <code>GoodsType</code> to produce.
     */
    public void changeWorkType(Unit unit, GoodsType workType) {
        if (!requireOurTurn()) return;

        UnitWas unitWas = new UnitWas(unit);
        if (askServer().changeWorkType(unit, workType)) {
            unitWas.fireChanges();
        }
    }

    /**
     * Check if a unit is a treasure train, and if it should be cashed in.
     * Transfers the gold carried by this unit to the {@link Player owner}.
     *
     * @param unit The <code>Unit</code> to be checked.
     * @return True if the unit was cashed in (and disposed).
     */
    public boolean checkCashInTreasureTrain(Unit unit) {
        if (!unit.canCarryTreasure() || !unit.canCashInTreasureTrain()
            || !requireOurTurn()) {
            return false; // Fail quickly if just not a candidate.
        }

        boolean cash;
        Tile tile = unit.getTile();
        Europe europe = unit.getOwner().getEurope();
        if (europe == null || unit.isInEurope()) {
            cash = true; // No need to check for transport.
        } else {
            int fee = unit.getTransportFee();
            StringTemplate template;
            if (fee == 0) {
                template = StringTemplate.template("cashInTreasureTrain.free");
            } else {
                int percent = getSpecification()
                    .getInteger("model.option.treasureTransportFee");
                template = StringTemplate.template("cashInTreasureTrain.pay")
                    .addAmount("%fee%", percent);
            }
            cash = gui.showConfirmDialog(unit.getTile(), template,
                "cashInTreasureTrain.yes", "cashInTreasureTrain.no");
        }

        // Update if cash in succeeds.
        UnitWas unitWas = new UnitWas(unit);
        if (cash && askServer().cashInTreasureTrain(unit)
            && unit.isDisposed()) {
            gui.playSound("sound.event.cashInTreasureTrain");
            unitWas.fireChanges();
            gui.updateMenuBar();
            nextActiveUnit(tile);
            return true;
        }
        return false;
    }

    /**
     * Claim a tile.
     *
     * @param tile The <code>Tile</code> to claim.
     * @param claimant The <code>Unit</code> or <code>Colony</code> claiming.
     * @param offer An offer to pay.
     * @return True if the claim succeeded.
     */
    public boolean claimLand(Tile tile, FreeColGameObject claimant, int offer) {
        if (!requireOurTurn()) return false;

        Player player = freeColClient.getMyPlayer();
        int price = ((claimant instanceof Settlement)
            ? player.canClaimForSettlement(tile)
            : player.canClaimForImprovement(tile)) ? 0
            : player.getLandPrice(tile);
        return claimTile(player, tile, claimant, price, offer);
    }

    /**
     * Clears the goto orders of the given unit by setting its destination
     * to null.
     *
     * @param unit The <code>Unit</code> to clear the destination for.
     */
    public void clearGotoOrders(Unit unit) {
        if (unit != null && unit.getDestination() != null) {
            setDestination(unit, null);
        }
    }

    /**
     * Clears the orders of the given unit.
     * Make the unit active and set a null destination and trade route.
     *
     * @param unit The <code>Unit</code> to clear the orders of
     * @return boolean <b>true</b> if the orders were cleared
     */
    public boolean clearOrders(Unit unit) {
        if (!requireOurTurn() || unit == null
            || !unit.checkSetState(UnitState.ACTIVE)) return false;

        if (unit.getState() == UnitState.IMPROVING
            && !gui.showConfirmDialog(unit.getTile(),
                StringTemplate.template("model.unit.confirmCancelWork")
                .addAmount("%turns%", unit.getWorkTurnsLeft()),
                "yes", "no")) {
            return false;
        }

        if (unit.getTradeRoute() != null) assignTradeRoute(unit, null);
        clearGotoOrders(unit);
        return askServer().changeState(unit, UnitState.ACTIVE);
    }

    /**
     * Clear the speciality of a Unit, making it a Free Colonist.
     *
     * @param unit The <code>Unit</code> to clear the speciality of.
     */
    public void clearSpeciality(Unit unit) {
        if (!requireOurTurn()) return;

        UnitType oldType = unit.getType();
        UnitType newType = oldType.getTargetType(ChangeType.CLEAR_SKILL,
                                                 unit.getOwner());
        if (newType == null) {
            gui.showInformationMessage(unit,
                StringTemplate.template("clearSpeciality.impossible")
                .addStringTemplate("%unit%", Messages.getLabel(unit)));
            return;
        }

        Tile tile = (gui.isShowingSubPanel()) ? null : unit.getTile();
        if (!gui.showConfirmDialog(tile,
                StringTemplate.template("clearSpeciality.areYouSure")
                .addStringTemplate("%oldUnit%", Messages.getLabel(unit))
                .add("%unit%", newType.getNameKey()),
                "yes", "no")) {
            return;
        }

        // Try to clear.
        if (askServer().clearSpeciality(unit)
            && unit.getType() == newType) {
            // Would expect to need to do:
            //    unit.firePropertyChange(Unit.UNIT_TYPE_CHANGE,
            //                            oldType, newType);
            // but this routine is only called out of UnitLabel, where the
            // unit icon is always updated anyway.
        }
        nextActiveUnit();
    }

    /**
     * Declares independence for the home country.
     */
    public void declareIndependence() {
        if (!requireOurTurn()) return;
        Game game = freeColClient.getGame();
        Player player = freeColClient.getMyPlayer();

        // Check for adequate support.
        Event event = getSpecification()
            .getEvent("model.event.declareIndependence");
        for (Limit limit : event.getLimits()) {
            if (!limit.evaluate(player)) {
                gui.showInformationMessage(StringTemplate
                    .template(limit.getDescriptionKey())
                    .addAmount("%limit%",
                        limit.getRightHandSide().getValue(game)));
                return;
            }
        }
        if (player.getNewLandName() == null) {
            // Can only happen in debug mode.
            return;
        }

        // Confirm intention, and collect nation+country names.
        List<String> names = gui.showConfirmDeclarationDialog();
        if (names == null
            || names.get(0) == null || names.get(0).length() == 0
            || names.get(1) == null || names.get(1).length() == 0) {
            // Empty name => user cancelled.
            return;
        }

        // Ask server.
        String nationName = names.get(0);
        String countryName = names.get(1);
        if (askServer().declareIndependence(nationName, countryName)
            && player.getPlayerType() == PlayerType.REBEL) {
            gui.showDeclarationDialog();
            freeColClient.updateActions();
            nextModelMessage();
        }
    }

    /**
     * Disbands the active unit.
     */
    public void disbandActiveUnit() {
        if (!requireOurTurn()) return;

        Unit unit = gui.getActiveUnit();
        if (unit == null) return;
        if (unit.getColony() != null && !gui.tryLeaveColony(unit)) return;

        Tile tile = (gui.isShowingSubPanel()) ? null
            : unit.getTile();
        if (!gui.showConfirmDialog(tile,
                StringTemplate.key("disbandUnit.text"),
                "disbandUnit.yes", "disbandUnit.no")) {
            return;
        }

        // Try to disband
        if (askServer().disbandUnit(unit)) {
            nextActiveUnit();
        }
    }

    /**
     * End the turn command.
     */
    public void endTurn() {
        if (!requireOurTurn()) return;

        // Show the end turn dialog, or not.
        if (freeColClient.getClientOptions()
            .getBoolean(ClientOptions.SHOW_END_TURN_DIALOG)) {
            List<Unit> units = new ArrayList<Unit>();
            for (Unit unit : freeColClient.getMyPlayer().getUnits()) {
                if (unit.couldMove()) units.add(unit);
            }
            if (!units.isEmpty() && !gui.showEndTurnDialog(units)) return;
        }

        // Ensure end-turn mode sticks.
        if (moveMode < MODE_END_TURN) moveMode = MODE_END_TURN;

        // Make sure all goto orders are complete before ending turn.
        if (!doExecuteGotoOrders()) return;

        doEndTurn();
    }

    /**
     * Change the amount of equipment a unit has.
     *
     * @param unit The <code>Unit</code>.
     * @param type The <code>EquipmentType</code> to equip with.
     * @param amount How to change the amount of equipment the unit has.
     */
    public void equipUnit(Unit unit, EquipmentType type, int amount) {
        if (!requireOurTurn() || amount == 0) return;

        Player player = freeColClient.getMyPlayer();
        List<AbstractGoods> requiredGoods = type.getGoodsRequired();
        Colony colony = null;
        if (unit.isInEurope()) {
            for (AbstractGoods goods : requiredGoods) {
                GoodsType goodsType = goods.getType();
                if (!player.canTrade(goodsType) && !payArrears(goodsType)) {
                    return; // payment failed for some reason
                }
            }
        } else {
            colony = unit.getColony();
            if (colony == null) {
                throw new IllegalStateException("Equip unit not in settlement/Europe");
            }
        }

        ColonyWas colonyWas = (colony == null) ? null : new ColonyWas(colony);
        UnitWas unitWas = new UnitWas(unit);
        if (askServer().equipUnit(unit, type, amount)) {
            if (colonyWas != null) colonyWas.fireChanges();
            unitWas.fireChanges();
            gui.updateMenuBar();
        }
    }

    /**
     * Execute goto orders command.
     */
    public void executeGotoOrders() {
        if (!requireOurTurn()) return;

        doExecuteGotoOrders();
    }

    /**
     * Retrieves client statistics.
     *
     * @return A <code>Map</code> containing the client statistics.
     */
    public java.util.Map<String, String> getClientStatistics() {
        return freeColClient.getGame().getStatistics();
    }

    /**
     * Retrieves high scores from server.
     *
     * @return The list of high scores.
     */
    public List<HighScore> getHighScores() {
        return askServer().getHighScores();
    }

    /**
     * Get the nation summary for a player.
     *
     * @param player The <code>Player</code> to summarize.
     * @return A summary of that nation, or null on error.
     */
    public NationSummary getNationSummary(Player player) {
        return askServer().getNationSummary(player);
    }

    /**
     * Gets a new trade route for a player.
     *
     * @param player The <code>Player</code> to get a new trade route for.
     * @return A new <code>TradeRoute</code>.
     */
    public TradeRoute getNewTradeRoute(Player player) {
        int n = player.getTradeRoutes().size();
        if (askServer().getNewTradeRoute()
            && player.getTradeRoutes().size() == n + 1) {
            return player.getTradeRoutes().get(n);
        }
        return null;
    }

    /**
     * Gathers information about the REF.
     *
     * @return a <code>List</code> value
     */
    public List<AbstractUnit> getREFUnits() {
        if (!requireOurTurn()) return Collections.emptyList();

        return askServer().getREFUnits();
    }

    /**
     * Retrieves the server statistics.
     *
     * @return A <code>Map</code> containing the server statistics.
     */
    public java.util.Map<String, String> getServerStatistics() {
        return askServer().getStatistics();
    }

    /**
     * Go to a tile.
     *
     * Called from the CanvasMouseListener and TilePopup when the
     * goto-path is set.
     *
     * @param unit The <code>Unit</code> to move.
     * @param tile The <code>Tile</code> to move to.
     */
    public void goToTile(Unit unit, Tile tile) {
        if (!requireOurTurn()) return;
        if (!setDestination(unit, tile)) return;
        if (!moveToDestination(unit)) nextActiveUnit();
    }

    /**
     * Leave a ship.  The ship must be in harbour.
     *
     * @param unit The <code>Unit</code> which is to leave the ship.
     * @return boolean
     */
    public boolean leaveShip(Unit unit) {
        if (!requireOurTurn()) {
           return false;
        }

        // Sanity check, and find our carrier before we get off.
        Unit carrier = unit.getCarrier();
        if (carrier == null) {
            logger.warning("Unit " + unit.getId() + " is not on a carrier.");
            return false;
        }

        // Proceed to disembark
        UnitWas unitWas = new UnitWas(unit);
        if (askServer().disembark(unit)
            && unit.getLocation() != carrier) {
            checkCashInTreasureTrain(unit);
            unitWas.fireChanges();
            nextActiveUnit();
            return true;
        }
        return false;
    }

    /**
     * Loads a cargo onto a carrier.
     *
     * @param goods The <code>Goods</code> which are going aboard the carrier.
     * @param carrier The <code>Unit</code> acting as carrier.
     */
    public void loadCargo(Goods goods, Unit carrier) {
        if (!requireOurTurn()) return;

        // Sanity checks.
        if (goods == null) {
            throw new IllegalArgumentException("Null goods.");
        } else if (goods.getAmount() <= 0) {
            throw new IllegalArgumentException("Empty goods.");
        } else if (carrier == null) {
            throw new IllegalArgumentException("Null carrier.");
        } else if (carrier.isInEurope()) {
            // empty
        } else if (carrier.getColony() == null) {
            throw new IllegalArgumentException("Carrier not at colony or Europe.");
        }

        // Try to load.
        if (loadGoods(goods, carrier)) {
            gui.playSound("sound.event.loadCargo");
        }
    }

    /**
     * Moves the specified unit somewhere that requires crossing the high seas.
     * Public because this is called from the TilePopup and the Europe panel.
     *
     * @param unit The <code>Unit</code> to be moved.
     * @param destination The <code>Location</code> to be moved to.
     * @throws IllegalArgumentException if destination or unit are null.
     */
    public void moveTo(Unit unit, Location destination) {
        if (!requireOurTurn()) return;

        // Sanity check current state.
        if (unit == null || destination == null) {
            throw new IllegalArgumentException("moveTo null argument");
        } else if (destination instanceof Europe) {
            if (unit.isInEurope()) {
                gui.playSound("sound.event.illegalMove");
                return;
            }
        } else if (destination instanceof Map) {
            if (unit.getTile() != null
                && unit.getTile().getMap() == destination) {
                gui.playSound("sound.event.illegalMove");
                return;
            }
        } else if (destination instanceof Settlement) {
            if (unit.getTile() != null) {
                gui.playSound("sound.event.illegalMove");
                return;
            }
        }

        // Autoload emigrants?
        if (freeColClient.getClientOptions()
            .getBoolean(ClientOptions.AUTOLOAD_EMIGRANTS)
            && unit.isInEurope()) {
            int spaceLeft = unit.getSpaceLeft();
            for (Unit u : unit.getOwner().getEurope().getUnitList()) {
                if (spaceLeft <= 0) break;
                if (!u.isNaval()
                    && u.getState() == UnitState.SENTRY
                    && u.getType().getSpaceTaken() <= spaceLeft) {
                    boardShip(u, unit);
                    spaceLeft -= u.getType().getSpaceTaken();
                }
            }
        }

        if (askServer().moveTo(unit, destination)) {
            nextActiveUnit();
        }
    }

    /**
     * Moves the active unit in a specified direction. This may result in an
     * attack, move... action.
     *
     * @param direction The direction in which to move the active unit.
     */
    public void moveActiveUnit(Direction direction) {
        Unit unit = gui.getActiveUnit();
        if (unit != null && requireOurTurn()) {
            clearGotoOrders(unit);
            move(unit, direction);
        } // else: nothing: There is no active unit that can be moved.
    }

    /**
     * Moves the specified unit in a specified direction. This may
     * result in many different types of action.
     *
     * @param unit The <code>Unit</code> to be moved.
     * @param direction The direction in which to move the unit.
     */
    public void move(Unit unit, Direction direction) {
        if (!requireOurTurn()) return;

        moveDirection(unit, direction, true);

        // TODO: check if this is necessary for all actions?
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    freeColClient.updateActions();
                    gui.updateMenuBar();
                }
            });
    }

    /**
     * Moves the given unit towards its destination/s if possible.
     *
     * @param unit The <code>Unit</code> to move.
     * @return True if the unit reached its destination and has more moves
     *     to make.
     */
    public boolean moveToDestination(Unit unit) {
        if (!requireOurTurn()) return false;
        if (unit.getTradeRoute() != null) return followTradeRoute(unit);
        gui.setActiveUnit(unit);
        Player player = freeColClient.getMyPlayer();
        Location destination;
        while (unit.getMovesLeft() > 0
            && unit.getState() != UnitState.SKIPPED) {
            // Look for valid destinations
            if ((destination = unit.getDestination()) == null) {
                break; // No destination
            } else if (destination instanceof Europe) {
                if (unit.isInEurope() || unit.isAtSea()) {
                    break; // Arrived in or between New World and Europe.
                }
            } else if (destination.getTile() == null) {
                break; // Not on the map, findPath* can not help.
            } else if (unit.getTile() == destination.getTile()) {
                break; // Arrived at on-map destination
            }

            // Find a path to the destination.
            PathNode path = unit.findFullPath(destination);
            if (path == null) {
                StringTemplate src = unit.getLocation()
                    .getLocationNameFor(player);
                StringTemplate dst = destination.getLocationNameFor(player);
                gui.showInformationMessage(unit,
                    StringTemplate.template("selectDestination.failed")
                        .addStringTemplate("%unit%", Messages.getLabel(unit))
                        .addStringTemplate("%location%", src)
                        .addStringTemplate("%destination%", dst));
                break;
            }

            // Try to follow the path.
            if (!followPath(unit, path)) break;
        }

        // Clear ordinary destinations if arrived.
        if ((destination = unit.getDestination()) != null) {
            if (unit.getTile() == null) {
                if (unit.getLocation() == destination) {
                    clearGotoOrders(unit);
                }
            } else if (unit.getTile() == destination.getTile()) {
                clearGotoOrders(unit);
                // Check cash-in, and if the unit has moves left
                // and was not set to SKIPPED by moveDirection,
                // then make sure it remains selected to show that
                // this unit could continue.
                if (!checkCashInTreasureTrain(unit)
                    && unit.getMovesLeft() > 0
                    && unit.getState() != UnitState.SKIPPED) {
                    gui.setActiveUnit(unit);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Move a unit in a given direction.
     *
     * @param unit The <code>Unit</code> to move.
     * @param direction The <code>Direction</code> to move in.
     * @param interactive Interactive mode: play sounds and emit errors.
     * @return True if the unit can possibly move further.
     */
    private boolean moveDirection(Unit unit, Direction direction,
                                  boolean interactive) {
        Location destination = unit.getDestination();

        // Consider all the move types
        switch (unit.getMoveType(direction)) {
        case MOVE_HIGH_SEAS:
            if (destination == null) {
                return moveHighSeas(unit, direction);
            } else if (destination instanceof Europe) {
                moveTo(unit, destination);
                return false;
            }
            // Fall through
        case MOVE:
            moveMove(unit, direction);
            return unit.getMovesLeft() > 0;
        case EXPLORE_LOST_CITY_RUMOUR:
            moveExplore(unit, direction);
            return false;
        case ATTACK_UNIT: case ATTACK_SETTLEMENT:
            moveAttack(unit, direction);
            return false;
        case EMBARK:
            moveEmbark(unit, direction);
            return false;
        case ENTER_INDIAN_SETTLEMENT_WITH_FREE_COLONIST:
            moveLearnSkill(unit, direction);
            return false;
        case ENTER_INDIAN_SETTLEMENT_WITH_SCOUT:
            moveScoutIndianSettlement(unit, direction);
            return false;
        case ENTER_INDIAN_SETTLEMENT_WITH_MISSIONARY:
            moveUseMissionary(unit, direction);
            return false;
        case ENTER_FOREIGN_COLONY_WITH_SCOUT:
            moveScoutColony(unit, direction);
            return false;
        case ENTER_SETTLEMENT_WITH_CARRIER_AND_GOODS:
            moveTrade(unit, direction);
            return false;

        case MOVE_NO_ACCESS_BEACHED:
            if (interactive) {
                gui.playSound("sound.event.illegalMove");
                StringTemplate nation = getNationAt(unit.getTile(), direction);
                gui.showInformationMessage(unit,
                    StringTemplate.template("move.noAccessBeached")
                    .addStringTemplate("%nation%", nation));
            }
            return false;
        case MOVE_NO_ACCESS_CONTACT:
            if (interactive) {
                gui.playSound("sound.event.illegalMove");
                StringTemplate nation = getNationAt(unit.getTile(), direction);
                gui.showInformationMessage(unit,
                    StringTemplate.template("move.noAccessContact")
                    .addStringTemplate("%nation%", nation));
            }
            return false;
        case MOVE_NO_ACCESS_GOODS:
            if (interactive) {
                gui.playSound("sound.event.illegalMove");
                StringTemplate nation = getNationAt(unit.getTile(), direction);
                gui.showInformationMessage(unit,
                    StringTemplate.template("move.noAccessGoods")
                    .addStringTemplate("%nation%", nation)
                    .addStringTemplate("%unit%", Messages.getLabel(unit)));
            }
            return false;
        case MOVE_NO_ACCESS_LAND:
            if (!moveDisembark(unit, direction)) {
                if (interactive) {
                    gui.playSound("sound.event.illegalMove");
                }
            }
            return false;
        case MOVE_NO_ACCESS_SETTLEMENT:
            if (interactive) {
                gui.playSound("sound.event.illegalMove");
                StringTemplate nation = getNationAt(unit.getTile(), direction);
                gui.showInformationMessage(unit,
                    StringTemplate.template("move.noAccessSettlement")
                    .addStringTemplate("%unit%", Messages.getLabel(unit))
                    .addStringTemplate("%nation%", nation));
            }
            return false;
        case MOVE_NO_ACCESS_SKILL:
            if (interactive) {
                gui.playSound("sound.event.illegalMove");
                gui.showInformationMessage(unit,
                    StringTemplate.template("move.noAccessSkill")
                    .addStringTemplate("%unit%", Messages.getLabel(unit)));
            }
            return false;
        case MOVE_NO_ACCESS_TRADE:
            if (interactive) {
                gui.playSound("sound.event.illegalMove");
                StringTemplate nation = getNationAt(unit.getTile(), direction);
                gui.showInformationMessage(unit,
                    StringTemplate.template("move.noAccessTrade")
                    .addStringTemplate("%nation%", nation));
            }
            return false;
        case MOVE_NO_ACCESS_WAR:
            if (interactive) {
                gui.playSound("sound.event.illegalMove");
                StringTemplate nation = getNationAt(unit.getTile(), direction);
                gui.showInformationMessage(unit,
                    StringTemplate.template("move.noAccessWar")
                    .addStringTemplate("%nation%", nation));
            }
            return false;
        case MOVE_NO_ACCESS_WATER:
            if (interactive) {
                gui.playSound("sound.event.illegalMove");
                gui.showInformationMessage(unit,
                    StringTemplate.template("move.noAccessWater")
                    .addStringTemplate("%unit%", Messages.getLabel(unit)));
            }
            return false;
        case MOVE_NO_ATTACK_MARINE:
            if (interactive) {
                gui.playSound("sound.event.illegalMove");
                gui.showInformationMessage(unit,
                    StringTemplate.template("move.noAttackWater")
                    .addStringTemplate("%unit%", Messages.getLabel(unit)));
            }
            return false;
        case MOVE_NO_MOVES:
            // The unit may have some moves left, but not enough
            // to move to the next node.
            unit.setState(UnitState.SKIPPED);
            return false;
        default:
            if (interactive) {
                gui.playSound("sound.event.illegalMove");
            }
            return false;
        }
    }

    /**
     * Confirm attack or demand a tribute from a native settlement, following
     * an attacking move.
     *
     * @param unit The <code>Unit</code> to perform the attack.
     * @param direction The direction in which to attack.
     */
    private void moveAttack(Unit unit, Direction direction) {
        clearGotoOrders(unit);

        // Extra option with native settlement
        Tile tile = unit.getTile();
        Tile target = tile.getNeighbourOrNull(direction);
        IndianSettlement is = target.getIndianSettlement();
        if (is != null && unit.isArmed()) {
            switch (gui.showArmedUnitIndianSettlementDialog(is)) {
            case CANCEL:
                return;
            case INDIAN_SETTLEMENT_ATTACK:
                break; // Go on to usual attack confirmation.
            case INDIAN_SETTLEMENT_TRIBUTE:
                moveTribute(unit, direction);
                return;
            default:
                logger.warning("showArmedUnitIndianSettlementDialog failure.");
                return;
            }
        }

        // Normal attack confirmation.
        if (confirmHostileAction(unit, target)
            && confirmPreCombat(unit, target)) {
            askServer().attack(unit, direction);
            nextActiveUnit();
        }
    }

    /**
     * Check the carrier for passengers to disembark, possibly
     * snatching a useful result from the jaws of a
     * MOVE_NO_ACCESS_LAND failure.
     *
     * @param unit The carrier containing the unit to disembark.
     * @param direction The direction in which to disembark the unit.
     * @return True if the disembark "succeeds" (which deliberately includes
     *         declined disembarks).
     */
    private boolean moveDisembark(Unit unit, final Direction direction) {
        Tile tile = unit.getTile().getNeighbourOrNull(direction);
        if (tile.getFirstUnit() != null
            && tile.getFirstUnit().getOwner() != unit.getOwner()) {
            return false; // Can not disembark onto other nation units.
        }

        // Disembark selected units able to move.
        final List<Unit> disembarkable = new ArrayList<Unit>();
        unit.setStateToAllChildren(UnitState.ACTIVE);
        for (Unit u : unit.getUnitList()) {
            if (u.getMoveType(tile).isProgress()) {
                disembarkable.add(u);
            }
        }
        if (disembarkable.size() == 0) {
            // Did not find any unit that could disembark, fail.
            return false;
        }

        while (disembarkable.size() > 0) {
            if (disembarkable.size() == 1) {
                if (gui.showConfirmDialog("disembark.text", "yes", "no")) {
                    move(disembarkable.get(0), direction);
                }
                break;
            }
            List<ChoiceItem<Unit>> choices = new ArrayList<ChoiceItem<Unit>>();
            for (Unit dUnit : disembarkable) {
                choices.add(new ChoiceItem<Unit>(Messages.message(Messages.getLabel(dUnit)), dUnit));
            }
            if (disembarkable.size() > 1) {
                choices.add(new ChoiceItem<Unit>(Messages.message("all"), unit));
            }

            // Use moveDirection() to disembark units as while the
            // destination tile is known to be clear of other player
            // units or settlements, it may have a rumour or need
            // other special handling.
            Unit u = gui.showChoiceDialog(unit.getTile(),
                Messages.message("disembark.text"),
                Messages.message("disembark.cancel"),
                choices);
            if (u == null) { // Cancelled, done.
                break;
            } else if (u == unit) { // Disembark all.
                for (Unit dUnit : disembarkable) {
                    // Guard against loss of control when asking the
                    // server to move the unit.
                    try {
                        moveDirection(dUnit, direction, false);
                    } finally {
                        continue;
                    }
                }
                return true;
            }
            moveDirection(u, direction, false);
            disembarkable.remove(u);
        }
        return true;
    }

    /**
     * Embarks the specified unit onto a carrier in a specified direction
     * following a move of MoveType.EMBARK.
     *
     * @param unit The <code>Unit</code> that wishes to embark.
     * @param direction The direction in which to embark.
     */
    private void moveEmbark(Unit unit, Direction direction) {
        if (unit.getColony() != null && !gui.tryLeaveColony(unit)) return;

        Tile sourceTile = unit.getTile();
        Tile destinationTile = sourceTile.getNeighbourOrNull(direction);
        Unit carrier = null;
        List<ChoiceItem<Unit>> choices = new ArrayList<ChoiceItem<Unit>>();
        for (Unit u : destinationTile.getUnitList()) {
            if (u.getSpaceLeft() >= unit.getType().getSpaceTaken()) {
                String m = Messages.message(Messages.getLabel(u));
                choices.add(new ChoiceItem<Unit>(m, u));
                carrier = u; // Save a default
            }
        }
        if (choices.size() == 0) {
            throw new RuntimeException("Unit " + unit.getId()
                + " found no carrier to embark upon.");
        } else if (choices.size() == 1) {
            // Use the default
        } else {
            carrier = gui.showChoiceDialog(unit.getTile(),
                Messages.message("embark.text"),
                Messages.message("embark.cancel"),
                choices);
            if (carrier == null) return; // User cancelled
        }

        // Proceed to embark
        clearGotoOrders(unit);
        if (askServer().embark(unit, carrier, direction)
            && unit.getLocation() == carrier) {
            if (carrier.getMovesLeft() > 0) {
                gui.setActiveUnit(carrier);
            } else {
                nextActiveUnit();
            }
        }
    }

    /**
     * Confirm exploration of a lost city rumour, following a move of
     * MoveType.EXPLORE_LOST_CITY_RUMOUR.
     *
     * @param unit The <code>Unit</code> that is exploring.
     * @param direction The direction of a rumour.
     */
    private void moveExplore(Unit unit, Direction direction) {
        Tile tile = unit.getTile().getNeighbourOrNull(direction);
        if (gui.showConfirmDialog(unit.getTile(),
                StringTemplate.key("exploreLostCityRumour.text"),
                "exploreLostCityRumour.yes", "exploreLostCityRumour.no")) {
            if (tile.getLostCityRumour().getType()
                == LostCityRumour.RumourType.MOUNDS
                && !gui.showConfirmDialog(unit.getTile(),
                    StringTemplate.key("exploreMoundsRumour.text"),
                    "exploreLostCityRumour.yes", "exploreLostCityRumour.no")) {
                askServer().declineMounds(unit, direction);
            }
            moveMove(unit, direction);
        }
    }

    /**
     * Moves a unit onto the "high seas" in a specified direction following
     * a move of MoveType.MOVE_HIGH_SEAS.
     * This may result in a move to Europe, no move, or an ordinary move.
     *
     * @param unit The <code>Unit</code> to be moved.
     * @param direction The direction in which to move.
     */
    private boolean moveHighSeas(Unit unit, Direction direction) {
        // Confirm moving to Europe if told to move to a null tile
        // (TODO: can this still happen?), or if crossing the boundary
        // between coastal and high sea.  Otherwise just move.
        Tile oldTile = unit.getTile();
        Tile newTile = oldTile.getNeighbourOrNull(direction);
        if ((newTile == null
             || (!oldTile.isDirectlyHighSeasConnected() && newTile.isDirectlyHighSeasConnected()))
            && gui.showConfirmDialog(oldTile,
                StringTemplate.template("highseas.text")
                .addAmount("%number%", unit.getSailTurns()),
                "highseas.yes", "highseas.no")) {
            moveTo(unit, unit.getOwner().getEurope());
            nextActiveUnit();
            return false;
        }
        moveMove(unit, direction);
        return true;
    }

    /**
     * Move a free colonist to a native settlement to learn a skill following
     * a move of MoveType.ENTER_INDIAN_SETTLEMENT_WITH_FREE_COLONIST.
     * The colonist does not physically get into the village, it will
     * just stay where it is and gain the skill.
     *
     * @param unit The <code>Unit</code> to learn the skill.
     * @param direction The direction in which the Indian settlement lies.
     */
    private void moveLearnSkill(Unit unit, Direction direction) {
        clearGotoOrders(unit);
        // Refresh knowledge of settlement skill.  It may have been
        // learned by another player.
        if (!askServer().askSkill(unit, direction)) {
            return;
        }

        IndianSettlement settlement
            = (IndianSettlement) getSettlementAt(unit.getTile(), direction);
        UnitType skill = settlement.getLearnableSkill();
        if (skill == null) {
            gui.showInformationMessage(settlement,
                                          "indianSettlement.noMoreSkill");
        } else if (!unit.getType().canBeUpgraded(skill, ChangeType.NATIVES)) {
            gui.showInformationMessage(settlement,
                StringTemplate.template("indianSettlement.cantLearnSkill")
                .addStringTemplate("%unit%", Messages.getLabel(unit))
                .add("%skill%", skill.getNameKey()));
        } else if (gui.showConfirmDialog(unit.getTile(),
                StringTemplate.template("learnSkill.text")
                .add("%skill%", skill.getNameKey()),
                "learnSkill.yes", "learnSkill.no")) {
            if (askServer().learnSkill(unit, direction)) {
                if (unit.isDisposed()) {
                    gui.showInformationMessage(settlement, "learnSkill.die");
                    nextActiveUnit(unit.getTile());
                    return;
                }
                if (unit.getType() != skill) {
                    gui.showInformationMessage(settlement, "learnSkill.leave");
                }
            }
        }
        nextActiveUnit();
    }

    /**
     * Actually move a unit in a specified direction, following a move
     * of MoveType.MOVE.
     *
     * @param unit The <code>Unit</code> to be moved.
     * @param direction The direction in which to move the Unit.
     */
    private void moveMove(Unit unit, Direction direction) {
        // If we are in a colony, or Europe, load sentries.
        if (unit.canCarryUnits() && unit.getSpaceLeft() > 0
            && (unit.getColony() != null || unit.isInEurope())) {
            for (Unit sentry : unit.getLocation().getUnitList()) {
                if (sentry.getState() == UnitState.SENTRY) {
                    if (sentry.getSpaceTaken() <= unit.getSpaceLeft()) {
                        boardShip(sentry, unit);
                        logger.finest("Unit " + unit.toString()
                            + " loaded sentry " + sentry.toString());
                    } else {
                        logger.finest("Unit " + sentry.toString()
                            + " is too big to board " + unit.toString());
                    }
                }
            }
        }

        // Ask the server
        UnitWas unitWas = new UnitWas(unit);
        if (!askServer().move(unit, direction)) return;
        unitWas.fireChanges();

        final Tile tile = unit.getTile();

        // Perform a short pause on an active unit's last move if
        // the option is enabled.
        ClientOptions options = freeColClient.getClientOptions();
        if (unit.getMovesLeft() <= 0
            && options.getBoolean(ClientOptions.UNIT_LAST_MOVE_DELAY)) {
            gui.paintImmediatelyCanvasInItsBounds();
            try {
                Thread.sleep(UNIT_LAST_MOVE_DELAY);
            } catch (InterruptedException e) {} // Ignore
        }

        // Update the active unit and GUI.
        if (unit.isDisposed() || checkCashInTreasureTrain(unit)) {
            nextActiveUnit(tile);
        } else {
            if (tile.getColony() != null
                && unit.isCarrier()
                && unit.getTradeRoute() == null
                && (unit.getDestination() == null
                    || unit.getDestination().getTile() == tile.getTile())) {
                gui.showColonyPanel(tile.getColony());
            }
            if (unit.getMovesLeft() == 0) {
                nextActiveUnit();
            } else {
                displayModelMessages(false);
                if (!gui.onScreen(tile))
                    gui.setSelectedTile(tile, false);
            }
        }
    }

    /**
     * Move to a foreign colony and either attack, negotiate with the
     * foreign power or spy on them.  Follows a move of
     * MoveType.ENTER_FOREIGN_COLONY_WITH_SCOUT.
     * TODO: Unify trade and negotiation.
     *
     * @param unit The unit that will spy, negotiate or attack.
     * @param direction The direction in which the foreign colony lies.
     */
    private void moveScoutColony(Unit unit, Direction direction) {
        Colony colony = (Colony) getSettlementAt(unit.getTile(), direction);
        boolean canNeg = colony.getOwner() != unit.getOwner().getREFPlayer();
        clearGotoOrders(unit);

        switch (gui.showScoutForeignColonyDialog(colony, unit, canNeg)) {
        case CANCEL:
            break;
        case FOREIGN_COLONY_ATTACK:
            moveAttack(unit, direction);
            break;
        case FOREIGN_COLONY_NEGOTIATE:
            moveTradeColony(unit, direction);
            break;
        case FOREIGN_COLONY_SPY:
            moveSpy(unit, direction);
            break;
        default:
            throw new IllegalArgumentException("showScoutForeignColonyDialog fail");
        }
    }

    /**
     * Move a scout into an Indian settlement to speak with the chief,
     * or demand a tribute following a move of
     * MoveType.ENTER_INDIAN_SETTLEMENT_WITH_SCOUT.
     * The scout does not physically get into the village, it will
     * just stay where it is.
     *
     * @param unit The <code>Unit</code> that is scouting.
     * @param direction The direction in which the Indian settlement lies.
     */
    private void moveScoutIndianSettlement(Unit unit, Direction direction) {
        Tile unitTile = unit.getTile();
        Tile tile = unitTile.getNeighbourOrNull(direction);
        IndianSettlement settlement = tile.getIndianSettlement();
        clearGotoOrders(unit);

        // Offer the choices.
        NationSummary ns = getNationSummary(settlement.getOwner());
        String number = (ns == null) ? "many" : ns.getNumberOfSettlements();
        switch (gui.showScoutIndianSettlementDialog(settlement, number)) {
        case CANCEL:
            return;
        case INDIAN_SETTLEMENT_ATTACK:
            if (confirmPreCombat(unit, tile)) {
                askServer().attack(unit, direction);
            }
            return;
        case INDIAN_SETTLEMENT_SPEAK:
            Player player = unit.getOwner();
            final int oldGold = player.getGold();
            String result = askServer().scoutSpeak(unit, direction);
            if (result == null) {
                return; // Fail
            } else if ("die".equals(result)) {
                gui.showInformationMessage(settlement,
                    "scoutSettlement.speakDie");
                nextActiveUnit(unitTile);
                return;
            } else if ("expert".equals(result)) {
                gui.showInformationMessage(settlement,
                    StringTemplate.template("scoutSettlement.expertScout")
                    .add("%unit%", unit.getType().getNameKey()));
            } else if ("tales".equals(result)) {
                gui.showInformationMessage(settlement,
                    "scoutSettlement.speakTales");
            } else if ("beads".equals(result)) {
                gui.updateMenuBar();
                gui.showInformationMessage(settlement,
                    StringTemplate.template("scoutSettlement.speakBeads")
                    .addAmount("%amount%", player.getGold() - oldGold));
            } else if ("nothing".equals(result)) {
                gui.showInformationMessage(settlement,
                    StringTemplate.template("scoutSettlement.speakNothing")
                    .addStringTemplate("%nation%", player.getNationName()));
            } else {
                logger.warning("Invalid result from askScoutSpeak: " + result);
            }
            nextActiveUnit();
            break;
        case INDIAN_SETTLEMENT_TRIBUTE:
            moveTribute(unit, direction);
            break;
        default:
            throw new IllegalArgumentException("showScoutIndianSettlementDialog fail");
        }
    }

    /**
     * Spy on a foreign colony.
     *
     * @param unit The <code>Unit</code> that is spying.
     * @param direction The <code>Direction</code> of a colony to spy on.
     */
    private void moveSpy(Unit unit, Direction direction) {
        if (askServer().spy(unit, direction)) {
            nextActiveUnit();
        }
    }

    /**
     * Arrive at a settlement with a laden carrier following a move of
     * MoveType.ENTER_SETTLEMENT_WITH_CARRIER_AND_GOODS.
     *
     * @param unit The carrier.
     * @param direction The direction to the settlement.
     */
    private void moveTrade(Unit unit, Direction direction) {
        clearGotoOrders(unit);

        Settlement settlement = getSettlementAt(unit.getTile(), direction);
        if (settlement instanceof Colony) {
            moveTradeColony(unit, direction);
        } else if (settlement instanceof IndianSettlement) {
            moveTradeIndianSettlement(unit, direction);
        } else {
            logger.warning("Bogus settlement: " + settlement.getId());
        }
    }

    /**
     * Initiates a negotiation with a foreign power. The player
     * creates a DiplomaticTrade with the NegotiationDialog. The
     * DiplomaticTrade is sent to the other player. If the other
     * player accepts the offer, the trade is concluded.  If not, this
     * method returns, since the next offer must come from the other
     * player.
     *
     * @param unit The <code>Unit</code> negotiating.
     * @param direction The direction of a settlement to negotiate with.
     */
    private void moveTradeColony(Unit unit, Direction direction) {
        Settlement settlement = getSettlementAt(unit.getTile(), direction);
        if (settlement == null) return;

        // Can not negotiate with the REF.
        Player player = unit.getOwner();
        if (settlement.getOwner() == player.getREFPlayer()) {
            throw new IllegalStateException("Unit tried to negotiate with REF");
        }

        ModelMessage m = null;
        String nation
            = Messages.message(settlement.getOwner().getNationName());
        DiplomaticTrade ourAgreement = null;
        DiplomaticTrade agreement = null;
        TradeStatus status;
        for (;;) {
            ourAgreement = gui.showNegotiationDialog(unit, settlement, agreement);
            if (ourAgreement == null) {
                if (agreement == null) break;
                agreement.setStatus(TradeStatus.REJECT_TRADE);
            } else {
                agreement = ourAgreement;
            }
            if (agreement.getStatus() != TradeStatus.PROPOSE_TRADE) {
                askServer().diplomacy(freeColClient.getGame(), unit, settlement, agreement);
                gui.updateMenuBar();
                break;
            }

            agreement = askServer().diplomacy(freeColClient.getGame(), unit, settlement, agreement);
            status = (agreement == null) ? TradeStatus.REJECT_TRADE
                : agreement.getStatus();
            switch (status) {
            case PROPOSE_TRADE:
                continue; // counter proposal, try again
            case ACCEPT_TRADE:
                m = new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                                     "negotiationDialog.offerAccepted",
                                     settlement)
                    .addName("%nation%", nation);
                break;
            case REJECT_TRADE:
                m = new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                                     "negotiationDialog.offerRejected",
                                     settlement)
                    .addName("%nation%", nation);
                break;
            default:
                throw new IllegalStateException("Bogus trade status" + status);
            }
            player.addModelMessage(m);
            break; // only counter proposals should loop
        }
        nextActiveUnit();
    }

    /**
     * Trading with the natives, including buying, selling and
     * delivering gifts.  (Deliberate use of Settlement rather than
     * IndianSettlement throughout these routines as some unification
     * with colony trading is anticipated, and the native AI already
     * uses the same DeliverGiftMessage to deliver gifts to Colonies).
     *
     * @param unit The <code>Unit</code> that is a carrier containing goods.
     * @param direction The direction the unit could move in order to enter a
     *            <code>Settlement</code>.
     * @exception IllegalArgumentException if the unit is not a carrier, or if
     *                there is no <code>Settlement</code> in the given
     *                direction.
     * @see Settlement
     */
    private void moveTradeIndianSettlement(Unit unit, Direction direction) {
        Settlement settlement = getSettlementAt(unit.getTile(), direction);
        boolean[] results;
        boolean done = false;

        while (!done) {
            results = askServer().openTransactionSession(unit, settlement);
            if (results == null) break;
            // The session tracks buy/sell/gift events and disables
            // them when one happens.  So only offer such options if
            // the session allows it and the carrier is in good shape.
            boolean buy = results[0] && unit.getSpaceLeft() > 0;
            boolean sel = results[1] && unit.getGoodsCount() > 0;
            boolean gif = results[2] && unit.getGoodsCount() > 0;
            if (!buy && !sel && !gif) break;

            switch (gui.showIndianSettlementTradeDialog(settlement,
                    buy, sel, gif)) {
            case CANCEL:
                done = true;
                break;
            case BUY:
                attemptBuyFromSettlement(unit, settlement);
                break;
            case SELL:
                attemptSellToSettlement(unit, settlement);
                break;
            case GIFT:
                attemptGiftToSettlement(unit, settlement);
                break;
            default:
                throw new IllegalArgumentException("showIndianSettlementTradeDialog fail");
            }
        }

        askServer().closeTransactionSession(unit, settlement);
        if (unit.getMovesLeft() > 0) { // May have been restored if no trade
            gui.setActiveUnit(unit);
        } else {
            nextActiveUnit();
        }
    }

    /**
     * Displays an appropriate trade failure message.
     *
     * @param fail The failure state.
     * @param settlement The <code>Settlement</code> that failed to trade.
     * @param goods The <code>Goods</code> that failed to trade.
     */
    private void showTradeFail(int fail, Settlement settlement, Goods goods) {
        switch (fail) {
        case NO_TRADE_GOODS:
            gui.showInformationMessage(settlement,
                StringTemplate.template("trade.noTradeGoods")
                .add("%goods%", goods.getNameKey()));
            return;
        case NO_TRADE_HAGGLE:
            gui.showInformationMessage(settlement, "trade.noTradeHaggle");
            break;
        case NO_TRADE_HOSTILE:
            gui.showInformationMessage(settlement, "trade.noTradeHostile");
            break;
        case NO_TRADE: // Proposal was refused
        default:
            gui.showInformationMessage(settlement, "trade.noTrade");
            break;
        }
    }

    /**
     * User interaction for buying from the natives.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     */
    private void attemptBuyFromSettlement(Unit unit, Settlement settlement) {
        Player player = freeColClient.getMyPlayer();
        Goods goods = null;

        // Get list of goods for sale
        List<Goods> forSale
            = askServer().getGoodsForSaleInSettlement(freeColClient.getGame(), unit, settlement);
        for (;;) {
            if (forSale.isEmpty()) {
                // There is nothing to sell to the player
                gui.showInformationMessage(settlement,
                    "trade.nothingToSell");
                return;
            }

            // Choose goods to buy
            goods = gui.showSimpleChoiceDialog(unit.getTile(),
                "buyProposition.text", "buyProposition.nothing", forSale);
            if (goods == null) break; // Trade aborted by the player

            int gold = -1; // Initially ask for a price
            for (;;) {
                gold = askServer().buyProposition(unit, settlement,
                    goods, gold);
                if (gold <= 0) {
                    showTradeFail(gold, settlement, goods);
                    return;
                }

                // Show dialog for buy proposal
                boolean canBuy = player.checkGold(gold);
                switch (gui.showBuyDialog(unit, settlement, goods, gold,
                        canBuy)) {
                case CANCEL: // User cancelled
                    return;
                case BUY: // Accept price, make purchase
                    if (askServer().buyFromSettlement(unit,
                            settlement, goods, gold)) {
                        gui.updateMenuBar(); // Assume success
                    }
                    return;
                case HAGGLE: // Try to negotiate a lower price
                    gold = gold * 9 / 10;
                    break;
                default:
                    throw new IllegalStateException("showBuyDialog fail");
                }
            }
        }
    }

    /**
     * User interaction for selling to the natives.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     */
    private void attemptSellToSettlement(Unit unit, Settlement settlement) {
        Goods goods = null;
        for (;;) {
            // Choose goods to sell
            goods = gui.showSimpleChoiceDialog(unit.getTile(),
                "sellProposition.text", "sellProposition.nothing",
                unit.getGoodsList());
            if (goods == null) break; // Trade aborted by the player

            int gold = -1; // Initially ask for a price
            for (;;) {
                gold = askServer().sellProposition(unit, settlement,
                    goods, gold);

                if (gold <= 0) {
                    showTradeFail(gold, settlement, goods);
                    return;
                }

                // Show dialog for sale proposal
                switch (gui.showSellDialog(unit, settlement, goods, gold)) {
                case CANCEL:
                    return;
                case SELL: // Accepted price, make the sale
                    if (askServer().sellToSettlement(unit,
                            settlement, goods, gold)) {
                        gui.updateMenuBar(); // Assume success
                    }
                    return;
                case HAGGLE: // Ask for more money
                    gold = (gold * 11) / 10;
                    break;
                case GIFT: // Decide to make a gift of the goods
                    askServer().deliverGiftToSettlement(unit,
                        settlement, goods);
                    return;
                default:
                    throw new IllegalStateException("showSellDialog fail");
                }
            }
        }
    }

    /**
     * User interaction for delivering a gift to the natives.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     */
    private void attemptGiftToSettlement(Unit unit, Settlement settlement) {
        Goods goods = gui.showSimpleChoiceDialog(unit.getTile(),
            "gift.text", "cancel", unit.getGoodsList());
        if (goods != null) {
            askServer().deliverGiftToSettlement(unit, settlement, goods);
        }
    }

    /**
     * Demand a tribute.
     *
     * @param unit The <code>Unit</code> to perform the attack.
     * @param direction The direction in which to attack.
     */
    private void moveTribute(Unit unit, Direction direction) {
        if (askServer().demandTribute(unit, direction)) {
            // Assume tribute paid
            gui.updateMenuBar();
            nextActiveUnit();
        }
    }

    /**
     * Move a missionary into a native settlement, following a move of
     * MoveType.ENTER_INDIAN_SETTLEMENT_WITH_MISSIONARY.
     *
     * @param unit The <code>Unit</code> that will enter the settlement.
     * @param direction The direction in which the Indian settlement lies.
     */
    private void moveUseMissionary(Unit unit, Direction direction) {
        IndianSettlement settlement
            = (IndianSettlement) getSettlementAt(unit.getTile(), direction);
        Unit missionary = settlement.getMissionary();
        boolean canEstablish = missionary == null;
        boolean canDenounce = missionary != null
            && missionary.getOwner() != unit.getOwner();
        clearGotoOrders(unit);

        // Offer the choices.
        switch (gui.showUseMissionaryDialog(unit, settlement,
                canEstablish, canDenounce)) {
        case CANCEL:
            return;
        case ESTABLISH_MISSION:
            if (askServer().missionary(unit, direction, false)) {
                if (settlement.getMissionary() == unit) {
                    gui.playSound("sound.event.missionEstablished");
                }
                nextActiveUnit();
            }
            break;
        case DENOUNCE_HERESY:
            if (askServer().missionary(unit, direction, true)) {
                if (settlement.getMissionary() == unit) {
                    gui.playSound("sound.event.missionEstablished");
                }
                nextModelMessage();
                nextActiveUnit();
            }
            break;
        case INCITE_INDIANS:
            List<Player> enemies = new ArrayList<Player>(freeColClient
                .getGame().getLiveEuropeanPlayers());
            Player player = freeColClient.getMyPlayer();
            enemies.remove(player);
            Player enemy = gui.showSimpleChoiceDialog(unit.getTile(),
                "missionarySettlement.inciteQuestion",
                "missionarySettlement.cancel",
                enemies);
            if (enemy == null) return;
            int gold = askServer().incite(unit, direction, enemy, -1);
            if (gold < 0) {
                // protocol fail
            } else if (!player.checkGold(gold)) {
                gui.showInformationMessage(settlement,
                    StringTemplate.template("missionarySettlement.inciteGoldFail")
                    .add("%player%", enemy.getName())
                    .addAmount("%amount%", gold));
            } else {
                if (gui.showConfirmDialog(unit.getTile(),
                        StringTemplate.template("missionarySettlement.inciteConfirm")
                        .add("%player%", enemy.getName())
                        .addAmount("%amount%", gold),
                        "yes", "no")) {
                    if (askServer().incite(unit, direction, enemy, gold) >= 0) {
                        gui.updateMenuBar();
                    }
                }
                nextActiveUnit();
            }
            break;
        default:
            logger.warning("showUseMissionaryDialog fail");
            break;
        }
    }

    // end move-consequents

    /**
     * Describe <code>moveTileCursor</code> method here.
     *
     * @param direction a <code>Direction</code> value 
     */
    public void moveTileCursor(Direction direction) {
        if (gui.getSelectedTile() != null) {
            Tile newTile = gui.getSelectedTile().getNeighbourOrNull(direction);
            if (newTile != null) 
                gui.setSelectedTile(newTile, false);
        } else {
            logger.warning("selectedTile is null");
        }
    } 

    /**
     * Makes a new unit active.
     */
    public void nextActiveUnit() {
        nextActiveUnit(null);
    }

    /**
     * Makes a new unit active if any, or focus on a tile (useful if the
     * current unit just died).
     * Displays any new <code>ModelMessage</code>s with
     * {@link #nextModelMessage}.
     *
     * @param tile The <code>Tile</code> to select if no new unit can
     *             be made active.
     */
    public void nextActiveUnit(Tile tile) {
        if (!requireOurTurn()) return;

        // Always flush outstanding messages first.
        nextModelMessage();
        //if (canvas.isShowingSubPanel()) {
        //    canvas.getShowingSubPanel().requestFocus();
        //    return;
        //}

        // Flush any outstanding orders once the mode is raised.
        if (moveMode >= MODE_EXECUTE_GOTO_ORDERS
            && !doExecuteGotoOrders()) {
            return;
        }

        // Look for active units.
        Player player = freeColClient.getMyPlayer();
        Unit unit = gui.getActiveUnit();
        if (unit != null
            && unit.couldMove()) return; // Current active unit has more to do.
        if (player.hasNextActiveUnit()) {
            gui.setActiveUnit(player.getNextActiveUnit());
            return; // Successfully found a unit to display
        }

        // No active units left.  Do the goto orders.
        if (!doExecuteGotoOrders()) return;

        // If not already ending the turn, use the fallback tile if
        // supplied, then check for automatic end of turn, otherwise
        // just select nothing and wait.
        gui.setActiveUnit(null);
        ClientOptions options = freeColClient.getClientOptions();
        if (tile != null) {
            gui.setSelectedTile(tile, false);
        } else if (options.getBoolean(ClientOptions.AUTO_END_TURN)) {
            endTurn();
        }
    }

    /**
     * Pays the tax arrears on this type of goods.
     *
     * @param type The type of goods for which to pay arrears.
     * @return True if the arrears were paid.
     */
    public boolean payArrears(GoodsType type) {
        if (!requireOurTurn()) return false;

        Player player = freeColClient.getMyPlayer();
        int arrears = player.getArrears(type);
        if (arrears <= 0) return false;
        if (!player.checkGold(arrears)) {
            gui.showInformationMessage(StringTemplate.template("model.europe.cantPayArrears")
                .addAmount("%amount%", arrears));
            return false;
        }
        if (gui.showConfirmDialog(null,
                StringTemplate.template("model.europe.payArrears")
                .addAmount("%amount%", arrears),
                "ok", "cancel")
            && askServer().payArrears(type)
            && player.canTrade(type)) {
            gui.updateMenuBar();
            return true;
        }
        return false;
    }

    /**
     * Buys the remaining hammers and tools for the {@link Building} currently
     * being built in the given <code>Colony</code>.
     *
     * @param colony The {@link Colony} where the building should be bought.
     */
    public void payForBuilding(Colony colony) {
        if (!requireOurTurn()) return;

        if (!colony.canPayToFinishBuilding()) {
            gui.errorMessage("notEnoughGold");
            return;
        }
        int price = colony.getPriceForBuilding();
        if (!gui.showConfirmDialog(null,
                StringTemplate.template("payForBuilding.text")
                .addAmount("%amount%", price),
                "payForBuilding.yes", "payForBuilding.no")) {
            return;
        }

        ColonyWas colonyWas = new ColonyWas(colony);
        if (askServer().payForBuilding(colony)
            && colony.getPriceForBuilding() == 0) {
            colonyWas.fireChanges();
            gui.updateMenuBar();
        }
    }

    /**
     * Puts the specified unit outside the colony.
     *
     * @param unit The <code>Unit</code>
     * @return <i>true</i> if the unit was successfully put outside the colony.
     */
    public boolean putOutsideColony(Unit unit) {
        if (!requireOurTurn()) return false;

        Colony colony = unit.getColony();
        if (colony == null) {
            throw new IllegalStateException("Unit is not in colony.");
        }
        if (!gui.tryLeaveColony(unit)) return false;

        ColonyWas colonyWas = new ColonyWas(colony);
        UnitWas unitWas = new UnitWas(unit);
        if (askServer().putOutsideColony(unit)) {
            colonyWas.fireChanges();
            unitWas.fireChanges();
            return true;
        }
        return false;
    }

    /**
     * Recruit a unit from a specified index in Europe.
     *
     * @param index The index in Europe to recruit from ([0..2]).
     */
    public void recruitUnitInEurope(int index) {
        if (!requireOurTurn()) return;

        Player player = freeColClient.getMyPlayer();
        if (!player.checkGold(player.getRecruitPrice())) {
            gui.errorMessage("notEnoughGold");
            return;
        }

        emigrate(player, index + 1);
    }

    /**
     * Renames a <code>Nameable</code>.
     * Apparently this can be done while it is not your turn.
     *
     * @param object The object to rename.
     */
    public void rename(Nameable object) {
        Player player = freeColClient.getMyPlayer();
        if (!(object instanceof Ownable)
            || !player.owns((Ownable) object)) {
            return;
        }

        String name = null;
        if (object instanceof Colony) {
            Colony colony = (Colony) object;
            name = gui.showInputDialog(colony.getTile(),
                StringTemplate.key("renameColony.text"), colony.getName(),
                "renameColony.yes", "renameColony.no", true);
            if (name == null) {
                // User cancelled, 0-length invalid.
                return;
            } else if (colony.getName().equals(name)) {
                // No change
                return;
            } else if (player.getSettlement(name) != null) {
                // Colony name must be unique.
                gui.showInformationMessage((Colony) object,
                    StringTemplate.template("nameColony.notUnique")
                    .addName("%name%", name));
                return;
            }
        } else if (object instanceof Unit) {
            Unit unit = (Unit) object;
            name = gui.showInputDialog(unit.getTile(),
                StringTemplate.key("renameUnit.text"), unit.getName(),
                "renameUnit.yes", "renameUnit.no", false);
            if (name == null) return; // User cancelled, 0-length clears name.
        } else {
            logger.warning("Tried to rename an unsupported Nameable: "
                + object.toString());
            return;
        }

        askServer().rename((FreeColGameObject) object, name);
    }

    /**
     * Selects a destination for this unit. Europe and the player's
     * colonies are valid destinations.
     *
     * @param unit The unit for which to select a destination.
     */
    public void selectDestination(Unit unit) {
        Location destination = gui.showSelectDestinationDialog(unit);
        if (destination == null) return; // user aborted

        if (setDestination(unit, destination)
            && freeColClient.currentPlayerIsMyPlayer()) {
            if (destination instanceof Europe) {
                if (unit.getTile() != null
                    && unit.getTile().isDirectlyHighSeasConnected()) {
                    moveTo(unit, destination);
                } else {
                    moveToDestination(unit);
                }
            } else {
                if (unit.isInEurope()) {
                    moveTo(unit, destination);
                } else {
                    moveToDestination(unit);
                }
            }
        }
        nextActiveUnit(); // Unit may have become unmovable.
    }

    /**
     * Sells goods in Europe.
     *
     * @param goods The goods to be sold.
     * @return True if the sale succeeds.
     */
    public boolean sellGoods(Goods goods) {
        if (!requireOurTurn()) return false;

        // Sanity checks.
        Player player = freeColClient.getMyPlayer();
        if (goods == null) {
            throw new NullPointerException("Goods must not be null.");
        }
        Unit carrier = null;
        if (goods.getLocation() instanceof Unit) {
            carrier = (Unit) goods.getLocation();
        }
        if (carrier == null) {
            throw new IllegalStateException("Goods not on carrier.");
        } else if (!carrier.isInEurope()) {
            throw new IllegalStateException("Goods not on carrier in Europe.");
        } else if (!player.canTrade(goods)) {
            throw new IllegalStateException("Goods are boycotted.");
        }

        // Try to sell.  Remember a bunch of stuff first so the transaction
        // can be logged.
        Market market = player.getMarket();
        GoodsType type = goods.getType();
        int amount = goods.getAmount();
        int price = market.getPaidForSale(type);
        int tax = player.getTax();
        int oldAmount = carrier.getGoodsContainer().getGoodsCount(type);
        UnitWas unitWas = new UnitWas(carrier);
        if (askServer().sellGoods(goods, carrier)
            && carrier.getGoodsContainer().getGoodsCount(type) != oldAmount) {
            gui.playSound("sound.event.sellCargo");
            unitWas.fireChanges();
            for (TransactionListener l : market.getTransactionListener()) {
                l.logSale(type, amount, price, tax);
            }
            gui.updateMenuBar();
            nextModelMessage();
            return true;
        }

        // Sale failed for some reason.
        return false;
    }

    /**
     * Sends a public chat message.
     *
     * @param chat The text of the message.
     */
    public void sendChat(String chat) {
        askServer().chat(freeColClient.getMyPlayer(), chat);
    }

    /**
     * Changes the current construction project of a <code>Colony</code>.
     *
     * @param colony The <code>Colony</code>
     * @param buildQueue List of <code>BuildableType</code>
     */
    public void setBuildQueue(Colony colony, List<BuildableType> buildQueue) {
        if (!requireOurTurn()) return;

        ColonyWas colonyWas = new ColonyWas(colony);
        if (askServer().setBuildQueue(colony, buildQueue)) {
            colonyWas.fireChanges();
        }
    }

    /**
     * Set a player to be the new current player.
     *
     * @param player The <code>Player</code> to be the new current player.
     */
    public void setCurrentPlayer(Player player) {
        logger.finest("Entering client setCurrentPlayer: " + player.getName());
        Game game = freeColClient.getGame();
        game.setCurrentPlayer(player);

        if (freeColClient.getMyPlayer().equals(player)
            && freeColClient.getFreeColServer() != null) {

           // auto-save the game (if it isn't newly loaded)
           if ( turnsPlayed > 0 ) {
              autosave_game();
           }

           player.invalidateCanSeeTiles();

           // Check for emigration.
           while (player.checkEmigrate()) {
                if (player.hasAbility("model.ability.selectRecruit")
                    && player.getEurope().recruitablesDiffer()) {
                    int index = gui.showEmigrationPanel(false);
                    emigrate(player, index + 1);
                } else {
                    emigrate(player, 0);
                }
           }

           // GUI management.
           if (!freeColClient.isSinglePlayer()) {
               gui.playSound("sound.anthem." + player.getNationID());
           }
           displayModelMessages(true, true);
        }
        logger.finest("Exiting client setCurrentPlayer: " + player.getName());
    }

    /**
     * Set the destination of the given unit.
     *
     * @param unit The <code>Unit</code> to direct.
     * @param destination The destination <code>Location</code>.
     * @return True if the destination was set.
     * @see Unit#setDestination(Location)
     */
    public boolean setDestination(Unit unit, Location destination) {
        if (unit.getTradeRoute() != null) {
            StringTemplate template
                = StringTemplate.template("traderoute.reassignRoute")
                .addStringTemplate("%unit%", Messages.getLabel(unit))
                .addName("%route%", unit.getTradeRoute().getName());
            if (!gui.showConfirmDialog(unit.getTile(), template,
                    "yes", "no")) return false;
        }
        return askServer().setDestination(unit, destination)
            && unit.getDestination() == destination;
    }

    /**
     * Sets the export settings of the custom house.
     *
     * @param colony The colony with the custom house.
     * @param goodsType The goods for which to set the settings.
     */
    public void setGoodsLevels(Colony colony, GoodsType goodsType) {
        askServer().setGoodsLevels(colony,
            colony.getExportData(goodsType));
    }

    /**
     * Sets the trade routes for this player
     *
     * @param routes The trade routes to set.
     */
    public void setTradeRoutes(List<TradeRoute> routes) {
        askServer().setTradeRoutes(routes);
    }

    /**
     * Skip a unit.
     */
    public void skipActiveUnit() {
        final Unit unit = gui.getActiveUnit();
        if (unit != null && unit.getState() != UnitState.SKIPPED) {
            unit.setState(UnitState.SKIPPED);
        }
        nextActiveUnit();
    }

    /**
     * Trains a unit of a specified type in Europe.
     *
     * @param unitType The type of unit to be trained.
     */
    public void trainUnitInEurope(UnitType unitType) {
        if (!requireOurTurn()) return;

        Player player = freeColClient.getMyPlayer();
        Europe europe = player.getEurope();
        if (!player.checkGold(europe.getUnitPrice(unitType))) {
            gui.errorMessage("notEnoughGold");
            return;
        }

        EuropeWas europeWas = new EuropeWas(europe);
        if (askServer().trainUnitInEurope(unitType)) {
            gui.updateMenuBar();
            europeWas.fireChanges();
        }
    }

    /**
     * Unload, including dumping cargo.
     *
     * @param unit The <code>Unit<code> that is dumping.
     */
    public void unload(Unit unit) {
        if (!requireOurTurn()) return;

        // Sanity tests.
        if (unit == null) {
            throw new IllegalArgumentException("Null unit.");
        } else if (!unit.isCarrier()) {
            throw new IllegalArgumentException("Unit is not a carrier.");
        }

        Player player = freeColClient.getMyPlayer();
        boolean inEurope = unit.isInEurope();
        if (unit.getColony() != null) {
            // In colony, unload units and goods.
            for (Unit u : unit.getUnitList()) {
                leaveShip(u);
            }
            for (Goods goods : new ArrayList<Goods>(unit.getGoodsList())) {
                unloadCargo(goods, false);
            }
        } else {
            if (inEurope) { // In Europe, unload non-boycotted goods
                for (Goods goods : new ArrayList<Goods>(unit.getGoodsList())) {
                    if (player.canTrade(goods)) {
                        unloadCargo(goods, false);
                    }
                }
            }
            // Goods left here must be dumped.
            if (unit.getGoodsCount() > 0) {
                List<Goods> goodsList
                    = gui.showDumpCargoDialog(unit);
                if (goodsList != null) {
                    for (Goods goods : goodsList) {
                        unloadCargo(goods, true);
                    }
                }
            }
        }
    }

    /**
     * Unload cargo. If the unit carrying the cargo is not in a
     * harbour, or if the given boolean is true, the goods will be
     * dumped.
     *
     * @param goods The <code>Goods<code> to unload.
     * @param dump If true, dump the goods.
     */
    public void unloadCargo(Goods goods, boolean dump) {
        if (!requireOurTurn()) return;

        // Sanity tests.
        if (goods == null) {
            throw new IllegalArgumentException("Null goods.");
        } else if (goods.getAmount() <= 0) {
            throw new IllegalArgumentException("Empty goods.");
        }
        Unit carrier = null;
        if (!(goods.getLocation() instanceof Unit)) {
            throw new IllegalArgumentException("Unload from non-unit.");
        }
        carrier = (Unit) goods.getLocation();
        Colony colony = null;
        if (!carrier.isInEurope()) {
            if (carrier.getTile() == null) {
                throw new IllegalArgumentException("Carrier with null location.");
            }
            colony = carrier.getColony();
            if (!dump && colony == null) {
                throw new IllegalArgumentException("Unload is really a dump.");
            }
        }

        // Try to unload.  TODO: should there be a sound for this?
        unloadGoods(goods, carrier, colony);
    }

    /**
     * Updates a trade route.
     *
     * @param route The trade route to update.
     */
    public void updateTradeRoute(TradeRoute route) {
        askServer().updateTradeRoute(route);
    }

    /**
     * Tell a unit to wait.
     */
    public void waitActiveUnit() {
        gui.setActiveUnit(null);
        nextActiveUnit();
    }

    /**
     * Moves a <code>Unit</code> to a <code>WorkLocation</code>.
     *
     * @param unit The <code>Unit</code>.
     * @param workLocation The <code>WorkLocation</code>.
     */
    public void work(Unit unit, WorkLocation workLocation) {
        if (!requireOurTurn()) return;

        if (unit.getStudent() != null
            && !gui.confirmAbandonEducation(unit, false)) return;

        Colony colony = workLocation.getColony();
        if (workLocation instanceof ColonyTile) {
            Tile tile = ((ColonyTile) workLocation).getWorkTile();
            if (tile.hasLostCityRumour()) {
                gui.showInformationMessage("tileHasRumour");
                return;
            }
            if (tile.getOwner() != unit.getOwner()) {
                if (!claimLand(tile, ((colony != null) ? colony : unit),
                               0)) return;
            }
        }

        // Try to change the work location.
        ColonyWas colonyWas = new ColonyWas(colony);
        UnitWas unitWas = new UnitWas(unit);
        if (askServer().work(unit, workLocation)
            && unit.getLocation() == workLocation) {
            colonyWas.fireChanges();
            unitWas.fireChanges();
        }
    }
}
