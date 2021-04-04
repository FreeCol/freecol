/**
 *  Copyright (C) 2002-2021   The FreeCol Team
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

package net.sf.freecol.client.gui.dialog;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ChoiceItem;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.panel.*;
import net.sf.freecol.client.gui.Size;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTradeItem;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.DiplomaticTrade.TradeContext;
import net.sf.freecol.common.model.DiplomaticTrade.TradeStatus;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoldTradeItem;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsLocation;
import net.sf.freecol.common.model.GoodsTradeItem;
import net.sf.freecol.common.model.InciteTradeItem;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.NationSummary;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.model.StanceTradeItem;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.TradeItem;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitTradeItem;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * The panel that allows negotiations between players.
 * <p>
 * Panel Layout:
 * <p>
 * The Negotiation Dialog is laid out in three columns. The outer two columns are 30% of the panel's width while the
 *      center column is 40% the panel's width.
 * <p style="display: block; font-family: monospace; white-space: pre; margin: 1em 0;">
 * --------------------------------------------------------------------------
 * |                         negotiationDialog.title                        |
 * | ---------------------------------------------------------------------- |
 * |       Left Pane      |       Center Pane       |      Right Pane       |
 * |                      |      (centerPanel)      |                       |
 * | ---------------------------------------------------------------------- |
 * |                       [cancelButton][sendButton]                       |
 * --------------------------------------------------------------------------
 * <p>
 * The Left and Right Panes are laid out as follows:
 * <p style="display: block; font-family: monospace; white-space: pre; margin: 1em 0;">
 * | --------------- |
 * | gold            |
 * | --------------- |
 * | colony          |
 * | --------------- |
 * | stance          |
 * | --------------- |
 * | war declaration |
 * | --------------- |
 * TODO: Improve layout description
 */
public final class NegotiationDialog extends FreeColDialog<DiplomaticTrade> {

    private static final Logger logger = Logger.getLogger(NegotiationDialog.class.getName());

    private static final int HUGE_DEMAND = 100000;

    private static final String ADD = "add";
    private static final String CLEAR = "clear";


    private class RemoveAction extends AbstractAction {
        private final TradeItem item;

        public RemoveAction(TradeItem item) {
            this.item = item;
        }

        // Interface ActionListener

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(ActionEvent ae) {
            agreement.remove(item);
            updateDialog();
        }
    }

