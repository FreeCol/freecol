package net.sf.freecol.server.generator;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.option.OptionGroup;

/**
 * Creates maps and sets the starting locations for the players.
 */
public interface MapGenerator {

    /** 
     * Creates the map with the current set options
     */
    public abstract void createMap(Game game) throws FreeColException;

    /**
     * Gets the options used when generating the map.
     * @return The <code>MapGeneratorOptions</code>.
     */
    public abstract OptionGroup getMapGeneratorOptions();

}