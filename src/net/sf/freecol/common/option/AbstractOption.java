package net.sf.freecol.common.option;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.FreeColObject;

/**
 * The super class of all options. GUI components making use of this
 * class can refer to its name and shortDescription properties. The
 * complete keys of these properties consist of the id of the option
 * group (if any), followed by a "." unless the option group is null,
 * followed by the id of the option object, followed by a ".",
 * followed by "name" or "shortDescription".
 */
abstract public class AbstractOption extends FreeColObject implements Option {
    public static final String COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    public static final String NO_ID = "NO_ID";

    private static Logger logger = Logger.getLogger(AbstractOption.class.getName());

    private ArrayList<PropertyChangeListener> propertyChangeListeners = new ArrayList<PropertyChangeListener>();

    private final String id;
    private final String group;


    /**
     * Creates a new <code>AbstractOption</code>.
     * 
     * @param id The identifier for this option. This is used when the object
     *            should be found in an {@link OptionGroup}.
     */
    public AbstractOption(String id) {
        this(id, null);
    }

    /**
     * Creates a new <code>AbstractOption</code>.
     * 
     * @param id The identifier for this option. This is used when the object
     *            should be found in an {@link OptionGroup}.
     * @param optionGroup The OptionGroup this Option belongs to.
     */
    public AbstractOption(String id, OptionGroup optionGroup) {
        this.id = id;
        if (optionGroup == null) {
            this.group = "";
        } else {
            this.group = optionGroup.getId() + ".";
            optionGroup.add(this);
        }
    }

    
    /**
     * Adds a new <code>PropertyChangeListener</code> for monitoring state
     * changes. Events are generated when variables are changed.
     * 
     * @param pcl The <code>PropertyChangeListener</code> to be added.
     */
    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        propertyChangeListeners.add(pcl);
    }

    /**
     * Remove the given <code>PropertyChangeListener</code>.
     * 
     * @param pcl The <code>PropertyChangeListener</code> to be removed.
     */
    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        propertyChangeListeners.remove(pcl);
    }

    /**
     * Fires a <code>PropertyChangeEvent</code> to all listeners.
     * 
     * @param name The name of the changed variable.
     * @param oldValue The old value.
     * @param newValue The new value.
     */
    protected void firePropertyChange(String name, Object oldValue, Object newValue) {
        Iterator<PropertyChangeListener> it = propertyChangeListeners.iterator();
        while (it.hasNext()) {
            it.next().propertyChange(new PropertyChangeEvent(this, name, oldValue, newValue));
        }
    }

    /**
     * Gives a short description of this <code>Option</code>. Can for
     * instance be used as a tooltip text.
     * 
     * @return A short description of this <code>Option</code>.
     */
    public String getShortDescription() {
        return Messages.message(group + id + ".shortDescription");
    }

    /**
     * Returns a textual representation of this object.
     * 
     * @return The name of this <code>Option</code>.
     * @see #getName
     */
    public String toString() {
        return getName();
    }

    /**
     * Returns the id of this <code>Option</code>.
     * 
     * @return The unique identifier as provided in the constructor.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the string prefix that identifies the group of this
     * <code>Option</code>.
     * 
     * @return The string prefix provided by the OptionGroup.
     */
    public String getGroup() {
        return group;
    }

    /**
     * Returns the name of this <code>Option</code>.
     * 
     * @return The name as provided in the constructor.
     */
    public String getName() {
        return Messages.message(group + id + ".name");
    }

    /**
     * Gets the tag name of the root element representing this object.
     * 
     * @return "option".
     */
    public static String getXMLElementTagName() {
        return "option";
    }

}
