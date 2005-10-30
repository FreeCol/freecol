
package net.sf.freecol.server.ai;

import java.util.Random;

import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
* An <code>AIObject</code> contains AI-related information and methods.
* Each <code>FreeColGameObject</code>, that is owned by an AI-controlled
* player, can have a single <code>AIObject</code> attached to it.
*/
public abstract class AIObject {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    private final AIMain aiMain;

    
    /**
    * Creates a new <code>AIObject</code>.
    * @param aiMain The main AI-object.
    */
    public AIObject(AIMain aiMain) {
        this.aiMain = aiMain;
    }
    
    
    /**
     * Returns the main AI-object.
     * @return The <code>AIMain</code>.
     */
    public AIMain getAIMain() {
        return aiMain;
    }

    
    /**
    * Returns the ID of this <code>AIObject</code>.
    * @return The ID of this <code>AIObject</code>. This is normally
    *         the ID of the {@link FreeColGameObject} this object
    *         represents.
    */
    public abstract String getID();

    
    /**
     * Creates an XML-representation of this object.
     * @param document The <code>Document</code> in which
     *      the XML-representation should be created.
     * @return The XML-representation.
     */    
    public abstract Element toXMLElement(Document document);
    
    
    /**
     * Updates this object from an XML-representation of
     * a <code>AIObject</code> of the same type as the
     * implementing class.
     * 
     * @param element The XML-representation.
     */    
    public abstract void readFromXMLElement(Element element);
    
    
    /**
    * Returns an instance of the class <code>Random</code>. It that can be
    * used to generate random numbers.
    * 
    * @return An instance of <code>Random</code>.
    */
    protected Random getRandom() {
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
