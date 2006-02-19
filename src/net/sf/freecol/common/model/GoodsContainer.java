
package net.sf.freecol.common.model;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
* Contains goods and can be used by a {@link Location} to make certain
* tasks easier.
*/
public class GoodsContainer extends FreeColGameObject {
    private static Logger logger = Logger.getLogger(Location.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /** The list of Goods stored in this <code>GoodsContainer</code>. */
    private int[] storedGoods = new int[Goods.NUMBER_OF_TYPES];

    /** The previous list of Goods stored in this <code>GoodsContainer</code>. */
    private int[] oldStoredGoods = new int[Goods.NUMBER_OF_TYPES];

    /** The owner of this <code>GoodsContainer</code>. */
    private Location parent;




    /**
    * Creates an empty <code>GoodsContainer</code>.
    *
    * @param game The <code>Game</code> in which this <code>GoodsContainer</code> belong.
    * @param parent The <code>Location</code> this <code>GoodsContainer</code> will be containg goods for.
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
    * @param game The <code>Game</code> in which this <code>GoodsContainer</code>
    *       belong.
    * @param parent The object using this <code>GoodsContainer</code>
    *       for storing it's goods.
    * @param element The <code>Element</code> (in a DOM-parsed XML-tree) 
    *       that describes this object.
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
    * @param g The Goods to add to this container.
    */
    public void addGoods(Goods g) {
        addGoods(g.getType(), g.getAmount());
    }


    public void addGoods(int type, int amount) {
        if (storedGoods[type] + amount < 0) {
            throw new IllegalStateException("Operation would leave " + (storedGoods[type] + amount) + " goods of type " + type + " here.");
        }        

        storedGoods[type] += amount;
    }


    /**
    * Removes Goods from this containter.
    *
    * @param g The Goods to remove from this container.
    * @return The goods that has been removed from this container (if any).
    */
    public Goods removeGoods(Goods g) {
        removeGoods(g.getType(), g.getAmount());
        return g;
    }


    /**
    * Removes the given amount of the given type of goods.
    *
    * @param type The type of goods to remove.
    * @param amount The type of amount to remove.
    */
    public void removeGoods(int type, int amount) {
        if (storedGoods[type] - amount < 0) {
            throw new IllegalStateException("Operation would leave " + (storedGoods[type] - amount) + " goods of type " 
                    + Goods.getName(type) + " (" + type + ") here. Location: " + parent);
        }

        storedGoods[type] -= amount;
    }
    
    
    /**
    * Removes all goods above given amount, except for
    * <code>Goods.FOOD</code> which is left unchanged.
    * 
    * @param amount The treshold.
    */
    public void removeAbove(int amount) {
        for (int i=1; i<storedGoods.length; i++) {
            if (storedGoods[i] > amount) {
                storedGoods[i] = amount;
            }
        }
    }


    /**
    * Checks if the specified <code>Goods</code> is in this container.
    *
    * @param g The <code>Goods</code> to test the presence of.
    * @return The result.
    */
    public boolean contains(Goods g) {
        throw new UnsupportedOperationException();
    }


    /**
    * Returns the amount of one type of Goods in this container.
    * @param type The type of Goods being looked for in this container.
    * @return The amount of this type of Goods in this container.
    */
    public int getGoodsCount(int type) {
        return storedGoods[type];
    }


    /**
    * Gets the number of goods-packages. A goods package contain between 1-100.
    * @return The number of goods packages.
    */
    public int getGoodsCount() {
        int count = 0;
        for (int i=0; i<storedGoods.length; i++) {
            int j = 0;
            while (storedGoods[i] - j > 0) {
                count++;
                j+=100;
            }
        }

        return count;
    }


    /**
    * Gets an <code>Iterator</code> of every <code>Goods</code> in this
    * <code>GoodsContainer</code>. Each <code>Goods</code> have a maximum
    * amount of 100.
    *
    * @return The <code>Iterator</code>.
    * @see #getCompactGoodsIterator
    */
    public Iterator getGoodsIterator() {
        ArrayList totalGoods = new ArrayList();

        for (int i=0; i<storedGoods.length; i++) {
            int j = 0;
            while (storedGoods[i] - j > 0) {
                int a = (storedGoods[i] - j < 100) ? storedGoods[i] - j : 100;
                totalGoods.add(new Goods(getGame(), parent, i, a));
                j += a;
            }
        }

        return totalGoods.iterator();
    }

    
    /**
    * Gets an <code>Iterator</code> of every <code>Goods</code> in this
    * <code>GoodsContainer</code>. There is only one <code>Goods</code>
    * for each type of goods.
    *
    * @return The <code>Iterator</code>.
    * @see #getGoodsIterator
    */
    public Iterator getCompactGoodsIterator() {
        ArrayList totalGoods = new ArrayList();

        for (int i=0; i<storedGoods.length; i++) {
            if (storedGoods[i] > 0) {
                totalGoods.add(new Goods(getGame(), parent, i, storedGoods[i]));
            }
        }

        return totalGoods.iterator();
    }

    /**
    * Gets an <code>Iterator</code> of every <code>Goods</code> in this
    * <code>GoodsContainer</code>. There is only one <code>Goods</code>
    * for each type of goods.
    *
    * @return The <code>Iterator</code>.
    * @see #getGoodsIterator
    */
    public Iterator getFullGoodsIterator() {
        ArrayList totalGoods = new ArrayList();

        for (int i=0; i<storedGoods.length; i++) {
            totalGoods.add(new Goods(getGame(), parent, i, storedGoods[i]));
        }

        return totalGoods.iterator();
    }


    /**
    * Gets the first <code>Goods</code> in this <code>GoodsContainer</code>.
    * @return The <code>Goods</code>.
    */
    public Goods getFirstGoods() {
        throw new UnsupportedOperationException();
    }


    /**
    * Gets the last <code>Goods</code> in this <code>GoodsContainer</code>.
    * @return The <code>Goods</code>.
    */
    public Goods getLastGoods() {
        throw new UnsupportedOperationException();
    }


    /**
    * Removes all references to this object.
    */
    public void dispose() {
        super.dispose();
    }


    /**
    * Prepares this <code>GoodsContainer</code> for a new turn.
    */
    public void newTurn() {

        for (int i = 0; i < Goods.NUMBER_OF_TYPES; i++) {
            oldStoredGoods[i] = storedGoods[i];
        }

    }

    /**
     * Removes goods exceeding limit and reports on goods exceeding levels.
     *
     * Note: The levels should be in descending order.
     *
     * @param limit The capacity of this <code>GoodsContainer</code>.
     * @param levels An array of level to report about (probably {200, 100}).
     */
    public void cleanAndReport(int limit, int[] levels) {

        FreeColGameObject source = (FreeColGameObject) parent;
        
        for (int i = 0; i < Goods.NUMBER_OF_TYPES; i++) {
            if (i == Goods.FOOD) {
                // ignore food
                continue;
            } else if (storedGoods[i] > limit ) {
                // limit has been exceeded
                storedGoods[i] = limit;
                addModelMessage(source, "model.building.warehouseOverfull",
                                new String [][] {{"%goods%", Goods.getName(i)},
                                                 {"%colony%", ((Colony) parent).getName()}},
                                ModelMessage.WAREHOUSE_CAPACITY);
            } else if (storedGoods[i] == limit && oldStoredGoods[i] < limit) {
                // limit has been reached during this turn
                addModelMessage(source, "model.building.warehouseOverfull",
                                new String [][] {{"%goods%", Goods.getName(i)},
                                                 {"%colony%", ((Colony) parent).getName()}},
                                ModelMessage.WAREHOUSE_CAPACITY) ;
            } else {
                // check whether certain levels have been exceeded
                for (int k = 0; k < levels.length; k++) {
                    if (storedGoods[i] > levels[k] && oldStoredGoods[i] <= levels[k]) {
                        addModelMessage(source, "model.building.warehouseFull",
                                        new String [][] {{"%goods%", Goods.getName(i)},
                                                         {"%level%", String.valueOf(levels[k])},
                                                         {"%colony%", ((Colony) parent).getName()}},
                                        ModelMessage.WAREHOUSE_CAPACITY) ;
                        // don't report on more than one level
                        break;
                    }
                }
            }
        }

    }

    /**
    * Makes an XML-representation of this object.
    *
    * @param document The document to use when creating new componenets.
    * @return The DOM-element ("Document Object Model") made to represent this "GoodsContainer".
    */
    public Element toXMLElement(Player player, Document document, boolean showAll, boolean toSavedGame) {
        Element element = document.createElement(getXMLElementTagName());

        element.setAttribute("ID", getID());

        for (int i=0; i<storedGoods.length; i++) {
            element.setAttribute("goods" + Integer.toString(i), Integer.toString(storedGoods[i]));
        }
        return element;
    }


    /**
    * Initializes this object from an XML-representation of this object.
    * @param element The DOM-element ("Document Object Model") made to represent this "GoodsContainer".
    */
    public void readFromXMLElement(Element element) {
        setID(element.getAttribute("ID"));

        storedGoods = new int[Goods.NUMBER_OF_TYPES];

        for (int i=0; i<storedGoods.length; i++) {
            storedGoods[i] = Integer.parseInt(element.getAttribute("goods" + Integer.toString(i)));
        }

    }


    /**
    * Gets the tag name of the root element representing this object.
    * @return "goodsContainer".
    */
    public static String getXMLElementTagName() {
        return "goodsContainer";
    }
    
    
    /**
    * Creates a <code>String</code> representation of this
    * <code>GoodsContainer</code>.    
    */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("GoodsContainer with: ");
        for (int i=0; i<storedGoods.length; i++) {
            sb.append(Goods.getName(i) + "=" + storedGoods[i] + ", ");
        }
        return sb.toString();
    }

}
