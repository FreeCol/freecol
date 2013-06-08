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
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;


/**
 * This class describes a possible production type of a tile or building.
 */
public class ProductionType extends FreeColObject {

    /** Whether this production type applies only to colony center tiles. */
    private boolean unattended;

    /**
     * The production level of this production type (usually a
     * difficulty level key).
     */
    private String productionLevel;

    /** The goods that are produced by this production type. */
    private List<AbstractGoods> outputs = null;

    /** The goods that are consumed by this production type. */
    private List<AbstractGoods> inputs = null;


    /**
     * Simple constructor.
     *
     * @param specification The <code>Specification</code> to refer to.
     */
    public ProductionType(Specification specification) {
        setSpecification(specification);
    }

    /**
     * Creates a new production type that consumes no raw materials
     * and produces the given output.
     *
     * @param outputs A list of the <code>AbstractGoods</code> produced.
     */
    public ProductionType(List<AbstractGoods> outputs) {
        this.outputs = outputs;
    }

    /**
     * Creates a new production type that consumes the given raw materials
     * and produces the given output.
     *
     * @param inputs A list of the <code>AbstractGoods</code> consumed.
     * @param outputs A list of the <code>AbstractGoods</code> produced.
     */
    public ProductionType(List<AbstractGoods> inputs, List<AbstractGoods> outputs) {
        this.inputs = inputs;
        this.outputs = outputs;
    }

    /**
     * Convenience constructor for a production type with a single output.
     *
     * @param output The <code>AbstractGoods</code> produced.
     * @param center True if this is a colony center tile.
     * @param level The difficulty level key.
     */
    public ProductionType(AbstractGoods output, boolean center, String level) {
        outputs = new ArrayList<AbstractGoods>();
        outputs.add(output);
        unattended = center;
        productionLevel = level;
    }

    /**
     * Convenience constructor for a new <code>ProductionType</code>
     * instance with a single input and output.
     *
     * @param input The <code>GoodsType</code> consumed.
     * @param output The <code>GoodsType</code> produced.
     * @param amount The amount of goods both produced and consumed.
     */
    public ProductionType(GoodsType input, GoodsType output, int amount) {
        if (input != null) {
            inputs = new ArrayList<AbstractGoods>();
            inputs.add(new AbstractGoods(input, amount));
        }
        if (output != null) {
            outputs = new ArrayList<AbstractGoods>();
            outputs.add(new AbstractGoods(output, amount));
        }
    }

    /**
     * Creates a new <code>ProductionType</code> instance.
     *
     * @param xr The <code>FreeColXMLReader</code> to read from.
     * @param specification The <code>Specification</code> to refer to.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public ProductionType(FreeColXMLReader xr,
                          Specification specification) throws XMLStreamException {
        this(specification);

        readFromXML(xr);
    }


    /**
     * Get the input goods.
     *
     * @return A list of the input <code>AbstractGoods</code>.
     */
    public final List<AbstractGoods> getInputs() {
        return inputs;
    }

    /**
     * Set the input goods.
     *
     * @param newInputs The new list of input <code>AbstractGoods</code>.
     */
    public final void setInputs(final List<AbstractGoods> newInputs) {
        this.inputs = newInputs;
    }

    /**
     * Add a new input.
     *
     * @param type The <code>GoodsType</code> to add.
     * @param amount The amount of goods.
     */
    private void addInput(GoodsType type, int amount) {
        if (inputs == null) inputs = new ArrayList<AbstractGoods>(1);
        inputs.add(new AbstractGoods(type, amount));
    }

    /**
     * Get the output goods.
     *
     * @return A list of the output <code>AbstractGoods</code>.
     */
    public final List<AbstractGoods> getOutputs() {
        return outputs;
    }

    /**
     * Set the output goods.
     *
     * @param newOutputs The new list of output <code>AbstractGoods</code>.
     */
    public final void setOutputs(final List<AbstractGoods> newOutputs) {
        this.outputs = newOutputs;
    }

    public AbstractGoods getOutput(GoodsType goodsType) {
        if (inputs == null && outputs != null) {
            for (AbstractGoods output : outputs) {
                if (goodsType == output.getType()) {
                    return output;
                }
            }
        }
        return null;
    }

