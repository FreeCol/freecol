
package net.sf.freecol.common.model;


import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


public class GoodsTradeItem extends TradeItem {
    
    private Goods goods;
    private Settlement settlement;
        
    public GoodsTradeItem(Game game, Player source, Player destination, Goods goods, Settlement settlement) {
        super(game, "tradeItem.goods", source, destination);
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
                this.goods = new Goods(getGame(), in);
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
    public static String getXMLElementTagName() {
        return "goodsTradeItem";
    }

}


