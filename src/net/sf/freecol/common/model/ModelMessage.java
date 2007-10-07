
package net.sf.freecol.common.model;

import java.util.Arrays;
import java.util.ArrayList;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;

/**
 * Contains a message about a change in the model.
 */
public class ModelMessage extends PersistentObject {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /** Constants describing the type of message. */
    public static final int DEFAULT = 0;
    public static final int WARNING = 1;
    public static final int SONS_OF_LIBERTY = 2;
    public static final int GOVERNMENT_EFFICIENCY = 3;
    public static final int WAREHOUSE_CAPACITY = 4;
    public static final int UNIT_IMPROVED = 5;
    public static final int UNIT_DEMOTED = 6;
    public static final int UNIT_LOST = 7;
    public static final int UNIT_ADDED = 8;
    public static final int BUILDING_COMPLETED = 9;
    public static final int FOREIGN_DIPLOMACY = 10;
    public static final int MARKET_PRICES = 11;
    public static final int LOST_CITY_RUMOUR = 12;
    public static final int GIFT_GOODS = 13;
    public static final int MISSING_GOODS = 14;
    public static final int TUTORIAL = 15;

    private final FreeColGameObject source;
    private final PersistentObject display;
    private final int type;
    private final String messageID;
    private final String[][] data;
    private boolean beenDisplayed = false;


    /**
    * Creates a new <code>ModelMessage</code>.
    *
    * @param source The source of the message. This is what the message should be
    *               associated with. In addition, the owner of the source is the
    *               player getting the message.
    * @param messageID The ID of the message to display.
    * @param data Contains data to be displayed in the message or <i>null</i>.
    * @param type The type of this model message.
    * @param display The Object to display.
    * @see FreeColGameObject#addModelMessage(FreeColGameObject, String, String[][], int)
    */
    public ModelMessage(FreeColGameObject source, String messageID, String[][] data, int type, PersistentObject display) {
        this.source = source;
        this.messageID = messageID;
        this.data = data;
        this.type = type;
        this.display = display;
        verifyFields();
    }
    
    /**
     * Checks that all the fields as they are set in the constructor are valid.
     */
    private void verifyFields() {
        if (source == null) {
            throw new IllegalArgumentException("The source cannot be null.");
        }
        if (messageID == null) {
            throw new IllegalArgumentException("The messageID cannot be null.");
        }
        if (!(display == null ||
              display instanceof FreeColGameObject ||
              display instanceof FreeColGameObjectType)) {
            throw new IllegalArgumentException("The display must be a FreeColGameObject or FreeColGameObjectType!");
        }

        if (data != null) {
            for (String[] s : data) {
                if (s == null || s.length != 2) {
                    throw new IllegalArgumentException("The data can only contain arrays of size 2.");
                }
            }
        }
    }

    /**
    * Creates a new <code>ModelMessage</code>.
    *
    * @param source The source of the message. This is what the message should be
    *               associated with. In addition, the owner of the source is the
    *               player getting the message.
    * @param messageID The ID of the message to display.
    * @param data Contains data to be displayed in the message or <i>null</i>.
    * @param type The type of this model message.
    * @see FreeColGameObject#addModelMessage(FreeColGameObject, String, String[][], int)
    */
    public ModelMessage(FreeColGameObject source, String messageID, String[][] data, int type) {
        this(source, messageID, data, type, getDefaultDisplay(type, source));

    }

    /**
    * Creates a new <code>ModelMessage</code>.
    *
    * @param source The source of the message. This is what the message should be
    *               associated with. In addition, the owner of the source is the
    *               player getting the message.
    * @param messageID The ID of the message to display.
    * @param data Contains data to be displayed in the message or <i>null</i>.
    * @see FreeColGameObject#addModelMessage(FreeColGameObject, String, String[][], int)
    */
    public ModelMessage(FreeColGameObject source, String messageID, String[][] data) {
        this(source, messageID, data, DEFAULT);
    }
    
    /**
     * Returns the default display object for the given type.
     * @param type the type for which to find the default display object.
     * @param source the source object
     * @return an object to be displayed for the message. 
     */
    static private PersistentObject getDefaultDisplay(int type, FreeColGameObject source) {
        PersistentObject newDisplay = null;
        switch(type) {
        case SONS_OF_LIBERTY:
        case GOVERNMENT_EFFICIENCY:
            newDisplay = FreeCol.getSpecification().getGoodsType("model.goods.bells");
            break;
        case LOST_CITY_RUMOUR:
            newDisplay = new LostCityRumour();
            break;
        case UNIT_IMPROVED:
        case UNIT_DEMOTED:
        case UNIT_LOST:
        case UNIT_ADDED:
            newDisplay = source;
            break;
        case BUILDING_COMPLETED:
            newDisplay = FreeCol.getSpecification().getGoodsType("model.goods.hammers");
            break;
        case DEFAULT:
        case WARNING:
        case WAREHOUSE_CAPACITY:
        case FOREIGN_DIPLOMACY:
        case MARKET_PRICES:
        case GIFT_GOODS:
        case MISSING_GOODS:
        default:
            if (source instanceof Player) {
                newDisplay = source;
            }
        }
        return newDisplay;
    }


    /**
    * Checks if this <code>ModelMessage</code> has been displayed.
    * @return <i>true</i> if this <code>ModelMessage</code> has been displayed.
    * @see #setBeenDisplayed
    */
    public boolean hasBeenDisplayed() {
        return beenDisplayed;
    }


