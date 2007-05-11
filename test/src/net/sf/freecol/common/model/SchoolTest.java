package net.sf.freecol.common.model;

import java.util.Iterator;

import net.sf.freecol.util.test.FreeColTestCase;

public class SchoolTest extends FreeColTestCase {

    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

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

        Building school = colony.getBuilding(Building.SCHOOLHOUSE);

        school.setLevel(Building.HOUSE);

        ore.setLocation(school);

        // It should take 4 turns to train a EXPERT
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn(); // 1
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn(); // 2
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn(); // 3
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn(); // 4
        assertEquals(0, colony.getUnitList(Unit.FREE_COLONIST).size());

        assertEquals(Unit.EXPERT_ORE_MINER, colonist.getType());
    }

    public void testCollege() {
        
        Colony colony = getStandardColony(2);

        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist = units.next();
        colonist.setType(Unit.FREE_COLONIST);

        Unit blackSmith = units.next();
        blackSmith.setType(Unit.MASTER_BLACKSMITH);

        Building school = colony.getBuilding(Building.SCHOOLHOUSE);

        school.setLevel(Building.SHOP);

        blackSmith.setLocation(school);

        // It should take 6 turns to train a MASTER, i.e. college level
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn(); // 1
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn(); // 2
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn(); // 3
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn(); // 4
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn(); // 5
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn(); // 6
        assertEquals(0, colony.getUnitList(Unit.FREE_COLONIST).size());

        assertEquals(Unit.MASTER_BLACKSMITH, colonist.getType());
    }

    public void testUniversity() {
        
        Colony colony = getStandardColony(2);

        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist = units.next();
        colonist.setType(Unit.FREE_COLONIST);

        Unit elder = units.next();
        elder.setType(Unit.ELDER_STATESMAN);

        Building school = colony.getBuilding(Building.SCHOOLHOUSE);

        school.setLevel(Building.FACTORY);

        elder.setLocation(school);

        // It should take 8 turns to train a person at university level
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn(); // 1
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn(); // 2
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn(); // 3
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn(); // 4
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn(); // 5
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn(); // 6
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn(); // 7
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn(); // 8
        assertEquals(0, colony.getUnitList(Unit.FREE_COLONIST).size());

        assertEquals(Unit.ELDER_STATESMAN, colonist.getType());
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
        Building school = colony.getBuilding(Building.SCHOOLHOUSE);
        school.setLevel(Building.SHOP);

        blacksmith.setLocation(school);
        lumberjack.setLocation(school);

        // It should not take more than 15 turns (my guess) to get the whole
        // story over with.
        int maxTurns = 15;

        while (4 == colony.getUnitList(Unit.FREE_COLONIST).size() && maxTurns-- > 0) {
            school.newTurn();
        }
        assertEquals(3, colony.getUnitList(Unit.FREE_COLONIST).size());
        assertEquals(1, colony.getUnitList(Unit.MASTER_BLACKSMITH).size());
        assertEquals(2, colony.getUnitList(Unit.EXPERT_LUMBER_JACK).size());

        lumberjack.setLocation(colony.getVacantColonyTileFor(lumberjack, Goods.FOOD));
        ore.setLocation(school);

        while (3 == colony.getUnitList(Unit.FREE_COLONIST).size() && maxTurns-- > 0) {
            school.newTurn();
        }
        assertEquals(2, colony.getUnitList(Unit.FREE_COLONIST).size());
        assertEquals(2, colony.getUnitList(Unit.MASTER_BLACKSMITH).size());

        blacksmith.setLocation(colony.getVacantColonyTileFor(blacksmith, Goods.FOOD));
        veteran.setLocation(school);

        while (2 == colony.getUnitList(Unit.FREE_COLONIST).size() && maxTurns-- > 0) {
            school.newTurn();
        }
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        assertEquals(2, colony.getUnitList(Unit.EXPERT_ORE_MINER).size());

        ore.setLocation(colony.getVacantColonyTileFor(ore, Goods.FOOD));

        while (1 == colony.getUnitList(Unit.FREE_COLONIST).size() && maxTurns-- > 0) {
            school.newTurn();
        }
        assertEquals(0, colony.getUnitList(Unit.FREE_COLONIST).size());
        assertEquals(2, colony.getUnitList(Unit.VETERAN_SOLDIER).size());

    }

    public void testTwoTeachersSimple() {
        
        Colony colony = getStandardColony(8);

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

        Building school = colony.getBuilding(Building.SCHOOLHOUSE);

        school.setLevel(Building.FACTORY);

        black.setLocation(school);
        ore.setLocation(school);

        assertEquals(4, colony.getUnitList(Unit.FREE_COLONIST).size());
        assertEquals(1, colony.getUnitList(Unit.MASTER_BLACKSMITH).size());
        assertEquals(1, colony.getUnitList(Unit.EXPERT_ORE_MINER).size());

        school.newTurn();
        assertEquals(4, colony.getUnitList(Unit.FREE_COLONIST).size());
        assertEquals(1, colony.getUnitList(Unit.MASTER_BLACKSMITH).size());
        assertEquals(1, colony.getUnitList(Unit.EXPERT_ORE_MINER).size());

        school.newTurn();
        assertEquals(4, colony.getUnitList(Unit.FREE_COLONIST).size());
        assertEquals(1, colony.getUnitList(Unit.MASTER_BLACKSMITH).size());
        assertEquals(1, colony.getUnitList(Unit.EXPERT_ORE_MINER).size());

        school.newTurn();
        assertEquals(4, colony.getUnitList(Unit.FREE_COLONIST).size());
        assertEquals(1, colony.getUnitList(Unit.MASTER_BLACKSMITH).size());
        assertEquals(1, colony.getUnitList(Unit.EXPERT_ORE_MINER).size());

        school.newTurn();
        assertEquals(3, colony.getUnitList(Unit.FREE_COLONIST).size());
        assertEquals(1, colony.getUnitList(Unit.MASTER_BLACKSMITH).size());
        assertEquals(2, colony.getUnitList(Unit.EXPERT_ORE_MINER).size());

        school.newTurn();
        assertEquals(3, colony.getUnitList(Unit.FREE_COLONIST).size());
        assertEquals(1, colony.getUnitList(Unit.MASTER_BLACKSMITH).size());
        assertEquals(2, colony.getUnitList(Unit.EXPERT_ORE_MINER).size());

        school.newTurn();
        assertEquals(2, colony.getUnitList(Unit.FREE_COLONIST).size());
        assertEquals(2, colony.getUnitList(Unit.MASTER_BLACKSMITH).size());
        assertEquals(2, colony.getUnitList(Unit.EXPERT_ORE_MINER).size());
    }

    /**
     * If there are two teachers, but just one colonist to be taught.
     */
    public void testSingleGuyTwoTeachers() {
        
        Colony colony = getStandardColony(5);

        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist1 = units.next();
        colonist1.setType(Unit.FREE_COLONIST);

        Unit lumber = units.next();
        lumber.setType(Unit.EXPERT_LUMBER_JACK);

        Unit black = units.next();
        black.setType(Unit.MASTER_BLACKSMITH);

        Unit veteran = units.next();
        veteran.setType(Unit.VETERAN_SOLDIER);

        Unit ore = units.next();
        ore.setType(Unit.EXPERT_ORE_MINER);

        Building school = colony.getBuilding(Building.SCHOOLHOUSE);

        school.setLevel(Building.SHOP);

        // It should take 4 turns to train an expert lumber jack and 6 to train
        // a blacksmith
        // The lumber jack chould be finished teaching first.
        // But the school works for now as first come first serve
        black.setLocation(school);
        lumber.setLocation(school);

        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn(); // 1
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn(); // 2
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn(); // 3
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn(); // 4
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn(); // 5
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn(); // 6

        assertEquals(0, colony.getUnitList(Unit.FREE_COLONIST).size());
        assertEquals(1, colony.getUnitList(Unit.EXPERT_LUMBER_JACK).size());
        assertEquals(2, colony.getUnitList(Unit.MASTER_BLACKSMITH).size());
    }

    /**
     * If there are two teachers of the same kind, but just one colonist to be
     * taught, this should not mean any speed up.
     */
    public void testTwoTeachersOfSameKind() {
        
        Colony colony = getStandardColony(3);

        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist1 = units.next();
        colonist1.setType(Unit.FREE_COLONIST);

        Unit lumberjack1 = units.next();
        lumberjack1.setType(Unit.EXPERT_LUMBER_JACK);

        Unit lumberjack2 = units.next();
        lumberjack2.setType(Unit.EXPERT_LUMBER_JACK);

        Building school = colony.getBuilding(Building.SCHOOLHOUSE);

        school.setLevel(Building.SHOP);

        lumberjack1.setLocation(school);
        lumberjack2.setLocation(school);

        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn(); // 1
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn(); // 2
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn(); // 3
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn(); // 4

        assertEquals(0, colony.getUnitList(Unit.FREE_COLONIST).size());
        assertEquals(3, colony.getUnitList(Unit.EXPERT_LUMBER_JACK).size());
    }

    /**
     * If there are two teachers with the same skill level, the first to be put
     * in the school should be used for teaching.
     * 
     */
    public void testSingleGuyTwoTeachers2() {
        Colony colony = getStandardColony(5);

        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist1 = units.next();
        colonist1.setType(Unit.FREE_COLONIST);

        Unit lumber = units.next();
        lumber.setType(Unit.EXPERT_LUMBER_JACK);

        Unit black = units.next();
        black.setType(Unit.MASTER_BLACKSMITH);

        Unit veteran = units.next();
        veteran.setType(Unit.VETERAN_SOLDIER);

        Unit ore = units.next();
        ore.setType(Unit.EXPERT_ORE_MINER);

        Building school = colony.getBuilding(Building.SCHOOLHOUSE);

        school.setLevel(Building.SHOP);

        // It should take 3 turns to train an expert lumber jack and also 3 to
        // train a ore miner
        // First come first serve, the lumber jack wins.
        lumber.setLocation(school);
        ore.setLocation(school);

        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        assertEquals(1, colony.getUnitList(Unit.EXPERT_LUMBER_JACK).size());
        assertEquals(1, colony.getUnitList(Unit.EXPERT_ORE_MINER).size());

        school.newTurn();
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        assertEquals(1, colony.getUnitList(Unit.EXPERT_LUMBER_JACK).size());
        assertEquals(1, colony.getUnitList(Unit.EXPERT_ORE_MINER).size());

        school.newTurn();
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        assertEquals(1, colony.getUnitList(Unit.EXPERT_LUMBER_JACK).size());
        assertEquals(1, colony.getUnitList(Unit.EXPERT_ORE_MINER).size());

        school.newTurn();
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        assertEquals(1, colony.getUnitList(Unit.EXPERT_LUMBER_JACK).size());
        assertEquals(1, colony.getUnitList(Unit.EXPERT_ORE_MINER).size());

        school.newTurn();
        assertEquals(0, colony.getUnitList(Unit.FREE_COLONIST).size());
        assertEquals(2, colony.getUnitList(Unit.EXPERT_LUMBER_JACK).size());
        assertEquals(1, colony.getUnitList(Unit.EXPERT_ORE_MINER).size());
    }

    /**
     * Test that an petty criminal becomes an indentured servant
     */
    public void testTeachPettyCriminals() {
        Colony colony = getStandardColony(2);

        Iterator<Unit> units = colony.getUnitIterator();

        Unit criminal = units.next();
        criminal.setType(Unit.PETTY_CRIMINAL);

        Unit teacher = units.next();
        teacher.setType(Unit.EXPERT_ORE_MINER);

        Building school = colony.getBuilding(Building.SCHOOLHOUSE);
        school.setLevel(Building.SHOP);

        teacher.setLocation(school);

        // PETTY_CRIMINALS become INDENTURED_SERVANTS
        assertEquals(1, colony.getUnitList(Unit.PETTY_CRIMINAL).size());
        school.newTurn();
        assertEquals(1, colony.getUnitList(Unit.PETTY_CRIMINAL).size());
        school.newTurn();
        assertEquals(1, colony.getUnitList(Unit.PETTY_CRIMINAL).size());
        school.newTurn();
        assertEquals(1, colony.getUnitList(Unit.PETTY_CRIMINAL).size());
        school.newTurn();
        assertEquals(0, colony.getUnitList(Unit.PETTY_CRIMINAL).size());
        assertEquals(Unit.INDENTURED_SERVANT, criminal.getType());
    }

    /**
     * The time to teach somebody does not depend on the one who is being
     * taught, but on the teacher.
     */
    public void testTeachPettyCriminalsByMaster() {
        Colony colony = getStandardColony(2);

        Iterator<Unit> units = colony.getUnitIterator();

        Unit criminal = units.next();
        criminal.setType(Unit.PETTY_CRIMINAL);

        Unit teacher = units.next();
        teacher.setType(Unit.MASTER_BLACKSMITH);

        Building school = colony.getBuilding(Building.SCHOOLHOUSE);
        school.setLevel(Building.SHOP);

        teacher.setLocation(school);

        // It takes two turns longer to train a petty criminal by a master than
        // by an expert!
        assertEquals(1, colony.getUnitList(Unit.PETTY_CRIMINAL).size());
        school.newTurn();
        assertEquals(1, colony.getUnitList(Unit.PETTY_CRIMINAL).size());
        school.newTurn();
        assertEquals(1, colony.getUnitList(Unit.PETTY_CRIMINAL).size());
        school.newTurn();
        assertEquals(1, colony.getUnitList(Unit.PETTY_CRIMINAL).size());
        school.newTurn();
        assertEquals(1, colony.getUnitList(Unit.PETTY_CRIMINAL).size());
        school.newTurn();
        assertEquals(1, colony.getUnitList(Unit.PETTY_CRIMINAL).size());
        school.newTurn();
        assertEquals(0, colony.getUnitList(Unit.PETTY_CRIMINAL).size());
        assertEquals(Unit.INDENTURED_SERVANT, criminal.getType());
    }

    /**
     * Test that an indentured servant becomes a free colonist
     * 
     */
    public void testTeachIndenturedServants() {
        Colony colony = getStandardColony(2);

        Iterator<Unit> units = colony.getUnitIterator();

        Unit indenturedServant = units.next();
        indenturedServant.setType(Unit.INDENTURED_SERVANT);

        Unit teacher = units.next();
        teacher.setType(Unit.EXPERT_ORE_MINER);

        Building school = colony.getBuilding(Building.SCHOOLHOUSE);
        school.setLevel(Building.SHOP);

        teacher.setLocation(school);

        // Train to become free colonist
        assertEquals(1, colony.getUnitList(Unit.INDENTURED_SERVANT).size());
        school.newTurn();
        assertEquals(1, colony.getUnitList(Unit.INDENTURED_SERVANT).size());
        school.newTurn();
        assertEquals(1, colony.getUnitList(Unit.INDENTURED_SERVANT).size());
        school.newTurn();
        assertEquals(1, colony.getUnitList(Unit.INDENTURED_SERVANT).size());
        school.newTurn();
        assertEquals(0, colony.getUnitList(Unit.INDENTURED_SERVANT).size());

        assertEquals(Unit.FREE_COLONIST, indenturedServant.getType());
    }

    /**
     * Progress in teaching is bound to the teacher and not the learner.
     * 
     */
    public void testTeacherStoresProgress() {
        Colony outsideColony = getStandardColony(1);
        Iterator<Unit> outsideUnits = outsideColony.getUnitIterator();
        Unit outsider = outsideUnits.next();
        outsider.setType(Unit.FREE_COLONIST);

        Colony colony = getStandardColony(2);
        Iterator<Unit> units = colony.getUnitIterator();
        Unit student = units.next();
        student.setType(Unit.FREE_COLONIST);
        Unit teacher = units.next();
        teacher.setType(Unit.EXPERT_ORE_MINER);

        Building school = colony.getBuilding(Building.SCHOOLHOUSE);
        school.setLevel(Building.SHOP);
        teacher.setLocation(school);

        // Train to become free colonist
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn();
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn();
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn();

        // We swap the colonist with another one
        student.setLocation(outsideColony);
        outsider.setLocation(colony);

        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn();
        assertEquals(0, colony.getUnitList(Unit.FREE_COLONIST).size());
        assertEquals(Unit.EXPERT_ORE_MINER, outsider.getType());
    }

    /**
     * Sons of Liberty should not influence teaching.
     */
    public void testSonsOfLiberty() {
        Colony colony = getStandardColony(2);
        colony.addSoL(100);
        colony.newTurn();

        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist1 = units.next();
        colonist1.setType(Unit.FREE_COLONIST);

        Unit lumberjack = units.next();
        lumberjack.setType(Unit.EXPERT_LUMBER_JACK);

        Building school = colony.getBuilding(Building.SCHOOLHOUSE);
        school.setLevel(Building.SHOP);

        lumberjack.setLocation(school);

        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn(); // 1
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn(); // 2
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn(); // 3
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn(); // 4

        assertEquals(0, colony.getUnitList(Unit.FREE_COLONIST).size());
        assertEquals(2, colony.getUnitList(Unit.EXPERT_LUMBER_JACK).size());
    }

    /**
     * Trains partly one colonist then put another teacher.
     * 
     * Should not save progress but start all over.
     */
    public void testPartTraining() {
        
        // Create colony
        Colony colony = getStandardColony(3);

        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist = units.next();
        colonist.setType(Unit.FREE_COLONIST);

        Unit lumberjack = units.next();
        lumberjack.setType(Unit.EXPERT_LUMBER_JACK);

        Unit miner = units.next();
        miner.setType(Unit.EXPERT_ORE_MINER);

        Building school = colony.getBuilding(Building.SCHOOLHOUSE);
        school.setLevel(Building.HOUSE);

        // Put LumberJack in School
        lumberjack.setLocation(school);

        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn();

        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn();

        // After 2 turns replace by miner. Progress starts from scratch.
        lumberjack.setLocation(colony.getVacantColonyTileFor(lumberjack, Goods.FOOD));
        miner.setLocation(school);

        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn();

        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn();

        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn();

        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn();

        assertEquals(0, colony.getUnitList(Unit.FREE_COLONIST).size());
        assertEquals(2, colony.getUnitList(Unit.EXPERT_ORE_MINER).size());
    }

    /**
     * Test that free colonists are trained before indentured servants, which
     * are preferred to petty criminals.
     * 
     */
    public void testTeachingOrder() {
        Colony colony = getStandardColony(4);
        Building school = colony.getBuilding(Building.SCHOOLHOUSE);
        school.setLevel(Building.SHOP);

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
        assertEquals(0, colony.getUnitList(Unit.FREE_COLONIST).size());
        assertEquals(Unit.EXPERT_ORE_MINER, colonist.getType());

        // Servant training
        assertEquals(0, colony.getUnitList(Unit.FREE_COLONIST).size());
        school.newTurn();
        school.newTurn();
        school.newTurn();
        school.newTurn();
        assertEquals(0, colony.getUnitList(Unit.INDENTURED_SERVANT).size());
        assertEquals(Unit.FREE_COLONIST, indenturedServant.getType());
        indenturedServant.setLocation(getGame().getMap().getTile(10,8));

        // Criminal training
        assertEquals(0, colony.getUnitList(Unit.FREE_COLONIST).size());
        assertEquals(0, colony.getUnitList(Unit.INDENTURED_SERVANT).size());
        school.newTurn();
        school.newTurn();
        school.newTurn();
        school.newTurn();
        assertEquals(0, colony.getUnitList(Unit.PETTY_CRIMINAL).size());
        assertEquals(Unit.INDENTURED_SERVANT, criminal.getType());
    }

    /**
     * Test that an indentured servant cannot be promoted to free colonist and
     * learn a skill at the same time.
     */
    public void testTeachingDoublePromotion() {
        Colony colony = getStandardColony(4);
        Building school = colony.getBuilding(Building.SCHOOLHOUSE);
        school.setLevel(Building.SHOP);

        Iterator<Unit> units = colony.getUnitIterator();

        Unit indenturedServant = units.next();
        indenturedServant.setType(Unit.INDENTURED_SERVANT);

        Unit criminal = units.next();
        criminal.setType(Unit.PETTY_CRIMINAL);

        Unit teacher1 = units.next();
        teacher1.setType(Unit.EXPERT_ORE_MINER);
        teacher1.setLocation(school);

        Unit teacher2 = units.next();
        teacher2.setType(Unit.EXPERT_LUMBER_JACK);
        teacher2.setLocation(school);

        // Training time
        school.newTurn();
        school.newTurn();
        school.newTurn();
        school.newTurn();
        assertEquals(1, colony.getUnitList(Unit.EXPERT_ORE_MINER).size());
        assertEquals(1, colony.getUnitList(Unit.EXPERT_LUMBER_JACK).size());
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        assertEquals(1, colony.getUnitList(Unit.INDENTURED_SERVANT).size());
        assertEquals(0, colony.getUnitList(Unit.PETTY_CRIMINAL).size());
        assertEquals(Unit.FREE_COLONIST, indenturedServant.getType());
        assertEquals(Unit.INDENTURED_SERVANT, criminal.getType());
        
        // Train again
        school.newTurn();
        school.newTurn();
        school.newTurn();
        school.newTurn();
        assertEquals(2, colony.getUnitList(Unit.EXPERT_ORE_MINER).size());
        assertEquals(1, colony.getUnitList(Unit.EXPERT_LUMBER_JACK).size());
        assertEquals(1, colony.getUnitList(Unit.FREE_COLONIST).size());
        assertEquals(0, colony.getUnitList(Unit.INDENTURED_SERVANT).size());
        assertEquals(0, colony.getUnitList(Unit.PETTY_CRIMINAL).size());
        assertEquals(Unit.EXPERT_ORE_MINER, indenturedServant.getType());
        assertEquals(Unit.FREE_COLONIST, criminal.getType());
    }

}
