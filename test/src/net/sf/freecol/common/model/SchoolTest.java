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

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sf.freecol.util.test.FreeColTestCase;

public class SchoolTest extends FreeColTestCase {


    /**
     * Returns a list of all units in this colony of the given type.
     * 
     * @param type The type of the units to include in the list. For instance
     *            Unit.EXPERT_FARMER.
     * @return A list of all the units of the given type in this colony.
     */
    private List<Unit> getUnitList(Colony colony, int type) {
        ArrayList<Unit> units = new ArrayList<Unit>();
        for (WorkLocation wl : colony.getWorkLocations()) {
            for (Unit unit : wl.getUnitList()) {
                if (unit.getType() == type) {
                    units.add(unit);
                }
            }
        }
        return units;
    }

    private void trainForTurns(Colony colony, int requiredTurns) {
        trainForTurns(colony, requiredTurns, Unit.FREE_COLONIST);
    }

    private void trainForTurns(Colony colony, int requiredTurns, int unitType) {
        for (int turn = 0; turn < requiredTurns; turn++) {
           /* assertEquals("wrong number of units in turn " + turn + ": " + unitType,
                         1, getUnitList(colony, unitType).size()); */
            colony.getBuilding(spec().getBuildingType("model.building.Schoolhouse")).newTurn();
        }
    }

    BuildingType schoolType = spec().getBuildingType("model.building.Schoolhouse");
    
    /**
     * Check that a free colonist can be taught something.
     * 
     */
    public void testExpertTeaching() {
        
        Colony colony = getStandardColony(4);

        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist = units.next();
        colonist.setType(Unit.FREE_COLONIST);

        Unit lumber = units.next();
        lumber.setType(Unit.EXPERT_LUMBER_JACK);

        Unit black = units.next();
        black.setType(Unit.MASTER_BLACKSMITH);

        Unit ore = units.next();
        ore.setType(Unit.EXPERT_ORE_MINER);

        BuildingType schoolType = spec().getBuildingType("model.building.Schoolhouse");
        colony.addBuilding(new Building(getGame(), colony, schoolType));
        Building school = colony.getBuilding(spec().getBuildingType("model.building.Schoolhouse"));
        assertTrue(schoolType.hasAbility("model.ability.teach"));
        assertTrue(colony.canTrain(ore));

        ore.setLocation(school);
        trainForTurns(colony, ore.getNeededTurnsOfTraining());
        assertEquals(Unit.EXPERT_ORE_MINER, colonist.getType());
        colony.dispose();
    }

    public void testCollege() {
        
        Colony colony = getStandardColony(8);

        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist = units.next();
        colonist.setType(Unit.FREE_COLONIST);

        Unit blackSmith = units.next();
        blackSmith.setType(Unit.MASTER_BLACKSMITH);

        BuildingType schoolType = spec().getBuildingType("model.building.Schoolhouse");
        colony.addBuilding(new Building(getGame(), colony, schoolType));
        Building school = colony.getBuilding(spec().getBuildingType("model.building.Schoolhouse"));
        school.upgrade();

        blackSmith.setLocation(school);
        trainForTurns(colony, blackSmith.getNeededTurnsOfTraining());
        assertEquals(Unit.MASTER_BLACKSMITH, colonist.getType());
        colony.dispose();
    }

