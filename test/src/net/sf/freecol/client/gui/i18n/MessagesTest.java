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
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.StringTemplate;

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

    public void testMessageStringVarargs() {

        try {
            Messages.message(null);
            fail("We should never get here");
        } catch (NullPointerException npe) {
            // Expected
        }

        assertEquals("Trade Advisor", Messages.message("reportTradeAction.name"));

        // With parameters for "Gold: %gold% | Tax: %tax%% | Year: %year%"
        assertEquals("Score: 1050    |    Gold: silver    |    Tax: 13%    |    Year: %year%",
                     Messages.message("menuBar.statusLine", "%score%", "1050", "%gold%", "silver", "%tax%", "13"));

        // Long String
        assertEquals("Food is necessary to feed your colonists and to breed horses. "
                     + "A new colonist is born whenever a colony has 200 units of food or more.",
                     Messages.message("model.goods.food.description"));

        try {
            Messages.message("menuBar.statusLine", "%tax%");
            fail("We should never have gotten here.");
        } catch (RuntimeException re) {
            // Expected
        }


        // Message not found
        assertEquals(noSuchKey, Messages.message(noSuchKey));

        assertEquals(noSuchKey, Messages.message(noSuchKey,
                                                 "%gold%", "silver", "%tax%", "13"));
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
            message = Messages.message("model.history.FOUND_COLONY","%colony%",colNameWithSpecialChars);
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
        assertEquals("abc   def", Messages.replaceChoices("{{}}abc   {{xyz}}def{{123|567}}"));
    }

    public void testReplaceNumber() {

        double[] numbers = new double[] {
            -1.3, -1, -0.5, 0, 0.33, 1, 1.2, 2, 2.7, 3, 3.4, 11, 13, 27, 100
        };
        String choices = "|zero=zero|one=one|two=two|few=few|many=many|other=other}}|xyz";
        // default Number is Other
        Messages.setGrammaticalNumber(NumberRules.OTHER_NUMBER_RULE);
        for (double d : numbers) {
            assertEquals("abcother|xyz", Messages.replaceChoices("abc{{plural:" + d + choices));
        }
        // apply English rules
        Messages.setGrammaticalNumber(NumberRules.PLURAL_NUMBER_RULE);
        for (double d : numbers) {
            if (d == 1) {
                assertEquals("abcone|xyz", Messages.replaceChoices("abc{{plural:" + d + choices));
            } else {
                assertEquals("abcother|xyz", Messages.replaceChoices("abc{{plural:" + d + choices));
            }
        }

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

        assertEquals("This is one of several tests.", Messages.message("some.key", "%number%", "0"));
        assertEquals("This is a test.", Messages.message("some.key", "%number%", "1"));
        assertEquals("This is one of several tests.", Messages.message("some.key", "%number%", "2"));
        assertEquals("This is one of several tests.", Messages.message("some.key", "%number%", "24"));

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

}
