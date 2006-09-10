
package net.sf.freecol.common.model;

import java.util.logging.Logger;

import net.sf.freecol.common.networking.Message;

import org.w3c.dom.Document;
import org.w3c.dom.Element;





/**
* The superclass of all game objects in FreeCol.
*/
abstract public class FreeColGameObject {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static final Logger logger = Logger.getLogger(FreeColGameObject.class.getName());



    private String id;
    protected Game game;
    private boolean disposed = false;



    public FreeColGameObject() {    
        logger.info("FreeColGameObject without ID created.");
    }
    

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
    * Gets the <code>GameOptions</code> that is associated with the 
    * {@link Game} owning this <code>FreeColGameObject</code>.
    * 
    * @return The same <code>GameOptions</code>-object as returned
    *       by <code>getGame().getGameOptions()</code>.
    */
    public GameOptions getGameOptions() {
        return game.getGameOptions();
    }

    
    /**
    * Sets the game object this <code>FreeColGameObject</code> belongs to.
    * @param game The <code>game</code>.
    */
    public void setGame(Game game) {
        this.game = game;
    }    

    
    /**
    * Prepares the object for a new turn.
    */
    abstract public void newTurn();


    /**
    * Removes all references to this object.
    */
    public void dispose() {
        disposed = true;
        getGame().removeFreeColGameObject(getID());
    }
    

    /**
    * Checks if this object has been disposed.
    * @return <code>true</code> if this object has been disposed.
    * @see #dispose
    */
    public boolean isDisposed() {
        return disposed;
    }


