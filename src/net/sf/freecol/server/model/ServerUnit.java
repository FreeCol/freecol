/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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


package net.sf.freecol.server.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.util.Utils;
import net.sf.freecol.server.control.ChangeSet;
import net.sf.freecol.server.control.ChangeSet.ChangePriority;
import net.sf.freecol.server.control.ChangeSet.See;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerModelObject;


/**
 * Server version of a unit.
 *
 */
public class ServerUnit extends Unit implements ServerModelObject {

    private static final Logger logger = Logger.getLogger(ServerUnit.class.getName());


    /**
     * Trivial constructor required for all ServerModelObjects.
     */
    public ServerUnit(Game game, String id) {
        super(game, id);
    }

    /**
     * Creates a new ServerUnit.
     *
     * @param game The <code>Game</code> in which this unit belongs.
     * @param location The <code>Location</code> to place this at.
     * @param owner The <code>Player</code> owning this unit.
     * @param type The type of the unit.
     * @param state The initial state for this unit.
     * @param initialEquipment The list of initial EquimentTypes
     */
    public ServerUnit(Game game, Location location, Player owner,
                      UnitType type, UnitState state) {
        this(game, location, owner, type, state, type.getDefaultEquipment());
    }

    /**
     * Creates a new ServerUnit.
     *
     * @param game The <code>Game</code> in which this unit belongs.
     * @param location The <code>Location</code> to place this at.
     * @param owner The <code>Player</code> owning this unit.
     * @param type The type of the unit.
     * @param state The initial state for this unit.
     * @param initialEquipment The list of initial EquimentTypes
     */
    public ServerUnit(Game game, Location location, Player owner,
                      UnitType type, UnitState state,
                      EquipmentType... initialEquipment) {
        super(game);

        visibleGoodsCount = -1;

        if (type.canCarryGoods()) {
            goodsContainer = new GoodsContainer(game, this);
        }

        UnitType newType = type.getUnitTypeChange(ChangeType.CREATION, owner);
        unitType = (newType == null) ? type : newType;
        this.owner = owner;
        owner.getNationID();
        naval = unitType.hasAbility("model.ability.navalUnit");
        setLocation(location);

        workLeft = -1;
        workType = getSpecification().getGoodsFood().get(0);

        this.movesLeft = getInitialMovesLeft();
        hitpoints = unitType.getHitPoints();

        for (EquipmentType equipmentType : initialEquipment) {
            if (EquipmentType.NO_EQUIPMENT.equals(equipmentType)) {
                equipment.clear();
                break;
            }
            equipment.incrementCount(equipmentType, 1);
        }
        setRole();
        setStateUnchecked(state);

        owner.setUnit(this);
        owner.invalidateCanSeeTiles();
        owner.modifyScore(unitType.getScoreValue());
    }


