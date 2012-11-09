/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTradeItem;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.DiplomaticTrade.TradeStatus;
import net.sf.freecol.common.model.GoldTradeItem;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsTradeItem;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;
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
public final class NegotiationDialog extends FreeColDialog<DiplomaticTrade>
    implements ActionListener {

    private static Logger logger = Logger.getLogger(NegotiationDialog.class.getName());

    private static final String ACCEPT = "accept";
    private static final String CANCEL = "cancel";
    private static final String SEND = "send";

    private static final int HUGE_DEMAND = 100000;

    private DiplomaticTrade agreement;

    private JButton acceptButton, cancelButton, sendButton;
    private StanceTradeItemPanel stancePanel;
    private GoldTradeItemPanel goldOfferPanel, goldDemandPanel;
    private ColonyTradeItemPanel colonyOfferPanel, colonyDemandPanel;
    private GoodsTradeItemPanel goodsOfferPanel, goodsDemandPanel;
    //private UnitTradeItemPanel unitOffer, unitDemand;
    private JPanel summary;

    private final Unit unit;
    private final Settlement settlement;
    private Player player;
    private Player otherPlayer;
    private Player sender;
    private Player recipient;
    private boolean canAccept;

    private String demandMessage;
    private String offerMessage;
    private String exchangeMessage;


    /**
     * Creates a new <code>NegotiationDialog</code> instance.
     *
     * @param freeColClient The current <code>FreeColClient</code>.
     * @param gui The <code>GUI</code> to display on.
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     */
    public NegotiationDialog(FreeColClient freeColClient, GUI gui, Unit unit,
                             Settlement settlement) {
        this(freeColClient, gui, unit, settlement, null);
    }

    /**
     * Creates a new <code>NegotiationDialog</code> instance.
     *
     * @param freeColClient The current <code>FreeColClient</code>.
     * @param gui The <code>GUI</code> to display on.
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @param agreement The <code>DiplomaticTrade</code> agreement that
     *     is being negotiated.
     */
    public NegotiationDialog(FreeColClient freeColClient, GUI gui, Unit unit,
                             Settlement settlement, DiplomaticTrade agreement){
        super(freeColClient, gui);
        setFocusCycleRoot(true);

        this.unit = unit;
        this.settlement = settlement;
        this.player = getMyPlayer();
        this.sender = unit.getOwner();
        this.recipient = settlement.getOwner();
        this.otherPlayer = (sender == player) ? recipient : sender;
        this.canAccept = agreement != null; // a new offer can't be accepted
        this.agreement = (agreement != null) ? agreement
            : new DiplomaticTrade(unit.getGame(), sender, recipient);

        demandMessage = Messages
            .message(StringTemplate.template("negotiationDialog.demand")
                     .addStringTemplate("%nation%", sender.getNationName()));
        offerMessage = Messages
            .message(StringTemplate.template("negotiationDialog.offer")
                     .addStringTemplate("%nation%", sender.getNationName()));
        exchangeMessage = Messages
            .message(StringTemplate.template("negotiationDialog.exchange")
                     .addStringTemplate("%nation%", sender.getNationName()));

        if (player.atWarWith(otherPlayer)) {
            if (getStance() == null) {
                this.agreement.add(new StanceTradeItem(getGame(), player,
                        otherPlayer, Stance.PEACE));
            }
        }

        summary = new JPanel(new MigLayout("wrap 2", "[20px][]"));
        summary.setOpaque(false);
    }

    /**
     * Set up the dialog.
     */
    @Override
    public void initialize() {
        sendButton = new JButton(Messages.message("negotiationDialog.send"));
        sendButton.addActionListener(this);
        sendButton.setActionCommand(SEND);
        FreeColPanel.enterPressesWhenFocused(sendButton);

        acceptButton = new JButton(Messages.message("negotiationDialog.accept"));
        acceptButton.addActionListener(this);
        acceptButton.setActionCommand(ACCEPT);
        FreeColPanel.enterPressesWhenFocused(acceptButton);
        acceptButton.setEnabled(canAccept);

        cancelButton = new JButton(Messages.message("negotiationDialog.cancel"));
        cancelButton.addActionListener(this);
        cancelButton.setActionCommand(CANCEL);
        setCancelComponent(cancelButton);
        FreeColPanel.enterPressesWhenFocused(cancelButton);

        stancePanel = new StanceTradeItemPanel(this, player, otherPlayer);
        goldDemandPanel = new GoldTradeItemPanel(this, otherPlayer,
                                                 HUGE_DEMAND);
        goldOfferPanel = new GoldTradeItemPanel(this, player,
            ((player.getGold() == Player.GOLD_NOT_ACCOUNTED) ? HUGE_DEMAND
                : player.getGold()));
        colonyDemandPanel = new ColonyTradeItemPanel(this, otherPlayer);
        colonyOfferPanel = new ColonyTradeItemPanel(this, player);
        /** TODO: UnitTrade
            unitDemand = new UnitTradeItemPanel(this, otherPlayer);
            unitOffer = new UnitTradeItemPanel(this, player);
        */

        setLayout(new MigLayout("wrap 3", "[200, fill][300, fill][200, fill]",
                                ""));

        add(new JLabel(demandMessage), "center");
        add(new JLabel(offerMessage), "skip, center");

        add(goldDemandPanel);
        add(summary, "spany, top");
        add(goldOfferPanel);
        if (unit.isCarrier()) {
            List<Goods> unitGoods = unit.getGoodsList();
            goodsDemandPanel = new GoodsTradeItemPanel(this, otherPlayer,
                                                       getAnyGoods());
            add(goodsDemandPanel);
            goodsOfferPanel = new GoodsTradeItemPanel(this, player,
                ((unit.getOwner() == player) ? unit.getGoodsList()
                    : settlement.getCompactGoods()));
            add(goodsOfferPanel);
        } else {
            add(colonyDemandPanel);
            add(colonyOfferPanel);
        }
        add(stancePanel, "skip");
        /** TODO: UnitTrade
            add(unitDemand, higConst.rc(row, demandColumn));
            add(unitOffer, higConst.rc(row, offerColumn));
        */
        add(sendButton, "newline 20, span, split 3");
        add(acceptButton, "tag ok");
        add(cancelButton, "tag cancel");

        updateDialog();
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
        goldOfferPanel.setAvailableGold(player.getGold());

        if (unit.isCarrier()) {
            List<Goods> goodsAvail = new ArrayList<Goods>();
            if (unit.getOwner() == player) {
                goodsAvail.addAll(unit.getGoodsList());
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
        NationSummary ns = getController().getNationSummary(otherPlayer);
        int foreignGold = (ns == null) ? 0 : ns.getGold();
        goldDemandPanel.setAvailableGold(foreignGold);

        if (unit.isCarrier()) {
            goodsDemandPanel.updateGoodsBox(agreement.getGoodsGivenBy(otherPlayer));

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
            description = Messages.message(i.getUnit().getLabel());
        }
        JButton button = new JButton(new RemoveAction(item));
        button.setText(description);
        button.setMargin(emptyMargin);
        button.setOpaque(false);
        button.setForeground(LINK_COLOR);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    /**
     * Update the text summary of the proposed transaction.
     */
    private void updateSummary() {
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

    /**
     * Analyzes an event and calls the right external methods to take care of
     * the user's request.
     *
     * @param event The incoming <code>ActionEvent</code>.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (command.equals(CANCEL)) {
            agreement.setStatus(TradeStatus.REJECT_TRADE);
            setResponse(agreement);
        } else if (command.equals(ACCEPT)) {
            agreement.setStatus(TradeStatus.ACCEPT_TRADE);
            setResponse(agreement);
        } else if (command.equals(SEND)) {
            agreement.setStatus(TradeStatus.PROPOSE_TRADE);
            setResponse(agreement);
        }
    }


    private class RemoveAction extends AbstractAction {
        private TradeItem item;

        public RemoveAction(TradeItem item) {
            this.item = item;
        }

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
        private NegotiationDialog negotiationDialog;
        private JLabel textLabel;


        /**
         * Creates a new <code>ColonyTradeItemPanel</code> instance.
         *
         * @param parent The parent <code>NegotiationDialog</code>.
         * @param source The <code>Player</code> source.
         */
        public ColonyTradeItemPanel(NegotiationDialog parent, Player source) {
            this.player = source;
            this.negotiationDialog = parent;
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

            Iterator<Colony> coloniesInAgreement = agreement.getColoniesGivenBy(player).iterator();
            List<Colony> coloniesAvail = getClientOptions().getSortedColonies(player);

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

        /**
         * Analyzes an event and calls the right external methods to
         * take care of the user's request.
         *
         * @param event The incoming <code>ActionEvent</code>.
         */
        public void actionPerformed(ActionEvent event) {
            String command = event.getActionCommand();
            if (command.equals("add")) {
                negotiationDialog.addColonyTradeItem(player,
                    (Colony)colonyBox.getSelectedItem());
                updateDialog();
            }
        }
    }

    private class GoldTradeItemPanel extends JPanel
        implements ActionListener {

        private JSpinner spinner;
        private JButton addButton;
        private Player player;
        private NegotiationDialog negotiationDialog;


        /**
         * Creates a new <code>GoldTradeItemPanel</code> instance.
         *
         * @param parent The parent <code>NegotiationDialog</code>.
         * @param source The <code>Player</code> that is trading.
         * @param gold The amount of gold to trade.
         */
        public GoldTradeItemPanel(NegotiationDialog parent, Player source,
                                  int gold) {
            this.player = source;
            this.negotiationDialog = parent;
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

        /**
         * Analyzes an event and calls the right external methods to
         * take care of the user's request.
         *
         * @param event The incoming action event
         */
        public void actionPerformed(ActionEvent event) {
            String command = event.getActionCommand();
            if (command.equals("add")) {
                int amount = ((Integer) spinner.getValue()).intValue();
                negotiationDialog.addGoldTradeItem(player, amount);
                updateDialog();
            }
        }

        public void setAvailableGold(int gold) {
            SpinnerNumberModel model = (SpinnerNumberModel)spinner.getModel();
            model.setMaximum(new Integer(gold));
        }
    }

    private class GoodsTradeItemPanel extends JPanel
        implements ActionListener {

        private class GoodsItem {
            private Goods value;

            public GoodsItem(Goods value) {
                if (value == null) throw new NullPointerException();
                this.value = value;
            }

            @Override
            public String toString() {
                return Messages.message(value.getLabel(true));
            }

            public Goods getValue() {
                return value;
            }

            @Override
            public boolean equals(Object other) {
                return (other instanceof GoodsItem)
                    ? value.equals(((GoodsItem)other).value)
                    : false;
            }

            @Override
            public int hashCode() {
                return value.hashCode();
            }
        }

        private JComboBox goodsBox;
        private JButton addButton;
        private Player player;
        private NegotiationDialog negotiationDialog;
        private JLabel label;


        /**
         * Creates a new <code>GoodsTradeItemPanel</code> instance.
         *
         * @param parent The parent <code>NegotiationDialog</code>.
         * @param source The <code>Player</code> nominally in posession of the
         *     goods (this may be totally fictional).
         * @param allGoods The <code>Goods</code> to trade.
         */
        @SuppressWarnings("unchecked") // FIXME in Java7
        public GoodsTradeItemPanel(NegotiationDialog parent, Player source,
                                   List<Goods> allGoods) {
            this.player = source;
            this.negotiationDialog = parent;
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

        /**
         * Analyzes an event and calls the right external methods to
         * take care of the user's request.
         *
         * @param event The incoming <code>ActionEvent</code>.
         */
        public void actionPerformed(ActionEvent event) {
            String command = event.getActionCommand();
            if (command.equals("add")) {
                negotiationDialog.addGoodsTradeItem(player,
                    ((GoodsItem)goodsBox.getSelectedItem()).getValue());
                updateDialog();
            }
        }
    }

    private class StanceTradeItemPanel extends JPanel
        implements ActionListener {

        private class StanceItem {
            private Stance value;

            public StanceItem(Stance value) {
                this.value = value;
            }

            @Override
            public String toString() {
                return Messages.message(value.getKey());
            }

            public Stance getValue() {
                return value;
            }

            @Override
            public boolean equals(Object other) {
                if (other == null || !(other instanceof StanceItem)) {
                    return false;
                }
                return value.equals(((StanceItem) other).value);
            }

            @Override
            public int hashCode() {
                return value.hashCode();
            }
        }

        private JComboBox stanceBox;
        private JButton addButton;
        private NegotiationDialog negotiationDialog;
        private Player source;
        private Player target;


        /**
         * Creates a new <code>StanceTradeItemPanel</code> instance.
         *
         * @param parent The parent <code>NegotiationDialog</code>.
         * @param source The <code>Player</code> offering the stance change.
         * @param target The <code>Player</code> to consider the stance change.
         */
        public StanceTradeItemPanel(NegotiationDialog parent, Player source,
                                    Player target) {
            this.negotiationDialog = parent;
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

        /**
         * Analyzes an event and calls the right external methods to
         * take care of the user's request.
         *
         * @param event The incoming <code>ActionEvent</code>.
         */
        public void actionPerformed(ActionEvent event) {
            String command = event.getActionCommand();
            if (command.equals("add")) {
                StanceItem stance = (StanceItem) stanceBox.getSelectedItem();
                negotiationDialog.setStance(stance.getValue());
                updateSummary();
            }
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

            Stance select = negotiationDialog.getStance();
            if (select != null) {
                stanceBox.setSelectedItem(new StanceItem(select));
            }
        }
    }
}
