
package net.sf.freecol.common.model;


import java.util.Map;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.client.gui.i18n.Messages;

public final class GoodsType extends FreeColGameObjectType
{
    public static final  String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final  String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final  String  REVISION = "$Revision$";

    private boolean isFarmed;
    private boolean isFood;
    private boolean ignoreLimit;

    private GoodsType madeFrom;
    private GoodsType makes;
    
    private GoodsType storedAs;
    private boolean storable;

    private String art;
    
    private int initialAmount;
    private int initialPrice;
    private int priceDiff;

    // ----------------------------------------------------------- constructors

    public GoodsType(int index) {
        setIndex(index);
    }

    // ----------------------------------------------------------- retriveal methods

    public String getName(boolean sellable) {
        if (sellable) {
            return getName();
        } else {
            return getName() + " (" + Messages.message("model.goods.Boycotted") + ")";
        }
    }

    public boolean isRawMaterial() {
        return makes != null;
    }

    public boolean isRefined() {
        return madeFrom != null;
    }

    public GoodsType getRawMaterial() {
        return madeFrom;
    }

    public GoodsType getProducedMaterial() {
        return makes;
    }

    public boolean isFarmed() {
        return isFarmed;
    }

    public boolean limitIgnored() {
        return ignoreLimit;
    }

    public boolean isStorable() {
        return storable;
    }

    public GoodsType getStoredAs() {
        return storedAs;
    }

    public int getInitialAmount() {
        return initialAmount;
    }

    public int getInitialSellPrice() {
        return initialPrice;
    }

    public int getInitialBuyPrice() {
        return initialPrice + priceDiff;
    }

    public int getPriceDifference() {
        return priceDiff;
    }

    // TODO: give this some meaning
    // Originally intended for when there are no static variables
    public boolean isFoodType() {
        return isFood;
    }
    
    public String getArt() {
        return art;
    }

    public GoodsType outputType() {
        return makes;
    }

    public GoodsType inputType() {
        return madeFrom;
    }

    // ------------------------------------------------------------ API methods

    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        readFromXML(in, null);
    }

    public void readFromXML(XMLStreamReader in, final Map<String, GoodsType> goodsTypeByRef)
            throws XMLStreamException {
        setID(in.getAttributeValue(null, "id"));
        isFarmed = parseTruth(in.getAttributeValue(null, "is-farmed"));
        isFood = getAttribute(in, "is-food", false);
        ignoreLimit = getAttribute(in, "ignore-limit", false);
        art = in.getAttributeValue(null, "art");

        if (hasAttribute(in, "made-from")) {
            String  madeFromRef = in.getAttributeValue(null, "made-from");
            GoodsType  rawMaterial = goodsTypeByRef.get(madeFromRef);
            madeFrom = rawMaterial;
            if (rawMaterial != null) {
                rawMaterial.makes = this;
            }
        }

        storable = getAttribute(in, "storable", true);
        if (hasAttribute(in, "stored-as")) {
            storedAs = goodsTypeByRef.get(in.getAttributeValue(null, "stored-as"));
        }

        // Only expected child is 'market'
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            initialAmount = Integer.parseInt(in.getAttributeValue(null, "initial-amount"));
            initialPrice = getAttribute(in, "initial-price", 1);
            priceDiff = getAttribute(in, "price-difference", 1);
            in.nextTag(); // close this element
        }
    }

}
