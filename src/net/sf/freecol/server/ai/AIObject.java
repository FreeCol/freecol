
package net.sf.freecol.server.ai;

import net.sf.freecol.common.model.*;
import net.sf.freecol.common.networking.Message;

import org.w3c.dom.*;


/**
* An <code>AIObject</code> contains AI-related information and methods.
* Each <code>FreeColGameObject</code>, that is owned by an AI-controlled
* player, can have a single <code>AIObject</code> attached to it.
*/
public abstract class AIObject {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    private final AIMain aiMain;
    
    
    /**
    * Creates a new <code>AIObject</code>.
    */
    public AIObject(AIMain aiMain) {
        this.aiMain = aiMain;
    }
    
    
    public AIMain getAIMain() {
        return aiMain;
    }
    

    public abstract Element toXMLElement(Document document);

    
    public abstract void readFromXMLElement(Element element);
    

    /**
    * Returns the tag name of the root element representing this object.
    * @return The <code>String</code> "unknown".
    */
    public static String getXMLElementTagName() {
        return "unknown";
    }
}
