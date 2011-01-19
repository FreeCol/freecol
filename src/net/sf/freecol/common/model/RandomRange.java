package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

public class RandomRange {

    /**
     * Probability that the result is not zero.
     */
    private int probability = 0;

    /**
     * Minimum value.
     */
    private int minimum = 0;

    /**
     * Maximum value.
     */
    private int maximum = 0;

    /**
     * Factor to multiply the value with.
     */
    private int factor = 1;

    /**
     * A list of Scopes limiting the applicability of this Feature.
     */
    private List<Scope> scopes = new ArrayList<Scope>();


    public RandomRange() {
        // empty constructor
    }

    /**
     * Creates a new <code>RandomRange</code> instance.
     *
     * @param probability an <code>int</code> value
     * @param minimum an <code>int</code> value
     * @param maximum an <code>int</code> value
     * @param factor an <code>int</code> value
     */
    public RandomRange(int probability, int minimum, int maximum, int factor) {
        this.probability = probability;
        this.minimum = minimum;
        this.maximum = maximum;
        this.factor = factor;
    }


    /**
     * Creates a new <code>RandomRange</code> instance.
     *
     * @param in a <code>XMLStreamReader</code> value
     */
    public RandomRange(XMLStreamReader in) throws XMLStreamException {
        readFromXML(in);
    }

    /**
     * Get the <code>Probability</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getProbability() {
        return probability;
    }

    /**
     * Set the <code>Probability</code> value.
     *
     * @param newProbability The new Probability value.
     */
    public final void setProbability(final int newProbability) {
        this.probability = newProbability;
    }

    /**
     * Get the <code>Minimum</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getMinimum() {
        return minimum;
    }

    /**
     * Set the <code>Minimum</code> value.
     *
     * @param newMinimum The new Minimum value.
     */
    public final void setMinimum(final int newMinimum) {
        this.minimum = newMinimum;
    }

    /**
     * Get the <code>Maximum</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getMaximum() {
        return maximum;
    }

    /**
     * Set the <code>Maximum</code> value.
     *
     * @param newMaximum The new Maximum value.
     */
    public final void setMaximum(final int newMaximum) {
        this.maximum = newMaximum;
    }

    /**
     * Get the <code>Factor</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getFactor() {
        return factor;
    }

    /**
     * Set the <code>Factor</code> value.
     *
     * @param newFactor The new Factor value.
     */
    public final void setFactor(final int newFactor) {
        this.factor = newFactor;
    }

    /**
     * Describe <code>getRange</code> method here.
     *
     * @return an <code>int</code> value
     */
    public int getRange() {
        return maximum - minimum;
    }

    /**
     * Describe <code>getRandomLimit</code> method here.
     *
     * @return an <code>int</code> value
     */
    public int getRandomLimit() {
        return getRange() * 100 / factor;
    }

    /**
     * Describe <code>getAmount</code> method here.
     *
     * @param random an <code>int</code> value
     * @return an <code>int</code> value
     */
    public int getAmount(int random) {
        int value = minimum + random;
        if (value <= maximum) {
            return value * factor;
        } else {
            return 0;
        }
    }

    /**
     * Return the scopes of this RandomRange.
     *
     * @return the scopes of this RandomRange
     */
    public List<Scope> getScopes() {
        return scopes;
    }

    public RandomRange clone() {
        return new RandomRange(probability, maximum, minimum, factor);
    }


    public void readFromXML(XMLStreamReader in) throws XMLStreamException {
        probability = Integer.parseInt(in.getAttributeValue(null, "probability"));
        minimum = Integer.parseInt(in.getAttributeValue(null, "minimum"));
        maximum = Integer.parseInt(in.getAttributeValue(null, "maximum"));
        factor = Integer.parseInt(in.getAttributeValue(null, "factor"));
        scopes.clear();
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String nodeName = in.getLocalName();
            if ("scope".equals(nodeName)) {
                scopes.add(new Scope(in));
            }
        }
    }

    public void toXML(XMLStreamWriter out, String tag) throws XMLStreamException {
        out.writeStartElement(tag);
        out.writeAttribute("probability", Integer.toString(probability));
        out.writeAttribute("minimum", Integer.toString(minimum));
        out.writeAttribute("maximum", Integer.toString(maximum));
        out.writeAttribute("factor", Integer.toString(factor));
        for (Scope scope : scopes) {
            scope.toXML(out);
        }
        out.writeEndElement();
    }

}
