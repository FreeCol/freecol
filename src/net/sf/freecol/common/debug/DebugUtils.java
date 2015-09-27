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

package net.sf.freecol.common.debug;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.ChangeEvent;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ChoiceItem;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColXMLWriter.WriteScope;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Disaster;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.LostCityRumour.RumourType;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.util.LogBuilder;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.RandomChoice;
import static net.sf.freecol.common.util.StringUtils.*;

import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIColony;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.ai.mission.Mission;
import net.sf.freecol.server.ai.mission.TransportMission;
import net.sf.freecol.server.model.ServerBuilding;
import net.sf.freecol.server.model.ServerColony;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;


/**
 * Utilities for in game debug support.
 *
 * Several client GUI features have optional debug routines.  These
 * routines are typically quite special in that they need to intrusive
 * things with the server, and do not have much in common with the
 * client code while still sometimes needing user interaction.  They
 * also have considerable similarity amongst themselves.  So they have
 * been collected here.
 *
 * The client GUI features that are the source of these routines are:
 *   - The colony panel
 *   - The debug menu
 *   - The tile popup menu
 */
public class DebugUtils {

    private static final Logger logger = Logger.getLogger(DebugUtils.class.getName());


    /**
     * Debug action to add buildings to the user colonies.
     *
     * Called from the debug menu.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param buildingTitle A label for the choice dialog.
     */
    public static void addBuildings(final FreeColClient freeColClient,
                                    String buildingTitle) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game game = freeColClient.getGame();
        final GUI gui = freeColClient.getGUI();
        final Player player = freeColClient.getMyPlayer();

        BuildingType buildingType = gui.getChoice(null, buildingTitle,
            "cancel",
            game.getSpecification().getBuildingTypeList().stream()
                .map(bt -> {
                        String msg = Messages.getName(bt);
                        return new ChoiceItem<BuildingType>(msg, bt);
                    })
                .sorted().collect(Collectors.toList()));
        if (buildingType == null) return;

