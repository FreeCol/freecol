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

package net.sf.freecol.server.model;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.FreeColTestUtils;


public class ServerBuildingTest extends FreeColTestCase {

    private static final BuildingType collegeType
        = spec().getBuildingType("model.building.college");
    private static final BuildingType lumberMillType
        = spec().getBuildingType("model.building.lumberMill");
    private static final BuildingType schoolType
        = spec().getBuildingType("model.building.schoolhouse");
    private static final BuildingType townHallType
        = spec().getBuildingType("model.building.townHall");
    private static final BuildingType universityType
        = spec().getBuildingType("model.building.university");

    private static final GoodsType bellsType
        = spec().getGoodsType("model.goods.bells");
    private static final GoodsType foodType
        = spec().getGoodsType("model.goods.food");
    private static final GoodsType grainType
        = spec().getGoodsType("model.goods.grain");

    private static final UnitType freeColonistType
        = spec().getUnitType("model.unit.freeColonist");
    private static final UnitType indenturedServantType
        = spec().getUnitType("model.unit.indenturedServant");
    private static final UnitType pettyCriminalType
        = spec().getUnitType("model.unit.pettyCriminal");
    private static final UnitType expertOreMinerType
        = spec().getUnitType("model.unit.expertOreMiner");
    private static final UnitType expertLumberJackType
        = spec().getUnitType("model.unit.expertLumberJack");
    private static final UnitType masterCarpenterType
        = spec().getUnitType("model.unit.masterCarpenter");
    private static final UnitType masterBlacksmithType
        = spec().getUnitType("model.unit.masterBlacksmith");
    private static final UnitType veteranSoldierType
        = spec().getUnitType("model.unit.veteranSoldier");
    private static final UnitType elderStatesmanType
        = spec().getUnitType("model.unit.elderStatesman");
    private static final UnitType colonialRegularType
        = spec().getUnitType("model.unit.colonialRegular");


    private enum SchoolLevel { SCHOOLHOUSE, COLLEGE, UNIVERSITY };