    /**
     * New turn for this unit.
     *
     * @param random A <code>Random</code> number source.
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csNewTurn(Random random, ChangeSet cs) {
        logger.finest("ServerUnit.csNewTurn, for " + toString());
        ServerPlayer owner = (ServerPlayer) getOwner();
        Specification spec = getSpecification();
        Location loc = getLocation();
        boolean tileDirty = false;
        boolean unitDirty = false;

        // Check for experience-promotion.
        // TODO: magic 5000/200
        GoodsType produce;
        UnitType learn;
        if (loc instanceof WorkLocation
            && (produce = getWorkType()) != null
            && (learn = spec.getExpertForProducing(produce)) != null
            && learn != getType()
            && getType().canBeUpgraded(learn, ChangeType.EXPERIENCE)
            && (Utils.randomInt(logger, "Experience", random, 5000)
                < Math.min(getExperience(), 200))) {
            StringTemplate oldName = getLabel();
            setType(learn);
            cs.addMessage(See.only(owner),
                          new ModelMessage(ModelMessage.MessageType.UNIT_IMPROVED,
                                           "model.unit.experience",
                                           getColony(), this)
                          .addStringTemplate("%oldName%", oldName)
                          .addStringTemplate("%unit%", getLabel())
                          .addName("%colony%", getColony().getName()));
            logger.finest("Experience upgrade for unit " + getId()
                          + " to " + getType());
            unitDirty = true;
        }

        // Attrition
        if (loc instanceof Tile && ((Tile) loc).getSettlement() == null) {
            int attrition = getAttrition() + 1;
            setAttrition(attrition);
            if (attrition > getType().getMaximumAttrition()) {
                cs.addMessage(See.only(owner),
                    new ModelMessage(ModelMessage.MessageType.UNIT_LOST,
                                     "model.unit.attrition", this)
                              .addStringTemplate("%unit%", getLabel()));
                cs.addDispose(owner, loc, this);
            }
        } else {
            setAttrition(0);
        }

        setMovesLeft((isUnderRepair()) ? 0 : getInitialMovesLeft());

        if (getWorkLeft() > 0) {
            unitDirty = true;
            tileDirty = csDoAssignedWork(cs);
        }

        if (getState() == UnitState.SKIPPED) {
            setState(UnitState.ACTIVE);
            unitDirty = true;
        }

        if (tileDirty) {
            cs.add(See.perhaps(), getTile());
        } else if (unitDirty) {
            cs.add(See.perhaps(), this);
        } else {
            cs.addPartial(See.only(owner), this, "movesLeft");
        }
    }

    /**
     * The status of units that are currently working (for instance on
     * building a road, or fortifying themselves) is updated in this
     * method.
     *
     * @param cs A <code>ChangeSet</code> to update.
     * @return True if the tile under the unit needs an update.
     */
    private boolean csDoAssignedWork(ChangeSet cs) {
        ServerPlayer owner = (ServerPlayer) getOwner();

        switch (getState()) {
        case IMPROVING:
            // Has the improvement been completed already? Do nothing.
            TileImprovement ti = getWorkImprovement();
            if (ti.isComplete()) {
                setState(UnitState.ACTIVE);
                return false;
            }

            // Otherwise do work
            int amount = (getType().hasAbility("model.ability.expertPioneer"))
                ? 2 : 1;
            int turns = ti.getTurnsToComplete();
            if ((turns -= amount) < 0) turns = 0;
            ti.setTurnsToComplete(turns);
            setWorkLeft(turns);
            break;
        case TO_AMERICA:
            if (getOwner().isREF()) { // Shorter travel to America for the REF
                setWorkLeft(0);
                break;
            }
            // Fall through
        default:
            setWorkLeft(getWorkLeft() - 1);
            break;
        }

        if (getWorkLeft() == 0) {
            setWorkLeft(-1);
            switch (getState()) {
            case TO_EUROPE:
                logger.info(toString() + " arrives in Europe");
                if (getTradeRoute() != null) {
                    setMovesLeft(0);
                    setState(UnitState.ACTIVE);
                    return false;
                }
                Europe europe = owner.getEurope();
                cs.addMessage(See.only(owner),
                    new ModelMessage(ModelMessage.MessageType.DEFAULT,
                                     "model.unit.arriveInEurope",
                                     europe, this)
                              .add("%europe%", europe.getNameKey()));
                setState(UnitState.ACTIVE);
                break;
            case TO_AMERICA:
                logger.info(toString() + " arrives in America");
                getGame().getModelController().setToVacantEntryLocation(this);
                setState(UnitState.ACTIVE);
                break;
            case FORTIFYING:
                setState(UnitState.FORTIFIED);
                break;
            case IMPROVING: // Deliver goods if any
                GoodsType deliver = getWorkImprovement().getDeliverGoodsType();
                if (deliver != null) {
                    int amount = getTile().potential(deliver, getType())
                        * getWorkImprovement().getDeliverAmount();
                    if (getType().hasAbility("model.ability.expertPioneer")) {
                        amount *= 2;
                    }
                    Settlement settlement = getTile().getSettlement();
                    if (settlement != null
                        && (ServerPlayer) settlement.getOwner() == owner) {
                        settlement.addGoods(deliver, amount);
                    } else {
                        List<Settlement> adjacent = new ArrayList<Settlement>();
                        for (Tile t : getTile().getSurroundingTiles(1)) {
                            if (t.getSettlement() != null
                                && (ServerPlayer) t.getSettlement().getOwner()
                                == owner) {
                                adjacent.add(t.getSettlement());
                            }
                        }
                        if (adjacent.size() > 0) {
                            int deliverPerCity = amount / adjacent.size();
                            for (Settlement s : adjacent) {
                                s.addGoods(deliver, deliverPerCity);
                            }
                            // Add residue to first adjacent settlement.
                            adjacent.get(0).addGoods(deliver,
                                                     amount % adjacent.size());
                        }
                    }
                }

                // Finish up
                TileImprovement ti = getWorkImprovement();
                EquipmentType type = ti.getExpendedEquipmentType();
                changeEquipment(type, -ti.getExpendedAmount());
                for (Unit unit : getTile().getUnitList()) {
                    if (unit.getWorkImprovement() != null
                        && unit.getWorkImprovement().getType() == ti.getType()
                        && unit.getState() == UnitState.IMPROVING) {
                        unit.setWorkLeft(-1);
                        unit.setWorkImprovement(null);
                        unit.setState(UnitState.ACTIVE);
                        unit.setMovesLeft(0);
                    }
                }
                // TODO: make this more generic, currently assumes tools used
                EquipmentType tools = getSpecification()
                    .getEquipmentType("model.equipment.tools");
                if (type == tools && getEquipmentCount(tools) == 0) {
                    StringTemplate locName
                        = getLocation().getLocationNameFor(owner);
                    String messageId = (getType().getDefaultEquipmentType() == type)
                        ? getType() + ".noMoreTools"
                        : "model.unit.noMoreTools";
                    cs.addMessage(See.only(owner),
                        new ModelMessage(ModelMessage.MessageType.WARNING,
                                         messageId, this)
                                  .addStringTemplate("%unit%", getLabel())
                                  .addStringTemplate("%location%", locName));
                }
                return true;
            default:
                logger.warning("Unknown work completed, state=" + getState());
                setState(UnitState.ACTIVE);
                break;
            }
        }
        return false;
    }

    /**
     * Repair a unit.
     *
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csRepairUnit(ChangeSet cs) {
        ServerPlayer owner = (ServerPlayer) getOwner();
        setHitpoints(getHitpoints() + 1);
        if (!isUnderRepair()) {
            Location loc = getLocation();
            cs.addMessage(See.only(owner),
                new ModelMessage("model.unit.unitRepaired",
                                 this, (FreeColGameObject) loc)
                          .addStringTemplate("%unit%", getLabel())
                          .addStringTemplate("%repairLocation%",
                                             loc.getLocationNameFor(owner)));
        }
        cs.addPartial(See.only(owner), this, "hitpoints");
    }


    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "serverUnit"
     */
    public String getServerXMLElementTagName() {
        return "serverUnit";
    }
}