        final Game sGame = server.getGame();
        final BuildingType sBuildingType = server.getSpecification()
            .getBuildingType(buildingType.getId());
        final Player sPlayer = sGame.getFreeColGameObject(player.getId(),
                                                          Player.class);
        List<String> results = new ArrayList<>();
        int fails = 0;
        for (Colony sColony : sPlayer.getColonies()) {
            Colony.NoBuildReason reason
                = sColony.getNoBuildReason(sBuildingType, null);
            results.add(sColony.getName() + ": " + reason);
            if (reason == Colony.NoBuildReason.NONE) {
                if (sBuildingType.isDefenceType()) {
                    sColony.getTile().cacheUnseen();//+til
                }
                Building sBuilding = new ServerBuilding(sGame, sColony,
                                                        sBuildingType);
                sColony.addBuilding(sBuilding);//-til
            } else {
                fails++;
            }
        }
        gui.showInformationMessage(join(", ", results));
        if (fails < sPlayer.getNumberOfSettlements()) {
            // Brutally resynchronize
            freeColClient.getConnectController().reconnect();
        }
    }

    /**
     * Debug action to add a founding father.
     *
     * Called from the debug menu.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param fatherTitle A label for the choice dialog.
     */
    public static void addFathers(final FreeColClient freeColClient,
                                  String fatherTitle) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final GUI gui = freeColClient.getGUI();
        final Player player = freeColClient.getMyPlayer();
        final Game sGame = server.getGame();
        final Specification sSpec = sGame.getSpecification();
        final Player sPlayer = sGame.getFreeColGameObject(player.getId(),
                                                          Player.class);

        FoundingFather father = gui.getChoice(null, fatherTitle,
            "cancel",
            sSpec.getFoundingFathers().stream()
                .filter(f -> !sPlayer.hasFather(f))
                .map(f -> {
                        String msg = Messages.getName(f);
                        return new ChoiceItem<FoundingFather>(msg, f);
                    })
                .sorted().collect(Collectors.toList()));
        if (father != null) {
            server.getInGameController()
                .addFoundingFather((ServerPlayer)sPlayer, father);
        }
    }

    /**
     * Debug action to add gold.
     *
     * Called from the debug menu.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public static void addGold(final FreeColClient freeColClient) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final GUI gui = freeColClient.getGUI();
        final Player player = freeColClient.getMyPlayer();
        final Game sGame = server.getGame();
        final Player sPlayer = sGame.getFreeColGameObject(player.getId(),
                                                          Player.class);

        String response = gui.getInput(null,
            StringTemplate.template("prompt.selectGold"),
            Integer.toString(1000), "ok", "cancel");
        if (response == null || response.isEmpty()) return;
        int gold;
        try {
            gold = Integer.parseInt(response);
        } catch (NumberFormatException x) {
            return;
        }
        player.modifyGold(gold);
        sPlayer.modifyGold(gold);
    }

    /**
     * Debug action to add immigration.
     *
     * Called from the debug menu.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public static void addImmigration(final FreeColClient freeColClient) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final GUI gui = freeColClient.getGUI();
        final Player player = freeColClient.getMyPlayer();
        final Game sGame = server.getGame();
        final Player sPlayer = sGame.getFreeColGameObject(player.getId(),
                                                          Player.class);

        String response = gui.getInput(null,
            StringTemplate.template("prompt.selectImmigration"),
            Integer.toString(100), "ok", "cancel");
        if (response == null || response.isEmpty()) return;
        int crosses;
        try {
            crosses = Integer.parseInt(response);
        } catch (NumberFormatException x) {
            return;
        }
        player.modifyImmigration(crosses);
        sPlayer.modifyImmigration(crosses);
    }

    /**
     * Debug action to add liberty to the player colonies.
     *
     * Called from the debug menu.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public static void addLiberty(final FreeColClient freeColClient) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final GUI gui = freeColClient.getGUI();
        final Player player = freeColClient.getMyPlayer();
        final Game sGame = server.getGame();

        String response = gui.getInput(null,
            StringTemplate.template("prompt.selectLiberty"),
            Integer.toString(100), "ok", "cancel");
        if (response == null || response.isEmpty()) return;
        int liberty;
        try {
            liberty = Integer.parseInt(response);
        } catch (NumberFormatException x) {
            return;
        }
        for (Colony c : player.getColonies()) {
            c.addLiberty(liberty);
            sGame.getFreeColGameObject(c.getId(), Colony.class)
                .addLiberty(liberty);
        }
    }

    /**
     * Adds a change listener to a menu (the debug menu in fact),
     * that changes the label on a menu item when the skip status changes.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param menu The menu to add the change listener to.
     * @param item The menu item whose label should change.
     */
    public static void addSkipChangeListener(final FreeColClient freeColClient,
                                             JMenu menu, final JMenuItem item) {
        final FreeColServer server = freeColClient.getFreeColServer();
        if (server == null) return;

        menu.addChangeListener((ChangeEvent e) -> {
                boolean skipping = server.getInGameController()
                    .getSkippedTurns() > 0;
                item.setText(Messages.message((skipping)
                        ? "menuBar.debug.stopSkippingTurns"
                        : "menuBar.debug.skipTurns"));
            });
    }

    /**
     * Debug action to add a new unit to a tile.
     *
     * Called from tile popup menu.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param tile The <code>Tile</code> to add to.
     */
    public static void addNewUnitToTile(final FreeColClient freeColClient,
                                        Tile tile) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game sGame = server.getGame();
        final Specification sSpec = sGame.getSpecification();
        final Player player = freeColClient.getMyPlayer();
        final Player sPlayer = sGame.getFreeColGameObject(player.getId(),
                                                          Player.class);
        final Tile sTile = sGame.getFreeColGameObject(tile.getId(), Tile.class);
        final GUI gui = freeColClient.getGUI();

        UnitType unitChoice = gui.getChoice(null,
            StringTemplate.template("prompt.selectUnitType"), "cancel",
            sSpec.getUnitTypeList().stream()
                .map(ut -> {
                        String msg = Messages.getName(ut);
                        return new ChoiceItem<UnitType>(msg, ut);
                    })
                .sorted().collect(Collectors.toList()));
        if (unitChoice == null) return;

        Unit carrier = null, sCarrier = null;
        if (!sTile.isLand() && !unitChoice.isNaval()) {
            sCarrier = find(sTile.getUnitList(), u -> u.isNaval()
                && u.getSpaceLeft() >= unitChoice.getSpaceTaken());
        }
        Location loc = (sCarrier != null) ? sCarrier : sTile;
        ServerUnit sUnit = new ServerUnit(sGame, loc, sPlayer,
                                          unitChoice);//-vis(sPlayer)
        ((ServerPlayer)sPlayer).exploreForUnit(sUnit);
        sUnit.setMovesLeft(sUnit.getInitialMovesLeft());
        sPlayer.invalidateCanSeeTiles();//+vis(sPlayer)

        freeColClient.getConnectController().reconnect();
        // Note "game" is no longer valid after reconnect.
        Unit unit = freeColClient.getGame()
            .getFreeColGameObject(sUnit.getId(), Unit.class);
        if (unit != null) {
            gui.setActiveUnit(unit);
            gui.refresh();
            gui.resetMenuBar();
        }
    }

    /**
     * Debug action to add goods to a unit.
     *
     * Called from tile popup menu.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param unit The <code>Unit</code> to add to.
     */
    public static void addUnitGoods(final FreeColClient freeColClient,
                                    final Unit unit) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game sGame = server.getGame();
        final Specification sSpec = sGame.getSpecification();
        final GUI gui = freeColClient.getGUI();

        List<ChoiceItem<GoodsType>> gtl = new ArrayList<>();
        for (GoodsType t : sSpec.getGoodsTypeList()) {
            if (t.isFoodType() && t != sSpec.getPrimaryFoodType()) continue;
            String msg = Messages.getName(t);
            gtl.add(new ChoiceItem<>(msg, t));
        }
        Collections.sort(gtl);
        GoodsType goodsType = gui.getChoice(null,
            StringTemplate.template("prompt.selectGoodsType"),
            "cancel",
            sSpec.getGoodsTypeList().stream()
            .filter(gt -> !gt.isFoodType() || gt == sSpec.getPrimaryFoodType())
            .map(gt -> {
                    String msg = Messages.getName(gt);
                    return new ChoiceItem<GoodsType>(msg, gt);
                })
            .sorted().collect(Collectors.toList()));
        if (goodsType == null) return;

        String amount = gui.getInput(null,
            StringTemplate.template("prompt.selectGoodsAmount"),
            "20", "ok", "cancel");
        if (amount == null) return;

        int a;
        try {
            a = Integer.parseInt(amount);
        } catch (NumberFormatException nfe) {
            return;
        }
        GoodsType sGoodsType = sSpec.getGoodsType(goodsType.getId());
        GoodsContainer ugc = unit.getGoodsContainer();
        GoodsContainer sgc = sGame.getFreeColGameObject(ugc.getId(),
                                                        GoodsContainer.class);
        ugc.setAmount(goodsType, a);
        sgc.setAmount(sGoodsType, a);
    }

    /**
     * Debug action to apply a disaster to a colony.
     *
     * Called from tile popup menu.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param colony The <code>Colony</code> to apply a disaster to.
     */
    public static void applyDisaster(final FreeColClient freeColClient,
                                     final Colony colony) {
        final GUI gui = freeColClient.getGUI();
        List<RandomChoice<Disaster>> disasters = colony.getDisasters();
        if (disasters.isEmpty()) {
            gui.showErrorMessage(StringTemplate
                .template("error.disasterNotAvailable")
                .addName("%colony%", colony.getName()));
            return;
        }
        Disaster disaster = gui.getChoice(null,
            StringTemplate.template("prompt.selectDisaster"), "cancel",
            disasters.stream()
                .map(rc -> {
                        String label = Messages.getName(rc.getObject())
                            + " " + Integer.toString(rc.getProbability());
                        return new ChoiceItem<Disaster>(label, rc.getObject());
                    })
                .sorted().collect(Collectors.toList()));
        if (disaster == null) return;

        final FreeColServer server = freeColClient.getFreeColServer();
        final Game sGame = server.getGame();
        final ServerColony sColony = sGame.getFreeColGameObject(colony.getId(),
            ServerColony.class);
        final Disaster sDisaster = sGame.getSpecification()
            .getDisaster(disaster.getId());
        if (server.getInGameController().debugApplyDisaster(sColony, sDisaster)
            <= 0) {
            gui.showErrorMessage(StringTemplate
                .template("error.disasterAvoided")
                .addName("%colony%", colony.getName())
                .addNamed("%disaster%", disaster));
        }
        freeColClient.getInGameController().nextModelMessage();
    }

    /**
     * Debug action to change ownership of a colony.
     *
     * Called from tile popup menu.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param colony The <code>Colony</code> to take ownership of.
     */
    public static void changeOwnership(final FreeColClient freeColClient,
                                       final Colony colony) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game sGame = server.getGame();
        final ServerColony sColony = sGame.getFreeColGameObject(colony.getId(),
            ServerColony.class);
        final GUI gui = freeColClient.getGUI();
        final Game game = freeColClient.getGame();

        Player player = gui.getChoice(null,
            StringTemplate.template("prompt.selectOwner"), "cancel",
            game.getLiveEuropeanPlayers(colony.getOwner()).stream()
                .map(p -> {
                        String msg = Messages.message(p.getCountryLabel());
                        return new ChoiceItem<Player>(msg, p);
                    })
                .sorted().collect(Collectors.toList()));
        if (player == null) return;

        ServerPlayer sPlayer = sGame.getFreeColGameObject(player.getId(),
                                                          ServerPlayer.class);
        server.getInGameController().debugChangeOwner(sColony, sPlayer);

        Player myPlayer = freeColClient.getMyPlayer();
        if (gui.getActiveUnit() != null
            && gui.getActiveUnit().getOwner() != myPlayer) {
            freeColClient.getInGameController().nextActiveUnit();
        }
        gui.refresh();
        gui.resetMenuBar();
    }

    /**
     * Debug action to change ownership of a unit.
     *
     * Called from tile popup menu.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param unit The <code>Unit</code> to take ownership of.
     */
    public static void changeOwnership(final FreeColClient freeColClient,
                                       final Unit unit) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final GUI gui = freeColClient.getGUI();
        final Game game = unit.getGame();

        Player player = gui.getChoice(null,
            StringTemplate.template("prompt.selectOwner"), "cancel",
            game.getLivePlayers(null).stream()
            .filter(p -> unit.getType().isAvailableTo(p))
            .map(p -> {
                    String msg = Messages.message(p.getCountryLabel());
                    return new ChoiceItem<Player>(msg, p);
                })
            .sorted().collect(Collectors.toList()));
        if (player == null || unit.getOwner() == player) return;

        final Game sGame = server.getGame();
        ServerUnit sUnit = sGame.getFreeColGameObject(unit.getId(), 
                                                      ServerUnit.class);
        ServerPlayer sPlayer = sGame.getFreeColGameObject(player.getId(),
                                                          ServerPlayer.class);
        server.getInGameController().debugChangeOwner(sUnit, sPlayer);

        Player myPlayer = freeColClient.getMyPlayer();
        if (unit.getOwner() == myPlayer) {
            gui.setActiveUnit(unit);
        } else {
            freeColClient.getInGameController().nextActiveUnit();
        }
        gui.refresh();
        gui.resetMenuBar();
    }

    /**
     * Debug action to change the roles of a unit.
     *
     * Called from tile popup menu.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param unit The <code>Unit</code> to change the role of.
     */
    public static void changeRole(final FreeColClient freeColClient,
                                  final Unit unit) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game sGame = server.getGame();
        final Unit sUnit = sGame.getFreeColGameObject(unit.getId(), Unit.class);
        final GUI gui = freeColClient.getGUI();

        Role roleChoice = gui.getChoice(null,
            StringTemplate.template("prompt.selectRole"), "cancel",
            sGame.getSpecification().getRoles().stream()
                .map(r -> new ChoiceItem<Role>(r.getId(), r))
                .sorted().collect(Collectors.toList()));
        if (roleChoice == null) return;

        sUnit.changeRole(roleChoice, roleChoice.getMaximumCount());
        freeColClient.getConnectController().reconnect();
    }

    /**
     * Debug action to check for client-server desynchronization.
     *
     * Called from the debug menu and client controller.
     * TODO: This is still fairly new and messy.  Defer i18n for a while.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @return True if desynchronization found.
     */
    public static boolean checkDesyncAction(final FreeColClient freeColClient) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game game = freeColClient.getGame();
        final Map map = game.getMap();
        final Player player = freeColClient.getMyPlayer();
        final Game sGame = server.getGame();
        final Map sMap = sGame.getMap();
        final ServerPlayer sPlayer = sGame.getFreeColGameObject(player.getId(),
                                                                ServerPlayer.class);

        boolean problemDetected = false;
        LogBuilder lb = new LogBuilder(256);
        lb.add("Desynchronization detected\n");
        for (Tile t : sMap.getAllTiles()) {
            if (!sPlayer.canSee(t)) continue;
            for (Unit u : t.getUnitList()) {
                if (!sPlayer.owns(u)
                    && (t.hasSettlement() || u.isOnCarrier())) continue;
                if (game.getFreeColGameObject(u.getId(), Unit.class) == null) {
                    lb.add("Unit missing on client-side.\n", "  Server: ",
                           u.getDescription(Unit.UnitLabelType.NATIONAL),
                           "(", u.getId(), ") from: ", 
                           u.getLocation().getId(), ".\n");
                    try {
                        lb.add("  Client: ", map.getTile(u.getTile().getX(),
                               u.getTile().getY()).getFirstUnit().getId(),
                               "\n");
                    } catch (NullPointerException npe) {}
                    problemDetected = true;
                } else {
                    Unit cUnit = game.getFreeColGameObject(u.getId(),
                                                           Unit.class);
                    if (cUnit.hasTile()
                        && !cUnit.getTile().getId().equals(u.getTile().getId())) {
                        lb.add("Unit located on different tiles.\n",
                            "  Server: ", u.getDescription(Unit.UnitLabelType.NATIONAL),
                            "(", u.getId(), ") from: ",
                            u.getLocation().getId(), "\n",
                            "  Client: ", cUnit.getDescription(Unit.UnitLabelType.NATIONAL),
                            "(", cUnit.getId(), ") at: ",
                            cUnit.getLocation().getId(), "\n");
                        problemDetected = true;
                    }
                }
            }
            Tile ct = game.getFreeColGameObject(t.getId(), Tile.class);
            Settlement sSettlement = t.getSettlement();
            Settlement cSettlement = ct.getSettlement();
            if (sSettlement == null) {
                if (cSettlement == null) {
                    ;// OK
                } else {
                    lb.add("Settlement still present in client: ", cSettlement);
                    problemDetected = true;
                }
            } else {
                if (cSettlement == null) {
                    lb.add("Settlement not present in client: ", sSettlement);
                    problemDetected = true;
                } else if (sSettlement.getId().equals(cSettlement.getId())) {
                    ;// OK
                } else {
                    lb.add("Settlements differ.\n  Server: ",
                        sSettlement.toString(), "\n  Client: ", 
                        cSettlement.toString(), "\n");
                }
            }
        }

        boolean goodsProblemDetected = false;
        for (GoodsType sg : sGame.getSpecification().getGoodsTypeList()) {
            int sPrice = sPlayer.getMarket().getBidPrice(sg, 1);
            GoodsType cg = game.getSpecification().getGoodsType(sg.getId());
            int cPrice = player.getMarket().getBidPrice(cg, 1);
            if (sPrice != cPrice) {
                lb.add("Goods prices for ", sg, " differ.\n");
                goodsProblemDetected = true;
            }
        }
        if (goodsProblemDetected) {
            lb.add("  Server:\n", sPlayer.getMarket(), "\n",
                "  Client:\n", player.getMarket(), "\n");
            problemDetected = true;
        }

        if (problemDetected) {
            lb.shrink("\n");
            String err = lb.toString();
            freeColClient.getGUI().showInformationMessage(err);
            logger.severe(err);
        }
        return problemDetected;
    }

    /**
     * Debug action to display an AI colony plan.
     *
     * Called from tile popup menu.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param colony The <code>Colony</code> to summarize.
     */
    public static void displayColonyPlan(final FreeColClient freeColClient,
                                         final Colony colony) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final AIMain aiMain = server.getAIMain();
        final AIColony aiColony = aiMain.getAIColony(colony);
        if (aiColony == null) {
            freeColClient.getGUI().showErrorMessage(StringTemplate
                .template("error.notAIColony")
                .addName("%colony%", colony.getName()));
        } else {
            // TODO: Missing i18n
            freeColClient.getGUI().showInformationMessage(aiColony.planToString());
        }
    }

    /**
     * Debug action to create a string showing the colony value for
     * a given tile and player.
     *
     * Note: passing the freeColClient is redundant for now, but will
     * be needed if/when we move getColonyValue into the AI.
     *
     * @param tile The colony <code>Tile</code> to evaluate.
     * @return A string describing the colony value of a tile.
     */
    public static String getColonyValue(Tile tile) {
        Player player = FreeColDebugger.debugDisplayColonyValuePlayer();
        if (player == null) return null;
        int value = player.getColonyValue(tile);
        if (value < 0) {
            return Player.NoValueType.fromValue(value).toString();
        }
        return Integer.toString(value);
    }

    /**
     * Debug action to display Europe.
     *
     * Called from the debug popup menu.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public static void displayEurope(final FreeColClient freeColClient) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game sGame = server.getGame();
        final AIMain aiMain = server.getAIMain();

        LogBuilder lb = new LogBuilder(256);
        List<Unit> inEurope = new ArrayList<>();
        List<Unit> toEurope = new ArrayList<>();
        List<Unit> toAmerica = new ArrayList<>();
        HashMap<String,List<Unit>> units = new HashMap<>();
        for (Player tp : sGame.getLiveEuropeanPlayers(null)) {
            Player p = sGame.getFreeColGameObject(tp.getId(), Player.class);
            if (p.getEurope() == null) continue;
            inEurope.clear();
            toEurope.clear();
            toAmerica.clear();
            units.clear();
            units.put(Messages.message("sailingToEurope"), toEurope);
            units.put(Messages.getName(p.getEurope()), inEurope);
            units.put(Messages.message("sailingToAmerica"), toAmerica);
            lb.add("\n==", Messages.message(p.getCountryLabel()), "==\n");

            for (Unit u : p.getEurope().getUnitList()) {
                if (u.getDestination() instanceof Map) {
                    toAmerica.add(u);
                    continue;
                }
                if (u.getDestination() instanceof Europe) {
                    toEurope.add(u);
                    continue;
                }
                inEurope.add(u);
            }

            for (Entry<String, List<Unit>> entry : units.entrySet()) {
                final String label = entry.getKey();
                final List<Unit> list = entry.getValue();
                if (list.isEmpty()) continue;
                lb.add("\n->", label, "\n");
                for (Unit u : list) {
                    lb.add("\n", u.getDescription(Unit.UnitLabelType.NATIONAL));
                    if (u.isDamaged()) {
                        lb.add(" (", Messages.message(u.getRepairLabel()),
                            ")");
                    } else {
                        lb.add("    ");
                        AIUnit aiu = aiMain.getAIUnit(u);
                        if (!aiu.hasMission()) {
                            lb.add(" (", Messages.message("none"), ")");
                        } else {
                            lb.add(aiu.getMission().toString()
                                .replaceAll("\n", "    \n"));
                        }
                    }
                }
                lb.add("\n");
            }
            lb.add("\n->", Messages.message("immigrants"), "\n\n");
            for (UnitType unitType : p.getEurope().getRecruitables()) {
                lb.add(Messages.getName(unitType), "\n");
            }
            lb.add("\n");
        }
        freeColClient.getGUI().showInformationMessage(lb.toString());
    }

    /**
     * Debug action to display a mission.
     *
     * Called from tile popup menu.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param unit The <code>Unit</code> to display.
     */
    public static void displayMission(final FreeColClient freeColClient,
                                      final Unit unit) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final AIMain aiMain = server.getAIMain();
        final AIUnit aiUnit = aiMain.getAIUnit(unit);
        Mission m = aiUnit.getMission();
        String msg = (m == null) ? Messages.message("none")
            : (m instanceof TransportMission) ? ((TransportMission)m).toFullString()
            : m.toString();
        freeColClient.getGUI().showInformationMessage(msg);
    }

    /**
     * Debug action to dump a players units/iterators to stderr.
     *
     * Called from the debug menu.
     * TODO: missing i18n
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public static void displayUnits(final FreeColClient freeColClient) {
        final Player player = freeColClient.getMyPlayer();
        List<Unit> all = player.getUnits();
        LogBuilder lb = new LogBuilder(256);
        lb.add("\nActive units:\n");

        Unit u, first = player.getNextActiveUnit();
        if (first != null) {
            lb.add(first.toString(), "\nat ", first.getLocation(), "\n");
            all.remove(first);
            while (player.hasNextActiveUnit()
                && (u = player.getNextActiveUnit()) != first) {
                lb.add(u, "\nat ", u.getLocation(), "\n");
                all.remove(u);
            }
        }
        lb.add("Going-to units:\n");
        first = player.getNextGoingToUnit();
        if (first != null) {
            all.remove(first);
            lb.add(first, "\nat ", first.getLocation(), "\n");
            while (player.hasNextGoingToUnit()
                && (u = player.getNextGoingToUnit()) != first) {
                lb.add(u, "\nat ", u.getLocation(), "\n");
                all.remove(u);
            }
        }
        lb.add("Remaining units:\n");
        while (!all.isEmpty()) {
            u = all.remove(0);
            lb.add(u, "\nat ", u.getLocation(), "\n");
        }

        freeColClient.getGUI().showInformationMessage(lb.toString());
    }

    /**
     * Debug action to dump a tile to stderr.
     *
     * Called from tile popup menu.
     * Not concerned with i18n for stderr output.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param tile The <code>Tile</code> to dump.
     */
    public static void dumpTile(final FreeColClient freeColClient,
                                final Tile tile) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game sGame = server.getGame();
        final Player player = freeColClient.getMyPlayer();

        System.err.println("\nClient (" + player.getId() + "):");
        tile.save(System.err, WriteScope.toClient(player), true);
        System.err.println("\n\nServer:");
        Tile sTile = sGame.getFreeColGameObject(tile.getId(), Tile.class);
        sTile.save(System.err, WriteScope.toServer(), true);
        System.err.println("\n\nSave:");
        sTile.save(System.err, WriteScope.toSave(), true);
        System.err.println("\n");
    }

    /**
     * Debug action to reset the moves left of the units on a tile.
     *
     * Called from tile popup menu.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param units The <code>Unit</code>s to reactivate.
     */
    public static void resetMoves(final FreeColClient freeColClient,
                                  List<Unit> units) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game sGame = server.getGame();
        final GUI gui = freeColClient.getGUI();

        boolean first = true;
        for (Unit u : units) {
            Unit su = sGame.getFreeColGameObject(u.getId(), Unit.class);
            u.setMovesLeft(u.getInitialMovesLeft());
            su.setMovesLeft(su.getInitialMovesLeft());
            if (first) {
                gui.setActiveUnit(u);
                first = false;
            }
            for (Unit u2 : u.getUnitList()) {
                Unit su2 = sGame.getFreeColGameObject(u2.getId(), Unit.class);
                u2.setMovesLeft(u2.getInitialMovesLeft());
                su2.setMovesLeft(su2.getInitialMovesLeft());
            }
        }
        gui.refresh();
        gui.resetMenuBar();
    }

    /**
     * Debug action to reveal or hide the map.
     *
     * Called from the debug menu.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param reveal If true, reveal the map, else hide the map.
     */
    public static void revealMap(final FreeColClient freeColClient,
                                 boolean reveal) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game game = freeColClient.getGame();

        server.exploreMapForAllPlayers(reveal);

        // Removes fog of war when revealing the whole map
        // Restores previous setting when hiding it back again
        BooleanOption fogOfWarSetting = game.getSpecification()
            .getBooleanOption(GameOptions.FOG_OF_WAR);
        if (reveal) {
            FreeColDebugger.setNormalGameFogOfWar(fogOfWarSetting.getValue());
            fogOfWarSetting.setValue(false);
        } else {
            fogOfWarSetting.setValue(FreeColDebugger.getNormalGameFogOfWar());
        }
    }

    /**
     * Debug action to set the amount of goods in a colony.
     *
     * Called from the colony panel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param colony The <code>Colony</code> to set goods amounts in.
     */
    public static void setColonyGoods(final FreeColClient freeColClient,
                                      final Colony colony) {
        final Specification spec = colony.getSpecification();
        GoodsType goodsType = freeColClient.getGUI().getChoice(null,
            StringTemplate.template("prompt.selectGoodsType"), "cancel",
            spec.getGoodsTypeList().stream()
                .filter(gt -> !gt.isFoodType() || gt == spec.getPrimaryFoodType())
                .map(gt -> {
                        String msg = Messages.getName(gt);
                        return new ChoiceItem<GoodsType>(msg, gt);
                    })
                .sorted().collect(Collectors.toList()));
        if (goodsType == null) return;

        String response = freeColClient.getGUI().getInput(null,
                StringTemplate.template("prompt.selectGoodsAmount"),
                Integer.toString(colony.getGoodsCount(goodsType)),
                "ok", "cancel");
        if (response == null || response.isEmpty()) return;
        int a;
        try {
            a = Integer.parseInt(response);
        } catch (NumberFormatException nfe) {
            return;
        }

        final FreeColServer server = freeColClient.getFreeColServer();
        final Game sGame = server.getGame();
        final Specification sSpec = server.getSpecification();
        final GoodsType sGoodsType = sSpec.getGoodsType(goodsType.getId());
        GoodsContainer cgc = colony.getGoodsContainer();
        GoodsContainer sgc = sGame.getFreeColGameObject(cgc.getId(),
                                                        GoodsContainer.class);
        cgc.setAmount(goodsType, a);
        sgc.setAmount(sGoodsType, a);
    }

    /**
     * Debug action to set the next monarch action.
     *
     * Called from the debug menu.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param monarchTitle A label for the choice dialog.
     */
    public static void setMonarchAction(final FreeColClient freeColClient,
                                        String monarchTitle) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game sGame = server.getGame();
        final Player player = freeColClient.getMyPlayer();
        final ServerPlayer sPlayer = sGame.getFreeColGameObject(player.getId(),
            ServerPlayer.class);
        final GUI gui = freeColClient.getGUI();

        MonarchAction action = gui.getChoice(null, monarchTitle,
            "cancel",
            Arrays.stream(MonarchAction.values())
                .map(a -> new ChoiceItem<MonarchAction>(a))
                .sorted().collect(Collectors.toList()));
        if (action == null) return;
        
        server.getInGameController().setMonarchAction(sPlayer, action);
    }

    /**
     * Debug action to set the lost city rumour type on a tile.
     *
     * Called from tile popup menu.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param tile The <code>Tile</code> to operate on.
     */
    public static void setRumourType(final FreeColClient freeColClient,
                                     final Tile tile) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game sGame = server.getGame();
        final Tile sTile = sGame.getFreeColGameObject(tile.getId(),
                                                      Tile.class);

        RumourType rumourChoice = freeColClient.getGUI().getChoice(null,
            StringTemplate.template("prompt.selectLostCityRumour"),
            "cancel",
            Arrays.stream(RumourType.values())
                .filter(r -> r != RumourType.NO_SUCH_RUMOUR)
                .map(r -> new ChoiceItem<RumourType>(r.toString(), r))
                .sorted().collect(Collectors.toList()));
        if (rumourChoice == null) return;

        tile.getTileItemContainer().getLostCityRumour().setType(rumourChoice);
        sTile.getTileItemContainer().getLostCityRumour()
            .setType(rumourChoice);
    }

    /**
     * Debug action to skip turns.
     *
     * Called from the debug menu.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public static void skipTurns(FreeColClient freeColClient) {
        freeColClient.skipTurns(0); // Clear existing skipping

        String response = freeColClient.getGUI().getInput(null,
            StringTemplate.key("prompt.selectTurnsToSkip"),
            Integer.toString(10), "ok", "cancel");
        if (response == null || response.isEmpty()) return;
        int skip;
        try {
            skip = Integer.parseInt(response);
        } catch (NumberFormatException nfe) {
            skip = -1;
        }
        if (skip > 0) freeColClient.skipTurns(skip);
    }

    /**
     * Debug action to step the random number generator.
     *
     * Called from the debug menu.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public static void stepRNG(FreeColClient freeColClient) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final GUI gui = freeColClient.getGUI();

        boolean more = true;
        while (more) {
            int val = server.getInGameController().stepRandom();
            more = gui.confirm(null, StringTemplate
                .template("prompt.stepRNG")
                .addAmount("%value%", val),
                "more", "cancel");
        }
    }

    /**
     * Debug action to summarize information about a native settlement
     * that is normally hidden.
     *
     * Called from tile popup menu.
     * TODO: missing i18n
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param is The <code>IndianSettlement</code> to summarize.
     */
    public static void summarizeSettlement(final FreeColClient freeColClient,
                                           final IndianSettlement is) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game sGame = server.getGame();
        final AIMain aiMain = server.getAIMain();
        final Specification sSpec = sGame.getSpecification();
        final IndianSettlement sis = sGame.getFreeColGameObject(is.getId(),
            IndianSettlement.class);

        LogBuilder lb = new LogBuilder(256);
        lb.add(sis.getName(), "\n\nAlarm\n");
        Player mostHated = sis.getMostHated();
        for (Player p : sGame.getLiveEuropeanPlayers(null)) {
            Tension tension = sis.getAlarm(p);
            lb.add(Messages.message(p.getNationLabel()),
                   " ", ((tension == null) ? "(none)"
                       : Integer.toString(tension.getValue())),
                   ((mostHated == p) ? " (most hated)" : ""),
                   " ", Messages.message(sis.getAlarmLevelKey(p)),
                   " ", sis.getContactLevel(p), "\n");
        }

        lb.add("\nGoods Present\n");
        for (Goods goods : sis.getCompactGoods()) {
            lb.add(Messages.message(goods.getLabel(true)), "\n");
        }

        lb.add("\nGoods Production\n");
        for (GoodsType type : sSpec.getGoodsTypeList()) {
            int prod = sis.getTotalProductionOf(type);
            if (prod > 0) {
                lb.add(Messages.getName(type), " ", prod, "\n");
            }
        }

        lb.add("\nPrices (buy 1/100 / sell 1/100)\n");
        GoodsType[] wanted = sis.getWantedGoods();
        for (GoodsType type : sSpec.getStorableGoodsTypeList()) {
            int i;
            for (i = wanted.length - 1; i >= 0; i--) {
                if (type == wanted[i]) break;
            }
            lb.add(Messages.getName(type),
                   ": ", sis.getPriceToBuy(type, 1),
                   "/", sis.getPriceToBuy(type, 100),
                   " / ", sis.getPriceToSell(type, 1),
                   "/", sis.getPriceToSell(type, 100),
                   ((i < 0) ? "" : " wanted[" + Integer.toString(i) + "]"),
                   "\n");
        }

        lb.add("\nUnits present\n");
        for (Unit u : sis.getUnitList()) {
            Mission m = aiMain.getAIUnit(u).getMission();
            lb.add(u, " at ", u.getLocation());
            if (m != null) {
                lb.add(" ", m.getClass(), ".");
            }
            lb.add("\n");
        }
        lb.add("\nUnits owned\n");
        for (Unit u : sis.getOwnedUnits()) {
            Mission m = aiMain.getAIUnit(u).getMission();
            lb.add(u, " at ", u.getLocation());
            if (m != null) {
                lb.add(" ", m.getClass(), ".");
            }
            lb.add("\n");
        }

        lb.add("\nTiles\n");
        for (Tile t : sis.getOwnedTiles()) lb.add(t, "\n");

        lb.add("\nConvert Progress = ", sis.getConvertProgress());
        lb.add("\nLast Tribute = ", sis.getLastTribute());

        freeColClient.getGUI().showInformationMessage(lb.toString());
    }

    /**
     * Debug action to run the AI.
     *
     * Called from the debug menu.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public static void useAI(final FreeColClient freeColClient) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Player player = freeColClient.getMyPlayer();
        final AIMain aiMain = server.getAIMain();
        final AIPlayer ap = aiMain.getAIPlayer(player);

        ap.setDebuggingConnection(freeColClient.askServer().getConnection());
        ap.startWorking();
        freeColClient.getConnectController().reconnect();
    }
}
