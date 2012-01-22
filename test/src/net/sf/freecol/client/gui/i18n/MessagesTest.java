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

package net.sf.freecol.client.gui.i18n;

import java.io.ByteArrayInputStream;
import java.util.Locale;

import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.server.model.ServerUnit;

public class MessagesTest extends FreeColTestCase {

    public static final String noSuchKey = "should.not.exist.and.thus.return.null";

    public void tearDown(){
        Messages.setMessageBundle(Locale.US);
    }

    public void testMessageString() {

    	assertEquals("Press enter in order to end the turn.", Messages.message("infoPanel.endTurnPanel.text"));
        assertEquals("Trade Advisor", Messages.message("reportTradeAction.name"));

        // With parameters
        assertEquals("Score: %score%    |    Gold: %gold%    |    Tax: %tax%%    |    Year: %year%",
                     Messages.message("menuBar.statusLine"));

        // Long String
        assertEquals("Food is necessary to feed your colonists and to breed horses. "
                     + "A new colonist is born whenever a colony has 200 units of food or more.",
                     Messages.message("model.goods.food.description"));

        // Message not found
        assertEquals(noSuchKey, Messages.message(noSuchKey));
    }


    public void testChangeLocaleSettings() {
        Messages.setMessageBundle(Locale.US);

        assertEquals("Trade Advisor", Messages.message("reportTradeAction.name"));

        Messages.setMessageBundle(Locale.GERMANY);

        assertEquals("Handelsberater", Messages.message("reportTradeAction.name"));
    }

    // Tests if messages with special chars (like $) are well processed
    public void testMessageWithSpecialChars(){
    	String errMsg = "Error setting up test.";
    	String expected = "You establish the colony of %colony%.";
        String message = Messages.message("model.history.FOUND_COLONY");
    	assertEquals(errMsg, expected, message);

        String colNameWithSpecialChars="$specialColName\\";
        errMsg = "Wrong message";
        expected = "You establish the colony of $specialColName\\.";
        try{
            message = Messages.message(StringTemplate.template("model.history.FOUND_COLONY")
                                       .addName("%colony%", colNameWithSpecialChars));
        }
        catch(IllegalArgumentException e){
            if(e.getMessage().contains("Illegal group reference")){
                fail("Does not process messages with special chars");
            }
            throw e;
        }
        assertEquals(errMsg, expected, message);
    }

    public void testStringTemplates() {

        Messages.setMessageBundle(Locale.US);

        // template with key not in message bundle
	StringTemplate s1 = StringTemplate.key("!no.such.string.template");
        assertEquals(s1.getId(), Messages.message(s1));

	StringTemplate s2 = StringTemplate.key("model.tile.plains.name");
        assertEquals("Plains", Messages.message(s2));

	StringTemplate t1 = StringTemplate.template("model.goods.goodsAmount")
	    .add("%goods%", "model.goods.food.name")
	    .addName("%amount%", "100");
        assertEquals(2, t1.getKeys().size());
        assertEquals(2, t1.getReplacements().size());
        assertEquals(StringTemplate.TemplateType.KEY,
                     t1.getReplacements().get(0).getTemplateType());
        assertEquals(StringTemplate.TemplateType.NAME,
                     t1.getReplacements().get(1).getTemplateType());
        assertEquals("model.goods.goodsAmount", t1.getId());
        assertEquals("100 Food", Messages.message(t1));

	StringTemplate t2 = StringTemplate.label(" / ")
	    .add("model.goods.food.name")
	    .addName("xyz");
        assertEquals("Food / xyz", Messages.message(t2));

        Game game = getGame();
    	game.setMap(getTestMap());
    	Colony colony = getStandardColony();
        assertEquals("New Amsterdam", colony.getName());

	StringTemplate t3 = StringTemplate.template("inLocation")
	    .addName("%location%", colony.getName());
        assertEquals("In New Amsterdam", Messages.message(t3));

        StringTemplate t4 = StringTemplate.label("")
            .addName("(")
            .add("model.goods.food.name")
            .addName(")");
        assertEquals("(Food)", Messages.message(t4));

    }

    public void testReplaceGarbage() {
        // random garbage enclosed in double brackets should be
        // removed
        String mapping = "some.key={{}}abc   {{xyz}}def{{123|567}}\n";
        ByteArrayInputStream stream = new ByteArrayInputStream(mapping.getBytes());
        Messages.loadResources(stream);
        assertEquals("abc   def", Messages.message("some.key"));
    }

