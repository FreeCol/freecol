
package net.sf.freecol.common.model;

import net.sf.freecol.common.FreeColException;


import java.util.logging.Logger;
import java.util.ArrayList;
//import java.util.HashMap;
import java.util.Iterator;

import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.Document;


/**
* Contains goods and can be used by a {@link Location} to make certain
* tasks easier.
*/
public class GoodsContainer extends FreeColGameObject {
    private static Logger logger = Logger.getLogger(Location.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /** The list of Goods stored in this <code>GoodsContainer</code>. */
    private ArrayList goods = new ArrayList();
    
    /** The owner of this <code>GoodsContainer</code>. */
    private Location parent;




    /**
    * Creates an empty <code>GoodsContainer</code>.
    *
    * @param game The <code>Game</code> in which this <code>GoodsContainer</code> belong.
    * @param location The <code>Location</code> this <code>GoodsContainer</code> will be containg goods for.
    */
    public GoodsContainer(Game game, Location parent) {
        super(game);

        if (parent == null) {
            throw new NullPointerException();
        }

        this.parent = parent;
    }


    /**
    * Initiates a new <code>GoodsContainer</code> from an <code>Element</code>.
    *
    * @param game The <code>Game</code> in which this <code>GoodsContainer</code> belong.
    * @param element The <code>Element</code> (in a DOM-parsed XML-tree) that describes
    *                this object.
    */
    public GoodsContainer(Game game, Location parent, Element element) {
        super(game, element);

        if (parent == null) {
            throw new NullPointerException();
        }

        this.parent = parent;
        readFromXMLElement(element);
    }





    /**
    * Adds a <code>Goods</code> to this containter.
    * @param goods The Goods to add to this container.
    */
    public void addGoods(Goods toadd) {
        // See if there's already goods of this type. If so, unify into one pile.
        Iterator goodsIterator = getGoodsIterator();
        Goods g;
        while (goodsIterator.hasNext()) {
            g = ((Goods) goodsIterator.next());
            if (g.getType() == toadd.getType())
            {
              g.setAmount(g.getAmount() + toadd.getAmount());
              //toadd.dispose(); // This isn't feasible. It causes a NullPointerException. -sjm
              return;
            }
        }
        // If we get this far, we haven't found any goods of the type; make a new pile.
        goods.add(toadd);
    }


    /**
    * Removes Goods from this containter.
    *
    * @param g The Goods to remove from this container.
    * @return The goods that has been removed from this container (if any).
    */
    public Goods removeGoods(Goods g) {
        int index = goods.indexOf(g);

        if (index != -1) {
            return (Goods) goods.remove(index);
        } else {
            return null;
        }
    }
    
    /**
    * Removes a specified amount of a type of Goods from this containter.
    *
    * @param type The type of Goods to remove from this container.
    * @param amount The amount of Goods to remove from this container.
    * @return The goods that have been removed from this container (if any).
    */
    public Goods removeAmountAndTypeOfGoods(int type, int amount) {
        Iterator goodsIterator = getGoodsIterator();
        Goods g, thepackage;
        
        // Find goods of this type.
        while (goodsIterator.hasNext()) {
            g = ((Goods) goodsIterator.next());
            if (g.getType() == type)
            {
                // All of one type of goods should be in one pile, so assume that hereforward.
                if (amount >= g.getAmount())
                {
                    // Don't have enough? Give as much as we can.
                    return removeGoods(g);
                }
                // Make a nice little package of the goods we removed, and remove them.
                thepackage = new Goods(getGame(), null, type, amount);
                g.setAmount(g.getAmount() - amount);
                return thepackage;
            }
        }
        // We can't find anything. Give up.
        return null;
    }


    /**
    * Checks if the specified <code>Goods</code> is in this container.
    *
    * @param The <code>Goods</code> to test the presence of.
    * @return The result.
    */
    public boolean contains(Goods g) {
        return goods.contains(g);
    }


    /**
    * Returns the amount of one type of Goods in this container.
    * @param type The type of Goods being looked for in this container.
    * @return The amount of this type of Goods in this container.
    */
    public int getGoodsCount(int type) {
        Iterator goodsIterator = getGoodsIterator();
        int amount = 0;
        Goods g;
        while (goodsIterator.hasNext()) {
            g = ((Goods) goodsIterator.next());
            if (g.getType() == type)
            {
                amount += g.getAmount();
            }
        }
        return amount;
    }


    /**
    * Returns the total amount of Goods at this Location, e.g. the total tonnage.
    * @return The total amount of Goods at this Location, e.g. the total tonnage.
    */
    public int getTotalGoodsCount() {
        int amount = 0;

        Iterator goodsIterator = getGoodsIterator();

        while (goodsIterator.hasNext()) {
            amount += ((Goods) goodsIterator.next()).getAmount();
        }

        return amount;
    }


    /**
    * Gets an <code>Iterator</code> of every <code>Goods</code> in this
    * <code>GoodsContainer</code>.
    *
    * @return The <code>Iterator</code>.
    */
    public Iterator getGoodsIterator() {
        return goods.iterator();
    }


    /**
    * Gets the first <code>Goods</code> in this <code>GoodsContainer</code>.
    * @return The <code>Goods</code>.
    */
    public Goods getFirstGoods() {
        if (goods.size() == 0) {
            return null;
        } else {
            return (Goods) goods.get(0);
        }
    }


    /**
    * Gets the last <code>Goods</code> in this <code>GoodsContainer</code>.
    * @return The <code>Goods</code>.
    */
    public Goods getLastGoods() {
        if (goods.size() == 0) {
            return null;
        } else {
            return (Goods) goods.get(goods.size() - 1);
        }
    }


    /**
    * Removes all references to this object.
    */
    public void dispose() {
        Iterator i = getGoodsIterator();
        while (i.hasNext()) {
            ((Goods) i.next()).dispose();
        }

        super.dispose();
    }


    /**
    * Prepares this <code>GoodsContainer</code> for a new turn.
    */
    public void newTurn() {

    }


    /**
    * Makes an XML-representation of this object.
    *
    * @param document The document to use when creating new componenets.
    * @return The DOM-element ("Document Object Model") made to represent this "GoodsContainer".
    */
    public Element toXMLElement(Player player, Document document) {
        Element element = document.createElement(getXMLElementTagName());

        element.setAttribute("ID", getID());

        Iterator goodsIterator = getGoodsIterator();

        while (goodsIterator.hasNext()) {
            element.appendChild(((Goods) goodsIterator.next()).toXMLElement(player, document));
        }

        return element;
    }


    /**
    * Initializes this object from an XML-representation of this object.
    * @param element The DOM-element ("Document Object Model") made to represent this "GoodsContainer".
    */
    public void readFromXMLElement(Element element) {
        setID(element.getAttribute("ID"));

        //NodeList goodsNodeList = element.getElementsByTagName(Goods.getXMLElementTagName());
        NodeList goodsNodeList = element.getChildNodes();

        goods.clear();

        for (int i=0; i<goodsNodeList.getLength(); i++) {
            Element goodsElement = (Element) goodsNodeList.item(i);

            // Check if the goods are already here -> only update:
            Goods g = (Goods) getGame().getFreeColGameObject(goodsElement.getAttribute("ID"));

            if (g != null) {
                g.readFromXMLElement(goodsElement);
                goods.add(g);
                //u.setLocation(parent);
            } else {
                g = new Goods(getGame(), goodsElement);
                goods.add(g);
                //u.setLocation(parent);
            }
        }
    }


    /**
    * Gets the tag name of the root element representing this object.
    * @return "goodsContainer".
    */
    public static String getXMLElementTagName() {
        return "goodsContainer";
    }
}
