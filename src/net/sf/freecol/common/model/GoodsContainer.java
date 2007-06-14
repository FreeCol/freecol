
package net.sf.freecol.common.model;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Element;

/**
* Contains goods and can be used by a {@link Location} to make certain
* tasks easier.
*/
public class GoodsContainer extends FreeColGameObject {
    @SuppressWarnings("unused")
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
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public GoodsContainer(Game game, Location parent, XMLStreamReader in) throws XMLStreamException {
        super(game, in);

        if (parent == null) {
            throw new NullPointerException();
        }

        this.parent = parent;
        readFromXML(in);
    }

    /**
     * Initiates a new <code>GoodsContainer</code> from an <code>Element</code>.
     *
     * @param game The <code>Game</code> in which this <code>GoodsContainer</code>
     *       belong.
     * @param parent The object using this <code>GoodsContainer</code>
     *       for storing it's goods.
     * @param e An XML-element that will be used to initialize
     *      this object.
     */
    public GoodsContainer(Game game, Location parent, Element e) {
        super(game, e);

        if (parent == null) {
            throw new NullPointerException();
        }

        this.parent = parent;
        readFromXMLElement(e);
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
    * Removes all goods.
    * 
    */
    public void removeAll() {
        for (int i=0; i<storedGoods.length; i++) {
            storedGoods[i] = 0;
        }
    }

    
    /**
     * Checks if any type of goods, except for
     * <code>Goods.FOOD</code>, has reached the given
     * amount.
     * 
     * @param amount The amount.
     * @return <code>true</code> if any type of goods, 
     * except for <code>Goods.FOOD</code>, has reached 
     * the given amount.
     */
     public boolean hasReachedCapacity(int amount) {
         for (int i=1; i<storedGoods.length; i++) {
             if (storedGoods[i] >= amount) {
                 return true;
             }
         }
         return false;
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
    public Iterator<Goods> getGoodsIterator() {
        return getGoods().iterator();
    }

    /**
    * Returns an <code>ArrayList</code> containing all
    * <code>Goods</code> in this <code>GoodsContainer</code>. Each
    * <code>Goods</code> has a maximum amount of 100.
    *
    * @return The <code>ArrayList</code>.
    * @see #getGoodsIterator
    */
    public ArrayList<Goods> getGoods() {
        ArrayList<Goods> totalGoods = new ArrayList<Goods>();

        for (int i=0; i<storedGoods.length; i++) {
            int j = 0;
            while (storedGoods[i] - j > 0) {
                int a = (storedGoods[i] - j < 100) ? storedGoods[i] - j : 100;
                totalGoods.add(new Goods(getGame(), parent, i, a));
                j += a;
            }
        }

        return totalGoods;
    }

    
    /**
    * Gets an <code>Iterator</code> of every <code>Goods</code> in this
    * <code>GoodsContainer</code>. There is only one <code>Goods</code>
    * for each type of goods.
    *
    * @return The <code>Iterator</code>.
    * @see #getGoodsIterator
    */
    public ArrayList<Goods> getCompactGoods() {
        ArrayList<Goods> totalGoods = new ArrayList<Goods>();

        for (int i=0; i<storedGoods.length; i++) {
            if (storedGoods[i] > 0) {
                totalGoods.add(new Goods(getGame(), parent, i, storedGoods[i]));
            }
        }

        return totalGoods;
    }

    /**
    * Gets an <code>Iterator</code> of every <code>Goods</code> in this
    * <code>GoodsContainer</code>. There is only one <code>Goods</code>
    * for each type of goods.
    *
    * @return The <code>Iterator</code>.
    * @see #getGoodsIterator
    */
    public List<Goods> getFullGoods() {
        ArrayList<Goods> totalGoods = new ArrayList<Goods>();

        for (int i=0; i<storedGoods.length; i++) {
            totalGoods.add(new Goods(getGame(), parent, i, storedGoods[i]));
        }

        return totalGoods;
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
    public void saveState() {

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
     * @param lowLevel The lowest levels not to warn about.
     * @param highLevel The highest levels not to warn about.
     */
    public void cleanAndReport(int limit, int[] lowLevel, int[] highLevel) {

        FreeColGameObject source = (FreeColGameObject) parent;
        int adjustment = limit / 100;
        
        for (int typeOfGoods = 0; typeOfGoods < Goods.NUMBER_OF_TYPES; typeOfGoods++) {
            int low = lowLevel[typeOfGoods] * adjustment;
            int high = highLevel[typeOfGoods] * adjustment;
            if (typeOfGoods == Goods.FOOD) {
                // ignore food
                continue;
            } else if (storedGoods[typeOfGoods] > limit ) {
                // limit has been exceeded
                int waste = storedGoods[typeOfGoods] - limit;
                storedGoods[typeOfGoods] = limit;
                addModelMessage(source, "model.building.warehouseWaste",
                                new String [][] {{"%goods%", Goods.getName(typeOfGoods)},
                                                 {"%waste%", String.valueOf(waste)},
                                                 {"%colony%", ((Colony) parent).getName()}},
                                ModelMessage.WAREHOUSE_CAPACITY,
                                new Goods(typeOfGoods));
            } else if (storedGoods[typeOfGoods] == limit && 
                       oldStoredGoods[typeOfGoods] < limit) {
                // limit has been reached during this turn
                addModelMessage(source, "model.building.warehouseOverfull",
                                new String [][] {{"%goods%", Goods.getName(typeOfGoods)},
                                                 {"%colony%", ((Colony) parent).getName()}},
                                ModelMessage.WAREHOUSE_CAPACITY,
                                new Goods(typeOfGoods));
            } else if (storedGoods[typeOfGoods] > high &&
                       oldStoredGoods[typeOfGoods] <= high) {
                addModelMessage(source, "model.building.warehouseFull",
                                new String [][] {{"%goods%", Goods.getName(typeOfGoods)},
                                                 {"%level%", String.valueOf(high)},
                                                 {"%colony%", ((Colony) parent).getName()}},
                                ModelMessage.WAREHOUSE_CAPACITY,
                                new Goods(typeOfGoods));
            } else if (storedGoods[typeOfGoods] < low &&
                       oldStoredGoods[typeOfGoods] >= low) {
                addModelMessage(source, "model.building.warehouseEmpty",
                                new String [][] {{"%goods%", Goods.getName(typeOfGoods)},
                                                 {"%level%", String.valueOf(low)},
                                                 {"%colony%", ((Colony) parent).getName()}},
                                ModelMessage.WAREHOUSE_CAPACITY,
                                new Goods(typeOfGoods));
            }
        }

    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     * 
     * <br><br>
     * 
     * Only attributes visible to the given <code>Player</code> will 
     * be added to that representation if <code>showAll</code> is
     * set to <code>false</code>.
     *  
     * @param out The target stream.
     * @param player The <code>Player</code> this XML-representation 
     *      should be made for, or <code>null</code> if
     *      <code>showAll == true</code>.
     * @param showAll Only attributes visible to <code>player</code> 
     *      will be added to the representation if <code>showAll</code>
     *      is set to <i>false</i>.
     * @param toSavedGame If <code>true</code> then information that
     *      is only needed when saving a game is added.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame) throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute("ID", getID());

        for (int i=0; i<storedGoods.length; i++) {
            out.writeAttribute("goods" + Integer.toString(i), Integer.toString(storedGoods[i]));
        }
        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setID(in.getAttributeValue(null, "ID"));
        
        storedGoods = new int[Goods.NUMBER_OF_TYPES];

        for (int i=0; i<storedGoods.length; i++) {
            storedGoods[i] = Integer.parseInt(in.getAttributeValue(null, "goods" + Integer.toString(i)));
        }

        in.nextTag();
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