    public void testReplaceNumber() {

        double[] numbers = new double[] {
            -1.3, -1, -0.5, 0, 0.33, 1, 1.2, 2, 2.7, 3, 3.4, 11, 13, 27, 100
        };
        String mapping = "some.key=abc{{plural:%number%|zero=zero|one=one|two=two"
            + "|few=few|many=many|other=other}}|xyz";
        ByteArrayInputStream stream = new ByteArrayInputStream(mapping.getBytes());
        Messages.loadResources(stream);

        // default Number is Other
        Messages.setGrammaticalNumber(NumberRules.OTHER_NUMBER_RULE);
        for (double d : numbers) {
            assertEquals("abcother|xyz", Messages.message(StringTemplate.template("some.key")
                                                          .addAmount("%number%", d)));
        }
        // apply English rules
        Messages.setGrammaticalNumber(NumberRules.PLURAL_NUMBER_RULE);
        for (double d : numbers) {
            if (d == 1) {
                assertEquals("abcone|xyz", Messages.message(StringTemplate.template("some.key")
                                                            .addAmount("%number%", d)));
            } else {
                assertEquals("abcother|xyz", Messages.message(StringTemplate.template("some.key")
                                                              .addAmount("%number%", d)));
            }
        }

    }

    public void testReplaceArbitraryTag() {
        StringTemplate template = StringTemplate.template("tutorial.startGame")
            .add("%direction%", "east");
        String expected = "After months at sea, you have finally arrived off the "
            + "coast of an unknown continent. Sail eastward in order to discover "
            + "the New World and to claim it for the Crown.";
        assertEquals(expected, Messages.message(template));

        template = StringTemplate.template("tutorial.startGame")
            .add("%direction%", "west");
        expected = "After months at sea, you have finally arrived off the "
            + "coast of an unknown continent. Sail westward in order to discover "
            + "the New World and to claim it for the Crown.";
        assertEquals(expected, Messages.message(template));

        template = StringTemplate.template("tutorial.startGame")
            .add("%direction%", "whatever");
        expected = "After months at sea, you have finally arrived off the "
            + "coast of an unknown continent. Sail into the wind in order to discover "
            + "the New World and to claim it for the Crown.";
        assertEquals(expected, Messages.message(template));

    }


    public void testReplaceChoicesPlural() {

        String mapping = "some.key=This is {{plural:%number%|one=a test|other=one of several tests"
            + "|default=not much of a test}}.\n"
            + "unit.template=%number% {{plural:%number%|%unit%}}\n"
            + "unit.key={{plural:%number%|one=piece of artillery|other=pieces of artillery|"
            + "default=artillery}}";
        ByteArrayInputStream stream = new ByteArrayInputStream(mapping.getBytes());
        Messages.loadResources(stream);

        assertEquals("artillery", Messages.message("unit.key"));

        assertEquals("This is one of several tests.",
                     Messages.message(StringTemplate.template("some.key")
                                      .addAmount("%number%", 0)));
        assertEquals("This is a test.",
                     Messages.message(StringTemplate.template("some.key")
                                      .addAmount("%number%", 1)));
        assertEquals("This is one of several tests.",
                     Messages.message(StringTemplate.template("some.key")
                                      .addAmount("%number%", 2)));
        assertEquals("This is one of several tests.",
                     Messages.message(StringTemplate.template("some.key")
                                      .addAmount("%number%", 24)));

        StringTemplate template = StringTemplate.template("unit.template")
            .addAmount("%number%", 1)
            .add("%unit%", "unit.key");

        assertEquals("1 piece of artillery", Messages.message(template));

    }

    public void testReplaceChoicesGrammar() {
        String mapping = "key.france={{randomTag:%randomKey%|country=France|people=French|"
            + "default=French people}}\n"
            + "greeting1=The {{otherRandomTag:default|%nation%}} are happy to see you.\n"
            + "greeting2=The {{otherRandomTag:people|%nation%}} are happy to see you.\n";

        ByteArrayInputStream stream = new ByteArrayInputStream(mapping.getBytes());
        Messages.loadResources(stream);

        assertEquals("French people", Messages.message("key.france"));

        StringTemplate t1 = StringTemplate.template("key.france")
            .add("%randomKey%", "country");
        assertEquals("France", Messages.message(t1));

        StringTemplate t2 = StringTemplate.template("greeting1")
            .add("%nation%", "key.france");
        assertEquals("The French people are happy to see you.", Messages.message(t2));

        StringTemplate t3 = StringTemplate.template("greeting2")
            .add("%nation%", "key.france");
        assertEquals("The French are happy to see you.", Messages.message(t3));


    }

    public void testNestedChoices() {
        String mapping = "key1=%colony% tuottaa tuotetta "
            + "{{tag:acc|%goods%}}.\n"
            + "key2={{plural:%amount%|one=ruoka|other=ruokaa|"
            + "default={{tag:|acc=viljaa|default=Vilja}}}}\n"
            + "key3={{tag:|acc=viljaa|default={{plural:%amount%|one=ruoka|other=ruokaa|default=Ruoka}}}}\n";

        ByteArrayInputStream stream = new ByteArrayInputStream(mapping.getBytes());
        Messages.loadResources(stream);

        StringTemplate t = StringTemplate.template("key1")
            .addName("%colony%", "someColony")
            .add("%goods%", "key2");

        assertEquals("someColony tuottaa tuotetta viljaa.", Messages.message(t));

        assertEquals("Ruoka", Messages.message(StringTemplate.key("key3")));
        assertEquals("Ruoka", Messages.message("key3"));

    }


