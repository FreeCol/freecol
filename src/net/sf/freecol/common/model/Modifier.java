
package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.w3c.dom.Element;


/**
 * The <code>Modifier</code> class encapsulates a bonus or penalty
 * that can be applied to any action within the game, most obviously
 * combat.
 */
public final class Modifier extends FreeColObject implements Cloneable {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE   = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


    public static final int ADDITIVE = 0;
    public static final int MULTIPLICATIVE = 1;
    public static final int PERCENTAGE = 2;
    public static final int COMBINED = 3;
    public static final int BOOLEAN = 4;
    
    /**
     * The ID of the Modifier, used to look up name, etc.
     */
    private String id;

    /**
     * The ID of the source of this Modifier, e.g. "model.foundingFather.thomasJefferson"
     */
    private String source;

    /**
     * The type of this Modifier
     */
    private int type;

    /**
     * The float values of this modifier
     */
    private float values[] = {0, 1, 0};

    /**
     * The boolean value of this modifier.
     */
    private boolean booleanValue;

    /**
     * A list of modifiers that contributed to this one.
     */
    private List<Modifier> modifiers;

    /**
     * Describe scopes here.
     */
    private List<Scope> scopes = new ArrayList<Scope>();


    // -- Constructors --


    /**
     * Creates a new <code>Modifier</code> instance.
     *
     * @param id a <code>String</code> value
     * @param value an <code>float</code> value
     * @param type the type of the modifier
     */
    public Modifier(String id, float value, int type) {
        this(id, null, value, type);
    }

    /**
     * Creates a new <code>Modifier</code> instance.
     *
     * @param id a <code>String</code> value
     * @param source a <code>String</code> value
     * @param value an <code>float</code> value
     * @param type the type of the modifier
     */
    public Modifier(String id, String source, float value, int type) {
        setId(id);
        setSource(source);
        setType(type);
        this.values[type] = value;
    }

    /**
     * Creates a new <code>Modifier</code> instance.
     *
     * @param id a <code>String</code> value
     * @param value an <code>float</code> value
     * @param type the type of the modifier
     */
    public Modifier(String id, float value, String type) {
        this(id, null, value, type);
    }


    /**
     * Creates a new <code>Modifier</code> instance.
     *
     * @param id a <code>String</code> value
     * @param source a <code>String</code> value
     * @param value an <code>float</code> value
     * @param type the type of the modifier
     */
    public Modifier(String id, String source, float value, String type) {
        setId(id);
        setSource(source);
        setType(getTypeFromString(type));
        this.values[getType()] = value;
    }


    /**
     * Creates a new <code>Modifier</code> instance.
     *
     * @param id a <code>String</code> value
     * @param value a <code>boolean</code> value
     * @param type an <code>int</code> value
     */
    public Modifier(String id, boolean value, int type) {
        this(id, null, value, type);
    }

    /**
     * Creates a new <code>Modifier</code> instance.
     *
     * @param id a <code>String</code> value
     * @param source a <code>String</code> value
     * @param value a <code>boolean</code> value
     * @param type an <code>int</code> value
     */
    public Modifier(String id, String source, boolean value, int type) {
        this.id = id;
        this.source = source;
        this.booleanValue = value;
        this.type = type;
    }

    
    /**
     * Creates a new <code>Modifier</code> instance.
     *
     * @param id a <code>String</code> value
     * @param value a <code>boolean</code> value
     * @param type an <code>String</code> value
     */
    public Modifier(String id, boolean value, String type) {
        this(id, null, value, type);
    }

    /**
     * Creates a new <code>Modifier</code> instance.
     *
     * @param id a <code>String</code> value
     * @param source a <code>String</code> value
     * @param value a <code>boolean</code> value
     * @param type an <code>String</code> value
     */
    public Modifier(String id, String source, boolean value, String type) {
        this.id = id;
        this.source = source;
        this.booleanValue = value;
        this.type = getTypeFromString(type);
    }

    
    /**
     * Creates a new <code>Modifier</code> instance.
     *
     * @param element an <code>Element</code> value
     */
    public Modifier(Element element) {
        readFromXMLElement(element);
    }
    
    /**
     * Creates a new <code>Modifier</code> instance.
     *
     * @param in a <code>XMLStreamReader</code> value
     * @exception XMLStreamException if an error occurs
     */
    public Modifier(XMLStreamReader in) throws XMLStreamException {
        readFromXML(in);
    }
    
