package net.sf.freecol.client.gui.i18n;

import java.util.Locale;

import net.sf.freecol.util.test.FreeColTestCase;

public class MessagesTest extends FreeColTestCase {

    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    public static final String noSuchKey = "should.not.exist.and.thus.return.null";

    public void tearDown(){
        Messages.setMessageBundle(Locale.US);
    }

    public void testMessageString() {

        assertEquals("Trade Advisor", Messages.message("menuBar.report.trade"));

        // With parameters
        assertEquals("Score: %score%    |    Gold: %gold%    |    Tax: %tax%%    |    Year: %year%", 
                     Messages.message("menuBar.statusLine"));

        // Long String
        assertEquals("Food is necessary to feed your colonists and to breed horses. "
                + "A new colonist is born whenever a colony has 200 units of food or more.",
                     Messages.message("model.goods.Food.description"));

        // Message not found
        assertEquals(noSuchKey, Messages.message(noSuchKey));
    }

    @SuppressWarnings("deprecation")
    public void testMessageStringStringArrayArray() {

        assertEquals("Trade Advisor", Messages.message("menuBar.report.trade", new String[][] {} ));

        // With parameters for "Gold: %gold% | Tax: %tax%% | Year: %year%"
        assertEquals("Score: 1050    |    Gold: silver    |    Tax: 13%    |    Year: %year%", 
                     Messages.message("menuBar.statusLine",
                                      new String[][] {
                                          { "%score%", "1050" }, 
                                          { "%gold%", "silver" }, 
                                          { "%tax%", "13" } }));

        // Long String
        assertEquals("Food is necessary to feed your colonists and to breed horses. "
                + "A new colonist is born whenever a colony has 200 units of food or more.", 
                     Messages.message("model.goods.Food.description", new String[][] {}));

        // Invalid Inputs
        assertEquals("Trade Advisor", Messages.message("menuBar.report.trade", new String[][] {}));
        assertEquals("Score: %score%    |    Gold: %gold%    |    Tax: %tax%%    |    Year: %year%", 
                     Messages.message("menuBar.statusLine",
                                      new String[][] { new String[] { "%tax%" } }));

        
        // Message not found
        assertEquals(noSuchKey, Messages.message(noSuchKey, new String[][] {}));
        
        assertEquals(noSuchKey, Messages.message(noSuchKey, 
                new String[][] { new String[] { "%gold%", "silver" }, new String[] { "%tax%", "13" } }));
    }
    
    public void testMessageStringVarargs() {
        
        try {
            Messages.message(null);
            fail("We should never get here");
        } catch (NullPointerException npe) {
            // Expected
        }

        assertEquals("Trade Advisor", Messages.message("menuBar.report.trade"));

        // With parameters for "Gold: %gold% | Tax: %tax%% | Year: %year%"
        assertEquals("Score: 1050    |    Gold: silver    |    Tax: 13%    |    Year: %year%", 
                     Messages.message("menuBar.statusLine", "%score%", "1050", "%gold%", "silver", "%tax%", "13"));

        // Long String
        assertEquals("Food is necessary to feed your colonists and to breed horses. "
                + "A new colonist is born whenever a colony has 200 units of food or more.", Messages.message(
                "model.goods.Food.description"));

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

        assertEquals("Trade Advisor", Messages.message("menuBar.report.trade"));

        Messages.setMessageBundle(Locale.GERMANY);

        assertEquals("Handelsberater", Messages.message("menuBar.report.trade"));
    }
}
