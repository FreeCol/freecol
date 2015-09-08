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

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Colony.ColonyChangeEvent;
import net.sf.freecol.common.util.Utils;


/**
 * Helper container to remember a unit state prior to some change,
 * and fire off any consequent property changes.
 */
public class UnitWas implements Comparable<UnitWas> {

    private static final Logger logger = Logger.getLogger(UnitWas.class.getName());

    private final Unit unit;
    private final UnitType type;
    private final Role role;
    private final int roleCount;
    private final Location loc;
    private final GoodsType work;
    private final int workAmount;
    private final int movesLeft;
    private final List<Unit> units;
    private final Colony colony;


    /**
     * Record the state of a unit.
     *
     * @param unit The <code>Unit</code> to remember.
     */
    public UnitWas(Unit unit) {
        this.unit = unit;
        this.type = unit.getType();
        this.role = unit.getRole();
        this.roleCount = unit.getRoleCount();
        this.loc = unit.getLocation();
        this.work = unit.getWorkType();
        this.workAmount = getAmount(loc, work);
        this.movesLeft = unit.getMovesLeft();
        this.units = new ArrayList<>(unit.getUnitList());
        this.colony = unit.getColony();
        if (unit.getGoodsContainer() != null) {
            unit.getGoodsContainer().saveState();
        }
    }


    public Unit getUnit() {
        return unit;
    }

    public Location getLocation() {
        return loc;
    }

    public GoodsType getWorkType() {
        return work;
    }

    /**
     * Fire any property changes resulting from actions of a unit.
     *
     * @return True if something changed.
     */
    public boolean fireChanges() {
        UnitType newType = null;
        Role newRole = null;
        int newRoleCount = 0;
        Location newLoc = null;
        GoodsType newWork = null;
        int newWorkAmount = 0;
        int newMovesLeft = 0;
        boolean ret = false;
        if (!unit.isDisposed()) {
            newLoc = unit.getLocation();
            if (colony != null) {
                newType = unit.getType();
                newRole = unit.getRole();
                newRoleCount = unit.getRoleCount();
                newWork = unit.getWorkType();
                newWorkAmount = (newWork == null) ? 0
                    : getAmount(newLoc, newWork);
            }
            newMovesLeft = unit.getMovesLeft();
        }

        FreeColGameObject oldFcgo = (FreeColGameObject)loc;
        FreeColGameObject newFcgo = (FreeColGameObject)newLoc;
        if (loc != newLoc) {
            oldFcgo.firePropertyChange(change(oldFcgo), unit, null);
            if (newLoc != null) {
                newFcgo.firePropertyChange(change(newFcgo), null, unit);
            }
            ret = true;
        }
        if (colony != null) {
            if (type != newType && newType != null) {
                String pc = ColonyChangeEvent.UNIT_TYPE_CHANGE.toString();
                colony.firePropertyChange(pc, type, newType);
                ret = true;
            } else if (role != newRole && newRole != null) {
                String pc = Tile.UNIT_CHANGE;
                colony.firePropertyChange(pc, role.toString(),
                                          newRole.toString());
                ret = true;
            }
            if (work != newWork) {
                if (work != null && oldFcgo != null && workAmount != 0) {
                    oldFcgo.firePropertyChange(work.getId(), workAmount, 0);
                }
                if (newWork != null && newFcgo != null && newWorkAmount != 0) {
                    newFcgo.firePropertyChange(newWork.getId(),
                                               0, newWorkAmount);
                }
                ret = true;
            } else if (workAmount != newWorkAmount) {
                newFcgo.firePropertyChange(newWork.getId(),
                                           workAmount, newWorkAmount);
                ret = true;
            }
        }
        if (role != newRole && newRole != null) {
            unit.firePropertyChange(Unit.ROLE_CHANGE, role, newRole);
            ret = true;
        } else if (roleCount != newRoleCount && newRoleCount >= 0) {
            unit.firePropertyChange(Unit.ROLE_CHANGE, roleCount, newRoleCount);
            ret = true;
        }
        if (unit.getGoodsContainer() != null) {
            ret |= unit.getGoodsContainer().fireChanges();
        }
        if (!units.equals(unit.getUnitList())) {
            unit.firePropertyChange(Unit.CARGO_CHANGE, null, unit);
            ret = true;
        }
        if (movesLeft != newMovesLeft) {
            unit.firePropertyChange(Unit.MOVE_CHANGE, movesLeft, newMovesLeft);
            ret = true;
        }
        return ret;
    }

