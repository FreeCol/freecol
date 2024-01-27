package net.sf.freecol.server.generator;

import static net.sf.freecol.common.model.Constants.INFINITY;
import static net.sf.freecol.common.util.CollectionUtils.find;
import static net.sf.freecol.common.util.CollectionUtils.minimize;
import static net.sf.freecol.common.util.CollectionUtils.transform;
import static net.sf.freecol.common.util.RandomUtils.getRandomMember;
import static net.sf.freecol.common.util.RandomUtils.randomShuffle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.function.ToIntFunction;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.EuropeanNationType;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.option.GameOptions;
import net.sf.freecol.server.model.ServerBuilding;
import net.sf.freecol.server.model.ServerColony;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;

/**
 * Handles the starting placement of European units. 
 */
class EuropeanStartingPositionsGenerator {
    
    private static final Logger logger = Logger.getLogger(EuropeanStartingPositionsGenerator.class.getName());
    private final Random random;
    
    
    EuropeanStartingPositionsGenerator(Random random) {
        this.random = random;
    }
    

    /**
     * Creates and places the European units as determined by the map settings.
     * 
     * The non-naval are placed on land if there are no starting naval unit able to
     * carry them, and in Europe if there's a starting naval unit but no more room.
     * 
     * Naval units are placed in Europe if non-naval units are placed on land.
     *
     * @param map The {@code Map} to place the european units on.
     * @param players The players in the game. Starting units are still only created
     *      for non-REF European players.
     */
    void createEuropeanUnits(Map map, List<Player> players) {
        final List<Player> europeanPlayers = players.stream()
                .filter(p -> p.isEuropean() && !p.isREF())
                .collect(Collectors.toList());
        if (europeanPlayers.isEmpty()) {
            throw new RuntimeException("No players to generate units for!");
        }

        final java.util.Map<Player, StartingUnits> playerStartingUnits = determineStartingUnits(europeanPlayers);
        final java.util.Map<Player, Tile> playerStartingTiles = determineStartingTiles(map, europeanPlayers, playerStartingUnits);

        for (Player player : europeanPlayers) {
            final Tile start = playerStartingTiles.get(player);
            final StartingUnits startingUnits = playerStartingUnits.get(player);
            
            placeUnitsAtStartingLocation(player, start, startingUnits);
        }
    }


    private java.util.Map<Player, StartingUnits> determineStartingUnits(List<Player> europeanPlayers) {
        final java.util.Map<Player, StartingUnits> playerStartingUnits = new HashMap<>();
        for (Player player : europeanPlayers) {
            playerStartingUnits.put(player, new StartingUnits(player, random));
        }
        return playerStartingUnits;
    }

    private java.util.Map<Player, Tile> determineStartingTiles(Map map, List<Player> europeanPlayers, java.util.Map<Player, StartingUnits> playerStartingUnits) {
        return determineStartingTilesWithoutUsingPredeterminedPositions(map, europeanPlayers, playerStartingUnits);
    }

