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

package net.sf.freecol.common.model.production;

import net.sf.freecol.common.model.ProductionType;
import net.sf.freecol.common.model.UnitType;

public class WorkerAssignment {

    private final UnitType unitType;
    private final ProductionType productionType;
    
    
    public WorkerAssignment(UnitType unitType, ProductionType productionType) {
        this.unitType = unitType;
        this.productionType = productionType;
    }
    
    public UnitType getUnitType() {
        return unitType;
    }
    
    public ProductionType getProductionType() {
        return productionType;
    }
}
