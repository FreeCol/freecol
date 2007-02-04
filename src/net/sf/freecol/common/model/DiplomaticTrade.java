
package net.sf.freecol.common.model;


import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;


/**
 * The class <code>DiplomaticTrade</code> represents an offer one
 * player can make another.
 *
 */
public class DiplomaticTrade extends PersistentObject {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE   = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    // the individual items the trade consists of
    private ArrayList<TradeItem> items = new ArrayList<TradeItem>();

    private final Game game;

    public DiplomaticTrade(Game game) {
        this.game = game;
    }

    /**
     * Add a TradeItem to the DiplomaticTrade.
     *
     * @param newItem a <code>TradeItem</code> value
     */
    public void add(TradeItem newItem) {
        items.add(newItem);
    }

    /**
     * Remove a TradeItem from the DiplomaticTrade.
     *
     * @param newItem a <code>TradeItem</code> value
     */
    public void remove(TradeItem newItem) {
        items.remove(newItem);
    }

    /**
     * Returns an iterator for all TradeItems.
     *
     * @return an iterator for all TradeItems.
     */
    public Iterator<TradeItem> iterator() {
        return items.iterator();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
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
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    public void toXML(XMLStreamWriter out, Player player) throws XMLStreamException {
    }
    

    /**
     * Gets the tag name of the root element representing this object.
     * @return "goods".
     */
    public String getXMLElementTagName() {
        return "diplomaticTrade";
    }


    /**
     * One of the items a DiplomaticTrade consists of.
     *
     */
    public abstract class TradeItem extends PersistentObject {
    
        // the ID, used to get a name, etc.
        protected String ID;
        // the player offering something
        protected Player source;
        // the player who is to receive something
        protected Player destination;
        
        /**
         * Creates a new <code>TradeItem</code> instance.
         *
         * @param id a <code>String</code> value
         * @param source a <code>Player</code> value
         * @param destination a <code>Player</code> value
         */
        public TradeItem(String id, Player source, Player destination) {
            this.ID = id;
            this.source = source;
            this.destination = destination;
        }

        /**
         * Returns whether this TradeItem is valid.
         *
         * @return a <code>boolean</code> value
         */
        public abstract boolean isValid();

        /**
         * Concludes the trade.
         *
         */
        public abstract void makeTrade();

        /**
         * Initialize this object from an XML-representation of this object.
         * @param in The input stream with the XML.
         * @throws XMLStreamException if a problem was encountered
         *      during parsing.
         */
        protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
            this.ID = in.getAttributeValue(null, "ID");
            String sourceID = in.getAttributeValue(null, "source");
            this.source = (Player) game.getFreeColGameObject(sourceID);
            String destinationID = in.getAttributeValue(null, "destination");
            this.destination = (Player) game.getFreeColGameObject(destinationID);
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
         * @throws XMLStreamException if there are any problems writing
         *      to the stream.
         */
        public void toXML(XMLStreamWriter out, Player player) throws XMLStreamException {
            out.writeAttribute("ID", this.ID);
            out.writeAttribute("source", this.source.getID());
            out.writeAttribute("destination", this.destination.getID());
        }
    

    }

    public class GoldTradeItem extends TradeItem {
    
        private int gold;
        
        public GoldTradeItem(Player source, Player destination, int gold) {
            super("tradeItem.gold", source, destination);
            this.gold = gold;
        }

        public boolean isValid() {
            return ((gold >= 0) && (source.getGold() >= gold));
        }

        public void makeTrade() {
            source.modifyGold(-gold);
            destination.modifyGold(gold);
        }


        /**
         * Initialize this object from an XML-representation of this object.
         * @param in The input stream with the XML.
         * @throws XMLStreamException if a problem was encountered
         *      during parsing.
         */
        protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
            super.readFromXMLImpl(in);
            this.gold = Integer.parseInt(in.getAttributeValue(null, "gold"));
            in.nextTag();
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
         * @throws XMLStreamException if there are any problems writing
         *      to the stream.
         */
        public void toXML(XMLStreamWriter out, Player player) throws XMLStreamException {
            out.writeStartElement(getXMLElementTagName());
            super.toXML(out, player);
            out.writeAttribute("gold", Integer.toString(this.gold));
            out.writeEndElement();
        }
    
        /**
         * Gets the tag name of the root element representing this object.
         * @return "goods".
         */
        public String getXMLElementTagName() {
            return "goldTradeItem";
        }

    }

    public class StanceTradeItem extends TradeItem {
    
        private int stance;
        
        public StanceTradeItem(Player source, Player destination, int stance) {
            super("tradeItem.stance", source, destination);
            this.stance = stance;
        }

        public boolean isValid() {
            return (stance == Player.WAR ||
                    stance == Player.CEASE_FIRE ||
                    stance == Player.PEACE ||
                    stance == Player.ALLIANCE);
        }

        public void makeTrade() {
            source.setStance(destination, stance);
            destination.setStance(source, stance);
        }


        /**
         * Initialize this object from an XML-representation of this object.
         * @param in The input stream with the XML.
         * @throws XMLStreamException if a problem was encountered
         *      during parsing.
         */
        protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
            super.readFromXMLImpl(in);
            this.stance = Integer.parseInt(in.getAttributeValue(null, "stance"));
            in.nextTag();
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
         * @throws XMLStreamException if there are any problems writing
         *      to the stream.
         */
        public void toXML(XMLStreamWriter out, Player player) throws XMLStreamException {
            out.writeStartElement(getXMLElementTagName());
            super.toXML(out, player);
            out.writeAttribute("stance", Integer.toString(this.stance));
            out.writeEndElement();
        }
    
        /**
         * Gets the tag name of the root element representing this object.
         * @return "goods".
         */
        public String getXMLElementTagName() {
            return "stanceTradeItem";
        }

    }

    public class GoodsTradeItem extends TradeItem {
    
        private Goods goods;
        private Settlement settlement;
        
        public GoodsTradeItem(Player source, Player destination, Goods goods, Settlement settlement) {
            super("tradeItem.goods", source, destination);
            this.goods = goods;
            this.settlement = settlement;
        }

        public boolean isValid() {
            if (!(goods.getLocation() instanceof Unit)) {
                return false;
            }
            Unit unit = (Unit) goods.getLocation();
            if (unit.getOwner() != source) {
                return false;
            }
            if (settlement != null && settlement.getOwner() == destination) {
                return true;
            } else {
                return false;
            }

        }

        public void makeTrade() {
            goods.getLocation().remove(goods);
            settlement.add(goods);
        }

        /**
         * Initialize this object from an XML-representation of this object.
         * @param in The input stream with the XML.
         * @throws XMLStreamException if a problem was encountered
         *      during parsing.
         */
        protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
            super.readFromXMLImpl(in);
            while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
                if (in.getLocalName().equals(Goods.getXMLElementTagName())) {
                    this.goods = new Goods(game, in);
                }
            }
            in.nextTag();
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
         * @throws XMLStreamException if there are any problems writing
         *      to the stream.
         */
        public void toXML(XMLStreamWriter out, Player player) throws XMLStreamException {
            out.writeStartElement(getXMLElementTagName());
            super.toXML(out, player);
            this.goods.toXML(out, player);
            out.writeEndElement();
        }
    
        /**
         * Gets the tag name of the root element representing this object.
         * @return "goods".
         */
        public String getXMLElementTagName() {
            return "goodsTradeItem";
        }

    }