    /**
     * Creates a colony with a university and n elder statesmen
     */
    private Colony getSchoolColony(int n, SchoolLevel slevel) {
        Colony colony = getStandardColony(n);
        for (Unit u : colony.getUnitList()) u.setType(elderStatesmanType);
        BuildingType type = null;
        switch (slevel) {
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
        Building school = new ServerBuilding(colony.getGame(), colony, type);
        colony.addBuilding(school);
        assertEquals(school.getUnitList().size(), 0);
        colony.addGoods(grainType, 150); // prevent starving during tests
        return colony;
    }

    private void trainForTurns(Colony colony, int requiredTurns) {
        for (int turn = 0; turn < requiredTurns; turn++) {
            ServerTestHelper.newTurn();
        }
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

    /**
     * Check that a free colonist can be taught something.
     *
     */
    public void testExpertTeaching() {
        Game game = ServerTestHelper.startServerGame(getTestMap(true));

        // otherwise this test will crash and burn
        boolean selection = FreeColTestUtils.setStudentSelection(false);

        Colony colony = getSchoolColony(4, SchoolLevel.SCHOOLHOUSE);
        Building school = colony.getBuilding(schoolType);
        assertTrue(school.canTeach());
        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist = units.next();
        colonist.setType(freeColonistType);
        colonist.setLocation(colony.getBuilding(townHallType));

        Unit lumber = units.next();
        lumber.setType(expertLumberJackType);

        Unit black = units.next();
        black.setType(masterBlacksmithType);

        Unit ore = units.next();
        ore.setType(expertOreMinerType);

        assertTrue(colony.canTrain(ore));
        ore.setLocation(school);
        assertEquals(ore.getStudent(), colonist);

        trainForTurns(colony, ore.getNeededTurnsOfTraining());
        assertEquals(expertOreMinerType, colonist.getType());

        FreeColTestUtils.setStudentSelection(selection);
    }

    public void testCollege() {
        Game game = ServerTestHelper.startServerGame(getTestMap(true));

        // otherwise this test will crash and burn
        boolean selection = FreeColTestUtils.setStudentSelection(false);

        Colony colony = getSchoolColony(4, SchoolLevel.COLLEGE);
        Building college = colony.getBuilding(collegeType);
        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist = units.next();
        colonist.setType(freeColonistType);
        colonist.setLocation(colony.getBuilding(townHallType));

        Unit blackSmith = units.next();
        blackSmith.setType(masterBlacksmithType);

        blackSmith.setLocation(college);
        assertEquals(blackSmith.getStudent(), colonist);

        trainForTurns(colony, blackSmith.getNeededTurnsOfTraining());
        assertEquals(masterBlacksmithType, colonist.getType());

        FreeColTestUtils.setStudentSelection(selection);
    }

    public void testUniversity() {
        Game game = ServerTestHelper.startServerGame(getTestMap(true));

        // otherwise this test will crash and burn
        boolean selection = FreeColTestUtils.setStudentSelection(false);

        Colony colony = getSchoolColony(4, SchoolLevel.UNIVERSITY);
        assertEquals(4, colony.getUnitCount());
        Building university = colony.getBuilding(universityType);
        assertNotNull(university);
        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist = units.next();
        colonist.setType(freeColonistType);
        colonist.setLocation(colony.getBuilding(townHallType));

        Unit elder = units.next();
        assertEquals(elder.getType(), elderStatesmanType);

        elder.setLocation(university);
        assertEquals(elder.getStudent(), colonist);

        trainForTurns(colony, elder.getNeededTurnsOfTraining());
        assertEquals(elderStatesmanType, colonist.getType());

        FreeColTestUtils.setStudentSelection(selection);
    }

    /**
     * [ 1616384 ] Teaching
     *
     * One LumberJack and one BlackSmith in a college. 4 Free Colonists, one as
     * LumberJack, one as BlackSmith two as Farmers.
     *
     * After some turns (2 or 3 I don't know) a new LumberJack is ready.
     * Removing the teacher LumberJack replaced by an Ore Miner.
     *
     * Next turn, a new BlackSmith is ready. Removing the teacher
     * BlackSmith replaced by a Veteran Soldier. There is still 2 Free
     * Colonists as Farmers in the Colony.
     *
     * Waiting during more than 8 turns. NOTHING happens.
     *
     * Changing the two Free Colonists by two other Free Colonists.
     *
     * After 2 or 3 turns, a new Ore Miner and a new Veteran Soldier
     * are ready.
     *
     * http://sourceforge.net/tracker/index.php?func=detail&aid=1616384&group_id=43225&atid=435578
     *
     * CO: I think this is a special case of the
     * testSingleGuyTwoTeachers.  But since already the
     * TwoTeachersSimple case fails, I think that needs to be sorted
     * out first.
     */
    public void testTrackerBug1616384() {
        Game game = ServerTestHelper.startServerGame(getTestMap(true));

        // otherwise this test will crash and burn
        boolean selection = FreeColTestUtils.setStudentSelection(false);

        Colony colony = getSchoolColony(8, SchoolLevel.COLLEGE);
        // prevent starvation
        colony.addGoods(foodType, 100);

        Building townHall = colony.getBuilding(townHallType);
        Building lumberMill = colony.getBuilding(lumberMillType);
        Building college = colony.getBuilding(collegeType);
        Iterator<Unit> units = colony.getUnitIterator();

        clearWorkLocation(townHall);
        Unit colonist1 = units.next();
        colonist1.setType(freeColonistType);
        colonist1.setLocation(townHall);
        Unit colonist2 = units.next();
        colonist2.setType(freeColonistType);
        colonist2.setLocation(townHall);

        clearWorkLocation(lumberMill);
        Unit colonist3 = units.next();
        colonist3.setType(freeColonistType);
        colonist3.setLocation(lumberMill);
        Unit colonist4 = units.next();
        colonist4.setType(freeColonistType);
        colonist4.setLocation(lumberMill);

        Unit lumberjack = units.next();
        lumberjack.setType(expertLumberJackType);

        Unit blacksmith = units.next();
        blacksmith.setType(masterBlacksmithType);

        Unit veteran = units.next();
        veteran.setType(veteranSoldierType);

        Unit ore = units.next();
        ore.setType(expertOreMinerType);

        clearWorkLocation(college);
        lumberjack.setLocation(college);
        assertNotNull(lumberjack.getStudent());
        blacksmith.setLocation(college);
        assertNotNull(blacksmith.getStudent());
        
        assertEquals(4, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, expertLumberJackType).size());
        assertEquals(1, getUnitList(colony, masterBlacksmithType).size());

        while (4 == getUnitList(colony, freeColonistType).size()) {
            ServerTestHelper.newTurn();
        }

        assertEquals(3, getUnitList(colony, freeColonistType).size());
        assertEquals(2, getUnitList(colony, expertLumberJackType).size());
        assertEquals(1, getUnitList(colony, masterBlacksmithType).size());

        lumberjack.setLocation(colony.getWorkLocationFor(lumberjack,
                                                         grainType));
        assertNull(lumberjack.getStudent());
        Unit smithToBe = blacksmith.getStudent();
        assertNotNull(smithToBe);
        ore.setLocation(college);
        assertNotNull(ore.getStudent());

        while (3 == getUnitList(colony, freeColonistType).size()) {
            ServerTestHelper.newTurn();
        }
        assertEquals(masterBlacksmithType, smithToBe.getType());
        assertEquals(2, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, expertOreMinerType).size());
        assertEquals(2, getUnitList(colony, masterBlacksmithType).size());

        blacksmith.setLocation(colony.getWorkLocationFor(blacksmith,
                                                         grainType));
        assertNull(blacksmith.getStudent());
        veteran.setLocation(college);
        assertNotNull(veteran.getStudent());

        while (2 == getUnitList(colony, freeColonistType).size()) {
            ServerTestHelper.newTurn();
        }
        assertEquals(1, getUnitList(colony, freeColonistType).size());
        assertEquals(2, getUnitList(colony, expertOreMinerType).size());

        ore.setLocation(colony.getWorkLocationFor(ore, grainType));
        assertNull(ore.getStudent());

        while (1 == getUnitList(colony, freeColonistType).size()) {
            ServerTestHelper.newTurn();
        }
        assertEquals(0, getUnitList(colony, freeColonistType).size());
        assertEquals(2, getUnitList(colony, veteranSoldierType).size());

        FreeColTestUtils.setStudentSelection(selection);
    }

