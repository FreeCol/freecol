/**
 *  Copyright (C) 2002-2014  The FreeCol Team
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

package net.sf.freecol.client.gui.i18n;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;

import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;


public class MessagesTest extends FreeColTestCase {

    public static final String noSuchKey = "should.not.exist.and.thus.return.null";

    private static final Role armedBraveRole
        = spec().getRole("model.role.armedBrave");
    private static final Role cavalryRole
        = spec().getRole("model.role.cavalry");
    private static final Role defaultRole
        = spec().getDefaultRole();
    private static final Role dragoonRole
        = spec().getRole("model.role.dragoon");
    private static final Role infantryRole
        = spec().getRole("model.role.infantry");
    private static final Role missionaryRole
        = spec().getRole("model.role.missionary");
    private static final Role mountedBraveRole
        = spec().getRole("model.role.mountedBrave");
    private static final Role nativeDragoonRole
        = spec().getRole("model.role.nativeDragoon");
    private static final Role pioneerRole
        = spec().getRole("model.role.pioneer");
    private static final Role scoutRole
        = spec().getRole("model.role.scout");
    private static final Role soldierRole
        = spec().getRole("model.role.soldier");

    private static final UnitType artillery
        = spec().getUnitType("model.unit.artillery");
    private static final UnitType brave
        = spec().getUnitType("model.unit.brave");
    private static final UnitType caravel
        = spec().getUnitType("model.unit.caravel");
    private static final UnitType colonialRegular
        = spec().getUnitType("model.unit.colonialRegular");
    private static final UnitType freeColonist
        = spec().getUnitType("model.unit.freeColonist");
    private static final UnitType hardyPioneer
        = spec().getUnitType("model.unit.hardyPioneer");
    private static final UnitType jesuitMissionary
        = spec().getUnitType("model.unit.jesuitMissionary");
    private static final UnitType kingsRegular
        = spec().getUnitType("model.unit.kingsRegular");
    private static final UnitType manOWar
        = spec().getUnitType("model.unit.manOWar");
    private static final UnitType masterCarpenter
        = spec().getUnitType("model.unit.masterCarpenter");
    private static final UnitType seasonedScout
        = spec().getUnitType("model.unit.seasonedScout");
    private static final UnitType treasureTrain
        = spec().getUnitType("model.unit.treasureTrain");
    private static final UnitType veteranSoldier
        = spec().getUnitType("model.unit.veteranSoldier");


    public void tearDown(){
        Messages.setMessageBundle(Locale.US);
    }

    public void testMessageString() {
        assertTrue("Press enter in order to end the turn."
            .equals(Messages.message("infoPanel.endTurnPanel.text")));
        assertTrue("Trade Advisor"
            .equals(Messages.message("reportTradeAction.name")));

        // With parameters
        assertTrue("Score: %score%    |    Gold: %gold%    |    Tax: %tax%%    |    Year: %year%"
            .equals(Messages.message("menuBar.statusLine")));

        // Long String
        assertTrue("Food is necessary to feed your colonists and to breed horses. A new colonist is born whenever a colony has 200 units of food or more."
            .equals(Messages.message("model.goods.food.description")));

        // Message not found
        assertTrue(noSuchKey.equals(Messages.message(noSuchKey)));
    }


    public void testChangeLocaleSettings() {
        Messages.setMessageBundle(Locale.US);

        assertTrue("Trade Advisor"
            .equals(Messages.message("reportTradeAction.name")));

        Messages.setMessageBundle(Locale.GERMANY);

        assertTrue("Handelsberater"
            .equals(Messages.message("reportTradeAction.name")));
    }

    // Tests if messages with special chars (like $) are well processed
    public void testMessageWithSpecialChars(){
        String errMsg = "Error setting up test.";
        String expected = "You establish the colony of %colony%.";
        String message = Messages.message("model.history.FOUND_COLONY");
        assertTrue(errMsg, expected.equals(message));

        String colNameWithSpecialChars="$specialColName\\";
        errMsg = "Wrong message";
        expected = "You establish the colony of $specialColName\\.";
        try {
            message = Messages.message(StringTemplate.template("model.history.FOUND_COLONY")
                                       .addName("%colony%", colNameWithSpecialChars));
        } catch(IllegalArgumentException e){
            if (e.getMessage().contains("Illegal group reference")){
                fail("Does not process messages with special chars");
            }
            throw e;
        }
        assertTrue(errMsg, expected.equals(message));
    }

    public void testStringTemplates() {
        Messages.setMessageBundle(Locale.US);

        // template with key not in message bundle
        StringTemplate s1 = StringTemplate.key("!no.such.string.template");
        assertTrue(s1.getId()
            .equals(Messages.message(s1)));

        StringTemplate s2 = StringTemplate.key("model.tile.plains.name");
        assertTrue("Plains"
            .equals(Messages.message(s2)));

        StringTemplate t1 = StringTemplate.template("model.goods.goodsAmount")
            .add("%goods%", "model.goods.food.name")
            .addName("%amount%", "100");
        assertEquals(2, t1.getKeys().size());
        assertEquals(2, t1.getReplacements().size());
        assertEquals(StringTemplate.TemplateType.KEY,
                     t1.getReplacements().get(0).getTemplateType());
        assertEquals(StringTemplate.TemplateType.NAME,
                     t1.getReplacements().get(1).getTemplateType());
        assertTrue("model.goods.goodsAmount"
            .equals(t1.getId()));
        assertTrue("100 Food"
            .equals(Messages.message(t1)));

        StringTemplate t2 = StringTemplate.label(" / ")
            .add("model.goods.food.name")
            .addName("xyz");
        assertTrue("Food / xyz"
            .equals(Messages.message(t2)));

        Game game = getGame();
        game.setMap(getTestMap());
        Colony colony = getStandardColony();
        assertTrue("New Amsterdam"
            .equals(colony.getName()));

        StringTemplate t3 = StringTemplate.template("inLocation")
            .addName("%location%", colony.getName());
        assertTrue("In New Amsterdam"
            .equals(Messages.message(t3)));

        StringTemplate t4 = StringTemplate.label("")
            .addName("(")
            .add("model.goods.food.name")
            .addName(")");
        assertTrue("(Food)"
            .equals(Messages.message(t4)));
    }

    public void testReplaceGarbage() {
        // random garbage enclosed in double brackets should be
        // removed
        String mapping = "some.key={{}}abc   {{xyz}}def{{123|567}}\n";
        ByteArrayInputStream stream = new ByteArrayInputStream(mapping.getBytes());
        try {
            Messages.loadMessages(stream);
        } catch (IOException ioe) { fail(); }
        assertTrue("abc   def"
            .equals(Messages.message("some.key")));
    }

    public void testReplaceNumber() {

        double[] numbers = new double[] {
            -1.3, -1, -0.5, 0, 0.33, 1, 1.2, 2, 2.7, 3, 3.4, 11, 13, 27, 100
        };
        String mapping = "some.key=abc{{plural:%number%|zero=zero|one=one|two=two"
            + "|few=few|many=many|other=other}}|xyz";
        ByteArrayInputStream stream = new ByteArrayInputStream(mapping.getBytes());
        try {
            Messages.loadMessages(stream);
        } catch (IOException ioe) { fail(); }

        // default Number is Other
        Messages.setGrammaticalNumber(NumberRules.OTHER_NUMBER_RULE);
        for (double d : numbers) {
            assertTrue("abcother|xyz"
                .equals(Messages.message(StringTemplate.template("some.key")
                        .addAmount("%number%", d))));
        }
        // apply English rules
        Messages.setGrammaticalNumber(NumberRules.PLURAL_NUMBER_RULE);
        for (double d : numbers) {
            if (d == 1) {
                assertTrue("abcone|xyz"
                    .equals(Messages.message(StringTemplate.template("some.key")
                            .addAmount("%number%", d))));
            } else {
                assertTrue("abcother|xyz"
                    .equals(Messages.message(StringTemplate.template("some.key")
                            .addAmount("%number%", d))));
            }
        }
    }

    public void testReplaceArbitraryTag() {
        StringTemplate template = StringTemplate.template("tutorial.startGame")
            .add("%direction%", "east");
        String expected = "After months at sea, you have finally arrived off the "
            + "coast of an unknown continent. Sail eastward in order to discover "
            + "the New World and to claim it for the Crown.";
        assertTrue(expected
            .equals(Messages.message(template)));

        template = StringTemplate.template("tutorial.startGame")
            .add("%direction%", "west");
        expected = "After months at sea, you have finally arrived off the "
            + "coast of an unknown continent. Sail westward in order to discover "
            + "the New World and to claim it for the Crown.";
        assertTrue(expected
            .equals(Messages.message(template)));

        template = StringTemplate.template("tutorial.startGame")
            .add("%direction%", "whatever");
        expected = "After months at sea, you have finally arrived off the "
            + "coast of an unknown continent. Sail into the wind in order to discover "
            + "the New World and to claim it for the Crown.";
        assertTrue(expected
            .equals(Messages.message(template)));
    }

    public void testReplaceChoicesPlural() {
        String mapping = "some.key=This is {{plural:%number%|one=a test|other=one of several tests"
            + "|default=not much of a test}}.\n"
            + "unit.template=%number% {{plural:%number%|%unit%}}\n"
            + "unit.key={{plural:%number%|one=piece of artillery|other=pieces of artillery|"
            + "default=artillery}}";
        ByteArrayInputStream stream = new ByteArrayInputStream(mapping.getBytes());
        try {
            Messages.loadMessages(stream);
        } catch (IOException ioe) { fail(); }

        assertTrue("artillery"
            .equals(Messages.message("unit.key")));

        assertTrue("This is one of several tests."
            .equals(Messages.message(StringTemplate.template("some.key")
                                                   .addAmount("%number%", 0))));
        assertTrue("This is a test."
            .equals(Messages.message(StringTemplate.template("some.key")
                                                   .addAmount("%number%", 1))));
        assertTrue("This is one of several tests."
            .equals(Messages.message(StringTemplate.template("some.key")
                                                   .addAmount("%number%", 2))));
        assertTrue("This is one of several tests."
            .equals(Messages.message(StringTemplate.template("some.key")
                                                   .addAmount("%number%", 24))));

        StringTemplate template = StringTemplate.template("unit.template")
            .addAmount("%number%", 1)
            .add("%unit%", "unit.key");
        assertTrue("1 piece of artillery"
            .equals(Messages.message(template)));
    }

    public void testReplaceChoicesGrammar() {
        String mapping = "key.france={{randomTag:%randomKey%|country=France|people=French|"
            + "default=French people}}\n"
            + "greeting1=The {{otherRandomTag:default|%nation%}} are happy to see you.\n"
            + "greeting2=The {{otherRandomTag:people|%nation%}} are happy to see you.\n";

        ByteArrayInputStream stream = new ByteArrayInputStream(mapping.getBytes());
        try {
            Messages.loadMessages(stream);
        } catch (IOException ioe) { fail(); }

        assertTrue("French people"
            .equals(Messages.message("key.france")));

        StringTemplate t1 = StringTemplate.template("key.france")
            .add("%randomKey%", "country");
        assertTrue("France"
            .equals(Messages.message(t1)));

        StringTemplate t2 = StringTemplate.template("greeting1")
            .add("%nation%", "key.france");
        assertTrue("The French people are happy to see you."
            .equals(Messages.message(t2)));

        StringTemplate t3 = StringTemplate.template("greeting2")
            .add("%nation%", "key.france");
        assertTrue("The French are happy to see you."
            .equals(Messages.message(t3)));
    }

    public void testNestedChoices() {
        String mapping = "key1=%colony% tuottaa tuotetta "
            + "{{tag:acc|%goods%}}.\n"
            + "key2={{plural:%amount%|one=ruoka|other=ruokaa|"
            + "default={{tag:|acc=viljaa|default=Vilja}}}}\n"
            + "key3={{tag:|acc=viljaa|default={{plural:%amount%|one=ruoka|other=ruokaa|default=Ruoka}}}}\n";

        ByteArrayInputStream stream = new ByteArrayInputStream(mapping.getBytes());
        try {
            Messages.loadMessages(stream);
        } catch (IOException ioe) { fail(); }

        StringTemplate t = StringTemplate.template("key1")
            .addName("%colony%", "someColony")
            .add("%goods%", "key2");
        assertTrue("someColony tuottaa tuotetta viljaa."
            .equals(Messages.message(t)));
        assertTrue("Ruoka"
            .equals(Messages.message(StringTemplate.key("key3"))));
        assertTrue("Ruoka"
            .equals(Messages.message("key3")));
    }

    public void testTurnChoices() {
        String mapping = "monarch={{turn:%turn%|1492=bob|SPRING 1493=anson"
            + "|AUTUMN 1493-1588=paul|1589-SPRING 1612=james"
            + "|AUTUMN 1612-AUTUMN 1667=nathan|default=fred}}";

        ByteArrayInputStream stream = new ByteArrayInputStream(mapping.getBytes());
        try {
            Messages.loadMessages(stream);
        } catch (IOException ioe) { fail(); }

        StringTemplate t = StringTemplate.template("monarch")
            .addName("%turn%", Turn.toString(1));
        assertTrue("bob"
            .equals(Messages.message(t)));

        t = StringTemplate.template("monarch")
            .addName("%turn%", Turn.toString(2));
        assertTrue("anson"
            .equals(Messages.message(t)));

        t = StringTemplate.template("monarch")
            .addName("%turn%", "AUTUMN 1493");
        assertTrue("paul"
            .equals(Messages.message(t)));

        t = StringTemplate.template("monarch")
            .addName("%turn%", Turn.toString(100));
        assertTrue("james"
            .equals(Messages.message(t)));

        t = StringTemplate.template("monarch")
            .addName("%turn%", Turn.toString(150));
        assertTrue("nathan"
            .equals(Messages.message(t)));

        t = StringTemplate.template("monarch")
            .addName("%turn%", "YEAR 1624");
        assertTrue("nathan"
            .equals(Messages.message(t)));

        t = StringTemplate.template("monarch")
            .addName("%turn%", Turn.toString(1000));
        assertTrue("fred"
            .equals(Messages.message(t)));
    }

    public void testREFMessages() {
        StringTemplate template
            = StringTemplate.template("model.monarch.action.ADD_TO_REF")
                            .addAmount("%number%", 1)
                            .add("%unit%", kingsRegular.getNameKey());
        String expected = "The Crown has added 1 King's Regular"
            + " to the Royal Expeditionary Force."
            + " Colonial leaders express concern.";
        assertTrue(expected
            .equals(Messages.message(template)));

        template = StringTemplate.template("model.monarch.action.ADD_TO_REF")
                                 .addAmount("%number%", 2)
                                 .add("%unit%", artillery.getNameKey());
        expected = "The Crown has added 2 Pieces of Artillery"
            + " to the Royal Expeditionary Force."
            + " Colonial leaders express concern.";
        assertTrue(expected
            .equals(Messages.message(template)));

        template = StringTemplate.template("model.monarch.action.ADD_TO_REF")
                                 .addAmount("%number%", 3)
                                 .add("%unit%", manOWar.getNameKey());
        expected = "The Crown has added 3 Men of War"
            + " to the Royal Expeditionary Force."
            + " Colonial leaders express concern.";
        assertTrue(expected
            .equals(Messages.message(template)));
    }

    public void testAbstractUnitDescription() {
        AbstractUnit au = new AbstractUnit("model.unit.merchantman",
                                           Specification.DEFAULT_ROLE_ID, 1);
        assertTrue("one Merchantman".equals(au.getDescription()));
    }

    public void testUnitDescription() {
        // Unit.getDescription/getFullDescription are just wrappers around
        // Messages.getTemplate/getFullTemplate.
        Game game = getStandardGame();
        ServerPlayer dutch = (ServerPlayer)game.getPlayer("model.nation.dutch");
        ServerPlayer dutchREF = new ServerPlayer(game, "dutchREF", false,
            dutch.getNation().getREFNation(), null, null);
        ServerPlayer sioux = (ServerPlayer)game.getPlayer("model.nation.sioux");
        Unit unit;

        // King's regulars
        unit = new ServerUnit(game, null, dutchREF, kingsRegular, defaultRole);

        assertTrue("King's Regular"
            .equals(unit.getDescription()));
        assertTrue("Dutch Royal Expeditionary Force King's Regular"
            .equals(unit.getFullDescription(false)));
        assertTrue("Dutch Royal Expeditionary Force King's Regular"
            .equals(unit.getFullDescription(true)));

        unit.changeRole(infantryRole, 1);
        assertTrue("Infantry"
            .equals(unit.getDescription()));
        assertTrue("Dutch Royal Expeditionary Force Infantry"
            .equals(unit.getFullDescription(false)));
        assertTrue("Dutch Royal Expeditionary Force Infantry (50 Muskets)"
            .equals(unit.getFullDescription(true)));

        unit.changeRole(cavalryRole, 1);
        assertTrue("Cavalry"
            .equals(unit.getDescription()));
        assertTrue("Dutch Royal Expeditionary Force Cavalry"
            .equals(unit.getFullDescription(false)));
        assertTrue("Dutch Royal Expeditionary Force Cavalry (50 Muskets 50 Horses)"
            .equals(unit.getFullDescription(true)));

        // Colonial regulars
        unit = new ServerUnit(game, null, dutch, colonialRegular, defaultRole);

        assertTrue("Colonial Regular"
            .equals(unit.getDescription()));
        assertTrue("Dutch Colonial Regular"
            .equals(unit.getFullDescription(false)));
        assertTrue("Dutch Colonial Regular"
            .equals(unit.getFullDescription(true)));

        unit.changeRole(soldierRole, 1);
        assertTrue("Continental Army"
            .equals(unit.getDescription()));
        assertTrue("Dutch Continental Army"
            .equals(unit.getFullDescription(false)));
        assertTrue("Dutch Continental Army (50 Muskets)"
            .equals(unit.getFullDescription(true)));

        unit.changeRole(dragoonRole, 1);
        assertTrue("Continental Cavalry"
            .equals(unit.getDescription()));
        assertTrue("Dutch Continental Cavalry"
            .equals(unit.getFullDescription(false)));
        assertTrue("Dutch Continental Cavalry (50 Muskets 50 Horses)"
            .equals(unit.getFullDescription(true)));

        // Veteran Soldiers
        unit = new ServerUnit(game, null, dutch, veteranSoldier, soldierRole);

        assertTrue("Veteran Soldier"
            .equals(unit.getDescription()));
        assertTrue("Dutch Veteran Soldier"
            .equals(unit.getFullDescription(false)));
        assertTrue("Dutch Veteran Soldier (50 Muskets)"
            .equals(unit.getFullDescription(true)));

        unit.changeRole(defaultRole, 0);
        assertTrue("Veteran Soldier"
            .equals(unit.getDescription()));
        assertTrue("Dutch Veteran Soldier (no muskets)"
            .equals(unit.getFullDescription(false)));
        assertTrue("Dutch Veteran Soldier (no muskets)"
            .equals(unit.getFullDescription(true)));

        unit.changeRole(dragoonRole, 1);
        assertTrue("Veteran Dragoon"
            .equals(unit.getDescription()));
        assertTrue("Dutch Veteran Dragoon"
            .equals(unit.getFullDescription(false)));
        assertTrue("Dutch Veteran Dragoon (50 Muskets 50 Horses)"
            .equals(unit.getFullDescription(true)));

        unit.setName("Davy Crockett");
        assertTrue("Davy Crockett (Veteran Dragoon)"
            .equals(unit.getDescription()));
        assertTrue("Davy Crockett (Dutch Veteran Dragoon)"
            .equals(unit.getFullDescription(false)));
        assertTrue("Davy Crockett (Dutch Veteran Dragoon/50 Muskets 50 Horses)"
            .equals(unit.getFullDescription(true)));

        // Indian Braves
        unit = new ServerUnit(game, null, sioux, brave, defaultRole);

        assertTrue("Brave"
            .equals(unit.getDescription()));
        assertTrue("Sioux Brave"
            .equals(unit.getFullDescription(false)));
        assertTrue("Sioux Brave"
            .equals(unit.getFullDescription(true)));

        unit.changeRole(armedBraveRole, 1);
        assertTrue("Armed Brave"
            .equals(unit.getDescription()));
        assertTrue("Sioux Armed Brave"
            .equals(unit.getFullDescription(false)));
        assertTrue("Sioux Armed Brave (25 Muskets)"
            .equals(unit.getFullDescription(true)));

        unit.changeRole(mountedBraveRole, 1);
        assertTrue("Mounted Brave"
            .equals(unit.getDescription()));
        assertTrue("Sioux Mounted Brave"
            .equals(unit.getFullDescription(false)));
        assertTrue("Sioux Mounted Brave (25 Horses)"
            .equals(unit.getFullDescription(true)));

        unit.changeRole(nativeDragoonRole, 1);
        assertTrue("Native Dragoon"
            .equals(unit.getDescription()));
        assertTrue("Sioux Native Dragoon"
            .equals(unit.getFullDescription(false)));
        assertTrue("Sioux Native Dragoon (25 Muskets 25 Horses)"
            .equals(unit.getFullDescription(true)));

        unit.setName("Chingachgook");
        assertTrue("Chingachgook (Native Dragoon)"
            .equals(unit.getDescription()));
        assertTrue("Chingachgook (Sioux Native Dragoon)"
            .equals(unit.getFullDescription(false)));
        assertTrue("Chingachgook (Sioux Native Dragoon/25 Muskets 25 Horses)"
            .equals(unit.getFullDescription(true)));

        // Hardy Pioneers
        unit = new ServerUnit(game, null, dutch, hardyPioneer, pioneerRole);

        assertTrue("Hardy Pioneer"
            .equals(unit.getDescription()));
        assertTrue("Dutch Hardy Pioneer (100 Tools)"
            .equals(unit.getFullDescription(false)));
        assertTrue("Dutch Hardy Pioneer (100 Tools)"
            .equals(unit.getFullDescription(true)));

        unit.changeRole(defaultRole, 0);
        assertTrue("Hardy Pioneer"
            .equals(unit.getDescription()));
        assertTrue("Dutch Hardy Pioneer (no tools)"
            .equals(unit.getFullDescription(false)));
        assertTrue("Dutch Hardy Pioneer (no tools)"
            .equals(unit.getFullDescription(true)));

        unit.setName("Daniel Boone");
        assertTrue("Daniel Boone (Hardy Pioneer)"
            .equals(unit.getDescription()));
        assertTrue("Daniel Boone (Dutch Hardy Pioneer/no tools)"
            .equals(unit.getFullDescription(false)));
        assertTrue("Daniel Boone (Dutch Hardy Pioneer/no tools)"
            .equals(unit.getFullDescription(true)));

        // Jesuit Missionaries
        unit = new ServerUnit(game, null, dutch, jesuitMissionary, 
                              missionaryRole);

        assertTrue("Jesuit Missionary"
            .equals(unit.getDescription()));
        assertTrue("Dutch Jesuit Missionary"
            .equals(unit.getFullDescription(false)));
        assertTrue("Dutch Jesuit Missionary (1 Bible)"
            .equals(unit.getFullDescription(true)));

        unit.changeRole(defaultRole, 0);
        assertTrue("Jesuit Missionary"
            .equals(unit.getDescription()));
        assertTrue("Dutch Jesuit Missionary (not commissioned)"
            .equals(unit.getFullDescription(false)));
        assertTrue("Dutch Jesuit Missionary (not commissioned)"
            .equals(unit.getFullDescription(true)));

        // Free Colonists
        unit = new ServerUnit(game, null, dutch, freeColonist, defaultRole);

        assertTrue("Free Colonist"
            .equals(unit.getDescription()));
        assertTrue("Dutch Free Colonist"
            .equals(unit.getFullDescription(false)));
        assertTrue("Dutch Free Colonist"
            .equals(unit.getFullDescription(true)));

        unit.setRole(soldierRole);
        assertTrue("Free Colonist Soldier" // FIXME: "Solider (Free Colonist)"
            .equals(unit.getDescription()));
        assertTrue("Dutch Soldier (Free Colonist)"
            .equals(unit.getFullDescription(false)));
        assertTrue("Dutch Free Colonist (50 Muskets)"
            .equals(unit.getFullDescription(true)));

        unit.setName("John Doe");
        assertTrue("John Doe (Free Colonist Soldier)" // FIXME "John Doe (Solider/Free Colonist)"
            .equals(unit.getDescription()));
        assertTrue("John Doe (Dutch Soldier/Free Colonist)"
            .equals(unit.getFullDescription(false)));
        assertTrue("John Doe (Dutch Free Colonist/50 Muskets)" // FIXME: John Doe (Dutch Soldier/Free Colonist/50 Muskets)"
            .equals(unit.getFullDescription(true)));

        // Expert
        unit = new ServerUnit(game, null, dutch, masterCarpenter, defaultRole);

        assertTrue("Master Carpenter"
            .equals(unit.getDescription()));
        assertTrue("Dutch Master Carpenter"
            .equals(unit.getFullDescription(false)));
        assertTrue("Dutch Master Carpenter"
            .equals(unit.getFullDescription(true)));

        unit.setRole(missionaryRole);
        assertTrue("Master Carpenter Missionary" // FIXME "Missionary (Master Carpenter)"
            .equals(unit.getDescription()));
        assertTrue("Dutch Missionary (Master Carpenter)"
            .equals(unit.getFullDescription(false)));
        assertTrue("Dutch Master Carpenter (1 Bible)" // FIXME: "Dutch Missionary (Master Carpenter/1 Bible)"
            .equals(unit.getFullDescription(true)));

        // Treasure Train
        unit = new ServerUnit(game, null, dutch, treasureTrain, defaultRole);
        unit.setTreasureAmount(4567);

        assertTrue("Treasure Train"
            .equals(unit.getDescription()));
        assertTrue("Dutch Treasure Train (4567 gold)"
            .equals(unit.getFullDescription(false)));
        assertTrue("Dutch Treasure Train (4567 gold)"
            .equals(unit.getFullDescription(true)));

        unit.setName("The Gold of El Dorado");
        assertTrue("The Gold of El Dorado (Treasure Train)"
            .equals(unit.getDescription()));
        assertTrue("The Gold of El Dorado (Dutch Treasure Train/4567 gold)"
            .equals(unit.getFullDescription(false)));
        assertTrue("The Gold of El Dorado (Dutch Treasure Train/4567 gold)"
            .equals(unit.getFullDescription(true)));

        // Caravel
        unit = new ServerUnit(game, null, dutch, caravel, defaultRole);

        assertTrue("Caravel"
            .equals(unit.getDescription()));
        assertTrue("Dutch Caravel"
            .equals(unit.getFullDescription(false)));
        assertTrue("Dutch Caravel"
            .equals(unit.getFullDescription(true)));

        unit.setName("Santa Maria");
        assertTrue("Santa Maria (Caravel)"
            .equals(unit.getDescription()));
        assertTrue("Santa Maria (Dutch Caravel)"
            .equals(unit.getFullDescription(false)));
        assertTrue("Santa Maria (Dutch Caravel)"
            .equals(unit.getFullDescription(true)));
    }
}
