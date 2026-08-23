package net.sf.freecol.common.model;

import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.FreeColTestUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.sf.freecol.common.util.CollectionUtils.toList;


public class CombatAmbushTest extends FreeColTestCase {

    private static final Role dragoonRole = spec().getRole("model.role.dragoon");
    private static final Role infantryRole = spec().getRole("model.role.infantry");
    private static final Role nativeDragoonRole = spec().getRole("model.role.nativeDragoon");

    private static final TileType hills = spec().getTileType("model.tile.hills");
    private static final TileType plains = spec().getTileType("model.tile.plains");

    private static final UnitType braveType = spec().getUnitType("model.unit.brave");
    private static final UnitType colonialRegularType = spec().getUnitType("model.unit.colonialRegular");
    private static final UnitType kingsRegularType = spec().getUnitType("model.unit.kingsRegular");
    private static final UnitType artilleryType = spec().getUnitType("model.unit.artillery");


    // Goal: check that a colonial dragoon gets an ambush bonus against King's Regulars
    // when the defender stands on defensive terrain.
    // Expected: ambush modifiers match the terrain defence modifiers exactly.
    public void testAmbushBonusAppliedAgainstREF() {
        // Start a new server game using a plains-heavy map for predictable terrain.
        Game game = ServerTestHelper.startServerGame(getTestMap(plains, true));
        InGameController igc = ServerTestHelper.getInGameController();

        // Create a rebel player that has declared independence and the opposing REF side.
        ServerPlayer rebels = getServerPlayer(game, "model.nation.french");
        rebels.addAbility(new Ability(Ability.INDEPENDENCE_DECLARED));
        ServerPlayer refPlayer = igc.createREFPlayer(rebels);

        SimpleCombatModel combatModel = new SimpleCombatModel();

        Map map = game.getMap();
        // Place the defending King's Regular on hills to activate defensive terrain modifiers.
        Tile defenderTile = map.getTile(5, 8);
        defenderTile.setType(hills);
        defenderTile.setExplored(rebels, true);
        defenderTile.setExplored(refPlayer, true);

        // Place the attacking colonial dragoon on adjacent plains for the ambush attempt.
        Tile attackerTile = map.getTile(4, 8);
        attackerTile.setType(plains);
        attackerTile.setExplored(rebels, true);
        attackerTile.setExplored(refPlayer, true);

        Unit defender = new ServerUnit(game, defenderTile, refPlayer,
                                       kingsRegularType, infantryRole);
        Unit attacker = new ServerUnit(game, attackerTile, rebels,
                                       colonialRegularType, dragoonRole);
        attacker.setMovesLeft(attacker.getInitialMovesLeft());
        defender.setStateUnchecked(UnitState.ACTIVE);

        rebels.setStance(refPlayer, Stance.WAR);
        refPlayer.setStance(rebels, Stance.WAR);

        List<Modifier> hillDefenceModifiers = toList(hills.getDefenceModifiers());
        assertFalse(hillDefenceModifiers.isEmpty());
        assertTrue(defender.hasAbility(Ability.AMBUSH_PENALTY));
        assertTrue(hills.hasAbility(Ability.AMBUSH_TERRAIN));
        assertTrue(attacker.canAmbush(defender));

        // Ambush modifiers should mirror the terrain defence modifiers exactly.
        Set<Float> expectedValues = collectModifierValues(hillDefenceModifiers);
        Set<Float> ambushValues = collectAmbushValues(combatModel, attacker, defender);

        assertEquals(expectedValues.size(), ambushValues.size());
        assertEquals(expectedValues, ambushValues);
    }

