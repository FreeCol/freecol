/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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

package net.sf.freecol.server.ai.military;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.ai.AIColony;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.ai.EuropeanAIPlayer;
import net.sf.freecol.server.ai.mission.DefendSettlementMission;
import net.sf.freecol.server.ai.mission.Mission;
import net.sf.freecol.server.ai.mission.UnitSeekAndDestroyMission;
import net.sf.freecol.server.ai.mission.UnitWanderHostileMission;

/**
 * Provides missions for all military (land) units.
 */
public final class MilitaryCoordinator {

    private final EuropeanAIPlayer player;
    private final Set<AIUnit> unusedUnits;
    private final List<AIColony> ourColonies;
    private final Map<AIColony, List<AIUnit>> defenders;
    private final DefensiveMap defensiveMap;
    
    private final Set<Unit> targetedEnemies = Collections.newSetFromMap(new IdentityHashMap<>());
    
    /**
     * Creates a new military coordinator for the given military units.
     * 
     * Please note that a new instance needs to be created every time the map has
     * changed (for example, by executing other missions).
     *  
     * @param player The player owning the units to be coordinated.
     * @param militaryUnits The units to be managed by this instance.
     */
    public MilitaryCoordinator(EuropeanAIPlayer player, Set<AIUnit> militaryUnits) {
        assert player != null;
        assert militaryUnits.stream().noneMatch(au -> au.getOwner() != player.getPlayer());
        
        this.player = player;
        this.unusedUnits = identitySet(militaryUnits);
        
        this.ourColonies = getOurColoniesSortedByValue();
        this.defenders = new HashMap<>();
        
        for (AIColony colony : ourColonies) {
            this.defenders.put(colony, new ArrayList<>());
        }
        
        defensiveMap = DefensiveMap.createDefensiveMap(player);
    }
    
    
    /**
     * Determines the missions for the units this coordinator controls.
     */
    public void determineMissions() {
        for (AIUnit au : unusedUnits) {
            au.setMission(null);
        }
        
        final Set<AIUnit> artilleryUnits = identitySet(onlyArtillery(unusedUnits));
        final Set<AIUnit> dragoonUnits = identitySet(onlyDragoons(unusedUnits));
        final Set<AIUnit> otherMilitaryUnits = identitySet(neitherArtilleryNorDragoons(unusedUnits));
        
        /*
         * The order here is really important, since military units might run out.
         */
        
        // Do not move away artillery from attacked zones:
        keepUnitsInColonies(defensiveMap.getAttackedColonies(), artilleryUnits, always());
        
        // Keep at least one decent defender in every colony:
        keepUnitsInColonies(defensiveMap.getAttackedColonies(), dragoonUnits, maxDefenders(1));
        keepUnitsInColonies(ourColonies, artilleryUnits, maxDefenders(1));
        placeUnitsInColonies(ourColonies, artilleryUnits, maxDefenders(1));
        keepUnitsInColonies(ourColonies, dragoonUnits, maxDefenders(1));
        placeUnitsInColonies(ourColonies, dragoonUnits, maxDefenders(1));
        
        // Don't move unmounted soldiers out of the colonies:
        keepUnitsInColonies(ourColonies, otherMilitaryUnits, always());
        
        keepUnitsInColonies(defensiveMap.getThreatenedColonies(), artilleryUnits, maxArtilleries(3));
        keepUnitsInColonies(defensiveMap.getColoniesExposedWater(), artilleryUnits, maxArtilleries(2));
        placeUnitsInColonies(defensiveMap.getThreatenedColonies(), artilleryUnits, maxArtilleries(2));
               
        counterattackEnemyValuableUnitsReachableInTurns(dragoonUnits, 0);
        counterattackEnemyValuableUnitsReachableInTurns(dragoonUnits, 1);
        counterattackAllEnemyUnitsReachableInTurns(dragoonUnits, 1);
        counterattackAllEnemyUnitsReachableInTurns(dragoonUnits, 2);
        
        // TODO: Here's the place for attacking enemy colonies etc.
        
        keepUnitsInColonies(defensiveMap.getAttackedColonies(), dragoonUnits, always());
        keepUnitsInColonies(defensiveMap.getThreatenedColonies(), artilleryUnits, always());
        placeUnitsInColonies(defensiveMap.getColoniesExposedWater(), artilleryUnits, maxArtilleries(2));
        keepUnitsInColonies(defensiveMap.getThreatenedColonies(), dragoonUnits, always());
        
        keepUnitsInColonies(defensiveMap.getColoniesExposedLand(), artilleryUnits, maxArtilleries(2));
        placeUnitsInColonies(defensiveMap.getColoniesExposedLand(), dragoonUnits, maxDragoons(2));
        
        assignDefendClosestColony(otherMilitaryUnits);
        assignWanderHostile();
    }

