
package net.sf.freecol.server.ai;

import java.util.logging.Logger;

import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.ai.mission.PioneeringMission;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * Represents a <code>Tile</code> which should be improved in some way.
 * For instance by plowing or by building a road.
 *
 * @see Tile
 */
public class TileImprovement extends AIObject {
    private static final Logger logger = Logger.getLogger(TileImprovement.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final int PLOW = Unit.PLOW;
    public static final int BUILD_ROAD = Unit.BUILD_ROAD;
       
    private String id;
    
    /**
     * The type of improvement, either {@link #PLOW} or {@link #BUILD_ROAD}.
     */
    private int type;
    
    /**
     * The value of this improvement.
     */
    private int value;
    
    /**
     * The pioneer which should make the improvement (if a <code>Unit</code> has
     * been assigned).
     */
    private AIUnit pioneer = null;
    
    /**
     * The <code>Tile</code> to be improved.
     */
    private Tile target;


    /**
     * Creates a new <code>TileImprovement</code>.
     * @param aiMain The main AI-object.
     */
    public TileImprovement(AIMain aiMain, Tile target, int type, int value) {
        super(aiMain);
        
        id = aiMain.getNextID();
        aiMain.addAIObject(id, this);       
        
        this.target = target;
        this.type = type;
        this.value = value;
    }

    /**
     * Creates a new <code>TileImprovement</code> from the given XML-representation.
     *
     * @param aiMain The main AI-object.
     * @param element The root element for the XML-representation 
     *       of a <code>Wish</code>.
     */
    public TileImprovement(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }

    
    /**
     * Disposes this <code>TileImprovement</code>.
     * If a pioneer has been assigned to making this improvement,
     * then this pioneer gets informed that the improvement is
     * no longer wanted.
     */
    public void dispose() {
    	if (pioneer != null && pioneer.getMission() != null) {
    		((PioneeringMission) pioneer.getMission()).setTileImprovement(null);
    	}
    	super.dispose();
    }    

    /**
     * Returns the ID for this <code>TileImprovement</code>.
     * @return The ID of this <code>TileImprovement</code>.
     */
    public String getID() {
        return id;
    }

    /**
    * Returns the value for this <code>TileImprovement</code>.
    * @return The value identifying the importance of
    *         this <code>TileImprovement</code> - a higher value 
    *         signals a higher importance.
    */
    public int getValue() {
        return value;
    }

    /**
     * Sets the value of this <code>TileImprovement</code>.
     * @param value The value.
     * @see #getValue
     */
    public void setValue(int value) {
    	this.value = value;
    }
    
    /**
     * Gets the pioneer who have been assigned to making the
     * improvement described by this object.
     * 
     * @return The pioneer which should make the improvement, if 
     * 		such a <code>AIUnit</code> has been assigned, and 
     * 		<code>null</code> if nobody has been assigned this
     * 		mission.
     */
    public AIUnit getPioneer() {
    	return pioneer;
    }
    
    /**
     * Sets the pioneer who have been assigned to making the
     * improvement described by this object.
     * 
     * @param pioneer The pioneer which should make the improvement, if 
     * 		such a <code>Unit</code> has been assigned, and 
     * 		<code>null</code> if nobody has been assigned this
     * 		mission.
     */    
    public void setPioneer(AIUnit pioneer) {
    	this.pioneer = pioneer;    
    }
    
    /**
     * Returns the type of improvement.
     * 
     * @return The type of the improvement, either 
     * 		{@link #PLOW} or {@link #BUILD_ROAD}.
     */
    public int getType() {
    	return type;
    }
    
    /**
     * Sets the type of this <code>TileImprovement</code>.
     * @param type The type.
     * @see #getType
     */
    public void setType(int type) {
    	this.type = type;
    }
    
    /**
    * Gets the target of this <code>TileImprovement</code>.
    * @return The <code>Tile</code> where
    *       {@link #getPioneer pioneer} should make the
    *       given {@link #getType improvement}.
    */
    public Tile getTarget() {
        return target;
    }
    
    /**
     * Creates an XML-representation of this object.
     * @param document The <code>Document</code> in which
     *      the XML-representation should be created.
     * @return The XML-representation.
     */    
    public Element toXMLElement(Document document) {
        Element element = document.createElement(getXMLElementTagName());

        element.setAttribute("ID", id);
        element.setAttribute("type", Integer.toString(type));
        element.setAttribute("value", Integer.toString(value));
        if (pioneer != null) {
        	element.setAttribute("pioneer", pioneer.getID());
        }
        element.setAttribute("target", target.getID());

        return element;
    }

    /**
     * Updates this object from an XML-representation of
     * a <code>TileImprovement</code>.
     * 
     * @param element The XML-representation.
     */    
    public void readFromXMLElement(Element element) {
        id = element.getAttribute("ID");
        type = Integer.parseInt(element.getAttribute("type"));
        value = Integer.parseInt(element.getAttribute("value"));
        if (element.hasAttribute("pioneer")) {
        	pioneer = (AIUnit) getAIMain().getAIObject(element.getAttribute("pioneer"));
        } else {
        	pioneer = null;
        }
        target = (Tile) getAIMain().getFreeColGameObject(element.getAttribute("target"));        
    }

    /**
    * Returns the tag name of the root element representing this object.
    * @return "tileImprovement"
    */
    public static String getXMLElementTagName() {
        return "tileImprovement";
    }    
}
