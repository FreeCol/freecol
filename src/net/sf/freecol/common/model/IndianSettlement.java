/*
 *  Settlement.java - Holds all information about an Indian settlement.
 *
 *  Copyright (C) 2003  The FreeCol Team
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Library General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package net.sf.freecol.common.model;

import java.util.Iterator;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net.sf.freecol.common.FreeColException;


/**
 * Represents an Indian settlement.
 */
public class IndianSettlement extends Settlement {
    private static final Logger logger = Logger.getLogger(IndianSettlement.class.getName());

    public static final int INCA = 0;
    public static final int AZTEC = 1;
    public static final int ARAWAK = 2;
    public static final int CHEROKEE = 3;
    public static final int IROQUOIS = 4;
    public static final int SIOUX = 5;
    public static final int APACHE = 6;
    public static final int TUPI = 7;
    public static final int LAST_TRIBE = 7;
    public static final int CAMP = 0;
    public static final int VILLAGE = 1;
    public static final int CITY = 2;
    public static final int LAST_KIND = 2;


    private int kind;
    private int tribe;
    private boolean isCapital;
    private UnitContainer unitContainer;





    /**
     * The constructor to use.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param tile The location of the <code>IndianSettlement</code>.
     * @param tribe Tribe of settlement
     * @param kind Kind of settlement
     * @param isCapital True if settlement is tribe's capital
     * @exception IllegalArgumentException if an invalid tribe or kind is given
     */
    public IndianSettlement(Game game, Player player, Tile tile, int tribe, int kind, boolean isCapital) {
        // TODO: Change 'null' to the indian AI-player:

        super(game, player, tile);

        unitContainer = new UnitContainer(game, this);

        if (tribe < 0 || tribe > LAST_TRIBE) {
            throw new IllegalArgumentException("Invalid tribe provided");
        }

        this.tribe = tribe;

        if (kind < 0 || kind > LAST_KIND) {
            throw new IllegalArgumentException("Invalid settlement kind provided");
        }

        this.kind = kind;
        this.isCapital = isCapital;
    }



    /**
    * Initiates a new <code>IndianSettlement</code> from an <code>Element</code>.
    *
    * @param game The <code>Game</code> in which this object belong.
    * @param element The <code>Element</code> (in a DOM-parsed XML-tree) that describes
    *                this object.
    */
    public IndianSettlement(Game game, Element element) {
        super(game, element);

        readFromXMLElement(element);
    }





    /**
    * Gets the kind of Indian settlement.
    */
    public int getKind() {
        return kind;
    }


    /**
    * Gets the tribe of the Indian settlement.
    */
    public int getTribe() {
        return tribe;
    }


    /**
     * Gets the radius of what the <code>Settlement</code> considers
     * as it's own land.  Cities dominate 2 tiles, other settlements 1 tile.
     *
     * @return Settlement radius
     */
    public int getRadius() {
        if (kind == CITY)
            return 2;
        else
            return 1;
    }


    /**
    * Returns <i>null</i>.
    * @return <i>null</i>.
    */
    public Tile getTile() {
        return null;
    }


    /**
    * Adds a <code>Locatable</code> to this Location.
    *
    * @param locatable The code>Locatable</code> to add to this Location.
    */
    public void add(Locatable locatable) {
        if (locatable instanceof Unit) {
            unitContainer.addUnit((Unit) locatable);
        } else {
            logger.warning("Tried to add an unrecognized 'Locatable' to a IndianSettlement.");
        }
    }


    /**
    * Removes a code>Locatable</code> from this Location.
    *
    * @param locatable The <code>Locatable</code> to remove from this Location.
    */
    public void remove(Locatable locatable) {
        if (locatable instanceof Unit) {
            unitContainer.removeUnit((Unit) locatable);
        } else {
            logger.warning("Tried to remove an unrecognized 'Locatable' from a IndianSettlement.");
        }
    }


    /**
    * Returns the amount of Units at this Location.
    *
    * @return The amount of Units at this Location.
    */
    public int getUnitCount() {
        return unitContainer.getUnitCount();
    }


    public Iterator getUnitIterator() {
        return unitContainer.getUnitIterator();
    }
    
    

    public Unit getFirstUnit() {
        return unitContainer.getFirstUnit();
    }


    public Unit getLastUnit() {
        return unitContainer.getLastUnit();
    }


    public boolean contains(Locatable locatable) {
        if (locatable instanceof Unit) {
            return unitContainer.contains((Unit) locatable);
        } else {
            return false;
        }
    }

    
    public boolean canAdd(Locatable locatable) {
        return true;
    }
    

    public void newTurn() {

    }
        
    
    /**
    * Make a XML-representation of this object.
    *
    * @param document The document to use when creating new componenets.
    * @return The DOM-element ("Document Object Model") made to represent this "IndianSettlement".
    */
    public Element toXMLElement(Player player, Document document) {
        Element indianSettlementElement = document.createElement(getXMLElementTagName());

        indianSettlementElement.setAttribute("ID", getID());

        indianSettlementElement.appendChild(unitContainer.toXMLElement(player, document));

        return indianSettlementElement;
    }


    /**
    * Initialize this object from an XML-representation of this object.
    *
    * @param indianSettlementElement The DOM-element ("Document Object Model") made to represent this "IndianSettlement".
    */
    public void readFromXMLElement(Element indianSettlementElement) {
        setID(indianSettlementElement.getAttribute("ID"));
        
        Element unitContainerElement = (Element) indianSettlementElement.getElementsByTagName(UnitContainer.getXMLElementTagName()).item(0);
        if (unitContainer != null) {
            unitContainer.readFromXMLElement(unitContainerElement);
        } else {
            unitContainer = new UnitContainer(getGame(), this, unitContainerElement);
        }
    }


    /**
    * Returns the tag name of the root element representing this object.
    *
    * @return the tag name.
    */
    public static String getXMLElementTagName() {
        return "indianSettlement";
    }
}
