
package net.sf.freecol.common.model;


import java.util.ArrayList;
import java.util.Iterator;


/**
 * The class <code>DiplomaticTrade</code> represents an offer one
 * player can make another.
 *
 */
public class DiplomaticTrade {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE   = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    // the individual items the trade consists of
    private ArrayList<TradeItem> items = new ArrayList<TradeItem>();

    /**
     * Add a TradeItem to the DiplomaticTrade.
     *
     * @param newItem a <code>TradeItem</code> value
     */
    public void add(TradeItem newItem) {
        items.add(newItem);
    }

    /**
     * Remove a TradeItem from the DiplomaticTrade.
     *
     * @param newItem a <code>TradeItem</code> value
     */
    public void remove(TradeItem newItem) {
        items.remove(newItem);
    }

    /**
     * Returns an iterator for all TradeItems.
     *
     * @return an iterator for all TradeItems.
     */
    public Iterator<TradeItem> iterator() {
        return items.iterator();
    }

    /**
     * One of the items a DiplomaticTrade consists of.
     *
     */
    public abstract class TradeItem {
    
        // the ID, used to get a name, etc.
        protected final String ID;
        // the player offering something
        protected final Player source;
        // the player who is to receive something
        protected final Player destination;
        
        /**
         * Creates a new <code>TradeItem</code> instance.
         *
         * @param id a <code>String</code> value
         * @param source a <code>Player</code> value
         * @param destination a <code>Player</code> value
         */
        public TradeItem(String id, Player source, Player destination) {
            this.ID = id;
            this.source = source;
            this.destination = destination;
        }

        /**
         * Returns whether this TradeItem is valid.
         *
         * @return a <code>boolean</code> value
         */
        public abstract boolean isValid();

        /**
         * Concludes the trade.
         *
         */
        public abstract void makeTrade();

    }

    public class GoldTradeItem extends TradeItem {
    
        private int gold;
        
        public GoldTradeItem(Player source, Player destination, int gold) {
            super("tradeItem.gold", source, destination);
            this.gold = gold;
        }

        public boolean isValid() {
            return ((gold >= 0) && (source.getGold() >= gold));
        }

        public void makeTrade() {
            source.modifyGold(-gold);
            destination.modifyGold(gold);
        }

    }

    public class StanceTradeItem extends TradeItem {
    
        private int stance;
        
        public StanceTradeItem(Player source, Player destination, int stance) {
            super("tradeItem.stance", source, destination);
            this.stance = stance;
        }

        public boolean isValid() {
            return (stance == Player.WAR ||
                    stance == Player.CEASE_FIRE ||
                    stance == Player.PEACE ||
                    stance == Player.ALLIANCE);
        }

        public void makeTrade() {
            source.setStance(destination, stance);
            destination.setStance(source, stance);
        }

    }

    public class GoodsTradeItem extends TradeItem {
    
        private Goods goods;
        private Settlement settlement;
        
        public GoodsTradeItem(Player source, Player destination, Goods goods, Settlement settlement) {
            super("tradeItem.goods", source, destination);
            this.goods = goods;
            this.settlement = settlement;
        }

        public boolean isValid() {
            if (!(goods.getLocation() instanceof Unit)) {
                return false;
            }
            Unit unit = (Unit) goods.getLocation();
            if (unit.getOwner() != source) {
                return false;
            }
            if (settlement != null && settlement.getOwner() == destination) {
                return true;
            } else {
                return false;
            }
        }

        public void makeTrade() {
            goods.getLocation().remove(goods);
            settlement.add(goods);
        }

    }


    public class ColonyTradeItem extends TradeItem {
    
        private Colony colony;
        
        public ColonyTradeItem(Player source, Player destination, Colony colony) {
            super("tradeItem.colony", source, destination);
            this.colony = colony;
        }

        public boolean isValid() {
            return (colony.getOwner() == source);
        }

        public void makeTrade() {
            colony.setOwner(destination);
        }

    }

    public class UnitTradeItem extends TradeItem {
    
        private Unit unit;
        
        public UnitTradeItem(Player source, Player destination, Unit unit) {
            super("tradeItem.unit", source, destination);
            this.unit = unit;
        }

        public boolean isValid() {
            return (unit.getOwner() == source);
        }

        public void makeTrade() {
            unit.setOwner(destination);
        }

    }

}
