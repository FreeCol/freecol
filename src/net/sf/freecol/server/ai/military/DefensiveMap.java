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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;

import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.pathfinding.CostDecider;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.server.ai.AIColony;
import net.sf.freecol.server.ai.EuropeanAIPlayer;

/**
 * A tactical map of the areas the AI should defend.
 * 
 * @see #createDefensiveMap(EuropeanAIPlayer)
 */
public class DefensiveMap {
    
    private static final int ZONE_SIZE_TURNS = 3;
    
    private final Map<AIColony, DefensiveZone> defensiveZones;
    private Map<String, DefensiveZone> tileDefensiveZone;
    
    
    private DefensiveMap(Map<AIColony, DefensiveZone> defensiveZones, Map<String, DefensiveZone> tileDefensiveZone) {
        this.defensiveZones = defensiveZones;
        this.tileDefensiveZone = tileDefensiveZone;
    }
    
    
    /**
     * Returns the defensive zone a tile is considered a part of.
     * 
     * @param tile The {@code Tile} to obtain the {@code DefensiveZone} for.
     * @return The {@code DefensiveZone}, or {@code null} if the {@code Tile}
     *      is not part of a defensive zone.
     */
    public DefensiveZone getDefensiveZone(Tile tile) {
        return tileDefensiveZone.get(tile.getId());
    }
    
    
    /**
     * Returns all the defensive zones.
     */
    public List<DefensiveZone> getDefensiveZones() {
        return new ArrayList<>(defensiveZones.values());
    }
    
