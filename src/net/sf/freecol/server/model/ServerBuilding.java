/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

import java.util.Random;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.control.ChangeSet;
import net.sf.freecol.server.control.ChangeSet.See;


/**
 * The server version of a building.
 */
public class ServerBuilding extends Building implements ServerModelObject {

    private static final Logger logger = Logger.getLogger(ServerBuilding.class.getName());


    /**
     * Trivial constructor required for all ServerModelObjects.
     */
    public ServerBuilding(Game game, String id) {
        super(game, id);
    }

    /**
     * Creates a new ServerBuilding.
     *
     * @param game The <code>Game</code> this object belongs to.
     * @param colony The colony in which this building is located.
     * @param type The type of building.
     */
    public ServerBuilding(Game game, Colony colony, BuildingType type) {
        super(game);
        setColony(colony);
        this.buildingType = type;
    }


    /**
     * New turn for this building.
     *
     * @param random A <code>Random</code> number source.
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csNewTurn(Random random, ChangeSet cs) {
        logger.finest("ServerBuilding.csNewTurn, for " + toString());
        BuildingType type = getType();
        ServerPlayer owner = (ServerPlayer) getColony().getOwner();

        if (canTeach()) {
            teach(cs, owner);
        }

        if (type.hasAbility("model.ability.repairUnits")) {
            repairUnits(cs);
        }

    }

    private void teach(ChangeSet cs, ServerPlayer owner) {
        for (Unit teacher : getUnitList()) {
            boolean teacherDirty = false;
            boolean studentDirty = false;
            Unit student = teacher.getStudent();

            if (student != null && student.getTeacher() != teacher) {
                // Sanitation, make sure we have the proper
                // teacher/student relation.
                logger.warning("Bogus teacher/student assignment.");
                teacher.setStudent(null);
                student = null;
                teacherDirty = true;
            }

            // Student may have changed
            if (student == null && csAssignStudent(teacher, cs)) {
                teacherDirty = true;
                studentDirty = true;
                student = teacher.getStudent();
            }

            // Ready to train?
            if (student != null) {
                final int training = teacher.getTurnsOfTraining() + 1;
                if (training < teacher.getNeededTurnsOfTraining()) {
                    teacher.setTurnsOfTraining(training);
                    if (!teacherDirty) {
                        cs.addPartial(See.only(owner), teacher,
                                      "turnsOfTraining");
                    }
                } else {
                    StringTemplate oldName = student.getLabel();
                    UnitType teach = teacher.getType().getSkillTaught();
                    UnitType skill = Unit
                        .getUnitTypeTeaching(teach, student.getType());
                    if (skill == null) {
                        logger.warning("Student " + student.getId()
                                       + " can not learn from "
                                       + teacher.getId());
                    } else {
                        student.setType(skill);
                        StringTemplate newName = student.getLabel();
                        cs.addMessage(See.only(owner),
                                      new ModelMessage(ModelMessage.MessageType.UNIT_IMPROVED,
                                                       "model.unit.unitEducated",
                                                       getColony(), this)
                                      .addStringTemplate("%oldName%", oldName)
                                      .addStringTemplate("%unit%", newName)
                                      .addName("%colony%", getColony().getName()));
                    }
                    student.setTurnsOfTraining(0);
                    student.setMovesLeft(0);
                    cs.add(See.only(owner), student);
                    studentDirty = false;

                    teacher.setTurnsOfTraining(0);
                    if (student.canBeStudent(teacher)) { // Keep teaching
                        cs.addPartial(See.only(owner), teacher,
                                      "turnsOfTraining");
                    } else {
                        student.setTeacher(null);
                        teacher.setStudent(null);
                        teacherDirty = true;
                        if (csAssignStudent(teacher, cs)) {
                            student = teacher.getStudent();
                            studentDirty = true;
                        }
                    }
                }
            }

            if (teacherDirty) cs.add(See.only(owner), teacher);
            if (studentDirty) cs.add(See.only(owner), student);
        }
    }


    /**
     * Assigns a student to a teacher within a building.
     *
     * @param teacher The <code>Unit</code> that is teaching.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private boolean csAssignStudent(Unit teacher, ChangeSet cs) {
        Colony colony = getColony();
        final Unit student = colony.findStudent(teacher);
        if (student == null) {
            cs.addMessage(See.only((ServerPlayer) colony.getOwner()),
                new ModelMessage(ModelMessage.MessageType.WARNING,
                                 "model.building.noStudent",
                                 colony, teacher)
                          .addStringTemplate("%teacher%", teacher.getLabel())
                          .addName("%colony%", colony.getName()));
            return false;
        }
        teacher.setStudent(student);
        teacher.setWorkType(null);
        student.setTeacher(teacher);
        return true;
    }

    private void repairUnits(ChangeSet cs) {
        for (Unit unit : getTile().getUnitList()) {
            if (unit.isUnderRepair()
                && getType().hasAbility("model.ability.repairUnits",
                                        unit.getType())) {
                ((ServerUnit) unit).csRepairUnit(cs);
            }
        }
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "serverBuilding"
     */
    public String getServerXMLElementTagName() {
        return "serverBuilding";
    }
}
