/**
 *  Copyright (C) 2002-2007  The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.model;

import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.FreeCol;

/**
 * Represents a difficulty level.
 */
public class DifficultyLevel extends FreeColGameObjectType {

    /**
     * Describe crossesIncrement here.
     */
    private int crossesIncrement;

    /**
     * Describe landPriceFactor here.
     */
    private int landPriceFactor;

    /**
     * Describe foundingFatherFactor here.
     */
    private int foundingFatherFactor;

    /**
     * Describe arrearsFactor here.
     */
    private int arrearsFactor;

    /**
     * Describe nativeConvertProbability here.
     */
    private int nativeConvertProbability;

    /**
     * Describe burnProbability here.
     */
    private int burnProbability;

    /**
     * Describe recruitPriceIncrease here.
     */
    private int recruitPriceIncrease;

    /**
     * Describe lowerCapIncrease here.
     */
    private int lowerCapIncrease;

    /**
     * Describe purchasePriceIncrease here.
     */
    private int purchasePriceIncrease;

    /**
     * Describe trainingPriceIncrease here.
     */
    private int trainingPriceIncrease;

    /**
     * Describe purchasePricePerUnitType here.
     */
    private boolean purchasePricePerUnitType;

    /**
     * Describe trainingPricePerUnitType here.
     */
    private boolean trainingPricePerUnitType;

    /**
     * Describe badGovernmentLimit here.
     */
    private int badGovernmentLimit;

    /**
     * Describe veryBadGovernmentLimit here.
     */
    private int veryBadGovernmentLimit;

    private Map<UnitType, Integer> purchasePrices;

    private Map<UnitType, Integer> trainingPrices;


    public DifficultyLevel(int index) {
        setIndex(index);
    }


    /**
     * Get the <code>CrossesIncrement</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getCrossesIncrement() {
        return crossesIncrement;
    }

    /**
     * Set the <code>CrossesIncrement</code> value.
     *
     * @param newCrossesIncrement The new CrossesIncrement value.
     */
    public final void setCrossesIncrement(final int newCrossesIncrement) {
        this.crossesIncrement = newCrossesIncrement;
    }

    /**
     * Get the <code>BadGovernmentLimit</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getBadGovernmentLimit() {
        return badGovernmentLimit;
    }

    /**
     * Set the <code>BadGovernmentLimit</code> value.
     *
     * @param newBadGovernmentLimit The new BadGovernmentLimit value.
     */
    public final void setBadGovernmentLimit(final int newBadGovernmentLimit) {
        this.badGovernmentLimit = newBadGovernmentLimit;
    }

    /**
     * Get the <code>VeryBadGovernmentLimit</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getVeryBadGovernmentLimit() {
        return veryBadGovernmentLimit;
    }

    /**
     * Set the <code>VeryBadGovernmentLimit</code> value.
     *
     * @param newVeryBadGovernmentLimit The new VeryBadGovernmentLimit value.
     */
    public final void setVeryBadGovernmentLimit(final int newVeryBadGovernmentLimit) {
        this.veryBadGovernmentLimit = newVeryBadGovernmentLimit;
    }

    /**
     * Get the <code>LandPriceFactor</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getLandPriceFactor() {
        return landPriceFactor;
    }

    /**
     * Set the <code>LandPriceFactor</code> value.
     *
     * @param newLandPriceFactor The new LandPriceFactor value.
     */
    public final void setLandPriceFactor(final int newLandPriceFactor) {
        this.landPriceFactor = newLandPriceFactor;
    }

    /**
     * Get the <code>FoundingFatherFactor</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getFoundingFatherFactor() {
        return foundingFatherFactor;
    }

    /**
     * Set the <code>FoundingFatherFactor</code> value.
     *
     * @param newFoundingFatherFactor The new FoundingFatherFactor value.
     */
    public final void setFoundingFatherFactor(final int newFoundingFatherFactor) {
        this.foundingFatherFactor = newFoundingFatherFactor;
    }

    /**
     * Get the <code>ArrearsFactor</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getArrearsFactor() {
        return arrearsFactor;
    }

    /**
     * Set the <code>ArrearsFactor</code> value.
     *
     * @param newArrearsFactor The new ArrearsFactor value.
     */
    public final void setArrearsFactor(final int newArrearsFactor) {
        this.arrearsFactor = newArrearsFactor;
    }

    /**
     * Get the <code>NativeConvertProbability</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getNativeConvertProbability() {
        return nativeConvertProbability;
    }

    /**
     * Set the <code>NativeConvertProbability</code> value.
     *
     * @param newNativeConvertProbability The new NativeConvertProbability value.
     */
    public final void setNativeConvertProbability(final int newNativeConvertProbability) {
        this.nativeConvertProbability = newNativeConvertProbability;
    }

    /**
     * Get the <code>BurnProbability</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getBurnProbability() {
        return burnProbability;
    }

    /**
     * Set the <code>BurnProbability</code> value.
     *
     * @param newBurnProbability The new BurnProbability value.
     */
    public final void setBurnProbability(final int newBurnProbability) {
        this.burnProbability = newBurnProbability;
    }

    /**
     * Get the <code>RecruitPriceIncrease</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getRecruitPriceIncrease() {
        return recruitPriceIncrease;
    }

    /**
     * Set the <code>RecruitPriceIncrease</code> value.
     *
     * @param newRecruitPriceIncrease The new RecruitPriceIncrease value.
     */
    public final void setRecruitPriceIncrease(final int newRecruitPriceIncrease) {
        this.recruitPriceIncrease = newRecruitPriceIncrease;
    }

