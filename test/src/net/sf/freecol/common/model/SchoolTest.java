/**
 *  Copyright (C) 2002-2015  The FreeCol Team
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

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import net.sf.freecol.server.model.ServerBuilding;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.FreeColTestUtils;
import net.sf.freecol.util.test.FreeColTestUtils.ColonyBuilder;


public class SchoolTest extends FreeColTestCase {

    private enum SchoolLevel { SCHOOLHOUSE, COLLEGE, UNIVERSITY };

    private static final BuildingType schoolType
        = spec().getBuildingType("model.building.schoolhouse");
    private static final BuildingType collegeType
        = spec().getBuildingType("model.building.college");
    private static final BuildingType universityType
        = spec().getBuildingType("model.building.university");

    private static final GoodsType lumber
        = spec().getGoodsType("model.goods.lumber");
    private static final GoodsType cotton
        = spec().getGoodsType("model.goods.cotton");

    private static final UnitType colonialRegularType
        = spec().getUnitType("model.unit.colonialRegular");
    private static final UnitType elderStatesmanType
        = spec().getUnitType("model.unit.elderStatesman");
    private static final UnitType expertLumberJackType
        = spec().getUnitType("model.unit.expertLumberJack");
    private static final UnitType expertOreMinerType
        = spec().getUnitType("model.unit.expertOreMiner");
    private static final UnitType freeColonistType
        = spec().getUnitType("model.unit.freeColonist");
    private static final UnitType indenturedServantType
        = spec().getUnitType("model.unit.indenturedServant");
    private static final UnitType pettyCriminalType
        = spec().getUnitType("model.unit.pettyCriminal");
    private static final UnitType masterBlacksmithType
        = spec().getUnitType("model.unit.masterBlacksmith");
    private static final UnitType masterCarpenterType
        = spec().getUnitType("model.unit.masterCarpenter");
    private static final UnitType veteranSoldierType
        = spec().getUnitType("model.unit.veteranSoldier");


    private Building addSchoolToColony(Game game, Colony colony,
                                       SchoolLevel level) {
        BuildingType type = null;;
        switch (level) {
        case SCHOOLHOUSE:
            type = schoolType;
            break;
        case COLLEGE:
            type = collegeType;
            break;
        case UNIVERSITY:
            type = universityType;
            break;
        default:
            fail("Setup error, cannot setup school");
        }
        colony.addBuilding(new ServerBuilding(game, colony, type));
        return colony.getBuilding(type);
    }

    /**
     * Returns a list of all units in this colony of the given type.
     *
     * @param type The type of the units to include in the list. For instance
     *            Unit.EXPERT_FARMER.
     * @return A list of all the units of the given type in this colony.
     */
    private List<Unit> getUnitList(Colony colony, UnitType type) {
        return colony.getUnitList().stream()
            .filter(u -> u.getType() == type).collect(Collectors.toList());
    }

    public void testUpgrades() {
        assertEquals("Colonist should upgrade to carpenter",
            masterCarpenterType,
            Unit.getUnitTypeTeaching(masterCarpenterType, freeColonistType));
        assertEquals("Servant should upgrade to colonist",
            freeColonistType,
            Unit.getUnitTypeTeaching(masterCarpenterType, indenturedServantType));
        assertEquals("Criminal should upgrade to servant",
            indenturedServantType,
            Unit.getUnitTypeTeaching(masterCarpenterType, pettyCriminalType));
    }

    public void testEducationOption() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony(5);

        Iterator<Unit> units = colony.getUnitIterator();

        Unit lumberJack = units.next();
        lumberJack.setType(expertLumberJackType);
        Unit criminal1 = units.next();
        criminal1.setType(pettyCriminalType);
        Unit criminal2 = units.next();
        criminal2.setType(pettyCriminalType);
        Unit colonist1 = units.next();
        colonist1.setType(freeColonistType);
        Unit colonist2 = units.next();
        colonist2.setType(freeColonistType);

        boolean selection = FreeColTestUtils.setStudentSelection(true);

        colony.addBuilding(new ServerBuilding(getGame(), colony, schoolType));
        Building school = colony.getBuilding(schoolType);
        assertTrue(school.canTeach());
        assertTrue(colony.canTrain(lumberJack));
        lumberJack.setLocation(school);

        colonist1.changeWorkType(cotton);
        colonist2.changeWorkType(lumber);
        assertEquals(cotton, colonist1.getWorkType());
        assertEquals(expertLumberJackType.getExpertProduction(), colonist2.getWorkType());
        assertEquals(null, colony.findStudent(lumberJack));

        lumberJack.setStudent(null);
        colonist2.setTeacher(null);

        FreeColTestUtils.setStudentSelection(false);

        criminal1.changeWorkType(cotton);
        criminal2.changeWorkType(lumber);
        assertEquals(criminal2, colony.findStudent(lumberJack));

        FreeColTestUtils.setStudentSelection(selection);
    }

    public void testChangeTeachers() {
        Game game = getGame();
        game.setMap(getTestMap());

        // Setup
        ColonyBuilder colBuilder = FreeColTestUtils.getColonyBuilder();
        colBuilder.initialColonists(3).addColonist(expertLumberJackType)
            .addColonist(expertLumberJackType);
        Colony colony = colBuilder.build();
        Building school = addSchoolToColony(game, colony, SchoolLevel.COLLEGE);

        Unit student = getUnitList(colony, freeColonistType).get(0);
        List<Unit> teacherList = getUnitList(colony, expertLumberJackType);
        Unit teacher1 = teacherList.get(0);
        Unit teacher2 = teacherList.get(1);
        assertNull("Teacher1 should not have a student yet",
                   teacher1.getStudent());
        assertNull("Teacher2 should not have a student yet",
                   teacher2.getStudent());

        boolean selection = FreeColTestUtils.setStudentSelection(false);

        // add first teacher
        teacher1.setLocation(school);
        assertEquals("Teacher1 should now have a student", student,
                     teacher1.getStudent());
        assertEquals("Student should have assigned teacher1", teacher1,
                     student.getTeacher());

        // add a second teacher
        teacher2.setLocation(school);
        assertEquals("Teacher1 should still have a student",
                     teacher1.getStudent(), student);
        assertNull("Teacher2 should not have a student yet",
                   teacher2.getStudent());
        assertEquals("Student should have assigned teacher1",
                     student.getTeacher(), teacher1);

        // change teacher
        student.setTeacher(teacher2);
        assertNull("Teacher1 should not have a student now",
                   teacher1.getStudent());
        assertEquals("Teacher2 should now have a student", student,
                     teacher2.getStudent());
        assertEquals("Student should have assigned teacher2", teacher2,
                     student.getTeacher());

        FreeColTestUtils.setStudentSelection(selection);
    }
}