    public void testTwoTeachersSimple() {
        boolean selection = FreeColTestUtils.setStudentSelection(false);

        Game game = ServerTestHelper.startServerGame(getTestMap(true));
        Colony colony = getSchoolColony(5, SchoolLevel.UNIVERSITY);
        // prevent starvation
        colony.addGoods(foodType, 100);

        Building university = colony.getBuilding(universityType);
        Building townHall = colony.getBuilding(townHallType);
        clearWorkLocation(townHall);
        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist1 = units.next();
        colonist1.setType(freeColonistType);
        colonist1.setLocation(townHall);

        Unit colonist2 = units.next();
        colonist2.setType(freeColonistType);
        colonist2.setLocation(townHall);

        Unit colonist3 = units.next();
        colonist3.setType(freeColonistType);
        colonist3.setLocation(townHall);

        Unit black = units.next();
        black.setType(masterBlacksmithType);
        black.setLocation(university);

        Unit ore = units.next();
        ore.setType(expertOreMinerType);
        ore.setLocation(university);

        assertTrue("Should be longer to train blacksmith then ore miner",
            black.getNeededTurnsOfTraining() > ore.getNeededTurnsOfTraining());
        assertEquals(3, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, masterBlacksmithType).size());
        assertEquals(1, getUnitList(colony, expertOreMinerType).size());