    private java.util.Map<Player, Tile> determineStartingTilesWithoutUsingPredeterminedPositions(Map map, List<Player> europeanPlayers, java.util.Map<Player, StartingUnits> playerStartingUnits) {
        final Specification spec = map.getSpecification();
        final int positionType = spec.getInteger(GameOptions.STARTING_POSITIONS);
        final int number = europeanPlayers.size();
        
        // Make lists of candidate starting tiles on the east and west
        // of the map, then break them up by land and "sea" (revisit
        // if we get a map with a lake on the edge)
        List<Tile> eastTiles = new ArrayList<>();
        List<Tile> westTiles = new ArrayList<>();
        List<Tile> eastLandTiles = new ArrayList<>();
        List<Tile> westLandTiles = new ArrayList<>();
        List<Tile> eastSeaTiles = new ArrayList<>();
        List<Tile> westSeaTiles = new ArrayList<>();
        map.collectStartingTiles(eastTiles, westTiles);
        for (Tile t : eastTiles) {
            if (t.isLand()) eastLandTiles.add(t); else eastSeaTiles.add(t);
        }
        for (Tile t : westTiles) {
            if (t.isLand()) westLandTiles.add(t); else westSeaTiles.add(t);
        }

        // Now consider what type of positions we are selecting from
        switch (positionType) {
        case GameOptions.STARTING_POSITIONS_CLASSIC:
            // Break the lists up into at least <number> candidate
            // positions and shuffle.  Empty the relevant list on failure.
            sampleTiles(eastLandTiles, number);
            sampleTiles(eastSeaTiles, number);
            sampleTiles(westLandTiles, number);
            sampleTiles(westSeaTiles, number);
            break;
        case GameOptions.STARTING_POSITIONS_RANDOM:
            // Random starts are the same as classic but do not
            // distinguish between east and west
            eastLandTiles.addAll(westLandTiles);
            eastSeaTiles.addAll(westSeaTiles);
            sampleTiles(eastLandTiles, number);
            sampleTiles(eastSeaTiles, number);
            break;
        case GameOptions.STARTING_POSITIONS_HISTORICAL:
            break; // Historic positions retain all the possible tiles
        }

        final java.util.Map<Player, Tile> playerStartingTiles = new HashMap<>();
        for (Player player : europeanPlayers) {
            boolean startEast = player.getNation().getStartsOnEastCoast();
            boolean startAtSea = !playerStartingUnits.get(player).getCarriers().isEmpty();

            // Select a starting position from the available tiles
            Tile start = null;
            switch (positionType) {
            case GameOptions.STARTING_POSITIONS_CLASSIC:
                // Classic mode respects coast preference, the lists
                // are pre-sampled and shuffled
                start = ((startAtSea)
                    ? ((startEast) ? eastSeaTiles : westSeaTiles)
                    : ((startEast) ? eastLandTiles : westLandTiles)).remove(0);
                break;
            case GameOptions.STARTING_POSITIONS_RANDOM:
                // Random mode is as classic but ignores coast
                // preference, the east lists already contain the west
                start = ((startAtSea) ? eastSeaTiles : eastLandTiles).remove(0);
                break;
            case GameOptions.STARTING_POSITIONS_HISTORICAL:
                start = (startAtSea)
                    ? findHistoricalStartingPosition(player, map,
                        eastSeaTiles, westSeaTiles)
                    : findHistoricalStartingPosition(player, map,
                        eastLandTiles, westLandTiles);
                break;
            }
            if (start == null) {
                throw new RuntimeException("Failed to find start tile "
                    + ((startAtSea) ? "at sea" : "on land")
                    + " for player " + player);
            }
            playerStartingTiles.put(player, start);
        }
        return playerStartingTiles;
    }


    private void placeUnitsAtStartingLocation(Player player, Tile start, StartingUnits startingUnits) {
        final Map map = player.getGame().getMap();
        final Specification spec = map.getSpecification();
        
        player.setEntryTile(start);

        final Europe europe = player.getEurope();
        final ServerPlayer serverPlayer = (ServerPlayer) player;
        if (!start.isLand()) { // All aboard!
            for (Unit u : startingUnits.getCarriers()) {
                u.setLocation(start);
                serverPlayer.exploreForUnit(u);
            }
            passengers: for (Unit unit : startingUnits.getPassengers()) {
                for (Unit carrier : startingUnits.getCarriers()) {
                    if (carrier.canAdd(unit)) {
                        unit.setLocation(carrier);
                        continue passengers;
                    }
                }
                // no space left on carriers
                unit.setLocation(europe);
            }
            for (Unit u : startingUnits.getOtherNaval()) {
                u.setLocation(start);
                serverPlayer.exploreForUnit(u);
            }
        } else { // Land ho!
            for (Unit u : startingUnits.getPassengers()) {
                u.setLocation(start);
                serverPlayer.exploreForUnit(u);
            }
            for (Unit u : startingUnits.getOtherNaval()) {
                u.setLocation(europe);
            }
        }

        if (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.INIT)) {
            createDebugUnits(map, player, start);
            spec.setInteger(GameOptions.STARTING_MONEY, 10000);
        }

        // Start our REF player entry tile somewhere near to our
        // starting tile, but do not place their units.
        // They are assumed to always be on naval transport.
        final Player ourREF = player.getREFPlayer();
        if (ourREF == null) {
            return;
        }
        final int distance = 10;
        final List<Tile> refTiles = StreamSupport.stream(map.getCircleTiles(start, true, distance).spliterator(), false)
                .filter(t -> !t.isLand())
                .collect(Collectors.toList());
        final Tile startRef = getRandomMember(logger, ourREF + " start", refTiles, random);
            