    public void testUniversity() {
        
        Colony colony = getStandardColony(10);

        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist = units.next();
        colonist.setType(Unit.FREE_COLONIST);

        Unit elder = units.next();
        elder.setType(Unit.ELDER_STATESMAN);

        BuildingType schoolType = spec().getBuildingType("model.building.Schoolhouse");
        colony.addBuilding(new Building(getGame(), colony, schoolType));
        Building school = colony.getBuilding(spec().getBuildingType("model.building.Schoolhouse"));
        school.upgrade();
        school.upgrade();

        elder.setLocation(school);
        trainForTurns(colony, elder.getNeededTurnsOfTraining());
        assertEquals(Unit.ELDER_STATESMAN, colonist.getType());
        colony.dispose();
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
     * Next turn, a new BlackSmith id ready. Removing the teacher BlackSmith
     * replaced by a Veteran Soldier. There is still 2 Free Colonists as Farmers
     * in the Colony.
     * 
     * Waiting during more than 8 turns. NOTHING happens.
     * 
     * Changing the two Free Colonists by two other Free Colonists.
     * 
     * After 2 or 3 turns, a new Ore Miner and a new Veteran Soldier are ready.
     * 
     * http://sourceforge.net/tracker/index.php?func=detail&aid=1616384&group_id=43225&atid=435578
     * 
     * CO: I think this is a special case of the testSingleGuyTwoTeachers. But
     * since already the TwoTeachersSimple case fails, I think that needs to be
     * sorted out first.
     */
    public void testTrackerBug1616384() {
        
        Colony colony = getStandardColony(8);

        // Setting the stage...
        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist1 = units.next();
        colonist1.setType(Unit.FREE_COLONIST);

        Unit colonist2 = units.next();
        colonist2.setType(Unit.FREE_COLONIST);

        Unit colonist3 = units.next();
        colonist3.setType(Unit.FREE_COLONIST);

        Unit colonist4 = units.next();
        colonist4.setType(Unit.FREE_COLONIST);

        Unit lumberjack = units.next();
        lumberjack.setType(Unit.EXPERT_LUMBER_JACK);

        Unit blacksmith = units.next();
        blacksmith.setType(Unit.MASTER_BLACKSMITH);

        Unit veteran = units.next();
        veteran.setType(Unit.VETERAN_SOLDIER);

        Unit ore = units.next();
        ore.setType(Unit.EXPERT_ORE_MINER);

        // Build a college...
        BuildingType schoolType = spec().getBuildingType("model.building.Schoolhouse");
        colony.addBuilding(new Building(getGame(), colony, schoolType));
        Building school = colony.getBuilding(spec().getBuildingType("model.building.Schoolhouse"));
        school.upgrade();

        blacksmith.setLocation(school);
        lumberjack.setLocation(school);

        // It should not take more than 15 turns (my guess) to get the whole
        // story over with.
        int maxTurns = 15;

        while (4 == getUnitList(colony, Unit.FREE_COLONIST).size() && maxTurns-- > 0) {
            school.newTurn();
        }
        assertEquals(3, getUnitList(colony, Unit.FREE_COLONIST).size());
        assertEquals(1, getUnitList(colony, Unit.MASTER_BLACKSMITH).size());
        assertEquals(2, getUnitList(colony, Unit.EXPERT_LUMBER_JACK).size());

        lumberjack.setLocation(colony.getVacantColonyTileFor(lumberjack, Goods.FOOD));
        ore.setLocation(school);

        while (3 == getUnitList(colony, Unit.FREE_COLONIST).size() && maxTurns-- > 0) {
            school.newTurn();
        }
        assertEquals(2, getUnitList(colony, Unit.FREE_COLONIST).size());
        assertEquals(2, getUnitList(colony, Unit.MASTER_BLACKSMITH).size());

        blacksmith.setLocation(colony.getVacantColonyTileFor(blacksmith, Goods.FOOD));
        veteran.setLocation(school);

        while (2 == getUnitList(colony, Unit.FREE_COLONIST).size() && maxTurns-- > 0) {
            school.newTurn();
        }
        assertEquals(1, getUnitList(colony, Unit.FREE_COLONIST).size());
        assertEquals(2, getUnitList(colony, Unit.EXPERT_ORE_MINER).size());

        ore.setLocation(colony.getVacantColonyTileFor(ore, Goods.FOOD));

        while (1 == getUnitList(colony, Unit.FREE_COLONIST).size() && maxTurns-- > 0) {
            school.newTurn();
        }
        assertEquals(0, getUnitList(colony, Unit.FREE_COLONIST).size());
        assertEquals(2, getUnitList(colony, Unit.VETERAN_SOLDIER).size());
        colony.dispose();

    }

    public void testTwoTeachersSimple() {
        
        Colony colony = getStandardColony(10);

        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist1 = units.next();
        colonist1.setType(Unit.FREE_COLONIST);

        Unit colonist2 = units.next();
        colonist2.setType(Unit.FREE_COLONIST);

        Unit colonist3 = units.next();
        colonist3.setType(Unit.FREE_COLONIST);

        Unit colonist4 = units.next();
        colonist4.setType(Unit.FREE_COLONIST);

        Unit lumber = units.next();
        lumber.setType(Unit.EXPERT_LUMBER_JACK);

        Unit black = units.next();
        black.setType(Unit.MASTER_BLACKSMITH);

        Unit veteran = units.next();
        veteran.setType(Unit.VETERAN_SOLDIER);

        Unit ore = units.next();
        ore.setType(Unit.EXPERT_ORE_MINER);

        BuildingType schoolType = spec().getBuildingType("model.building.Schoolhouse");
        colony.addBuilding(new Building(getGame(), colony, schoolType));
        Building school = colony.getBuilding(spec().getBuildingType("model.building.Schoolhouse"));
        school.upgrade();
        school.upgrade();

        black.setLocation(school);
        ore.setLocation(school);

        assertEquals(6, getUnitList(colony, Unit.FREE_COLONIST).size());
        assertEquals(1, getUnitList(colony, Unit.MASTER_BLACKSMITH).size());
        assertEquals(1, getUnitList(colony, Unit.EXPERT_ORE_MINER).size());

        school.newTurn();
        assertEquals(6, getUnitList(colony, Unit.FREE_COLONIST).size());
        assertEquals(1, getUnitList(colony, Unit.MASTER_BLACKSMITH).size());
        assertEquals(1, getUnitList(colony, Unit.EXPERT_ORE_MINER).size());

        school.newTurn();
        assertEquals(6, getUnitList(colony, Unit.FREE_COLONIST).size());
        assertEquals(1, getUnitList(colony, Unit.MASTER_BLACKSMITH).size());
        assertEquals(1, getUnitList(colony, Unit.EXPERT_ORE_MINER).size());

        school.newTurn();
        assertEquals(6, getUnitList(colony, Unit.FREE_COLONIST).size());
        assertEquals(1, getUnitList(colony, Unit.MASTER_BLACKSMITH).size());
        assertEquals(1, getUnitList(colony, Unit.EXPERT_ORE_MINER).size());

        school.newTurn();
        assertEquals(5, getUnitList(colony, Unit.FREE_COLONIST).size());
        assertEquals(1, getUnitList(colony, Unit.MASTER_BLACKSMITH).size());
        assertEquals(2, getUnitList(colony, Unit.EXPERT_ORE_MINER).size());

        school.newTurn();
        assertEquals(5, getUnitList(colony, Unit.FREE_COLONIST).size());
        assertEquals(1, getUnitList(colony, Unit.MASTER_BLACKSMITH).size());
        assertEquals(2, getUnitList(colony, Unit.EXPERT_ORE_MINER).size());

        school.newTurn();
        assertEquals(4, getUnitList(colony, Unit.FREE_COLONIST).size());
        assertEquals(2, getUnitList(colony, Unit.MASTER_BLACKSMITH).size());
        assertEquals(2, getUnitList(colony, Unit.EXPERT_ORE_MINER).size());
        colony.dispose();
    }

    /**
     * Return a colony with a university and 10 elder statesmen
     * @return
     */
    public Colony getUniversityColony(){
        Colony colony = getStandardColony(10);

        for (Unit u : colony.getUnitList()){
            u.setType(Unit.ELDER_STATESMAN);
        }

        BuildingType schoolType = spec().getBuildingType("model.building.University");
        colony.addBuilding(new Building(getGame(), colony, schoolType));
        return colony;
    }
    
    /**
     * If there are two teachers, but just one colonist to be taught.
     */
    public void testSingleGuyTwoTeachers() {
        
        Colony colony = getUniversityColony();

        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist1 = units.next();
        colonist1.setType(Unit.FREE_COLONIST);

        Unit lumber = units.next();
        lumber.setType(Unit.EXPERT_LUMBER_JACK);

        Unit black = units.next();
        black.setType(Unit.MASTER_BLACKSMITH);

        Building school = colony.getBuilding(spec().getBuildingType("model.building.Schoolhouse"));

        // It should take 4 turns to train an expert lumber jack and 6 to train
        // a blacksmith
        // The lumber jack chould be finished teaching first.
        // But the school works for now as first come first serve
        black.setLocation(school);
        lumber.setLocation(school);

        trainForTurns(colony, black.getNeededTurnsOfTraining());
        assertEquals(0, getUnitList(colony, Unit.FREE_COLONIST).size());
        assertEquals(1, getUnitList(colony, Unit.EXPERT_LUMBER_JACK).size());
        assertEquals(2, getUnitList(colony, Unit.MASTER_BLACKSMITH).size());
        colony.dispose();
    }

    /**
     * If there are two teachers of the same kind, but just one colonist to be
     * taught, this should not mean any speed up.
     */
    public void testTwoTeachersOfSameKind() {
        
        Colony colony = getUniversityColony();
        Building school = colony.getBuilding(schoolType);
        
        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist1 = units.next();
        colonist1.setType(Unit.FREE_COLONIST);

        Unit lumberjack1 = units.next();
        lumberjack1.setType(Unit.EXPERT_LUMBER_JACK);

        Unit lumberjack2 = units.next();
        lumberjack2.setType(Unit.EXPERT_LUMBER_JACK);

        lumberjack1.setLocation(school);
        lumberjack2.setLocation(school);

        trainForTurns(colony, lumberjack1.getNeededTurnsOfTraining());
        assertEquals(0, getUnitList(colony, Unit.FREE_COLONIST).size());
        assertEquals(3, getUnitList(colony, Unit.EXPERT_LUMBER_JACK).size());
        colony.dispose();
    }

    /**
     * If there are two teachers with the same skill level, the first to be put
     * in the school should be used for teaching.
     * 
     */
    public void testSingleGuyTwoTeachers2() {
        
        Colony colony = getUniversityColony();
        Building school = colony.getBuilding(schoolType);

        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist1 = units.next();
        colonist1.setType(Unit.FREE_COLONIST);

        Unit lumber = units.next();
        lumber.setType(Unit.EXPERT_LUMBER_JACK);

        Unit ore = units.next();
        ore.setType(Unit.EXPERT_ORE_MINER);

        // It should take 3 turns to train an expert lumber jack and also 3 to
        // train a ore miner
        // First come first serve, the lumber jack wins.
        lumber.setLocation(school);
        ore.setLocation(school);

        assertEquals(1, getUnitList(colony, Unit.FREE_COLONIST).size());
        assertEquals(1, getUnitList(colony, Unit.EXPERT_LUMBER_JACK).size());
        assertEquals(1, getUnitList(colony, Unit.EXPERT_ORE_MINER).size());

        school.newTurn();
        assertEquals(1, getUnitList(colony, Unit.FREE_COLONIST).size());
        assertEquals(1, getUnitList(colony, Unit.EXPERT_LUMBER_JACK).size());
        assertEquals(1, getUnitList(colony, Unit.EXPERT_ORE_MINER).size());

        school.newTurn();
        assertEquals(1, getUnitList(colony, Unit.FREE_COLONIST).size());
        assertEquals(1, getUnitList(colony, Unit.EXPERT_LUMBER_JACK).size());
        assertEquals(1, getUnitList(colony, Unit.EXPERT_ORE_MINER).size());

        school.newTurn();
        assertEquals(1, getUnitList(colony, Unit.FREE_COLONIST).size());
        assertEquals(1, getUnitList(colony, Unit.EXPERT_LUMBER_JACK).size());
        assertEquals(1, getUnitList(colony, Unit.EXPERT_ORE_MINER).size());

        school.newTurn();
        assertEquals(0, getUnitList(colony, Unit.FREE_COLONIST).size());
        assertEquals(2, getUnitList(colony, Unit.EXPERT_LUMBER_JACK).size());
        assertEquals(1, getUnitList(colony, Unit.EXPERT_ORE_MINER).size());
        colony.dispose();
    }

    /**
     * Test that an petty criminal becomes an indentured servant
     */
    public void testTeachPettyCriminals() {
        
        Colony colony = getUniversityColony();
        Building school = colony.getBuilding(schoolType);

        Iterator<Unit> units = colony.getUnitIterator();

        Unit criminal = units.next();
        criminal.setType(Unit.PETTY_CRIMINAL);

        Unit teacher = units.next();
        teacher.setType(Unit.EXPERT_ORE_MINER);

        teacher.setLocation(school);

        // PETTY_CRIMINALS become INDENTURED_SERVANTS
        trainForTurns(colony, teacher.getNeededTurnsOfTraining(), Unit.PETTY_CRIMINAL);
        assertEquals(0, getUnitList(colony, Unit.PETTY_CRIMINAL).size());
        assertEquals(Unit.INDENTURED_SERVANT, criminal.getType());
        colony.dispose();
    }

    /**
     * The time to teach somebody does not depend on the one who is being
     * taught, but on the teacher.
     */
    public void testTeachPettyCriminalsByMaster() {
        
        Colony colony = getUniversityColony();
        Building school = colony.getBuilding(schoolType);
        
        Iterator<Unit> units = colony.getUnitIterator();

        Unit criminal = units.next();
        criminal.setType(Unit.PETTY_CRIMINAL);

        Unit teacher = units.next();
        teacher.setType(Unit.MASTER_BLACKSMITH);

        teacher.setLocation(school);

        assertEquals(teacher.getNeededTurnsOfTraining(), 4);
        trainForTurns(colony, teacher.getNeededTurnsOfTraining(), Unit.PETTY_CRIMINAL);
        assertEquals(Unit.INDENTURED_SERVANT, criminal.getType());
        colony.dispose();
    }

    /**
     * Test that an indentured servant becomes a free colonist
     * 
     */
    public void testTeachIndenturedServants() {
        
        Colony colony = getUniversityColony();
        Building school = colony.getBuilding(schoolType);
        
        Iterator<Unit> units = colony.getUnitIterator();

        Unit indenturedServant = units.next();
        indenturedServant.setType(Unit.INDENTURED_SERVANT);

        Unit teacher = units.next();
        teacher.setType(Unit.MASTER_BLACKSMITH);

        teacher.setLocation(school);
        assertEquals(teacher.getNeededTurnsOfTraining(), 4);
        trainForTurns(colony, teacher.getNeededTurnsOfTraining(), Unit.INDENTURED_SERVANT);
        // Train to become free colonist
        assertEquals(Unit.FREE_COLONIST, indenturedServant.getType());
    }

    /**
     * Progress in teaching is bound to the teacher and not the learner.
     * 
     * Moving students around does not slow education. This behavior is 
     * there to simplify gameplay.
     */
    public void testTeacherStoresProgress() {
        Colony outsideColony = getStandardColony(1, 10, 8);
        Iterator<Unit> outsideUnits = outsideColony.getUnitIterator();
        Unit outsider = outsideUnits.next();
        outsider.setType(Unit.FREE_COLONIST);
        
        Colony colony = getUniversityColony();
        Building school = colony.getBuilding(schoolType);
        Iterator<Unit> units = colony.getUnitIterator();
        Unit student = units.next();
        student.setType(Unit.FREE_COLONIST);
        Unit teacher = units.next();
        teacher.setType(Unit.EXPERT_ORE_MINER);

        
        teacher.setLocation(school);

        // Train to become free colonist
        trainForTurns(colony, teacher.getNeededTurnsOfTraining() - 1);

        // We swap the colonist with another one
        student.setLocation(outsideColony);
        outsider.setLocation(colony);

        assertEquals(1, getUnitList(colony, Unit.FREE_COLONIST).size());
        school.newTurn();
        assertEquals(0, getUnitList(colony, Unit.FREE_COLONIST).size());
        assertEquals(Unit.EXPERT_ORE_MINER, outsider.getType());
    }

    /**
     * Progress in teaching is bound to the teacher and not the learner.
     * 
     * Moving a teacher inside the colony should not reset its training.
     */
    public void testMoveTeacherInside() {
        
        Colony colony = getUniversityColony();
        Building school = colony.getBuilding(schoolType);
        
        Iterator<Unit> units = colony.getUnitIterator();
        Unit colonist = units.next();
        colonist.setType(Unit.FREE_COLONIST);
        Unit criminal = units.next();
        criminal.setType(Unit.PETTY_CRIMINAL);
        
        Unit teacher1 = units.next();
        teacher1.setType(Unit.EXPERT_ORE_MINER);
        Unit teacher2 = units.next();
        teacher2.setType(Unit.MASTER_CARPENTER);

        // The ore miner is set in the school before the carpenter (note: the
        // carpenter is the only master of skill level 1).
        // In this case, the colonist will become a miner (and the criminal 
        // will become a servant).
        teacher1.setLocation(school);
        teacher2.setLocation(school);

        // wait a little
        school.newTurn();
        school.newTurn();
        assertEquals(2, teacher1.getTurnsOfTraining());
        assertEquals(2, teacher2.getTurnsOfTraining());
        assertEquals(1, getUnitList(colony, Unit.FREE_COLONIST).size());
        assertEquals(1, getUnitList(colony, Unit.PETTY_CRIMINAL).size());
        assertEquals(1, getUnitList(colony, Unit.EXPERT_ORE_MINER).size());
        assertEquals(1, getUnitList(colony, Unit.MASTER_CARPENTER).size());

        // Now we want the colonist to be a carpenter. We just want to 
        // shuffle the teachers.
        teacher2.setLocation(colony.getVacantColonyTileFor(teacher2, Goods.FOOD));
        // outside the colony is still considered OK (same Tile)
        teacher1.putOutsideColony();

        // Passing a turn outside school does not reset training at this time
        school.newTurn();
        assertEquals(2, teacher1.getTurnsOfTraining());
        assertEquals(2, teacher2.getTurnsOfTraining());
        assertEquals(1, getUnitList(colony, Unit.FREE_COLONIST).size());
        assertEquals(1, getUnitList(colony, Unit.PETTY_CRIMINAL).size());

        // Move teacher2 back to school
        teacher2.setLocation(school);

        school.newTurn();
        assertEquals(2, teacher1.getTurnsOfTraining());
        assertEquals(3, teacher2.getTurnsOfTraining());
        assertEquals(1, getUnitList(colony, Unit.FREE_COLONIST).size());
        assertEquals(1, getUnitList(colony, Unit.PETTY_CRIMINAL).size());
        
        // Move teacher1 back to school
        teacher1.setLocation(school);

        school.newTurn();
        assertEquals(3, teacher1.getTurnsOfTraining());
        assertEquals(0, teacher2.getTurnsOfTraining());

        // Teacher1's student (petty criminal) should still be a petty criminal
        // Teacher2's student (free colonist) should have been promoted to master carpenter
        assertEquals(0, getUnitList(colony, Unit.FREE_COLONIST).size());
        assertEquals(1, getUnitList(colony, Unit.PETTY_CRIMINAL).size());
        assertEquals(0, getUnitList(colony, Unit.INDENTURED_SERVANT).size());
        assertEquals(1, getUnitList(colony, Unit.EXPERT_ORE_MINER).size());
        assertEquals(2, getUnitList(colony, Unit.MASTER_CARPENTER).size());

        school.newTurn();
        assertEquals(0, teacher1.getTurnsOfTraining());
        assertEquals(0, teacher2.getTurnsOfTraining());
        assertEquals(null, teacher2.getStudent());

        // Teacher1's student (petty criminal) should have been promoted to indentured servant
        assertEquals(0, getUnitList(colony, Unit.FREE_COLONIST).size());
        assertEquals(0, getUnitList(colony, Unit.PETTY_CRIMINAL).size());
        assertEquals(1, getUnitList(colony, Unit.INDENTURED_SERVANT).size());
        assertEquals(1, getUnitList(colony, Unit.EXPERT_ORE_MINER).size());
        assertEquals(2, getUnitList(colony, Unit.MASTER_CARPENTER).size());
        
        /**
         * Since teacher2 was move back to school first, it is
         * actually the first teacher in the unit list. Therefore
         * teacher2 will get the new student (indentured servant), and
         * teacher1 will get none.
         */
        school.newTurn();
        assertEquals(0, teacher1.getTurnsOfTraining());
        assertEquals(1, teacher2.getTurnsOfTraining());
        assertEquals(Unit.INDENTURED_SERVANT, teacher2.getStudent().getType());

    }
    
    public void testCaseTwoTeachersWithDifferentExp(){
        
        Colony colony = getUniversityColony();
        Building school = colony.getBuilding(schoolType);
        Iterator<Unit> units = colony.getUnitIterator();
        Unit colonist = units.next();
        colonist.setType(Unit.FREE_COLONIST);
        Unit teacher1 = units.next();
        teacher1.setType(Unit.EXPERT_ORE_MINER);
        Unit teacher2 = units.next();
        teacher2.setType(Unit.MASTER_CARPENTER);

        // First we let the teacher1 train for 3 turns
        teacher1.setLocation(school);
        school.newTurn();
        school.newTurn();
        school.newTurn();
        assertEquals(3, teacher1.getTurnsOfTraining());
        
        // Then teacher2 for 1 turn
        teacher1.setLocation(colony.getVacantColonyTileFor(teacher1, Goods.FOOD));
        teacher2.setLocation(school);
        school.newTurn();
        assertEquals(3, teacher1.getTurnsOfTraining());
        assertEquals(1, teacher2.getTurnsOfTraining());
        
        // If we now also add teacher2 to the school, then 
        // Teacher1 will still be the teacher in charge
        teacher1.setLocation(school);
        school.newTurn();
        
        assertEquals(3, teacher1.getTurnsOfTraining());
        assertEquals(2, teacher2.getTurnsOfTraining());
    }

    /**
     * Progress in teaching is bound to the teacher and not the learner.
     * 
     * Moving a teacher outside the colony should reset its training.
     */
    public void testMoveTeacherOutside() {
        
        Colony outsideColony = getStandardColony(1, 10, 8);
        Iterator<Unit> outsideUnits = outsideColony.getUnitIterator();
        Unit outsider = outsideUnits.next();
        outsider.setType(Unit.FREE_COLONIST);

        Colony colony = getUniversityColony();
        Building school = colony.getBuilding(schoolType);
        
        Iterator<Unit> units = colony.getUnitIterator();
        Unit colonist = units.next();
        colonist.setType(Unit.FREE_COLONIST);
        Unit criminal = units.next();
        criminal.setType(Unit.PETTY_CRIMINAL);
        
        Unit teacher1 = units.next();
        teacher1.setType(Unit.EXPERT_ORE_MINER);
        Unit teacher2 = units.next();
        teacher2.setType(Unit.MASTER_CARPENTER);

        // The ore miner is set in the school before the carpenter (note: the
        // carpenter is the only master of skill level 1).
        // In this case, the colonist will become a miner (and the criminal 
        // will become a servant).
        teacher1.setLocation(school);
        teacher2.setLocation(school);

        // wait a little
        school.newTurn();
        school.newTurn();
        assertEquals(1, getUnitList(colony, Unit.FREE_COLONIST).size());
        assertEquals(1, getUnitList(colony, Unit.PETTY_CRIMINAL).size());
        assertEquals(1, getUnitList(colony, Unit.EXPERT_ORE_MINER).size());
        assertEquals(1, getUnitList(colony, Unit.MASTER_CARPENTER).size());
        assertEquals(2, teacher1.getTurnsOfTraining());
        assertEquals(2, teacher2.getTurnsOfTraining());
        
        // Now we move the teachers somewhere else
        teacher1.setLocation(getGame().getMap().getTile(6, 8));
        teacher2.setLocation(outsideColony.getVacantColonyTileFor(teacher2, Goods.FOOD));
        assertEquals(0, teacher1.getTurnsOfTraining());
        assertEquals(0, teacher2.getTurnsOfTraining());
        assertEquals(1, getUnitList(colony, Unit.FREE_COLONIST).size());
        assertEquals(1, getUnitList(colony, Unit.PETTY_CRIMINAL).size());
        assertEquals(0, getUnitList(colony, Unit.EXPERT_ORE_MINER).size());
        assertEquals(0, getUnitList(colony, Unit.MASTER_CARPENTER).size());
        
        // Put them back here
        teacher1.setLocation(school);
        teacher2.setLocation(school);
        assertEquals(0, teacher1.getTurnsOfTraining());
        assertEquals(0, teacher2.getTurnsOfTraining());
        
        // Check that 2 new turns aren't enough for training
        school.newTurn();
        assertEquals(1, getUnitList(colony, Unit.FREE_COLONIST).size());
        assertEquals(1, getUnitList(colony, Unit.PETTY_CRIMINAL).size());
        assertEquals(1, getUnitList(colony, Unit.EXPERT_ORE_MINER).size());
        assertEquals(1, getUnitList(colony, Unit.MASTER_CARPENTER).size());

        school.newTurn();
        assertEquals(1, getUnitList(colony, Unit.FREE_COLONIST).size());
        assertEquals(1, getUnitList(colony, Unit.PETTY_CRIMINAL).size());
        assertEquals(1, getUnitList(colony, Unit.EXPERT_ORE_MINER).size());
        assertEquals(1, getUnitList(colony, Unit.MASTER_CARPENTER).size());

        school.newTurn();
        assertEquals(1, getUnitList(colony, Unit.FREE_COLONIST).size());
        assertEquals(1, getUnitList(colony, Unit.PETTY_CRIMINAL).size());
        assertEquals(1, getUnitList(colony, Unit.EXPERT_ORE_MINER).size());
        assertEquals(1, getUnitList(colony, Unit.MASTER_CARPENTER).size());

        school.newTurn();
        assertEquals(0, getUnitList(colony, Unit.FREE_COLONIST).size());
        assertEquals(0, getUnitList(colony, Unit.PETTY_CRIMINAL).size());
        assertEquals(1, getUnitList(colony, Unit.INDENTURED_SERVANT).size());
        assertEquals(2, getUnitList(colony, Unit.EXPERT_ORE_MINER).size());
        assertEquals(1, getUnitList(colony, Unit.MASTER_CARPENTER).size());
    }

    /**
     * Sons of Liberty should not influence teaching.
     */
    public void testSonsOfLiberty() {
        
        Colony colony = getUniversityColony();
        Building school = colony.getBuilding(schoolType);
        
        colony.addSoL(100);
        colony.newTurn();

        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist1 = units.next();
        colonist1.setType(Unit.FREE_COLONIST);

        Unit lumberjack = units.next();
        lumberjack.setType(Unit.EXPERT_LUMBER_JACK);

        lumberjack.setLocation(school);
        trainForTurns(colony, lumberjack.getNeededTurnsOfTraining());
        assertEquals(0, getUnitList(colony, Unit.FREE_COLONIST).size());
        assertEquals(2, getUnitList(colony, Unit.EXPERT_LUMBER_JACK).size());
    }

    /**
     * Trains partly one colonist then put another teacher.
     * 
     * Should not save progress but start all over.
     */
    public void testPartTraining() {
        
        Colony colony = getUniversityColony();
        Building school = colony.getBuilding(schoolType);
        
        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist = units.next();
        colonist.setType(Unit.FREE_COLONIST);

        Unit lumberjack = units.next();
        lumberjack.setType(Unit.EXPERT_LUMBER_JACK);

        Unit miner = units.next();
        miner.setType(Unit.EXPERT_ORE_MINER);

        // Put LumberJack in School
        lumberjack.setLocation(school);
        trainForTurns(colony, 2);

        // After 2 turns replace by miner. Progress starts from scratch.
        lumberjack.setLocation(colony.getVacantColonyTileFor(lumberjack, Goods.FOOD));
        miner.setLocation(school);
        trainForTurns(colony, miner.getNeededTurnsOfTraining());
        assertEquals(0, getUnitList(colony, Unit.FREE_COLONIST).size());
        assertEquals(2, getUnitList(colony, Unit.EXPERT_ORE_MINER).size());
        colony.dispose();
    }

    /**
     * Test that free colonists are trained before indentured servants, which
     * are preferred to petty criminals.
     * 
     */
    public void testTeachingOrder() {

        Colony colony = getUniversityColony();
        Building school = colony.getBuilding(schoolType);
        
        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist = units.next();
        colonist.setType(Unit.FREE_COLONIST);

        Unit indenturedServant = units.next();
        indenturedServant.setType(Unit.INDENTURED_SERVANT);

        Unit criminal = units.next();
        criminal.setType(Unit.PETTY_CRIMINAL);

        Unit teacher = units.next();
        teacher.setType(Unit.EXPERT_ORE_MINER);
        teacher.setLocation(school);

        // Colonist training
        school.newTurn();
        school.newTurn();
        school.newTurn();
        school.newTurn();
        assertEquals(0, getUnitList(colony, Unit.FREE_COLONIST).size());
        assertEquals(Unit.EXPERT_ORE_MINER, colonist.getType());

        // Servant training
        assertEquals(0, getUnitList(colony, Unit.FREE_COLONIST).size());
        school.newTurn();
        school.newTurn();
        school.newTurn();
        school.newTurn();
        assertEquals(0, getUnitList(colony, Unit.INDENTURED_SERVANT).size());
        assertEquals(Unit.FREE_COLONIST, indenturedServant.getType());
        indenturedServant.setLocation(getGame().getMap().getTile(10,8));

        // Criminal training
        assertEquals(0, getUnitList(colony, Unit.FREE_COLONIST).size());
        assertEquals(0, getUnitList(colony, Unit.INDENTURED_SERVANT).size());
        school.newTurn();
        school.newTurn();
        school.newTurn();
        school.newTurn();
        assertEquals(0, getUnitList(colony, Unit.PETTY_CRIMINAL).size());
        assertEquals(Unit.INDENTURED_SERVANT, criminal.getType());
    }

    /**
     * Test that an indentured servant cannot be promoted to free colonist and
     * learn a skill at the same time.
     */
    public void testTeachingDoublePromotion() {
        
        Colony colony = getUniversityColony();
        Building school = colony.getBuilding(schoolType);
        
        Iterator<Unit> units = colony.getUnitIterator();

        Unit indenturedServant = units.next();
        indenturedServant.setType(Unit.INDENTURED_SERVANT);

        Unit criminal = units.next();
        criminal.setType(Unit.PETTY_CRIMINAL);

        Unit teacher1 = units.next();
        teacher1.setType(Unit.EXPERT_ORE_MINER);

        Unit teacher2 = units.next();
        teacher2.setType(Unit.EXPERT_LUMBER_JACK);

        // set location only AFTER all types have been set!
        teacher1.setLocation(school);
        teacher2.setLocation(school);

        // Training time
        trainForTurns(colony, teacher1.getNeededTurnsOfTraining(), Unit.PETTY_CRIMINAL);

        // indentured servant should have been promoted to free colonist
        // petty criminal should have been promoted to indentured servant
        assertEquals(1, getUnitList(colony, Unit.EXPERT_ORE_MINER).size());
        assertEquals(1, getUnitList(colony, Unit.EXPERT_LUMBER_JACK).size());
        assertEquals(1, getUnitList(colony, Unit.FREE_COLONIST).size());
        assertEquals(1, getUnitList(colony, Unit.INDENTURED_SERVANT).size());
        assertEquals(0, getUnitList(colony, Unit.PETTY_CRIMINAL).size());
        assertEquals(Unit.FREE_COLONIST, indenturedServant.getType());
        assertEquals(Unit.INDENTURED_SERVANT, criminal.getType());
        
        // Train again
        school.newTurn();
        school.newTurn();
        school.newTurn();
        school.newTurn();
        assertEquals(2, getUnitList(colony, Unit.EXPERT_ORE_MINER).size());
        assertEquals(1, getUnitList(colony, Unit.EXPERT_LUMBER_JACK).size());
        assertEquals(1, getUnitList(colony, Unit.FREE_COLONIST).size());
        assertEquals(0, getUnitList(colony, Unit.INDENTURED_SERVANT).size());
        assertEquals(0, getUnitList(colony, Unit.PETTY_CRIMINAL).size());
        assertEquals(Unit.EXPERT_ORE_MINER, indenturedServant.getType());
        assertEquals(Unit.FREE_COLONIST, criminal.getType());
    }

}