    /**
     * Creates a new <code>Modifier</code> instance.
     *
     * @param modifier a <code>Modifier</code> value
     */
    public Modifier(Modifier modifier) {
        setId(modifier.getId());
        setType(modifier.getType());
        setSource(modifier.getSource());
        scopes = new ArrayList(modifier.getScopes());
        if (modifier.getModifiers() != null) {
            modifiers = new ArrayList(modifier.getModifiers());
        }
        booleanValue = modifier.getBooleanValue();
        for (int i = 0; i < values.length; i++) {
            values[i] = modifier.values[i];
        }
    }
    
    // -- Methods --

    /**
     * Get the <code>Modifiers</code> value.
     *
     * @return a <code>List<Modifier></code> value
     */
    public List<Modifier> getModifiers() {
        return modifiers;
    }

    /**
     * Set the <code>Modifiers</code> value.
     *
     * @param newModifiers The new Modifiers value.
     */
    public void setModifiers(final List<Modifier> newModifiers) {
        this.modifiers = newModifiers;
    }

    /**
     * Get the <code>BooleanValue</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public boolean getBooleanValue() {
        return booleanValue;
    }

    /**
     * Set the <code>BooleanValue</code> value.
     *
     * @param newBooleanValue The new BooleanValue value.
     */
    public void setBooleanValue(final boolean newBooleanValue) {
        this.booleanValue = newBooleanValue;
    }

    /**
     * Get the <code>Scopes</code> value.
     *
     * @return a <code>List<Scope></code> value
     */
    public final List<Scope> getScopes() {
        return scopes;
    }

    /**
     * Set the <code>Scopes</code> value.
     *
     * @param newScopes The new Scopes value.
     */
    public final void setScopes(final List<Scope> newScopes) {
        this.scopes = newScopes;
    }

    /**
     * Get the <code>Source</code> value.
     *
     * @return a <code>String</code> value
     */
    public final String getSource() {
        return source;
    }

    /**
     * Set the <code>Source</code> value.
     *
     * @param newSource The new Source value.
     */
    public final void setSource(final String newSource) {
        this.source = newSource;
    }


    /**
     * Describe <code>getTypeFromString</code> method here.
     *
     * @param type a <code>String</code> value
     * @return an <code>int</code> value
     */
    protected int getTypeFromString(String type) {
        if ("additive".equals(type)) {
            return ADDITIVE;
        } else if ("multiplicative".equals(type)) {
            return MULTIPLICATIVE;
        } else if ("percentage".equals(type)) {
            return PERCENTAGE;
        } else if ("combined".equals(type)) {
            return COMBINED;
        } else if ("boolean".equals(type) || type == null) {
            return BOOLEAN;
        } else {
            throw new IllegalArgumentException("Unknown type");
        }
    }
    
    /**
     * Describe <code>getTypeAsString</code> method here.
     *
     * @return a <code>String</code> value
     */
    protected String getTypeAsString() {
        switch(getType()) {
        case ADDITIVE:
            return "additive";
        case MULTIPLICATIVE:
            return "multiplicative";
        case PERCENTAGE:
            return "percentage";
        case COMBINED:
            return "combined";
        case BOOLEAN:
            return "boolean";
        default:
            // It can't happen
            return null;
        }
    }

    /**
     * Get the <code>Id</code> value.
     *
     * @return a <code>String</code> value
     */
    public String getId() {
        return id;
    }

    /**
     * Set the <code>Id</code> value.
     *
     * @param newId The new Id value.
     */
    public void setId(final String newId) {
        this.id = newId;
    }

    /**
     * Get the <code>Type</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getType() {
        return type;
    }

    /**
     * Set the <code>Type</code> value.
     *
     * @param newType The new Type value.
     */
    public void setType(final int newType) {
        this.type = newType;
    }

    /**
     * Returns true if the <code>appliesTo</code> method of at least
     * one <code>Scope</code> object returns true.
     *
     * @param objectType a <code>FreeColGameObjectType</code> value
     * @return a <code>boolean</code> value
     */
    public boolean appliesTo(FreeColGameObjectType objectType) {
        for (Scope scope : scopes) {
            if (scope.appliesTo(objectType)) {
                return true;
            }
        }
        return false;
    }



    public static String getXMLElementTagName() {
        return "modifier";
    }

    /**
     * Get the <code>Value</code> value.
     *
     * @return a <code>float</code> value
     */
    public float getValue() {
        return values[getType()];
    }

