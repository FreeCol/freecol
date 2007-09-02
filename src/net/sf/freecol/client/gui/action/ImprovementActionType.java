package net.sf.freecol.client.gui.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import net.sf.freecol.common.model.FreeColGameObjectType;
import net.sf.freecol.common.model.TileImprovementType;

/**
 * A storage class for ImprovementActionType used to create ImprovementActions.
 * Filled by Specification.java, utilized by ActionManager.java
 */
public final class ImprovementActionType extends FreeColGameObjectType
{
    public static final  String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";

    public static final  String  LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final  String  REVISION = "$Revision: 2889 $";

    private char accelerator;
    
    private final List<String> names;
    private final List<TileImprovementType> impTypes;
    private final List<Integer> imageIDs;
    
    // ------------------------------------------------------------ constructors
    
    public ImprovementActionType() {
        names = new ArrayList<String>();
        impTypes = new ArrayList<TileImprovementType>();
        imageIDs = new ArrayList<Integer>();
    }

    // ------------------------------------------------------------ retrieval methods

    public char getAccelerator() {
        return accelerator;
    }

    public List<String> getNames() {
        return names;
    }

    public List<TileImprovementType> getImpTypes() {
        return impTypes;
    }
    
    public List<Integer> getImageIDs() {
        return imageIDs;
    }

    // ------------------------------------------------------------ API methods

    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        readFromXML(in, null);
    }

    public void readFromXML(XMLStreamReader in, final Map<String, TileImprovementType> tileImprovementTypeByRef)
           throws XMLStreamException {
        setID(in.getAttributeValue(null, "id"));
        accelerator = in.getAttributeValue(null, "accelerator").charAt(0);

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            names.add(in.getAttributeValue(null, "name"));
            String t = in.getAttributeValue(null, "tileimprovement-type");
            impTypes.add(tileImprovementTypeByRef.get(t));
            imageIDs.add(Integer.parseInt(in.getAttributeValue(null, "image-id")));
            in.nextTag(); // close this element
        }
    }   
}