    /**
     * Gets the defensive zones that are considered to be under attack.
     */
    public List<DefensiveZone> getAttackedDefensiveZones() {
        return defensiveZones.values().stream()
                .filter(dz -> dz.getNumberOfMilitaryEnemies() > 0)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets the colonies of defensive zones that are considered to
     * be under attack.
     */
    public List<AIColony> getAttackedColonies() {
        return getAttackedDefensiveZones().stream()
                .map(DefensiveZone::getAiColony)
                .collect(Collectors.toList());
    }
    
    /**
     * Returns colonies for defensive zones that are close to potential
     * enemy military units. 
     */
    public List<AIColony> getThreatenedColonies() {
        return defensiveZones.values().stream()
                .filter(dz -> dz.isEnemiesInNeighbour()
                        || dz.getNumberOfPotentialMilitaryEnemies() > 0)
                .map(DefensiveZone::getAiColony)
                .collect(Collectors.toList());
    }
    
    /**
     * Returns a list of colonies that have land areas not protected by
     * other defensive zones.
     */
    public List<AIColony> getColoniesExposedLand() {
        return defensiveZones.values().stream()
                .filter(dz -> dz.isExposedLand())
                .map(DefensiveZone::getAiColony)
                .collect(Collectors.toList());
    }
    
    /**
     * Returns a list of colonies that have defensive zones adjacent to water.
     */
    public List<AIColony> getColoniesExposedWater() {
        return defensiveZones.values().stream()
                .filter(dz -> dz.isExposedWater())
                .map(DefensiveZone::getAiColony)
                .collect(Collectors.toList());
    }

    /**
     * Creates a defensive map for the given AI player.
     * 
     * The defensive map is not updated automatically, and need to be
     * recreated if an update is required.
     *
     * @param aiPlayer The player to create the defensive map for.
     * @return A defensive map that contains information about how easy
     *      an area can be defended, and possible threats.
     */
    public static DefensiveMap createDefensiveMap(EuropeanAIPlayer aiPlayer) {
        final List<AIColony> colonies = aiPlayer.getAIColonies();
        
        if (colonies.isEmpty()) {
            return new DefensiveMap(new HashMap<>(), new HashMap<>());
        }
        
        final Map<AIColony, DefensiveZone> defensiveZones = new HashMap<>();
        final Map<String, DefensiveZone> tileDefensiveZone = new HashMap<>();
        
        final PriorityQueue<SearchNode> openMapQueue = new PriorityQueue<>(1024,
                Comparator.comparingInt(SearchNode::getCost));
        final Map<String, SearchNode> openMap = new HashMap<>();
        final Map<String, SearchNode> closedMap = new HashMap<>();
        final int movesLeft = 1;
        
        for (AIColony ac : colonies) {
            final DefensiveZone defensiveZone = new DefensiveZone(ac);
            defensiveZones.put(ac, defensiveZone);
            openMapQueue.add(new SearchNode(ac.getColony().getTile(), null, movesLeft, 0, defensiveZone, 0));
        }
        
        final CostDecider costDecider = CostDeciders.numberOfTiles();
        while (!openMapQueue.isEmpty()) {
            final SearchNode searchNode = openMapQueue.poll();
            closedMap.put(searchNode.getTile().getId(), searchNode);
            tileDefensiveZone.put(searchNode.getTile().getId(), searchNode.defensiveZone);
            
            if (searchNode.getTile().hasSettlement()) {
                final Settlement settlement = searchNode.getTile().getSettlement();
                if (aiPlayer.getPlayer().getStance(settlement.getOwner()) != Stance.ALLIANCE) {
                    searchNode.defensiveZone.addPotentialEnemySettlement(settlement);
                }
            } else {
                final Set<Unit> enemyUnits = searchNode.getTile()
                        .getUnits()
                        .filter(u -> !aiPlayer.getPlayer().equals(u.getOwner())
                                && aiPlayer.getPlayer().getStance(u.getOwner()) != Stance.ALLIANCE)
                        .collect(Collectors.toSet());
                searchNode.defensiveZone.addAllPotentialEnemies(enemyUnits);
            }
            
            for (Tile tile : searchNode.getTile().getSurroundingTiles(1)) {
                if (!tile.isLand()) {
                    if (searchNode.previous != null) {
                        searchNode.previous.defensiveZone.setExposedWater(true);
                    }
                    continue;
                }
                
                final SearchNode closedMatch = closedMap.get(tile.getId());
                if (closedMatch != null) {
                    if (searchNode.defensiveZone != closedMatch.defensiveZone) {
                        closedMatch.defensiveZone.addNeighbour(searchNode.defensiveZone);
                    }
                    continue;
                }
                
                final int cost = costDecider.getCost(null, searchNode.getTile(), tile, searchNode.movesLeft);
                
                final SearchNode newSearchNode = new SearchNode(tile,
                        searchNode,
                        costDecider.getMovesLeft(),
                        searchNode.turns + costDecider.getNewTurns(),
                        searchNode.defensiveZone,
                        searchNode.getCost() + cost);
                
                if (openMap.containsKey(tile.getId())) {
                    continue;
                }
                
                if (newSearchNode.cost > ZONE_SIZE_TURNS) {
                    searchNode.defensiveZone.setExposedLand(true);
                    continue;
                }
                
                openMapQueue.add(newSearchNode);
                openMap.put(tile.getId(), newSearchNode);
            }
        }

        for (DefensiveZone defensiveZone : defensiveZones.values()) {
            for (DefensiveZone neighbour : defensiveZone.getNeighbours()) {
                if (neighbour.getNumberOfMilitaryEnemies() > 0) {
                    defensiveZone.setEnemiesInNeighbour(true);
                    break;
                }
            }
        }
        
        return new DefensiveMap(defensiveZones, tileDefensiveZone);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (Entry<AIColony, DefensiveZone> entry : defensiveZones.entrySet()) {
            sb.append(entry.getKey().getColony().getName());
            sb.append(": exposedLand=" + entry.getValue().isExposedLand());
            sb.append(": exposedWater=" + entry.getValue().isExposedWater());
            sb.append(" enemies=" + entry.getValue().getNumberOfMilitaryEnemies() + "\n");
        }
        return sb.toString();
    }
    
    
    private static final class SearchNode {
        private final Tile tile;
        private final SearchNode previous;
        private final int turns;
        private final int movesLeft;
        private final DefensiveZone defensiveZone;
        private final int cost;
        
        public SearchNode(Tile tile, SearchNode previous, int movesLeft, int turns, DefensiveZone defensiveZone, int cost) {
            this.tile = Objects.requireNonNull(tile);
            this.previous = previous;
            this.turns = turns;
            this.movesLeft = movesLeft;
            this.defensiveZone = Objects.requireNonNull(defensiveZone);
            this.cost = cost;
        }
        
        int getCost() {
            return cost;
        }
        
        Tile getTile() {
            return tile;
        }
    }

   
}

    