    // Goal: make sure King's Regulars cannot be ambushed without defensive terrain.
    // Expected: attacker cannot ambush and no modifier uses Specification.AMBUSH_BONUS_SOURCE.
    public void testNoAmbushAgainstREFWithoutDefensiveTerrain() {
        // Start a fresh game where both tiles remain plains to remove terrain defence.
        Game game = ServerTestHelper.startServerGame(getTestMap(plains, true));
        InGameController igc = ServerTestHelper.getInGameController();

        // Rebels with independence declared face the REF without helpful terrain.
        ServerPlayer rebels = getServerPlayer(game, "model.nation.french");
        rebels.addAbility(new Ability(Ability.INDEPENDENCE_DECLARED));
        ServerPlayer refPlayer = igc.createREFPlayer(rebels);

        SimpleCombatModel combatModel = new SimpleCombatModel();

        Map map = game.getMap();
        // Keep both defender and attacker on plains tiles.
        Tile defenderTile = map.getTile(5, 8);
        defenderTile.setType(plains);
        defenderTile.setExplored(rebels, true);
        defenderTile.setExplored(refPlayer, true);

        Tile attackerTile = map.getTile(4, 8);
        attackerTile.setType(plains);
        attackerTile.setExplored(rebels, true);
        attackerTile.setExplored(refPlayer, true);

        Unit defender = new ServerUnit(game, defenderTile, refPlayer,
                                       kingsRegularType, infantryRole);
        Unit attacker = new ServerUnit(game, attackerTile, rebels,
                                       colonialRegularType, dragoonRole);
        attacker.setMovesLeft(attacker.getInitialMovesLeft());
        defender.setStateUnchecked(UnitState.ACTIVE);

        rebels.setStance(refPlayer, Stance.WAR);
        refPlayer.setStance(rebels, Stance.WAR);

        assertTrue(defender.getTile().getType().getDefenceModifiers().isEmpty());
        assertFalse(attacker.canAmbush(defender));

        // There should be no ambush bonus applied when the terrain offers no defence.
        Set<Modifier> offensiveModifiers = combatModel.getOffensiveModifiers(attacker, defender);
        for (Modifier modifier : offensiveModifiers) {
            assertFalse(Specification.AMBUSH_BONUS_SOURCE == modifier.getSource());
        }
    }

    // Goal: ensure attacking from a colony blocks ambush against King's Regulars.
    // Expected: attacker cannot ambush and no ambush modifier is added.
    public void testNoAmbushAgainstREFFromColony() {
        // Launch another game but make the attacker start from a colony instead of the tile.
        Game game = ServerTestHelper.startServerGame(getTestMap(plains, true));
        InGameController igc = ServerTestHelper.getInGameController();

        // Rebels again fight the REF with independence already declared.
        ServerPlayer rebels = getServerPlayer(game, "model.nation.french");
        rebels.addAbility(new Ability(Ability.INDEPENDENCE_DECLARED));
        ServerPlayer refPlayer = igc.createREFPlayer(rebels);

        SimpleCombatModel combatModel = new SimpleCombatModel();

        Map map = game.getMap();
        // Defender benefits from hills, attacker colony sits on adjacent plains.
        Tile defenderTile = map.getTile(5, 8);
        defenderTile.setType(hills);
        defenderTile.setExplored(rebels, true);
        defenderTile.setExplored(refPlayer, true);

        Tile attackerTile = map.getTile(4, 8);
        attackerTile.setType(plains);
        attackerTile.setExplored(rebels, true);
        attackerTile.setExplored(refPlayer, true);

        Colony colony = FreeColTestUtils.getColonyBuilder()
            .player(rebels).colonyTile(attackerTile).initialColonists(1)
            .build();

        Unit defender = new ServerUnit(game, defenderTile, refPlayer,
                                       kingsRegularType, infantryRole);
        Unit attacker = new ServerUnit(game, colony, rebels,
                                       colonialRegularType, dragoonRole);
        attacker.setMovesLeft(attacker.getInitialMovesLeft());
        defender.setStateUnchecked(UnitState.ACTIVE);

        rebels.setStance(refPlayer, Stance.WAR);
        refPlayer.setStance(rebels, Stance.WAR);

        assertFalse(attacker.canAmbush(defender));

        // Units attacking from a colony should not receive an ambush bonus.
        Set<Modifier> offensiveModifiers = combatModel.getOffensiveModifiers(attacker, defender);
        for (Modifier modifier : offensiveModifiers) {
            assertFalse(Specification.AMBUSH_BONUS_SOURCE == modifier.getSource());
        }
    }

