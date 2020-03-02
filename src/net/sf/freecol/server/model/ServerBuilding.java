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

package net.sf.freecol.server.model;

import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.ProductionInfo;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.networking.ChangeSet;
import net.sf.freecol.common.networking.ChangeSet.See;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.LogBuilder;


/**
 * The server version of a building.
 */
public class ServerBuilding extends Building implements TurnTaker {

    private static final Logger logger = Logger.getLogger(ServerBuilding.class.getName());


    /**
     * Trivial constructor for Game.newInstance.
     *
     * @param game The {@code Game} this object belongs to.
     * @param id The object identifier.
     */
    public ServerBuilding(Game game, String id) {
        super(game, id);
    }

    /**
     * Creates a new ServerBuilding.
     *
     * @param game The {@code Game} this object belongs to.
     * @param colony The {@code Colony} in which this building is located.
     * @param type The {@code BuildingType} of building.
     */
    public ServerBuilding(Game game, Colony colony, BuildingType type) {
        super(game, colony, type);
    }


    /**
     * Teach all the units in this school.
     *
     * @param cs A {@code ChangeSet} to update.
     */
    private void csTeach(ChangeSet cs) {
        final Player owner = getColony().getOwner();
        
        for (Unit teacher : getUnitList()) {
            Unit student = teacher.getStudent();

            if (student != null && student.getTeacher() != teacher) {
                // Sanitation, make sure we have the proper
                // teacher/student relation.
                logger.warning("Bogus teacher/student assignment.");
                teacher.setStudent(null);
                student = null;
            }

            // Student may have changed
            if (student == null && csAssignStudent(teacher, cs)) {
                student = teacher.getStudent();
            }

            // Update teaching amount.
            teacher.setTurnsOfTraining((student == null) ? 0
                : teacher.getTurnsOfTraining() + 1);
            cs.add(See.only(owner), teacher);

            // Do not check for completed training, see csCheckTeach below.
        }
    }

    /**
     * Check and complete teaching if possible.
     *
     * This needs to be separate and public because of the recheck of
     * teaching required if the colony production bonus rises at end
     * of new turn calculations.
     *
     * @param teacher The teaching {@code Unit}.
     * @param cs A {@code ChangeSet} to update.
     * @return True if teaching occurred.
     */
    public boolean csCheckTeach(Unit teacher, ChangeSet cs) {
        final Player owner = getColony().getOwner();

        Unit student = teacher.getStudent();
        if (student != null
            && teacher.getTurnsOfTraining()
                >= teacher.getNeededTurnsOfTraining()) {
            csTrainStudent(teacher, student, cs);
            // Student will have changed, teacher already added in csTeach
            cs.add(See.only(owner), student);
            if (teacher.getStudent() == null) csAssignStudent(teacher, cs);
            return true;
        }
        return false;
    }
        
    /**
     * Train a student.
     *
     * @param teacher The teacher {@code Unit}.
     * @param student The student {@code Unit} to train.
     * @param cs A {@code ChangeSet} to update.
     * @return True if teaching occurred.
     */
    private boolean csTrainStudent(Unit teacher, Unit student, ChangeSet cs) {
        final Player owner = getColony().getOwner();
        StringTemplate oldName = student.getLabel();
        UnitType skill = student.getTeachingType(teacher);
        boolean ret = skill != null;
        if (skill == null) {
            logger.warning("Student " + student.getId()
                           + " can not learn from " + teacher.getId());
        } else {
            student.changeType(skill);//-vis: safe within colony
            StringTemplate newName = student.getLabel();
            cs.addMessage(owner,
                new ModelMessage(ModelMessage.MessageType.UNIT_IMPROVED,
                                 "model.building.unitEducated",
                                 getColony(), this)
                    .addStringTemplate("%oldName%", oldName)
                    .addStringTemplate("%unit%", newName)
                    .addName("%colony%", getColony().getName()));
        }
        student.setTurnsOfTraining(0);
        student.setMovesLeft(0);
        teacher.setTurnsOfTraining(0);
        teacher.setMovesLeft(0);
        if (!student.canBeStudent(teacher)) {
            student.setTeacher(null);
            teacher.setStudent(null);
        }
        return ret;
    }

    /**
     * Assigns a student to a teacher within a building.
     *
     * @param teacher The {@code Unit} that is teaching.
     * @param cs A {@code ChangeSet} to update.
     * @return True if a student was assigned.
     */
    private boolean csAssignStudent(Unit teacher, ChangeSet cs) {
        final Colony colony = getColony();
        final Player owner = colony.getOwner();
        final Unit student = colony.findStudent(teacher);
        if (student == null) {
            cs.addMessage(owner,
                new ModelMessage(ModelMessage.MessageType.WARNING,
                                 "model.building.noStudent", colony, teacher)
                    .addStringTemplate("%teacher%", teacher.getLabel())
                    .addName("%colony%", colony.getName()));
            return false;
        }
        teacher.setStudent(student);
        teacher.changeWorkType(null);
        student.setTeacher(teacher);
        cs.add(See.only(owner), student);
        return true;
    }

    /**
     * Repair the units in this building.
     *
     * @param cs A {@code ChangeSet} to update.
     */
    private void csRepairUnits(ChangeSet cs) {
        for (Unit unit : transform(getTile().getUnits(), u ->
                (u.isDamaged()
                    && getType().hasAbility(Ability.REPAIR_UNITS, u.getType())))) {
            ((ServerUnit) unit).csRepairUnit(cs);
        }
    }

    /**
     * Check a building to see if it is missing input.
     *
     * The building must need input, have a person working there, and have
     * no production occurring.
     *
     * @param pi The {@code ProductionInfo} for the building.
     * @param cs A {@code ChangeSet} to update.
     */
    public void csCheckMissingInput(ProductionInfo pi, ChangeSet cs) {
        if (canAutoProduce()) return;
        List<AbstractGoods> production = pi.getProduction();
        if (!production.isEmpty()) {
            if (all(production, ag -> ag.getAmount() == 0)) {
                for (GoodsType gt : transform(getInputs(),
                                              ag -> ag.getAmount() > 0,
                                              AbstractGoods::getType)) {
                    cs.addMessage(getOwner(),
                        new ModelMessage(ModelMessage.MessageType.MISSING_GOODS,
                                         "model.building.noInput",
                                         this, gt)
                            .addNamed("%inputGoods%", gt)
                            .addNamed("%building%", this)
                            .addName("%colony%", getColony().getName()));
                }
            } else {
                for (AbstractGoods ag : pi.getConsumptionDeficit()) {
                    cs.addMessage(getOwner(),
                        new ModelMessage(ModelMessage.MessageType.MISSING_GOODS,
                                         "model.building.notEnoughInput",
                                         this, ag.getType())
                            .addNamed("%building%", this)
                            .addName("%colony%", getColony().getName())
                            .addAmount("%number%", ag.getAmount())
                            .addNamed("%goodsType%", ag.getType()));
                }
            }
        }
    }


    // Implement TurnTaker

    /**
     * New turn for this building.
     *
     * @param random A {@code Random} number source.
     * @param lb A {@code LogBuilder} to log to.
     * @param cs A {@code ChangeSet} to update.
     */
    @Override
    public void csNewTurn(Random random, LogBuilder lb, ChangeSet cs) {
        BuildingType type = getType();

        if (canTeach()) csTeach(cs);

        if (type.hasAbility(Ability.REPAIR_UNITS)) {
            csRepairUnits(cs);
        }
    }
}
