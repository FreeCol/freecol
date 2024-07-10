/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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

package net.sf.freecol.server.ai.colony;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import net.sf.freecol.common.model.BuildableType;

/**
 * The AI's plan for a colony. This class is both used for representing the final result
 * and for the current layout in the colony.
 */
public final class ColonyPlan {

    private final Set<BuildableType> buildables;
    private final List<TileImprovementPlan> tileImprovements;
    private final List<WorkerPlan> workers;
    
    
    public ColonyPlan(Set<BuildableType> buildables, List<TileImprovementPlan> tileImprovements, List<WorkerPlan> workers) {
        this.buildables = Objects.requireNonNull(buildables, "buildables");
        this.tileImprovements = Objects.requireNonNull(tileImprovements, "tileImprovements");
        this.workers = Objects.requireNonNull(workers, "workers");
    }


    public Set<BuildableType> getBuildables() {
        return buildables;
    }

    public List<TileImprovementPlan> getTileImprovements() {
        return tileImprovements;
    }

    public List<WorkerPlan> getWorkers() {
        return workers;
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("=== Workers        ===\n");
        for (WorkerPlan wp : workers) {
            sb.append(wp.toString());
            sb.append("\n");
        }
        sb.append("\n=== Improvements ===\n");
        for (TileImprovementPlan tip : tileImprovements) {
            sb.append(tip.toString());
            sb.append("\n");
        }
        sb.append("\n=== Buildables   ===\n");
        for (BuildableType b : buildables) {
            sb.append(Utils.compressIdForLogging(b.getId()));
            sb.append("\n");
        }
        return sb.toString();
    }
}