    // FIXME: fix this non-OO nastiness
    private String change(FreeColGameObject fcgo) {
        return (fcgo instanceof Tile) ? Tile.UNIT_CHANGE
            : (fcgo instanceof Europe) ? Europe.UNIT_CHANGE
            : (fcgo instanceof ColonyTile) ? ColonyTile.UNIT_CHANGE
            : (fcgo instanceof Building) ? Building.UNIT_CHANGE
            : (fcgo instanceof Unit) ? Unit.CARGO_CHANGE
            : null;
    }

    // FIXME: fix this non-OO nastiness
    private int getAmount(Location location, GoodsType goodsType) {
        if (goodsType == null) return 0;
        if (location instanceof WorkLocation) {
            ProductionInfo info = ((WorkLocation)location).getProductionInfo();
            return AbstractGoods.getCount(goodsType, info.getProduction());
        }
        return 0;
    }

    // Implement Comparable<UnitWas>

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(UnitWas uw) {
        // Order by decreasing capacity of the location the unit is to
        // be moved to, so that if we traverse a sorted list of
        // UnitWas we minimize the chance of a unit being moved to a
        // full location.
        //
        // Unfortunately this also tends to move units that need
        // equipment first, leading to failures to rearm, so it is
        // best to make two passes anyway.  See revertAll().  However
        // we can still try our best by using the amount of equipment
        // the unit needs as a secondary criterion (favouring the
        // least equipped).
        List<Role> roles = this.unit.getAvailableRoles(null);
        int cmp = ((UnitLocation)uw.loc).getUnitCapacity()
            - ((UnitLocation)this.loc).getUnitCapacity();
        if (cmp == 0) {
            cmp = roles.indexOf(this.role) - roles.indexOf(uw.role);
        }
        return cmp;
    }

    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof UnitWas) {
            return this.compareTo((UnitWas)other) == 0;
        }
        return super.equals(other);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 37 * hash + Utils.hashCode(unit);
        hash = 37 * hash + Utils.hashCode(type);
        hash = 37 * hash + Utils.hashCode(role);
        hash = 37 * hash + roleCount;
        hash = 37 * hash + Utils.hashCode(loc);
        hash = 37 * hash + Utils.hashCode(work);
        hash = 37 * hash + workAmount;
        hash = 37 * hash + movesLeft;
        return 37 * hash + Utils.hashCode(colony);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        Tile tile = colony.getTile();
        String roleStr = "/" + role.getSuffix();
        if (roleCount > 0) roleStr += "." + roleCount;
        String locStr = (loc == null) ? ""
            : (loc instanceof Building)
            ? ((Building)loc).getType().getSuffix()
            : (loc instanceof ColonyTile)
            ? tile.getDirection(((ColonyTile)loc).getWorkTile()).toString()
            : (loc instanceof Tile)
            ? (loc.getId() + roleStr)
            : loc.getId();
        Location newLoc = unit.getLocation();
        String newRoleStr = "/" + unit.getRole().getSuffix();
        if (unit.getRoleCount() > 0) newRoleStr += "." + unit.getRoleCount();
        String newLocStr = (newLoc == null) ? ""
            : (newLoc instanceof Building)
            ? ((Building)newLoc).getType().getSuffix()
            : (newLoc instanceof ColonyTile)
            ? tile.getDirection(((ColonyTile)newLoc).getWorkTile()).toString()
            : (newLoc instanceof Tile)
            ? (newLoc.getId() + newRoleStr)
            : newLoc.getId();
        GoodsType newWork = unit.getWorkType();
        int newWorkAmount = (newWork == null) ? 0 : getAmount(newLoc, newWork);
        return String.format("%-30s %-25s -> %-25s",
            unit.getId() + ":" + unit.getType().getSuffix(),
            locStr + ((work == null || workAmount <= 0) ? "" : "("
                + Integer.toString(workAmount) + " " + work.getSuffix() + ")"),
            newLocStr + ((newWork == null || newWorkAmount <= 0) ? "" : "("
                + Integer.toString(newWorkAmount) + " "
                + newWork.getSuffix() + ")")).trim();
    }
}