    private class ColonyTradeItemPanel extends MigPanel
            implements ActionListener {

        private final Player source;
        private final JComboBox<Colony> colonyBox;
        private final JButton clearButton;
        private final JButton addButton;
        private final JLabel label;
        private final List<Colony> allColonies;


        /**
         * Creates a new {@code ColonyTradeItemPanel} instance.
         *
         * @param source The {@code Player} source.
         */
        public ColonyTradeItemPanel(Player source) {
            super(new MigLayout("wrap 1", "", ""));

            this.source = source;
            this.colonyBox = new JComboBox<>();
            this.clearButton = Utility.localizedButton("negotiationDialog.clear");
            this.clearButton.addActionListener(this);
            this.clearButton.setActionCommand(CLEAR);
            this.addButton = Utility.localizedButton("negotiationDialog.add");
            this.addButton.addActionListener(this);
            this.addButton.setActionCommand(ADD);
            this.label = Utility.localizedLabel(Messages.getName("model.tradeItem.colony"));
            this.allColonies = source.getColonyList();

            setBorder(Utility.getSimpleLineBorder());

            add(this.label);
            add(this.colonyBox);
            add(this.clearButton, "split 2");
            add(this.addButton);

            setSize(getPreferredSize());
        }


        /**
         * Update this panel.
         *
         * @param dt The {@code DiplomaticTrade} to update with.
         */
        private void update(DiplomaticTrade dt) {
            if (!source.isEuropean()) return;

            // Remove all action listeners, so the update has no effect (except
            // updating the list).
            ActionListener[] listeners = this.colonyBox.getActionListeners();
            for (ActionListener al : listeners) {
                this.colonyBox.removeActionListener(al);
            }

            List<Colony> available = new ArrayList<>(allColonies);
            for (Colony c : dt.getColoniesGivenBy(source)) {
                if (available.contains(c)) {
                    available.remove(c);
                } else {
                    allColonies.add(c); // did not know about this!
                }
            }

            this.colonyBox.removeAllItems();
            for (Colony c : available) this.colonyBox.addItem(c);

            boolean enable = !available.isEmpty();
            this.clearButton.setEnabled(!enable);
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
        @Override
        public void actionPerformed(ActionEvent ae) {
            final String command = ae.getActionCommand();
            if (null != command) switch (command) {
                case ADD:
                    NegotiationDialog.this.addColonyTradeItem(source,
                            (Colony)colonyBox.getSelectedItem());
                    break;
                case CLEAR:
                    NegotiationDialog.this
                            .removeTradeItems(ColonyTradeItem.class);
                    break;
                default:
                    logger.warning("Bad command: " + command);
                    break;
            }
        }
    }

    private class GoldTradeItemPanel extends MigPanel
            implements ActionListener {

        private final Player source;
        private final JSpinner spinner;


        /**
         * Creates a new {@code GoldTradeItemPanel} instance.
         *
         * @param source The {@code Player} that is trading.
         * @param gold The maximum amount of gold to trade.
         */
        public GoldTradeItemPanel(Player source, int gold) {
            super(new MigLayout("wrap 1", "", ""));

            this.source = source;
            this.spinner = new JSpinner(new SpinnerNumberModel(0, 0, gold, 1));
            JButton clearButton = Utility.localizedButton("negotiationDialog.clear");
            clearButton.addActionListener(this);
            clearButton.setActionCommand(CLEAR);
            JButton addButton = Utility.localizedButton("negotiationDialog.add");
            addButton.addActionListener(this);
            addButton.setActionCommand(ADD);
            // adjust entry size
            ((JSpinner.DefaultEditor)this.spinner.getEditor())
                                                 .getTextField()
                                                 .setColumns(5);

            setBorder(Utility.getSimpleLineBorder());

            add(Utility.localizedLabel(Messages.getName("model.tradeItem.gold")));
            add(Utility.localizedLabel(StringTemplate
                    .template("negotiationDialog.goldAvailable")
                    .addAmount("%amount%", gold)));
            add(this.spinner);
            add(clearButton, "split 2");
            add(addButton);

            setSize(getPreferredSize());
        }


        /**
         * Update this panel.
         *
         * @param dt The {@code DiplomaticTrade} to update with.
         */
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
        @Override
        public void actionPerformed(ActionEvent ae) {
            final String command = ae.getActionCommand();
            if (null != command) switch (command) {
                case ADD:
                    int amount = ((Integer)spinner.getValue());
                    NegotiationDialog.this.addGoldTradeItem(source, amount);
                    break;
                case CLEAR:
                    NegotiationDialog.this.removeTradeItems(GoldTradeItem.class);
                    break;
                default:
                    logger.warning("Bad command: " + command);
                    break;
            }
        }
    }

    private class GoodsTradeItemPanel extends MigPanel
        implements ActionListener {

        private class GoodsBoxRenderer extends JLabel
                implements ListCellRenderer<Goods> {

            /**
             * {@inheritDoc}
             */
            @Override
            public Component getListCellRendererComponent(JList<? extends Goods> list,
                                                          Goods value, int index, boolean isSelected,
                                                          boolean cellHasFocus) {
                setText((value == null) ? ""
                        : Messages.message(value.getLabel(true)));
                return this;
            }
        }

        private final Player source;
        private final JComboBox<Goods> goodsBox;
        private final JButton clearButton;
        private final JButton addButton;
        private final JLabel label;
        private final List<Goods> allGoods;


        /**
         * Creates a new {@code GoodsTradeItemPanel} instance.
         *
         * @param source The {@code Player} nominally in possession of the
         *     goods (this may be totally fictional).
         * @param allGoods The {@code Goods} to trade.
         */
        public GoodsTradeItemPanel(Player source, List<Goods> allGoods) {
            super(new MigLayout("wrap 1", "", ""));

            this.source = source;
            this.goodsBox = new JComboBox<>(new DefaultComboBoxModel<Goods>());
            this.goodsBox.setRenderer(new GoodsBoxRenderer());
            this.clearButton = Utility.localizedButton("negotiationDialog.clear");
            this.clearButton.addActionListener(this);
            this.clearButton.setActionCommand(CLEAR);
            this.addButton = Utility.localizedButton("negotiationDialog.add");
            this.addButton.addActionListener(this);
            this.addButton.setActionCommand(ADD);
            this.label = Utility.localizedLabel(Messages.nameKey("model.tradeItem.goods"));
            this.allGoods = allGoods;

            setBorder(Utility.getSimpleLineBorder());

            add(this.label);
            add(this.goodsBox);
            add(this.clearButton, "split 2");
            add(this.addButton);

            setSize(getPreferredSize());
        }


        /**
         * Update this panel.
         *
         * @param dt The {@code DiplomaticTrade} to update with.
         */
        public void update(DiplomaticTrade dt) {
            // Remove all action listeners, so the update has no
            // effect (except updating the list).
            ActionListener[] listeners = this.goodsBox.getActionListeners();
            for (ActionListener al : listeners) {
                this.goodsBox.removeActionListener(al);
            }

            List<Goods> available = new ArrayList<>(allGoods);
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
            this.clearButton.setEnabled(!enable);
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
        @Override
        public void actionPerformed(ActionEvent ae) {
            final String command = ae.getActionCommand();
            if (null != command) switch (command) {
                case ADD:
                    NegotiationDialog.this.addGoodsTradeItem(source,
                            (Goods)goodsBox.getSelectedItem());
                    break;
                case CLEAR:
                    NegotiationDialog.this
                            .removeTradeItems(GoodsTradeItem.class);
                    break;
                default:
                    logger.warning("Bad command: " + command);
                    break;
            }
        }
    }

    private class InciteTradeItemPanel extends MigPanel
        implements ActionListener {

        private class InciteBoxRenderer extends JLabel
                implements ListCellRenderer<Player> {

            /**
             * {@inheritDoc}
             */
            @Override
            public Component getListCellRendererComponent(JList<? extends Player> list,
                                                          Player value, int index, boolean isSelected,
                                                          boolean cellHasFocus) {
                setText((value == null) ? ""
                        : Messages.message(value.getNationLabel()));
                return this;
            }
        }

        private final Player source;
        private final Player other;
        private final JComboBox<Player> victimBox;
        private final JLabel label;
        private final JButton clearButton;
        private final JButton addButton;
        private final List<Player> available = new ArrayList<>();


        /**
         * Creates a new {@code InciteTradeItemPanel} instance.
         *
         * @param source The {@code Player} that is trading.
         * @param other The {@code Player} negotiated with.
         */
        public InciteTradeItemPanel(Player source, Player other) {
            super(new MigLayout("wrap 1", "", ""));

            this.source = source;
            this.other = other;
            this.victimBox = new JComboBox<>(new DefaultComboBoxModel<Player>());
            this.victimBox.setRenderer(new InciteBoxRenderer());
            this.clearButton = Utility.localizedButton("negotiationDialog.clear");
            this.clearButton.addActionListener(this);
            this.clearButton.setActionCommand(CLEAR);
            this.addButton = Utility.localizedButton("negotiationDialog.add");
            this.addButton.addActionListener(this);
            this.addButton.setActionCommand(ADD);
            this.label = Utility.localizedLabel(Messages.nameKey("model.tradeItem.incite"));

            setBorder(Utility.getSimpleLineBorder());

            available.clear();
            final Predicate<Player> incitablePred = p ->
                    p != this.other && this.source.getStance(p).isIncitable();
            available.addAll(transform(getGame().getLivePlayers(this.source),
                    incitablePred));

            add(this.label);
            add(this.victimBox);
            add(this.clearButton, "split 2");
            add(this.addButton);

            setSize(getPreferredSize());
        }


        /**
         * Update this panel.
         *
         * @param dt The {@code DiplomaticTrade} to update with.
         */
        public void update(DiplomaticTrade dt) {
            // Remove all action listeners, so the update has no
            // effect (except updating the list).
            ActionListener[] listeners = this.victimBox.getActionListeners();
            for (ActionListener al : listeners) {
                this.victimBox.removeActionListener(al);
            }

            this.victimBox.removeAllItems();
            for (Player p : available) victimBox.addItem(p);

            boolean enable = !available.isEmpty();
            this.label.setEnabled(enable);
            this.clearButton.setEnabled(!enable);
            this.addButton.setEnabled(enable);
            this.victimBox.setEnabled(enable);

            for (ActionListener al : listeners) {
                this.victimBox.addActionListener(al);
            }
        }


        // Implement ActionListener

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(ActionEvent ae) {
            final String command = ae.getActionCommand();
            if (null != command) switch (command) {
                case ADD:
                    Player victim = (Player)victimBox.getSelectedItem();
                    if (victim != null) {
                        NegotiationDialog.this
                                .addInciteTradeItem(source, victim);
                    }   break;
                case CLEAR:
                    NegotiationDialog.this
                            .removeTradeItems(InciteTradeItem.class);
                    break;
                default:
                    logger.warning("Bad command: " + command);
                    break;
            }
        }
    }

    /**
     * Class for the stance trade panel.  Access needs to be public so
     * that comboBoxLabel() is externally visible.
     */
    public class StanceTradeItemPanel extends MigPanel
        implements ActionListener {

        private class StanceBoxRenderer extends JLabel
                implements ListCellRenderer<Stance> {

            /**
             * {@inheritDoc}
             */
            @Override
            public Component getListCellRendererComponent(JList<? extends Stance> list,
                                                          Stance value, int index, boolean isSelected,
                                                          boolean cellHasFocus) {
                setText((value == null) ? "" : Messages.getName(value));
                return this;
            }
        }

        private final Player source;
        private final Player target;
        private final JComboBox<Stance> stanceBox;
        private final JButton clearButton;
        private final JButton addButton;


        /**
         * Creates a new {@code StanceTradeItemPanel} instance.
         *
         * @param source The {@code Player} offering the stance change.
         * @param target The {@code Player} to consider the stance change.
         */
        public StanceTradeItemPanel(Player source, Player target) {
            super(new MigLayout("wrap 1", "", ""));

            this.source = source;
            this.target = target;
            this.stanceBox = new JComboBox<>(new DefaultComboBoxModel<Stance>());
            this.stanceBox.setRenderer(new StanceBoxRenderer());
            this.clearButton = Utility.localizedButton("negotiationDialog.clear");
            this.clearButton.addActionListener(this);
            this.clearButton.setActionCommand(CLEAR);
            this.addButton = Utility.localizedButton("negotiationDialog.add");
            this.addButton.addActionListener(this);
            this.addButton.setActionCommand(ADD);

            setBorder(Utility.getSimpleLineBorder());

            add(Utility.localizedLabel(Messages.nameKey("model.tradeItem.stance")));
            add(this.stanceBox);
            add(this.clearButton, "split 2");
            add(this.addButton);
        }


        /**
         * Select the item with a given stance.
         *
         * @param stance The {@code Stance} to select.
         */
        private void setSelectedValue(Stance stance) {
            for (int i = 0; i < stanceBox.getItemCount(); i++) {
                if (stanceBox.getItemAt(i) == stance) {
                    stanceBox.setSelectedItem(i);
                }
            }
        }

        /**
         * Update this panel with a given trade.
         *
         * @param dt The {@code DiplomaticTrade} to update with.
         */
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
            if (stance == Stance.PEACE) {
                stanceBox.addItem(Stance.ALLIANCE);
            }

            Stance select = dt.getStance();
            if (select != null) setSelectedValue(select);
        }


        // Interface ActionListener

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(ActionEvent ae) {
            final String command = ae.getActionCommand();
            if (null != command) switch (command) {
                case ADD:
                    Stance stance = (Stance)stanceBox.getSelectedItem();
                    NegotiationDialog.this.addStanceTradeItem(stance);
                    break;
                case CLEAR:
                    NegotiationDialog.this
                            .removeTradeItems(StanceTradeItem.class);
                    break;
                default:
                    logger.warning("Bad command: " + command);
                    break;
            }
        }
    }

    private class UnitTradeItemPanel extends MigPanel
        implements ActionListener {

        private class UnitBoxRenderer extends JLabel
                implements ListCellRenderer<Unit> {

            /**
             * {@inheritDoc}
             */
            @Override
            public Component getListCellRendererComponent(JList<? extends Unit> list,
                                                          Unit value, int index, boolean isSelected,
                                                          boolean cellHasFocus) {
                setText((value == null) ? "" : value.getDescription());
                return this;
            }
        }

        private final Player source;
        private final JComboBox<Unit> unitBox;
        private final JButton clearButton;
        private final JButton addButton;
        private final JLabel label;
        private final List<Unit> allUnits;


        /**
         * Creates a new {@code UnitTradeItemPanel} instance.
         *
         * @param source The {@code Player} nominally in posession of the
         *     unit (this may be totally fictional).
         * @param allUnits The {@code Unit}s to trade.
         */
        public UnitTradeItemPanel(Player source, List<Unit> allUnits) {
            super(new MigLayout("wrap 1", "", ""));

            this.source = source;
            this.unitBox = new JComboBox<>(new DefaultComboBoxModel<Unit>());
            this.unitBox.setRenderer(new UnitBoxRenderer());
            this.clearButton = Utility.localizedButton("negotiationDialog.clear");
            this.clearButton.addActionListener(this);
            this.clearButton.setActionCommand(CLEAR);
            this.addButton = Utility.localizedButton("negotiationDialog.add");
            this.addButton.addActionListener(this);
            this.addButton.setActionCommand(ADD);
            this.label = Utility.localizedLabel(Messages.nameKey("model.tradeItem.unit"));
            this.allUnits = allUnits;

            setBorder(Utility.getSimpleLineBorder());

            add(this.label);
            add(this.unitBox);
            add(this.clearButton, "split 2");
            add(this.addButton);

            setSize(getPreferredSize());
        }


        /**
         * Update this panel with a given trade.
         *
         * @param dt The {@code DiplomaticTrade} to update with.
         */
        private void update(DiplomaticTrade dt) {
            // Remove all action listeners, so the update has no
            // effect (except updating the list).
            ActionListener[] listeners = unitBox.getActionListeners();
            for (ActionListener al : listeners) {
                unitBox.removeActionListener(al);
            }

            List<Unit> available = new ArrayList<>(allUnits);
            for (Unit u : dt.getUnitsGivenBy(source)) {
                // Remove the ones already on the table
                if (available.contains(u)) {
                    available.remove(u);
                } else {
                    allUnits.add(u); // Did not know about this!
                }
            }

            unitBox.removeAllItems();
            for (Unit u : available) unitBox.addItem(u);

            boolean enable = !available.isEmpty();
            this.label.setEnabled(enable);
            clearButton.setEnabled(!enable);
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
        @Override
        public void actionPerformed(ActionEvent ae) {
            final String command = ae.getActionCommand();
            if (null != command) switch (command) {
                case ADD:
                    NegotiationDialog.this.addUnitTradeItem(source,
                            (Unit)unitBox.getSelectedItem());
                    break;
                case CLEAR:
                    NegotiationDialog.this
                            .removeTradeItems(UnitTradeItem.class);
                    break;
                default:
                    logger.warning("Bad command: " + command);
                    break;
            }
        }
    }


    /** The other player in the negotiation (!= getMyPlayer()). */
    private final Player otherPlayer;

    /** The agreement under negotiation. */
    private final DiplomaticTrade agreement;

    /** A comment message. */
    private final StringTemplate comment;

    /** The panels for various negotiable data. */
    private StanceTradeItemPanel stancePanel;
    private GoldTradeItemPanel goldOfferPanel, goldDemandPanel;
    private ColonyTradeItemPanel colonyOfferPanel, colonyDemandPanel;
    private GoodsTradeItemPanel goodsOfferPanel, goodsDemandPanel;
    private InciteTradeItemPanel inciteOfferPanel, inciteDemandPanel;
    private UnitTradeItemPanel unitOfferPanel, unitDemandPanel;

    /** A panel showing a summary of the current agreement. */
    private JPanel summary;

    /** Useful internal messages. */
    private StringTemplate demand, offer;
    private String exchangeMessage;

    /** Responses. */
    private ChoiceItem<DiplomaticTrade> send = null, accept = null;


    /**
     * Creates a new {@code NegotiationDialog} instance.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param frame The owner frame.
     * @param our Our {@code FreeColGameObject} that is negotiating.
     * @param other The other {@code FreeColGameObject}.
     * @param agreement The {@code DiplomaticTrade} agreement that
     *     is being negotiated.
     * @param comment An optional {@code StringTemplate}
     *     commentary message.
     */
    public NegotiationDialog(FreeColClient freeColClient, JFrame frame,
                             FreeColGameObject our, FreeColGameObject other,
                             DiplomaticTrade agreement, StringTemplate comment) {
        super(freeColClient, frame);

        final Player player = getMyPlayer();
        final Unit ourUnit = (our instanceof Unit) ? (Unit)our : null;
        final Colony ourColony = (our instanceof Colony) ? (Colony)our : null;
        final Unit otherUnit = (other instanceof Unit) ? (Unit)other : null;
        final Colony otherColony = (other instanceof Colony) ? (Colony)other
                : null;

        this.otherPlayer = ((Ownable)other).getOwner();
        this.agreement = agreement;
        this.comment = comment;

        StringTemplate nation = player.getCountryLabel(),
                otherNation = otherPlayer.getCountryLabel();
        this.demand = StringTemplate.template("negotiationDialog.demand")
                .addStringTemplate("%nation%", nation)
                .addStringTemplate("%otherNation%", otherNation);
        this.offer = StringTemplate.template("negotiationDialog.offer")
                .addStringTemplate("%nation%", nation)
                .addStringTemplate("%otherNation%", otherNation);
        this.exchangeMessage = Messages.message("negotiationDialog.exchange");

        NationSummary ns = igc().nationSummary(otherPlayer);
        int gold = (ns == null
                || ns.getGold() == Player.GOLD_NOT_ACCOUNTED) ? HUGE_DEMAND
                : ns.getGold();
        this.goldDemandPanel = new GoldTradeItemPanel(otherPlayer, gold);

        gold = (player.getGold() == Player.GOLD_NOT_ACCOUNTED) ? HUGE_DEMAND
                : player.getGold();
        this.goldOfferPanel = new GoldTradeItemPanel(player, gold);

        StringTemplate tutorial = null;
        TradeContext context = agreement.getContext();
        switch (context) {
            case CONTACT:
                if (freeColClient.tutorialMode()) {
                    tutorial = StringTemplate.key("negotiationDialog.contact.tutorial");
                }
                this.stancePanel = new StanceTradeItemPanel(player, otherPlayer);
                this.inciteOfferPanel = new InciteTradeItemPanel(player, otherPlayer);
                this.inciteDemandPanel = new InciteTradeItemPanel(otherPlayer, player);
                break;
            case DIPLOMATIC:
                this.stancePanel = new StanceTradeItemPanel(player, otherPlayer);
                this.colonyDemandPanel = new ColonyTradeItemPanel(otherPlayer);
                this.colonyOfferPanel = new ColonyTradeItemPanel(player);
                this.goodsDemandPanel = this.goodsOfferPanel = null;
                this.inciteOfferPanel = new InciteTradeItemPanel(player, otherPlayer);
                this.inciteDemandPanel = new InciteTradeItemPanel(otherPlayer, player);
                this.unitOfferPanel = this.unitDemandPanel = null;
                break;
            case TRADE:
                this.stancePanel = null;
                this.colonyDemandPanel = this.colonyOfferPanel = null;
                List<Goods> goods = getAnyGoods();
                this.goodsDemandPanel = new GoodsTradeItemPanel(otherPlayer, goods);
                GoodsLocation gl = (ourUnit != null) ? ourUnit : ourColony;
                goods = (ourUnit != null) ? ourUnit.getGoodsList()
                        : ourColony.getCompactGoodsList();
                for (Goods g : goods) {
                    if (g.getAmount() > GoodsContainer.CARGO_SIZE) {
                        g.setAmount(GoodsContainer.CARGO_SIZE);
                    }
                    g.setLocation(gl);
                }
                this.goodsOfferPanel = new GoodsTradeItemPanel(player, goods);
                this.inciteOfferPanel = this.inciteDemandPanel = null;
                this.unitDemandPanel = new UnitTradeItemPanel(otherPlayer,
                        getUnitUnitList(null));
                this.unitOfferPanel = new UnitTradeItemPanel(player,
                        ((ourUnit != null) ? getUnitUnitList(ourUnit)
                                : ourColony.getUnitList()));
                break;
            case TRIBUTE:
                this.stancePanel = new StanceTradeItemPanel(player, otherPlayer);
                this.colonyDemandPanel = this.colonyOfferPanel = null;
                this.goodsDemandPanel = this.goodsOfferPanel = null;
                this.inciteOfferPanel = new InciteTradeItemPanel(player, otherPlayer);
                this.inciteDemandPanel = new InciteTradeItemPanel(otherPlayer, player);
                this.unitOfferPanel = this.unitDemandPanel = null;
                break;
            default:
                throw new IllegalStateException("Bogus trade context: " + context);
        }

        this.summary = new MigPanel(new MigLayout("wrap 2", "[20px:n:n][]"));
        this.summary.setOpaque(false);
        this.summary.add(Utility.localizedTextArea(comment), "center, span 2");
        /**
         * Build Layout of Diplomatic Trade Dialog
         */
        JPanel panel = new MigPanel(new MigLayout("wrap 3",
                                                  "[30%|40%|30%]", ""));
        // Main Panel Header
        panel.add(Utility.localizedHeader("negotiationDialog.title."
                                          + agreement.getContext().getKey(),
                                          Utility.FONTSPEC_TITLE),
                "span 3, center");

        // Panel contents Header row
        JTextArea labelDemandMessage = Utility.localizedTextArea(this.demand);
        Font font = FontLibrary.getUnscaledFont("normal-bold-tiny");
        labelDemandMessage.setFont(font);
        panel.add(labelDemandMessage);
        JTextArea blank = new JTextArea(" ");
        blank.setVisible(false);
        panel.add(blank, "");
        JTextArea labelOfferMessage = Utility.localizedTextArea(this.offer);
        labelOfferMessage.setFont(font);
        panel.add(labelOfferMessage);

        // Panel contents
        // TODO: Expand center panel so that contents fill cell horizontally. 
        panel.add(this.goldDemandPanel); // Left pane
        JPanel centerPanel = new MigPanel(new MigLayout("wrap 1"));
        centerPanel.setMinimumSize(new Dimension(250, 50));
        if (tutorial != null) {
            // Display only if tutorial variable contents overriden
            //      Can only occur if: First Contact with a forgeign Nation
            JTextArea tutArea = Utility.localizedTextArea(tutorial, 30);
            centerPanel.add(tutArea, "center");
        }
        JScrollPane scroll = new JScrollPane(this.summary,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        centerPanel.add(scroll, "top, width 100%");
        panel.add(centerPanel, "spany, top"); // Center pane
        panel.add(this.goldOfferPanel); // Right pane

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
        if (this.inciteDemandPanel != null) {
            panel.add(this.inciteDemandPanel);
            panel.add(this.inciteOfferPanel);
        }
        if (this.unitDemandPanel != null) {
            panel.add(this.unitDemandPanel);
            panel.add(this.unitOfferPanel);
        }
        if (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.MENUS)) {
            panel.add(new JLabel("Version = " + agreement.getVersion()));
        }

        String str;
        List<ChoiceItem<DiplomaticTrade>> c = choices();
        if (agreement.getVersion() > 0) { // A new offer can not be accepted
            str = Messages.message("negotiationDialog.accept");
            c.add(this.accept = new ChoiceItem<>(str, (DiplomaticTrade)null));
        }
        str = Messages.message("negotiationDialog.send");
        c.add(this.send = new ChoiceItem<>(str,
                (DiplomaticTrade)null).okOption());
        if (agreement.getVersion() > 0 || context != TradeContext.CONTACT) {
            str = Messages.message("negotiationDialog.cancel");
            c.add(new ChoiceItem<>(str,
                    (DiplomaticTrade)null).cancelOption().defaultOption());
        }
        updateDialog();

        ImageIcon icon = new ImageIcon((otherColony != null)
                ? getImageLibrary().getScaledSettlementImage(otherColony)
                : getImageLibrary().getScaledUnitImage(otherUnit));
        initializeDialog(frame, DialogType.QUESTION, true, panel, icon, c);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public DiplomaticTrade getResponse() {
        Object value = getValue();
        TradeStatus s = (value == null) ? TradeStatus.REJECT_TRADE
                : (value == this.accept) ? TradeStatus.ACCEPT_TRADE
                : (value == this.send) ? TradeStatus.PROPOSE_TRADE
                : TradeStatus.REJECT_TRADE;
        agreement.setStatus(s);
        return agreement;
    }

    /**
     * Gets a list of all possible storable goods (one cargo load).
     * Note that these goods are fictional.  They are the goods that
     * *might* be in the other player's store.  Therefore they have a
     * bogus location (i.e. not the actual goods container).
     *
     * @return A list of storable {@code Goods}.
     */
    private List<Goods> getAnyGoods() {
        final Game game = getGame();
        final Specification spec = getSpecification();
        return transform(spec.getStorableGoodsTypeList(), alwaysTrue(),
                gt -> new Goods(game, null, gt, GoodsContainer.CARGO_SIZE));
    }

    /**
     * Get a list of units to offer that are associated with a given unit.
     *
     * @param unit The {@code Unit} that is trading.
     * @return A list of {@code Unit}s.
     */
    private List<Unit> getUnitUnitList(Unit unit) {
        List<Unit> ul = new ArrayList<>();
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
        if (this.inciteOfferPanel != null) {
            this.inciteOfferPanel.update(agreement);
        }
        if (this.inciteDemandPanel != null) {
            this.inciteDemandPanel.update(agreement);
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
     * @param item The {@code TradeItem} to make a button for.
     * @param saleDir Boolean to indicate the EU price for sale (T) or buy (F)
     * @return A new {@code JButton} for the item.
     */
    private JButton getTradeItemButton(TradeItem item, boolean saleDir) {
        
        Market market = getMyPlayer().getMarket();
        JButton button = new JButton(new RemoveAction(item));
        
        // Checks if the items are goods
        if (item.getGoods() != null) {
            int buyPriceTotal = market.getBidPrice(item.getGoods().getType(), item.getGoods().getAmount());
            int salePriceTotal = market.getSalePrice(item.getGoods().getType(), item.getGoods().getAmount());
            
            // Depending on saleDir, creates a button for goods w/ EU buy or sale price
            if (saleDir) {
                button.setText(Messages.message(item.getLabel()) + " " +
                        Messages.message(StringTemplate
                                .template("negotiationDialog.euSalePrice")
                                .addAmount("%priceTotal%", salePriceTotal)));
            } else {
                button.setText(Messages.message(item.getLabel()) + " " +
                        Messages.message(StringTemplate
                                .template("negotiationDialog.euBuyPrice")
                                .addAmount("%priceTotal%", buyPriceTotal)));
            }
        } else {
            // If not goods, follow protocol
            button.setText(Messages.message(item.getLabel()));
        }
        
        button.setMargin(Utility.EMPTY_MARGIN);
        button.setOpaque(false);
        button.setForeground(Utility.getLinkColor());
        button.setBorder(Utility.blankBorder(0, 0, 0, 0));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    /**
     * Update the text summary of the proposed transaction.
     */
    private void updateSummary() {
        final Player player = getMyPlayer();

        summary.removeAll();

        summary.add(Utility.localizedTextArea(comment), "center, span 2");

        List<TradeItem> offers = agreement.getItemsGivenBy(player);
        if (!offers.isEmpty()) {
            summary.add(Utility.localizedLabel(this.offer), "span");
            for (TradeItem item : offers) {
                summary.add(getTradeItemButton(item, true), "skip");
            }
        }

        List<TradeItem> demands = agreement.getItemsGivenBy(otherPlayer);
        if (!demands.isEmpty()) {
            if (offers.isEmpty()) {
                summary.add(Utility.localizedLabel(this.demand), "span");
            } else {
                summary.add(new JLabel(exchangeMessage), "newline 20, span");
            }
            for (TradeItem item : demands) {
                summary.add(getTradeItemButton(item, false), "skip");
            }
        }
    }


    /**
     * Remove trade items of a given type.
     *
     * @param itemClass The class of {@code TradeItem} to remove.
     */
    public void removeTradeItems(Class<? extends TradeItem> itemClass) {
        this.agreement.removeType(itemClass);
        updateDialog();
    }

    /**
     * Adds a {@code ColonyTradeItem} to the list of TradeItems.
     *
     * @param source The sourced {@code Player}.
     * @param colony The {@code Colony} to add.
     */
    public void addColonyTradeItem(Player source, Colony colony) {
        final Player player = getMyPlayer();

        Player destination = (source == otherPlayer) ? player : otherPlayer;
        agreement.add(new ColonyTradeItem(getGame(), source, destination, colony));
        updateDialog();
    }

    /**
     * Adds a {@code GoldTradeItem} to the list of TradeItems.
     *
     * @param source The source {@code Player}.
     * @param amount The amount of gold.
     */
    public void addGoldTradeItem(Player source, int amount) {
        final Player player = getMyPlayer();

        Player destination = (source == otherPlayer) ? player : otherPlayer;
        agreement.add(new GoldTradeItem(getGame(), source, destination, amount));
        updateDialog();
    }

    /**
     * Adds a {@code GoodsTradeItem} to the list of TradeItems.
     *
     * @param source The source {@code Player}.
     * @param goods The {@code Goods} to add.
     */
    public void addGoodsTradeItem(Player source, Goods goods) {
        final Player player = getMyPlayer();

        Player destination = (source == otherPlayer) ? player : otherPlayer;
        agreement.add(new GoodsTradeItem(getGame(), source, destination, goods));
        updateDialog();
    }

    /**
     * Add an {@code InciteTradeItem} to the list of trade items.
     *
     * @param source The source {@code Player}.
     * @param victim The {@code Player} to be attacked.
     */
    public void addInciteTradeItem(Player source, Player victim) {
        final Player player = getMyPlayer();

        Player destination = (source == otherPlayer) ? player : otherPlayer;
        agreement.add(new InciteTradeItem(getGame(), source, destination, victim));
        updateDialog();
    }

    /**
     * Trade a stance change between the players.
     *
     * @param stance The {@code Stance} to trade.
     */
    public void addStanceTradeItem(Stance stance) {
        final Player player = getMyPlayer();

        agreement.add(new StanceTradeItem(getGame(), otherPlayer, player, stance));
        updateDialog();
    }

    /**
     * Adds a {@code UnitTradeItem} to the list of TradeItems.
     *
     * @param source The source {@code Player}.
     * @param unit The {@code Unit} to add.
     */
    public void addUnitTradeItem(Player source, Unit unit) {
        final Player player = getMyPlayer();

        Player destination = (source == otherPlayer) ? player : otherPlayer;
        agreement.add(new UnitTradeItem(getGame(), source, destination, unit));
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
        this.inciteOfferPanel = this.inciteDemandPanel = null;
        this.unitOfferPanel = this.unitDemandPanel = null;
        this.summary = null;
        this.demand = this.offer = null;
        this.exchangeMessage = null;
    }
}
