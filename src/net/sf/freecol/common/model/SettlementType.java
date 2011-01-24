package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

public class SettlementType extends FreeColGameObjectType {

    /**
     * Whether this SettlementType is a capital.
     */
    private boolean capital = false;

    /**
     * How many tiles this SettlementType can see.
     */
    private int visibleRadius = 2;

    /**
     * How many tiles this SettlementType can claim.
     */
    private int claimableRadius = 1;

    /**
     * The extra radius beyond the claimableRadius where wandering
     * units may claim as yet unclaimed tiles.
     */
    private int extraClaimableRadius = 2;

    /**
     * How far units from this SettlementType may roam.
     */
    private int wanderingRadius = 4;

    /**
     * The plunder this SettlementType generates when destroyed.
     */
    private List<RandomRange> plunder = new ArrayList<RandomRange>();

    /**
     * The gifts this SettlementType generates when visited by a
     * scout.
     */
    private List<RandomRange> gifts = new ArrayList<RandomRange>();

    /**
     * The minimum number of units for this SettlementType.
     */
    private int minimumSize = 3;

    /**
     * The maximum number of units for this SettlementType.
     */
    private int maximumSize = 10;

    /**
     * The minimum number of tiles to grow this SettlementType.
     */
    private int minimumGrowth = 1;

    /**
     * The maximum number of tiles to grown this SettlementType.
     */
    private int maximumGrowth = 10;

    /**
     * The general trade bonus, roughly proportional to the settlement
     * size and general sophistication.
     */
    private int tradeBonus = 1;


    /**
     * Creates a new <code>SettlementType</code> instance.
     *
     * @param id a <code>String</code> value
     * @param specification a <code>Specification</code> value
     */
    public SettlementType(String id, Specification specification) {
        super(id, specification);
    }

    /**
     * Get the <code>Capital</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean isCapital() {
        return capital;
    }

    /**
     * Set the <code>Capital</code> value.
     *
     * @param newCapital The new Capital value.
     */
    public final void setCapital(final boolean newCapital) {
        this.capital = newCapital;
    }

    /**
     * Get the <code>MinimumSize</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getMinimumSize() {
        return minimumSize;
    }

    /**
     * Set the <code>MinimumSize</code> value.
     *
     * @param newMinimumSize The new MinimumSize value.
     */
    public final void setMinimumSize(final int newMinimumSize) {
        this.minimumSize = newMinimumSize;
    }

    /**
     * Get the <code>MaximumSize</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getMaximumSize() {
        return maximumSize;
    }

    /**
     * Set the <code>MaximumSize</code> value.
     *
     * @param newMaximumSize The new MaximumSize value.
     */
    public final void setMaximumSize(final int newMaximumSize) {
        this.maximumSize = newMaximumSize;
    }

    /**
     * Get the <code>VisibleRadius</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getVisibleRadius() {
        return visibleRadius;
    }

    /**
     * Set the <code>VisibleRadius</code> value.
     *
     * @param newVisibleRadius The new VisibleRadius value.
     */
    public final void setVisibleRadius(final int newVisibleRadius) {
        this.visibleRadius = newVisibleRadius;
    }

    /**
     * Get the <code>ClaimableRadius</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getClaimableRadius() {
        return claimableRadius;
    }

    /**
     * Get the <code>extraClaimableRadius</code> value.
     *
     * @return The extra claimable radius.
     */
    public final int getExtraClaimableRadius() {
        return extraClaimableRadius;
    }

    /**
     * Set the <code>ClaimableRadius</code> value.
     *
     * @param newClaimableRadius The new ClaimableRadius value.
     */
    public final void setClaimableRadius(final int newClaimableRadius) {
        this.claimableRadius = newClaimableRadius;
    }

    /**
     * Get the <code>WanderingRadius</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getWanderingRadius() {
        return wanderingRadius;
    }

    /**
     * Set the <code>WanderingRadius</code> value.
     *
     * @param newWanderingRadius The new WanderingRadius value.
     */
    public final void setWanderingRadius(final int newWanderingRadius) {
        this.wanderingRadius = newWanderingRadius;
    }

    /**
     * Get the minimum growth value.
     *
     * @return The minimum number of tiles to try to grow this
     *     settlement type by.
     */
    public final int getMinimumGrowth() {
        return minimumGrowth;
    }