    // Goal: ensure REF artillery under ambush outside settlements also suffers the
    // artillery-in-the-open penalty.
    // Expected: attacker gains ambush modifiers while defender gains an artillery
    // penalty and no hill defence modifiers apply.
    public void testREFArtilleryUnderAmbushHasArtilleryPenalty() {
        // Set up a rebel versus REF battle with defensive terrain for the artillery.
        Game game = ServerTestHelper.startServerGame(getTestMap(plains, true));
        InGameController igc = ServerTestHelper.getInGameController();

        ServerPlayer rebels = getServerPlayer(game, "model.nation.french");
        rebels.addAbility(new Ability(Ability.INDEPENDENCE_DECLARED));
        ServerPlayer refPlayer = igc.createREFPlayer(rebels);

        SimpleCombatModel combatModel = new SimpleCombatModel();

        Map map = game.getMap();
        Tile defenderTile = map.getTile(5, 8);
        defenderTile.setType(hills);
        defenderTile.setExplored(rebels, true);
        defenderTile.setExplored(refPlayer, true);

        Tile attackerTile = map.getTile(4, 8);
        attackerTile.setType(plains);
        attackerTile.setExplored(rebels, true);
        attackerTile.setExplored(refPlayer, true);

        Unit defender = new ServerUnit(game, defenderTile, refPlayer,
                                       artilleryType);
        Unit attacker = new ServerUnit(game, attackerTile, rebels,
                                       colonialRegularType, dragoonRole);
        attacker.setMovesLeft(attacker.getInitialMovesLeft());
        defender.setStateUnchecked(UnitState.ACTIVE);

        rebels.setStance(refPlayer, Stance.WAR);
        refPlayer.setStance(rebels, Stance.WAR);

        assertTrue(defender.hasAbility(Ability.BOMBARD));
        assertTrue(attacker.canAmbush(defender));

        // Ambush should grant offensive modifiers sourced from the ambush bonus.
        Set<Modifier> offensiveModifiers = combatModel.getOffensiveModifiers(attacker, defender);
        boolean ambushBonusFound = false;
        for (Modifier modifier : offensiveModifiers) {
            if (Specification.AMBUSH_BONUS_SOURCE == modifier.getSource()) {
                ambushBonusFound = true;
                break;
            }
        }
        assertTrue(ambushBonusFound);

        List<Modifier> hillDefenceModifiers = toList(hills.getDefenceModifiers());
        assertFalse(hillDefenceModifiers.isEmpty());

        // Defensive modifiers should contain the artillery penalty but no hill defence.
        Set<Modifier> defensiveModifiers = combatModel.getDefensiveModifiers(attacker, defender);
        boolean artilleryPenaltyFound = false;
        for (Modifier modifier : defensiveModifiers) {
            assertFalse(hills == modifier.getSource());
            if (Specification.ARTILLERY_PENALTY_SOURCE == modifier.getSource()) {
                artilleryPenaltyFound = true;
            }
        }
        assertTrue(artilleryPenaltyFound);
    }

    // Goal: confirm native units apply ambush bonus when a European defender has terrain defence.
    // Expected: ambush values match the defender's terrain defence modifiers.
    public void testNativesAmbushAppliesAgainstEuropeanUnit() {
        // Start a game between Dutch colonists and Tupi natives.
        Game game = ServerTestHelper.startServerGame(getTestMap(plains, true));

        ServerPlayer dutch = getServerPlayer(game, "model.nation.dutch");
        ServerPlayer tupi = getServerPlayer(game, "model.nation.tupi");

        SimpleCombatModel combatModel = new SimpleCombatModel();

        Map map = game.getMap();
        // Give the European defender a hill tile to provide defensive modifiers.
        Tile defenderTile = map.getTile(5, 8);
        defenderTile.setType(hills);
        defenderTile.setExplored(dutch, true);
        defenderTile.setExplored(tupi, true);

        // Place the native attacker on neighbouring plains so ambush is possible.
        Tile attackerTile = map.getTile(4, 8);
        attackerTile.setType(plains);
        attackerTile.setExplored(dutch, true);
        attackerTile.setExplored(tupi, true);

        Unit defender = new ServerUnit(game, defenderTile, dutch,
                                       colonialRegularType, dragoonRole);
        Unit attacker = new ServerUnit(game, attackerTile, tupi,
                                       braveType, nativeDragoonRole);
        attacker.setMovesLeft(attacker.getInitialMovesLeft());
        defender.setStateUnchecked(UnitState.ACTIVE);

        tupi.setStance(dutch, Stance.WAR);
        dutch.setStance(tupi, Stance.WAR);

        List<Modifier> hillDefenceModifiers = toList(hills.getDefenceModifiers());
        assertFalse(hillDefenceModifiers.isEmpty());
        assertTrue(attacker.hasAbility(Ability.AMBUSH_BONUS));
        assertTrue(attacker.canAmbush(defender));

        // Ambush bonuses for natives should match the hill defence modifiers.
        Set<Float> expectedValues = collectModifierValues(hillDefenceModifiers);
        Set<Float> ambushValues = collectAmbushValues(combatModel, attacker, defender);

        assertEquals(expectedValues.size(), ambushValues.size());
        assertEquals(expectedValues, ambushValues);
    }