    public void testLabels() {
        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");

        EquipmentType muskets = spec().getEquipmentType("model.equipment.muskets");
        EquipmentType horses = spec().getEquipmentType("model.equipment.horses");
        EquipmentType tools = spec().getEquipmentType("model.equipment.tools");
        EquipmentType bible = spec().getEquipmentType("model.equipment.missionary");

        // King's regulars
        Unit unit = new ServerUnit(game, null, dutch, spec().getUnitType("model.unit.kingsRegular"));
        assertEquals("King's Regular", Messages.message(Messages.getLabel(unit)));

        unit.changeEquipment(muskets, 1);
        assertEquals("Infantry", Messages.message(Messages.getLabel(unit)));

        unit.changeEquipment(horses, 1);
        assertEquals("Cavalry", Messages.message(Messages.getLabel(unit)));

        // Colonial regulars
        unit = new ServerUnit(game, null, dutch, spec().getUnitType("model.unit.colonialRegular"));
        assertEquals("Colonial Regular", Messages.message(Messages.getLabel(unit)));

        unit.changeEquipment(muskets, 1);
        assertEquals("Continental Army", Messages.message(Messages.getLabel(unit)));

        unit.changeEquipment(horses, 1);
        assertEquals("Continental Cavalry", Messages.message(Messages.getLabel(unit)));

        // Veteran Soldiers
        unit = new ServerUnit(game, null, dutch, spec().getUnitType("model.unit.veteranSoldier"));
        assertEquals(1, unit.getEquipment().getCount(muskets));
        assertEquals("Veteran Soldier", Messages.message(Messages.getLabel(unit)));

        unit.changeEquipment(muskets, -1);
        assertEquals("Veteran Soldier (no muskets)", Messages.message(Messages.getLabel(unit)));

        unit.changeEquipment(horses, 1);
        unit.changeEquipment(muskets, 1);
        assertEquals("Veteran Dragoon", Messages.message(Messages.getLabel(unit)));

        // Indian Braves
        unit = new ServerUnit(game, null, dutch, spec().getUnitType("model.unit.brave"));
        assertEquals(0, unit.getEquipment().getCount(muskets));
        assertEquals("Brave", Messages.message(Messages.getLabel(unit)));

        unit.changeEquipment(muskets, 1);
        assertEquals("Armed Brave", Messages.message(Messages.getLabel(unit)));

        unit.changeEquipment(horses, 1);
        assertEquals("Indian Dragoon", Messages.message(Messages.getLabel(unit)));

        // Hardy Pioneers
        unit = new ServerUnit(game, null, dutch, spec().getUnitType("model.unit.hardyPioneer"));
        assertEquals(5, unit.getEquipment().getCount(tools));
        assertEquals("Hardy Pioneer", Messages.message(Messages.getLabel(unit)));

        unit.changeEquipment(tools, -5);
        assertEquals("Hardy Pioneer (no tools)", Messages.message(Messages.getLabel(unit)));

        // Jesuit Missionaries
        unit = new ServerUnit(game, null, dutch, spec().getUnitType("model.unit.jesuitMissionary"));
        assertEquals(1, unit.getEquipment().getCount(bible));
        assertEquals("Jesuit Missionary", Messages.message(Messages.getLabel(unit)));

        unit.changeEquipment(bible, -1);
        assertEquals("Jesuit Missionary (not commissioned)", Messages.message(Messages.getLabel(unit)));

        // REF addition message
        StringTemplate template = StringTemplate.template("model.monarch.action.ADD_TO_REF")
            .addAmount("%number%", 1)
            .add("%unit%", spec().getUnitType("model.unit.kingsRegular").getNameKey());
        String expected = "The Crown has added 1 King's Regular to the Royal Expeditionary Force."
            + " Colonial leaders express concern.";
        assertEquals(expected, Messages.message(template));

        template = StringTemplate.template("model.monarch.action.ADD_TO_REF")
            .addAmount("%number%", 2)
            .add("%unit%", spec().getUnitType("model.unit.artillery").getNameKey());
        expected = "The Crown has added 2 Pieces of Artillery to the Royal Expeditionary Force."
            + " Colonial leaders express concern.";
        assertEquals(expected, Messages.message(template));

        template = StringTemplate.template("model.monarch.action.ADD_TO_REF")
            .addAmount("%number%", 3)
            .add("%unit%", spec().getUnitType("model.unit.manOWar").getNameKey());
        expected = "The Crown has added 3 Men of War to the Royal Expeditionary Force."
            + " Colonial leaders express concern.";
        assertEquals(expected, Messages.message(template));

    }


}