    /**
     * Get the maximum growth value.
     *
     * @return The maximum number of tiles to try to grow this
     *     settlement type by.
     */
    public final int getMaximumGrowth() {
        return maximumGrowth;
    }

    /**
     * Gets the trade bonus.
     *
     * @return The general bonus to trade.
     */
    public final int getTradeBonus() {
        return tradeBonus;
    }

    /**
     * Get the <code>Plunder</code> value.
     *
     * @param unit an <code>Unit</code> value
     * @return a <code>RandomRange</code> value
     */
    public final RandomRange getPlunder(Unit unit) {
        for (RandomRange range : plunder) {
            List<Scope> scopes = range.getScopes();
            if (scopes.isEmpty()) {
                return range;
            } else {
                for (Scope scope : scopes) {
                    if (scope.appliesTo(unit)) {
                        return range;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Set the <code>Plunder</code> value.
     *
     * @param newPlunder The new Plunder value.
     */
    public final void setPlunder(final RandomRange newPlunder) {
        plunder.add(newPlunder);
    }


    /**
     * Get the <code>Gifts</code> value.
     *
     * @param unit an <code>Unit</code> value
     * @return a <code>RandomRange</code> value
     */
    public final RandomRange getGifts(Unit unit) {
        for (RandomRange range : gifts) {
            List<Scope> scopes = range.getScopes();
            if (scopes.isEmpty()) {
                return range;
            } else {
                for (Scope scope : scopes) {
                    if (scope.appliesTo(unit)) {
                        return range;
                    }
                }
            }
        }
        return null;
    }


    /**
     * Set the <code>Gifts</code> value.
     *
     * @param newGifts The new Gifts value.
     */
    public final void setGifts(final RandomRange newGifts) {
        gifts.add(newGifts);
    }

    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXMLImpl(out, getXMLElementTagName());
    }

    public void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);
        out.writeAttribute("capital", Boolean.toString(capital));
        out.writeAttribute("minimumSize", Integer.toString(minimumSize));
        out.writeAttribute("maximumSize", Integer.toString(maximumSize));
        out.writeAttribute("visibleRadius", Integer.toString(visibleRadius));
        out.writeAttribute("claimableRadius", Integer.toString(claimableRadius));
        out.writeAttribute("extraClaimableRadius", Integer.toString(extraClaimableRadius));
        out.writeAttribute("wanderingRadius", Integer.toString(wanderingRadius));
        out.writeAttribute("minimumGrowth", Integer.toString(minimumGrowth));
        out.writeAttribute("maximumGrowth", Integer.toString(maximumGrowth));
        out.writeAttribute("tradeBonus", Integer.toString(tradeBonus));
    }


    public void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        super.writeChildren(out);
        for (RandomRange range : plunder) {
            range.toXML(out, "plunder");
        }
        for (RandomRange range : gifts) {
            range.toXML(out, "gifts");
        }
    }

    public void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);
        capital = getAttribute(in, "capital", capital);
        minimumSize = getAttribute(in, "minimumSize", minimumSize);
        maximumSize = getAttribute(in, "maximumSize", maximumSize);
        visibleRadius = getAttribute(in, "visibleRadius", visibleRadius);
        claimableRadius = getAttribute(in, "claimableRadius", claimableRadius);
        extraClaimableRadius = getAttribute(in, "extraClaimableRadius", extraClaimableRadius);
        wanderingRadius = getAttribute(in, "wanderingRadius", wanderingRadius);
        minimumGrowth = getAttribute(in, "minimumGrowth", minimumGrowth);
        maximumGrowth = getAttribute(in, "maximumGrowth", maximumGrowth);
        tradeBonus = getAttribute(in, "tradeBonus", tradeBonus);
    }

    @Override
    public void readChild(XMLStreamReader in) throws XMLStreamException {
        if ("plunder".equals(in.getLocalName())) {
            RandomRange range = new RandomRange();
            range.readFromXML(in);
            plunder.add(range);
        } else if ("gifts".equals(in.getLocalName())) {
            RandomRange range = new RandomRange();
            range.readFromXML(in);
            gifts.add(range);
        } else {
            super.readChild(in);
        }
    }


    public static String getXMLElementTagName() {
        return "settlementType";
    }

}