    /**
     * Get the <code>LowerCapIncrease</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getLowerCapIncrease() {
        return lowerCapIncrease;
    }

    /**
     * Set the <code>LowerCapIncrease</code> value.
     *
     * @param newLowerCapIncrease The new LowerCapIncrease value.
     */
    public final void setLowerCapIncrease(final int newLowerCapIncrease) {
        this.lowerCapIncrease = newLowerCapIncrease;
    }

    /**
     * Get the <code>PurchasePriceIncrease</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getPurchasePriceIncrease() {
        return purchasePriceIncrease;
    }

    /**
     * Get the <code>PurchasePriceIncrease</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getPurchasePriceIncrease(UnitType unitType) {
        if (isPurchasePricePerUnitType()) {
            return purchasePriceIncrease;
        } else if (purchasePrices.containsKey(unitType)) {
            return purchasePrices.get(unitType);
        } else {
            return purchasePriceIncrease;
        }
    }

    /**
     * Set the <code>PurchasePriceIncrease</code> value.
     *
     * @param newPurchasePriceIncrease The new PurchasePriceIncrease value.
     */
    public final void setPurchasePriceIncrease(final int newPurchasePriceIncrease) {
        this.purchasePriceIncrease = newPurchasePriceIncrease;
    }

    /**
     * Get the <code>TrainingPriceIncrease</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getTrainingPriceIncrease() {
        return trainingPriceIncrease;
    }

    /**
     * Get the <code>TrainingPriceIncrease</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getTrainingPriceIncrease(UnitType unitType) {
        if (isTrainingPricePerUnitType()) {
            return trainingPriceIncrease;
        } else if (trainingPrices.containsKey(unitType)) {
            return trainingPrices.get(unitType);
        } else {
            return trainingPriceIncrease;
        }
    }

    /**
     * Set the <code>TrainingPriceIncrease</code> value.
     *
     * @param newTrainingPriceIncrease The new TrainingPriceIncrease value.
     */
    public final void setTrainingPriceIncrease(final int newTrainingPriceIncrease) {
        this.trainingPriceIncrease = newTrainingPriceIncrease;
    }

    /**
     * Get the <code>PurchasePricePerUnitType</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean isPurchasePricePerUnitType() {
        return purchasePricePerUnitType;
    }

    /**
     * Set the <code>PurchasePricePerUnitType</code> value.
     *
     * @param newPurchasePricePerUnitType The new PurchasePricePerUnitType value.
     */
    public final void setPurchasePricePerUnitType(final boolean newPurchasePricePerUnitType) {
        this.purchasePricePerUnitType = newPurchasePricePerUnitType;
    }

    /**
     * Get the <code>TrainingPricePerUnitType</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean isTrainingPricePerUnitType() {
        return trainingPricePerUnitType;
    }

    /**
     * Set the <code>TrainingPricePerUnitType</code> value.
     *
     * @param newTrainingPricePerUnitType The new TrainingPricePerUnitType value.
     */
    public final void setTrainingPricePerUnitType(final boolean newTrainingPricePerUnitType) {
        this.trainingPricePerUnitType = newTrainingPricePerUnitType;
    }


    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setId(in.getAttributeValue(null, "id"));
        crossesIncrement = getAttribute(in, "crosses-increment", 10);
        badGovernmentLimit = getAttribute(in, "bad-government-limit", 8);
        veryBadGovernmentLimit = getAttribute(in, "very-bad-government-limit", 12);
        landPriceFactor = getAttribute(in, "land-price-factor", 60);
        foundingFatherFactor = getAttribute(in, "founding-father-factor", 7);
        arrearsFactor = getAttribute(in, "arrears-factor", 500);
        nativeConvertProbability = getAttribute(in, "native-convert-probability", 30);
        burnProbability = getAttribute(in, "burn-probability", 6);
        recruitPriceIncrease = getAttribute(in, "recruit-price-increase", 40);
        lowerCapIncrease = getAttribute(in, "lower-cap-increase", 0);

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String childName = in.getLocalName();
            if ("purchase-prices".equals(childName)) {
                purchasePriceIncrease = getAttribute(in, "price-increase", 0);
                purchasePricePerUnitType = getAttribute(in, "per-unit-type", false);
                while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
                    childName = in.getLocalName();
                    if ("purchase-price".equals(childName)) {
                        String unitTypeId = getAttribute(in, "unit-type", null);
                        int priceIncrease = getAttribute(in, "price-increase", 0);
                        purchasePrices.put(FreeCol.getSpecification().getUnitType(unitTypeId),
                                           new Integer(priceIncrease));
                        in.nextTag();
                    } else {
                        logger.finest("Parsing of " + childName + " is not implemented yet");
                        in.nextTag();
                    }
                }
            } else if ("training-prices".equals(childName)) {
                trainingPriceIncrease = getAttribute(in, "price-increase", 0);
                trainingPricePerUnitType = getAttribute(in, "per-unit-type", false);
                while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
                    childName = in.getLocalName();
                    if ("training-price".equals(childName)) {
                        String unitTypeId = getAttribute(in, "unit-type", null);
                        int priceIncrease = getAttribute(in, "price-increase", 0);
                        trainingPrices.put(FreeCol.getSpecification().getUnitType(unitTypeId),
                                           new Integer(priceIncrease));
                        in.nextTag();
                    } else {
                        logger.finest("Parsing of " + childName + " is not implemented yet");
                        in.nextTag();
                    }
                }
            } else {
                logger.finest("Parsing of " + childName + " is not implemented yet");
                in.nextTag();
            }
        }
    }

}


