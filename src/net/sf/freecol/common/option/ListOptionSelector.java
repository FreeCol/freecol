package net.sf.freecol.common.option;

import java.util.List;

/**
 * Instances of this class is responsible for offering a
 * list of objects that can be added to a {@link ListOption}.
 * It's also responsible for generating IDs and creating
 * string representations of the objects.
 *
 * @param <T> The type of the objects that can be added to
 *      a <code>ListOption</code>.
 */
public interface ListOptionSelector<T> {

    /**
     * Gets all available options.
     * @return A list of all the options that can be
     *      added to a specific <code>ListOption</code>.
     */
    public List<T> getOptions();

    /**
     * Gets an object using the specific id.
     *
     * @param id The string identifying the object.
     * @return The object.
     */
    public T getObject(String id);

    /**
     * Gets the id that should be used to identify an object
     * in a <code>ListOption</code>.
     *
     * @param t The id.
     * @return String
     */
    public String getId(T t);

    /**
     * Returns a human readable presentation of the given object.
     *
     * @param t The object to create a string representation for.
     * @return A string representation that can be used in
     *      a user interface.
     */
    public String toString(T t);
}