        trainForTurns(colony, ore.getNeededTurnsOfTraining());
        assertEquals(2, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, masterBlacksmithType).size());
        assertEquals(2, getUnitList(colony, expertOreMinerType).size());
        assertNotNull(ore.getStudent());
        assertNotNull(black.getStudent());

        trainForTurns(colony, black.getNeededTurnsOfTraining()
                      - black.getTurnsOfTraining());
        assertEquals(1, getUnitList(colony, freeColonistType).size());
        assertEquals(2, getUnitList(colony, masterBlacksmithType).size());
        assertEquals(2, getUnitList(colony, expertOreMinerType).size());

        FreeColTestUtils.setStudentSelection(selection);
    }


    /**
     * If there are two teachers, but just one colonist to be taught.
     */
    public void testSingleGuyTwoTeachers() {
        boolean selection = FreeColTestUtils.setStudentSelection(false);
        Game game = ServerTestHelper.startServerGame(getTestMap(true));

        Colony colony = getSchoolColony(4, SchoolLevel.UNIVERSITY);
        Building university = colony.getBuilding(universityType);
        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist = units.next();
        colonist.setType(freeColonistType);
        Building townHall = colony.getBuilding(townHallType);
        clearWorkLocation(townHall);
        colonist.setLocation(townHall);

        Unit lumberJack = units.next();
        lumberJack.setType(expertLumberJackType);

        Unit blackSmith = units.next();
        blackSmith.setType(masterBlacksmithType);

        // It should take 4 turns to train an expert lumber jack and 6
        // to train a blacksmith, so the lumber jack chould be
        // finished teaching first.  The school works for now as
        // first come first serve.
        blackSmith.setLocation(university);
        lumberJack.setLocation(university);
        assertTrue(colonist.getTeacher() == blackSmith);
        assertNull(lumberJack.getStudent());

        assertEquals(1, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, expertLumberJackType).size());
        assertEquals(1, getUnitList(colony, masterBlacksmithType).size());

        trainForTurns(colony, blackSmith.getNeededTurnsOfTraining());
        assertEquals(0, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, expertLumberJackType).size());
        assertEquals(2, getUnitList(colony, masterBlacksmithType).size());

        FreeColTestUtils.setStudentSelection(selection);
    }

    /**
     * If there are two teachers of the same kind, but just one
     * colonist to be taught, this should not mean any speed up.
     */
    public void testTwoTeachersOfSameKind() {
        boolean selection = FreeColTestUtils.setStudentSelection(false);
        Game game = ServerTestHelper.startServerGame(getTestMap(true));

        Colony colony = getSchoolColony(4, SchoolLevel.UNIVERSITY);
        Building university = colony.getBuilding(universityType);
        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist = units.next();
        colonist.setType(freeColonistType);
        Building townHall = colony.getBuilding(townHallType);
        clearWorkLocation(townHall);
        colonist.setLocation(townHall);

        Unit lumberjack1 = units.next();
        lumberjack1.setType(expertLumberJackType);

        Unit lumberjack2 = units.next();
        lumberjack2.setType(expertLumberJackType);

        lumberjack1.setLocation(university);
        lumberjack2.setLocation(university);
        assertEquals(colonist, lumberjack1.getStudent());
        assertNull(lumberjack2.getStudent());

        for (int i = lumberjack1.getNeededTurnsOfTraining(); i > 0; i--) {
            assertEquals(1, getUnitList(colony, freeColonistType).size());
            trainForTurns(colony, 1);
        }

        assertEquals(0, getUnitList(colony, freeColonistType).size());
        assertEquals(3, getUnitList(colony, expertLumberJackType).size());

        FreeColTestUtils.setStudentSelection(selection);
    }

    /**
     * If there are two teachers with the same skill level, the first
     * to be put in the school should be used for teaching.
     */
    public void testSingleGuyTwoTeachers2() {
        boolean selection = FreeColTestUtils.setStudentSelection(false);
        Game game = ServerTestHelper.startServerGame(getTestMap(true));

        Colony colony = getSchoolColony(4, SchoolLevel.UNIVERSITY);
        Building university = colony.getBuilding(universityType);
        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist = units.next();
        colonist.setType(freeColonistType);
        Building townHall = colony.getBuilding(townHallType);
        clearWorkLocation(townHall);
        colonist.setLocation(townHall);

        Unit lumber = units.next();
        lumber.setType(expertLumberJackType);

        Unit ore = units.next();
        ore.setType(expertOreMinerType);

        // It should take 4 turns to train an expert lumber jack and
        // also 4 to train a ore miner.  First come first serve, the
        // lumber jack wins.
        lumber.setLocation(university);
        ore.setLocation(university);
        assertEquals(colonist.getTeacher(), lumber);
        assertNull(ore.getStudent());

        assertEquals(1, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, expertLumberJackType).size());
        assertEquals(1, getUnitList(colony, expertOreMinerType).size());

        ServerTestHelper.newTurn();
        assertEquals(1, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, expertLumberJackType).size());
        assertEquals(1, getUnitList(colony, expertOreMinerType).size());

        ServerTestHelper.newTurn();
        assertEquals(1, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, expertLumberJackType).size());
        assertEquals(1, getUnitList(colony, expertOreMinerType).size());

        ServerTestHelper.newTurn();
        assertEquals(1, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, expertLumberJackType).size());
        assertEquals(1, getUnitList(colony, expertOreMinerType).size());

        ServerTestHelper.newTurn();
        assertEquals(0, getUnitList(colony, freeColonistType).size());
        assertEquals(2, getUnitList(colony, expertLumberJackType).size());
        assertEquals(1, getUnitList(colony, expertOreMinerType).size());

        FreeColTestUtils.setStudentSelection(selection);
    }

    /**
     * Test that an petty criminal becomes an indentured servant
     */
    public void testTeachPettyCriminals() {
        boolean selection = FreeColTestUtils.setStudentSelection(false);
        Game game = ServerTestHelper.startServerGame(getTestMap(true));

        Colony colony = getSchoolColony(4, SchoolLevel.UNIVERSITY);
        Building university = colony.getBuilding(universityType);
        Iterator<Unit> units = colony.getUnitIterator();

        Unit criminal = units.next();
        criminal.setType(pettyCriminalType);
        criminal.setLocation(colony.getBuilding(townHallType));

        Unit teacher = units.next();
        teacher.setType(expertOreMinerType);

        teacher.setLocation(university);
        assertEquals(teacher.getNeededTurnsOfTraining(), 4);
        assertTrue(criminal.canBeStudent(teacher));
        assertEquals(criminal, teacher.getStudent());

        // PETTY_CRIMINALS become INDENTURED_SERVANTS
        trainForTurns(colony, teacher.getNeededTurnsOfTraining());
        assertEquals(0, getUnitList(colony, pettyCriminalType).size());
        assertEquals(indenturedServantType, criminal.getType());

        FreeColTestUtils.setStudentSelection(selection);
    }

    /**
     * The time to teach somebody does not depend on the one who is
     * being taught, but on the teacher.
     */
    public void testTeachPettyCriminalsByMaster() {
        boolean selection = FreeColTestUtils.setStudentSelection(false);
        Game game = ServerTestHelper.startServerGame(getTestMap(true));

        Colony colony = getSchoolColony(4, SchoolLevel.UNIVERSITY);
        Building university = colony.getBuilding(universityType);
        Iterator<Unit> units = colony.getUnitIterator();

        Unit criminal = units.next();
        criminal.setType(pettyCriminalType);
        criminal.setLocation(colony.getBuilding(townHallType));

        Unit teacher = units.next();
        teacher.setType(masterBlacksmithType);

        teacher.setLocation(university);
        assertEquals(teacher.getNeededTurnsOfTraining(), 4);
        assertEquals(criminal, teacher.getStudent());

        trainForTurns(colony, teacher.getNeededTurnsOfTraining());
        assertEquals(0, getUnitList(colony, pettyCriminalType).size());
        assertEquals(indenturedServantType, criminal.getType());

        FreeColTestUtils.setStudentSelection(selection);
    }

    /**
     * Test that an indentured servant becomes a free colonist
     */
    public void testTeachIndenturedServants() {
        boolean selection = FreeColTestUtils.setStudentSelection(false);
        Game game = ServerTestHelper.startServerGame(getTestMap(true));

        Colony colony = getSchoolColony(4, SchoolLevel.UNIVERSITY);
        Building university = colony.getBuilding(universityType);
        Iterator<Unit> units = colony.getUnitIterator();

        Unit indenturedServant = units.next();
        indenturedServant.setType(indenturedServantType);

        Unit teacher = units.next();
        teacher.setType(masterBlacksmithType);

        teacher.setLocation(university);
        assertEquals(teacher.getNeededTurnsOfTraining(), 4);
        assertEquals(indenturedServant, teacher.getStudent());

        // Train to become free colonist
        trainForTurns(colony, teacher.getNeededTurnsOfTraining());
        assertEquals(0, getUnitList(colony, indenturedServantType).size());
        assertEquals(freeColonistType, indenturedServant.getType());

        FreeColTestUtils.setStudentSelection(selection);
    }

    /**
     * Progress in teaching is bound to the teacher and not the learner.
     *
     * Moving students around does not slow education. This behavior is
     * there to simplify gameplay.
     */
    public void testTeacherStoresProgress() {
        boolean selection = FreeColTestUtils.setStudentSelection(false);
        Game game = ServerTestHelper.startServerGame(getTestMap(true));

        Colony colony = getSchoolColony(4, SchoolLevel.UNIVERSITY);
        Building university = colony.getBuilding(universityType);
        Iterator<Unit> units = colony.getUnitIterator();
        Unit outsider = new ServerUnit(game, colony.getTile(),
                                       colony.getOwner(), freeColonistType);

        Building townHall = colony.getBuilding(townHallType);
        Unit colonist = units.next();
        colonist.setType(freeColonistType);
        colonist.setLocation(townHall);

        Unit teacher = units.next();
        teacher.setType(expertOreMinerType);
        teacher.setLocation(university);
        assertEquals(teacher.getStudent(), colonist);
        assertEquals(colonist.getTeacher(), teacher);

        // Train to become free colonist then swap the colonist with
        // another one.
        trainForTurns(colony, teacher.getNeededTurnsOfTraining() - 1);
        colonist.setLocation(colony.getTile());
        outsider.setLocation(townHall);
        assertEquals(1, getUnitList(colony, freeColonistType).size());
        assertEquals(teacher.getStudent(), outsider);

        ServerTestHelper.newTurn();
        assertEquals(0, getUnitList(colony, freeColonistType).size());
        assertEquals(expertOreMinerType, outsider.getType());

        FreeColTestUtils.setStudentSelection(selection);
    }

    /**
     * Progress in teaching is bound to the teacher and not the learner.
     *
     * Moving a teacher inside the colony should not reset its training.
     */
    public void testMoveTeacherInside() {
        boolean selection = FreeColTestUtils.setStudentSelection(false);
        Game game = ServerTestHelper.startServerGame(getTestMap(true));

        Colony colony = getSchoolColony(4, SchoolLevel.UNIVERSITY);
        // prevent starvation
        colony.addGoods(foodType, 100);

        Building university = colony.getBuilding(universityType);
        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist = units.next();
        colonist.setType(freeColonistType);
        colonist.setLocation(colony.getBuilding(townHallType));

        Unit criminal = units.next();
        criminal.setType(pettyCriminalType);
        criminal.setLocation(colony.getBuilding(townHallType));

        Unit teacher1 = units.next();
        teacher1.setType(expertOreMinerType);

        Unit teacher2 = units.next();
        teacher2.setType(masterCarpenterType);

        // The carpenter is set in the school before the miner.
        // In this case, the colonist will become a miner (and the criminal
        // will become a servant).
        teacher2.setLocation(university);
        teacher1.setLocation(university);
        assertEquals(4, teacher1.getNeededTurnsOfTraining());
        assertEquals(4, teacher2.getNeededTurnsOfTraining());

        // wait a little
        ServerTestHelper.newTurn();
        ServerTestHelper.newTurn();
        assertEquals(2, teacher1.getTurnsOfTraining());
        assertEquals(2, teacher2.getTurnsOfTraining());
        assertEquals(1, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, pettyCriminalType).size());
        assertEquals(1, getUnitList(colony, expertOreMinerType).size());
        assertEquals(1, getUnitList(colony, masterCarpenterType).size());

        // Now we want the colonist to be a carpenter. We just want to
        // shuffle the teachers.  Move them out of the school.
        teacher2.setLocation(colony.getWorkLocationFor(teacher2,
                                                       grainType));
        teacher1.setLocation(colony.getTile());
        assertNull(teacher1.getStudent());
        assertNull(teacher2.getStudent());

        // Moving outside school resets training
        ServerTestHelper.newTurn();
        assertEquals(0, teacher1.getTurnsOfTraining());
        assertEquals(0, teacher2.getTurnsOfTraining());
        assertEquals(1, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, pettyCriminalType).size());

        // Move teachers back to school, miner first to pick up the criminal
        teacher1.setLocation(university);
        teacher2.setLocation(university);

        ServerTestHelper.newTurn();
        assertEquals(1, teacher1.getTurnsOfTraining());
        assertEquals(1, teacher2.getTurnsOfTraining());
        assertEquals(1, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, pettyCriminalType).size());

        ServerTestHelper.newTurn();
        ServerTestHelper.newTurn();
        ServerTestHelper.newTurn();
        assertEquals(0, teacher1.getTurnsOfTraining());
        assertEquals(0, teacher2.getTurnsOfTraining());

        // Teacher1's student (criminal) should be a servant now
        // Teacher2's student (colonist) should be a carpenter now
        assertEquals(0, getUnitList(colony, freeColonistType).size());
        assertEquals(0, getUnitList(colony, pettyCriminalType).size());
        assertEquals(1, getUnitList(colony, indenturedServantType).size());
        assertEquals(1, getUnitList(colony, expertOreMinerType).size());
        assertEquals(2, getUnitList(colony, masterCarpenterType).size());

        FreeColTestUtils.setStudentSelection(selection);
    }

    public void testCaseTwoTeachersWithDifferentExp() {
        boolean selection = FreeColTestUtils.setStudentSelection(false);
        Game game = ServerTestHelper.startServerGame(getTestMap(true));

        Colony colony = getSchoolColony(4, SchoolLevel.UNIVERSITY);
        Building university = colony.getBuilding(universityType);
        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist = units.next();
        colonist.setType(freeColonistType);
        colonist.setLocation(colony.getBuilding(townHallType));

        Unit teacher1 = units.next();
        teacher1.setType(expertOreMinerType);

        Unit teacher2 = units.next();
        teacher2.setType(masterCarpenterType);

        // First we let the teacher1 train for 3 turns
        teacher1.setLocation(university);
        ServerTestHelper.newTurn();
        ServerTestHelper.newTurn();
        ServerTestHelper.newTurn();
        assertEquals(3, teacher1.getTurnsOfTraining());

        // Then teacher2 for 1 turn
        teacher1.setLocation(colony.getWorkLocationFor(teacher1,
                                                       grainType));
        teacher2.setLocation(university);
        ServerTestHelper.newTurn();
        assertEquals(0, teacher1.getTurnsOfTraining());
        assertEquals(1, teacher2.getTurnsOfTraining());

        // If we now also add teacher1 to the university, but
        // teacher2 should still be the teacher in charge
        teacher1.setLocation(university);
        assertNull(teacher1.getStudent());
        ServerTestHelper.newTurn();

        assertEquals(0, teacher1.getTurnsOfTraining());
        assertEquals(2, teacher2.getTurnsOfTraining());

        FreeColTestUtils.setStudentSelection(selection);
    }

    /**
     * Progress in teaching is bound to the teacher and not the learner.
     *
     * Moving a teacher outside the colony should reset its training.
     */
    public void testMoveTeacherOutside() {
        boolean selection = FreeColTestUtils.setStudentSelection(false);
        Game game = ServerTestHelper.startServerGame(getTestMap(true));

        Colony otherColony = getStandardColony(1, 10, 10);
        Colony colony = getSchoolColony(4, SchoolLevel.UNIVERSITY);
        // prevent starvation
        colony.addGoods(foodType, 100);

        Building university = colony.getBuilding(universityType);
        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist = units.next();
        colonist.setType(freeColonistType);
        colonist.setLocation(colony.getBuilding(townHallType));

        Unit criminal = units.next();
        criminal.setType(pettyCriminalType);
        criminal.setLocation(colony.getBuilding(townHallType));

        Unit teacher1 = units.next();
        teacher1.setType(expertOreMinerType);

        Unit teacher2 = units.next();
        teacher2.setType(masterCarpenterType);

        // The carpenter is set in the school before the miner
        // In this case, the colonist will become a miner (and the criminal
        // will become a servant).
        teacher2.setLocation(university);
        teacher1.setLocation(university);

        // wait a little
        ServerTestHelper.newTurn();
        ServerTestHelper.newTurn();
        assertEquals(1, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, pettyCriminalType).size());
        assertEquals(1, getUnitList(colony, expertOreMinerType).size());
        assertEquals(1, getUnitList(colony, masterCarpenterType).size());
        assertEquals(2, teacher1.getTurnsOfTraining());
        assertEquals(2, teacher2.getTurnsOfTraining());

        // Now we move the teachers somewhere beyond the colony
        teacher1.setLocation(getGame().getMap().getTile(6, 8));
        teacher2.setLocation(otherColony);
        assertEquals(0, teacher1.getTurnsOfTraining());
        assertEquals(0, teacher2.getTurnsOfTraining());
        assertEquals(1, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, pettyCriminalType).size());
        assertEquals(0, getUnitList(colony, expertOreMinerType).size());
        assertEquals(0, getUnitList(colony, masterCarpenterType).size());

        // Put them back here
        teacher2.setLocation(university);
        teacher1.setLocation(university);
        assertEquals(0, teacher1.getTurnsOfTraining());
        assertEquals(0, teacher2.getTurnsOfTraining());
        assertEquals(teacher1, colonist.getTeacher());
        assertEquals(teacher2, criminal.getTeacher());

        // Check that 2 new turns aren't enough for training
        ServerTestHelper.newTurn();
        assertEquals(1, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, pettyCriminalType).size());
        assertEquals(1, getUnitList(colony, expertOreMinerType).size());
        assertEquals(1, getUnitList(colony, masterCarpenterType).size());

        ServerTestHelper.newTurn();
        assertEquals(1, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, pettyCriminalType).size());
        assertEquals(1, getUnitList(colony, expertOreMinerType).size());
        assertEquals(1, getUnitList(colony, masterCarpenterType).size());

        ServerTestHelper.newTurn();
        assertEquals(1, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, pettyCriminalType).size());
        assertEquals(1, getUnitList(colony, expertOreMinerType).size());
        assertEquals(1, getUnitList(colony, masterCarpenterType).size());

        ServerTestHelper.newTurn();
        assertEquals(0, getUnitList(colony, freeColonistType).size());
        assertEquals(0, getUnitList(colony, pettyCriminalType).size());
        assertEquals(1, getUnitList(colony, indenturedServantType).size());
        assertEquals(2, getUnitList(colony, expertOreMinerType).size());
        assertEquals(1, getUnitList(colony, masterCarpenterType).size());

        FreeColTestUtils.setStudentSelection(selection);
    }

    /**
     * Sons of Liberty influences teaching.
     */
    public void testSonsOfLiberty() {
        boolean selection = FreeColTestUtils.setStudentSelection(false);
        Game game = ServerTestHelper.startServerGame(getTestMap(true));

        Colony colony = getSchoolColony(4, SchoolLevel.UNIVERSITY);
        Building university = colony.getBuilding(universityType);
        Iterator<Unit> units = colony.getUnitIterator();
        Unit colonist = units.next();
        colonist.setType(freeColonistType);
        colonist.setLocation(colony.getBuilding(townHallType));

        Unit lumberjack = units.next();
        lumberjack.setType(expertLumberJackType);
        lumberjack.setLocation(university);

        assertEquals(4, lumberjack.getNeededTurnsOfTraining());

        colony.addGoods(bellsType, 100000);
        assertEquals(2, lumberjack.getNeededTurnsOfTraining());

        trainForTurns(colony, 2);
        assertEquals(0, getUnitList(colony, freeColonistType).size());
        assertEquals(2, getUnitList(colony, expertLumberJackType).size());

        FreeColTestUtils.setStudentSelection(selection);
    }

    /**
     * Trains partly one colonist then put another teacher.
     *
     * Should not save progress but start all over.
     */
    public void testPartTraining() {
        boolean selection = FreeColTestUtils.setStudentSelection(false);
        Game game = ServerTestHelper.startServerGame(getTestMap(true));

        Colony colony = getSchoolColony(4, SchoolLevel.UNIVERSITY);
        Building university = colony.getBuilding(universityType);
        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist = units.next();
        colonist.setType(freeColonistType);
        colonist.setLocation(colony.getBuilding(townHallType));

        Unit lumberjack = units.next();
        lumberjack.setType(expertLumberJackType);

        Unit miner = units.next();
        miner.setType(expertOreMinerType);

        // Put Lumberjack in School
        lumberjack.setLocation(university);
        assertEquals(lumberjack.getStudent(), colonist);
        assertEquals(colonist.getTeacher(), lumberjack);

        ServerTestHelper.newTurn();
        ServerTestHelper.newTurn();

        // After 2 turns replace by miner. Progress starts from scratch.
        lumberjack.setLocation(colony.getWorkLocationFor(lumberjack, grainType));
        assertNull(lumberjack.getStudent());
        assertNull(colonist.getTeacher());

        miner.setLocation(university);
        assertEquals(miner.getStudent(), colonist);
        assertEquals(colonist.getTeacher(), miner);

        trainForTurns(colony, miner.getNeededTurnsOfTraining());
        assertEquals(0, getUnitList(colony, freeColonistType).size());
        assertEquals(2, getUnitList(colony, expertOreMinerType).size());

        FreeColTestUtils.setStudentSelection(selection);
    }

    /**
     * Test that free colonists are trained before indentured servants, which
     * are preferred to petty criminals.
     */
    public void testTeachingOrder() {
        boolean selection = FreeColTestUtils.setStudentSelection(false);
        Game game = ServerTestHelper.startServerGame(getTestMap(true));

        Colony colony = getSchoolColony(4, SchoolLevel.UNIVERSITY);
        // prevent starvation
        colony.addGoods(foodType, 100);

        Building university = colony.getBuilding(universityType);
        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist = units.next();
        colonist.setType(freeColonistType);
        colonist.setLocation(colony.getBuilding(townHallType));

        Unit indenturedServant = units.next();
        indenturedServant.setType(indenturedServantType);
        indenturedServant.setLocation(colony.getBuilding(townHallType));

        Unit criminal = units.next();
        criminal.setType(pettyCriminalType);
        criminal.setLocation(colony.getBuilding(townHallType));

        Unit teacher = units.next();
        teacher.setType(expertOreMinerType);
        teacher.setLocation(university);

        assertTrue(colonist.canBeStudent(teacher));
        assertTrue(indenturedServant.canBeStudent(teacher));
        assertTrue(criminal.canBeStudent(teacher));

        // Criminal training
        assertEquals(teacher, criminal.getTeacher());
        assertEquals(criminal, teacher.getStudent());
        trainForTurns(colony, teacher.getNeededTurnsOfTraining());

        assertEquals(0, getUnitList(colony, pettyCriminalType).size());
        assertEquals(indenturedServantType, criminal.getType());
        criminal.setLocation(getGame().getMap().getTile(10,8));

        // Servant training
        assertNull(teacher.getStudent());
        ServerTestHelper.newTurn();
        assertEquals(teacher, indenturedServant.getTeacher());
        assertEquals(indenturedServant, teacher.getStudent());
        trainForTurns(colony, teacher.getNeededTurnsOfTraining());

        assertEquals(0, getUnitList(colony, indenturedServantType).size());
        assertEquals(2, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, expertOreMinerType).size());
        assertEquals(freeColonistType, indenturedServant.getType());

        // Colonist(former servant) training continues
        assertEquals(teacher, indenturedServant.getTeacher());
        assertEquals(indenturedServant, teacher.getStudent());
        assertEquals(colonist.getTeacher(), null);

        trainForTurns(colony, teacher.getNeededTurnsOfTraining());
        assertEquals(0, getUnitList(colony, indenturedServantType).size());
        assertEquals(1, getUnitList(colony, freeColonistType).size());
        assertEquals(2, getUnitList(colony, expertOreMinerType).size());
        assertEquals(expertOreMinerType, indenturedServant.getType());
        assertEquals(indenturedServant.getTeacher(), null);

        FreeColTestUtils.setStudentSelection(selection);
    }

    /**
     * Test that an indentured servant cannot be promoted to free colonist and
     * learn a skill at the same time.
     */
    public void testTeachingDoublePromotion() {
        boolean selection = FreeColTestUtils.setStudentSelection(false);
        Game game = ServerTestHelper.startServerGame(getTestMap(true));

        Colony colony = getSchoolColony(4, SchoolLevel.UNIVERSITY);
        // prevent starvation
        colony.addGoods(foodType, 100);

        Building university = colony.getBuilding(universityType);
        Iterator<Unit> units = colony.getUnitIterator();

        Unit indenturedServant = units.next();
        indenturedServant.setType(indenturedServantType);
        indenturedServant.setLocation(colony.getBuilding(townHallType));

        Unit criminal = units.next();
        criminal.setType(pettyCriminalType);
        criminal.setLocation(colony.getBuilding(townHallType));

        Unit teacher1 = units.next();
        teacher1.setType(expertOreMinerType);

        Unit teacher2 = units.next();
        teacher2.setType(expertLumberJackType);

        // set location only AFTER all types have been set!
        teacher1.setLocation(university);
        teacher2.setLocation(university);
        assertEquals(criminal, teacher1.getStudent());
        assertEquals(indenturedServant, teacher2.getStudent());

        // Training time
        trainForTurns(colony, teacher1.getNeededTurnsOfTraining());

        // indentured servant should have been promoted to free colonist
        // petty criminal should have been promoted to indentured servant
        assertEquals(freeColonistType, indenturedServant.getType());
        assertEquals(indenturedServantType, criminal.getType());
        assertEquals(1, getUnitList(colony, expertOreMinerType).size());
        assertEquals(1, getUnitList(colony, expertLumberJackType).size());
        assertEquals(1, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, indenturedServantType).size());
        assertEquals(0, getUnitList(colony, pettyCriminalType).size());
        criminal.setLocation(getGame().getMap().getTile(10,8));
        assertNull(teacher1.getStudent());
        assertEquals(teacher2, indenturedServant.getTeacher());

        // Train again
        trainForTurns(colony, teacher2.getNeededTurnsOfTraining());
        assertEquals(expertLumberJackType, indenturedServant.getType());
        assertEquals(1, getUnitList(colony, expertOreMinerType).size());
        assertEquals(2, getUnitList(colony, expertLumberJackType).size());
        assertEquals(0, getUnitList(colony, freeColonistType).size());
        assertEquals(0, getUnitList(colony, indenturedServantType).size());
        assertEquals(0, getUnitList(colony, pettyCriminalType).size());

        FreeColTestUtils.setStudentSelection(selection);
    }

    public void testColonialRegular() {
        boolean selection = FreeColTestUtils.setStudentSelection(false);
        Game game = ServerTestHelper.startServerGame(getTestMap(true));

        Colony colony = getSchoolColony(4, SchoolLevel.UNIVERSITY);
        Building university = colony.getBuilding(universityType);
        colony.getOwner().addAbility(new Ability(Ability.INDEPENDENCE_DECLARED));
        Iterator<Unit> units = colony.getUnitIterator();

        Unit regular = units.next();
        regular.setType(colonialRegularType);

        Unit colonist = units.next();
        colonist.setType(freeColonistType);
        colonist.setLocation(colony.getBuilding(townHallType));

        regular.setLocation(university);
        assertEquals(colonist, regular.getStudent());
        trainForTurns(colony, freeColonistType.getEducationTurns(veteranSoldierType));

        assertEquals(veteranSoldierType, colonist.getType());

        FreeColTestUtils.setStudentSelection(selection);
    }

    public void testConcurrentUpgrade() {
        boolean selection = FreeColTestUtils.setStudentSelection(false);
        Game game = ServerTestHelper.startServerGame(getTestMap(true));

        Colony colony = getSchoolColony(2, SchoolLevel.SCHOOLHOUSE);
        Building school = colony.getBuilding(schoolType);
        Iterator<Unit> units = colony.getUnitIterator();

        Unit lumber = units.next();
        lumber.setType(expertLumberJackType);

        Unit student = units.next();
        student.setType(pettyCriminalType);
        student.setLocation(colony.getBuilding(townHallType));

        assertTrue(school.canTeach());
        assertTrue(colony.canTrain(lumber));
        lumber.setLocation(school);

        ServerTestHelper.newTurn();
        assertEquals(student, lumber.getStudent());

        // lumber jack can teach indentured servant
        student.changeType(indenturedServantType);
        assertEquals(student, lumber.getStudent());

        // lumber jack can teach free colonist
        student.changeType(freeColonistType);
        assertEquals(student, lumber.getStudent());

        // lumber jack can not teach expert
        student.changeType(masterCarpenterType);
        assertNull(lumber.getStudent());
        assertNull(student.getTeacher());

        FreeColTestUtils.setStudentSelection(selection);
    }

    public void testProductionBonus() {
        boolean selection = FreeColTestUtils.setStudentSelection(false);
        Game game = ServerTestHelper.startServerGame(getTestMap(true));

        Colony colony = getSchoolColony(6, SchoolLevel.UNIVERSITY);
        Building university = colony.getBuilding(universityType);

        Iterator<Unit> units = colony.getUnitIterator();

        Unit carpenter = units.next();
        carpenter.setType(masterCarpenterType);

        Unit blacksmith = units.next();
        blacksmith.setType(masterBlacksmithType);

        Unit statesman = units.next();
        statesman.setType(elderStatesmanType);

        units.next().setType(freeColonistType);
        units.next().setType(freeColonistType);
        units.next().setType(freeColonistType);

        carpenter.setLocation(university);
        blacksmith.setLocation(university);
        statesman.setLocation(university);

        for (int bonus = -2; bonus < 3; bonus++) {
            setProductionBonus(colony, bonus);
            assertEquals(4 - bonus, carpenter.getNeededTurnsOfTraining());
            assertEquals(6 - bonus, blacksmith.getNeededTurnsOfTraining());
            assertEquals(8 - bonus, statesman.getNeededTurnsOfTraining());
        }

        FreeColTestUtils.setStudentSelection(selection);
    }
}
