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
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import javax.swing.SpinnerNumberModel;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.MigPanel;
import net.sf.freecol.common.ObjectWithId;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTradeItem;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.DiplomaticTrade.TradeContext;
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

        private final Player source;
        private JComboBox colonyBox;
        private JButton addButton;
        private JLabel label;
        private final List<Colony> allColonies;


        /**
         * Creates a new <code>ColonyTradeItemPanel</code> instance.
         *
         * @param source The <code>Player</code> source.
         */
        public ColonyTradeItemPanel(Player source) {
            this.source = source;
            this.colonyBox = new JComboBox();
            this.addButton
                = new JButton(Messages.message("negotiationDialog.add"));
            this.addButton.addActionListener(this);
            this.addButton.setActionCommand("add");
            this.label = new JLabel(Messages.message("tradeItem.colony"));
            this.allColonies = source.getColonies();

            setLayout(new MigLayout("wrap 1", "", ""));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.BLACK),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)));

            add(this.label);
            add(this.colonyBox);
            add(this.addButton);

            setSize(getPreferredSize());
        }


        @SuppressWarnings("unchecked") // FIXME in Java7
        private void update(DiplomaticTrade dt) {
            if (!source.isEuropean()) return;

            // Remove all action listeners, so the update has no effect (except
            // updating the list).
            ActionListener[] listeners = this.colonyBox.getActionListeners();
            for (ActionListener al : listeners) {
                this.colonyBox.removeActionListener(al);
            }

            List<Colony> available = new ArrayList<Colony>(allColonies);
            for (Colony c : dt.getColoniesGivenBy(source)) {
                if (available.contains(c)) {
                    available.remove(c);
                } else {
                    allColonies.add(c); // did not know about this!
                }
            }
            Collections.sort(available, getFreeColClient()
                .getClientOptions().getColonyComparator());

            this.colonyBox.removeAllItems();
            for (Colony c : available) this.colonyBox.addItem(c);

            boolean enable = !available.isEmpty();
            this.addButton.setEnabled(enable);
            this.colonyBox.setEnabled(enable);
            this.label.setEnabled(enable);

            for (ActionListener al : listeners) {
                this.colonyBox.addActionListener(al);
            }
        }


        // Implement ActionListener

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(ActionEvent event) {
            final String command = event.getActionCommand();
            if (command.equals("add")) {
                DiplomaticTradeDialog.this.addColonyTradeItem(source,
                    (Colony)colonyBox.getSelectedItem());
            } else {
                logger.warning("Bad command: " + command);
            }
        }
    }

    private class GoldTradeItemPanel extends JPanel
        implements ActionListener {

        private final Player source;
        private JSpinner spinner;
        private JButton addButton;


        /**
         * Creates a new <code>GoldTradeItemPanel</code> instance.
         *
         * @param source The <code>Player</code> that is trading.
         * @param gold The maximum amount of gold to trade.
         */
        public GoldTradeItemPanel(Player source, int gold) {
            this.source = source;
            this.spinner = new JSpinner(new SpinnerNumberModel(0, 0, gold, 1));
            this.addButton
                = new JButton(Messages.message("negotiationDialog.add"));
            this.addButton.addActionListener(this);
            this.addButton.setActionCommand("add");
            // adjust entry size
            ((JSpinner.DefaultEditor)this.spinner.getEditor())
                .getTextField().setColumns(5);

            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.BLACK),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)));
            setLayout(new MigLayout("wrap 1", "", ""));

            add(new JLabel(Messages.message("tradeItem.gold")));
            add(this.spinner);
            add(this.addButton);

            setSize(getPreferredSize());
        }


        public void update(DiplomaticTrade dt) {
            int gold = dt.getGoldGivenBy(source);
            if (gold >= 0) {
                SpinnerNumberModel model
                    = (SpinnerNumberModel)spinner.getModel();
                model.setValue(gold);
            }
        }


        // Implement ActionListener

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(ActionEvent event) {
            final String command = event.getActionCommand();
            if (command.equals("add")) {
                int amount = ((Integer)spinner.getValue()).intValue();
                DiplomaticTradeDialog.this.addGoldTradeItem(source, amount);
            } else {
                logger.warning("Bad command: " + command);
            }
        }
    }

    private class GoodsTradeItemPanel extends JPanel
        implements ActionListener {

        private class GoodsBoxRenderer extends JLabel
            implements ListCellRenderer {

            public Component getListCellRendererComponent(JList list,
                Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
                Goods goods = (Goods)value;
                setText((goods == null) ? ""
                    : Messages.message(goods.getLabel(true)));
                return this;
            }
        }

        private final Player source;
        private JComboBox goodsBox;
        private JButton addButton;
        private JLabel label;
        private final List<Goods> allGoods;


        /**
         * Creates a new <code>GoodsTradeItemPanel</code> instance.
         *
         * @param source The <code>Player</code> nominally in posession of the
         *     goods (this may be totally fictional).
         * @param allGoods The <code>Goods</code> to trade.
         */
        @SuppressWarnings("unchecked") // FIXME in Java7
        public GoodsTradeItemPanel(Player source, List<Goods> allGoods) {
            this.source = source;
            this.goodsBox = new JComboBox(new DefaultComboBoxModel());
            this.goodsBox.setRenderer(new GoodsBoxRenderer());
            this.addButton
                = new JButton(Messages.message("negotiationDialog.add"));
            this.addButton.addActionListener(this);
            this.addButton.setActionCommand("add");
            this.label = new JLabel(Messages.message("tradeItem.goods"));
            this.allGoods = allGoods;

            setLayout(new MigLayout("wrap 1", "", ""));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.BLACK),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)));

            add(this.label);
            add(this.goodsBox);
            add(this.addButton);

            setSize(getPreferredSize());
        }


        @SuppressWarnings("unchecked") // FIXME in Java7
        public void update(DiplomaticTrade dt) {
            // Remove all action listeners, so the update has no
            // effect (except updating the list).
            ActionListener[] listeners = this.goodsBox.getActionListeners();
            for (ActionListener al : listeners) {
                this.goodsBox.removeActionListener(al);
            }

            List<Goods> available = new ArrayList<Goods>(allGoods);
            for (Goods goods : dt.getGoodsGivenBy(source)) {
                // Remove the ones already on the table
                for (int i = 0; i < available.size(); i++) {
                    Goods g = available.get(i);
                    if (g.getType() == goods.getType()) {
                        if (g.getAmount() <= goods.getAmount()) {
                            available.remove(i);
                        } else {
                            g.setAmount(g.getAmount() - goods.getAmount());
                        }
                        break;
                    }
                }
            }

            this.goodsBox.removeAllItems();
            for (Goods g : available) goodsBox.addItem(g);

            boolean enable = !available.isEmpty();
            this.label.setEnabled(enable);
            this.addButton.setEnabled(enable);
            this.goodsBox.setEnabled(enable);

            for (ActionListener al : listeners) {
                this.goodsBox.addActionListener(al);
            }
        }


        // Interface ActionListener

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(ActionEvent event) {
            final String command = event.getActionCommand();
            if (command.equals("add")) {
                DiplomaticTradeDialog.this.addGoodsTradeItem(source,
                    (Goods)goodsBox.getSelectedItem());
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

        private class StanceBoxRenderer extends JLabel
            implements ListCellRenderer {

            public Component getListCellRendererComponent(JList list,
                Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
                Stance stance = (Stance)value;
                setText((stance == null) ? ""
                    : Messages.message(stance.getLabel()));
                return this;
            }
        }

        private Player source;
        private Player target;
        private JComboBox stanceBox;
        private JButton addButton;


        /**
         * Creates a new <code>StanceTradeItemPanel</code> instance.
         *
         * @param source The <code>Player</code> offering the stance change.
         * @param target The <code>Player</code> to consider the stance change.
         */
        @SuppressWarnings("unchecked") // FIXME in Java7
        public StanceTradeItemPanel(Player source, Player target) {
            this.source = source;
            this.target = target;
            this.stanceBox = new JComboBox(new DefaultComboBoxModel());
            this.stanceBox.setRenderer(new StanceBoxRenderer());
            this.addButton
                = new JButton(Messages.message("negotiationDialog.add"));
            this.addButton.addActionListener(this);
            this.addButton.setActionCommand("add");

            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.BLACK),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)));
            setLayout(new MigLayout("wrap 1", "", ""));

            add(new JLabel(Messages.message("tradeItem.stance")));
            add(this.stanceBox);
            add(this.addButton);
        }


        private void setSelectedValue(Stance stance) {
            for (int i = 0; i < stanceBox.getItemCount(); i++) {
                if (((Stance)stanceBox.getItemAt(i)) == stance) {
                    stanceBox.setSelectedItem(i);
                }
            }
        }

        @SuppressWarnings("unchecked") // FIXME in Java7
        public void update(DiplomaticTrade dt) {
            stanceBox.removeAllItems();

            Stance stance = source.getStance(target);
            if (stance != Stance.WAR) {
                stanceBox.addItem(Stance.WAR);
            } else {
                stanceBox.addItem(Stance.CEASE_FIRE);
            }
            if (stance != Stance.PEACE && stance != Stance.UNCONTACTED) {
                stanceBox.addItem(Stance.PEACE);
            }
            if (stance != Stance.WAR && stance != Stance.ALLIANCE) {
                stanceBox.addItem(Stance.ALLIANCE);
            }

            Stance select = dt.getStance();
            if (select != null) setSelectedValue(select);
        }


        // Interface ActionListener

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(ActionEvent event) {
            final String command = event.getActionCommand();
            if (command.equals("add")) {
                Stance stance = (Stance)stanceBox.getSelectedItem();
                DiplomaticTradeDialog.this.addStanceTradeItem(stance);
            } else {
                logger.warning("Bad command: " + command);
            }
        }
    }

    private class UnitTradeItemPanel extends JPanel
        implements ActionListener {

        private class UnitBoxRenderer extends JLabel
            implements ListCellRenderer {

            public Component getListCellRendererComponent(JList list,
                Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
                Unit unit = (Unit)value;
                setText((unit == null) ? ""
                    : Messages.message(unit.getLabel()));
                return this;
            }
        }

        private final Player source;
        private JComboBox unitBox;
        private JButton addButton;
        private JLabel label;
        private final List<Unit> allUnits;


        /**
         * Creates a new <code>UnitTradeItemPanel</code> instance.
         *
         * @param source The <code>Player</code> nominally in posession of the
         *     unit (this may be totally fictional).
         * @param allUnits The <code>Unit</code>s to trade.
         */
        @SuppressWarnings("unchecked") // FIXME in Java7
        public UnitTradeItemPanel(Player source, List<Unit> allUnits) {
            this.source = source;
            this.unitBox = new JComboBox(new DefaultComboBoxModel());
            this.unitBox.setRenderer(new UnitBoxRenderer());
            this.addButton
                = new JButton(Messages.message("negotiationDialog.add"));
            this.addButton.addActionListener(this);
            this.addButton.setActionCommand("add");
            this.label = new JLabel(Messages.message("tradeItem.unit"));
            this.allUnits = allUnits;

            setLayout(new MigLayout("wrap 1", "", ""));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.BLACK),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)));

            add(this.label);
            add(this.unitBox);
            add(this.addButton);

            setSize(getPreferredSize());
        }


        @SuppressWarnings("unchecked") // FIXME in Java7
        private void update(DiplomaticTrade dt) {
            // Remove all action listeners, so the update has no
            // effect (except updating the list).
            ActionListener[] listeners = unitBox.getActionListeners();
            for (ActionListener al : listeners) {
                unitBox.removeActionListener(al);
            }

            List<Unit> available = new ArrayList<Unit>(allUnits);
            for (Unit unit : dt.getUnitsGivenBy(source)) {
                // Remove the ones already on the table
                if (available.contains(unit)) {
                    available.remove(unit);
                } else {
                    allUnits.add(unit); // Did not know about this!
                }
            }

            unitBox.removeAllItems();
            for (Unit u : available) unitBox.addItem(u);

            boolean enable = !available.isEmpty();
            this.label.setEnabled(enable);
            addButton.setEnabled(enable);
            unitBox.setEnabled(enable);

            for (ActionListener al : listeners) {
                unitBox.addActionListener(al);
            }
        }


        // Interface ActionListener

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(ActionEvent event) {
            final String command = event.getActionCommand();
            if (command.equals("add")) {
                DiplomaticTradeDialog.this.addUnitTradeItem(source,
                    (Unit)unitBox.getSelectedItem());
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

    /** A comment message. */
    private StringTemplate comment;

    /** The panels for various negotiable data. */
    private StanceTradeItemPanel stancePanel;
    private GoldTradeItemPanel goldOfferPanel, goldDemandPanel;
    private ColonyTradeItemPanel colonyOfferPanel, colonyDemandPanel;
    private GoodsTradeItemPanel goodsOfferPanel, goodsDemandPanel;
    private UnitTradeItemPanel unitOfferPanel, unitDemandPanel;

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
     * @param otherUnit The other <code>Unit</code> at first contact.
     * @param agreement The <code>DiplomaticTrade</code> agreement that
     *     is being negotiated.
     * @param comment An optional <code>StringTemplate</code>
     *     commentary message.
     */
    public DiplomaticTradeDialog(FreeColClient freeColClient, Unit unit,
                                 Settlement settlement, Unit otherUnit,
                                 DiplomaticTrade agreement,
                                 StringTemplate comment) {
        super(freeColClient);

        final Player player = getMyPlayer();
        final Player unitPlayer = unit.getOwner();
        final Player nonUnitPlayer = (settlement != null)
            ? settlement.getOwner()
            : otherUnit.getOwner();
        StringTemplate t;

        this.unit = unit;
        this.settlement = settlement;
        this.otherPlayer = (unitPlayer == player) ? nonUnitPlayer
            : unitPlayer;
        this.agreement = agreement;
        this.comment = comment;

        JLabel header
            = GUI.getDefaultHeader(Messages.message("negotiationDialog.title."
                    + agreement.getContext().getKey()));
        StringTemplate nation = player.getNationName(),
            otherNation = otherPlayer.getNationName();
        t = StringTemplate.template("negotiationDialog.demand")
            .addStringTemplate("%nation%", nation)
            .addStringTemplate("%otherNation%", otherNation);
        this.demandMessage = Messages.message(t);
        t = StringTemplate.template("negotiationDialog.offer")
            .addStringTemplate("%nation%", nation)
            .addStringTemplate("%otherNation%", otherNation);
        this.offerMessage = Messages.message(t);
        t = StringTemplate.template("negotiationDialog.exchange")
            .addStringTemplate("%nation%", unitPlayer.getNationName());
        this.exchangeMessage = Messages.message(t);

        TradeContext context = agreement.getContext();

        NationSummary ns = getController().getNationSummary(otherPlayer);
        int gold = (ns == null
            || ns.getGold() == Player.GOLD_NOT_ACCOUNTED) ? HUGE_DEMAND
            : ns.getGold();
        this.goldDemandPanel = new GoldTradeItemPanel(otherPlayer, gold);

        gold = (player.getGold() == Player.GOLD_NOT_ACCOUNTED) ? HUGE_DEMAND
            : player.getGold();
        this.goldOfferPanel = new GoldTradeItemPanel(player, gold);

        switch (context) {
        case CONTACT:
            this.stancePanel = new StanceTradeItemPanel(player, otherPlayer);
            break;
        case DIPLOMATIC:
            this.stancePanel = new StanceTradeItemPanel(player, otherPlayer);
            this.colonyDemandPanel = new ColonyTradeItemPanel(otherPlayer);
            this.colonyOfferPanel = new ColonyTradeItemPanel(player);
            this.goodsDemandPanel = this.goodsOfferPanel = null;
            this.unitOfferPanel = this.unitDemandPanel = null;
            break;
        case TRADE:
            this.stancePanel = null;
            this.colonyDemandPanel = this.colonyOfferPanel = null;
            this.goodsDemandPanel = new GoodsTradeItemPanel(otherPlayer,
                                                            getAnyGoods());
            List<Goods> goods = (unitPlayer == player) ? unit.getGoodsList()
                : settlement.getCompactGoods();
            for (Goods g : goods) {
                if (g.getAmount() > GoodsContainer.CARGO_SIZE) {
                    g.setAmount(GoodsContainer.CARGO_SIZE);
                }
            }
            this.goodsOfferPanel = new GoodsTradeItemPanel(player, goods);
            this.unitDemandPanel = new UnitTradeItemPanel(otherPlayer,
                                                          getUnitUnitList(null));
            this.unitOfferPanel = new UnitTradeItemPanel(player,
                ((unitPlayer == player) ? getUnitUnitList(unit)
                    : settlement.getUnitList()));
            break;
        case TRIBUTE:
            this.stancePanel = new StanceTradeItemPanel(player, otherPlayer);
            this.colonyDemandPanel = this.colonyOfferPanel = null;
            this.goodsDemandPanel = this.goodsOfferPanel = null;
            this.unitOfferPanel = this.unitDemandPanel = null;
            break;
        default:
            throw new IllegalStateException("Bogus trade context: " + context);
        }

        this.summary = new MigPanel(new MigLayout("wrap 2", "[20px][]"));
        this.summary.setOpaque(false);
        this.summary.add(GUI.getDefaultTextArea(Messages.message(comment)),
                         "center, span 2");

        MigPanel panel = new MigPanel(new MigLayout("wrap 3",
                "[200, fill][300, fill][200, fill]", ""));
        panel.add(header, "span 3, center");
        panel.add(GUI.getDefaultTextArea(this.demandMessage), "center");
        panel.add(new JLabel(), "center");
        panel.add(GUI.getDefaultTextArea(this.offerMessage), "center");

        panel.add(this.goldDemandPanel);
        panel.add(this.summary, "spany, top");
        panel.add(this.goldOfferPanel);

        if (this.colonyDemandPanel != null) {
            panel.add(this.colonyDemandPanel);
            panel.add(this.colonyOfferPanel);
        }
        if (this.stancePanel != null) {
            panel.add(this.stancePanel, "skip");
        }
        if (this.goodsDemandPanel != null) {
            panel.add(this.goodsDemandPanel);
            panel.add(this.goodsOfferPanel);
        }
        if (this.unitDemandPanel != null) {
            panel.add(this.unitDemandPanel);
            panel.add(this.unitOfferPanel);
        }
        panel.setPreferredSize(panel.getPreferredSize());
        setFocusCycleRoot(true);

        DiplomaticTrade bogus = null;
        String str;
        List<ChoiceItem<DiplomaticTrade>> c = choices();
        if (agreement.getVersion() > 0) { // A new offer can not be accepted
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
            .getImageIcon((settlement != null) ? settlement : otherUnit,
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
     * Get a list of units to offer that are associated with a given unit.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @return A list of <code>Unit</code>s.
     */
    private List<Unit> getUnitUnitList(Unit unit) {
        List<Unit> ul = new ArrayList<Unit>();
        if (unit != null) {
            if (unit.isCarrier()) {
                ul.addAll(unit.getUnitList());
            } else if (unit.isOnCarrier()) {
                ul.addAll(unit.getCarrier().getUnitList());
            } else {
                ul.add(unit);
            }
        }
        return ul;
    }

    /**
     * Update the entire dialog.
     */
    private void updateDialog() {
        if (this.goldOfferPanel != null) {
            this.goldOfferPanel.update(agreement);
        }
        if (this.stancePanel != null) {
            this.stancePanel.update(agreement);
        }
        if (this.colonyOfferPanel != null) {
            this.colonyOfferPanel.update(agreement);
        }
        if (this.colonyDemandPanel != null) {
            this.colonyDemandPanel.update(agreement);
        }
        if (this.goodsOfferPanel != null) {
            this.goodsOfferPanel.update(agreement);
        }
        if (this.goodsDemandPanel != null) {
            this.goodsDemandPanel.update(agreement);
        }
        if (this.unitOfferPanel != null) {
            this.unitOfferPanel.update(agreement);
        }
        if (this.unitDemandPanel != null) {
            this.unitDemandPanel.update(agreement);
        }

        updateSummary();
    }

    /**
     * Gets a trade item button for a given item.
     *
     * @param item The <code>TradeItem</code> to make a button for.
     * @return A new <code>JButton</code> for the item.
     */
    private JButton getTradeItemButton(TradeItem item) {
        String description = Messages.message(item.getDescription());
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

        summary.add(GUI.getDefaultTextArea(Messages.message(comment)),
                    "center, span 2");

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
     * @param source The sourced <code>Player</code>.
     * @param colony The <code>Colony</code> to add.
     */
    public void addColonyTradeItem(Player source, Colony colony) {
        final Player player = getMyPlayer();

        Player destination = (source == otherPlayer) ? player : otherPlayer;
        agreement.add(new ColonyTradeItem(getGame(), source, destination,
                                          colony));
        updateDialog();
    }

    /**
     * Adds a <code>GoldTradeItem</code> to the list of TradeItems.
     *
     * @param source The source <code>Player</code>.
     * @param amount The amount of gold.
     */
    public void addGoldTradeItem(Player source, int amount) {
        final Player player = getMyPlayer();

        Player destination = (source == otherPlayer) ? player : otherPlayer;
        agreement.add(new GoldTradeItem(getGame(), source, destination,
                                        amount));
        updateDialog();
    }

    /**
     * Adds a <code>GoodsTradeItem</code> to the list of TradeItems.
     *
     * @param source The source <code>Player</code>.
     * @param goods The <code>Goods</code> to add.
     */
    public void addGoodsTradeItem(Player source, Goods goods) {
        final Player player = getMyPlayer();

        Player destination = (source == otherPlayer) ? player : otherPlayer;
        agreement.add(new GoodsTradeItem(getGame(), source, destination,
                                         goods, settlement));
        updateDialog();
    }

    /**
     * Trade a stance change between the players.
     *
     * @param stance The <code>Stance</code> to trade.
     */
    public void addStanceTradeItem(Stance stance) {
        final Player player = getMyPlayer();

        agreement.add(new StanceTradeItem(getGame(), otherPlayer, player,
                                          stance));
        updateDialog();
    }

    /**
     * Adds a <code>UnitTradeItem</code> to the list of TradeItems.
     *
     * @param source The source <code>Player</code>.
     * @param unit The <code>Unit</code> to add.
     */
    public void addUnitTradeItem(Player source, Unit unit) {
        final Player player = getMyPlayer();

        Player destination = (source == otherPlayer) ? player : otherPlayer;
        agreement.add(new UnitTradeItem(getGame(), source, destination,
                                        unit));
        updateDialog();
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
        this.unitOfferPanel = this.unitDemandPanel = null;
        this.summary = null;
        this.demandMessage = this.offerMessage = this.exchangeMessage = null;
    }
}
