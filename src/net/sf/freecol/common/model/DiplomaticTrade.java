package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

/**
 * The class <code>DiplomaticTrade</code> represents an offer one player can
 * make another.
 * 
 */
public class DiplomaticTrade extends PersistentObject {

    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String REVISION = "$Revision$";

    // the individual items the trade consists of
    private ArrayList<TradeItem> items = new ArrayList<TradeItem>();

    @SuppressWarnings("unused")
    private final Game game;

    public DiplomaticTrade(Game game) {
        this.game = game;
    }

    /**
     * Add a TradeItem to the DiplomaticTrade.
     * 
     * @param newItem
     *            a <code>TradeItem</code> value
     */
    public void add(TradeItem newItem) {
        items.add(newItem);
    }

    /**
     * Remove a TradeItem from the DiplomaticTrade.
     * 
     * @param newItem
     *            a <code>TradeItem</code> value
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
     * 
     * @param in
     *            The input stream with the XML.
     * @throws XMLStreamException
     *             if a problem was encountered during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in)
            throws XMLStreamException {
    }

    /**
     * This method writes an XML-representation of this object to the given
     * stream.
     * 
     * <br>
     * <br>
     * 
     * Only attributes visible to the given <code>Player</code> will be added
     * to that representation if <code>showAll</code> is set to
     * <code>false</code>.
     * 
     * @param out
     *            The target stream.
     * @param player
     *            The <code>Player</code> this XML-representation should be
     *            made for, or <code>null</code> if
     *            <code>showAll == true</code>.
     * @throws XMLStreamException
     *             if there are any problems writing to the stream.
     */
    public void toXML(XMLStreamWriter out, Player player)
            throws XMLStreamException {
    }

    /**
     * Gets the tag name of the root element representing this object.
     * 
     * @return "goods".
     */
    public static String getXMLElementTagName() {
        return "diplomaticTrade";
    }

}
