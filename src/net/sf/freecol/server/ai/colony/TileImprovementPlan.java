package net.sf.freecol.server.ai.colony;

import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovementType;

public class TileImprovementPlan {
    
    /** The type of improvement, from TileImprovementTypes. */
    private TileImprovementType type;

    /** The {@code Tile} to be improved. */
    private Tile target;

    
    public TileImprovementPlan(TileImprovementType type, Tile target) {
        this.type = type;
        this.target = target;
    }
    
    
    public TileImprovementType getType() {
        return type;
    }
    
    public Tile getTarget() {
        return target;
    }
    
    @Override
    public String toString() {
        return target.getX() + "," + target.getY() + ": " + Utils.compressIdForLogging(type.getId());
    }
}