    // Goal: check natives need the defender on defensive terrain to use ambush.
    // Expected: attacker cannot ambush and no ambush modifier appears.
    public void testNativeAmbushRequiresDefensiveTerrain() {
        // Run the same native versus European scenario but keep everyone on plains.
        Game game = ServerTestHelper.startServerGame(getTestMap(plains, true));

        ServerPlayer dutch = getServerPlayer(game, "model.nation.dutch");
        ServerPlayer tupi = getServerPlayer(game, "model.nation.tupi");

        SimpleCombatModel combatModel = new SimpleCombatModel();

        Map map = game.getMap();
        Tile defenderTile = map.getTile(5, 8);
        defenderTile.setType(plains);
        defenderTile.setExplored(dutch, true);
        defenderTile.setExplored(tupi, true);

        Tile attackerTile = map.getTile(4, 8);
        attackerTile.setType(plains);
        attackerTile.setExplored(dutch, true);
        attackerTile.setExplored(tupi, true);

        Unit defender = new ServerUnit(game, defenderTile, dutch,
                                       colonialRegularType, dragoonRole);
        Unit attacker = new ServerUnit(game, attackerTile, tupi,
                                       braveType, nativeDragoonRole);
        attacker.setMovesLeft(attacker.getInitialMovesLeft());
        defender.setStateUnchecked(UnitState.ACTIVE);

        tupi.setStance(dutch, Stance.WAR);
        dutch.setStance(tupi, Stance.WAR);

        assertTrue(defender.getTile().getType().getDefenceModifiers().isEmpty());
        assertFalse(attacker.canAmbush(defender));

        // Without terrain modifiers the ambush bonus must not appear.
        Set<Modifier> offensiveModifiers = combatModel.getOffensiveModifiers(attacker, defender);
        for (Modifier modifier : offensiveModifiers) {
            assertFalse(Specification.AMBUSH_BONUS_SOURCE == modifier.getSource());
        }
    }

    // Goal: verify attacking a settlement stops native ambush even with ambush terrain.
    // Expected: ambush is not allowed and no ambush bonus modifier is generated.
    public void testNativeAmbushBlockedFromSettlement() {
        // Start a new game where Tupi attackers target a Dutch colony.
        Game game = ServerTestHelper.startServerGame(getTestMap(plains, true));

        ServerPlayer dutch = getServerPlayer(game, "model.nation.dutch");
        ServerPlayer tupi = getServerPlayer(game, "model.nation.tupi");

        SimpleCombatModel combatModel = new SimpleCombatModel();

        Map map = game.getMap();
        Tile colonyTile = map.getTile(5, 8);
        colonyTile.setType(hills);
        colonyTile.setExplored(dutch, true);
        colonyTile.setExplored(tupi, true);

        Tile attackerTile = map.getTile(4, 8);
        attackerTile.setType(plains);
        attackerTile.setExplored(dutch, true);
        attackerTile.setExplored(tupi, true);

        Colony colony = FreeColTestUtils.getColonyBuilder()
            .player(dutch).colonyTile(colonyTile).initialColonists(1)
            .build();

        Unit defender = new ServerUnit(game, colony, dutch,
                                       colonialRegularType, infantryRole);
        Unit attacker = new ServerUnit(game, attackerTile, tupi,
                                       braveType, nativeDragoonRole);
        attacker.setMovesLeft(attacker.getInitialMovesLeft());
        defender.setStateUnchecked(UnitState.ACTIVE);

        tupi.setStance(dutch, Stance.WAR);
        dutch.setStance(tupi, Stance.WAR);

        assertTrue(colonyTile.hasAbility(Ability.AMBUSH_TERRAIN));
        assertFalse(attacker.canAmbush(defender));

        // Units attacking a colony should not receive ambush modifiers.
        Set<Modifier> offensiveModifiers = combatModel.getOffensiveModifiers(attacker, defender);
        for (Modifier modifier : offensiveModifiers) {
            assertFalse(Specification.AMBUSH_BONUS_SOURCE == modifier.getSource());
        }
    }