    /**
     * Add a new output.
     *
     * @param type The <code>GoodsType</code> to add.
     * @param amount The amount of goods.
     */
    private void addOutput(GoodsType type, int amount) {
        if (outputs == null) outputs = new ArrayList<AbstractGoods>(1);
        outputs.add(new AbstractGoods(type, amount));
    }

    /**
     * Is this production from a colony center tile?
     *
     * @return True if this production is from a colony center tile.
     */
    public final boolean isColonyCenterTile() {
        return unattended;
    }

    /**
     * The production level of this type of production (used by
     * difficulty levels).
     *
     * @return The production level key.
     */
    public final String getProductionLevel() {
        return productionLevel;
    }

    /**
     * Does this production apply to a given difficulty level.
     *
     * @param level The difficulty level key to check.
     * @return True if this production applies.
     */
    public boolean appliesTo(String level) {
        return level == null
            || productionLevel == null
            || level.equals(productionLevel);
    }


    // Serialization

    private static final String UNATTENDED_TAG = "unattended";
    private static final String GOODS_TYPE_TAG = "goods-type";
    private static final String INPUT_TAG = "input";
    private static final String OUTPUT_TAG = "output";
    private static final String PRODUCTION_LEVEL_TAG = "productionLevel";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        // ProductionType does not need an id.
        // No need for: super.writeAttributes(out);

        if (unattended) {
            xw.writeAttribute(UNATTENDED_TAG, unattended);
        }

        if (productionLevel != null) {
            xw.writeAttribute(PRODUCTION_LEVEL_TAG, productionLevel);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        if (inputs != null) {
            for (AbstractGoods input : inputs) {
                xw.writeStartElement(INPUT_TAG);

                xw.writeAttribute(GOODS_TYPE_TAG, input.getType());

                xw.writeAttribute(VALUE_TAG, input.getAmount());

                xw.writeEndElement();
            }
        }

        if (outputs != null) {
            for (AbstractGoods output : outputs) {
                xw.writeStartElement(OUTPUT_TAG);

                xw.writeAttribute(GOODS_TYPE_TAG, output.getType());

                xw.writeAttribute(VALUE_TAG, output.getAmount());

                xw.writeEndElement();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        // ProductionType does not need an id.
        // No need for: super.readAttributes(in);
        // TODO: as soon as we allow the user to select a production type,
        // we will need an id

        unattended = xr.getAttribute(UNATTENDED_TAG, false);

        productionLevel = xr.getAttribute(PRODUCTION_LEVEL_TAG, (String)null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers
        if (inputs != null) inputs.clear();
        if (outputs != null) outputs.clear();

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final Specification spec = getSpecification();
        final String tag = xr.getLocalName();

        if (INPUT_TAG.equals(tag)) {
            GoodsType type = xr.getType(spec, GOODS_TYPE_TAG,
                                        GoodsType.class, (GoodsType)null);
            int amount = xr.getAttribute(VALUE_TAG, -1);
            addInput(type, amount);
            xr.closeTag(INPUT_TAG);

        } else if (OUTPUT_TAG.equals(tag)) {
            GoodsType type = xr.getType(spec, GOODS_TYPE_TAG,
                                        GoodsType.class, (GoodsType)null);
            int amount = xr.getAttribute(VALUE_TAG, -1);
            addOutput(type, amount);
            xr.closeTag(OUTPUT_TAG);

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(64);
        result.append("[").append(getId()).append(": ");
        if (productionLevel != null) {
            result.append(productionLevel);
        }
        if (unattended) {
            result.append(", colony center tile");
        }
        if (!(inputs == null || inputs.isEmpty())) {
            result.append(" [inputs: ");
            for (AbstractGoods input : inputs) {
                result.append(input).append(", ");
            }
            int length = result.length();
            result.replace(length - 2, length, "]");
        }
        if (!(outputs == null || outputs.isEmpty())) {
            result.append(" [outputs: ");
            for (AbstractGoods output : outputs) {
                result.append(output).append(", ");
            }
            int length = result.length();
            result.replace(length - 2, length, "]");
        }
        result.append("]");
        return result.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "production".
     */
    public static String getXMLElementTagName() {
        return "production";
    }
}