    public class ColonyTradeItem extends TradeItem {
    
        private Colony colony;
        
        public ColonyTradeItem(Player source, Player destination, Colony colony) {
            super("tradeItem.colony", source, destination);
            this.colony = colony;
        }

        public boolean isValid() {
            return (colony.getOwner() == source);
        }

        public void makeTrade() {
            colony.setOwner(destination);
        }

        /**
         * Initialize this object from an XML-representation of this object.
         * @param in The input stream with the XML.
         * @throws XMLStreamException if a problem was encountered
         *      during parsing.
         */
        protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
            super.readFromXMLImpl(in);
            String colonyID = in.getAttributeValue(null, "colony");
            this.colony = (Colony) game.getFreeColGameObject(colonyID);
            in.nextTag();
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
         * @throws XMLStreamException if there are any problems writing
         *      to the stream.
         */
        public void toXML(XMLStreamWriter out, Player player) throws XMLStreamException {
            out.writeStartElement(getXMLElementTagName());
            super.toXML(out, player);
            out.writeAttribute("colony", this.colony.getID());
            out.writeEndElement();
        }
    
        /**
         * Gets the tag name of the root element representing this object.
         * @return "goods".
         */
        public String getXMLElementTagName() {
            return "colonyTradeItem";
        }

    }

    public class UnitTradeItem extends TradeItem {
    
        private Unit unit;
        
        public UnitTradeItem(Player source, Player destination, Unit unit) {
            super("tradeItem.unit", source, destination);
            this.unit = unit;
        }

        public boolean isValid() {
            return (unit.getOwner() == source);
        }

        public void makeTrade() {
            unit.setOwner(destination);
        }


        /**
         * Initialize this object from an XML-representation of this object.
         * @param in The input stream with the XML.
         * @throws XMLStreamException if a problem was encountered
         *      during parsing.
         */
        protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
            super.readFromXMLImpl(in);
            String unitID = in.getAttributeValue(null, "unit");
            this.unit = (Unit) game.getFreeColGameObject(unitID);
            in.nextTag();
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
         * @throws XMLStreamException if there are any problems writing
         *      to the stream.
         */
        public void toXML(XMLStreamWriter out, Player player) throws XMLStreamException {
            out.writeStartElement(getXMLElementTagName());
            super.toXML(out, player);
            out.writeAttribute("unit", this.unit.getID());
            out.writeEndElement();
        }
    
        /**
         * Gets the tag name of the root element representing this object.
         * @return "goods".
         */
        public String getXMLElementTagName() {
            return "unitTradeItem";
        }

    }

}
