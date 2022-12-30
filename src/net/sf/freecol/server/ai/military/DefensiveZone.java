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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.ai.AIColony;

/**
 * Represents the area around a colony that should be defended.
 */
public final class DefensiveZone {

    private final AIColony aiColony;
    private final Set<Unit> potentialEnemies = new HashSet<>();
    private boolean exposedLand = false;
    private boolean exposedWater = false;
    private final Set<DefensiveZone> neighbours = new HashSet<>();
    private boolean enemiesInNeighbour = false;  
    
    
    public DefensiveZone(AIColony aiColony) {
        this.aiColony = aiColony;
    }
    
    public AIColony getAiColony() {
        return aiColony;
    }
    
    public void setExposedWater(boolean exposedWater) {
        this.exposedWater = exposedWater;
    }
    
    public void setExposedLand(boolean exposedLand) {
        this.exposedLand = exposedLand;
    }
    
    public void setEnemiesInNeighbour(boolean enemiesInNeighbour) {
        this.enemiesInNeighbour = enemiesInNeighbour;
    }

    public void addAllPotentialEnemies(Set<Unit> units) {
        potentialEnemies.addAll(units);
    }
    
    public void addNeighbour(DefensiveZone defensiveZone) {
        neighbours.add(defensiveZone);
    }
    
    public boolean isExposed() {
        return exposedLand || exposedWater;
    }
    
    public boolean isExposedWater() {
        return exposedWater;
    }
    
    public boolean isExposedLand() {
        return exposedLand;
    }
    
    public boolean isEnemiesInNeighbour() {
        return enemiesInNeighbour;
    }
    
    public int getNumberOfEnemies() {
        return (int) potentialEnemies.stream()
                .filter(enemiesOnly())
                .count();
    }
    
    public Set<Unit> getEnemies() {
        return potentialEnemies.stream()
                .filter(enemiesOnly())
                .collect(Collectors.toSet());
    }
    
    public int getNumberOfMilitaryEnemies() {
        return (int) potentialEnemies.stream()
                .filter(enemiesOnly())
                .filter(u -> u.isOffensiveUnit())
                .count();
    }
    
    public int getNumberOfPotentialMilitaryEnemies() {
        return (int) potentialEnemies.stream()
                /*
                 * TODO: This filter should be added after we have a system of trust
                 *       built in, where all AI players ally themselves (and attack)
                 *       if the human player breaks the peace too often.
                 *
                .filter(u -> getPlayer().getStance(u.getOwner()) == Stance.WAR
                        || getPlayer().getStance(u.getOwner()) == Stance.CEASE_FIRE)
                */
                .filter(u -> u.isOffensiveUnit())
                .count();
    }

    private Player getPlayer() {
        return aiColony.getColony().getOwner();
    }
    
    /**
     * Gets adjacent defensive zones. The defensive zones are only considered
     * to be adjacent if they are connected by land.
     */
    public Set<DefensiveZone> getNeighbours() {
        return neighbours;
    }
    
    private Predicate<? super Unit> enemiesOnly() {
        return u -> getPlayer().atWarWith(u.getOwner());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(aiColony);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DefensiveZone other = (DefensiveZone) obj;
        return Objects.equals(aiColony, other.aiColony);
    }
}
