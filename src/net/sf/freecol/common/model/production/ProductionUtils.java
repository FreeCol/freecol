package net.sf.freecol.common.model.production;

import java.util.stream.Stream;

import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.FeatureContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.UnitType;

public final class ProductionUtils {

    private ProductionUtils() {}
    
    
    /**
     * Gets the current production {@code Modifier}, which is
     * generated from the current production bonus.
     *
     * @param buildingType A {@code BuildingType} for getting a rebel factor. Use {@code null}
     *      for a tile.
     * @param colonyProductionBonus The production bonus in the colony.
     * @param goodsType The {@code GoodsType} to produce.
     * @param unitType The unit that is working in the building
     * @return A stream of suitable {@code Modifier}s.
     */
    public static Stream<Modifier> getRebelProductionModifiersForBuilding(BuildingType buildingType, int colonyProductionBonus, GoodsType goodsType, UnitType unitType) {
        if (colonyProductionBonus == 0) {
            return Stream.<Modifier>empty();
        }
        final float rebelFactor = (buildingType != null) ? buildingType.getRebelFactor() : 1.0F;
        final int bonus = (int) Math.floor(colonyProductionBonus * rebelFactor);
        return createRebelProductionModifierStream(Modifier.COLONY_PRODUCTION_INDEX, goodsType, bonus);
    }
    
    /**
     * Gets the current production {@code Modifier}, which is
     * generated from the current production bonus.
     *
     * @param colonyProductionBonus The production bonus in the colony.
     * @param goodsType The {@code GoodsType} to produce.
     * @param unitType The unit that is working on the tile.
     * @return A stream of suitable {@code Modifier}s.
     */
    public static Stream<Modifier> getRebelProductionModifiersForTile(Tile tile, int colonyProductionBonus, GoodsType goodsType, UnitType unitType) {
        if (colonyProductionBonus == 0) {
            return Stream.<Modifier>empty();
        }
        
        if (unitType == null && colonyProductionBonus < 0) { // unattended
            return Stream.<Modifier>empty();
        }
        
        if (unitType == null) { // unattended
            return createRebelProductionModifierStream(Modifier.COLONYTILE_PRODUCTION_INDEX, goodsType, colonyProductionBonus);
        }
        
        float rebelFactor = 1.0F;
        if (unitType != null
                && unitType.getExpertProduction() != null
                && unitType.getExpertProduction().equals(goodsType)) {
            rebelFactor *= 2.0F;
        }
        if (tile.getResource() != null) {
            final Stream<Modifier> multiplicativeResourceModifiers = tile.getResource().getProductionModifiers(goodsType, unitType)
                    .filter(m -> m.getType() == Modifier.ModifierType.MULTIPLICATIVE);
           
            rebelFactor = FeatureContainer.applyModifiers(rebelFactor, tile.getGame().getTurn(), multiplicativeResourceModifiers);
        }
        
        int bonus = (int) Math.max(colonyProductionBonus, Math.floor(colonyProductionBonus * rebelFactor));
        return createRebelProductionModifierStream(Modifier.COLONYTILE_PRODUCTION_INDEX, goodsType, bonus);
    }


    private static Stream<Modifier> createRebelProductionModifierStream(int modifierIndex, GoodsType goodsType, int bonus) {
        Modifier mod = new Modifier(goodsType.getId(), bonus,
                Modifier.ModifierType.ADDITIVE,
                Specification.SOL_MODIFIER_SOURCE);
        mod.setModifierIndex(modifierIndex);
        return Stream.of(mod);
    }
}