    // Goal: check native ambush works against REF units standing on defensive terrain.
    // Expected: ambush values match the hill defence modifiers on the REF defender.
    public void testNativeAmbushAppliesAgainstREFUnit() {
        // Create a Dutch rebellion to spawn the REF and add in a Tupi attacker.
        Game game = ServerTestHelper.startServerGame(getTestMap(plains, true));
        InGameController igc = ServerTestHelper.getInGameController();

        ServerPlayer dutch = getServerPlayer(game, "model.nation.dutch");
        dutch.addAbility(new Ability(Ability.INDEPENDENCE_DECLARED));
        ServerPlayer refPlayer = igc.createREFPlayer(dutch);
        ServerPlayer tupi = getServerPlayer(game, "model.nation.tupi");

        SimpleCombatModel combatModel = new SimpleCombatModel();

        Map map = game.getMap();
        Tile defenderTile = map.getTile(5, 8);
        defenderTile.setType(hills);
        defenderTile.setExplored(tupi, true);
        defenderTile.setExplored(refPlayer, true);

        Tile attackerTile = map.getTile(4, 8);
        attackerTile.setType(plains);
        attackerTile.setExplored(tupi, true);
        attackerTile.setExplored(refPlayer, true);

        Unit defender = new ServerUnit(game, defenderTile, refPlayer,
                                       kingsRegularType, infantryRole);
        Unit attacker = new ServerUnit(game, attackerTile, tupi,
                                       braveType, nativeDragoonRole);
        attacker.setMovesLeft(attacker.getInitialMovesLeft());
        defender.setStateUnchecked(UnitState.ACTIVE);

        tupi.setStance(refPlayer, Stance.WAR);
        refPlayer.setStance(tupi, Stance.WAR);

        List<Modifier> hillDefenceModifiers = toList(hills.getDefenceModifiers());
        assertFalse(hillDefenceModifiers.isEmpty());
        assertTrue(attacker.hasAbility(Ability.AMBUSH_BONUS));
        assertTrue(attacker.canAmbush(defender));

        Set<Float> expectedValues = collectModifierValues(hillDefenceModifiers);
        Set<Float> ambushValues = collectAmbushValues(combatModel, attacker, defender);

        assertEquals(expectedValues.size(), ambushValues.size());
        assertEquals(expectedValues, ambushValues);
    }

    private Set<Float> collectModifierValues(List<Modifier> modifiers) {
        // Gather distinct modifier values to compare with ambush bonuses.
        Set<Float> values = new HashSet<>();
        for (Modifier modifier : modifiers) {
            values.add(modifier.getValue());
        }
        return values;
    }

    private Set<Float> collectAmbushValues(SimpleCombatModel combatModel,
                                           Unit attacker, Unit defender) {
        // Extract all ambush-related modifier values applied during combat.
        Set<Float> values = new HashSet<>();
        Set<Modifier> offensiveModifiers = combatModel.getOffensiveModifiers(attacker, defender);
        for (Modifier modifier : offensiveModifiers) {
            if (Specification.AMBUSH_BONUS_SOURCE == modifier.getSource()) {
                assertEquals(Modifier.OFFENCE, modifier.getId());
                values.add(modifier.getValue());
            }
        }
        return values;
    }
}