    private void counterattackEnemyValuableUnitsReachableInTurns(final Set<AIUnit> dragoonUnits, int turns) {
        for (DefensiveZone defensiveZone : defensiveMap.getAttackedDefensiveZones()) {
            final Set<Unit> unprotectedUnarmedSoldiers = identitySet(onlyUnprotectedUnarmedSoldierUnits(defensiveZone.getEnemies()));
            final Set<Unit> enemyArtillery = identitySet(onlyArtilleryUnits(defensiveZone.getEnemies()));
            
            for (Unit enemy : unprotectedUnarmedSoldiers) {
                for (AIUnit dragoon : new HashSet<>(dragoonUnits)) {
                    if (dragoon.getUnit().getTile() == null) {
                        continue;
                    }
                    if (dragoon.getUnit().getTile().getContiguity() != enemy.getTile().getContiguity()) {
                        continue;
                    }
                    
                    final PathNode path = dragoon.getUnit().findPath(enemy.getTile()); // TODO: add max turns to the search.
                    if (path != null & path.getTurns() <= turns) {
                        dragoon.setMission(new UnitSeekAndDestroyMission(dragoon.getAIMain(), dragoon, enemy));
                        unusedUnits.remove(dragoon);
                        dragoonUnits.remove(dragoon);
                        targetedEnemies.add(enemy);
                        break;
                    }
                }
            }
            
            for (int i=0; i<2; i++) { // Two per artillery, regardless of defenders.
                for (Unit enemy : enemyArtillery) {
                    for (AIUnit dragoon : new HashSet<>(dragoonUnits)) {
                        if (dragoon.getUnit().getTile() == null) {
                            continue;
                        }
                        if (dragoon.getUnit().getTile().getContiguity() != enemy.getTile().getContiguity()) {
                            continue;
                        }
                        
                        final PathNode path = dragoon.getUnit().findPath(enemy.getTile()); // TODO: add max turns to the search.
                        if (path != null & path.getTurns() <= turns) {
                            dragoon.setMission(new UnitSeekAndDestroyMission(dragoon.getAIMain(), dragoon, enemy));
                            unusedUnits.remove(dragoon);
                            dragoonUnits.remove(dragoon);
                            targetedEnemies.add(enemy);
                            break;
                        }
                    }
                }
            }
        }
    }
    
    private void counterattackAllEnemyUnitsReachableInTurns(final Set<AIUnit> dragoonUnits, int turns) {
        for (DefensiveZone defensiveZone : defensiveMap.getAttackedDefensiveZones()) { 
            for (Unit enemy : defensiveZone.getEnemies()) {
                if (targetedEnemies.contains(enemy)) {
                    continue;
                }
                for (AIUnit dragoon : new HashSet<>(dragoonUnits)) {
                    if (dragoon.getUnit().getTile() == null) {
                        continue;
                    }
                    if (dragoon.getUnit().getTile().getContiguity() != enemy.getTile().getContiguity()) {
                        continue;
                    }
                    
                    final PathNode path = dragoon.getUnit().findPath(enemy.getTile()); // TODO: add max turns to the search.
                    if (path != null & path.getTurns() <= turns) {
                        dragoon.setMission(new UnitSeekAndDestroyMission(dragoon.getAIMain(), dragoon, enemy));
                        unusedUnits.remove(dragoon);
                        dragoonUnits.remove(dragoon);
                        targetedEnemies.add(enemy);
                        break;
                    }
                }
            }
        }
    }
    
    private void assignDefendClosestColony(Set<AIUnit> otherMilitaryUnits) {
        for (AIUnit unit : new HashSet<>(unusedUnits)) {
            final Mission mission = player.getDefendSettlementMission(unit, true, true);
            if (mission != null) {
                unit.setMission(mission);
                unusedUnits.remove(unit);
            }
        }
    }
    
    private void placeUnit(List<AIColony> aiColonies, Set<AIUnit> units, Function<List<AIUnit>, Boolean> checkIfDefenderShouldBeAdded, boolean onlySameTile) {
        for (AIColony colony : aiColonies) {
            if (!checkIfDefenderShouldBeAdded.apply(defenders.get(colony))) {
                continue;
            }
            final AIUnit unit;
            if (onlySameTile) {
                unit = findUnitInColony(colony, units);
            } else {
                unit = findUnitClosestToColony(colony, units);
            }
            if (unit == null) {
                break;
            }
            placeDefender(unit, colony);
            units.remove(unit);
        }
    }
    
