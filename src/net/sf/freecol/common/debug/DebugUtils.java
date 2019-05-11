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

package net.sf.freecol.common.debug;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.ChangeEvent;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ChoiceItem;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColXMLWriter.WriteScope;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Disaster;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Game.LogoutReason;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.LostCityRumour.RumourType;
import net.sf.freecol.common.model.Market;
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
import net.sf.freecol.common.option.GameOptions;
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
 * Utilities for in-game debug support.
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
     * Reconnect utility.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    private static void reconnect(FreeColClient freeColClient) {
        freeColClient.getConnectController()
            .requestLogout(LogoutReason.RECONNECT);
    }
        

    /**
     * Debug action to add buildings to the user colonies.
     *
     * Called from the debug menu.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param title A label for the choice dialog.
     */
    public static void addBuildings(final FreeColClient freeColClient,
                                    String title) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game game = freeColClient.getGame();
        final Specification spec = game.getSpecification();
        final GUI gui = freeColClient.getGUI();
        final Player player = freeColClient.getMyPlayer();
        final Function<BuildingType, ChoiceItem<BuildingType>> mapper = bt ->
            new ChoiceItem<BuildingType>(Messages.getName(bt), bt);

        StringTemplate tmpl = StringTemplate.name(title);
        BuildingType buildingType = gui.getChoice(tmpl, "cancel",
            transform(spec.getBuildingTypeList(), alwaysTrue(), mapper,
                      Comparator.naturalOrder()));
        if (buildingType == null) return;

        final Game sGame = server.getGame();
        final BuildingType sBuildingType = server.getSpecification()
            .getBuildingType(buildingType.getId());
        final Player sPlayer = sGame.getFreeColGameObject(player.getId(),
                                                          Player.class);
        int fails = 0;
        List<Colony> sColonies = sPlayer.getColonyList();
        List<String> results = new ArrayList<>(sColonies.size());
        for (Colony sColony : sColonies) {
            Colony.NoBuildReason reason
                = sColony.getNoBuildReason(sBuildingType, null);
            results.add(sColony.getName() + ": " + reason);
            if (reason == Colony.NoBuildReason.NONE) {
                Building sBuilding = sColony.getBuilding(sBuildingType);
                List<Unit> present = (sBuilding == null)
                    ? Collections.<Unit>emptyList()
                    : sBuilding.getUnitList();
                if (sBuildingType.isDefenceType()) {
                    sColony.getTile().cacheUnseen();//+til
                }
                sBuilding = new ServerBuilding(sGame, sColony, sBuildingType);
                sColony.addBuilding(sBuilding);//-til
                for (Unit u : present) u.setLocation(sBuilding);
            } else {
                fails++;
            }
        }
        gui.showInformationMessage(join(", ", results));
        if (fails < sPlayer.getSettlementCount()) { // Brutally resynchronize
            reconnect(freeColClient);
        }
    }

    /**
     * Debug action to add a founding father.
     *
     * Called from the debug menu.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
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
        final Predicate<FoundingFather> noFatherPred = f ->
            !sPlayer.hasFather(f);
        final Function<FoundingFather, ChoiceItem<FoundingFather>> mapper
            = f -> new ChoiceItem<FoundingFather>(Messages.getName(f), f);

        StringTemplate tmpl = StringTemplate.name(fatherTitle);
        FoundingFather father = gui.getChoice(tmpl, "cancel",
            transform(sSpec.getFoundingFathers(), noFatherPred, mapper,
                      Comparator.naturalOrder()));
        if (father != null) {
            server.getInGameController().addFoundingFather(sPlayer, father);
        }
    }

    /**
     * Debug action to add gold.
     *
     * Called from the debug menu.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
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
     * Add an "add goods" menu item to a unit menu.
     *
     * @param fcc The enclosing {@code FreeColClient}.
     * @param unit The {@code Unit} to add goods to.
     * @param menu The {@code JPopupMenu} to add the entry to.
     */
    public static void addGoodsAdditionEntry(final FreeColClient fcc,
                                             final Unit unit, JPopupMenu menu) {
        JMenuItem addg = new JMenuItem("Add goods");
        addg.setOpaque(false);
        addg.addActionListener((ActionEvent ae) -> {
                DebugUtils.addUnitGoods(fcc, unit);
            });
        addg.setEnabled(true);
        menu.add(addg);
    }

    /**
     * Debug action to add immigration.
     *
     * Called from the debug menu.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
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
     * @param freeColClient The {@code FreeColClient} for the game.
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
        for (Colony c : player.getColonyList()) {
            c.addLiberty(liberty);
            sGame.getFreeColGameObject(c.getId(), Colony.class)
                .addLiberty(liberty);
        }
    }

    /**
     * Adds a change listener to a menu (the debug menu in fact),
     * that changes the label on a menu item when the skip status changes.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
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
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param tile The {@code Tile} to add to.
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
        final Function<UnitType, ChoiceItem<UnitType>> mapper = ut ->
            new ChoiceItem<UnitType>(Messages.getName(ut), ut);

        StringTemplate tmpl = StringTemplate.template("prompt.selectUnitType");
        UnitType unitChoice = gui.getChoice(tmpl, "cancel",
            transform(sSpec.getUnitTypeList(), alwaysTrue(), mapper,
                      Comparator.naturalOrder()));
        if (unitChoice == null) return;

        // Is there a server-unit with space left?
        Unit sCarrier = (sTile.isLand() || unitChoice.isNaval()) ? null
            : find(sTile.getUnitList(), u ->
                u.isNaval() && u.getSpaceLeft() >= unitChoice.getSpaceTaken());

        ServerUnit sUnit
            = new ServerUnit(sGame, ((sCarrier != null) ? sCarrier : sTile),
                             sPlayer, unitChoice);//-vis(sPlayer)
        ((ServerPlayer)sPlayer).exploreForUnit(sUnit);
        sUnit.setMovesLeft(sUnit.getInitialMovesLeft());
        sPlayer.invalidateCanSeeTiles();//+vis(sPlayer)

        reconnect(freeColClient);
        // Note "game" is no longer valid after reconnect.
        Unit unit = freeColClient.getGame()
            .getFreeColGameObject(sUnit.getId(), Unit.class);
        if (unit != null) {
            gui.changeView(unit);
            gui.refresh();
            gui.resetMenuBar();
        }
    }

    /**
     * Debug action to add goods to a unit.
     *
     * Called from tile popup menu.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param unit The {@code Unit} to add to.
     */
    private static void addUnitGoods(final FreeColClient freeColClient,
                                     final Unit unit) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game sGame = server.getGame();
        final Specification sSpec = sGame.getSpecification();
        final GUI gui = freeColClient.getGUI();
        final Predicate<GoodsType> goodsPred = gt ->
            !gt.isFoodType() || gt == sSpec.getPrimaryFoodType();            
        final Function<GoodsType, ChoiceItem<GoodsType>> mapper = gt ->
            new ChoiceItem<GoodsType>(Messages.getName(gt), gt);
            
        StringTemplate tmpl = StringTemplate.template("prompt.selectGoodsType");
        GoodsType goodsType = gui.getChoice(tmpl, "cancel",
            transform(sSpec.getGoodsTypeList(), goodsPred, mapper,
                      Comparator.naturalOrder()));
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
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param colony The {@code Colony} to apply a disaster to.
     */
    public static void applyDisaster(final FreeColClient freeColClient,
                                     final Colony colony) {
        final GUI gui = freeColClient.getGUI();
        final Function<RandomChoice<Disaster>,
                       ChoiceItem<Disaster>> mapper = rc ->
            new ChoiceItem<Disaster>(Messages.getName(rc.getObject()) + " "
                                     + Integer.toString(rc.getProbability()),
                                     rc.getObject());
        List<ChoiceItem<Disaster>> disasters
            = transform(colony.getDisasterChoices(), alwaysTrue(), mapper,
                        Comparator.naturalOrder());
        if (disasters.isEmpty()) {
            gui.showErrorMessage(StringTemplate
                .template("error.disasterNotAvailable")
                .addName("%colony%", colony.getName()));
            return;
        }
        StringTemplate tmpl = StringTemplate.template("prompt.selectDisaster");
        Disaster disaster = gui.getChoice(tmpl, "cancel", disasters);
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
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param colony The {@code Colony} to take ownership of.
     */
    public static void changeOwnership(final FreeColClient freeColClient,
                                       final Colony colony) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game sGame = server.getGame();
        final ServerColony sColony = sGame.getFreeColGameObject(colony.getId(),
            ServerColony.class);
        final GUI gui = freeColClient.getGUI();
        final Game game = freeColClient.getGame();
        final Function<Player, ChoiceItem<Player>> mapper = p ->
            new ChoiceItem<Player>(Messages.message(p.getCountryLabel()), p);

        StringTemplate tmpl = StringTemplate.template("prompt.selectOwner");
        Player player = gui.getChoice(tmpl, "cancel",
            transform(game.getLiveEuropeanPlayers(colony.getOwner()),
                      alwaysTrue(), mapper, Comparator.naturalOrder()));
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
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param unit The {@code Unit} to take ownership of.
     */
    public static void changeOwnership(final FreeColClient freeColClient,
                                       final Unit unit) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final GUI gui = freeColClient.getGUI();
        final Game game = unit.getGame();
        final Function<Player, ChoiceItem<Player>> mapper = p ->
            new ChoiceItem<Player>(Messages.message(p.getCountryLabel()), p);

        StringTemplate tmpl = StringTemplate.template("prompt.selectOwner");
        Player player = gui.getChoice(tmpl, "cancel",
            transform(game.getLivePlayers(),
                      p -> unit.getType().isAvailableTo(p), mapper,
                      Comparator.naturalOrder()));
        if (player == null || unit.getOwner() == player) return;

        final Game sGame = server.getGame();
        ServerUnit sUnit = sGame.getFreeColGameObject(unit.getId(), 
                                                      ServerUnit.class);
        ServerPlayer sPlayer = sGame.getFreeColGameObject(player.getId(),
                                                          ServerPlayer.class);
        server.getInGameController().debugChangeOwner(sUnit, sPlayer);

        Player myPlayer = freeColClient.getMyPlayer();
        if (myPlayer.owns(unit)) gui.changeView(unit);
        gui.refresh();
        gui.resetMenuBar();
    }

    /**
     * Debug action to change the roles of a unit.
     *
     * Called from tile popup menu.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param unit The {@code Unit} to change the role of.
     */
    public static void changeRole(final FreeColClient freeColClient,
                                  final Unit unit) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game sGame = server.getGame();
        final Unit sUnit = sGame.getFreeColGameObject(unit.getId(), Unit.class);
        final GUI gui = freeColClient.getGUI();
        final Function<Role, ChoiceItem<Role>> roleMapper = r ->
            new ChoiceItem<Role>(r.getId(), r);

        StringTemplate tmpl = StringTemplate.template("prompt.selectRole");
        Role roleChoice = gui.getChoice(tmpl, "cancel",
            transform(sGame.getSpecification().getRoles(), alwaysTrue(),
                      roleMapper, Comparator.naturalOrder()));
        if (roleChoice == null) return;

        sUnit.changeRole(roleChoice, roleChoice.getMaximumCount());
        reconnect(freeColClient);
    }

    /**
     * Debug action to check for client-server desynchronization.
     *
     * Called from the debug menu and client controller.
     * TODO: This is still fairly new and messy.  Defer i18n for a while.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @return True if desynchronization found.
     */
    public static boolean checkDesyncAction(final FreeColClient freeColClient) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game cGame = freeColClient.getGame();
        final Player cPlayer = freeColClient.getMyPlayer();
        final Game sGame = server.getGame();
        final Map sMap = sGame.getMap();
        final ServerPlayer sPlayer
            = sGame.getFreeColGameObject(cPlayer.getId(), ServerPlayer.class);

        LogBuilder lb = new LogBuilder(256);
        lb.add("Desynchronization detected\n");
        lb.mark();
        sMap.forEachTile(t -> sPlayer.canSee(t),
                         t -> checkDesyncTile(cGame, sPlayer, t, lb));
        boolean problemDetected = lb.grew();
        // Do not check goods amount, the server only sends changes to
        // the client when the *price* changes.
        boolean goodsProblemDetected = false;
        final Market cMarket = cPlayer.getMarket();
        final Market sMarket = sPlayer.getMarket();
        if (sMarket != null && cMarket != null) {
            for (GoodsType sg : sGame.getSpecification().getGoodsTypeList()) {
                int sPrice = sMarket.getBidPrice(sg, 1);
                GoodsType cg = cGame.getSpecification().getGoodsType(sg.getId());
                int cPrice = cMarket.getBidPrice(cg, 1);
                if (sPrice != cPrice) {
                    lb.add("Goods prices for ", sg, " differ: ", sPrice,
                        "!=", cPrice, " ");
                    goodsProblemDetected = true;
                }
            }
        }
        if (goodsProblemDetected) {
            lb.add("  Server:\n", sPlayer.getMarket(), "\n",
                "  Client:\n", cPlayer.getMarket(), "\n");
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
     * Check if a tile is desynchronized.
     *
     * @param cGame The client-side {@code Game}.
     * @param sPlayer The server-side {@code ServerPlayer} to check for.
     * @param sTile The server-side {@code Tile} to check.
     * @param lb A {@code LogBuilder} to log problems to.
     */
    private static void checkDesyncTile(Game cGame, ServerPlayer sPlayer,
                                        Tile sTile, LogBuilder lb) {
        Tile cTile = cGame.getFreeColGameObject(sTile.getId(), Tile.class);
        for (Unit su : transform(sTile.getUnits(), u -> sPlayer.canSeeUnit(u))) {
            Unit cu = cGame.getFreeColGameObject(su.getId(), Unit.class);
            if (cu == null) {
                lb.add("Unit missing on client-side.\n", "  Server: ",
                    su.getDescription(Unit.UnitLabelType.NATIONAL),
                    "(", su.getId(), ") from: ", 
                    su.getLocation().getId(), ".\n");
                try {
                    lb.add("  Client: ", cTile.getFirstUnit(), "\n");
                } catch (NullPointerException npe) {}
            } else {
                if (cu.hasTile()
                    && !cu.getTile().getId().equals(su.getTile().getId())) {
                    lb.add("Unit located on different tiles.\n",
                        "  Server: ", su.getDescription(Unit.UnitLabelType.NATIONAL),
                        "(", su.getId(), ") from: ",
                        su.getLocation().getId(), "\n",
                        "  Client: ", cu.getDescription(Unit.UnitLabelType.NATIONAL),
                        "(", cu.getId(), ") at: ",
                        cu.getLocation().getId(), "\n");
                }
            }
        }
        Settlement sSettlement = sTile.getSettlement();
        Settlement cSettlement = cTile.getSettlement();
        if (sSettlement == null) {
            if (cSettlement == null) {
                ;// OK
            } else {
                lb.add("Settlement still present in client: ", cSettlement);
            }
        } else {
            if (cSettlement == null) {
                lb.add("Settlement not present in client: ", sSettlement);
            } else if (sSettlement.getId().equals(cSettlement.getId())) {
                ;// OK
            } else {
                lb.add("Settlements differ.\n  Server: ",
                    sSettlement.toString(), "\n  Client: ", 
                    cSettlement.toString(), "\n");
            }
        }
    }
        
    /**
     * Debug action to display an AI colony plan.
     *
     * Called from tile popup menu.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param colony The {@code Colony} to summarize.
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
     * @param tile The colony {@code Tile} to evaluate.
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
     * @param freeColClient The {@code FreeColClient} for the game.
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
        for (Player tp : sGame.getLiveEuropeanPlayerList()) {
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
                } else if (u.getDestination() instanceof Europe) {
                    toEurope.add(u);
                } else {
                    inEurope.add(u);
                }
            }
            forEachMapEntry(units,
                e -> logEurope(aiMain, lb, e.getKey(), e.getValue()));
            lb.add("\n->", Messages.message("immigrants"), "\n\n");
            for (AbstractUnit au : p.getEurope().getExpandedRecruitables(false)) {
                lb.add(Messages.message(au.getSingleLabel()), "\n");
            }
            lb.add("\n");
        }
        freeColClient.getGUI().showInformationMessage(lb.toString());
    }

    /**
     * Log European unit lists.
     *
     * @param aiMain The main AI object.
     * @param lb The {@code LogBuilder} to log to.
     * @param label A label for the group of units.
     * @param units The {@code Unit}s to log.
     */
    private static void logEurope(AIMain aiMain, LogBuilder lb, String label,
                                  List<Unit> units) {
        if (units.isEmpty()) return;
        lb.add("\n->", label, "\n");
        for (Unit u : units) {
            lb.add("\n", u.getDescription(Unit.UnitLabelType.NATIONAL));
            if (u.isDamaged()) {
                lb.add(" (", Messages.message(u.getRepairLabel()), ")");
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
    /**
     * Debug action to display a mission.
     *
     * Called from tile popup menu.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param unit The {@code Unit} to display.
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
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public static void displayUnits(final FreeColClient freeColClient) {
        final Player player = freeColClient.getMyPlayer();
        Set<Unit> all = player.getUnitSet();
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
        for (Unit x : all) {
            lb.add(x, "\nat ", x.getLocation(), "\n");
        }

        freeColClient.getGUI().showInformationMessage(lb.toString());
    }

    /**
     * Debug action to dump a tile to stderr.
     *
     * Called from tile popup menu.
     * Not concerned with i18n for stderr output.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param tile The {@code Tile} to dump.
     */
    public static void dumpTile(final FreeColClient freeColClient,
                                final Tile tile) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game game = freeColClient.getGame();
        final Game sGame = server.getGame();
        final Player player = freeColClient.getMyPlayer();

        System.err.println("\nClient (" + game.getClientUserName()
            + "/" + player.getId() + "):");
        tile.save(System.err, WriteScope.toClient(player), true);
        System.err.println("\n\nServer:");
        Tile sTile = sGame.getFreeColGameObject(tile.getId(), Tile.class);
        sTile.save(System.err, WriteScope.toServer(), true);
        System.err.println("\n\nSave:");
        sTile.save(System.err, WriteScope.toSave(), true);
        System.err.println('\n');
    }

    /**
     * Debug action to reset the moves left of the units on a tile.
     *
     * Called from tile popup menu.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param units The {@code Unit}s to reactivate.
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
                gui.changeView(u);
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
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param reveal If true, reveal the map, else hide the map.
     */
    public static void revealMap(final FreeColClient freeColClient,
                                 boolean reveal) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game game = freeColClient.getGame();
        final Specification spec = game.getSpecification();

        server.exploreMapForAllPlayers(reveal);

        // Removes fog of war when revealing the whole map
        // Restores previous setting when hiding it back again
        if (reveal) {
            FreeColDebugger.setNormalGameFogOfWar(spec.getBoolean(GameOptions.FOG_OF_WAR));
            spec.setBoolean(GameOptions.FOG_OF_WAR, false);
        } else {
            spec.setBoolean(GameOptions.FOG_OF_WAR,
                            FreeColDebugger.getNormalGameFogOfWar());
        }
    }

    /**
     * Debug action to set the amount of goods in a colony.
     *
     * Called from the colony panel.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param colony The {@code Colony} to set goods amounts in.
     */
    public static void setColonyGoods(final FreeColClient freeColClient,
                                      final Colony colony) {
        final Specification spec = colony.getSpecification();
        final GUI gui = freeColClient.getGUI();
        final Predicate<GoodsType> goodsPred = gt ->
            !gt.isFoodType() || gt == spec.getPrimaryFoodType();
        final Function<GoodsType, ChoiceItem<GoodsType>> mapper = gt ->
            new ChoiceItem<GoodsType>(Messages.getName(gt), gt);

        StringTemplate tmpl = StringTemplate.template("prompt.selectGoodsType");
        GoodsType goodsType = gui.getChoice(tmpl, "cancel",
            transform(spec.getGoodsTypeList(), goodsPred, mapper,
                      Comparator.naturalOrder()));
        if (goodsType == null) return;

        String response = gui.getInput(null,
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
     * Set COMMS logging.
     *
     * @param log If true, enable COMMS logging.
     */
    public static void setCommsLogging(final FreeColClient freeColClient,
                                       boolean log) {
        final FreeColServer server = freeColClient.getFreeColServer();
        if (server != null) server.getServer().setLogging(log);
    }
    
    /**
     * Debug action to set the next monarch action.
     *
     * Called from the debug menu.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
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
        final Function<MonarchAction, ChoiceItem<MonarchAction>> mapper = a ->
            new ChoiceItem<MonarchAction>(a);

        StringTemplate tmpl = StringTemplate.name(monarchTitle);
        MonarchAction action = gui.getChoice(tmpl, "cancel",
            transform(MonarchAction.values(), alwaysTrue(), mapper,
                      Comparator.naturalOrder()));
        if (action == null) return;
        
        server.getInGameController().setMonarchAction(sPlayer, action);
    }

    /**
     * Debug action to set the lost city rumour type on a tile.
     *
     * Called from tile popup menu.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param tile The {@code Tile} to operate on.
     */
    public static void setRumourType(final FreeColClient freeColClient,
                                     final Tile tile) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game sGame = server.getGame();
        final Tile sTile = sGame.getFreeColGameObject(tile.getId(),
                                                      Tile.class);
        final Predicate<RumourType> realRumourPred = r ->
            r != RumourType.NO_SUCH_RUMOUR;
        final Function<RumourType, ChoiceItem<RumourType>> mapper = r ->
            new ChoiceItem<RumourType>(r.toString(), r);
            
        StringTemplate tmpl = StringTemplate.template("prompt.selectLostCityRumour");
        RumourType rumourChoice = freeColClient.getGUI()
            .getChoice(tmpl, "cancel",
                       transform(RumourType.values(), realRumourPred, mapper,
                                 Comparator.naturalOrder()));
        if (rumourChoice == null) return;

        tile.getTileItemContainer().getLostCityRumour().setType(rumourChoice);
        sTile.getTileItemContainer().getLostCityRumour()
            .setType(rumourChoice);
    }

    /**
     * Consider showing a foreign colony.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param colony The {@code Colony} to display.
     */
    public static void showForeignColony(FreeColClient freeColClient,
                                         Colony colony) {
        if (colony == null
            || !FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.MENUS))
            return;
        // Get the full details from the server if possible
        final FreeColServer server = freeColClient.getFreeColServer();
        if (server == null) return;
        colony = server.getGame()
            .getFreeColGameObject(colony.getId(), Colony.class);
        freeColClient.getGUI().showColonyPanel(colony, null);
    }
        
    /**
     * Debug action to skip turns.
     *
     * Called from the debug menu.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
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
     * Debug action to display statistics.
     *
     * Called from the debug menu.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public static void statistics(FreeColClient freeColClient) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game sGame = server.getGame();
        final Game cGame = freeColClient.getGame();
        final GUI gui = freeColClient.getGUI();

        java.util.Map<String, String> serverStats = sGame.getStatistics();
        serverStats.putAll(server.getAIMain().getAIStatistics());
        java.util.Map<String, String> clientStats = cGame.getStatistics();
        gui.showStatisticsPanel(serverStats, clientStats);
    }

    /**
     * Debug action to step the random number generator.
     *
     * Called from the debug menu.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public static void stepRNG(FreeColClient freeColClient) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final GUI gui = freeColClient.getGUI();

        boolean more = true;
        while (more) {
            int val = server.getInGameController().stepRandom();
            more = gui.confirm(StringTemplate.template("prompt.stepRNG")
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
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param is The {@code IndianSettlement} to summarize.
     */
    public static void summarizeSettlement(final FreeColClient freeColClient,
                                           final IndianSettlement is) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game sGame = server.getGame();
        final AIMain aiMain = server.getAIMain();
        final Specification sSpec = sGame.getSpecification();
        final IndianSettlement sis = sGame.getFreeColGameObject(is.getId(),
            IndianSettlement.class);
        final List<GoodsType> sGoodsTypes = sSpec.getGoodsTypeList();

        LogBuilder lb = new LogBuilder(256);
        lb.add(sis.getName(), "\n\nAlarm\n");
        Player mostHated = sis.getMostHated();
        for (Player p : sGame.getLiveEuropeanPlayerList()) {
            Tension tension = sis.getAlarm(p);
            lb.add(Messages.message(p.getNationLabel()),
                   " ", ((tension == null) ? "(none)"
                       : Integer.toString(tension.getValue())),
                   ((mostHated == p) ? " (most hated)" : ""),
                   " ", Messages.message(sis.getAlarmLevelKey(p)),
                   " ", sis.getContactLevel(p), "\n");
        }

        lb.add("\nGoods\n");
        for (GoodsType gt : sGoodsTypes) {
            int amount = sis.getGoodsCount(gt);
            int prod = sis.getTotalProductionOf(gt);
            if (amount > 0 || prod != 0) {
                lb.add(Messages.message(new AbstractGoods(gt, amount).getLabel()),
                    " ", ((prod > 0) ? "+" : ""), prod, "\n");
            }
        }

        lb.add("\nPotential sales\n");
        for (Goods g : sis.getSellGoods(null)) {
            lb.add(Messages.getName(g.getType()), " ", g.getAmount(),
                " = ", sis.getPriceToSell(g.getType(), g.getAmount()), "\n");
        }
        
        lb.add("\nPrices (buy 1/100 / sell 1/100)\n");
        List<GoodsType> wanted = sis.getWantedGoods();
        for (GoodsType type : sSpec.getStorableGoodsTypeList()) {
            int idx = wanted.indexOf(type);
            lb.add(Messages.getName(type),
                   ": ", sis.getPriceToBuy(type, 1),
                   "/", sis.getPriceToBuy(type, 100),
                   " / ", sis.getPriceToSell(type, 1),
                   "/", sis.getPriceToSell(type, 100),
                   ((idx < 0) ? "" : " wanted[" + Integer.toString(idx) + "]"),
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
        for (Unit u : sis.getOwnedUnitList()) {
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
}
