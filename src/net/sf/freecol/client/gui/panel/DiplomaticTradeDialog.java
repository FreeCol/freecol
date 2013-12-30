/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.MigPanel;
import net.sf.freecol.common.ObjectWithId;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTradeItem;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.DiplomaticTrade.TradeStatus;
import net.sf.freecol.common.model.GoldTradeItem;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsTradeItem;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.NationSummary;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.StanceTradeItem;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.TradeItem;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitTradeItem;


/**
 * The panel that allows negotiations between players.
 */
public final class DiplomaticTradeDialog extends FreeColDialog<DiplomaticTrade> {

    private static Logger logger = Logger.getLogger(DiplomaticTradeDialog.class.getName());

    private static final String ACCEPT = "accept";
    private static final String CANCEL = "cancel";
    private static final String SEND = "send";

    private static final int HUGE_DEMAND = 100000;

    private class RemoveAction extends AbstractAction {
        private TradeItem item;

        public RemoveAction(TradeItem item) {
            this.item = item;
        }

        // Interface ActionListener

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(ActionEvent e) {
            agreement.remove(item);
            updateDialog();
        }
    }

    private class ColonyTradeItemPanel extends JPanel
        implements ActionListener {

        private JComboBox colonyBox;
        private JButton addButton;
        private Player player;
        private DiplomaticTradeDialog diplomaticTradeDialog;
        private JLabel textLabel;


        /**
         * Creates a new <code>ColonyTradeItemPanel</code> instance.
         *
         * @param parent The parent <code>DiplomaticTradeDialog</code>.
         * @param source The <code>Player</code> source.
         */
        public ColonyTradeItemPanel(DiplomaticTradeDialog parent, Player source) {
            this.player = source;
            this.diplomaticTradeDialog = parent;
            addButton = new JButton(Messages.message("negotiationDialog.add"));
            addButton.addActionListener(this);
            addButton.setActionCommand("add");
            this.textLabel = new JLabel(Messages.message("tradeItem.colony"));
            colonyBox = new JComboBox();

            setLayout(new MigLayout("wrap 1", "", ""));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.BLACK),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)));
            add(this.textLabel);
            add(colonyBox);
            add(addButton);
            updateColonyBox();
        }

        @SuppressWarnings("unchecked") // FIXME in Java7
        private void updateColonyBox() {

            if (!player.isEuropean()) return;

            // Remove all action listeners, so the update has no effect (except
            // updating the list).
            ActionListener[] listeners = colonyBox.getActionListeners();
            for (ActionListener al : listeners) {
                colonyBox.removeActionListener(al);
            }

            colonyBox.removeAllItems();

            Iterator<Colony> coloniesInAgreement
                = agreement.getColoniesGivenBy(player).iterator();
            List<Colony> coloniesAvail = DiplomaticTradeDialog.this
                .getFreeColClient().getClientOptions().getSortedColonies(player);

            //remove the ones already on the table
            while(coloniesInAgreement.hasNext()){
                Colony colony = coloniesInAgreement.next();
                for(int i=0;i<coloniesAvail.size();i++){
                    Colony colonyAvail = coloniesAvail.get(i);
                    if(colonyAvail == colony){
                        // this good is already on the agreement, remove it
                        coloniesAvail.remove(i);
                        break;
                    }
                }
            }

            if (coloniesAvail.isEmpty()){
                addButton.setEnabled(false);
                colonyBox.setEnabled(false);
            } else {
                for (Colony c : coloniesAvail) {
                    colonyBox.addItem(c);
                }
                for(ActionListener al : listeners) {
                    colonyBox.addActionListener(al);
                }
                addButton.setEnabled(true);
                colonyBox.setEnabled(true);
            }
        }


        // Implement ActionListener

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(ActionEvent event) {
            final String command = event.getActionCommand();
            if (command.equals("add")) {
                diplomaticTradeDialog.addColonyTradeItem(player,
                    (Colony)colonyBox.getSelectedItem());
                updateDialog();
            } else {
                logger.warning("Bad command: " + command);
            }
        }
    }

    private class GoldTradeItemPanel extends JPanel
        implements ActionListener {

        private JSpinner spinner;
        private JButton addButton;
        private Player player;
        private DiplomaticTradeDialog diplomaticTradeDialog;


        /**
         * Creates a new <code>GoldTradeItemPanel</code> instance.
         *
         * @param parent The parent <code>DiplomaticTradeDialog</code>.
         * @param source The <code>Player</code> that is trading.
         * @param gold The amount of gold to trade.
         */
        public GoldTradeItemPanel(DiplomaticTradeDialog parent, Player source,
                                  int gold) {
            this.player = source;
            this.diplomaticTradeDialog = parent;
            addButton = new JButton(Messages.message("negotiationDialog.add"));
            addButton.addActionListener(this);
            addButton.setActionCommand("add");
            spinner = new JSpinner(new SpinnerNumberModel(0, 0, gold, 1));
            // adjust entry size
            ((JSpinner.DefaultEditor)spinner.getEditor())
                .getTextField().setColumns(5);

            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.BLACK),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)));
            setLayout(new MigLayout("wrap 1", "", ""));
            add(new JLabel(Messages.message("tradeItem.gold")));
            add(spinner);
            add(addButton);
        }


        public void setAvailableGold(int gold) {
            SpinnerNumberModel model = (SpinnerNumberModel)spinner.getModel();
            model.setMaximum(new Integer(gold));
        }


        // Implement ActionListener

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(ActionEvent event) {
            final String command = event.getActionCommand();
            if (command.equals("add")) {
                int amount = ((Integer) spinner.getValue()).intValue();
                diplomaticTradeDialog.addGoldTradeItem(player, amount);
                updateDialog();
            } else {
                logger.warning("Bad command: " + command);
            }
        }
    }

    private class GoodsTradeItemPanel extends JPanel
        implements ActionListener {

        private class GoodsItem implements ObjectWithId {
            private Goods value;

            public GoodsItem(Goods value) {
                if (value == null) throw new NullPointerException();
                this.value = value;
            }


            public Goods getValue() {
                return value;
            }


            // Implement ObjectWithId (so FreeColComboBoxRenderer works)

            /**
             * {@inheritDoc}
             */
            public String getId() {
                return value.getId();
            }


            // Override Object

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean equals(Object other) {
                return (other instanceof GoodsItem)
                    ? value.equals(((GoodsItem)other).value)
                    : false;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public int hashCode() {
                return value.hashCode();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return Messages.message(value.getLabel(true));
            }
        }

        private JComboBox goodsBox;
        private JButton addButton;
        private Player player;
        private DiplomaticTradeDialog diplomaticTradeDialog;
        private JLabel label;

        /**
         * Creates a new <code>GoodsTradeItemPanel</code> instance.
         *
         * @param parent The parent <code>DiplomaticTradeDialog</code>.
         * @param source The <code>Player</code> nominally in posession of the
         *     goods (this may be totally fictional).
         * @param allGoods The <code>Goods</code> to trade.
         */
        @SuppressWarnings("unchecked") // FIXME in Java7
        public GoodsTradeItemPanel(DiplomaticTradeDialog parent, Player source,
                                   List<Goods> allGoods) {
            this.player = source;
            this.diplomaticTradeDialog = parent;
            addButton = new JButton(Messages.message("negotiationDialog.add"));
            addButton.addActionListener(this);
            addButton.setActionCommand("add");
            this.label = new JLabel(Messages.message("tradeItem.goods"));
            goodsBox = new JComboBox(new DefaultComboBoxModel());

            setLayout(new MigLayout("wrap 1", "", ""));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.BLACK),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)));
            add(label);
            add(goodsBox);
            add(addButton);
            setSize(getPreferredSize());
            updateGoodsBox(allGoods);
        }

        @SuppressWarnings("unchecked") // FIXME in Java7
        private void updateGoodsBox(List<Goods> allGoods) {
            // Remove all action listeners, so the update has no
            // effect (except updating the list).
            ActionListener[] listeners = goodsBox.getActionListeners();
            for (ActionListener al : listeners) {
                goodsBox.removeActionListener(al);
            }

            goodsBox.removeAllItems();
            for (Goods g : allGoods) goodsBox.addItem(new GoodsItem(g));

            for (ActionListener al : listeners) {
                goodsBox.addActionListener(al);
            }

            boolean enable = !allGoods.isEmpty();
            this.label.setEnabled(enable);
            addButton.setEnabled(enable);
            goodsBox.setEnabled(enable);
        }


        // Interface ActionListener

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(ActionEvent event) {
            final String command = event.getActionCommand();
            if (command.equals("add")) {
                diplomaticTradeDialog.addGoodsTradeItem(player,
                    ((GoodsItem)goodsBox.getSelectedItem()).getValue());
                updateDialog();
            } else {
                logger.warning("Bad command: " + command);
            }
        }
    }

    /**
     * Class for the stance trade panel.  Access needs to be public so
     * that comboBoxLabel() is externally visible.
     */
    public class StanceTradeItemPanel extends JPanel
        implements ActionListener {

        public class StanceItem implements ObjectWithId {
            private Stance value;

            public StanceItem(Stance value) {
                this.value = value;
            }


            public Stance getValue() {
                return value;
            }


            // Implement ObjectWithId (so FreeColComboBoxRenderer works)
            
            /**
             * {@inheritDoc}
             */
            public String getId() {
                return value.getKey();
            }


            // Override Object

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean equals(Object other) {
                if (other == null || !(other instanceof StanceItem)) {
                    return false;
                }
                return value.equals(((StanceItem) other).value);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public int hashCode() {
                return value.hashCode();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return Messages.message(value.getKey());
            }
        }

        private JComboBox stanceBox;
        private JButton addButton;
        private DiplomaticTradeDialog diplomaticTradeDialog;
        private Player source;
        private Player target;


        /**
         * Creates a new <code>StanceTradeItemPanel</code> instance.
         *
         * @param parent The parent <code>DiplomaticTradeDialog</code>.
         * @param source The <code>Player</code> offering the stance change.
         * @param target The <code>Player</code> to consider the stance change.
         */
        public StanceTradeItemPanel(DiplomaticTradeDialog parent, Player source,
                                    Player target) {
            this.diplomaticTradeDialog = parent;
            this.source = source;
            this.target = target;

            addButton = new JButton(Messages.message("negotiationDialog.add"));
            addButton.addActionListener(this);
            addButton.setActionCommand("add");
            stanceBox = new JComboBox();

            updateStanceBox();

            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.BLACK),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)));
            setLayout(new MigLayout("wrap 1", "", ""));
            add(new JLabel(Messages.message("tradeItem.stance")));
            add(stanceBox);
            add(addButton);
        }

        @SuppressWarnings("unchecked") // FIXME in Java7
        public void updateStanceBox(){
            stanceBox.removeAllItems();

            Stance stance = source.getStance(target);
            if (stance != Stance.WAR) {
                stanceBox.addItem(new StanceItem(Stance.WAR));
            } else {
                stanceBox.addItem(new StanceItem(Stance.CEASE_FIRE));
            }
            if (stance != Stance.PEACE && stance != Stance.UNCONTACTED) {
                stanceBox.addItem(new StanceItem(Stance.PEACE));
            }
            if (stance != Stance.ALLIANCE) {
                stanceBox.addItem(new StanceItem(Stance.ALLIANCE));
            }

            Stance select = diplomaticTradeDialog.getStance();
            if (select != null) {
                stanceBox.setSelectedItem(new StanceItem(select));
            }
        }


        // Interface ActionListener

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(ActionEvent event) {
            final String command = event.getActionCommand();
            if (command.equals("add")) {
                StanceItem stance = (StanceItem) stanceBox.getSelectedItem();
                diplomaticTradeDialog.setStance(stance.getValue());
                updateSummary();
            } else {
                logger.warning("Bad command: " + command);
            }
        }
    }

    /** The unit negotiating the agreement. */
    private final Unit unit;

    /** The settlement to negotiate with. */
    private final Settlement settlement;

    /** The other player in the negotiation (!= getMyPlayer()). */
    private Player otherPlayer;

    /** The agreement under negotiation. */
    private DiplomaticTrade agreement;

    /** The panels for various negotiable data. */
    private StanceTradeItemPanel stancePanel;
    private GoldTradeItemPanel goldOfferPanel, goldDemandPanel;
    private ColonyTradeItemPanel colonyOfferPanel, colonyDemandPanel;
    private GoodsTradeItemPanel goodsOfferPanel, goodsDemandPanel;
    //private UnitTradeItemPanel unitOffer, unitDemand;

    /** A panel showing a summary of the current agreement. */
    private JPanel summary;

    /** Useful internal messages. */
    private String demandMessage, offerMessage, exchangeMessage;


    /**
     * Creates a new <code>DiplomaticTradeDialog</code> instance.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @param agreement The <code>DiplomaticTrade</code> agreement that
     *     is being negotiated.
     * @param comment An optional <code>StringTemplate</code>
     *     commentary message.
     */
    public DiplomaticTradeDialog(FreeColClient freeColClient, Unit unit,
                                 Settlement settlement,
                                 DiplomaticTrade agreement,
                                 StringTemplate comment) {
        super(freeColClient);

        final Player player = getMyPlayer();
        final Player unitPlayer = unit.getOwner();
        final Player colonyPlayer = settlement.getOwner();

        setFocusCycleRoot(true);

        this.unit = unit;
        this.settlement = settlement;
        this.otherPlayer = (unitPlayer == player) ? colonyPlayer
            : unitPlayer;
        this.agreement = (agreement != null) ? agreement
            : new DiplomaticTrade(unit.getGame(), unitPlayer, colonyPlayer, 
                                  null, 0);
        boolean canAccept = agreement != null; // a new offer can't be accepted
        this.demandMessage = Messages
            .message(StringTemplate.template("negotiationDialog.demand")
                .addStringTemplate("%nation%", unitPlayer.getNationName()));
        JTextArea commentArea = GUI.getDefaultTextArea((comment == null) ? ""
            : Messages.message(comment));
        this.offerMessage = Messages
            .message(StringTemplate.template("negotiationDialog.offer")
                .addStringTemplate("%nation%", unitPlayer.getNationName()));
        this.exchangeMessage = Messages
            .message(StringTemplate.template("negotiationDialog.exchange")
                .addStringTemplate("%nation%", unitPlayer.getNationName()));

        if (player.atWarWith(otherPlayer)) {
            if (getStance() == null) {
                this.agreement.add(new StanceTradeItem(getGame(), player,
                        otherPlayer, Stance.PEACE));
            }
        }

        this.stancePanel = new StanceTradeItemPanel(this, player, otherPlayer);
        this.goldDemandPanel = new GoldTradeItemPanel(this, otherPlayer,
                                                      HUGE_DEMAND);
        this.goldOfferPanel = new GoldTradeItemPanel(this, player,
            ((player.getGold() == Player.GOLD_NOT_ACCOUNTED) ? HUGE_DEMAND
                : player.getGold()));
        this.colonyDemandPanel = new ColonyTradeItemPanel(this, otherPlayer);
        this.colonyOfferPanel = new ColonyTradeItemPanel(this, player);
        /** TODO: UnitTrade
            unitDemand = new UnitTradeItemPanel(this, otherPlayer);
            unitOffer = new UnitTradeItemPanel(this, player);
        */

        this.summary = new MigPanel();
        this.summary.setLayout(new MigLayout("wrap 2", "[20px][]"));
        this.summary.setOpaque(false);

        MigPanel panel = new MigPanel(new MigLayout("wrap 3",
                "[200, fill][300, fill][200, fill]", ""));
        panel.add(new JLabel(this.demandMessage), "center");
        panel.add(commentArea, "center");
        panel.add(new JLabel(this.offerMessage), "center");

        panel.add(this.goldDemandPanel);
        panel.add(this.summary, "spany, top");
        panel.add(this.goldOfferPanel);
        if (unit.isCarrier()) {
            List<Goods> unitGoods = unit.getGoodsList();
            this.goodsDemandPanel = new GoodsTradeItemPanel(this, otherPlayer,
                                                            getAnyGoods());
            panel.add(this.goodsDemandPanel);
            this.goodsOfferPanel = new GoodsTradeItemPanel(this, player,
                ((unit.getOwner() == player) ? unit.getGoodsList()
                    : settlement.getCompactGoods()));
            panel.add(this.goodsOfferPanel);
        } else {
            panel.add(this.colonyDemandPanel);
            panel.add(this.colonyOfferPanel);
        }
        panel.add(this.stancePanel, "skip");
        /** TODO: UnitTrade
            add(unitDemand, higConst.rc(row, demandColumn));
            add(unitOffer, higConst.rc(row, offerColumn));
        */
        panel.setPreferredSize(panel.getPreferredSize());

        DiplomaticTrade bogus = null;
        String str;
        List<ChoiceItem<DiplomaticTrade>> c = choices();
        if (canAccept) {
            str = Messages.message("negotiationDialog.accept");
            c.add(new ChoiceItem<DiplomaticTrade>(str, bogus));
        }
        str = Messages.message("negotiationDialog.send");
        c.add(new ChoiceItem<DiplomaticTrade>(str, bogus)
            .okOption());
        str = Messages.message("negotiationDialog.cancel");
        c.add(new ChoiceItem<DiplomaticTrade>(str, bogus)
            .cancelOption().defaultOption());
        ImageIcon icon = getImageLibrary()
            .getImageIcon((otherPlayer == unitPlayer) ? unit : settlement,
                          false);
        initialize(DialogType.QUESTION, true, panel, icon, c);

        updateDialog();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public DiplomaticTrade getResponse() {
        Object value = getValue();
        agreement.setStatus((options.size()==3 && options.get(0).equals(value))
            ? TradeStatus.ACCEPT_TRADE
            : (options.size() == 3 && options.get(1).equals(value))
            ? TradeStatus.PROPOSE_TRADE
            : (options.size() == 2 && options.get(0).equals(value))
            ? TradeStatus.PROPOSE_TRADE
            : TradeStatus.REJECT_TRADE);
        return agreement;
    }

    /**
     * Gets a list of all possible storable goods (one cargo load).
     * Note that these goods are fictional.  They are the goods that
     * *might* be in the other player's store.  Therefore they have a
     * null location (using the other player unit or settlement is not
     * valid on the client side, as there may not even be a
     * GoodsContainer present).
     *
     * @return A list of storable <code>Goods</code>.
     */
    private List<Goods> getAnyGoods() {
        List<Goods> goodsList = new ArrayList<Goods>();
        for (GoodsType type : getSpecification().getGoodsTypeList()) {
            if (type.isStorable()) {
                goodsList.add(new Goods(getGame(), null, type,
                                        GoodsContainer.CARGO_SIZE));
            }
        }
        return goodsList;
    }

    /**
     * Update the entire dialog.
     */
    private void updateDialog() {
        stancePanel.updateStanceBox();
        updateOfferItems();
        updateDemandItems();
        updateSummary();
    }

    /**
     * Update the items being offered.
     */
    private void updateOfferItems() {
        final Player player = getMyPlayer();

        goldOfferPanel.setAvailableGold(player.getGold());

        if (unit.isCarrier()) {
            List<Goods> goodsAvail = new ArrayList<Goods>();
            if (unit.getOwner() == player) {
                goodsAvail.addAll(unit.getCompactGoodsList());
            } else {
                goodsAvail.addAll(settlement.getCompactGoods());
            }
            for (Goods g : goodsAvail) {
                if (g.getAmount() > GoodsContainer.CARGO_SIZE) {
                    g.setAmount(GoodsContainer.CARGO_SIZE);
                }
            }

            for (Goods goods : agreement.getGoodsGivenBy(player)) {
                // Remove the ones already on the table
                for (int i = 0; i < goodsAvail.size(); i++) {
                    Goods g = goodsAvail.get(i);
                    if (g.getType() == goods.getType()) {
                        if (g.getAmount() <= goods.getAmount()) {
                            goodsAvail.remove(i);
                        } else {
                            g.setAmount(g.getAmount() - goods.getAmount());
                        }
                        break;
                    }
                }
            }
            goodsOfferPanel.updateGoodsBox(goodsAvail);

        } else {
            colonyOfferPanel.updateColonyBox();
        }
    }

    /**
     * Update the items being demanded.
     */
    private void updateDemandItems() {
        final Player player = getMyPlayer();

        NationSummary ns = getController().getNationSummary(otherPlayer);
        int foreignGold = (ns == null) ? 0 : ns.getGold();
        goldDemandPanel.setAvailableGold(foreignGold);

        if (unit.isCarrier()) {
            // Not agreement.getGoodsGivenBy(otherPlayer))!
            goodsDemandPanel.updateGoodsBox(getAnyGoods());
        } else {
            colonyDemandPanel.updateColonyBox();
        }
    }

    /**
     * Gets a trade item button for a given item.
     *
     * @param item The <code>TradeItem</code> to make a button for.
     * @return A new <code>JButton</code> for the item.
     */
    private JButton getTradeItemButton(TradeItem item) {
        String description = null;
        if (item instanceof StanceTradeItem) {
            StanceTradeItem i = (StanceTradeItem)item;
            description = Messages.message(i.getStance().getKey());
        } else if (item instanceof GoldTradeItem) {
            int gold = ((GoldTradeItem)item).getGold();
            description = Messages
                .message(StringTemplate.template("tradeItem.gold.long")
                    .addAmount("%amount%", gold));
        } else if (item instanceof ColonyTradeItem) {
            ColonyTradeItem i = (ColonyTradeItem)item;
            description = Messages
                .message(StringTemplate.template("tradeItem.colony.long")
                    .addName("%colony%", i.getColonyName()));
        } else if (item instanceof GoodsTradeItem) {
            GoodsTradeItem i = (GoodsTradeItem)item;
            description = Messages
                .message(StringTemplate.template("model.goods.goodsAmount")
                    .addAmount("%amount%", i.getGoods().getAmount())
                    .add("%goods%", i.getGoods().getNameKey()));
        } else if (item instanceof UnitTradeItem) {
            UnitTradeItem i = (UnitTradeItem)item;
            description = Messages.getLabel(i.getUnit());
        }
        JButton button = new JButton(new RemoveAction(item));
        button.setText(description);
        button.setMargin(GUI.EMPTY_MARGIN);
        button.setOpaque(false);
        button.setForeground(GUI.LINK_COLOR);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    /**
     * Update the text summary of the proposed transaction.
     */
    private void updateSummary() {
        final Player player = getMyPlayer();

        summary.removeAll();

        List<TradeItem> offers = agreement.getItemsGivenBy(player);
        if (!offers.isEmpty()) {
            summary.add(new JLabel(offerMessage), "span");
            for (TradeItem item : offers) {
                summary.add(getTradeItemButton(item), "skip");
            }
        }

        List<TradeItem> demands = agreement.getItemsGivenBy(otherPlayer);
        if (!demands.isEmpty()) {
            if (demands.isEmpty()) {
                summary.add(new JLabel(demandMessage), "span");
            } else {
                summary.add(new JLabel(exchangeMessage), "newline 20, span");
            }
            for (TradeItem item : demands) {
                summary.add(getTradeItemButton(item), "skip");
            }
        }
    }


    /**
     * Adds a <code>ColonyTradeItem</code> to the list of TradeItems.
     *
     * @param source a <code>Player</code> value
     * @param colony a <code>Colony</code> value
     */
    public void addColonyTradeItem(Player source, Colony colony) {
        final Player player = getMyPlayer();

        Player destination = (source == otherPlayer) ? player : otherPlayer;
        agreement.add(new ColonyTradeItem(getGame(), source, destination,
                                          colony));
    }

    /**
     * Adds a <code>GoldTradeItem</code> to the list of TradeItems.
     *
     * @param source a <code>Player</code> value
     * @param amount an <code>int</code> value
     */
    public void addGoldTradeItem(Player source, int amount) {
        final Player player = getMyPlayer();

        Player destination = (source == otherPlayer) ? player : otherPlayer;
        agreement.add(new GoldTradeItem(getGame(), source, destination,
                                        amount));
    }

    /**
     * Adds a <code>GoodsTradeItem</code> to the list of TradeItems.
     *
     * @param source a <code>Player</code> value
     * @param goods a <code>Goods</code> value
     */
    public void addGoodsTradeItem(Player source, Goods goods) {
        final Player player = getMyPlayer();

        Player destination = (source == otherPlayer) ? player : otherPlayer;
        agreement.add(new GoodsTradeItem(getGame(), source, destination,
                                         goods, settlement));
    }

    /**
     * Trade a stance change between the players.
     *
     * @param stance The <code>Stance</code> to trade.
     */
    public void setStance(Stance stance) {
        final Player player = getMyPlayer();

        agreement.add(new StanceTradeItem(getGame(), otherPlayer, player,
                                          stance));
    }

    /**
     * Returns the stance being offered.
     *
     * @return a <code>Stance</code> value
     */
    public Stance getStance() {
        return agreement.getStance();
    }


    // Override Component

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeNotify() {
        super.removeNotify();
        
        removeAll();

        this.stancePanel = null;
        this.goldOfferPanel = this.goldDemandPanel = null;
        this.colonyOfferPanel = this.colonyDemandPanel = null;
        this.goodsOfferPanel = this.goodsDemandPanel = null;
        this.summary = null;
        this.demandMessage = this.offerMessage = this.exchangeMessage = null;
    }
}
