/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

package net.sf.freecol.server.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.CombatModel.CombatResult;
import net.sf.freecol.common.model.Constants.IndianDemandAction;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.DiplomaticTrade.TradeStatus;
import net.sf.freecol.common.model.Disaster;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Europe.MigrationType;
import net.sf.freecol.common.model.ExportData;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.Force;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsLocation;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.HighScore;
import net.sf.freecol.common.model.HighSeas;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.IndianNationType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Market.Access;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.ModelMessage.MessageType;
import net.sf.freecol.common.model.Monarch;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.model.Nameable;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationSummary;
import net.sf.freecol.common.model.NativeTrade;
import net.sf.freecol.common.model.NativeTradeItem;
import net.sf.freecol.common.model.NativeTrade.NativeTradeAction;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.PlayerType;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.model.RandomRange;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.TradeRouteStop;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitChangeType;
import net.sf.freecol.common.model.UnitTypeChange;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitLocation;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.option.GameOptions;
import net.sf.freecol.common.networking.ChangeSet;
import net.sf.freecol.common.networking.ChangeSet.See;
import net.sf.freecol.common.networking.ChatMessage;
import net.sf.freecol.common.networking.DiplomacyMessage;
import net.sf.freecol.common.networking.GameEndedMessage;
import net.sf.freecol.common.networking.GameStateMessage;
import net.sf.freecol.common.networking.HighScoresMessage;
import net.sf.freecol.common.networking.InciteMessage;
import net.sf.freecol.common.networking.IndianDemandMessage;
import net.sf.freecol.common.networking.LootCargoMessage;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.MonarchActionMessage;
import net.sf.freecol.common.networking.NationSummaryMessage;
import net.sf.freecol.common.networking.NativeTradeMessage;
import net.sf.freecol.common.networking.NewTradeRouteMessage;
import net.sf.freecol.common.networking.RearrangeColonyMessage.Arrangement;
import net.sf.freecol.common.networking.ScoutSpeakToChiefMessage;
import net.sf.freecol.common.networking.SetCurrentPlayerMessage;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.common.util.RandomChoice;
import static net.sf.freecol.common.util.RandomUtils.*;
import static net.sf.freecol.common.util.StringUtils.*;
import net.sf.freecol.common.util.Utils;

import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.REFAIPlayer;
import net.sf.freecol.server.model.DiplomacySession;
import net.sf.freecol.server.model.LootSession;
import net.sf.freecol.server.model.MonarchSession;
import net.sf.freecol.server.model.NativeDemandSession;
import net.sf.freecol.server.model.NativeTradeSession;
import net.sf.freecol.server.model.ServerColony;
import net.sf.freecol.server.model.ServerEurope;
import net.sf.freecol.server.model.ServerGame;
import net.sf.freecol.server.model.ServerIndianSettlement;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerRegion;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.server.model.Session;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


/**
 * The main server controller.
 */
public final class InGameController extends Controller {

    private static final Logger logger = Logger.getLogger(InGameController.class.getName());

    private static final Predicate<Player> coronadoPred = p ->
        p.hasAbility(Ability.SEE_ALL_COLONIES);

    /** The server random number source. */
    private Random random;

    /** Debug helpers, do not serialize. */
    private int debugOnlyAITurns = 0;
    private MonarchAction debugMonarchAction = null;
    private ServerPlayer debugMonarchPlayer = null;


    /**
     * The constructor to use.
     *
     * @param freeColServer The main server object.
     */
    public InGameController(FreeColServer freeColServer) {
        super(freeColServer);

        this.random = null;
    }


    /**
     * Set the PRNG.
     *
     * @param random The new {@code Random} to use in this controller.
     */
    public void setRandom(Random random) {
        this.random = random;
    }

    /**
     * Get the timeout for this game.
     *
     * @return A timeout.
     */
    private long getTimeout() {
        final boolean single = getFreeColServer().getSinglePlayer();
        return FreeCol.getTimeout(single);
    }


    // Debug support

