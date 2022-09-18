package net.sf.freecol.common.model.production;

import java.util.stream.Stream;

import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Specification;

public final class ProductionUtils {

    private ProductionUtils() {}
    
    
    /**
     * Gets the current production {@code Modifier}, which is
     * generated from the current production bonus.
     *
     * @param colonyProductionBonus The production bonus in the colony.
     * @param goodsType The {@code GoodsType} to produce.
     * @param buildingType A {@code BuildingType} for getting a rebel factor. Use {@code null}
     *      for a tile.
     * @return A stream of suitable {@code Modifier}s.
     */
    public static Stream<Modifier> getRebelProductionModifiers(int colonyProductionBonus,
            GoodsType goodsType, BuildingType buildingType) {
        final float rebelFactor = (buildingType != null) ? buildingType.getRebelFactor() : 1.0F;
        if (colonyProductionBonus == 0) return Stream.<Modifier>empty();
        int bonus = (int)Math.floor(colonyProductionBonus * rebelFactor);
        Modifier mod = new Modifier(goodsType.getId(), bonus,
                Modifier.ModifierType.ADDITIVE,
                Specification.SOL_MODIFIER_SOURCE);
        mod.setModifierIndex(Modifier.COLONY_PRODUCTION_INDEX);
        return Stream.of(mod);
    }
}
