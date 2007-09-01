
package net.sf.freecol.server.ai;

import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.common.PseudoRandom;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;

/**
* An <code>AIObject</code> contains AI-related information and methods.
* Each <code>FreeColGameObject</code>, that is owned by an AI-controlled
* player, can have a single <code>AIObject</code> attached to it.
*/
public abstract class AIObject extends FreeColObject {
    private static final Logger logger = Logger.getLogger(FreeColObject.class.getName());
    
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    private final AIMain aiMain;
    protected boolean uninitialized = false;
    protected String id = null;

    
    /**
     * Creates a new <code>AIObject</code>.
     * @param aiMain The main AI-object.
     */
    public AIObject(AIMain aiMain) {
        this.aiMain = aiMain;
    }

    /**
     * Creates a new <code>AIObject</code> and registers
     * this object with <code>AIMain</code>.
     * 
     * @param aiMain The main AI-object.
     * @param id The unique identifier.
     * @see AIMain#addAIObject(String, AIObject)
     */
    public AIObject(AIMain aiMain, String id) {
        this.aiMain = aiMain;
        this.id = id;
        aiMain.addAIObject(id, this);
    }

    
    /**
     * Returns the main AI-object.
     * @return The <code>AIMain</code>.
     */
    public AIMain getAIMain() {
        return aiMain;
    }

    /**
     * Checks if this <code>AIObject</code> 
     * is uninitialized. That is: it has been referenced
     * by another object, but has not yet been updated with
     * {@link #readFromXML}.
     * 
     * @return <code>true</code> if this object is not initialized.
     */
    public boolean isUninitialized() {
        return uninitialized;
    }
    
    /**
    * Returns the ID of this <code>AIObject</code>.
    * @return The ID of this <code>AIObject</code>. This is normally
    *         the ID of the {@link FreeColGameObject} this object
    *         represents.
    */
    public abstract String getID();
    
    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public final void readFromXML(XMLStreamReader in) throws XMLStreamException {
        super.readFromXML(in);
        uninitialized = false;
    }
    
    /**
    * Returns an instance of <code>PseudoRandom</code>. It that can be
    * used to generate random numbers.
    * 
    * @return An instance of <code>PseudoRandom</code>.
    */
    protected PseudoRandom getRandom() {
        return aiMain.getRandom();
    }

    
    /**
     * Disposes this <code>AIObject</code> by removing
     * any referances to this object.
     */
    public void dispose() {
        getAIMain().removeAIObject(getID());
    }
    
        
    /**
    * Returns the game.
    * @return The <code>Game</code>.
    */
    public Game getGame() {
        return aiMain.getGame();
    }


    /**
    * Returns the tag name of the root element representing this object.
    * @return The <code>String</code> "unknown".
    */
    public static String getXMLElementTagName() {
        return "unknown";
    }
}