        ourREF.setEntryTile(startRef);
    }
    
    /**
     * Find the best historical starting position for a player from lists
     * of tiles.
     *
     * @param player The {@code Player} to find a tile for.
     * @param map The {@code Map} to search.
     * @param east A list of starting {@code Tile}s on the east of the map.
     * @param west A list of starting {@code Tile}s on the west of the map.
     * @return The best {@code Tile} found, or null if none suitable.
     */
    private Tile findHistoricalStartingPosition(final Player player,
                                                final Map map,
                                                List<Tile> east,
                                                List<Tile> west) {
        final Nation nation = player.getNation();
        final int latY = map.getRow(nation.getPreferredLatitude());
        List<Tile> tiles = (nation.getStartsOnEastCoast()) ? east : west;
        if (tiles.isEmpty()) return null;
        final ToIntFunction<Tile> dist = t -> Math.abs(t.getY() - latY);
        final Comparator<Tile> closest = Comparator.comparingInt(dist);
        Tile ret = minimize(tiles, closest);
        tiles.remove(ret); // chosen tile is no longer a candidate
        return ret;
    }

    /**
     * Sample a list of tiles to pick spread out starting positions.
     * Shuffle the result or clear it if there were too few tiles.
     *
     * @param tiles The list of {@code Tile}s to sample.
     * @param number The number of players, which determines the spacing.
     * @return True if there were enough tiles in the list.
     */
    private boolean sampleTiles(List<Tile> tiles, int number) {
        final int n = tiles.size();
        final int step = n / number;
        if (step <= 1) {
            tiles.clear();
            return false;
        }
        List<Tile> samples = new ArrayList<>();
        // The offset start prevents selecting right on the poles
        for (int i = step/2; i < n; i += step) {
            samples.add(tiles.get(i));
        }
        tiles.clear();
        tiles.addAll(samples);
        randomShuffle(logger, "Starting tiles", tiles, random);
        return true;
    }

    private List<Unit> createDebugUnits(Map map, Player player, Tile startTile) {
        final Game game = map.getGame();
        final Specification spec = game.getSpecification();
        List<Unit> ret = new ArrayList<>(20);
        // In debug mode give each player a few more units and a colony.
        UnitType unitType = spec.getUnitType("model.unit.galleon");
        Unit galleon = new ServerUnit(game, startTile, player, unitType);
        galleon.setName(player.getNameForUnit(unitType, random));
        ret.add(galleon);
        unitType = spec.getUnitType("model.unit.privateer");
        Unit privateer = new ServerUnit(game, startTile, player, unitType);
        ret.add(privateer);
        privateer.setName(player.getNameForUnit(unitType, random));
        ((ServerPlayer)player).exploreForUnit(privateer);
        unitType = spec.getUnitType("model.unit.freeColonist");
        ret.add(new ServerUnit(game, galleon, player, unitType));
        unitType = spec.getUnitType("model.unit.veteranSoldier");
        ret.add(new ServerUnit(game, galleon, player, unitType));
        unitType = spec.getUnitType("model.unit.jesuitMissionary");
        ret.add(new ServerUnit(game, galleon, player, unitType));

        Tile colonyTile = null;
        for (Tile tempTile : map.getCircleTiles(startTile, true, INFINITY)) {
            if (tempTile.isPolar()) continue; // No initial polar colonies
            if (player.canClaimToFoundSettlement(tempTile)) {
                colonyTile = tempTile;
                break;
            }
        }

        if (colonyTile == null) {
            return ret;
        }
        colonyTile.setType(find(spec.getTileTypeList(), t -> !t.isWater()));
        unitType = spec.getUnitType("model.unit.expertFarmer");
        Unit buildColonyUnit = new ServerUnit(game, colonyTile,
                player, unitType);
        ret.add(buildColonyUnit);
        String colonyName = Messages.message(player.getNationLabel())
                + " " + Messages.message("Colony");
        Colony colony = new ServerColony(game, player, colonyName, colonyTile);
        player.addSettlement(colony);
        colony.placeSettlement(true);
        for (Tile tile : transform(colonyTile.getSurroundingTiles(1,1), t ->
        (!t.hasSettlement()
                && (t.getOwner() == null || !t.getOwner().isEuropean())))) {
            tile.changeOwnership(player, colony);
            if (tile.hasLostCityRumour()) tile.removeLostCityRumour();
        }
        buildColonyUnit.setLocation(colony);
        Tile ct = buildColonyUnit.getWorkTile();
        if (ct != null) {
            for (TileType t : transform(spec.getTileTypeList(),
                    tt -> !tt.isWater())) {
                ct.setType(t);
                TileImprovementType plowType = map.getSpecification()
                        .getTileImprovementType("model.improvement.plow");
                TileImprovement plow = new TileImprovement(game, ct, plowType, null);
                plow.setTurnsToComplete(0);
                ct.add(plow);
                break;
            }
        }
        BuildingType schoolType = spec.getBuildingType("model.building.schoolhouse");
        Building schoolhouse = new ServerBuilding(game, colony, schoolType);
        colony.addBuilding(schoolhouse);

        unitType = spec.getUnitType("model.unit.elderStatesman");
        Unit statesman = new ServerUnit(game, colonyTile, player, unitType);
        ret.add(statesman);
        statesman.setLocation(colony.getWorkLocationFor(statesman,
                statesman.getType().getExpertProduction()));

        unitType = spec.getUnitType("model.unit.expertLumberJack");
        Unit lumberjack = new ServerUnit(game, colony, player, unitType);
        ret.add(lumberjack);
        Tile lt = lumberjack.getWorkTile();
        if (lt != null) {
            TileType tt = find(spec.getTileTypeList(), TileType::isForested);
            if (tt != null) lt.setType(tt);
            lumberjack.changeWorkType(lumberjack.getType()
                    .getExpertProduction());
        }

        unitType = spec.getUnitType("model.unit.masterCarpenter");
        ret.add(new ServerUnit(game, colony, player, unitType));

        unitType = spec.getUnitType("model.unit.seasonedScout");
        Unit scout = new ServerUnit(game, colonyTile, player, unitType);
        ret.add(scout);
        ((ServerPlayer)player).exploreForUnit(scout);

        unitType = spec.getUnitType("model.unit.veteranSoldier");
        ret.add(new ServerUnit(game, colonyTile, player, unitType));
        ret.add(new ServerUnit(game, colonyTile, player, unitType));
        unitType = spec.getUnitType("model.unit.artillery");
        ret.add(new ServerUnit(game, colonyTile, player, unitType));
        ret.add(new ServerUnit(game, colonyTile, player, unitType));
        ret.add(new ServerUnit(game, colonyTile, player, unitType));
        unitType = spec.getUnitType("model.unit.treasureTrain");
        Unit train = new ServerUnit(game, colonyTile, player, unitType);
        ret.add(train);
        train.setTreasureAmount(10000);
        unitType = spec.getUnitType("model.unit.wagonTrain");
        Unit wagon = new ServerUnit(game, colonyTile, player, unitType);
        ret.add(wagon);
        GoodsType cigarsType = spec.getGoodsType("model.goods.cigars");
        Goods cigards = new Goods(game, wagon, cigarsType, 5);
        wagon.add(cigards);
        unitType = spec.getUnitType("model.unit.jesuitMissionary");
        ret.add(new ServerUnit(game, colonyTile, player, unitType));
        ret.add(new ServerUnit(game, colonyTile, player, unitType));

        ((ServerPlayer)player).exploreForSettlement(colony);
        return ret;
    }
    
    private static final class StartingUnits {
        
        private final List<Unit> carriers = new ArrayList<>();
        private final List<Unit> passengers = new ArrayList<>();
        private final List<Unit> otherNaval = new ArrayList<>();
        
        StartingUnits(Player player, Random random) {
            final Game game = player.getGame();
            final Specification spec = player.getSpecification();
            List<AbstractUnit> unitList = ((EuropeanNationType) player.getNationType()).getStartingUnits();
            for (AbstractUnit startingUnit : unitList) {
                UnitType type = startingUnit.getType(spec);
                Role role = startingUnit.getRole(spec);
                Unit newUnit = new ServerUnit(game, null, player, type, role);
                newUnit.setName(player.getNameForUnit(type, random));
                if (newUnit.isNaval()) {
                    if (newUnit.canCarryUnits()) {
                        newUnit.setState(Unit.UnitState.ACTIVE);
                        carriers.add(newUnit);
                    } else {
                        otherNaval.add(newUnit);
                    }
                } else {
                    newUnit.setState(Unit.UnitState.SENTRY);
                    passengers.add(newUnit);
                }
            }
        }
        
        List<Unit> getCarriers() {
            return carriers;
        }
        
        List<Unit> getPassengers() {
            return passengers;
        }
        
        List<Unit> getOtherNaval() {
            return otherNaval;
        }
    }
}