    /**
    * Sets the <code>beenDisplayed</code> value of this <code>ModelMessage</code>.
    * This is used to avoid showing the same message twice.
    * 
    * @param beenDisplayed Should be set to <code>true</code> after the
    *       message has been displayed.
    */
    public void setBeenDisplayed(boolean beenDisplayed) {
        this.beenDisplayed = beenDisplayed;
    }


    /**
    * Gets the source of the message. This is what the message
    * should be associated with. In addition, the owner of the source is the
    * player getting the message.
    *
    * @return The source of the message.
    * @see #getOwner
    */
    public FreeColGameObject getSource() {
        return source;
    }

    /**
    * Gets the ID of the message to display.   
    * @return The ID. 
    */
    public String getMessageID() {
        return messageID;
    }
    
    
    /**
    * Returns the data to be displayed in the message.
    * @return The data as a <code>String[][]</code> or <i>null</i>
    *         if no data applies.
    */
    public String[][] getData() {
        return data;
    }

    /**
     * Gets the type of the message to display.   
     * @return The type. 
     */
    public int getType() {
        return type;

    }


    public String getTypeName() {
        switch (type) {
        case WARNING:
            return Messages.message("model.message.warning");
        case SONS_OF_LIBERTY:
            return Messages.message("model.message.sonsOfLiberty");
        case GOVERNMENT_EFFICIENCY:
            return Messages.message("model.message.governmentEfficiency");
        case WAREHOUSE_CAPACITY:
            return Messages.message("model.message.warehouseCapacity");
        case UNIT_IMPROVED:
            return Messages.message("model.message.unitImproved");
        case UNIT_DEMOTED:
            return Messages.message("model.message.unitDemoted");
        case UNIT_LOST:
            return Messages.message("model.message.unitLost");
        case UNIT_ADDED:
            return Messages.message("model.message.unitAdded");
        case BUILDING_COMPLETED:
            return Messages.message("model.message.buildingCompleted");
        case FOREIGN_DIPLOMACY:
            return Messages.message("model.message.foreignDiplomacy");
        case MARKET_PRICES:
            return Messages.message("model.message.marketPrices");
        case LOST_CITY_RUMOUR:
            return Messages.message("model.message.lostCityRumour");
        case GIFT_GOODS:
            return Messages.message("model.message.giftGoods");
        case MISSING_GOODS:
            return Messages.message("model.message.missingGoods");
        case DEFAULT:
        default:
            return Messages.message("model.message.default");
        }
    }


    /**
     * Gets the Object to display.
     * @return The Object to display.
     */
    public PersistentObject getDisplay() {
        return display;
    }

    /**
    * Returns the owner of this message. The owner of this method
    * is the owner of the {@link #getSource source}.
    * 
    * @return The owner of the message. This is the <code>Player</code>
    *       who should receive the message.
    */
    public Player getOwner() {
        if (source instanceof Unit) {
            return ((Unit) source).getOwner();
        } else if (source instanceof Settlement) {
            return ((Settlement) source).getOwner();
        } else if (source instanceof Europe) {
            return ((Europe) source).getOwner();
        } else if (source instanceof Player) {
            return (Player) source;
        } else if (source instanceof Ownable) {
            return ((Ownable) source).getOwner();
        }

        return null;
    }


    /**
    * Checks if this <code>ModelMessage</code> is equal to another <code>ModelMessage</code>.
    * @return <i>true</i> if the sources, message IDs and data are equal.
    */
    @Override
    public boolean equals(Object o) {
        if ( ! (o instanceof ModelMessage) ) { return false; }
        ModelMessage m = (ModelMessage) o;
        // Check that the content of the data array is equal
        if (!(data == m.data)) {
            if (data != null && m.data != null && data.length == m.data.length) {
                for (int i = 0 ; i < data.length ; i++) {
                    if (!Arrays.equals(data[i], m.data[i]))
                        return false;
                }
            } else {
                return false;
            }
        }
        return ( source.equals(m.source)
                && messageID.equals(m.messageID)
                && type == m.type );
    }
    
    @Override
    public int hashCode() {
        int value = 1;
        value = 37 * value + source.hashCode();
        value = 37 * value + messageID.hashCode();
        if (data != null) {
            for (String[] s : data) {
                value = 37 * value + s[0].hashCode();
                value = 37 * value + s[1].hashCode();
            }
        }
        value = 37 * value + type;
        return value;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ModelMessage<");
        sb.append(hashCode() + ", " + source.getID() + ", " + messageID + ", ");
        if (data != null) {
            for (String[] s : data) {
                sb.append(Arrays.toString(s) + "/");
            }
        }
        sb.append(", " + type + " >");
        return sb.toString();
    }

    // TODO: make this serializable; can we avoid passing Game in Constructor?
    
    public static String getXMLElementTagName() {
        return "modelMessage";
    }

    public void toXML(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame)
        throws XMLStreamException {
        toXML(out);
    }

    public void toXML(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());
        out.writeAttribute("source", source.getID());
        if (display instanceof FreeColGameObject) {
            out.writeAttribute("display", ((FreeColGameObject) display).getID());
        } else if (display instanceof FreeColGameObjectType) {
            out.writeAttribute("display", ((FreeColGameObjectType) display).getID());
        }
        out.writeAttribute("type", String.valueOf(type));
        out.writeAttribute("messageID", messageID);
        out.writeAttribute("hasBeenDisplayed", String.valueOf(beenDisplayed));
        ArrayList<String> flatData = new ArrayList<String>();
        for (String[] element : data) {
            flatData.add(element[0]);
            flatData.add(element[1]);
        }
        toArrayElement("data", flatData.toArray(new String[] {}), out);
        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {


    }

}