    /**
     * Set the <code>Value</code> value.
     *
     * @param newValue The new Value value.
     */
    public void setValue(final float newValue) {
        this.values[getType()] = newValue;
    }

    /**
     * Returns the inverse modifier of this one
     *
     * @return a inverse <code>Modifier</code>
     */
    public Modifier getInverse() {
        Modifier newModifier = new Modifier(this);

        switch(type) {
        case COMBINED:
            for (int i = 0; i < values.length; i++) {
                newModifier.values[i] = getInverse(i);
            }
            break;
        case BOOLEAN:
            newModifier.booleanValue = !booleanValue;
            break;
        default:
            newModifier.values[getType()] = getInverse(getType());
            break;
        }
        return newModifier;
    }
    
    private float getInverse(int type) {
        switch(type) {
        case ADDITIVE:
        case PERCENTAGE:
            return -values[type];
        case MULTIPLICATIVE:
            return 1 / values[type];
        default:
            // It can't happen
            return values[type];
        }
    }
    

    /**
     * Combines this modifier with another.
     *
     * @param otherModifier a <code>Modifier</code> value
     */
    public void combine(Modifier otherModifier) {
        if (getType() != otherModifier.getType()) {
            setType(COMBINED);
        }

        if (modifiers == null) {
            modifiers = new ArrayList<Modifier>();
            modifiers.add(new Modifier(this));
        }
        if (otherModifier.getModifiers() == null) {
            modifiers.add(otherModifier);
        } else {
            modifiers.addAll(otherModifier.getModifiers());
        }

        if (otherModifier.getType() != COMBINED) {
            combine(otherModifier, otherModifier.getType());
        } else {
            for (int i = 0; i < values.length; i++) {
                combine(otherModifier, i);
            }
        }
    }
    
    private void combine(Modifier otherModifier, int type) {
        switch(type) {
        case ADDITIVE:
        case PERCENTAGE:
            values[type] += otherModifier.values[type];
            return;
        case MULTIPLICATIVE:
            values[type] *= otherModifier.values[type];
            return;
        }
    }
 
    /**
     * Applies this modifier to a number.
     *
     * @param number a <code>float</code> value
     * @return a <code>float</code> value
     */
    public float applyTo(float number) {
        if (getType() == COMBINED) {
            for (int i = 0; i < values.length; i++) {
                number = applyTo(number, i);
            }
            return number;
        } else {
            return applyTo(number, getType());
        }
    }
    
    private float applyTo(float number, int type) {
        switch(type) {
        case ADDITIVE:
            return number + values[type];
        case MULTIPLICATIVE:
            return number * values[type];
        case PERCENTAGE:
            return number + (number * values[type]) / 100;
        default:
            return number;
        }
    }

    // -- Serialization --


    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setId(in.getAttributeValue(null, "id"));
        setSource(in.getAttributeValue(null, "source"));
        setType(getTypeFromString(in.getAttributeValue(null, "type")));

        if (type == COMBINED) {
            values = readFromArrayElement("values", in, new float[0]);
        } else if (type == BOOLEAN) {
            setBooleanValue(getAttribute(in, "value", false));
        } else {
            values[getType()] = Float.parseFloat(in.getAttributeValue(null, "value"));
        }

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String childName = in.getLocalName();
            if ("scope".equals(childName)) {
                Scope scope = new Scope(in);
                getScopes().add(scope);
            } else if ("modifier".equals(childName)) {
                Modifier modifier = new Modifier(in);
                if (modifiers == null) {
                    modifiers = new ArrayList<Modifier>();
                }
                modifiers.add(modifier);
            } else {
                logger.finest("Parsing of " + childName + " is not implemented yet");
                while (in.nextTag() != XMLStreamConstants.END_ELEMENT ||
                       !in.getLocalName().equals(childName)) {
                    in.nextTag();
                }
            }
        }        

    }
    
    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute("id", getId());
        if (source != null) {
            out.writeAttribute("source", source);
        }
        out.writeAttribute("type", getTypeAsString());
        if (type == COMBINED) {
            toArrayElement("values", values, out);
        } else if (type == BOOLEAN) {
            out.writeAttribute("value", booleanValue ? "true" : "false");
        } else {
            out.writeAttribute("value", Float.toString(values[getType()]));
        }
        for (Scope scope : getScopes()) {
            scope.toXMLImpl(out);
        }
        if (modifiers != null) {
            for (Modifier modifier : getModifiers()) {
                modifier.toXMLImpl(out);
            }
        }

        out.writeEndElement();
    }
    
}
