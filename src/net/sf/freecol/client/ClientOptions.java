
package net.sf.freecol.client;

import net.sf.freecol.common.option.*;

import java.util.logging.Logger;
import java.util.*;

import org.w3c.dom.*;


/**
* Keeps track of the available client options.
*
* <br><br>
*
* New options should be added to
* {@link #addDefaultOptions} and each option should be given an unique
* identifier (defined as a constant in this class).
*/
public class ClientOptions extends OptionMap {
    private static Logger logger = Logger.getLogger(ClientOptions.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


    /** This is an example key: */
    //public static final String EXAMPLE = "example";



    /**
    * Creates a new <code>ClientOptions</code>.
    */
    public ClientOptions() {
        super(getXMLElementTagName(), "clientOptions.name", "clientOptions.shortDescription");
    }


    /**
    * Creates a <code>ClientOptions</code> from an XML representation.
    *
    * <br><br>
    *
    * @param element The XML <code>Element</code> from which this object
    *                should be constructed.
    */
    public ClientOptions(Element element) {
        super(element, getXMLElementTagName(), "clientOptions.name", "clientOptions.shortDescription");
    }




    /**
    * Adds the options to this <code>GameOptions</code>.
    */
    protected void addDefaultOptions() {
        /* Add options here: */

        /* Example: */
        /*
        OptionGroup exampleGroup = new OptionGroup("gameOptions.exampleGroup.name", "gameOptions.exampleGroup.shortDescription");
        exampleGroup.add(new IntegerOption(EXAMPLE, "gameOptions.example.name", "gameOptions.example.shortDescription", 0, 50000, 0));
        add(exampleGroup);
        */

    }


    /**
    * Gets the tag name of the root element representing this object.
    * @return "clientOptions".
    */
    public static String getXMLElementTagName() {
        return "clientOptions";
    }

}
