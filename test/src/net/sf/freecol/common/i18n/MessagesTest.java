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

package net.sf.freecol.common.i18n;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;

import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.Monarch;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitLabelType;
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


    @Override
    public void tearDown(){
        Messages.loadMessageBundle(Locale.US);
    }

    public void testMessageString() {
        assertTrue("Press enter in order to end the turn."
            .equals(Messages.message("infoPanel.endTurn")));
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
        Messages.loadMessageBundle(Locale.US);

        assertTrue("Trade Advisor"
            .equals(Messages.message("reportTradeAction.name")));

        Messages.loadMessageBundle(Locale.GERMANY);

        assertTrue("Handelsberater"
            .equals(Messages.message("reportTradeAction.name")));
    }

    // Tests if messages with special chars (like $) are well processed
    public void testMessageWithSpecialChars() {
        final String key = HistoryEvent.HistoryEventType.FOUND_COLONY.getDescriptionKey();
        try {
            assertEquals("You establish the colony of %colony%.",
                         Messages.message(key));
        } catch (Exception e) {
            fail("Message fail");
            throw e;
        }
        final String colNameWithSpecialChars="$specialColName\\";
        try {
            assertEquals("You establish the colony of $specialColName\\.",
                         Messages.message(StringTemplate
                             .template(key)
                             .addName("%colony%", colNameWithSpecialChars)));
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("Illegal group reference")) {
                fail("Does not process messages with special chars");
            }
            throw e;
        }
    }

    public void testStringTemplates() {
        final Game game = getGame();
        Messages.loadMessageBundle(Locale.US);

        // template with key not in message bundle
        StringTemplate s1 = StringTemplate.key("!no.such.string.template");
        assertTrue(s1.getId()
            .equals(Messages.message(s1)));

        StringTemplate s2 = StringTemplate.key("model.tile.plains.name");
        assertTrue("Plains"
            .equals(Messages.message(s2)));

        StringTemplate t1 = AbstractGoods
            .getLabel(game.getSpecification().getPrimaryFoodType(), 100);
        assertEquals(2, t1.getKeys().size());
        assertEquals(2, t1.getReplacements().size());
        assertEquals(StringTemplate.TemplateType.KEY,
                     t1.getReplacements().get(0).getTemplateType());
        assertEquals(StringTemplate.TemplateType.NAME,
                     t1.getReplacements().get(1).getTemplateType());
        assertTrue("model.abstractGoods.label"
            .equals(t1.getId()));
        assertTrue("100 Food"
            .equals(Messages.message(t1)));

        StringTemplate t2 = StringTemplate.label(" / ")
            .add("model.goods.food.name")
            .addName("xyz");
        assertTrue("Food / xyz"
            .equals(Messages.message(t2)));

        game.setMap(getTestMap());
        Colony colony = getStandardColony();
        assertTrue("New Amsterdam"
            .equals(colony.getName()));

        StringTemplate t3 = StringTemplate.template("model.building.locationLabel")
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
            assertTrue("abcother|xyz".equals(Messages.message(StringTemplate
                        .template("some.key")
                        .addAmount("%number%", d))));
        }
        // apply English rules
        Messages.setGrammaticalNumber(NumberRules.PLURAL_NUMBER_RULE);
        for (double d : numbers) {
            if (d == 1) {
                assertTrue("abcone|xyz".equals(Messages.message(StringTemplate
                            .template("some.key")
                            .addAmount("%number%", d))));
            } else {
                assertTrue("abcother|xyz".equals(Messages.message(StringTemplate
                            .template("some.key")
                            .addAmount("%number%", d))));
            }
        }
    }

    public void testReplaceArbitraryTag() {
        final String testKey = "model.player.startGame";
        StringTemplate template = StringTemplate.template(testKey)
            .add("%direction%", "east");
        String expected = "After months at sea, you have finally arrived off the "
            + "coast of an unknown continent. Sail eastward in order to discover "
            + "the New World and to claim it for the Crown.";
        assertTrue(expected.equals(Messages.message(template)));

        template = StringTemplate.template(testKey)
            .add("%direction%", "west");
        expected = "After months at sea, you have finally arrived off the "
            + "coast of an unknown continent. Sail westward in order to discover "
            + "the New World and to claim it for the Crown.";
        assertTrue(expected.equals(Messages.message(template)));

        template = StringTemplate.template(testKey)
            .add("%direction%", "whatever");
        expected = "After months at sea, you have finally arrived off the "
            + "coast of an unknown continent. Sail into the wind in order to discover "
            + "the New World and to claim it for the Crown.";
        assertTrue(expected.equals(Messages.message(template)));
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
        assertTrue("1 piece of artillery".equals(Messages.message(template)));
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

    public void testREFMessages() {
        StringTemplate template = StringTemplate
            .template(Monarch.MonarchAction.ADD_TO_REF.getTextKey())
            .addAmount("%number%", 1)
            .addNamed("%unit%", kingsRegular);
        String expected = "The Crown has added 1 King's Regular"
            + " to the Royal Expeditionary Force."
            + " Colonial leaders express concern.";
        assertTrue(expected.equals(Messages.message(template)));

        template = StringTemplate
            .template(Monarch.MonarchAction.ADD_TO_REF.getTextKey())
            .addAmount("%number%", 2)
            .addNamed("%unit%", artillery);
        expected = "The Crown has added 2 Pieces of Artillery"
            + " to the Royal Expeditionary Force."
            + " Colonial leaders express concern.";
        assertTrue(expected.equals(Messages.message(template)));

        template = StringTemplate
            .template(Monarch.MonarchAction.ADD_TO_REF.getTextKey())
            .addAmount("%number%", 3)
            .addNamed("%unit%", manOWar);
        expected = "The Crown has added 3 Men of War"
            + " to the Royal Expeditionary Force."
            + " Colonial leaders express concern.";
        assertTrue(expected.equals(Messages.message(template)));
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
        ServerPlayer dutch = (ServerPlayer)game
            .getPlayerByNationId("model.nation.dutch");
        Nation refNation = dutch.getNation().getREFNation();
        ServerPlayer dutchREF = new ServerPlayer(game, false, refNation, 
                                                 null, null);
        ServerPlayer sioux = (ServerPlayer)game
            .getPlayerByNationId("model.nation.sioux");
        Unit unit;

        // King's regulars
        unit = new ServerUnit(game, null, dutchREF, kingsRegular, defaultRole);
        assertEquals("King's Regular",
            unit.getDescription());
        assertEquals("Dutch Royal Expeditionary Force King's Regular",
            unit.getDescription(UnitLabelType.NATIONAL));
        assertEquals("Dutch Royal Expeditionary Force King's Regular",
            unit.getDescription(UnitLabelType.FULL));

        unit.changeRole(infantryRole, 1);
        assertEquals("Infantry",
            unit.getDescription());
        assertEquals("Dutch Royal Expeditionary Force Infantry",
            unit.getDescription(UnitLabelType.NATIONAL));
        assertEquals("Dutch Royal Expeditionary Force Infantry (50 Muskets)",
            unit.getDescription(UnitLabelType.FULL));

        unit.changeRole(cavalryRole, 1);
        assertEquals("Cavalry",
            unit.getDescription());
        assertEquals("Dutch Royal Expeditionary Force Cavalry",
            unit.getDescription(UnitLabelType.NATIONAL));
        assertEquals("Dutch Royal Expeditionary Force Cavalry (50 Muskets 50 Horses)",
            unit.getDescription(UnitLabelType.FULL));

        // Colonial regulars
        unit = new ServerUnit(game, null, dutch, colonialRegular, defaultRole);
        assertEquals("Colonial Regular",
            unit.getDescription());
        assertEquals("Dutch Colonial Regular",
            unit.getDescription(UnitLabelType.NATIONAL));
        assertEquals("Dutch Colonial Regular",
            unit.getDescription(UnitLabelType.FULL));

        unit.changeRole(soldierRole, 1);
        assertEquals("Continental Army",
            unit.getDescription());
        assertEquals("Dutch Continental Army",
            unit.getDescription(UnitLabelType.NATIONAL));
        assertEquals("Dutch Continental Army (50 Muskets)",
            unit.getDescription(UnitLabelType.FULL));

        unit.changeRole(dragoonRole, 1);
        assertEquals("Continental Cavalry",
            unit.getDescription());
        assertEquals("Dutch Continental Cavalry",
            unit.getDescription(UnitLabelType.NATIONAL));
        assertEquals("Dutch Continental Cavalry (50 Muskets 50 Horses)",
            unit.getDescription(UnitLabelType.FULL));

        // Veteran Soldiers
        unit = new ServerUnit(game, null, dutch, veteranSoldier, soldierRole);
        assertEquals("Veteran Soldier",
            unit.getDescription());
        assertEquals("Dutch Veteran Soldier",
            unit.getDescription(UnitLabelType.NATIONAL));
        assertEquals("Dutch Veteran Soldier (50 Muskets)",
            unit.getDescription(UnitLabelType.FULL));

        unit.changeRole(defaultRole, 0);
        assertEquals("Veteran Soldier",
            unit.getDescription());
        assertEquals("Dutch Veteran Soldier",
            unit.getDescription(UnitLabelType.NATIONAL));
        assertEquals("Dutch Veteran Soldier (no muskets)",
            unit.getDescription(UnitLabelType.FULL));

        unit.changeRole(dragoonRole, 1);
        assertEquals("Veteran Dragoon",
            unit.getDescription());
        assertEquals("Dutch Veteran Dragoon",
            unit.getDescription(UnitLabelType.NATIONAL));
        assertEquals("Dutch Veteran Dragoon (50 Muskets 50 Horses)",
            unit.getDescription(UnitLabelType.FULL));

        unit.setName("Davy Crockett");
        assertEquals("Davy Crockett (Veteran Dragoon)",
            unit.getDescription());
        assertEquals("Davy Crockett (Dutch Veteran Dragoon)",
            unit.getDescription(UnitLabelType.NATIONAL));
        assertEquals("Davy Crockett (Dutch Veteran Dragoon/50 Muskets 50 Horses)",
            unit.getDescription(UnitLabelType.FULL));

        // Indian Braves
        unit = new ServerUnit(game, null, sioux, brave, defaultRole);
        assertEquals("Brave",
            unit.getDescription());
        assertEquals("Sioux Brave",
            unit.getDescription(UnitLabelType.NATIONAL));
        assertEquals("Sioux Brave",
            unit.getDescription(UnitLabelType.FULL));

        unit.changeRole(armedBraveRole, 1);
        assertEquals("Armed Brave",
            unit.getDescription());
        assertEquals("Sioux Armed Brave",
            unit.getDescription(UnitLabelType.NATIONAL));
        assertEquals("Sioux Armed Brave (25 Muskets)",
            unit.getDescription(UnitLabelType.FULL));

        unit.changeRole(mountedBraveRole, 1);
        assertEquals("Mounted Brave",
            unit.getDescription());
        assertEquals("Sioux Mounted Brave",
            unit.getDescription(UnitLabelType.NATIONAL));
        assertEquals("Sioux Mounted Brave (25 Horses)",
            unit.getDescription(UnitLabelType.FULL));

        unit.changeRole(nativeDragoonRole, 1);
        assertEquals("Native Dragoon",
            unit.getDescription());
        assertEquals("Sioux Native Dragoon",
            unit.getDescription(UnitLabelType.NATIONAL));
        assertEquals("Sioux Native Dragoon (25 Muskets 25 Horses)",
            unit.getDescription(UnitLabelType.FULL));

        unit.setName("Chingachgook");
        assertEquals("Chingachgook (Native Dragoon)",
            unit.getDescription());
        assertEquals("Chingachgook (Sioux Native Dragoon)",
            unit.getDescription(UnitLabelType.NATIONAL));
        assertEquals("Chingachgook (Sioux Native Dragoon/25 Muskets 25 Horses)",
            unit.getDescription(UnitLabelType.FULL));

        // Hardy Pioneers
        unit = new ServerUnit(game, null, dutch, hardyPioneer, pioneerRole);
        assertEquals("Hardy Pioneer",
            unit.getDescription());
        assertEquals("Dutch Hardy Pioneer (100 Tools)",
            unit.getDescription(UnitLabelType.NATIONAL));
        assertEquals("Dutch Hardy Pioneer (100 Tools)",
            unit.getDescription(UnitLabelType.FULL));

        unit.changeRole(defaultRole, 0);
        assertEquals("Hardy Pioneer",
            unit.getDescription());
        assertEquals("Dutch Hardy Pioneer",
            unit.getDescription(UnitLabelType.NATIONAL));
        assertEquals("Dutch Hardy Pioneer (no tools)",
            unit.getDescription(UnitLabelType.FULL));

        unit.setName("Daniel Boone");
        assertEquals("Daniel Boone (Hardy Pioneer)",
            unit.getDescription());
        assertEquals("Daniel Boone (Dutch Hardy Pioneer)",
            unit.getDescription(UnitLabelType.NATIONAL));
        assertEquals("Daniel Boone (Dutch Hardy Pioneer/no tools)",
            unit.getDescription(UnitLabelType.FULL));

        // Jesuit Missionaries
        unit = new ServerUnit(game, null, dutch, jesuitMissionary, 
                              missionaryRole);
        assertEquals("Jesuit Missionary",
            unit.getDescription());
        assertEquals("Dutch Jesuit Missionary",
            unit.getDescription(UnitLabelType.NATIONAL));
        assertEquals("Dutch Jesuit Missionary (1 Bible)",
            unit.getDescription(UnitLabelType.FULL));

        unit.changeRole(defaultRole, 0);
        assertEquals("Jesuit Missionary",
            unit.getDescription());
        assertEquals("Dutch Jesuit Missionary",
            unit.getDescription(UnitLabelType.NATIONAL));
        assertEquals("Dutch Jesuit Missionary (not commissioned)",
            unit.getDescription(UnitLabelType.FULL));

        // Free Colonists
        unit = new ServerUnit(game, null, dutch, freeColonist, defaultRole);
        assertEquals("Free Colonist",
            unit.getDescription());
        assertEquals("Dutch Free Colonist",
            unit.getDescription(UnitLabelType.NATIONAL));
        assertEquals("Dutch Free Colonist",
            unit.getDescription(UnitLabelType.FULL));

        unit.setRole(soldierRole);
        assertEquals("Soldier (Free Colonist)",
            unit.getDescription());
        assertEquals("Dutch Soldier (Free Colonist)",
            unit.getDescription(UnitLabelType.NATIONAL));
        assertEquals("Dutch Soldier (Free Colonist/50 Muskets)",
            unit.getDescription(UnitLabelType.FULL));

        unit.setName("John Doe");
        assertEquals("John Doe (Soldier/Free Colonist)",
            unit.getDescription());
        assertEquals("John Doe (Dutch Soldier/Free Colonist)",
            unit.getDescription(UnitLabelType.NATIONAL));
        assertEquals("John Doe (Dutch Soldier/Free Colonist/50 Muskets)",
            unit.getDescription(UnitLabelType.FULL));

        // Expert
        unit = new ServerUnit(game, null, dutch, masterCarpenter, defaultRole);
        assertEquals("Master Carpenter",
            unit.getDescription());
        assertEquals("Dutch Master Carpenter",
            unit.getDescription(UnitLabelType.NATIONAL));
        assertEquals("Dutch Master Carpenter",
            unit.getDescription(UnitLabelType.FULL));

        unit.setRole(missionaryRole);
        assertEquals("Missionary (Master Carpenter)",
            unit.getDescription());
        assertEquals("Dutch Missionary (Master Carpenter)",
            unit.getDescription(UnitLabelType.NATIONAL));
        assertEquals("Dutch Missionary (Master Carpenter/1 Bible)",
            unit.getDescription(UnitLabelType.FULL));

        // Treasure Train
        unit = new ServerUnit(game, null, dutch, treasureTrain, defaultRole);
        unit.setTreasureAmount(4567);
        assertEquals("Treasure Train",
            unit.getDescription());
        assertEquals("Dutch Treasure Train",
            unit.getDescription(UnitLabelType.NATIONAL));
        assertEquals("Dutch Treasure Train (4567 gold)",
            unit.getDescription(UnitLabelType.FULL));

        unit.setName("The Gold of El Dorado");
        assertEquals("The Gold of El Dorado (Treasure Train)",
            unit.getDescription());
        assertEquals("The Gold of El Dorado (Dutch Treasure Train)",
            unit.getDescription(UnitLabelType.NATIONAL));
        assertEquals("The Gold of El Dorado (Dutch Treasure Train/4567 gold)",
            unit.getDescription(UnitLabelType.FULL));

        // Caravel
        unit = new ServerUnit(game, null, dutch, caravel, defaultRole);
        assertEquals("Caravel",
            unit.getDescription());
        assertEquals("Dutch Caravel",
            unit.getDescription(UnitLabelType.NATIONAL));
        assertEquals("Dutch Caravel",
            unit.getDescription(UnitLabelType.FULL));

        unit.setName("Santa Maria");
        assertEquals("Santa Maria (Caravel)",
            unit.getDescription());
        assertEquals("Santa Maria (Dutch Caravel)",
            unit.getDescription(UnitLabelType.NATIONAL));
        assertEquals("Santa Maria (Dutch Caravel)",
            unit.getDescription(UnitLabelType.FULL));
    }
}