    private void keepUnitsInColonies(List<AIColony> aiColonies, Set<AIUnit> units, Function<List<AIUnit>, Boolean> checkIfDefenderShouldBeAdded) {
        placeUnit(aiColonies, units, checkIfDefenderShouldBeAdded, true);
    }
    
    private void placeUnitsInColonies(List<AIColony> aiColonies, Set<AIUnit> units, Function<List<AIUnit>, Boolean> checkIfDefenderShouldBeAdded) {
        placeUnit(aiColonies, units, checkIfDefenderShouldBeAdded, false);
    }
    
    private void assignWanderHostile() {
        for (AIUnit unit : new HashSet<>(unusedUnits)) {
            unit.setMission(new UnitWanderHostileMission(unit.getAIMain(), unit));
            unusedUnits.remove(unit);
        }
        assert unusedUnits.isEmpty();
    }
    
    private void placeDefender(AIUnit unit, AIColony colony) {
        unit.setMission(new DefendSettlementMission(unit.getAIMain(), unit, colony.getColony()));
        final List<AIUnit> units = defenders.get(colony);
        units.add(unit);
        unusedUnits.remove(unit);
    }

    private static AIUnit findUnitClosestToColony(AIColony colony, Set<AIUnit> units) {
        // TODO: Handle Europe, handle with/without carrier
        return units.stream()
            .sorted((a, b) -> Integer.compare(a.getUnit().getTurnsToReach(colony.getColony().getTile()), b.getUnit().getTurnsToReach(colony.getColony().getTile())))
            .findFirst()
            .orElse(null);
    }

    private static AIUnit findUnitInColony(AIColony ac, Set<AIUnit> units) {
        return units.stream()
            .filter(au -> au.getUnit().getTile() != null && au.getUnit().getTile().equals(ac.getColony().getTile()))
            .findAny()
            .orElse(null);
    }

    private static Set<AIUnit> onlyArtillery(Set<AIUnit> militaryUnits) {
        return militaryUnits.stream()
                .filter(au -> isArtillery(au.getUnit()))
                .collect(Collectors.toSet());
    }
    
    private static Set<Unit> onlyArtilleryUnits(Set<Unit> militaryUnits) {
        return militaryUnits.stream()
                .filter(u -> isArtillery(u))
                .collect(Collectors.toSet());
    }

    private static boolean isArtillery(Unit unit) {
        return unit.hasAbility(Ability.BOMBARD);
    }
    
    private static Set<Unit> onlyUnprotectedUnarmedSoldierUnits(Set<Unit> militaryUnits) {
        return militaryUnits.stream()
                .filter(u -> !u.isArmed() && u.getSortedMilitaryRoles().stream().anyMatch(r -> r.getExpertUnit() == u.getType()))
                .filter(u -> militaryUnits.stream().anyMatch(guard -> guard.getTile() == u.getTile() && guard.isOffensiveUnit()))
                .collect(Collectors.toSet());
    }
    
    private static Set<AIUnit> onlyDragoons(Set<AIUnit> militaryUnits) {
        return militaryUnits.stream()
                .filter(au -> isDragoon(au.getUnit()))
                .collect(Collectors.toSet());
    }

    private static boolean isDragoon(Unit unit) {
        return unit.isMounted();
    }

    private static Set<AIUnit> neitherArtilleryNorDragoons(Set<AIUnit> militaryUnits) {
        return militaryUnits.stream()
                .filter(au -> !au.getUnit().hasAbility(Ability.BOMBARD) && !au.getUnit().isMounted())
                .collect(Collectors.toSet());
    }

    private List<AIColony> getOurColoniesSortedByValue() {
        final List<AIColony> ourColonies = player.getAIColonies();
        Collections.sort(ourColonies, (a, b) -> {
            return Integer.compare(b.getColony().getUnitCount(), a.getColony().getUnitCount());
        });
        return ourColonies;
    }
    
    private static <T> Set<T> identitySet(Collection<T> collection) {
        final Set<T> result = Collections.newSetFromMap(new IdentityHashMap<>());
        result.addAll(collection);
        return result;
    }
    
    private static Function<List<AIUnit>, Boolean> maxDefenders(int count) {
        return units -> units.size() < count;
    }
    
    private static Function<List<AIUnit>, Boolean> maxArtilleries(int count) {
        return units -> units.stream().filter(au -> isArtillery(au.getUnit())).count() < count;
    }
    
    private static Function<List<AIUnit>, Boolean> maxDragoons(int count) {
        return units -> units.stream().filter(au -> isDragoon(au.getUnit())).count() < count;
    }
    
    private static Function<List<AIUnit>, Boolean> always() {
        return units -> true;
    }
}