    /**
    * Updates the id. This method should be overwritten
    * by server model objects.
    */
    public void updateID() {
        
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
    * @see #toXMLElement(Player, Document, boolean, boolean)
    */
    public Element toXMLElement(Player player, Document document) {
        return toXMLElement(player, document, false, false);
    }
    
    
    /**
    * This method should return an XML-representation of this object.
    * All attributes will be made visable.
    *
    * @param document The document to use when creating new componenets.
    * @return The DOM-element ("Document Object Model").
    * @see #toXMLElement(Player, Document, boolean, boolean)
    */
    public Element toXMLElement(Document document) {
        return toXMLElement(null, document, true, false);
    }
        
    
    /**
    * This method should return an XML-representation of this object
    * for the purpose of storing this object when saving the game.
    *
    * @param document The document to use when creating new componenets.
    * @return The DOM-element ("Document Object Model").
    * @see #toXMLElement(Player, Document, boolean, boolean)
    */
    public Element toSavedXMLElement(Document document) {
        return toXMLElement(null, document, true, true);
    }
        
    
    /**
    * This method should return an XML-representation of this object.
    * Only attributes visible to <code>player</code> will be added to
    * that representation if <code>showAll</code> is set to <i>false</i>.
    *
    * @param player The <code>Player</code> this XML-representation is
    *               made for.
    * @param document The document to use when creating new componenets.
    * @param showAll Only attributes visible to <code>player</code> will be added to
    *                the representation if <code>showAll</code> is set to <i>false</i>.
    * @param toSavedGame If <i>true</i> then information that is only needed when saving a
    *                    game is added.
    * @return The DOM-element ("Document Object Model").
    */    
    abstract public Element toXMLElement(Player player, Document document, boolean showAll, boolean toSavedGame);


    /**
    * Initialize this object from an XML-representation of this object.
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
    * @return The unique ID of this object,
    */
    public String getID() {
        return id;
    }


    /**
    * Sets the unique ID of this object. When setting a new ID to this object,
    * it it automatically registered at the corresponding <code>Game</code>
    * with the new ID.
    *
    * @param newID the unique ID of this object,
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
    * @param newID the unique ID of this object,
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
     * @param type The type of message.
     * @see net.sf.freecol.client.gui.Canvas Canvas
     * @see Game#addModelMessage(ModelMessage)
     * @see ModelMessage
     */    
    protected void addModelMessage(FreeColGameObject source, String messageID, String[][] data, int type) {
        getGame().addModelMessage(new ModelMessage(source, messageID, data, type));
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
     * @param type The type of message.
     * @param display The Object to display.
     * @see net.sf.freecol.client.gui.Canvas Canvas
     * @see Game#addModelMessage(ModelMessage)
     * @see ModelMessage
     */
    protected void addModelMessage(FreeColGameObject source, String messageID, String[][] data, int type, Object display) {
        getGame().addModelMessage(new ModelMessage(source, messageID, data, type, display));
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
        if (o != null) {
            return getID().equals(o.getID());
        } else {
            return false;
        }
    }
    
    /**
     * Checks if the given <code>FreeColGameObject</code> equals this object.
     *
     * @param o The <code>FreeColGameObject</code> to compare against this object.
     * @return <i>true</i> if the two <code>FreeColGameObject</code> are equal and <i>false</i> otherwise.
     */
    public boolean equals(Object o) {
        return (o instanceof FreeColGameObject) ? equals((FreeColGameObject) o) : false;
    }
        
    public int hashCode() {
        return getID().hashCode();
    }

    
    /**
    * Returns a string representation of the object.
    * @return The <code>String</code>
    */
    public String toString() {
        return getClass().getName() + ": " + getID() + " (super's hash code: " + Integer.toHexString(super.hashCode()) + ")";
    }


    /**
    * Convenience method: returns the first child element with the
    * specified tagname.
    *
    * @param element The <code>Element</code> to search for the child element.
    * @param tagName The tag name of the child element to be found.
    * @return The first child element with the given name.
    */
    protected Element getChildElement(Element element, String tagName) {
        return Message.getChildElement(element, tagName);
    }

    
    /**
    * Creates an XML-representation of an array.
    * 
    * @param tagName The tagname for the <code>Element</code>
    *       representing the array.
    * @param array The array to represent.
    * @param document The document context in which the
    *       elemt should be created.
    * @return An XML DOM <code>Element</code> representing
    *       the given array.
    */
    protected Element toArrayElement(String tagName, int[][] array, Document document) {
        Element arrayElement = document.createElement(tagName);
        arrayElement.setAttribute("xLength", Integer.toString(array.length));
        arrayElement.setAttribute("yLength", Integer.toString(array[0].length));

        for (int x=0; x < array.length; x++) {
            for (int y=0; y < array[0].length; y++) {
                arrayElement.setAttribute("x" + Integer.toString(x) + "y" + Integer.toString(y), Integer.toString(array[x][y]));
            }
        }
        
        return arrayElement;
    }
    

    /**
    * Reads an XML-representation of an array.
    * 
    * @param tagName The tagname for the <code>Element</code>
    *       representing the array.
    * @param arrayElement An XML DOM <code>Element</code> 
    *       representing an array.
    * @param arrayType The type of array to be read.
    * @return The array.
    */                
    protected int[][] readFromArrayElement(String tagName, Element arrayElement, int[][] arrayType) {
        int[][] array = new int[Integer.parseInt(arrayElement.getAttribute("xLength"))][Integer.parseInt(arrayElement.getAttribute("yLength"))];
        
        for (int x=0; x<array.length; x++) {
            for (int y=0; y<array[0].length; y++) {
                array[x][y] = Integer.parseInt(arrayElement.getAttribute("x" + Integer.toString(x) + "y" + Integer.toString(y)));
            }
        }
        
        return array;
    }

    
    /**
     * Creates an XML-representation of an array.
     * 
     * @param tagName The tagname for the <code>Element</code>
     *       representing the array.
     * @param array The array to represent.
     * @param document The document context in which the
     *       elemt should be created.
     * @return An XML DOM <code>Element</code> representing
     *       the given array.
     */
    protected Element toArrayElement(String tagName, int[] array, Document document) {
        Element arrayElement = document.createElement(tagName);
        arrayElement.setAttribute("xLength", Integer.toString(array.length));

        for (int x=0; x < array.length; x++) {
            arrayElement.setAttribute("x" + Integer.toString(x), Integer.toString(array[x]));
        }
        
        return arrayElement;
    }
    

    /**
     * Reads an XML-representation of an array.
     * 
     * @param tagName The tagname for the <code>Element</code>
     *       representing the array.
     * @param arrayElement An XML DOM <code>Element</code> 
     *       representing an array.
     * @param arrayType The type of array to be read.
     * @return The array.
     */               
    protected int[] readFromArrayElement(String tagName, Element arrayElement, int[] arrayType) {
        int[] array = new int[Integer.parseInt(arrayElement.getAttribute("xLength"))];
        
        for (int x=0; x<array.length; x++) {
            array[x] = Integer.parseInt(arrayElement.getAttribute("x" + Integer.toString(x)));
        }
        
        return array;
    }


    /**
     * Creates an XML-representation of an array.
     * 
     * @param tagName The tagname for the <code>Element</code>
     *       representing the array.
     * @param array The array to represent.
     * @param document The document context in which the
     *       elemt should be created.
     * @return An XML DOM <code>Element</code> representing
     *       the given array.
     */    
    protected Element toArrayElement(String tagName, boolean[][] array, Document document) {
        Element arrayElement = document.createElement(tagName);
        arrayElement.setAttribute("xLength", Integer.toString(array.length));
        arrayElement.setAttribute("yLength", Integer.toString(array[0].length));
        
        StringBuffer sb = new StringBuffer(array.length * array[0].length);
        for (int x=0; x < array.length; x++) {
            for (int y=0; y < array[0].length; y++) {
                //arrayElement.setAttribute("x" + Integer.toString(x) + "y" + Integer.toString(y), Boolean.toString(array[x][y]));
                if (array[x][y]) {
                    sb.append("1");
                } else {
                    sb.append("0");
                }
            }
        }
        
        arrayElement.setAttribute("data", sb.toString());

        return arrayElement;
    }


    /**
     * Reads an XML-representation of an array.
     * 
     * @param tagName The tagname for the <code>Element</code>
     *       representing the array.
     * @param arrayElement An XML DOM <code>Element</code> 
     *       representing an array.
     * @param arrayType The type of array to be read.
     * @return The array.
     */ 
    protected boolean[][] readFromArrayElement(String tagName, Element arrayElement, boolean[][] arrayType) {
        boolean[][] array = new boolean[Integer.parseInt(arrayElement.getAttribute("xLength"))][Integer.parseInt(arrayElement.getAttribute("yLength"))];

        String data = null;
        if (arrayElement.hasAttribute("data")) {
            data = arrayElement.getAttribute("data");
        } 
        
        for (int x=0; x<array.length; x++) {
            for (int y=0; y<array[0].length; y++) {
                if (data != null) {
                    if (data.charAt(x*array[0].length+y) == '1') {
                        array[x][y] = true;
                    } else {
                        array[x][y] = false;
                    }
                } else { // Old type of storing booleans:
                    array[x][y] = Boolean.valueOf(arrayElement.getAttribute("x" + Integer.toString(x) + "y" + Integer.toString(y))).booleanValue();
                }
            }
        }

        return array;
    }


    
    /**
     * Creates an XML-representation of an array.
     * 
     * @param tagName The tagname for the <code>Element</code>
     *       representing the array.
     * @param array The array to represent.
     * @param document The document context in which the
     *       elemt should be created.
     * @return An XML DOM <code>Element</code> representing
     *       the given array.
     */
    protected Element toArrayElement(String tagName, String[] array, Document document) {
        Element arrayElement = document.createElement(tagName);
        arrayElement.setAttribute("xLength", Integer.toString(array.length));

        for (int x=0; x < array.length; x++) {
            arrayElement.setAttribute("x" + Integer.toString(x), array[x]);
        }
        
        return arrayElement;
    }
    

    /**
     * Reads an XML-representation of an array.
     * 
     * @param tagName The tagname for the <code>Element</code>
     *       representing the array.
     * @param arrayElement An XML DOM <code>Element</code> 
     *       representing an array.
     * @param arrayType The type of array to be read.
     * @return The array.
     */               
    protected String[] readFromArrayElement(String tagName, Element arrayElement, String[] arrayType) {
        String[] array = new String[Integer.parseInt(arrayElement.getAttribute("xLength"))];
        
        for (int x=0; x<array.length; x++) {
            array[x] = arrayElement.getAttribute("x" + Integer.toString(x));
        }
        
        return array;
    }

}
