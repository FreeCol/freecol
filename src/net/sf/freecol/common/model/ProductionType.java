/**
 *  Copyright (C) 2002-2013   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


/**
 * This class describes a possible production type of a tile or building.
 */
public class ProductionType {

    /**
     * Whether this production type applies only to colony center tiles.
     */
    private boolean colonyCenterTile;

    /**
     * The production level of this production type (used by difficulty levels).
     */
    private String productionLevel;

    /**
     * The goods that are produced by this production type.
     */
    private List<AbstractGoods> outputs;

    /**
     * The goods that are consumed by this production type.
     */
    private List<AbstractGoods> inputs;

    /**
     * Creates a new production type that consumes no raw materials
     * and produces the given output.
     *
     * @param outputs the goods produced
     */
    public ProductionType(List<AbstractGoods> outputs) {
        this.outputs = outputs;
    }

    /**
     * Creates a new production type that consumes the given raw materials
     * and produces the given output.
     *
     * @param inputs the goods consumed
     * @param outputs the goods produced
     */
    public ProductionType(List<AbstractGoods> inputs, List<AbstractGoods> outputs) {
        this.inputs = inputs;
        this.outputs = outputs;
    }

    /**
     * Convenience constructor for a production type with a single output.
     *
     * @param output an <code>AbstractGoods</code> value
     * @param center a <code>boolean</code> value
     * @param level a <code>String</code> value
     */
    public ProductionType(AbstractGoods output, boolean center, String level) {
        outputs = new ArrayList<AbstractGoods>();
        outputs.add(output);
        colonyCenterTile = center;
        productionLevel = level;
    }

    /**
     * Creates a new <code>ProductionType</code> instance.
     *
     * @param in a <code>XMLStreamReader</code> value
     */
    public ProductionType(XMLStreamReader in) {
        readFromXML(in);
    }

    /**
     * Get the <code>Inputs</code> value.
     *
     * @return a <code>List<AbstractGoods></code> value
     */
    public final List<AbstractGoods> getInputs() {
        return inputs;
    }

    /**
     * Set the <code>Inputs</code> value.
     *
     * @param newInputs The new Inputs value.
     */
    public final void setInputs(final List<AbstractGoods> newInputs) {
        this.inputs = newInputs;
    }

    /**
     * Get the <code>Outputs</code> value.
     *
     * @return a <code>List<AbstractGoods></code> value
     */
    public final List<AbstractGoods> getOutputs() {
        return outputs;
    }

    /**
     * Set the <code>Outputs</code> value.
     *
     * @param newOutputs The new Outputs value.
     */
    public final void setOutputs(final List<AbstractGoods> newOutputs) {
        this.outputs = newOutputs;
    }

    /**
     * Returns true if this production type only applies to colony
     * center tiles.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean isColonyCenterTile() {
        return colonyCenterTile;
    }

    /**
     * Set the <code>ColonyCenterTile</code> value.
     *
     * @param newColonyCenterTile The new ColonyCenterTile value.
     */
    public final void setColonyCenterTile(final boolean newColonyCenterTile) {
        this.colonyCenterTile = newColonyCenterTile;
    }

    /**
     * The production level of this type of production (used by
     * difficulty levels).
     *
     * @return a <code>String</code> value
     */
    public final String getProductionLevel() {
        return productionLevel;
    }

    /**
     * Set the <code>ProductionLevel</code> value.
     *
     * @param newProductionLevel The new ProductionLevel value.
     */
    public final void setProductionLevel(final String newProductionLevel) {
        this.productionLevel = newProductionLevel;
    }


    /**
     * Makes an XML-representation of this object.
     *
     * @param out The output stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    protected void toXML(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement("production");
        if (colonyCenterTile) {
            out.writeAttribute("colonyCenterTile", "true");
        }
        if (productionLevel != null) {
            out.writeAttribute("productionLevel", productionLevel);
        }
        for (AbstractGoods input : inputs) {
            input.toXML(out, "input");
        }
        for (AbstractGoods output : outputs) {
            output.toXML(out, "output");
        }
        out.writeEndElement();
    }

    /**
     * Initializes this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    public void readFromXML(XMLStreamReader in) throws XMLStreamException {
        colonyCenterTile = "true".equalsIgnoreCase(in.getAttributeValue(null, "colonyCenterTile"));
        productionLevel = in.getAttributeValue(null, "productionLevel");

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String childName = in.getLocalName();
            GoodsType type = getSpecification().getGoodsType(in.getAttributeValue(null, "goods-type"));
            int amount = Integer.parseInt(in.getAttributeValue(null, "value"));
            if ("input".equals(childName)) {
                inputs.add(new AbstractGoods(type, amount));
            } else if ("output".equals(childName)) {
                outputs.add(new AbstractGoods(type, amount));
            }
        }
    }

}