    /**
     * Gets the number of AI turns to skip through.
     *
     * @return The number of terms to skip.
     */
    public int getSkippedTurns() {
        return (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.MENUS))
            ? debugOnlyAITurns : -1;
    }

    /**
     * Sets the number of AI turns to skip through as a debug helper.
     *
     * @param turns The number of turns to skip through.
     */
    public void setSkippedTurns(int turns) {
        if (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.MENUS)) {
            debugOnlyAITurns = turns;
        }
    }

    /**
     * Sets a monarch action to debug/test.
     *
     * @param serverPlayer The {@code ServerPlayer} whose monarch
     *     should act.
     * @param action The {@code MonarchAction} to be taken.
     */
    public void setMonarchAction(ServerPlayer serverPlayer,
                                 MonarchAction action) {
        if (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.MENUS)) {
            debugMonarchPlayer = serverPlayer;
            debugMonarchAction = action;
        }
    }

    /**
     * Debug convenience to step the random number generator.
     *
     * @return The next random number in series, in the range 0-99.
     */
    public int stepRandom() {
        return randomInt(logger, "step random", random, 100);
    }

    /**
     * Public version of csAddFoundingFather so it can be used in the
     * test code and DebugMenu.
     *
     * @param player The {@code Player} who gains a father.
     * @param father The {@code FoundingFather} to add.
     */
    public void addFoundingFather(Player player, FoundingFather father) {
        ChangeSet cs = new ChangeSet();
        ((ServerPlayer)player).csAddFoundingFather(father, random, cs);
        cs.addAttribute(See.only(player), "flush", Boolean.TRUE.toString());
        getGame().sendTo(player, cs);
    }

    /**
     * Public change stance and inform all routine.  Mostly used in the
     * test suite, but the AIs also call it.
     *
     * @param player The originating {@code Player}.
     * @param stance The new {@code Stance}.
     * @param other The {@code Player} wrt which the stance changes.
     * @param symmetric If true, change the otherPlayer stance as well.
     */
    public void changeStance(Player player, Stance stance,
                             Player other, boolean symmetric) {
        ChangeSet cs = new ChangeSet();
        if (((ServerPlayer)player).csChangeStance(stance, other, symmetric, cs)) {
            getGame().sendToAll(cs);
        }
    }

    /**
     * Change colony owner.  Public for DebugUtils.
     *
     * @param colony The {@code ServerColony} to change.
     * @param serverPlayer The {@code ServerPlayer} to change to.
     */
    public void debugChangeOwner(ServerColony colony,
                                 ServerPlayer serverPlayer) {
        ChangeSet cs = new ChangeSet();
        final Player owner = colony.getOwner();
        colony.csChangeOwner(serverPlayer, false, cs);//-vis(serverPlayer,owner)
        serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)
        owner.invalidateCanSeeTiles();//+vis(owner)
        getGame().sendToAll(cs);
    }

    /**
     * Change unit owner.  Public for DebugUtils.
     *
     * @param unit The {@code ServerUnit} to change.
     * @param serverPlayer The {@code ServerPlayer} to change to.
     */
    public void debugChangeOwner(ServerUnit unit, ServerPlayer serverPlayer) {
        final Player owner = unit.getOwner();

        ChangeSet cs = new ChangeSet();
        ((ServerPlayer)owner).csChangeOwner(unit, serverPlayer, null, null,
            cs);//-vis(serverPlayer,owner)
        cs.add(See.perhaps().always(owner), unit.getTile());
        serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)
        owner.invalidateCanSeeTiles();//+vis(owner)
        getGame().sendToAll(cs);
    }

    /**
     * Apply a disaster to a colony.  Public for DebugUtils.
     *
     * @param colony The {@code Colony} to apply the disaster to.
     * @param disaster The {@code Disaster} to apply.
     * @return The number of messages generated.
     */
    public int debugApplyDisaster(ServerColony colony, Disaster disaster) {
        final ServerGame game = getGame();
        final Player owner = colony.getOwner();

        ChangeSet cs = new ChangeSet();
        List<ModelMessage> messages
            = ((ServerPlayer)owner).csApplyDisaster(random, colony, disaster, cs);
        if (!messages.isEmpty()) {
            cs.addGlobalMessage(game, null,
                new ModelMessage(MessageType.DISASTERS,
                                 "model.disaster.strikes", owner)
                    .addName("%colony%", colony.getName())
                    .addName("%disaster%", disaster));
            for (ModelMessage message : messages) {
                cs.addGlobalMessage(game, null, message);
            }
            game.sendToAll(cs);
        }
        return messages.size();
    }


    // Internal utilities

    /**
     * Create the Royal Expeditionary Force player corresponding to
     * a given player that is about to rebel.
     *
     * Public for the test suite.
     *
     * FIXME: this should eventually generate changes for the REF player.
     *
     * @param serverPlayer The {@code ServerPlayer} about to rebel.
     * @return The REF player.
     */
    public ServerPlayer createREFPlayer(ServerPlayer serverPlayer) {
        final Nation refNation = serverPlayer.getNation().getREFNation();
        final Monarch monarch = serverPlayer.getMonarch();
        final ServerPlayer refPlayer = getFreeColServer().makeAIPlayer(refNation);
        final Europe europe = refPlayer.getEurope();
        final Predicate<Tile> exploredPred = t ->
            ((!t.isLand() || t.isCoastland() || t.getOwner() == serverPlayer)
                && t.isExploredBy(serverPlayer));
        // Inherit rebel player knowledge of the seas, coasts, claimed
        // land but not full detailed scouting knowledge.
        Set<Tile> explore = new HashSet<>();
        getGame().getMap().forEachTile(exploredPred, t -> explore.add(t));
        refPlayer.exploreTiles(explore);

        // Trigger initial placement routine
        refPlayer.setEntryTile(null);
        // Will change, setup only
        Player.makeContact(serverPlayer, refPlayer);

        // Instantiate the REF in Europe
        Force exf = monarch.getExpeditionaryForce();
        if (!exf.prepareToBoard()) {
            logger.warning("Unable to ensure space for the REF land units.");
            // For now, do not fail completely
        }
        List<Unit> landUnits = refPlayer.createUnits(exf.getLandUnitsList(),
            europe, null);//-vis: safe!map
        List<Unit> navalUnits = refPlayer.createUnits(exf.getNavalUnitsList(),
            europe, random);//-vis: safe!map
        List<Unit> leftOver = refPlayer.loadShips(landUnits, navalUnits,
            random);//-vis: safe!map
        if (!leftOver.isEmpty()) {
            // Should not happen, make this return null one day
            logger.warning("Failed to board REF units: "
                + join(" ", transform(leftOver, alwaysTrue(),
                                      FreeColObject::getId)));
        }
        return refPlayer;
    }

    /**
     * Buy goods from a native settlement.
     *
     * @param unit The {@code Unit} that is buying.
     * @param goods The {@code Goods} to buy.
     * @param price The price to pay.
     * @param sis The {@code ServerIndianSettlement} to give to.
     * @param cs A {@code ChangeSet} to update.
     */
    private void csBuy(Unit unit, Goods goods, int price,
                       ServerIndianSettlement sis, ChangeSet cs) {
        final Specification spec = getGame().getSpecification();
        final int alarmBonus = -Math.round(price * 0.001f
            * spec.getPercentage(GameOptions.ALARM_BONUS_BUY));
        final Player owner = unit.getOwner();

        csVisit((ServerPlayer)owner, sis, 0, cs);
        GoodsLocation.moveGoods(sis, goods.getType(), goods.getAmount(), unit);
        cs.add(See.perhaps(), unit);
        sis.getOwner().modifyGold(price);
        owner.modifyGold(-price);
        sis.csModifyAlarm(owner, alarmBonus, true, cs);
        sis.updateWantedGoods();
        final Tile tile = sis.getTile();
        tile.updateIndianSettlement(owner);
        cs.add(See.only(owner), tile);
        cs.addPartial(See.only(owner), owner,
                      "gold", String.valueOf(owner.getGold()));
        logger.finest(owner.getSuffix() + " " + unit + " buys " + goods
                      + " at " + sis.getName() + " for " + price);
    }

    /**
     * Sell goods to a native settlement.
     *
     * @param unit The {@code Unit} that is selling.
     * @param goods The {@code Goods} to sell.
     * @param price The price to charge.
     * @param sis The {@code ServerIndianSettlement} to sell to.
     * @param cs A {@code ChangeSet} to update.
     */
    private void csSell(Unit unit, Goods goods, int price,
                        ServerIndianSettlement sis, ChangeSet cs) {
        final Specification spec = getGame().getSpecification();
        final Player owner = unit.getOwner();
        final int alarmBonus = -Math.round(price * 0.001f
            * spec.getPercentage(GameOptions.ALARM_BONUS_SELL));

        csVisit((ServerPlayer)owner, sis, 0, cs);
        GoodsLocation.moveGoods(unit, goods.getType(), goods.getAmount(), sis);
        cs.add(See.perhaps(), unit);
        sis.getOwner().modifyGold(-price);
        owner.modifyGold(price);
        sis.csModifyAlarm(owner, alarmBonus, true, cs);
        sis.updateWantedGoods();
        final Tile tile = sis.getTile();
        tile.updateIndianSettlement(owner);
        cs.add(See.only(owner), tile);
        cs.addPartial(See.only(owner), owner,
                      "gold", String.valueOf(owner.getGold()));
        cs.addSale(owner, sis, goods.getType(),
                   Math.round((float)price/goods.getAmount()));
        logger.finest(owner.getSuffix() + " " + unit + " sells " + goods
                      + " at " + sis.getName() + " for " + price);
    }

    /**
     * Give goods to a native settlement.
     *
     * @param unit The {@code Unit} that is giving.
     * @param goods The {@code Goods} to give.
     * @param price A price that the natives might have been willing to pay.
     * @param sis The {@code ServerIndianSettlement} to give to.
     * @param cs A {@code ChangeSet} to update.
     */
    private void csGift(Unit unit, Goods goods, int price,
                        ServerIndianSettlement sis, ChangeSet cs) {
        final Specification spec = getGame().getSpecification();
        final Player owner = unit.getOwner();
        final int alarmBonus = -Math.round(price * 0.001f
            * spec.getPercentage(GameOptions.ALARM_BONUS_GIFT));

        csVisit((ServerPlayer)owner, sis, 0, cs);
        GoodsLocation.moveGoods(unit, goods.getType(), goods.getAmount(), sis);
        cs.add(See.perhaps(), unit);
        sis.csModifyAlarm(owner, alarmBonus, true, cs);
        sis.updateWantedGoods();
        final Tile tile = sis.getTile();
        tile.updateIndianSettlement(owner);
        cs.add(See.only(owner), tile);
        logger.finest(owner.getSuffix() + " " + unit + " gives " + goods
                      + " at " + sis.getName() + " worth " + price);
    }

    /**
     * Visits a native settlement, possibly scouting it full if it is
     * as a result of a scout actually asking to speak to the chief,
     * or for other settlement-contacting events such as missionary
     * actions, demanding tribute, learning skills and trading if the
     * settlementActionsContactChief game option is enabled.  It is
     * still unclear what Col1 did here.
     *
     * @param serverPlayer The {@code ServerPlayer} that is contacting
     *     the settlement.
     * @param is The {@code IndianSettlement} to contact.
     * @param scout Positive if this contact is due to a scout asking to
     *     speak to the chief, zero if it is another unit, negative if
     *     this is from the greeting dialog generation.
     * @param cs A {@code ChangeSet} to update.
     */
    private void csVisit(ServerPlayer serverPlayer, IndianSettlement is,
                         int scout, ChangeSet cs) {
        final Player owner = is.getOwner();

        if (serverPlayer.csContact(owner, cs)) {
            serverPlayer.csNativeFirstContact(owner, null, cs);
        }
        is.setVisited(serverPlayer);
        if (scout > 0 || (scout == 0 && getGame().getSpecification()
                .getBoolean(GameOptions.SETTLEMENT_ACTIONS_CONTACT_CHIEF))) {
            is.setScouted(serverPlayer);
        }
        // Force the settlement tile to become uncached.  Should not
        // be necessary but this might mitigate BR#3128.
        is.getTile().seeTile(serverPlayer);
    }

    /**
     * Launch the REF.
     *
     * @param serverPlayer The REF {@code ServerPlayer}.
     * @param teleport If true, teleport the REF in.
     * @param cs A {@code ChangeSet} to update.
     */
    private void csLaunchREF(ServerPlayer serverPlayer, boolean teleport,
                             ChangeSet cs) {
        // Set the REF player entry location from the rebels.  Note
        // that the REF units will have their own unit-specific entry
        // locations set by their missions.  This just flags that the
        // REF is initialized.
        Player rebel = first(serverPlayer.getRebels());
        if (rebel != null) {
            serverPlayer.setEntryTile(rebel.getEntryTile());
        }

        if (teleport) { // Teleport in the units.
            List<Unit> naval = transform(serverPlayer.getUnits(), Unit::isNaval);
            Set<Tile> seen = new HashSet<>(naval.size());
            for (Unit u : naval) {
                Tile entry = u.getFullEntryLocation();
                u.setLocation(entry);//-vis(serverPlayer)
                u.setWorkLeft(-1);
                u.setState(Unit.UnitState.ACTIVE);
                if (seen.add(entry)) {
                    cs.add(See.only(serverPlayer),
                           serverPlayer.exploreForUnit(u));
                    cs.add(See.perhaps().except(serverPlayer), entry);
                }
            }
            serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)
        } else {
            // Put navy on the high seas, with 1-turn sail time
            for (Unit u : transform(serverPlayer.getUnits(), Unit::isNaval)) {
                u.setWorkLeft(1);
                u.setDestination(u.getFullEntryLocation());
                u.setLocation(u.getOwner().getHighSeas());//-vis: safe!map
            }
        }
    }

    /**
     * Give independence.  Note that the REF player is granting, but
     * most of the changes happen to the newly independent player.
     * hence the special handling.
     *
     * @param serverPlayer The REF {@code ServerPlayer} that is granting.
     * @param independent The newly independent {@code Player}.
     * @param cs A {@code ChangeSet} to update.
     */
    private void csGiveIndependence(ServerPlayer serverPlayer,
                                    Player independent, ChangeSet cs) {
        serverPlayer.csChangeStance(Stance.PEACE, independent, true, cs);
        independent.changePlayerType(PlayerType.INDEPENDENT);
        Game game = getGame();
        Turn turn = game.getTurn();
        independent.setTax(0);
        independent.reinitialiseMarket();
        HistoryEvent h = new HistoryEvent(turn,
            HistoryEvent.HistoryEventType.INDEPENDENCE, independent);

        // The score for actual independence is actually a percentage
        // bonus depending on how many other nations are independent.
        // If we ever go for a more complex scoring algorithm it might
        // be better to replace the score int with a Modifier, but for
        // now we just set the score value to the number of
        // independent players other than the one we are granting
        // here, and generate the bonus with a special case in
        // ServerPlayer.updateScore().
        int n = count(game.getLiveEuropeanPlayers(independent),
                      p -> p.getPlayerType() == PlayerType.INDEPENDENT);
        h.setScore(n);
        cs.addGlobalHistory(game, h);
        cs.addMessage(independent,
            new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                             "giveIndependence.announce", independent)
                .addStringTemplate("%ref%", serverPlayer.getNationLabel()));

        // Who surrenders?
        final Predicate<Unit> surrenderPred = u -> //-vis(both)
            (u.hasTile() && !u.isNaval() && !u.isOnCarrier()
                && (!u.hasAbility(Ability.REF_UNIT)
                    || u.hasAbility(Ability.CAN_BE_SURRENDERED))
                && serverPlayer.csChangeOwner(u, independent,
                    UnitChangeType.CAPTURE, null, cs));
        List<Unit> surrenderUnits
            = transform(serverPlayer.getUnits(), surrenderPred);
        for (Unit u : surrenderUnits) {
            u.setMovesLeft(0);
            u.setState(Unit.UnitState.ACTIVE);
            cs.add(See.perhaps().always(serverPlayer), u.getTile());
        }
        if (!surrenderUnits.isEmpty()) {
            cs.addMessage(independent,
                new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                                 "giveIndependence.unitsAcquired", independent)
                    .addStringTemplate("%units%",
                         unitTemplate(", ", surrenderUnits)));
            independent.invalidateCanSeeTiles();//+vis(independent)
            serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)
        }

        // Update player type.  Again, a pity to have to do a whole
        // player update, but a partial update works for other players.
        cs.addPartial(See.all().except(independent), independent,
            "playerType", String.valueOf(independent.getPlayerType()));
        cs.addGlobalMessage(game, independent,
            new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                             "giveIndependence.otherAnnounce", independent)
                .addStringTemplate("%nation%", independent.getNationLabel())
                .addStringTemplate("%ref%", serverPlayer.getNationLabel()));
        cs.add(See.only(independent), independent);

        // Reveal the map on independence.
        cs.add(See.only(independent),
               ((ServerPlayer)independent).exploreMap(true));
    }

    private StringTemplate unitTemplate(String base, List<Unit> units) {
        StringTemplate template = StringTemplate.label(base);
        for (Unit u : units) {
            template.addStringTemplate(u.getLabel(Unit.UnitLabelType.PLAIN));
        }
        return template;
    }

    /**
     * Resolves a tax raise.
     *
     * @param serverPlayer The {@code ServerPlayer} whose tax is rising.
     * @param taxRaise The amount of tax raise.
     * @param goods The {@code Goods} for a goods party.
     * @param result Whether the tax was accepted or not.
    private void raiseTax(ServerPlayer serverPlayer, int taxRaise, Goods goods,
                          boolean result) {
        ChangeSet cs = new ChangeSet();
        serverPlayer.csRaiseTax(taxRaise, goods, result, cs);
        getGame().sendTo(serverPlayer, cs);
    }
     */

    /**
     * Performs a monarch action.
     *
     * Note that CHANGE_LATE is used so that these actions follow
     * setting the current player, so that it is the players turn when
     * they respond to a monarch action.
     *
     * @param serverPlayer The {@code ServerPlayer} being acted upon.
     * @param action The monarch action.
     * @param cs A {@code ChangeSet} to update.
     */
    private void csMonarchAction(final ServerPlayer serverPlayer,
                                 MonarchAction action, ChangeSet cs) {
        final Monarch monarch = serverPlayer.getMonarch();
        final Game game = getGame();
        final Specification spec = game.getSpecification();
        boolean valid = monarch.actionIsValid(action);
        if (!valid) return;
        String messageId = action.getTextKey();
        StringTemplate template;
        MonarchActionMessage message;
        String monarchKey = serverPlayer.getNationId();

        switch (action) {
        case NO_ACTION:
            break;
        case RAISE_TAX_WAR: case RAISE_TAX_ACT:
            final int taxRaise = monarch.raiseTax(random);
            final Goods goods = serverPlayer.getMostValuableGoods();
            if (goods == null) {
                logger.finest("Ignoring tax raise, no goods to boycott.");
                break;
            }
            template = StringTemplate.template(messageId)
                .addStringTemplate("%goods%", goods.getType().getLabel())
                .addAmount("%amount%", taxRaise);
            if (action == MonarchAction.RAISE_TAX_WAR) {
                template = template.add("%nation%",
                    Nation.getRandomNonPlayerNationNameKey(game, random));
            } else if (action == MonarchAction.RAISE_TAX_ACT) {
                template = template.addAmount("%number%",
                    randomInt(logger, "Tax act goods", random, 6))
                    .addName("%newWorld%", serverPlayer.getNewLandName());
            }
            cs.add(See.only(serverPlayer),
                   new MonarchActionMessage(action, template, monarchKey)
                       .setTax(taxRaise));
            new MonarchSession(serverPlayer, action, taxRaise, goods).register();
            break;
        case LOWER_TAX_WAR: case LOWER_TAX_OTHER:
            int oldTax = serverPlayer.getTax();
            int taxLower = monarch.lowerTax(random);
            serverPlayer.csSetTax(taxLower, cs);
            template = StringTemplate.template(messageId)
                .addAmount("%difference%", oldTax - taxLower)
                .addAmount("%newTax%", taxLower);
            if (action == MonarchAction.LOWER_TAX_WAR) {
                template = template.add("%nation%",
                    Nation.getRandomNonPlayerNationNameKey(game, random));
            } else {
                template = template.addAmount("%number%",
                    randomInt(logger, "Lower tax reason", random, 5));
            }
            cs.add(See.only(serverPlayer),
                   new MonarchActionMessage(action, template, monarchKey));
            break;
        case WAIVE_TAX:
            cs.add(See.only(serverPlayer),
                   new MonarchActionMessage(action,
                       StringTemplate.template(messageId), monarchKey));
            break;
        case ADD_TO_REF:
            AbstractUnit refAdditions = monarch.addToREF(random);
            if (refAdditions == null) break;
            template = StringTemplate.template(messageId)
                .addAmount("%number%", refAdditions.getNumber())
                .addNamed("%unit%", refAdditions.getType(spec));
            cs.add(See.only(serverPlayer), monarch);
            cs.add(See.only(serverPlayer),
                   new MonarchActionMessage(action, template, monarchKey));
            break;
        case DECLARE_PEACE:
            List<Player> friends = monarch.collectPotentialFriends();
            if (friends.isEmpty()) break;
            Player friend = getRandomMember(logger, "Choose friend",
                                            friends, random);
            serverPlayer.csChangeStance(Stance.PEACE, friend, true, cs);
            cs.add(See.only(serverPlayer),
                   new MonarchActionMessage(action, StringTemplate
                       .template(messageId)
                       .addStringTemplate("%nation%", friend.getNationLabel()),
                       monarchKey));
            break;
        case DECLARE_WAR:
            List<Player> enemies = monarch.collectPotentialEnemies();
            if (enemies.isEmpty()) break;
            Player enemy = getRandomMember(logger, "Choose enemy",
                                           enemies, random);
            List<AbstractUnit> warSupport
                = monarch.getWarSupport(enemy, random);
            int warGold = 0;
            if (!warSupport.isEmpty()) {
                serverPlayer.createUnits(warSupport,
                    serverPlayer.getEurope(), random);//-vis: safe, Europe
                warGold = spec.getInteger(GameOptions.WAR_SUPPORT_GOLD);
                warGold += (warGold/10) * (randomInt(logger, "War support gold",
                                                     random, 5) - 2);
                serverPlayer.modifyGold(warGold);
                cs.addPartial(See.only(serverPlayer), serverPlayer,
                    "gold", String.valueOf(serverPlayer.getGold()),
                    "score", String.valueOf(serverPlayer.getScore()));
                logger.fine("War support v " + enemy.getNation().getSuffix()
                    + " " + warGold + " gold + " + Messages.message(AbstractUnit
                        .getListLabel(", ", warSupport)));
            }
            serverPlayer.csChangeStance(Stance.WAR, enemy, true, cs);
            cs.add(See.only(serverPlayer),
                   new MonarchActionMessage(action, StringTemplate
                       .template((warSupport.isEmpty()) ? messageId
                           : "model.monarch.action.declareWarSupported.text")
                       .addStringTemplate("%nation%", enemy.getNationLabel())
                       .addStringTemplate("%force%",
                           AbstractUnit.getListLabel(", ", warSupport))
                       .addAmount("%gold%", warGold),
                       monarchKey));
            break;
        case SUPPORT_LAND: case SUPPORT_SEA:
            boolean sea = action == MonarchAction.SUPPORT_SEA;
            List<AbstractUnit> support = monarch.getSupport(random, sea);
            if (support.isEmpty()) break;
            serverPlayer.createUnits(support,
                serverPlayer.getEurope(), random);//-vis: safe, Europe
            cs.add(See.only(serverPlayer), serverPlayer.getEurope());
            cs.add(See.only(serverPlayer),
                   new MonarchActionMessage(action, StringTemplate
                       .template(messageId)
                       .addStringTemplate("%addition%",
                           AbstractUnit.getListLabel(", ", support)),
                       monarchKey));
            break;
        case MONARCH_MERCENARIES:
            final List<AbstractUnit> mercenaries = new ArrayList<>();
            final int mercPrice = monarch.loadMercenaries(random, mercenaries);
            if (mercPrice <= 0) break;
            cs.add(See.only(serverPlayer),
                   new MonarchActionMessage(action, StringTemplate
                       .template(messageId)
                       .addAmount("%gold%", mercPrice)
                       .addStringTemplate("%mercenaries%",
                           AbstractUnit.getListLabel(", ", mercenaries)),
                       monarchKey));
            new MonarchSession(serverPlayer, action, mercenaries, mercPrice).register();
            break;
        case HESSIAN_MERCENARIES:
            final List<AbstractUnit> hessians = new ArrayList<>();
            final int hessianPrice = monarch.loadMercenaries(random, hessians);
            if (hessianPrice <= 0) break;
            serverPlayer.csMercenaries(hessianPrice, hessians, action,
                                       random, cs);
            break;
        case DISPLEASURE: default:
            logger.warning("Bogus action: " + action);
            break;
        }
    }


    // Routines that follow implement the controller response to
    // messages.  The convention is to return a change set back to the
    // invoking message handler, but handle changes for other players
    // directly here.


    /**
     * Abandon a settlement.
     *
     * @param serverPlayer The {@code ServerPlayer} that is abandoning.
     * @param settlement The {@code Settlement} to abandon.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet abandonSettlement(ServerPlayer serverPlayer,
                                       Settlement settlement) {
        ChangeSet cs = new ChangeSet();

        // Drop trade routes and create history event before disposing.
        if (settlement instanceof Colony) {
            serverPlayer.csLoseLocation(settlement, cs);
            cs.addHistory(serverPlayer,
                new HistoryEvent(getGame().getTurn(),
                    HistoryEvent.HistoryEventType.ABANDON_COLONY, serverPlayer)
                    .addName("%colony%", settlement.getName()));
        }

        // Comprehensive dispose.
        serverPlayer.csDisposeSettlement(settlement, cs);//+vis

        // FIXME: Player.settlements is still being fixed on the client side.
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Ask about learning a skill at a native settlement.
     *
     * @param serverPlayer The {@code ServerPlayer} that is learning.
     * @param unit The {@code Unit} that is learning.
     * @param is The {@code IndianSettlement} to learn from.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet askLearnSkill(ServerPlayer serverPlayer, Unit unit,
                                   IndianSettlement is) {
        ChangeSet cs = new ChangeSet();

        csVisit(serverPlayer, is, 0, cs);
        Tile tile = is.getTile();
        tile.updateIndianSettlement(serverPlayer);
        cs.add(See.only(serverPlayer), tile);
        unit.setMovesLeft(0);
        cs.addPartial(See.only(serverPlayer), unit,
            "movesLeft", String.valueOf(unit.getMovesLeft()));

        // Do not update others, nothing to see yet.
        return cs;
    }


    /**
     * Assign a student to a teacher.
     *
     * @param serverPlayer The {@code ServerPlayer} that owns the unit.
     * @param student The student {@code Unit}.
     * @param teacher The teacher {@code Unit}.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet assignTeacher(ServerPlayer serverPlayer, Unit student,
                                   Unit teacher) {
        Unit oldStudent = teacher.getStudent();
        Unit oldTeacher = student.getTeacher();

        // Only update units that changed their teaching situation.
        ChangeSet cs = new ChangeSet();
        if (oldTeacher != null) {
            oldTeacher.setStudent(null);
            cs.add(See.only(serverPlayer), oldTeacher);
        }
        if (oldStudent != null) {
            oldStudent.setTeacher(null);
            cs.add(See.only(serverPlayer), oldStudent);
        }
        teacher.setStudent(student);
        teacher.changeWorkType(null);
        student.setTeacher(teacher);
        cs.add(See.only(serverPlayer), student, teacher);
        return cs;
    }


    /**
     * Assign a trade route to a unit.
     *
     * @param serverPlayer The {@code ServerPlayer} that owns the unit.
     * @param unit The unit {@code Unit} to assign to.
     * @param tradeRoute The {@code TradeRoute} to assign.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet assignTradeRoute(ServerPlayer serverPlayer, Unit unit,
                                      TradeRoute tradeRoute) {
        // If clearing a trade route and the unit is at sea, set
        // the destination to the next stop.  Otherwise just clear
        // the destination.
        TradeRouteStop stop;
        unit.setDestination((tradeRoute == null && unit.isAtSea()
                && (stop = unit.getStop()) != null) ? stop.getLocation()
            : null);
        unit.setTradeRoute(tradeRoute);
        if (tradeRoute != null) {
            List<TradeRouteStop> stops = tradeRoute.getStopList();
            int found = -1;
            for (int i = 0; i < stops.size(); i++) {
                if (Map.isSameLocation(unit.getLocation(),
                                       stops.get(i).getLocation())) {
                    found = i;
                    break;
                }
            }
            if (found < 0) found = 0;
            unit.setCurrentStop(found);
        }

        // Only visible to the player
        return new ChangeSet().add(See.only(serverPlayer), unit);
    }


    /**
     * Build a settlement.
     *
     * +til: Resolves many tile appearance changes.
     *
     * @param serverPlayer The {@code ServerPlayer} that is building.
     * @param unit The {@code Unit} that is building.
     * @param name The new settlement name.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet buildSettlement(ServerPlayer serverPlayer, Unit unit,
                                     String name) {
        final ServerGame game = getGame();
        final Specification spec = game.getSpecification();
        ChangeSet cs = new ChangeSet();

        // Build settlement
        Tile tile = unit.getTile();
        Settlement settlement;
        if (Player.ASSIGN_SETTLEMENT_NAME.equals(name)) {
            name = serverPlayer.getSettlementName(random);
        }
        if (serverPlayer.isEuropean()) {
            StringTemplate nation = serverPlayer.getNationLabel();
            settlement = new ServerColony(game, serverPlayer, name, tile);
            for (Tile t : tile.getSurroundingTiles(settlement.getRadius())) {
                t.cacheUnseen();//+til
            }
            Set<Tile> visible = settlement.getVisibleTileSet();
            
            // Check new sightings before placing the settlement
            cs.add(See.only(serverPlayer),
                   serverPlayer.collectNewTiles(visible));
            // Coronado
            for (Player sp : transform(game.getConnectedPlayers(serverPlayer),
                                       coronadoPred)) {
                cs.add(See.only(sp), ((ServerPlayer)sp).exploreForSettlement(settlement));//-vis(sp)
                sp.invalidateCanSeeTiles();//+vis(sp)
                cs.addMessage(sp,
                    new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                                     "buildColony.others", settlement)
                        .addStringTemplate("%nation%", nation)
                        .addStringTemplate("%colony%",
                            settlement.getLocationLabelFor(sp))
                        .addStringTemplate("%region%",
                            tile.getRegion().getLabel()));
            }

            // Place settlement
            serverPlayer.addSettlement(settlement);
            settlement.placeSettlement(false);//-vis(serverPlayer,?),-til
            cs.addHistory(serverPlayer, new HistoryEvent(game.getTurn(),
                    HistoryEvent.HistoryEventType.FOUND_COLONY, serverPlayer)
                .addName("%colony%", settlement.getName()));

            // Remove equipment from founder in case role confuses
            // placement.
            settlement.equipForRole(unit, spec.getDefaultRole(), 0);
        } else {
            IndianNationType nationType
                = (IndianNationType) serverPlayer.getNationType();
            UnitType skill = RandomChoice
                .getWeightedRandom(logger, "Choose skill",
                                   nationType.generateSkillsForTile(tile),
                                   random);
            if (skill == null) { // Seasoned Scout
                List<UnitType> scouts = spec
                    .getUnitTypesWithAbility(Ability.EXPERT_SCOUT);
                skill = getRandomMember(logger, "Choose scout", scouts, random);
            }

            settlement = new ServerIndianSettlement(game, serverPlayer, name,
                                                    tile, false, skill, null);
            for (Tile t : tile.getSurroundingTiles(settlement.getRadius())) {
                t.cacheUnseen();//+til
            }

            // Place settlement
            serverPlayer.addSettlement(settlement);
            settlement.placeSettlement(true);//-vis(serverPlayer),-til

            for (Player p : getGame().getLivePlayerList(serverPlayer)) {
                ((IndianSettlement)settlement).setAlarm(p, (p.isIndian())
                    ? new Tension(Tension.Level.CONTENT.getLimit())
                    : serverPlayer.getTension(p));//-til
            }
        }

        // Join the settlement.
        unit.setLocation(settlement);//-vis(serverPlayer),-til
        unit.setMovesLeft(0);

        // Update with settlement tile, and newly owned tiles.
        cs.add(See.perhaps(), settlement.getOwnedTiles());
        serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)

        // Others can see tile changes.
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Buy goods in Europe.
     *
     * @param serverPlayer The {@code ServerPlayer} that is buying.
     * @param type The {@code GoodsType} to buy.
     * @param amount The amount of goods to buy.
     * @param carrier The {@code Unit} to carry the goods.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    private ChangeSet buyGoods(ServerPlayer serverPlayer, GoodsType type,
                               int amount, Unit carrier) {
        if (!serverPlayer.canTrade(type, Access.EUROPE)) {
            return serverPlayer.clientError("Can not trade boycotted goods");
        }

        ChangeSet cs = new ChangeSet();
        GoodsContainer container = carrier.getGoodsContainer();
        container.saveState();
        int gold = serverPlayer.getGold();
        int buyAmount = serverPlayer.buyInEurope(random, container, type, amount);
        if (buyAmount < 0) {
            return serverPlayer.clientError("Player " + serverPlayer.getName()
                + " tried to buy " + amount + " " + type.getSuffix());
        }
        serverPlayer.propagateToEuropeanMarkets(type, -buyAmount, random);
        serverPlayer.csFlushMarket(type, cs);
        carrier.setMovesLeft(0);
        cs.addPartial(See.only(serverPlayer), serverPlayer,
            "gold", String.valueOf(serverPlayer.getGold()));
        cs.add(See.only(serverPlayer), carrier);
        logger.finest(carrier + " bought " + amount + "(" + buyAmount + ")"
            + " " + type.getSuffix()
            + " in Europe for " + (serverPlayer.getGold() - gold));
        // Action occurs in Europe, nothing is visible to other players.
        return cs;
    }


    /**
     * Cash in a treasure train.
     *
     * @param serverPlayer The {@code ServerPlayer} that is cashing in.
     * @param unit The treasure train {@code Unit} to cash in.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet cashInTreasureTrain(ServerPlayer serverPlayer, Unit unit) {
        final ServerGame game = getGame();
        ChangeSet cs = new ChangeSet();

        // Work out the cash in amount and the message to send.
        int fullAmount = unit.getTreasureAmount();
        int cashInAmount;
        String messageId;
        if (serverPlayer.getPlayerType() == PlayerType.COLONIAL) {
            // Charge transport fee and apply tax
            cashInAmount = (fullAmount - unit.getTransportFee())
                * (100 - serverPlayer.getTax()) / 100;
            messageId = "cashInTreasureTrain.colonial";
        } else {
            // No fee possible, no tax applies.
            cashInAmount = fullAmount;
            messageId = "cashInTreasureTrain.independent";
        }

        serverPlayer.modifyGold(cashInAmount);
        cs.addPartial(See.only(serverPlayer), serverPlayer,
            "gold", String.valueOf(serverPlayer.getGold()),
            "score", String.valueOf(serverPlayer.getScore()));
        cs.addMessage(serverPlayer,
            new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                             messageId, serverPlayer, unit)
                .addAmount("%amount%", fullAmount)
                .addAmount("%cashInAmount%", cashInAmount));
        messageId = (serverPlayer.isRebel()
                     || serverPlayer.getPlayerType() == PlayerType.INDEPENDENT)
            ? "cashInTreasureTrain.otherIndependent"
            : "cashInTreasureTrain.otherColonial";
        cs.addGlobalMessage(game, serverPlayer,
            new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                             messageId, serverPlayer)
                .addAmount("%amount%", fullAmount)
                .addStringTemplate("%nation%", serverPlayer.getNationLabel()));

        // Dispose of the unit, only visible to the owner.
        cs.add(See.only(serverPlayer), (FreeColGameObject)unit.getLocation());
        ((ServerUnit)unit).csRemove(See.only(serverPlayer),
            null, cs);//-vis: safe in colony

        // Others can see the cash in message.
        game.sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Change a units state.
     *
     * @param serverPlayer The {@code ServerPlayer} that owns the unit.
     * @param unit The {@code Unit} to change the state of.
     * @param state The new {@code UnitState}.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet changeState(ServerPlayer serverPlayer, Unit unit,
                                 UnitState state) {
        ChangeSet cs = new ChangeSet();

        Tile tile = unit.getTile();
        boolean tileDirty = tile != null && tile.getIndianSettlement() != null;
        if (state == UnitState.FORTIFYING && tile != null) {
            ServerColony colony = (tile.getOwningSettlement() instanceof Colony)
                ? (ServerColony) tile.getOwningSettlement()
                : null;
            Player owner = (colony == null) ? null : colony.getOwner();
            if (owner != null
                && owner != unit.getOwner()
                && serverPlayer.getStance(owner) != Stance.ALLIANCE
                && serverPlayer.getStance(owner) != Stance.PEACE) {
                if (colony.isTileInUse(tile)) {
                    colony.csEvictUsers(unit, cs);
                }
                if (serverPlayer.getStance(owner) == Stance.WAR) {
                    tile.changeOwnership(null, null); // Clear owner if at war
                    tileDirty = true;
                }
            }
        }

        unit.setState(state);
        if (tileDirty) {
            cs.add(See.perhaps(), tile);
        } else {
            cs.add(See.perhaps(), (FreeColGameObject)unit.getLocation());
        }

        // Others might be able to see the unit.
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Change improvement work type.
     *
     * @param serverPlayer The {@code ServerPlayer} that owns the unit.
     * @param unit The {@code Unit} to change the work type of.
     * @param type The new {@code TileImprovementType} to produce.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet changeWorkImprovementType(ServerPlayer serverPlayer,
                                               Unit unit,
                                               TileImprovementType type) {
        Tile tile = unit.getTile();
        TileImprovement improvement = tile.getTileImprovement(type);
        if (improvement == null) { // Create the new improvement.
            improvement = new TileImprovement(getGame(), tile, type, null);
            tile.add(improvement);
        }

        unit.setWorkImprovement(improvement);
        unit.setState(UnitState.IMPROVING);

        // Private update of the tile.
        return new ChangeSet().add(See.only(serverPlayer), tile);
    }


    /**
     * Change work type.
     *
     * @param serverPlayer The {@code ServerPlayer} that owns the unit.
     * @param unit The {@code Unit} to change the work type of.
     * @param type The new {@code GoodsType} to produce.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet changeWorkType(ServerPlayer serverPlayer, Unit unit,
                                    GoodsType type) {
        if (unit.getWorkType() != type) {
            unit.setExperience(0);
            unit.changeWorkType(type);
        }

        // Private update of the colony.
        return new ChangeSet().add(See.only(serverPlayer), unit.getColony());
    }


    /**
     * Chat.
     *
     * @param serverPlayer The {@code ServerPlayer} that is chatting.
     * @param message The chat message.
     * @param pri A privacy setting, currently a noop.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet chat(ServerPlayer serverPlayer, String message,
                          boolean pri) {
        getGame().sendToOthers(serverPlayer,
            ChangeSet.simpleChange(See.all().except(serverPlayer),
                new ChatMessage(serverPlayer, message, false)));
        return null;
    }


    /**
     * Choose a founding father.
     *
     * @param serverPlayer The {@code ServerPlayer} that is choosing.
     * @param ff A {@code FoundingFather} to select.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet chooseFoundingFather(ServerPlayer serverPlayer,
                                          FoundingFather ff) {
        final List<FoundingFather> offered = serverPlayer.getOfferedFathers();

        if (!serverPlayer.canRecruitFoundingFather()) {
            return serverPlayer.clientError("Player can not recruit fathers: "
                + serverPlayer.getId());
        } else if (!offered.contains(ff)) {
            return serverPlayer.clientError("Founding father not offered: "
                + ff.getId());
        }
        serverPlayer.updateCurrentFather(ff);
        return null;
    }


    /**
     * Claim land.
     *
     * @param serverPlayer The {@code ServerPlayer} claiming.
     * @param tile The {@code Tile} to claim.
     * @param settlement The {@code Settlement} to claim for.
     * @param price The price to pay for the land, which must agree
     *     with the owner valuation, unless negative which denotes stealing.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet claimLand(ServerPlayer serverPlayer, Tile tile,
                               Settlement settlement, int price) {
        final ServerGame sg = getGame();
        ChangeSet cs = new ChangeSet();
        serverPlayer.csClaimLand(tile, settlement, price, cs);

        if (settlement != null && serverPlayer.isEuropean()) {
            // Define Coronado to make all colony-owned tiles visible
            for (Player sp : transform(sg.getConnectedPlayers(serverPlayer),
                                       coronadoPred)) {
                ((ServerPlayer)sp).exploreTile(tile);
                cs.add(See.only(sp), tile);
                sp.invalidateCanSeeTiles();//+vis(sp)
            }
        }

        // Others can see the tile.
        sg.sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Clear the specialty of a unit.
     *
     * FIXME: why not clear speciality in the open?  You can disband!
     * If we implement this remember to fix the visibility.
     *
     * @param serverPlayer The owner of the unit.
     * @param unit The {@code Unit} to clear the speciality of.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet clearSpeciality(ServerPlayer serverPlayer, Unit unit) {
        UnitTypeChange uc = unit.getUnitChange(UnitChangeType.CLEAR_SKILL);
        if (uc == null) {
            return serverPlayer.clientError("Can not clear unit speciality: "
                + unit.getId());
        }

        // There can be some restrictions that may prevent the
        // clearing of the speciality.  AFAICT the only ATM is that a
        // teacher can not lose its speciality, but this will need to
        // be revisited if we invent a work location that requires a
        // particular unit type.
        if (unit.getStudent() != null) {
            return serverPlayer.clientError("Can not clear speciality of a teacher.");
        }

        // Valid, change type.
        unit.changeType(uc.to);//-vis: safe in colony

        // Update just the unit, others can not see it as this only happens
        // in-colony.
        return new ChangeSet().add(See.only(serverPlayer), unit);
    }


    /**
     * Combat.  Public for the test suite.
     *
     * @param attackerPlayer The {@code ServerPlayer} who is attacking.
     * @param attacker The {@code FreeColGameObject} that is attacking.
     * @param defender The {@code FreeColGameObject} that is defending.
     * @param crs A list of {@code CombatResult}s defining the result.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet combat(ServerPlayer attackerPlayer,
                            FreeColGameObject attacker,
                            FreeColGameObject defender,
                            List<CombatResult> crs) {
        ChangeSet cs = new ChangeSet();
        try {
            attackerPlayer.csCombat(attacker, defender, crs, random, cs);
        } catch (IllegalStateException e) {
            logger.log(Level.WARNING, "Combat FAIL", e);
            return attackerPlayer.clientError(e.getMessage());
        }
        getGame().sendToOthers(attackerPlayer, cs);
        return cs;
    }


    /**
     * Continue playing after winning.
     *
     * @param serverPlayer The {@code ServerPlayer} that plays on.
     * @return Null.
     */
    public ChangeSet continuePlaying(ServerPlayer serverPlayer) {
        final ServerGame game = getGame();
        if (!getFreeColServer().getSinglePlayer()) {
            logger.warning("Can not continue playing in multiplayer!");
        } else if (serverPlayer != game.checkForWinner()) {
            logger.warning("Can not continue playing, as "
                           + serverPlayer.getName()
                           + " has not won the game!");
        } else {
            final Specification spec = game.getSpecification();
            spec.setBoolean(GameOptions.VICTORY_DEFEAT_REF, false);
            spec.setBoolean(GameOptions.VICTORY_DEFEAT_EUROPEANS, false);
            spec.setBoolean(GameOptions.VICTORY_DEFEAT_HUMANS, false);
            logger.info("Disabled victory conditions, as "
                        + serverPlayer.getName()
                        + " has won, but is continuing to play.");
        }
        return null;
    }


    /**
     * Declare independence.
     *
     * @param serverPlayer The {@code ServerPlayer} that is declaring.
     * @param nationName The new name for the independent nation.
     * @param countryName The new name for its residents.
     * @return A {@code ChangeSet} containing the response.
     */
    public ChangeSet declareIndependence(final ServerPlayer serverPlayer,
                                         String nationName, String countryName) {
        final ServerGame game = getGame();
        final Specification spec = game.getSpecification();
        ChangeSet cs = new ChangeSet();

        // Cross the Rubicon
        StringTemplate oldNation = serverPlayer.getNationLabel();
        serverPlayer.setIndependentNationName(nationName);
        serverPlayer.setNewLandName(countryName);
        serverPlayer.changePlayerType(PlayerType.REBEL);

        // Do not add history event to cs as we are going to update the
        // entire player.  Likewise clear model messages.
        Turn turn = game.getTurn();
        HistoryEvent h = new HistoryEvent(turn,
            HistoryEvent.HistoryEventType.DECLARE_INDEPENDENCE, serverPlayer);
        final int independenceTurn = spec.getInteger(GameOptions.INDEPENDENCE_TURN);
        h.setScore(Math.max(0, independenceTurn - turn.getNumber()));
        cs.addGlobalHistory(game, h);
        serverPlayer.clearModelMessages();
        cs.addMessage(serverPlayer,
            new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                             "declareIndependence.resolution", serverPlayer));

        // Dispose of units in or heading to Europe.
        Europe europe = serverPlayer.getEurope();
        StringTemplate seized = StringTemplate.label(", ");
        boolean lost = false;
        for (Unit u : europe.getUnitList()) {
            seized.addStringTemplate(u.getLabel());
            ((ServerUnit)u).csRemove(See.only(serverPlayer), null, cs);
            lost = true;
        }
        for (Unit u : transform(serverPlayer.getHighSeas().getUnits(),
                                matchKey(europe, Unit::getDestination))) {
            seized.addStringTemplate(u.getLabel());
            ((ServerUnit)u).csRemove(See.only(serverPlayer), null, cs);
            lost = true;
        }
        if (lost) {
            cs.addMessage(serverPlayer,
                new ModelMessage(MessageType.UNIT_LOST,
                                 "declareIndependence.unitsSeized",
                                 serverPlayer)
                    .addStringTemplate("%units%", seized));
        }
        serverPlayer.csLoseLocation(europe, cs);
        serverPlayer.reinitialiseMarket();

        // Create the REF.
        ServerPlayer refPlayer = createREFPlayer(serverPlayer);
        cs.addPlayers(Collections.singletonList(refPlayer));
        // Update the intervention force
        final Monarch monarch = serverPlayer.getMonarch();
        monarch.updateInterventionForce();
        String otherKey = Nation.getRandomNonPlayerNationNameKey(game, random);
        cs.addMessage(serverPlayer,
            new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                             "declareIndependence.interventionForce",
                             serverPlayer)
                .add("%nation%", otherKey)
                .addAmount("%number%",
                    spec.getInteger(GameOptions.INTERVENTION_BELLS)));
        serverPlayer.csChangeStance(Stance.WAR, refPlayer, true, cs);

        // Generalized continental army muster.
        // Do not use UnitType.getTargetType.
        java.util.Map<UnitType, List<Unit>> unitMap = new HashMap<>();
        for (Colony colony : transform(serverPlayer.getColonies(),
                                       c -> c.getSoL() > 50)) {
            List<Unit> allUnits = colony.getAllUnitsList();
            int limit = (allUnits.size() + 2) * (colony.getSoL() - 50) / 100;

            unitMap.clear();
            for (Unit unit : transform(allUnits,
                    u -> u.getUnitChange(UnitChangeType.INDEPENDENCE) != null)) {
                appendToMapList(unitMap, unit.getType(), unit);
            }
            for (Entry<UnitType, List<Unit>> entry : unitMap.entrySet()) {
                int n = 0;
                UnitType fromType = entry.getKey();
                UnitType toType = spec.getUnitChange(UnitChangeType.INDEPENDENCE,
                                                     fromType).to;
                List<Unit> units = entry.getValue();
                while (n < limit && !units.isEmpty()) {
                    Unit unit = units.remove(0);
                    unit.changeType(toType);//-vis
                    cs.add(See.only(serverPlayer), unit);
                    n++;
                }
                cs.addMessage(serverPlayer,
                    new ModelMessage(MessageType.UNIT_IMPROVED,
                                     "declareIndependence.continentalArmyMuster",
                                     serverPlayer, colony)
                        .addName("%colony%", colony.getName())
                        .addAmount("%number%", n)
                        .addNamed("%oldUnit%", fromType)
                        .addNamed("%unit%", toType));
                limit -= n;
            }
        }

        // The most hostile contacted non-warring natives declare war
        // on you and peace with the REF, least hostile contacted
        // natives declare peace on you and war on the REF.  If they
        // are the same nation, go to the next most hostile nation
        // that may already be at war.
        final Comparator<Player> comp = Comparator.comparingInt(p ->
            p.getTension(serverPlayer).getValue());
        List<Player> natives = transform(game.getLiveNativePlayers(),
                                         p -> p.hasContacted(serverPlayer),
                                         Function.<Player>identity(), comp);
        if (!natives.isEmpty()) {
            Player good = first(natives);
            logger.info("Native ally following independence: " + good);
            cs.addMessage(serverPlayer,
                new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                                 "declareIndependence.nativeSupport", good)
                    .addStringTemplate("%nation%", good.getNationLabel())
                    .add("%ruler%", serverPlayer.getRulerNameKey()));
            int delta;
            switch (good.getStance(serverPlayer)) {
            case ALLIANCE: case PEACE: default:
                delta = 0;
                break;
            case CEASE_FIRE:
                delta = Tension.Level.HAPPY.getLimit()
                    - good.getTension(serverPlayer).getValue();
                break;
            case WAR:
                delta = Tension.Level.CONTENT.getLimit()
                    - good.getTension(serverPlayer).getValue();
                break;
            }
            ((ServerPlayer)good).csModifyTension(serverPlayer, delta, cs);
            Player.makeContact(good, refPlayer);
            ((ServerPlayer)good).csModifyTension(refPlayer,
                Tension.Level.HATEFUL.getLimit(), cs);

            reverse(natives);
            Player bad = null;
            for (Player p : natives) {
                if (p == good
                    || p.getStance(serverPlayer) == Stance.ALLIANCE) break;
                bad = p;
                if (!p.atWarWith(serverPlayer)) break;
            }
            logger.info("Native enemy following independence: " + bad);
            if (bad != null) {
                switch (bad.getStance(serverPlayer)) {
                case PEACE: case CEASE_FIRE:
                    delta = Tension.Level.HATEFUL.getLimit()
                        - bad.getTension(serverPlayer).getValue();
                    break;
                case WAR: default:
                    delta = 0;
                    break;
                }
                cs.addMessage(serverPlayer,
                    new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                                     "declareIndependence.nativeHostile", bad)
                        .addStringTemplate("%nation%", bad.getNationLabel()));
                if (delta != 0) {
                    ((ServerPlayer)bad).csModifyTension(serverPlayer, delta, cs);
                }
                Player.makeContact(bad, refPlayer);
                ((ServerPlayer)bad).csModifyTension(refPlayer,
                    -bad.getTension(refPlayer).getValue(), cs);
            }
        }

        // Make the mercenary force offer
        List<AbstractUnit> mercs = new ArrayList<>();
        int mercPrice = monarch.loadMercenaryForce(random, mercs);
        if (mercPrice > 0) {
            serverPlayer.csMercenaries(mercPrice, mercs,
                Monarch.MonarchAction.HESSIAN_MERCENARIES, random, cs);
            logger.info("Mercenary force offer on declaration ("
                + Messages.message(AbstractUnit.getListLabel(", ", mercs))
                + ") for " + mercPrice);
        } else {
            logger.info("Mercenary force offer on declaration not affordable.");
        }
            

        // Pity to have to update such a heavy object as the player,
        // but we do this, at most, once per player.  Other players
        // only need a partial player update and the stance change.
        // Put the stance change after the name change so that the
        // other players see the new nation name declaring war.  The
        // REF is hardwired to declare war on rebels so there is no
        // need to adjust its stance or tension.
        cs.addPartial(See.all().except(serverPlayer), serverPlayer,
            "playerType", String.valueOf(serverPlayer.getPlayerType()),
            "independentNationName", serverPlayer.getIndependentNationName(),
            "newLandName", serverPlayer.getNewLandName());
        cs.addGlobalMessage(game, serverPlayer,
            new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                             "declareIndependence.announce",
                             serverPlayer)
                .addStringTemplate("%oldNation%", oldNation)
                .addStringTemplate("%newNation%", serverPlayer.getNationLabel())
                .add("%ruler%", serverPlayer.getRulerNameKey()));
        cs.add(See.only(serverPlayer), serverPlayer);
        serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)

        // Now that everything is ready, we can dispose of Europe.
        cs.addRemove(See.only(serverPlayer), null, europe);//-vis: not on map
        europe.dispose();
        // Do not clean up the Monarch, it contains the intervention force

        game.sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Decline to investigate strange mounds.
     *
     * @param serverPlayer The {@code ServerPlayer} that owns the unit.
     * @param tile The {@code Tile} where the mounds are.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet declineMounds(ServerPlayer serverPlayer, Tile tile) {
        tile.cacheUnseen();//+til
        tile.removeLostCityRumour();//-til

        // Others might see rumour disappear
        ChangeSet cs = new ChangeSet();
        cs.add(See.perhaps(), tile);
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Delete a trade route.
     *
     * @param serverPlayer The {@code ServerPlayer} to delete a trade
     *     route for.
     * @param tradeRoute The {@code TradeRoute} to delete.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet deleteTradeRoute(ServerPlayer serverPlayer,
                                      TradeRoute tradeRoute) {
        List<Unit> dropped = serverPlayer.removeTradeRoute(tradeRoute);

        // Trade route change is entirely internal
        ChangeSet cs = new ChangeSet();
        cs.add(See.only(serverPlayer), serverPlayer); // FIXME: big update
        if (!dropped.isEmpty()) cs.add(See.only(serverPlayer), dropped);
        return cs;
    }


    /**
     * Deliver gift to settlement.
     * Note that this includes both European and native gifts.
     *
     * @param serverPlayer The {@code ServerPlayer} that is delivering.
     * @param unit The {@code Unit} that is delivering.
     * @param settlement The {@code Settlement} to deliver to.
     * @param goods The {@code Goods} to deliver.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet deliverGiftToSettlement(ServerPlayer serverPlayer,
                                             Unit unit, Settlement settlement,
                                             Goods goods) {
        NativeTradeSession session
            = Session.lookup(NativeTradeSession.class, unit, settlement);
        if (session == null) {
            return serverPlayer.clientError("Trying to deliver gift without opening a session");
        }
        NativeTrade nt = session.getNativeTrade();
        if (!nt.getGift()) {
            return serverPlayer.clientError("Trying to deliver gift in a session where gift giving is not allowed: " + unit + " " + settlement + " " + session);
        }

        ChangeSet cs = new ChangeSet();
        Tile tile = settlement.getTile();
        GoodsLocation.moveGoods(unit, goods.getType(), goods.getAmount(), settlement);
        cs.add(See.perhaps(), unit);
        if (settlement instanceof ServerIndianSettlement) {
            ServerIndianSettlement sis = (ServerIndianSettlement)settlement;
            final int alarmBonus = -Math.round(sis.getPriceToBuy(goods)
                                               * 0.001f * getGame().getSpecification()
                                               .getPercentage(GameOptions.ALARM_BONUS_GIFT));

            csVisit(serverPlayer, sis, 0, cs);
            sis.csModifyAlarm(serverPlayer, alarmBonus, true, cs);
            sis.updateWantedGoods();
            tile.updateIndianSettlement(serverPlayer);
            cs.add(See.only(serverPlayer), tile);
        }
        nt.setGift(true);

        // Inform the receiver of the gift.
        ModelMessage m = new ModelMessage(MessageType.GIFT_GOODS,
                                          "deliverGift.goods",
                                          settlement, goods.getType())
            .addStringTemplate("%player%", serverPlayer.getNationLabel())
            .addNamed("%type%", goods)
            .addAmount("%amount%", goods.getAmount())
            .addName("%settlement%", settlement.getName());
        cs.addMessage(serverPlayer, m);
        final Player receiver = settlement.getOwner();
        if (receiver.isConnected() && settlement instanceof Colony) {
            cs.add(See.only(receiver), unit);
            cs.add(See.only(receiver), settlement);
            cs.addMessage(receiver, m);
        }
        logger.info("Gift delivered by unit: " + unit.getId()
                    + " to settlement: " + settlement.getName());

        // Others can see unit capacity, receiver gets it own items.
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Demand a tribute from a native settlement.
     *
     * FIXME: Move TURNS_PER_TRIBUTE magic number to the spec.
     *
     * @param serverPlayer The {@code ServerPlayer} demanding the tribute.
     * @param unit The {@code Unit} that is demanding the tribute.
     * @param is The {@code IndianSettlement} demanded of.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet demandTribute(ServerPlayer serverPlayer, Unit unit,
                                   IndianSettlement is) {
        ChangeSet cs = new ChangeSet();
        final int TURNS_PER_TRIBUTE = 5;

        csVisit(serverPlayer, is, 0, cs);

        Player indianPlayer = is.getOwner();
        int gold = 0;
        int year = getGame().getTurn().getNumber();
        RandomRange gifts = is.getType().getGifts();
        if (is.getLastTribute() + TURNS_PER_TRIBUTE < year
            && gifts != null) {
            switch (indianPlayer.getTension(serverPlayer).getLevel()) {
            case HAPPY: case CONTENT:
                gold = Math.min(gifts.getAmount("Tribute", random, true) / 10,
                                100);
                break;
            case DISPLEASED:
                gold = Math.min(gifts.getAmount("Tribute", random, true) / 20,
                                100);
                break;
            case ANGRY: case HATEFUL:
            default:
                gold = 0; // No tribute for you.
                break;
            }
        }

        // Increase tension whether we paid or not.  Apply tension
        // directly to the settlement and let propagation work.
        ((ServerIndianSettlement)is).csModifyAlarm(serverPlayer,
            Tension.TENSION_ADD_NORMAL, true, cs);
        is.setLastTribute(year);
        ModelMessage m;
        if (gold > 0) {
            indianPlayer.modifyGold(-gold);
            serverPlayer.modifyGold(gold);
            cs.addPartial(See.only(serverPlayer), serverPlayer,
                "gold", String.valueOf(serverPlayer.getGold()),
                "score", String.valueOf(serverPlayer.getScore()));
            m = new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                                 "scoutSettlement.tributeAgree",
                                 unit, is)
                .addAmount("%amount%", gold);
        } else {
            m = new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                                 "scoutSettlement.tributeDisagree",
                                 unit, is);
        }
        cs.addMessage(serverPlayer, m);
        final Tile tile = is.getTile();
        tile.updateIndianSettlement(serverPlayer);
        cs.add(See.only(serverPlayer), tile);
        unit.setMovesLeft(0);
        cs.addPartial(See.only(serverPlayer), unit,
            "movesLeft", String.valueOf(unit.getMovesLeft()));

        // Do not update others, this is all private.
        return cs;
    }


    /**
     * Denounce an existing mission.
     *
     * @param serverPlayer The {@code ServerPlayer} that is denouncing.
     * @param unit The {@code Unit} denouncing.
     * @param is The {@code IndianSettlement} containing the mission
     *     to denounce.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet denounceMission(ServerPlayer serverPlayer, Unit unit,
                                     IndianSettlement is) {
        ChangeSet cs = new ChangeSet();
        csVisit(serverPlayer, is, 0, cs);

        // Determine result
        Unit missionary = is.getMissionary();
        if (missionary == null) {
            return serverPlayer.clientError("Denouncing null missionary");
        }
        final Player enemy = missionary.getOwner();
        double denounce = randomDouble(logger, "Denounce base", random)
            * enemy.getImmigration() / (serverPlayer.getImmigration() + 1);
        if (missionary.hasAbility(Ability.EXPERT_MISSIONARY)) {
            denounce += 0.2;
        }
        if (unit.hasAbility(Ability.EXPERT_MISSIONARY)) {
            denounce -= 0.2;
        }

        if (denounce < 0.5) { // Success, remove old mission and establish ours
            return establishMission(serverPlayer, unit, is);
        }

        // Denounce failed
        final Player owner = is.getOwner();
        cs.add(See.only(serverPlayer), is);
        cs.addMessage(serverPlayer,
            new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                             "indianSettlement.mission.noDenounce",
                             serverPlayer, unit)
                .addStringTemplate("%nation%", owner.getNationLabel()));
        cs.addMessage(enemy,
            new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                             "indianSettlement.mission.enemyDenounce",
                             enemy, is)
                .addStringTemplate("%enemy%", serverPlayer.getNationLabel())
                .addStringTemplate("%settlement%",
                    is.getLocationLabelFor(enemy))
                .addStringTemplate("%nation%", owner.getNationLabel()));
        cs.add(See.perhaps().always(serverPlayer),
               (FreeColGameObject)unit.getLocation());
        ((ServerUnit)unit).csRemove(See.perhaps().always(serverPlayer),
            unit.getLocation(), cs);//-vis(serverPlayer)
        serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)

        // Others can see missionary disappear
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Diplomacy.
     *
     * @param serverPlayer The {@code ServerPlayer} that is trading.
     * @param ourUnit The {@code Unit} that is trading.
     * @param otherColony The {@code Colony} to trade with.
     * @param agreement The {@code DiplomaticTrade} to consider.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet diplomacy(ServerPlayer serverPlayer, Unit ourUnit,
                               Colony otherColony, DiplomaticTrade agreement) {
        ChangeSet cs = new ChangeSet();
        TradeStatus status = agreement.getStatus();
        DiplomacySession session
            = Session.lookup(DiplomacySession.class,
                DiplomacySession.makeDiplomacySessionKey(ourUnit, otherColony));
        if (session == null) {
            if (status != TradeStatus.PROPOSE_TRADE) {
                return serverPlayer.clientError("Missing uc-diplomacy session for "
                    + ourUnit.getId() + "/" + otherColony.getId()
                    + " with " + agreement);
            }
            session = new DiplomacySession(ourUnit, otherColony, getTimeout());
            session.register();
            logger.info("New diplomacy session: " + session);
        } else {
            logger.info("Continuing diplomacy session: " + session
                        + " from " + ourUnit);
        }
        serverPlayer.csDiplomacy(session, agreement, cs);
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }

    /**
     * Diplomacy.
     *
     * @param serverPlayer The {@code ServerPlayer} that is trading.
     * @param ourColony Our {@code Colony}.
     * @param otherUnit The other {@code Unit} that is trading.
     * @param agreement The {@code DiplomaticTrade} to consider.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet diplomacy(ServerPlayer serverPlayer, Colony ourColony,
                               Unit otherUnit, DiplomaticTrade agreement) {
        ChangeSet cs = new ChangeSet();
        DiplomacySession session
            = Session.lookup(DiplomacySession.class,
                DiplomacySession.makeDiplomacySessionKey(otherUnit, ourColony));
        if (session == null) {
            return serverPlayer.clientError("Missing cu-diplomacy session for "
                + otherUnit.getId() + "/" + ourColony.getId()
                + " with " + agreement);
        } else {
            logger.info("Continuing diplomacy session: " + session
                        + " from " + ourColony);
        }
        serverPlayer.csDiplomacy(session, agreement, cs);
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Disband a unit.
     *
     * @param serverPlayer The owner of the unit.
     * @param unit The {@code Unit} to disband.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet disbandUnit(ServerPlayer serverPlayer, Unit unit) {
        ChangeSet cs = new ChangeSet();

        // Dispose of the unit.
        Location loc = unit.getLocation();
        cs.add(See.perhaps().always(serverPlayer), (FreeColGameObject)loc);
        ((ServerUnit)unit).csRemove(See.perhaps().always(serverPlayer),
                                    loc, cs);//-vis(serverPlayer)
        serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)

        // Others can see the unit removal and the space it leaves.
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }

    /**
     * Disconnect the client.
     *
     * @param serverPlayer The {@code ServerPlayer} to disconnect.
     * @return Null, we never reply to a disconnect.
     */
    public ChangeSet disconnect(ServerPlayer serverPlayer) {
        final FreeColServer freeColServer = getFreeColServer();
        if (serverPlayer != null) {
            freeColServer.removePlayerConnection(serverPlayer);
        }
        return null;
    }

    /**
     * Disembark unit from a carrier.
     *
     * @param serverPlayer The {@code ServerPlayer} whose unit is
     *                     embarking.
     * @param serverUnit The {@code ServerUnit} that is disembarking.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet disembarkUnit(ServerPlayer serverPlayer,
                                   ServerUnit serverUnit) {
        if (serverUnit.isNaval()) {
            return serverPlayer.clientError("Naval unit " + serverUnit.getId()
                + " can not disembark.");
        }
        Unit carrier = serverUnit.getCarrier();
        if (carrier == null) {
            return serverPlayer.clientError("Unit " + serverUnit.getId()
                + " is not embarked.");
        }

        ChangeSet cs = new ChangeSet();

        Location newLocation = carrier.getLocation();
        Set<Tile> newTiles = (newLocation.getTile() == null) ? null
            : serverPlayer.collectNewTiles(newLocation.getTile(),
                                           serverUnit.getLineOfSight());
        serverUnit.setLocation(newLocation);//-vis(serverPlayer)
        serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)
        serverUnit.setMovesLeft(0); // In Col1 disembark consumes whole move.
        cs.add(See.perhaps(), (FreeColGameObject)newLocation);
        if (newTiles != null) {
            serverPlayer.csSeeNewTiles(newTiles, cs);
        }

        // Others can (potentially) see the location.
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Embark a unit onto a carrier.
     * Checking that the locations are appropriate is not done here.
     *
     * @param serverPlayer The {@code ServerPlayer} embarking.
     * @param serverUnit The {@code ServerUnit} that is embarking.
     * @param carrier The {@code Unit} to embark onto.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet embarkUnit(ServerPlayer serverPlayer,
                                ServerUnit serverUnit, Unit carrier) {
        if (serverUnit.isNaval()) {
            return serverPlayer.clientError("Naval unit " + serverUnit.getId()
                + " can not embark.");
        }
        UnitLocation.NoAddReason reason = carrier.getNoAddReason(serverUnit);
        if (reason != UnitLocation.NoAddReason.NONE) {
            return serverPlayer.clientError("Carrier: " + carrier.getId()
                + " can not carry " + serverUnit.getId() + ": " + reason);
        }

        ChangeSet cs = new ChangeSet();
        serverUnit.csEmbark(carrier, cs);

        // Others might see the unit disappear, or the carrier capacity.
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * A unit migrates from Europe.
     *
     * @param serverPlayer The {@code ServerPlayer} whose unit it will be.
     * @param slot The slot within {@code Europe} to select the unit from.
     * @param type The type of migration occurring.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet emigrate(ServerPlayer serverPlayer, int slot,
                              MigrationType type) {
        ChangeSet cs = new ChangeSet();
        serverPlayer.csEmigrate(slot, type, random, cs);

        // Do not update others, emigration is private.
        return cs;
    }


    /**
     * Ends the turn of the given player.
     *
     * Note: sends messages to other players.
     *
     * @param serverPlayer The {@code ServerPlayer} to end the turn of.
     * @return A {@code ChangeSet} encapsulating the end of turn changes.
     */
    public ChangeSet endTurn(ServerPlayer serverPlayer) {
        final FreeColServer freeColServer = getFreeColServer();
        final ServerGame serverGame = getGame();
        ServerPlayer winner = serverGame.checkForWinner();
        ServerPlayer current = (ServerPlayer)serverGame.getCurrentPlayer();

        if (serverPlayer != current) {
            throw new RuntimeException("It is not " + serverPlayer.getName()
                + "'s turn, it is " + ((current == null) ? "noone"
                    : current.getName()) + "'s!");
        }

        ChangeSet cs = new ChangeSet();
        for (;;) {
            logger.finest("Ending turn for " + current.getName());
            current.clearModelMessages();

            // Check for new turn
            if (serverGame.isNextPlayerInNewTurn()) {
                serverGame.csNextTurn(cs);

                LogBuilder lb = new LogBuilder(512);
                lb.add("New turn ", serverGame.getTurn(), " for ");
                serverGame.csNewTurn(random, lb, cs);
                lb.shrink(", ");
                lb.log(logger, Level.FINEST);
                if (debugOnlyAITurns > 0) {
                    if (--debugOnlyAITurns <= 0) {
                        // If this was a debug run, complete it.  This will
                        // signal the client to save and quit at the next
                        // suitable opportunity.
                        FreeColDebugger.signalEndDebugRun();
                    }
                }
                serverGame.sendToAll(cs); // Flush changes
                cs.clear();
            }

            if ((current = (ServerPlayer)serverGame.getNextPlayer()) == null) {
                // "can not happen"
                return serverPlayer.clientError("Can not get next player");
            }

            // Remove dead players and retry
            switch (current.checkForDeath()) {
            case IS_DEFEATED:
                for (Player p : current.getRebels()) {
                    csGiveIndependence(current, p, cs);
                }
                // Fall through
            case IS_DEAD:
                current.csWithdraw(cs, null, null);
                logger.info("For " + serverPlayer.getSuffix()
                    + ", " + current.getNation() + " has withdrawn.");
                break;
            case IS_AUTORECRUIT:
                // Need to autorecruit a unit to keep alive.
                current.csEmigrate(0, MigrationType.SURVIVAL, random, cs);
                break;
            case IS_ALIVE: default:
                break;
            }
            if (!cs.isEmpty()) { // Flush changes
                serverGame.sendToAll(cs);
                cs.clear();
            }
            // Do not proceed with a dead players turn
            if (current.isDead()) continue;

            // Are there humans left?
            // FIXME: see if this can be relaxed so we can run large
            // AI-only simulations.
            List<Player> connected = serverGame.getConnectedPlayers();
            boolean onlyAI = all(connected, Player::isAI);
            if (onlyAI) {
                final Comparator<Player> scoreComp
                    = Comparator.comparingInt(Player::getScore).reversed();
                winner = (ServerPlayer)first(sort(connected, scoreComp));
                logger.info("No human player left, winner is: " + winner);
                if (debugOnlyAITurns > 0) { // Complete debug runs
                    FreeColDebugger.signalEndDebugRun();
                }
                serverGame.setCurrentPlayer(null);

                cs.add(See.all(), new GameEndedMessage(winner, false));
                serverGame.sendToAll(cs);
                cs.clear();
            }

            // Has the current player won?
            // Do not end single player games where an AI has won,
            // that would stop revenge mode.
            if (winner == current
                && !(freeColServer.getSinglePlayer() && winner.isAI())) {
                boolean highScore = !winner.isAI()
                    && HighScore.newHighScore(winner);
                cs.add(See.all(), new GameEndedMessage(winner, highScore));
                serverGame.sendToAll(cs);
                cs.clear();
            }

            // Do "new turn"-like actions that need to wait until right
            // before the player is about to move.
            serverGame.setCurrentPlayer(current);
            if (current.isREF() && current.getEntryTile() == null) {
                // Initialize this newly created REF
                // If the teleportREF option is enabled, teleport it in.
                REFAIPlayer refAIPlayer = (REFAIPlayer)freeColServer
                    .getAIPlayer(current);
                boolean teleport = serverGame.getSpecification()
                    .getBoolean(GameOptions.TELEPORT_REF);
                if (refAIPlayer.initialize(teleport)) {
                    csLaunchREF(current, teleport, cs);
                } else {
                    logger.severe("REF failed to initialize.");
                }
            }
            current.csStartTurn(random, cs);

            cs.add(See.all(), new SetCurrentPlayerMessage(current));
            if (current.getPlayerType() == PlayerType.COLONIAL) {
                Monarch monarch = current.getMonarch();
                MonarchAction action = null;
                if (debugMonarchAction != null
                    && current == debugMonarchPlayer) {
                    action = debugMonarchAction;
                    debugMonarchAction = null;
                    debugMonarchPlayer = null;
                    logger.finest("Debug monarch action: " + action);
                } else if (monarch != null) {
                    action = RandomChoice.getWeightedRandom(logger,
                            "Choose monarch action",
                        monarch.getActionChoices(), random);
                }
                if (action != null) {
                    if (monarch.actionIsValid(action)) {
                        logger.finest("Monarch action: " + action);
                        csMonarchAction(current, action, cs);
                    } else {
                        logger.finest("Skipping invalid monarch action: "
                            + action);
                    }
                }
            }

            // Prepare to update, with current player last so that it
            // does not immediately start moving and cause further
            // changes which conflict with these updates.
            List<Player> players = serverGame.getConnectedPlayers(current);
            players.add(current);

            // If this is a debug run, update everyone and continue.
            boolean debugSkip = debugOnlyAITurns > 0
                && !current.isAI()
                && freeColServer.getSinglePlayer();
            if (debugSkip) {
                serverGame.sendToList(players, cs);
                cs.clear();
                continue;
            }
            // Flush accumulated changes, returning to serverPlayer.
            players.remove(serverPlayer);
            serverGame.sendToList(players, cs);
            return cs;
        }
    }


    /**
     * Enters revenge mode against those evil AIs.
     *
     * @param serverPlayer The {@code ServerPlayer} entering revenge mode.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet enterRevengeMode(ServerPlayer serverPlayer) {
        if (!getFreeColServer().getSinglePlayer()) {
            return serverPlayer.clientError("Can not enter revenge mode,"
                + " as this is not a single player game.");
        }
        Game game = getGame();
        List<UnitType> undeads = game.getSpecification()
            .getUnitTypesWithAbility(Ability.UNDEAD);
        List<UnitType> navalUnits = new ArrayList<>();
        List<UnitType> landUnits = new ArrayList<>();
        for (UnitType undead : undeads) {
            if (undead.hasAbility(Ability.NAVAL_UNIT)) {
                navalUnits.add(undead);
            } else if (undead.hasAbility(Ability.MULTIPLE_ATTACKS)) {
                landUnits.add(undead);
            }
        }
        if (navalUnits.isEmpty() || landUnits.isEmpty()) {
            return serverPlayer.clientError("Can not enter revenge mode,"
                + " because we can not find the undead units.");
        }

        ChangeSet cs = new ChangeSet();
        UnitType navalType = getRandomMember(logger, "Choose undead navy",
                                             navalUnits, random);
        Tile start = serverPlayer.getEntryTile()
            .getSafeTile(serverPlayer, random);
        Unit theFlyingDutchman
            = new ServerUnit(game, start, serverPlayer,
                             navalType);//-vis(serverPlayer)
        UnitType landType = getRandomMember(logger, "Choose undead army",
                                            landUnits, random);
        Unit undead = new ServerUnit(game, theFlyingDutchman,
                                     serverPlayer, landType);//-vis
        assert undead != null;
        cs.add(See.only(serverPlayer),
                        serverPlayer.exploreForUnit(theFlyingDutchman));
        serverPlayer.setDead(false);
        serverPlayer.changePlayerType(PlayerType.UNDEAD);
        serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)

        // No one likes the undead.
        for (Player p : transform(game.getLivePlayers(serverPlayer),
                                  p2 -> serverPlayer.hasContacted(p2))) {
            serverPlayer.csChangeStance(Stance.WAR, p, true, cs);
        }

        // Revenge begins
        game.setCurrentPlayer(serverPlayer);
        cs.add(See.all(), new SetCurrentPlayerMessage(serverPlayer));

        // Others can tell something has happened to the player,
        // and possibly see the units.
        cs.add(See.all(), serverPlayer);
        cs.add(See.perhaps(), start);
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Equip a unit for a specific role.
     * Currently the unit is either in Europe or in a settlement.
     * Might one day allow the unit to be on a tile co-located with
     * an equipment-bearing wagon.
     *
     * @param serverPlayer The {@code ServerPlayer} that owns the unit.
     * @param unit The {@code Unit} to equip.
     * @param role The {@code Role} to equip for.
     * @param roleCount The role count.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet equipForRole(ServerPlayer serverPlayer, Unit unit,
                                  Role role, int roleCount) {
        ChangeSet cs = new ChangeSet();
        boolean ret = false;
        if (unit.isInEurope()) {
            ServerEurope serverEurope = (ServerEurope)serverPlayer.getEurope();
            ret = serverEurope.csEquipForRole(unit, role, roleCount,
                                              random, cs);
        } else if (unit.getColony() != null) {
            ServerColony serverColony = (ServerColony)unit.getColony();
            ret = serverColony.csEquipForRole(unit, role, roleCount,
                                              random, cs);
        } else if (unit.getIndianSettlement() != null) {
            ServerIndianSettlement sis
                = (ServerIndianSettlement)unit.getIndianSettlement();
            ret = sis.csEquipForRole(unit, role, roleCount, random, cs);
        } else {
            return serverPlayer.clientError("Unsuitable equip location for: "
                + unit.getId());
        }
        if (!ret) return null;

        if (unit.getInitialMovesLeft() != unit.getMovesLeft()) {
            unit.setMovesLeft(0);
        }
        Unit carrier = unit.getCarrier();
        if (carrier != null
            && carrier.getInitialMovesLeft() != carrier.getMovesLeft()
            && carrier.getMovesLeft() != 0) {
            carrier.setMovesLeft(0);
        }
        return cs;
    }


    /**
     * Establish a new mission.
     *
     * @param serverPlayer The {@code ServerPlayer} that is establishing.
     * @param unit The missionary {@code Unit}.
     * @param is The {@code IndianSettlement} to establish at.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet establishMission(ServerPlayer serverPlayer, Unit unit,
                                      IndianSettlement is) {
        ChangeSet cs = new ChangeSet();
        csVisit(serverPlayer, is, 0, cs);

        // Result depends on tension wrt this settlement.
        // Establish if at least not angry.
        final Tension tension = is.getAlarm(serverPlayer);
        Location loc = unit.getLocation();
        switch (tension.getLevel()) {
        case HATEFUL: case ANGRY:
            cs.add(See.perhaps().always(serverPlayer), (FreeColGameObject)loc);
            ((ServerUnit)unit).csRemove(See.perhaps().always(serverPlayer),
                                        loc, cs);//-vis(serverPlayer)
            serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)
            break;

        case HAPPY: case CONTENT: case DISPLEASED:
            ServerIndianSettlement sis = (ServerIndianSettlement)is;
            if (is.hasMissionary()) sis.csKillMissionary(Boolean.FALSE, cs);

            // Always show the tile the unit was on
            cs.add(See.perhaps().always(serverPlayer), unit.getTile());

            sis.csChangeMissionary(unit, cs);//+vis(serverPlayer)
            break;
        }

        // Add the descriptive message.
        final StringTemplate nation = is.getOwner().getNationLabel();
        cs.addMessage(serverPlayer,
            new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                             "indianSettlement.mission." + tension.getKey(),
                             serverPlayer, unit)
                .addStringTemplate("%nation%", nation));

        // Others can see missionary disappear and settlement acquire
        // mission.
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Handle first contact between European players.
     *
     * @param serverPlayer The {@code ServerPlayer} making contact.
     * @param ourUnit The {@code Unit} making contact (may be null).
     * @param ourColony The {@code Colony} making contact (may be null).
     * @param otherUnit The other {@code Unit} making contact (may be null).
     * @param otherColony The other {@code Colony} making contact (may be null).
     * @param agreement The {@code DiplomaticTrade} to consider.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet europeanFirstContact(ServerPlayer serverPlayer,
                                          Unit ourUnit, Colony ourColony,
                                          Unit otherUnit, Colony otherColony,
                                          DiplomaticTrade agreement) {
        String err = "Missing contact diplomacy session for ";
        DiplomacySession ds;
        boolean compatible = false;
        if (ourColony != null) {
            ds = DiplomacySession.findContactSession(otherUnit, ourColony);
            if (ds == null) return serverPlayer.clientError(err
                + ourColony.getId() + " and " + otherUnit.getId());
            compatible = ds.isCompatible(ourColony, otherUnit);
        } else if (otherUnit != null) {
            ds = DiplomacySession.findContactSession(ourUnit, otherUnit);
            if (ds == null) return serverPlayer.clientError(err
                + ourUnit.getId() + " and " + otherUnit.getId());
            compatible = ds.isCompatible(ourUnit, otherUnit);
        } else {
            ds = DiplomacySession.findContactSession(ourUnit, otherColony);
            if (ds == null) return serverPlayer.clientError(err
                + ourUnit.getId() + " and " + otherColony.getId());
            compatible = ds.isCompatible(ourUnit, otherColony);
        }
        logger.info("Continuing " + ((compatible) ? "" : "in")
            + "compatible contact session: " + ds.getKey());

        ChangeSet cs = new ChangeSet();
        if (compatible) { // Update the other player
            serverPlayer.csDiplomacy(ds, agreement, cs);
            getGame().sendToOthers(serverPlayer, cs);
        }
        return cs;
    }


    /**
     * Get the game state.
     *
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet gameState() {
        final FreeColServer freeColServer = getFreeColServer();
        return ChangeSet.simpleChange((Player)null,
            new GameStateMessage(freeColServer.getServerState()));
    }


    /**
     * Gets the list of high scores.
     *
     * @param serverPlayer The {@code ServerPlayer} querying the scores.
     * @param key A score category key.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet getHighScores(ServerPlayer serverPlayer, String key) {
        return ChangeSet.simpleChange(serverPlayer,
            new HighScoresMessage(key, HighScore.loadHighScores()));
    }


    /**
     * Incite a settlement against an enemy.
     *
     * @param serverPlayer The {@code ServerPlayer} that is inciting.
     * @param unit The missionary {@code Unit} inciting.
     * @param is The {@code IndianSettlement} to incite.
     * @param enemy The {@code Player} to be incited against.
     * @param gold The amount of gold in the bribe.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet incite(ServerPlayer serverPlayer, Unit unit,
                            IndianSettlement is, Player enemy, int gold) {
        ChangeSet cs = new ChangeSet();

        Tile tile = is.getTile();
        csVisit(serverPlayer, is, 0, cs);
        tile.updateIndianSettlement(serverPlayer);
        cs.add(See.only(serverPlayer), tile);

        // How much gold will be needed?
        Player nativePlayer = is.getOwner();
        int payingValue = nativePlayer.getTension(serverPlayer).getValue();
        int targetValue = nativePlayer.getTension(enemy).getValue();
        int goldToPay = (payingValue > targetValue) ? 10000 : 5000;
        goldToPay += 20 * (payingValue - targetValue);
        goldToPay = Math.max(goldToPay, 650);

        // Try to incite?
        if (gold < 0) { // Initial inquiry
            cs.add(See.only(serverPlayer),
                   new InciteMessage(unit, is, enemy, goldToPay));
        } else if (gold < goldToPay || !serverPlayer.checkGold(gold)) {
            cs.addMessage(serverPlayer,
                new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                                 "missionarySettlement.inciteGoldFail",
                                 serverPlayer, is)
                    .addStringTemplate("%player%",
                        enemy.getNationLabel())
                    .addAmount("%amount%", goldToPay));
            unit.setMovesLeft(0);
            cs.addPartial(See.only(serverPlayer), unit,
                "movesLeft", String.valueOf(unit.getMovesLeft()));
        } else {
            // Success.  Raise the tension for the native player with respect
            // to the European player.  Let resulting stance changes happen
            // naturally in the AI player turn/s.
            ((ServerPlayer)nativePlayer).csModifyTension(enemy,
                Tension.WAR_MODIFIER, cs);//+til
            ((ServerPlayer)enemy).csModifyTension(serverPlayer,
                Tension.TENSION_ADD_WAR_INCITER, cs);//+til
            serverPlayer.modifyGold(-gold);
            nativePlayer.modifyGold(gold);
            cs.addMessage(serverPlayer,
                new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                                 "missionarySettlement.inciteSuccess",
                                 nativePlayer)
                .addStringTemplate("%native%", nativePlayer.getNationLabel())
                    .addStringTemplate("%enemy%", enemy.getNationLabel()));
            cs.addPartial(See.only(serverPlayer), serverPlayer,
                "gold", String.valueOf(serverPlayer.getGold()));
            unit.setMovesLeft(0);
            cs.addPartial(See.only(serverPlayer), unit,
                "movesLeft", String.valueOf(unit.getMovesLeft()));
        }

        // Others might include enemy.
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Indians making demands of a colony.
     *
     * @param serverPlayer The {@code ServerPlayer} that sent the message.
     * @param unit The {@code Unit} making the demands.
     * @param colony The {@code Colony} that is demanded of.
     * @param type The {@code GoodsType} being demanded, null
     *     implies gold.
     * @param amount The amount of goods/gold being demanded.
     * @param result The demand result (null initially).
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet indianDemand(final ServerPlayer serverPlayer, Unit unit,
                                  Colony colony, GoodsType type, int amount,
                                  IndianDemandAction result) {
        final Player victim = colony.getOwner();
        NativeDemandSession session = Session.lookup(NativeDemandSession.class,
                                                     unit, colony);
        ChangeSet cs = new ChangeSet();
        if (serverPlayer.isIndian()) {
            if (session != null) {
                return serverPlayer.clientError("Repeated native demand: "
                    + unit.getId() + "," + colony.getId());
            }
            session = new NativeDemandSession(unit, colony, type, amount,
                                              getTimeout());
            session.register();
            logger.info("Native demand(begin) " + session.getKey() + ": "
                + serverPlayer.getName() + " unit " + unit
                + " demands " + amount + " " + ((type == null) ? "gold" : type)
                + " from " + colony.getName());
            cs.add(See.only(victim),
                   new IndianDemandMessage(unit, colony, type, amount));
        } else {
            if (session == null) {
                return serverPlayer.clientError("Replying to missing demand: "
                    + unit.getId() + "," + colony.getId());
            }
            logger.info("Native demand(" + result + ") " + session.getKey()
                + ": " + serverPlayer.getName() + " unit " + unit
                + " demands " + amount + " " + ((type == null) ? "gold" : type)
                + " from " + colony.getName());
            session.complete(result == IndianDemandAction.INDIAN_DEMAND_ACCEPT,
                             cs);
        }
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Join a colony.
     *
     * @param serverPlayer The {@code ServerPlayer} that owns the unit.
     * @param unit The {@code Unit} that is joining.
     * @param colony The {@code Colony} to join.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet joinColony(ServerPlayer serverPlayer, Unit unit,
                                Colony colony) {
        final Specification spec = getGame().getSpecification();
        ChangeSet cs = new ChangeSet();
        Set<Tile> ownedTiles = colony.getOwnedTiles();
        Tile tile = colony.getTile();

        // Join.
        tile.cacheUnseen();//+til
        unit.setLocation(colony);//-vis: safe/colony,-til
        unit.setMovesLeft(0);
        colony.equipForRole(unit, spec.getDefaultRole(), 0);

        // Update with colony tile, and tiles now owned.
        cs.add(See.only(serverPlayer), tile);
        for (Tile t : transform(tile.getSurroundingTiles(1, colony.getRadius()),
                t2 -> (t2.getOwningSettlement() == colony
                    && !ownedTiles.contains(t2)))) {
            cs.add(See.perhaps(), t);
        }

        // Others might see a tile ownership change.
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Learn a skill at an IndianSettlement.
     *
     * @param serverPlayer The {@code ServerPlayer} that is learning.
     * @param unit The {@code Unit} that is learning.
     * @param is The {@code IndianSettlement} to learn from.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet learnFromIndianSettlement(ServerPlayer serverPlayer,
                                               Unit unit, IndianSettlement is) {
        // Sanity checks.
        final Specification spec = getGame().getSpecification();
        final UnitType skill = is.getLearnableSkill();
        if (skill == null) {
            return serverPlayer.clientError("No skill to learn at "
                + is.getName());
        }
        if (unit.getUnitChange(UnitChangeType.NATIVES, skill) == null) {
            return serverPlayer.clientError("Unit " + unit
                + " can not learn skill " + skill + " at " + is.getName());
        }

        // Try to learn
        ChangeSet cs = new ChangeSet();
        unit.setMovesLeft(0);
        csVisit(serverPlayer, is, 0, cs);
        Location loc = unit.getLocation();
        switch (is.getAlarm(serverPlayer).getLevel()) {
        case HATEFUL: // Killed, might be visible to other players.
            cs.add(See.perhaps().always(serverPlayer), (FreeColGameObject)loc);
            ((ServerUnit)unit).csRemove(See.perhaps().always(serverPlayer),
                                        loc, cs);//-vis(serverPlayer)
            serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)
            break;
        case ANGRY: // Learn nothing, not even a pet update
            cs.addPartial(See.only(serverPlayer), unit,
                "movesLeft", String.valueOf(unit.getMovesLeft()));
            break;
        default:
            // Teach the unit, and expend the skill if necessary.
            // Do a full information update as the unit is in the settlement.
            unit.changeType(skill);//-vis(serverPlayer)
            serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)
            cs.add(See.perhaps(), unit);
            if (!is.isCapital()
                && !(is.hasMissionary(serverPlayer)
                    && spec.getBoolean(GameOptions.ENHANCED_MISSIONARIES))) {
                is.setLearnableSkill(null);
            }
            break;
        }
        Tile tile = is.getTile();
        tile.updateIndianSettlement(serverPlayer);
        cs.add(See.only(serverPlayer), tile);

        // Others always see the unit, it may have died or been taught.
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Load goods.
     *
     * @param serverPlayer The {@code ServerPlayer} that is loading.
     * @param loc The {@code Location} where the goods are.
     * @param goodsType The {@code GoodsType} to load.
     * @param amount The amount of goods to load.
     * @param carrier The {@code Unit} to load.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet loadGoods(ServerPlayer serverPlayer, Location loc,
                               GoodsType goodsType, int amount, Unit carrier) {
        if (loc instanceof Europe) {
            if (carrier.isInEurope()) {
                return buyGoods(serverPlayer, goodsType, amount, carrier);
            } else {
                return serverPlayer.clientError("Carrier not in Europe: " + loc);
            }
        }
        // All loading locations other than Europe are GoodsLocations
        if (!(loc instanceof GoodsLocation)) {
            return serverPlayer.clientError("Not a goods location: " + loc);
        }
        GoodsLocation gl = (GoodsLocation)loc;
        if (!carrier.isAtLocation(loc)) {
            return serverPlayer.clientError("Carrier not at location: " + loc);
        }
        if (carrier.getLoadableAmount(goodsType) < amount) {
            return serverPlayer.clientError("Too much goods");
        }
        if (gl.getGoodsCount(goodsType) < amount) {
            return serverPlayer.clientError("Not enough goods ("
                + gl.getGoodsCount(goodsType) + " < " + amount
                + " " + goodsType.getSuffix() + ") at " + gl);
        }

        ChangeSet cs = new ChangeSet();
        GoodsLocation.moveGoods(gl, goodsType, amount, carrier);
        logger.finest(Messages.message(loc.getLocationLabel())
            + " loaded " + amount + " " + goodsType.getSuffix()
            + " onto " + carrier);
        cs.add(See.only(serverPlayer), gl.getGoodsContainer());
        cs.add(See.only(serverPlayer), carrier.getGoodsContainer());
        if (carrier.getInitialMovesLeft() != carrier.getMovesLeft()) {
            carrier.setMovesLeft(0);
            cs.addPartial(See.only(serverPlayer), carrier,
                "movesLeft", String.valueOf(carrier.getMovesLeft()));
        }
        if (gl instanceof Unit) {
            Unit dst = (Unit)gl;
            if (dst.getInitialMovesLeft() != dst.getMovesLeft()) {
                dst.setMovesLeft(0);
                cs.addPartial(See.only(serverPlayer), dst,
                    "movesLeft", String.valueOf(dst.getMovesLeft()));
            }
        }

        // Invisible in settlement
        return cs;
    }


    /**
     * Loot cargo.
     *
     * Note loser is passed by identifier, as by the time we get here
     * the unit may have been sunk.
     *
     * @param serverPlayer The {@code ServerPlayer} that owns the winner.
     * @param winner The {@code Unit} that looting.
     * @param loserId The object identifier of the {@code Unit}
     *     that is looted.
     * @param loot The {@code Goods} to loot.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet lootCargo(ServerPlayer serverPlayer, Unit winner,
                               String loserId, List<Goods> loot) {
        LootSession session = Session.lookup(LootSession.class,
                                             winner.getId(), loserId);
        if (session == null) {
            return serverPlayer.clientError("Bogus looting!");
        }
        if (!winner.hasSpaceLeft()) {
            return serverPlayer.clientError("No space to loot to: "
                + winner.getId());
        }

        ChangeSet cs = new ChangeSet();
        List<Goods> available = session.getCapture();
        if (loot == null) { // Initial inquiry
            cs.add(See.only(serverPlayer),
                   new LootCargoMessage(winner, loserId, available));
        } else {
            for (Goods g : loot) {
                if (!available.contains(g)) {
                    return serverPlayer.clientError("Invalid loot: " + g);
                }
                available.remove(g);
                if (!winner.canAdd(g)) {
                    return serverPlayer.clientError("Loot failed: " + g);
                }
                winner.add(g);
            }

            // Others can see cargo capacity change.
            session.complete(cs);
            cs.add(See.perhaps(), winner);
            getGame().sendToOthers(serverPlayer, cs);
        }
        return cs;
    }


    /**
     * Respond to a monarch action.
     *
     * @param serverPlayer The {@code ServerPlayer} that is to respond.
     * @param action The {@code MonarchAction} to respond to.
     * @param result The player response.
     * @return A {@code ChangeSet} containing the response.
     */
    public ChangeSet monarchAction(ServerPlayer serverPlayer,
                                   MonarchAction action, boolean result) {
        MonarchSession session = Session.lookup(MonarchSession.class,
            serverPlayer.getId(), "");
        if (session == null) {
            return serverPlayer.clientError("Bogus monarch action: " + action);
        } else if (action != session.getAction()) {
            return serverPlayer.clientError("Session action mismatch, "
                + session.getAction() + " expected: " + action);
        }

        ChangeSet cs = new ChangeSet();
        session.complete(result, cs);
        return cs;
    }


    /**
     * Move a unit.
     *
     * @param serverPlayer The {@code ServerPlayer} that is moving.
     * @param unit The {@code ServerUnit} to move.
     * @param newTile The {@code Tile} to move to.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet move(ServerPlayer serverPlayer, ServerUnit unit,
                          Tile newTile) {
        ChangeSet cs = new ChangeSet();
        unit.csMove(newTile, random, cs);
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Move a unit across the high seas.
     *
     * @param serverPlayer The {@code ServerPlayer} that owns the unit.
     * @param unit The {@code Unit} to move.
     * @param destination The {@code Location} to move to.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet moveTo(ServerPlayer serverPlayer, Unit unit,
                            Location destination) {
        ChangeSet cs = new ChangeSet();
        HighSeas highSeas = serverPlayer.getHighSeas();
        Location current = unit.getDestination();
        List<Location> destinations = highSeas.getDestinations();
        boolean others = false; // Notify others?
        boolean invalid = false; // Not a highSeas move?

        if (!unit.getType().canMoveToHighSeas()) {
            invalid = true;
        } else if (destination instanceof Europe) {
            if (!destinations.contains(destination)) {
                return serverPlayer.clientError("HighSeas does not connect to: "
                    + destination.getId()
                    + " in " + highSeas.destinationsToString());
            } else if (unit.getLocation() == highSeas) {
                if (!(current instanceof Europe)) {
                    // Changed direction
                    unit.setWorkLeft(unit.getSailTurns()
                        - unit.getWorkLeft() + 1);
                }
                unit.setDestination(destination);
                cs.add(See.only(serverPlayer), unit, highSeas);
            } else if (unit.hasTile()) {
                Tile tile = unit.getTile();
                unit.setEntryLocation(tile);
                unit.setWorkLeft(unit.getSailTurns());
                unit.setDestination(destination);
                unit.setMovesLeft(0);
                unit.setLocation(highSeas);//-vis(serverPlayer)
                serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)
                cs.addDisappear(serverPlayer, tile, unit);
                cs.add(See.only(serverPlayer), tile, highSeas);
                others = true;
            } else {
                invalid = true;
            }
        } else if (destination instanceof Map) {
            if (!destinations.contains(destination)) {
                return serverPlayer.clientError("HighSeas does not connect to: "
                    + destination.getId()
                    + " in " + highSeas.destinationsToString());
            } else if (unit.getLocation() == highSeas) {
                if (current != destination && (current == null
                        || current.getTile() == null
                        || current.getTile().getMap() != destination)) {
                    // Changed direction
                    unit.setWorkLeft(unit.getSailTurns()
                        - unit.getWorkLeft() + 1);
                }
                unit.setDestination(destination);
                cs.add(See.only(serverPlayer), highSeas);
            } else if (unit.getLocation() instanceof Europe) {
                Europe europe = (Europe) unit.getLocation();
                unit.setWorkLeft(unit.getSailTurns());
                unit.setDestination(destination);
                unit.setMovesLeft(0);
                unit.setLocation(highSeas);//-vis: safe!map
                cs.add(See.only(serverPlayer), europe, highSeas);
            } else {
                invalid = true;
            }
        } else if (destination instanceof Settlement) {
            Tile tile = destination.getTile();
            if (!destinations.contains(tile.getMap())) {
                return serverPlayer.clientError("HighSeas does not connect to: "
                    + destination.getId() + "/" + tile.getMap().getId()
                    + " in " + highSeas.destinationsToString());
            } else if (unit.getLocation() == highSeas) {
                // Direction is somewhat moot, so just reset.
                unit.setWorkLeft(unit.getSailTurns());
                unit.setDestination(destination);
                cs.add(See.only(serverPlayer), highSeas);
            } else if (unit.getLocation() instanceof Europe) {
                Europe europe = (Europe) unit.getLocation();
                unit.setWorkLeft(unit.getSailTurns());
                unit.setDestination(destination);
                unit.setMovesLeft(0);
                unit.setLocation(highSeas);//-vis: safe!map
                cs.add(See.only(serverPlayer), europe, highSeas);
            } else {
                invalid = true;
            }
        } else {
            return serverPlayer.clientError("Bogus moveTo destination: "
                + destination.getId());
        }
        if (invalid) {
            return serverPlayer.clientError("Invalid moveTo: unit=" + unit.getId()
                + " from=" + unit.getLocation().getId()
                + " to=" + destination.getId());
        }

        if (others) getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Get a nation summary.
     *
     * @param serverPlayer The {@code ServerPlayer} to make the summary for.
     * @param player The {@code Player} to summarize.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet nationSummary(ServerPlayer serverPlayer, Player player) {
        return ChangeSet.simpleChange(serverPlayer,
            new NationSummaryMessage(player,
                                     new NationSummary(player, serverPlayer)));
    }


    /**
     * Handle first contact between European and native player.
     *
     * Note that we check for a diplomacy session, but only bother in
     * the case of tile!=null as that is the only possibility for some
     * benefit.
     *
     * @param serverPlayer The {@code ServerPlayer} making contact.
     * @param other The native {@code Player} to contact.
     * @param tile A {@code Tile} on offer at first landing.
     * @param result Whether the initial peace treaty was accepted.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet nativeFirstContact(ServerPlayer serverPlayer,
                                        Player other, Tile tile,
                                        boolean result) {
        ChangeSet cs = new ChangeSet();
        DiplomacySession session = null;
        if (tile != null) {
            Unit u = tile.getFirstUnit();
            Settlement s = tile.getOwningSettlement();
            if (u != null && s != null) {
                session = DiplomacySession.findContactSession(u, s);
            }
        }
        if (result) {
            if (tile != null) {
                if (session == null) {
                    return serverPlayer.clientError("No diplomacy for: "
                        + tile.getId());
                }
                tile.cacheUnseen();//+til
                tile.changeOwnership(serverPlayer, null);//-til
                cs.add(See.perhaps(), tile);
            }
        } else {
            // Consider not accepting the treaty to be an insult and
            // ban missions.
            ((ServerPlayer)other).csModifyTension(serverPlayer,
                Tension.TENSION_ADD_MAJOR, cs);//+til
            ((ServerPlayer)other).addMissionBan(serverPlayer);
        }
        if (session != null) session.complete(result, cs);
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * A native unit delivers its gift to a colony.
     *
     * @param serverPlayer The {@code ServerPlayer} that is delivering.
     * @param unit The {@code Unit} that is delivering.
     * @param colony The {@code Colony} to deliver to.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet nativeGift(ServerPlayer serverPlayer,
                                Unit unit, Colony colony) {
        final Goods goods = first(unit.getGoodsList());
        if (goods == null) {
            return serverPlayer.clientError("No gift to deliver: "
                + unit.getId());
        }
        final Player otherPlayer = colony.getOwner();

        ChangeSet cs = new ChangeSet();
        GoodsLocation.moveGoods(unit, goods.getType(), goods.getAmount(), colony);
        cs.add(See.perhaps(), unit);

        // Inform the receiver of the gift.
        ModelMessage m = new ModelMessage(MessageType.GIFT_GOODS,
                                          "deliverGift.goods",
                                          colony, goods.getType())
            .addStringTemplate("%player%", serverPlayer.getNationLabel())
            .addNamed("%type%", goods)
            .addAmount("%amount%", goods.getAmount())
            .addName("%settlement%", colony.getName());
        cs.addMessage(otherPlayer, m);
        cs.add(See.only(otherPlayer), colony);
        logger.info("Gift delivered by unit: " + unit.getId()
            + " to colony " + colony.getName() + ": " + goods);

        // Others might see unit capacity?
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Handle native trade sessions.
     *
     * @param serverPlayer The {@code ServerPlayer} that is trading.
     * @param action The {@code NativeTradeAction} to perform.
     * @param nt The {@code NativeTrade} underway.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    @SuppressFBWarnings(value="SF_SWITCH_FALLTHROUGH")
    public ChangeSet nativeTrade(ServerPlayer serverPlayer,
                                 NativeTradeAction action, NativeTrade nt) {
        final Unit unit = nt.getUnit();
        final IndianSettlement is = nt.getIndianSettlement();
        final Player otherPlayer = (serverPlayer.owns(unit))
            ? is.getOwner() : unit.getOwner();

        // Server view of the transaction is kept in the NativeTradeSession,
        // which is always updated with what the native player says, and
        // only partially update with what the human player says.
        NativeTradeSession session = Session.lookup(NativeTradeSession.class,
                                                    unit, is);
        if (action.isEuropean() != serverPlayer.isEuropean()) {
            return serverPlayer.clientError(((action.isEuropean())
                    ? "European" : "Native")
                + " player expected for " + action
                + ": " + serverPlayer.getSuffix());
        } else if (action == NativeTradeAction.OPEN && session != null) {
            return serverPlayer.clientError("Session already open for: " + nt);
        } else if (action != NativeTradeAction.OPEN && session == null) {
            return serverPlayer.clientError("No session found for: " + nt);
        }

        ChangeSet cs = new ChangeSet();
        NativeTradeItem item;
        switch (action) {
        case OPEN: // Open a new session if possible
            if (unit.getMovesLeft() <= 0) {
                return serverPlayer.clientError("Unit " + unit.getId()
                    + " has no moves left.");
            }
            // Register a session and ask the natives to price the goods.
            nt = NativeTradeSession.openSession(nt);
            cs.add(See.only(otherPlayer),
                   new NativeTradeMessage(action, nt));
            break;

        case CLOSE: // Just close the session
            session.complete(cs);
            break;

        case BUY: // Check goods (not whole item, price might be wrong), forward
            item = nt.getItem();
            nt.mergeFrom(session.getNativeTrade());
            if (item == null) {
                return serverPlayer.clientError("Null purchase: " + nt);
            } else if (!nt.canBuy()) {
                return serverPlayer.clientError("Can not buy: " + nt);
            } else if (find(nt.getSettlementToUnit(),
                            item.goodsMatcher()) == null) {
                return serverPlayer.clientError("Item missing for "
                    + action + ": " + nt);
            }
            nt.setItem(item);
            cs.add(See.only(otherPlayer),
                   new NativeTradeMessage(action, nt));
            break;

        case SELL: // Check goods, forward
            item = nt.getItem();
            nt.mergeFrom(session.getNativeTrade());
            if (item == null) {
                return serverPlayer.clientError("Null sale: " + nt);
            } else if (item.priceIsSet() && !nt.canSell()) {
                return serverPlayer.clientError("Can not sell: " + nt);
            } else if (find(nt.getUnitToSettlement(),
                            item.goodsMatcher()) == null) {
                return serverPlayer.clientError("Item missing for "
                    + action + ": " + nt);
            }
            nt.setItem(item);
            cs.add(See.only(otherPlayer),
                   new NativeTradeMessage(action, nt));
            break;

        case GIFT: // Check goods, forward
            item = nt.getItem();
            nt.mergeFrom(session.getNativeTrade());
            if (item == null) {
                return serverPlayer.clientError("Null gift: " + nt);
            } else if (!nt.canGift()) {
                return serverPlayer.clientError("Can not gift: " + nt);
            } else if (find(nt.getUnitToSettlement(),
                            item.goodsMatcher()) == null) {
                return serverPlayer.clientError("Item missing for "
                    + action + ": " + nt);
            }
            nt.setItem(item);
            cs.add(See.only(otherPlayer),
                   new NativeTradeMessage(action, nt));
            break;

        case ACK_OPEN: // Natives are prepared to trade, inform player
            session.getNativeTrade().mergeFrom(nt);
            cs.add(See.only(otherPlayer),
                   new NativeTradeMessage(action, nt));
            // Set unit moves to zero to avoid cheating.  If no
            // action is taken, the moves will be restored when
            // closing the session.
            unit.setMovesLeft(0);
            cs.addPartial(See.only(otherPlayer), unit,
                "movesLeft", String.valueOf(unit.getMovesLeft()));
            break;

        case ACK_BUY_HAGGLE: case ACK_SELL_HAGGLE: case NAK_GOODS:
            // Successful haggle or polite refusal of gift
            session.getNativeTrade().mergeFrom(nt);
            cs.add(See.only(otherPlayer),
                   new NativeTradeMessage(action, nt));
            break;

        case ACK_BUY: // Buy succeeded, update goods, inform player
            item = nt.getItem();
            csBuy(unit, item.getGoods(), item.getPrice(),
                  (ServerIndianSettlement)is, cs);
            nt.setBuy(false);
            nt.addToUnit(item);
            session.getNativeTrade().mergeFrom(nt);
            session.getNativeTrade().setBuy(false);
            cs.add(See.only(otherPlayer),
                   new NativeTradeMessage(action, nt));
            break;

        case ACK_SELL: // Sell succeeded, update goods, inform player
            item = nt.getItem();
            csSell(unit, item.getGoods(), item.getPrice(),
                   (ServerIndianSettlement)is, cs);
            nt.setSell(false);
            nt.removeFromUnit(item);
            session.getNativeTrade().mergeFrom(nt);
            session.getNativeTrade().setSell(false);
            cs.add(See.only(otherPlayer),
                   new NativeTradeMessage(action, nt));
            break;

        case ACK_GIFT: // Gift succeeded, update goods, inform player
            item = nt.getItem();
            csGift(unit, item.getGoods(), item.getPrice(),
                   (ServerIndianSettlement)is, cs);
            nt.setGift(false);
            nt.removeFromUnit(item);
            session.getNativeTrade().mergeFrom(nt);
            session.getNativeTrade().setGift(false);
            cs.add(See.only(otherPlayer),
                   new NativeTradeMessage(action, nt));
            break;

        case NAK_HAGGLE: case NAK_NOSALE: // Fail, close
            unit.setMovesLeft(0);
            cs.addPartial(See.only(otherPlayer), unit,
                          "movesLeft", String.valueOf(unit.getMovesLeft()));
            // Fall through
        case NAK_INVALID: case NAK_HOSTILE:
            session.getNativeTrade().mergeFrom(nt);
            cs.add(See.only(otherPlayer),
                   new NativeTradeMessage(action, nt));
            session.complete(cs);
            break;

        default:
            return serverPlayer.clientError("Bogus action: " + action);
        }
        logger.fine("Native trade(" + downCase(action.toString()) + ": " + nt);

        // Update the other player if needed
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Create a new trade route for a player.
     *
     * @param serverPlayer The {@code ServerPlayer} that needs a new route.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet newTradeRoute(ServerPlayer serverPlayer) {
        return ChangeSet.simpleChange(serverPlayer,
            new NewTradeRouteMessage(serverPlayer.newTradeRoute()));
    }


    /**
     * Pay arrears.
     *
     * @param serverPlayer The {@code ServerPlayer} that owns the unit.
     * @param type The {@code GoodsType} to pay the arrears for.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet payArrears(ServerPlayer serverPlayer, GoodsType type) {
        int arrears = serverPlayer.getArrears(type);
        if (arrears <= 0) {
            return serverPlayer.clientError("No arrears for pay for: "
                + type.getId());
        } else if (!serverPlayer.checkGold(arrears)) {
            return serverPlayer.clientError("Not enough gold to pay arrears for: "
                + type.getId());
        }

        ChangeSet cs = new ChangeSet();
        Market market = serverPlayer.getMarket();
        serverPlayer.modifyGold(-arrears);
        market.setArrears(type, 0);
        cs.addPartial(See.only(serverPlayer), serverPlayer,
            "gold", String.valueOf(serverPlayer.getGold()));
        cs.add(See.only(serverPlayer), market.getMarketData(type));
        // Arrears payment is private.
        return cs;
    }


    /**
     * Pay for a building.
     *
     * @param serverPlayer The {@code ServerPlayer} that owns the
     *     colony.
     * @param colony The {@code Colony} that is building.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet payForBuilding(ServerPlayer serverPlayer, Colony colony) {
        if (!getGame().getSpecification()
            .getBoolean(GameOptions.PAY_FOR_BUILDING)) {
            return serverPlayer.clientError("Pay for building is disabled");
        }

        BuildableType build = colony.getCurrentlyBuilding();
        if (build == null) {
            return serverPlayer.clientError("Colony " + colony.getId()
                + " is not building anything!");
        }
        List<AbstractGoods> required = colony.getRequiredGoods(build);
        int price = colony.priceGoodsForBuilding(required);
        if (!serverPlayer.checkGold(price)) {
            return serverPlayer.clientError("Insufficient funds to pay for build.");
        }

        // Save the correct final gold for the player, as we are going to
        // use buy() below, but it deducts the normal uninflated price for
        // the goods being bought.  We restore this correct amount later.
        int savedGold = serverPlayer.modifyGold(-price);
        serverPlayer.modifyGold(price);

        ChangeSet cs = new ChangeSet();
        GoodsContainer container = colony.getGoodsContainer();
        container.saveState();
        for (AbstractGoods ag : required) {
            GoodsType type = ag.getType();
            int amount = ag.getAmount();
            if (type.isStorable()) {
                // FIXME: should also check canTrade(type, Access.?)
                if ((amount = serverPlayer.buyInEurope(random, container,
                                                       type, amount)) < 0) {
                    return serverPlayer.clientError("Can not buy " + amount
                        + " " + type + " for " + build);
                }
                serverPlayer.csFlushMarket(type, cs);
            } else {
                container.addGoods(type, amount);
            }
        }
        colony.invalidateCache();

        // Nothing to see for others, colony internal.
        serverPlayer.setGold(savedGold);
        cs.addPartial(See.only(serverPlayer), serverPlayer,
            "gold", String.valueOf(serverPlayer.getGold()));
        cs.add(See.only(serverPlayer), container);
        return cs;
    }


    /**
     * Put outside colony.
     *
     * @param serverPlayer The {@code ServerPlayer} that owns the unit.
     * @param unit The {@code Unit} to be put out.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet putOutsideColony(ServerPlayer serverPlayer, Unit unit) {
        Tile tile = unit.getTile();
        Colony colony = unit.getColony();
        if (unit.isInColony()) tile.cacheUnseen();//+til
        unit.setLocation(tile);//-vis: safe/colony,-til if in colony

        // Full tile update for the player, the rest get their limited
        // view of the colony so that population changes.
        ChangeSet cs = new ChangeSet();
        cs.add(See.only(serverPlayer), tile);
        cs.add(See.perhaps().except(serverPlayer), colony);
        return cs;
    }


    /**
     * Rearrange a colony.
     *
     * @param serverPlayer The {@code ServerPlayer} that is querying.
     * @param colony The {@code Colony} to rearrange.
     * @param arrangements A list of {@code Arrangement}s to apply.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    @SuppressFBWarnings(value="SF_SWITCH_FALLTHROUGH")
    public ChangeSet rearrangeColony(ServerPlayer serverPlayer, Colony colony,
                                     List<Arrangement> arrangements) {
        final Role defaultRole = getGame().getSpecification().getDefaultRole();
        Tile tile = colony.getTile();
        tile.cacheUnseen();//+til

        // Move everyone out of the way and stockpile their equipment.
        for (Arrangement a : arrangements) {
            a.unit.setLocation(tile);//-til
            if (!a.unit.hasDefaultRole()) {
                colony.equipForRole(a.unit, defaultRole, 0);
            }
        }

        List<Arrangement> todo = new ArrayList<>(arrangements);
        while (!todo.isEmpty()) {
            Arrangement a = todo.remove(0);
            if (a.loc == tile) continue;
            WorkLocation wl = (WorkLocation)a.loc;
            // Adding to wl can fail, and in the worst case there
            // might be a circular dependency.  If the move can
            // succeed, do it, but if not, retry.
            switch (wl.getNoAddReason(a.unit)) {
            case NONE:
                a.unit.setLocation(wl);
                // Fall through
            case ALREADY_PRESENT:
                if (a.unit.getWorkType() != a.work) {
                    a.unit.changeWorkType(a.work);
                }
                break;
            case CAPACITY_EXCEEDED:
                todo.add(todo.size(), a);
                break;
            default:
                logger.warning("Bad move for " + a.unit + " to " + wl);
                break;
            }
        }

        // Collect roles that cause a change, ordered by simplest change
        for (Arrangement a : transform(arrangements,
                a -> a.role != defaultRole && a.role != a.unit.getRole(),
                Function.<Arrangement>identity(),
                Arrangement::roleComparison)) {
            if (!colony.equipForRole(a.unit, a.role, a.roleCount)) {
                return serverPlayer.clientError("Failed to equip "
                    + a.unit.getId() + " for role " + a.role
                    + " at " + colony);
            }
        }

        // Just update the whole tile, including for other players
        // which might see colony population change.
        return new ChangeSet().add(See.perhaps(), tile);
    }


    /**
     * Rename an object.
     *
     * @param serverPlayer The {@code ServerPlayer} that is naming.
     * @param object The {@code Nameable} to rename.
     * @param newName The new name.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet renameObject(ServerPlayer serverPlayer, Nameable object,
                                  String newName) {
        ChangeSet cs = new ChangeSet();

        if (object instanceof Settlement) {
            ((Settlement)object).getTile().cacheUnseen();//+til
        }
        object.setName(newName);//-til?
        FreeColGameObject fcgo = (FreeColGameObject)object;
        cs.addPartial(See.all(), fcgo, "name", newName);

        // Others may be able to see the name change.
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Handle a player retiring.
     *
     * @param serverPlayer The {@code ServerPlayer} that is retiring.
     * @return A {@code ChangeSet} containing the response.
     */
    public ChangeSet retire(ServerPlayer serverPlayer) {
        boolean highScore = HighScore.newHighScore(serverPlayer);
        ChangeSet cs = new ChangeSet();
        serverPlayer.csWithdraw(cs, null, null); // Clean up the player.
        getGame().sendToOthers(serverPlayer, cs);
        cs.addAttribute(See.only(serverPlayer),
                        "highScore", Boolean.toString(highScore));
        return cs;
    }


    /**
     * Scout a native settlement, that is, the contacting action
     * that generates the greeting dialog.
     *
     * @param serverPlayer The {@code ServerPlayer} that is scouting.
     * @param unit The scout {@code Unit}.
     * @param is The {@code IndianSettlement} to scout.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet scoutIndianSettlement(ServerPlayer serverPlayer,
                                           Unit unit, IndianSettlement is) {
        final Player owner = is.getOwner();
        ChangeSet cs = new ChangeSet();
        Tile tile = is.getTile();

        csVisit(serverPlayer, is, -1, cs);
        tile.updateIndianSettlement(serverPlayer);
        cs.add(See.only(serverPlayer), tile);
        cs.add(See.only(serverPlayer), new NationSummaryMessage(owner,
                new NationSummary(owner, serverPlayer)));

        // This is private.
        return cs;
    }


    /**
     * Speak to the chief at a native settlement.
     *
     * @param serverPlayer The {@code ServerPlayer} that is scouting.
     * @param unit The scout {@code Unit}.
     * @param is The {@code IndianSettlement} to scout.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet scoutSpeakToChief(ServerPlayer serverPlayer,
                                       Unit unit, IndianSettlement is) {
        ChangeSet cs = new ChangeSet();
        Tile tile = is.getTile();
        boolean tileDirty = is.setVisited(serverPlayer);
        String result;

        // Hateful natives kill the scout right away.
        Tension tension = is.getAlarm(serverPlayer);
        if (tension.getLevel() == Tension.Level.HATEFUL) {
            Location loc = unit.getLocation();
            cs.add(See.perhaps().always(serverPlayer), (FreeColGameObject)loc);
            ((ServerUnit)unit).csRemove(See.perhaps().always(serverPlayer),
                                        loc, cs);//-vis(serverPlayer)
            serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)
            result = "die";
        } else {
            // Otherwise player gets to visit, and learn about the settlement.
            List<UnitType> scoutTypes = getGame().getSpecification()
                .getUnitTypesWithAbility(Ability.EXPERT_SCOUT);
            UnitType scoutSkill = first(scoutTypes);
            int radius = unit.getLineOfSight();
            UnitType skill = is.getLearnableSkill();
            int rnd = randomInt(logger, "scouting", random, 10);
            if (is.hasAnyScouted()) {
                // Do nothing if already spoken to.
                result = "nothing";
            } else if (scoutSkill != null && unit.getType() != scoutSkill
                && ((skill != null && skill.hasAbility(Ability.EXPERT_SCOUT))
                    || rnd == 0)) {
                // If the scout can be taught to be an expert it will be.
                unit.changeType(scoutSkill);//-vis(serverPlayer)
                serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)
                result = "expert";
            } else {
                // Choose tales 1/3 of the time, or if there are no beads.
                RandomRange gifts = is.getType().getGifts();
                int gold = (gifts == null) ? 0
                    : gifts.getAmount("Base beads amount", random, true);
                if (gold <= 0 || rnd <= 3) {
                    radius = Math.max(radius, IndianSettlement.TALES_RADIUS);
                    result = "tales";
                } else {
                    if (unit.hasAbility(Ability.EXPERT_SCOUT)) {
                        gold = (gold * 11) / 10; // FIXME: magic number
                    }
                    serverPlayer.modifyGold(gold);
                    is.getOwner().modifyGold(-gold);
                    result = Integer.toString(gold);
                    cs.addPartial(See.only(serverPlayer), serverPlayer,
                        "gold", String.valueOf(serverPlayer.getGold()),
                        "score", String.valueOf(serverPlayer.getScore()));
                }
            }

            // Have now spoken to the chief.
            csVisit(serverPlayer, is, 1, cs);
            tileDirty = true;

            // Update settlement tile with new information, and any
            // newly visible tiles, possibly with enhanced radius.
            Set<Tile> tiles = transform(tile.getSurroundingTiles(1, radius),
                t -> !serverPlayer.canSee(t) && (t.isLand() || t.isShore()),
                Function.<Tile>identity(), Collectors.toSet());
            cs.add(See.only(serverPlayer), serverPlayer.exploreTiles(tiles));

            // If the unit was promoted, update it completely, otherwise just
            // update moves and possibly gold+score.
            unit.setMovesLeft(0);
            if ("expert".equals(result)) {
                cs.add(See.perhaps(), unit);
            } else {
                cs.addPartial(See.only(serverPlayer), unit,
                    "movesLeft", String.valueOf(unit.getMovesLeft()));
            }
        }
        if (tileDirty) {
            tile.updateIndianSettlement(serverPlayer);
            cs.add(See.only(serverPlayer), tile);
        }

        // Always add result.
        cs.add(See.only(serverPlayer),
               new ScoutSpeakToChiefMessage(unit, is, result));

        // Other players may be able to see unit disappearing, or
        // learning.
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Sell goods in Europe.
     *
     * @param serverPlayer The {@code ServerPlayer} that is selling.
     * @param type The {@code GoodsType} to sell.
     * @param amount The amount of goods to sell.
     * @param carrier The {@code Unit} carrying the goods.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    private ChangeSet sellGoods(ServerPlayer serverPlayer, GoodsType type,
                                int amount, Unit carrier) {
        ChangeSet cs = new ChangeSet();
        GoodsContainer container = carrier.getGoodsContainer();
        container.saveState();
        if (serverPlayer.canTrade(type, Access.EUROPE)) {
            int gold = serverPlayer.getGold();
            int sellAmount = serverPlayer.sellInEurope(random, container,
                                                       type, amount);
            if (sellAmount < 0) {
                return serverPlayer.clientError("Player " + serverPlayer.getName()
                    + " tried to sell " + amount + " " + type.getSuffix());
            }
            serverPlayer.csFlushMarket(type, cs);
            cs.addPartial(See.only(serverPlayer), serverPlayer,
                "gold", String.valueOf(serverPlayer.getGold()));
            logger.finest(carrier + " sold " + amount + "(" + sellAmount + ")"
                + " " + type.getSuffix()
                + " in Europe for " + (serverPlayer.getGold() - gold));
        } else {
            // Dumping goods in Europe
            GoodsLocation.moveGoods(carrier, type, amount, null);
            logger.finest(carrier + " dumped " + amount
                + " " + type.getSuffix() + " in Europe");
        }
        carrier.setMovesLeft(0);
        cs.add(See.only(serverPlayer), carrier);
        // Action occurs in Europe, nothing is visible to other players.
        return cs;
    }


    /**
     * Set build queue.
     *
     * @param serverPlayer The {@code ServerPlayer} that owns the colony.
     * @param colony The {@code Colony} to set the queue of.
     * @param queue The new build queue.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet setBuildQueue(ServerPlayer serverPlayer, Colony colony,
                                   List<BuildableType> queue) {
        BuildableType current = colony.getCurrentlyBuilding();
        colony.setBuildQueue(queue);
        if (getGame().getSpecification()
            .getBoolean(GameOptions.CLEAR_HAMMERS_ON_CONSTRUCTION_SWITCH)
            && current != colony.getCurrentlyBuilding()) {
            for (AbstractGoods ag : transform(current.getRequiredGoods(),
                    g -> !g.getType().isStorable())) {
                colony.removeGoods(ag.getType());
            }
        }
        colony.invalidateCache();

        // Only visible to player.
        ChangeSet cs = new ChangeSet();
        cs.add(See.only(serverPlayer), colony);
        return cs;
    }


    /**
     * Set a unit stop.
     *
     * @param serverPlayer The {@code ServerPlayer} that owns the unit.
     * @param unit The {@code Unit} to set the destination for.
     * @param index The stop index.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet setCurrentStop(ServerPlayer serverPlayer, Unit unit,
                                    int index) {
        TradeRoute tr = unit.getTradeRoute();
        if (tr == null) {
            return serverPlayer.clientError("Unit has no trade route to set stop for.");
        } else if (index < 0 || index >= tr.getStopCount()) {
            return serverPlayer.clientError("Stop index out of range [0.."
                + tr.getStopCount() + "]: " + index);
        }

        unit.setCurrentStop(index);

        // Others can not see a stop change.
        return new ChangeSet().add(See.only(serverPlayer), unit);
    }


    /**
     * Set a unit destination.
     *
     * @param serverPlayer The {@code ServerPlayer} that owns the unit.
     * @param unit The {@code Unit} to set the destination for.
     * @param destination The {@code Location} to set as destination.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet setDestination(ServerPlayer serverPlayer, Unit unit,
                                    Location destination) {
        if (unit.getTradeRoute() != null) {
            // Override destination to bring the unit to port.
            if (destination == null && unit.isAtSea()) {
                destination = unit.getStop().getLocation();
            }
            unit.setTradeRoute(null);
        }
        unit.setDestination(destination);

        // Others can not see a destination change.
        return new ChangeSet().add(See.only(serverPlayer), unit);
    }


    /**
     * Set goods levels.
     *
     * @param serverPlayer The {@code ServerPlayer} that owns the colony.
     * @param colony The {@code Colony} to set the goods levels in.
     * @param exportData The new {@code ExportData}.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet setGoodsLevels(ServerPlayer serverPlayer, Colony colony,
                                    ExportData exportData) {
        colony.setExportData(exportData);
        return new ChangeSet().add(See.only(serverPlayer), colony);
    }


    /**
     * Set land name.
     *
     * @param serverPlayer The {@code ServerPlayer} who landed.
     * @param unit The {@code Unit} that has come ashore.
     * @param name The new land name.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet setNewLandName(ServerPlayer serverPlayer, Unit unit,
                                    String name) {
        ChangeSet cs = new ChangeSet();

        // Special case of a welcome from an adjacent native unit,
        // offering the land the landing unit is on if a peace treaty
        // is accepted.
        serverPlayer.setNewLandName(name);

        // Update the name and note the history.
        cs.addPartial(See.only(serverPlayer), serverPlayer,
            "newLandName", name);
        Turn turn = serverPlayer.getGame().getTurn();
        HistoryEvent h = new HistoryEvent(turn,
            HistoryEvent.HistoryEventType.DISCOVER_NEW_WORLD, serverPlayer)
                .addName("%name%", name);
        cs.addHistory(serverPlayer, h);
        return cs;
    }


    /**
     * Set region name.
     *
     * @param serverPlayer The {@code ServerPlayer} discovering.
     * @param unit The {@code Unit} that is discovering.
     * @param region The {@code Region} to discover.
     * @param name The new region name.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet setNewRegionName(ServerPlayer serverPlayer, Unit unit,
                                      Region region, String name) {
        final Game game = getGame();
        ServerRegion serverRegion = (ServerRegion)region;
        // Discoverer is set when unit moves in.
        if (!Utils.equals(region.getDiscoverer(), unit.getId())) {
            return serverPlayer.clientError("Discoverer mismatch, "
                + region.getDiscoverer() + " expected, "
                + unit.getId() + " provided.");
        }
        ChangeSet cs = new ChangeSet();
        serverRegion.csDiscover(serverPlayer, unit, game.getTurn(), name, cs);

        // Others do find out about region name changes.
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Spy on a settlement.
     *
     * @param serverPlayer The {@code ServerPlayer} that is spying.
     * @param unit The {@code Unit} that is spying.
     * @param settlement The {@code Settlement} to spy on.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet spySettlement(ServerPlayer serverPlayer, Unit unit,
                                   Settlement settlement) {
        ChangeSet cs = new ChangeSet();

        cs.addSpy(unit, settlement);
        unit.setMovesLeft(0);
        cs.addPartial(See.only(serverPlayer), unit,
            "movesLeft", String.valueOf(unit.getMovesLeft()));
        return cs;
    }


    /**
     * Train a unit in Europe.
     *
     * @param serverPlayer The {@code ServerPlayer} that is demanding.
     * @param type The {@code UnitType} to train.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet trainUnitInEurope(ServerPlayer serverPlayer,
                                       UnitType type) {

        Europe europe = serverPlayer.getEurope();
        if (europe == null) {
            return serverPlayer.clientError("No Europe to train in.");
        }
        int price = europe.getUnitPrice(type);
        if (price <= 0) {
            return serverPlayer.clientError("Bogus price: " + price);
        } else if (!serverPlayer.checkGold(price)) {
            return serverPlayer.clientError("Not enough gold ("
                + serverPlayer.getGold() + " < " + price
                + ") to train " + type);
        }

        final Game game = getGame();
        final Specification spec = game.getSpecification();
        Role role = (spec.getBoolean(GameOptions.EQUIP_EUROPEAN_RECRUITS))
            ? type.getDefaultRole()
            : spec.getDefaultRole();
        Unit unit = new ServerUnit(game, europe, serverPlayer, type,
                                   role);//-vis: safe, Europe
        unit.setName(serverPlayer.getNameForUnit(type, random));
        serverPlayer.modifyGold(-price);
        ((ServerEurope)europe).increasePrice(type, price);

        // Only visible in Europe
        ChangeSet cs = new ChangeSet();
        cs.addPartial(See.only(serverPlayer), serverPlayer,
            "gold", String.valueOf(serverPlayer.getGold()));
        cs.add(See.only(serverPlayer), europe);
        return cs;
    }


    /**
     * Unload goods.
     *
     * @param serverPlayer The {@code ServerPlayer} that is unloading.
     * @param goodsType The {@code GoodsType} to unload.
     * @param amount The amount of goods to unload.
     * @param carrier The {@code Unit} to unload.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet unloadGoods(ServerPlayer serverPlayer, GoodsType goodsType,
                                 int amount, Unit carrier) {
        if (carrier.getGoodsCount(goodsType) < amount) {
            return serverPlayer.clientError("Too few goods");
        }
        if (carrier.isInEurope()) {
            return sellGoods(serverPlayer, goodsType, amount, carrier);
        }

        ChangeSet cs = new ChangeSet();
        Settlement settlement = carrier.getSettlement();
        if (settlement != null) {
            GoodsLocation.moveGoods(carrier, goodsType, amount, settlement);
            logger.finest(carrier
                + " unloaded " + amount + " " + goodsType.getSuffix()
                + " to " + settlement.getName());
            cs.add(See.only(serverPlayer), settlement.getGoodsContainer());
            cs.add(See.only(serverPlayer), carrier.getGoodsContainer());
            if (carrier.getInitialMovesLeft() != carrier.getMovesLeft()) {
                carrier.setMovesLeft(0);
                cs.addPartial(See.only(serverPlayer), carrier,
                    "movesLeft", String.valueOf(carrier.getMovesLeft()));
            }
        } else { // Dump of goods onto a tile
            GoodsLocation.moveGoods(carrier, goodsType, amount, null);
            logger.finest(carrier + " dumped " + amount
                + " " + goodsType.getSuffix() + " to " + carrier.getLocation());
            cs.add(See.perhaps(), (FreeColGameObject)carrier.getLocation());
            // Others might see a capacity change.
            getGame().sendToOthers(serverPlayer, cs);
        }
        return cs;
    }


    /**
     * Update a trade route for a player.
     *
     * @param serverPlayer The {@code ServerPlayer} to set trade
     *     routes for.
     * @param tradeRoute An uninterned {@code TradeRoute} to update.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet updateTradeRoute(ServerPlayer serverPlayer,
                                      TradeRoute tradeRoute) {
        final Game game = getGame();
        String name;
        StringTemplate fail;
        TradeRoute tr;
        if (tradeRoute == null || tradeRoute.getId() == null
            || (name = tradeRoute.getName()) == null) {
            return serverPlayer.clientError("Bogus route");
        } else if ((fail = tradeRoute.verify()) != null) {
            return serverPlayer.clientError(Messages.message(fail));
        } else if ((tr = game.getFreeColGameObject(tradeRoute.getId(),
                    TradeRoute.class)) == null) {
            return serverPlayer.clientError("Not an existing trade route: "
                + tradeRoute.getId());
        }
        tr.copyIn(tradeRoute);
        return new ChangeSet().add(See.only(serverPlayer), tr);
    }


    /**
     * Change work location.
     *
     * @param serverPlayer The {@code ServerPlayer} that owns the unit.
     * @param unit The {@code Unit} to change the work location of.
     * @param workLocation The {@code WorkLocation} to change to.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet work(ServerPlayer serverPlayer, Unit unit,
                          WorkLocation workLocation) {
        final Specification spec = getGame().getSpecification();
        final Colony colony = workLocation.getColony();
        colony.getGoodsContainer().saveState();

        ChangeSet cs = new ChangeSet();
        Tile tile = workLocation.getWorkTile();
        if (tile != null && tile.getOwningSettlement() != colony) {
            // Claim known free land (because canAdd() succeeded).
            serverPlayer.csClaimLand(tile, colony, 0, cs);
        }

        colony.equipForRole(unit, spec.getDefaultRole(), 0);

        // Check for upgrade.
        UnitTypeChange uc = unit.getUnitChange(UnitChangeType.ENTER_COLONY);
        if (uc != null && uc.appliesTo(unit)) {
            unit.changeType(uc.to);//-vis: safe in colony
        }

        // Change the location.
        // We could avoid updating the whole tile if we knew that this
        // was definitely a move between locations and no student/teacher
        // interaction occurred.
        if (!unit.isInColony()) unit.getColony().getTile().cacheUnseen();//+til
        unit.setLocation(workLocation);//-vis: safe/colony,-til if not in colony
        cs.add(See.perhaps(), colony.getTile());
        // Others can see colony change size
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }
}
