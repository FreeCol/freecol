
package net.sf.freecol.common.model;

import java.util.logging.Logger;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;





/**
* The superclass of all game objects in FreeCol.
*/
abstract public class FreeColGameObject {
    public static final String  COPYRIGHT = "Copyright (C) 2003 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static final Logger logger = Logger.getLogger(FreeColGameObject.class.getName());



    private String id;
    private Game game;




    /**
    * Creates a new <code>FreeColGameObject</code> with an automatically assigned 
    * ID and registers this object at the specified <code>Game</code>.
    *
    * @param game The <code>Game</code> in which this object belong.
    */
    public FreeColGameObject(Game game) {
        this.game = game;

        if (game != null) {
            //game.setFreeColGameObject(id, this);
            String nextID = game.getNextID();
            if (nextID != null) {
                setID(nextID);
            }
        } else if (this instanceof Game) {
            setID("0");
        } else {
            logger.warning("Created 'FreeColGameObject' with 'game == null': " + this);
        }

    }


    /**
    * Initiates a new <code>FreeColGameObject</code> from an <code>Element</code>.
    *
    * @param game The <code>Game</code> in which this object belong.
    * @param element The <code>Element</code> (in a DOM-parsed XML-tree) that describes
    *                this object.
    */
    public FreeColGameObject(Game game, Element element) {
        this.game = game;

        if (game == null && !(this instanceof Game)) {
            logger.warning("Created 'FreeColGameObject' with 'game == null': " + this);
        }

    }




    /**
    * Gets the game object this <code>FreeColGameObject</code> belongs to.
    * @return The <code>game</code>.
    */
    public Game getGame() {
        return game;
    }


    /**
    * Prepares the object for a new turn.
    */
    abstract public void newTurn();


    /**
    * Removes all references to this object.
    */
    public void dispose() {
        getGame().removeFreeColGameObject(getID());
    }


    /**
    * This method should return an XML-representation of this object.
    * Only attributes visible to <code>player</code> will be added to
    * that representation.
    *
    * @param player The <code>Player</code> this XML-representation is
    *               made for.
    * @param document The document to use when creating new componenets.
    * @return The DOM-element ("Document Object Model").
    */
    abstract public Element toXMLElement(Player player, Document document);


    /**
    * Initialize this object from an XML-representation of this object.
    *
    * @param element The DOM-element ("Document Object Model") made to represent this object.
    */
    abstract public void readFromXMLElement(Element element);


    /**
    * Gets the tag name of the root element representing this object.
    * This method should be overwritten by any sub-class, preferably
    * with the name of the class with the first letter in lower case.
    *
    * @return "unknown".
    */
    public static String getXMLElementTagName() {
        return "unknown";
    }


    /**
    * Gets the unique ID of this object.
    *
    * @param the unique ID of this object,
    */
    public String getID() {
        return id;
    }


    /**
    * Sets the unique ID of this object. When setting a new ID to this object,
    * it it automatically registered at the corresponding <code>Game</code>
    * with the new ID.
    *
    * @param newId the unique ID of this object,
    */
    public void setID(String newID) {
        if (!(this instanceof Game)) {
            if (!newID.equals(getID())) {
                if (getID() != null) {
                    game.removeFreeColGameObject(getID());
                }

                game.setFreeColGameObject(newID, this);
            }
        }

        this.id = newID;
    }

    
    /**
    * Sets the ID of this object for temporary use with
    * <code>toXMLElement</code>. This method does not
    * register the object.
    *
    * @param newId the unique ID of this object,
    */
    public void setFakeID(String newID) {
        this.id = newID;
    }

    
    /**
    * Creates a <code>ModelMessage</code> and uses <code>
    * getGame().addModelMessage(modelMessage)</code>
    * to register it.
    *
    * <br><br><br>
    *
    * Example:<br><br>
    *
    * Using <code>addModelMessage(this, "messageID", new String[][] {{"%test1%", "ok1"}, {"%test2%", "ok2"})</code>
    * with the entry "messageID=This is %test1% and %test2%" in {@link net.sf.freecol.client.gui.i18n.Messages Messages},
    * would give the following message: "This is ok1 and ok2".
    *
    * @param source The source of the message. This is what the message should be 
    *               associated with. In addition, the owner of the source is the
    *               player getting the message.
    * @param messageID The ID of the message to display. See: {@link net.sf.freecol.client.gui.i18n.Messages Messages}.
    * @param data Contains the data to be displayed in the message or <i>null</i>.
    * @see net.sf.freecol.client.gui.Canvas Canvas
    * @see Game#addModelMessage
    * @see ModelMessage
    */
    protected void addModelMessage(FreeColGameObject source, String messageID, String[][] data) {
        getGame().addModelMessage(new ModelMessage(source, messageID, data));
    }


    /**
    * Checks if this object has the specified ID.
    *
    * @param id The ID to check against.
    * @return <i>true</i> if the specified ID match the ID of this object and
    *         <i>false</i> otherwise.
    */
    public boolean hasID(String id) {
        return getID().equals(id);
    }


    /**
    * Checks if the given <code>FreeColGameObject</code> equals this object.
    *
    * @param o The <code>FreeColGameObject</code> to compare against this object.
    * @return <i>true</i> if the two <code>FreeColGameObject</code> are equal and <i>false</i> otherwise.
    */
    public boolean equals(FreeColGameObject o) {
        if (o == null) {
            return false;
        }

        if (getID().equals(o.getID())) {
            return true;
        } else {
            return false;
        }
    }

    
    /**
    * Returns a string representation of the object.
    * @return The <code>String</code>
    */
    public String toString() {
        return getClass().getName() + ": " + getID() + " (hash code: " + Integer.toHexString(hashCode()) + ")";
    }


    /**
    * Convenience method: returns the first child element with the
    * specified tagname.
    *
    * @param element The <code>Element</code> to search for the child element.
    * @param tagName The tag name of the child element to be found.
    */
    protected Element getChildElement(Element element, String tagName) {
        NodeList n = element.getChildNodes();
        for (int i=0; i<n.getLength(); i++) {
            if (((Element) n.item(i)).getTagName().equals(tagName)) {
                return (Element) n.item(i);
            }
        }

        return null;
    }

}
