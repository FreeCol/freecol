/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.ComponentInputMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.HighSeas;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.TransactionListener;
import net.sf.freecol.common.model.Unit;


/**
 * This is a panel for the Europe display. It shows the ships in Europe and
 * allows the user to send them back.
 */
public final class EuropePanel extends FreeColPanel {

    private static Logger logger = Logger.getLogger(EuropePanel.class.getName());

    public static enum EuropeAction {
        EXIT,
        RECRUIT,
        PURCHASE,
        TRAIN,
        UNLOAD,
        SAIL
    }

    private final DestinationPanel toAmericaPanel;

    private final DestinationPanel toEuropePanel;

    private final InPortPanel inPortPanel;

    private final DocksPanel docksPanel;

    private final EuropeCargoPanel cargoPanel;

    private final MarketPanel marketPanel;

    private final TransactionLog log;

    private final DefaultTransferHandler defaultTransferHandler;

    private final MouseListener pressListener;

    private Europe europe;

    private UnitLabel selectedUnitLabel;

    private JButton exitButton;

    private JLabel header = getDefaultHeader("");


    /**
     * The constructor for a EuropePanel.
     * @param freeColClient 
     *
     * @param parent The parent of this panel
     */
    public EuropePanel(FreeColClient freeColClient, Canvas parent) {
        super(freeColClient, parent);

        setFocusCycleRoot(true);

        // Use ESCAPE for closing the ColonyPanel:
        exitButton = new EuropeButton(Messages.message("close"),
                                      KeyEvent.VK_ESCAPE, EuropeAction.EXIT.toString(), this);
        EuropeButton trainButton = new EuropeButton(Messages.message("train"),
                                                    KeyEvent.VK_T, EuropeAction.TRAIN.toString(), this);
        EuropeButton purchaseButton = new EuropeButton(Messages.message("purchase"),
                                                       KeyEvent.VK_P, EuropeAction.PURCHASE.toString(), this);
        EuropeButton recruitButton = new EuropeButton(Messages.message("recruit"),
                                                      KeyEvent.VK_R, EuropeAction.RECRUIT.toString(), this);
        EuropeButton unloadButton = new EuropeButton(Messages.message("unload"),
                                                     KeyEvent.VK_U, EuropeAction.UNLOAD.toString(), this);
        EuropeButton sailButton = new EuropeButton(Messages.message("sail"),
                                                   KeyEvent.VK_S, EuropeAction.SAIL.toString(), this);

        toAmericaPanel = new DestinationPanel();
        toEuropePanel = new DestinationPanel();
        inPortPanel = new InPortPanel();
        cargoPanel = new EuropeCargoPanel(freeColClient, parent);
        cargoPanel.setParentPanel(this);
        docksPanel = new DocksPanel();
        marketPanel = new MarketPanel(this);

        log = new TransactionLog();
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setAlignment(attributes, StyleConstants.ALIGN_RIGHT);
        //StyleConstants.setForeground(attributes, Color.WHITE);
        StyleConstants.setBold(attributes, true);
        log.setParagraphAttributes(attributes, true);

        defaultTransferHandler = new DefaultTransferHandler(freeColClient, parent, this);
        toAmericaPanel.setTransferHandler(defaultTransferHandler);
        toEuropePanel.setTransferHandler(defaultTransferHandler);
        inPortPanel.setTransferHandler(defaultTransferHandler);
        cargoPanel.setTransferHandler(defaultTransferHandler);
        docksPanel.setTransferHandler(defaultTransferHandler);
        marketPanel.setTransferHandler(defaultTransferHandler);

        pressListener = new DragListener(freeColClient, this);
        MouseListener releaseListener = new DropListener();
        toAmericaPanel.addMouseListener(releaseListener);
        toEuropePanel.addMouseListener(releaseListener);
        inPortPanel.addMouseListener(releaseListener);
        cargoPanel.addMouseListener(releaseListener);
        docksPanel.addMouseListener(releaseListener);
        marketPanel.addMouseListener(releaseListener);

        toAmericaPanel.setLayout(new GridLayout(1, 0));
        toEuropePanel.setLayout(new GridLayout(1, 0));
        inPortPanel.setLayout(new GridLayout(1, 0));
        cargoPanel.setLayout(new GridLayout(1, 0));
        docksPanel.setLayout(new GridLayout(0, 5));

        JScrollPane toAmericaScroll = new JScrollPane(toAmericaPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        toAmericaScroll.getVerticalScrollBar().setUnitIncrement(16);
        JScrollPane toEuropeScroll = new JScrollPane(toEuropePanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        toEuropeScroll.getVerticalScrollBar().setUnitIncrement(16);
        JScrollPane inPortScroll = new JScrollPane(inPortPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        inPortScroll.getVerticalScrollBar().setUnitIncrement(16);
        JScrollPane cargoScroll = new JScrollPane(cargoPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        cargoScroll.getVerticalScrollBar().setUnitIncrement(16);
        JScrollPane docksScroll = new JScrollPane(docksPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        docksScroll.getVerticalScrollBar().setUnitIncrement(16);
        JScrollPane marketScroll = new JScrollPane(marketPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JScrollPane logScroll = new JScrollPane(log, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        logScroll.getVerticalScrollBar().setUnitIncrement(16);

        toAmericaPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), Messages
                .message("goingToAmerica")));
        toEuropePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), Messages
                .message("goingToEurope")));
        docksPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), Messages
                .message("docks")));
        inPortPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), Messages
                .message("inPort")));
        marketPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        log.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                                                       Messages.message("sales")));

        toAmericaScroll.getViewport().setOpaque(false);
        toAmericaPanel.setOpaque(false);
        toEuropeScroll.getViewport().setOpaque(false);
        toEuropePanel.setOpaque(false);
        inPortScroll.getViewport().setOpaque(false);
        inPortPanel.setOpaque(false);
        cargoScroll.getViewport().setOpaque(false);
        cargoPanel.setOpaque(false);
        docksScroll.getViewport().setOpaque(false);
        docksPanel.setOpaque(false);
        marketScroll.getViewport().setOpaque(false);
        marketPanel.setOpaque(false);
        logScroll.getViewport().setOpaque(false);
        log.setOpaque(false);

        setLayout(new MigLayout("wrap 3, insets 20, fill",
                                "[380:][380:][150:200:]"));

        if (parent.getHeight() > 750) {
            add(header, "span, center");
        }
        add(toAmericaScroll, "sg, height 124:, grow");
        add(toEuropeScroll, "sg, height 124:, grow");
        add(logScroll, "spany 3, grow");
        add(inPortScroll, "sg, height 124:, grow");
        add(docksScroll, "spany 2, grow");
        add(cargoScroll, "height 100:, grow");
        add(marketScroll, "span, grow");

        add(recruitButton, "span, split 6");
        add(purchaseButton);
        add(trainButton);
        add(unloadButton);
        add(sailButton);
        add(exitButton, "tag ok");

        selectedUnitLabel = null;

        // See the message of Ulf Onnen for more information about the presence
        // of this fake mouse listener.
        addMouseListener(new MouseAdapter() {});

        restoreSavedSize(1000, parent.getHeight() > 750 ? 700 : 600);
    }

    /**
     * What to do when requesting focus.
     */
    public void requestFocus() {
        exitButton.requestFocus();
    }

    /**
     * Refreshes this panel.
     */
    public void refresh() {
        repaint(0, 0, getWidth(), getHeight());
    }

    /**
     * Gets the cargo panel.
     *
     * @return The cargo panel.
     */
    public final CargoPanel getCargoPanel() {
        return cargoPanel;
    }

    /**
     * Returns the currently select unit.
     *
     * @return The currently select unit.
     */
    public Unit getSelectedUnit() {
        return (selectedUnitLabel == null) ? null
            : selectedUnitLabel.getUnit();
    }

    /**
     * Selects a unit that is potentially located somewhere in port.
     *
     * @param unit The <code>Unit</code> to select.
     */
    public void setSelectedUnit(Unit unit) {
        UnitLabel unitLabel = null;

        if (unit != null) {
            Component[] components = inPortPanel.getComponents();
            for (int i = 0; i < components.length; i++) {
                if (components[i] instanceof UnitLabel
                    && ((UnitLabel) components[i]).getUnit() == unit) {
                    unitLabel = (UnitLabel) components[i];
                    break;
                }
            }
        }

        setSelectedUnitLabel(unitLabel);
    }

    /**
     * Returns the currently select unit label.
     *
     * @return The currently select unit label.
     */
    public UnitLabel getSelectedUnitLabel() {
        return selectedUnitLabel;
    }

    /**
     * Selects a unit that is located somewhere on this panel.
     *
     * @param unitLabel The <code>UnitLabel</code> for the unit that
     *     is being selected.
     */
    public void setSelectedUnitLabel(UnitLabel unitLabel) {
        if (selectedUnitLabel != unitLabel) {
            if (selectedUnitLabel != null) {
                selectedUnitLabel.setSelected(false);
            }
            selectedUnitLabel = unitLabel;
            if (unitLabel == null) {
                cargoPanel.setCarrier(null);
            } else {
                cargoPanel.setCarrier(unitLabel.getUnit());
                unitLabel.setSelected(true);
            }
        }
        inPortPanel.revalidate();
        inPortPanel.repaint();
    }

    /**
     * Initialize this EuropePanel.
     *
     * @param europe The <code>Europe</code> this panel should display.
     * @param game The <code>Game</code> the <code>Europe</code> is in.
     */
    public void initialize(Europe europe, Game game) {
        this.europe = europe;
        header.setText(Messages.message(europe.getNameKey()));

        // Initialize the subpanels.
        toAmericaPanel.initialize(getGame().getMap());
        toEuropePanel.initialize(getMyPlayer().getEurope());
        // Initialize cargoPanel before inPortPanel calls setSelectedUnit().
        cargoPanel.initialize();
        inPortPanel.initialize();
        marketPanel.initialize();
        docksPanel.initialize();
        log.initialize();

    }

    /**
     * Cleans up this EuropePanel.
     */
    public void cleanup() {
        log.cleanup();
        docksPanel.cleanup();
        marketPanel.cleanup();
        inPortPanel.cleanup();
        cargoPanel.cleanup();
        toEuropePanel.cleanup();
        toAmericaPanel.cleanup();
    }

    /**
     * Exits this EuropePanel.
     */
    private void exitAction() {
        cleanup();
        getCanvas().remove(this);
        getController().nextModelMessage();
    }

    /**
     * Unload the contents of the currently selected carrier.
     */
    private void unloadAction() {
        Unit unit = getSelectedUnit();
        if (unit != null && unit.isCarrier()) {
            Iterator<Goods> goodsIterator = unit.getGoodsIterator();
            while (goodsIterator.hasNext()) {
                Goods goods = goodsIterator.next();
                if (getMyPlayer().canTrade(goods)) {
                    getController().sellGoods(goods);
                } else {
                    getController().payArrears(goods.getType());
                }
            }
            Iterator<Unit> unitIterator = unit.getUnitIterator();
            while (unitIterator.hasNext()) {
                Unit newUnit = unitIterator.next();
                getController().leaveShip(newUnit);
            }
            cargoPanel.update();
            docksPanel.update();
        }
        requestFocus();
    }

    /**
     * A unit sets sail for the new world.
     */
    private void sailAction() {
        Unit unit = getSelectedUnit();
        if (unit != null && unit.isNaval()) {
            UnitLabel unitLabel = getSelectedUnitLabel();
            toAmericaPanel.add(unitLabel, true);
        }
        requestFocus();
    }

    /**
     * Analyzes an event and calls the right external methods to take care of
     * the user's request.
     *
     * @param event The incoming action event
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        // Close any open Europe Dialog, and show new one if required
        EuropeAction act = EuropeAction.valueOf(command);
        getCanvas().showEuropeDialog(act);
        switch (act) {
        case EXIT:
            exitAction();
            break;
        case RECRUIT: case PURCHASE: case TRAIN:
            requestFocus(); // handled by docks panel
            break;
        case UNLOAD:
            unloadAction();
            break;
        case SAIL:
            sailAction();
            break;
        default:
            logger.warning("Invalid action command: " + command);
        }
    }


    /**
     * Trivial wrapper for CargoPanel.
     * TODO: check if still needed?
     */
    public final class EuropeCargoPanel extends CargoPanel {

        public EuropeCargoPanel(FreeColClient freeColClient, Canvas canvas) {
            super(freeColClient, canvas, true);
        }

    }

    /**
     * A panel that holds UnitsLabels that represent Units that are going to
     * America or Europe.
     */
    public final class DestinationPanel extends JPanel {

        private Location destination;

        /**
         * Initialize this DestinationPanel.
         */
        public void initialize(Location destination) {
            this.destination = destination;
            update();
        }

        /**
         * Cleans up this DestinationPanel.
         */
        public void cleanup() {}

        /**
         * Update this DestinationPanel.
         */
        public void update() {
            removeAll();

            HighSeas highSeas = getMyPlayer().getHighSeas();
            if (highSeas != null) {
                for (Unit unit : highSeas.getUnitList()) {
                    boolean belongs;
                    if (destination instanceof Europe) {
                        belongs = unit.getDestination() == destination;
                    } else if (destination instanceof Map) {
                        belongs = unit.getDestination() == destination
                            || (unit.getDestination() != null
                                && unit.getDestination().getTile() != null
                                && unit.getDestination().getTile().getMap()
                                == destination);
                    } else {
                        logger.warning("Bogus DestinationPanel location: "
                            + ((FreeColGameObject) destination)
                            + " for unit: " + unit);
                        belongs = false;
                    }
                    if (belongs) {
                        UnitLabel unitLabel = new UnitLabel(getFreeColClient(), unit, getCanvas());
                        unitLabel.setTransferHandler(defaultTransferHandler);
                        unitLabel.addMouseListener(pressListener);
                        add(unitLabel);
                    }
                }
            }

            StringTemplate t = StringTemplate.template("sailingTo")
                .addStringTemplate("%location%",
                    destination.getLocationNameFor(getMyPlayer()));
            ((TitledBorder) getBorder()).setTitle(Messages.message(t));
            revalidate();
        }

        /**
         * Adds a component to this DestinationPanel and makes sure that
         * the unit that the component represents gets modified so
         * that it will sail to America.
         *
         * @param comp The component to add.
         * @param editState Must be set to 'true' if the state of the component
         *            that is added (which should be a dropped component
         *            representing a Unit) should be changed so that the
         *            underlying unit is now sailing to America.
         * @return The component argument.
         */
        public Component add(Component comp, boolean editState) {
            if (editState) {
                if (!(comp instanceof UnitLabel)) {
                    logger.warning("Invalid component dropped on this DestinationPanel.");
                    return null;
                }
                final Canvas canvas = getCanvas();
                final Unit unit = ((UnitLabel) comp).getUnit();

                Location dest = destination;
                if (unit.isInEurope()) {
                    dest = canvas.showSelectDestinationDialog(unit);
                    if (dest == null) return null; // user aborted
                }

                final ClientOptions co = getClientOptions();
                if (!co.getBoolean(ClientOptions.AUTOLOAD_EMIGRANTS)
                    && unit.isInEurope()
                    && !(destination instanceof Europe)
                    && docksPanel.getComponentCount() > 0
                    && unit.getSpaceLeft() > 0) {
                    boolean leave = canvas.showConfirmDialog(null,
                        StringTemplate.template("europe.leaveColonists")
                            .addStringTemplate("%newWorld%",
                                destination.getLocationNameFor(unit.getOwner())),
                        "yes", "no");
                    if (!leave) return null; // Colonists remain in Europe.
                }

                comp.getParent().remove(comp);
                getController().moveTo(unit, dest);
                inPortPanel.update();
                docksPanel.update();
            }

            Component c = add(comp);
            revalidate();
            EuropePanel.this.refresh();
            return c;
        }
    }


    /**
     * A panel that holds UnitLabels that represent naval units that are
     * waiting in Europe.
     */
    public final class InPortPanel extends JPanel
        implements PropertyChangeListener {

        /**
         * Initialize this InPortPanel.
         */
        public void initialize() {
            addPropertyChangeListeners();
            update();
        }

        /**
         * Cleans up this InPortPanel.
         */
        public void cleanup() {
            removePropertyChangeListeners();
        }

        /**
         * Update this InPortPanel.
         */
        public void update() {
            removeAll();

            UnitLabel lastCarrier = null;
            UnitLabel prevCarrier = null;
            for (Unit unit : europe.getUnitList()) {
                if (unit.isNaval()
                    && (unit.getState() == Unit.UnitState.ACTIVE
                        || unit.getState() == Unit.UnitState.SENTRY)) {
                    UnitLabel unitLabel = new UnitLabel(getFreeColClient(), unit, getCanvas());
                    unitLabel.setTransferHandler(defaultTransferHandler);
                    unitLabel.addMouseListener(pressListener);
                    add(unitLabel);

                    lastCarrier = unitLabel;
                    if (getSelectedUnit() == unit) prevCarrier = unitLabel;
                }
            }

            // Keep the previous selected unit if possible, otherwise default
            // on the last carrier.
            setSelectedUnitLabel((prevCarrier != null) ? prevCarrier
                                 : (lastCarrier != null) ? lastCarrier
                                 : null);
            // No revalidate+repaint as this is done in setSelectedUnitLabel
        }

        public void addPropertyChangeListeners() {
            europe.addPropertyChangeListener(this);
        }

        public void removePropertyChangeListeners() {
            europe.removePropertyChangeListener(this);
        }

        public void propertyChange(PropertyChangeEvent event) {
            logger.finest("Europe-port change " + event.getPropertyName()
                          + ": " + event.getOldValue()
                          + " -> " + event.getNewValue());
            update();
        }
    }

    /**
     * A panel that holds UnitsLabels that represent Units that are
     * waiting on the docks in Europe.
     */
    public final class DocksPanel extends JPanel
        implements PropertyChangeListener {

        /**
         * Initializes this DocksPanel.
         */
        public void initialize() {
            setLayout(new MigLayout("wrap 6"));
            addPropertyChangeListeners();
            update();
        }

        /**
         * Cleans up this DocksPanel.
         */
        public void cleanup() {
            removePropertyChangeListeners();
        }

        /**
         * Update this DocksPanel.
         */
        public void update() {
            removeAll();

            List<Unit> units = europe.getUnitList();
            for (Unit unit : units) {
                if (!unit.isNaval()) {
                    UnitLabel unitLabel = new UnitLabel(getFreeColClient(), unit, getCanvas());
                    unitLabel.setTransferHandler(defaultTransferHandler);
                    unitLabel.addMouseListener(pressListener);
                    add(unitLabel);
                }
            }

            revalidate();
            repaint();
        }

        public Component add(Component comp, boolean editState) {
            Component c = add(comp);
            update();
            return c;
        }

        @Override
        public void remove(Component comp) {
            update();
        }

        public void addPropertyChangeListeners() {
            europe.addPropertyChangeListener(this);
        }

        public void removePropertyChangeListeners() {
            europe.removePropertyChangeListener(this);
        }

        public void propertyChange(PropertyChangeEvent event) {
            logger.finest("Europe-docks change " + event.getPropertyName()
                          + ": " + event.getOldValue()
                          + " -> " + event.getNewValue());
            update();
        }
    }

    /**
     * A panel that shows goods available for purchase in Europe.
     */
    public final class MarketPanel extends JPanel {

        private final EuropePanel europePanel;

        /**
         * Creates this MarketPanel.
         *
         * @param europePanel The panel that holds this CargoPanel.
         */
        public MarketPanel(EuropePanel europePanel) {
            this.europePanel = europePanel;
            setLayout(new GridLayout(2, 8));
        }

        /**
         * Initialize this MarketPanel.
         */
        public void initialize() {
            removeAll();

            List<GoodsType> goodsTypes = getSpecification().getGoodsTypeList();
            Market market = getMyPlayer().getMarket();
            for (GoodsType goodsType : goodsTypes) {
                if (goodsType.isStorable()) {
                    MarketLabel marketLabel
                        = new MarketLabel(goodsType, market, getCanvas());
                    marketLabel.setTransferHandler(defaultTransferHandler);
                    marketLabel.addMouseListener(pressListener);
                    add(marketLabel);
                }
            }
        }

        /**
         * Cleans up this MarketPanel.
         */
        public void cleanup() {}

        /**
         * If a GoodsLabel is dropped here, sell the goods.
         *
         * @param comp The component to add to this MarketPanel.
         * @param editState Must be set to 'true' if the state of the component
         *            that is added (which should be a dropped component
         *            representing goods) should be sold.
         * @return The component argument.
         */
        public Component add(Component comp, boolean editState) {
            if (editState) {
                if (!(comp instanceof GoodsLabel)) {
                    logger.warning("Invalid component dropped on this MarketPanel.");
                    return null;
                }

                Goods goods = ((GoodsLabel) comp).getGoods();
                if (getMyPlayer().canTrade(goods)) {
                    getController().sellGoods(goods);
                } else {
                    switch (getCanvas().showBoycottedGoodsDialog(goods, europe)) {
                    case PAY_ARREARS:
                        getController().payArrears(goods.getType());
                        break;
                    case DUMP_CARGO:
                        getController().unloadCargo(goods, true);
                        break;
                    case CANCEL:
                    default:
                        break;
                    }
                }
                cargoPanel.revalidate();
                revalidate();
                getController().nextModelMessage();
            }
            europePanel.refresh();
            return comp;
        }

        public void remove(Component comp) {
            // Don't remove market labels.
        }
    }

    /**
     * To log transactions made in Europe
     */
    public class TransactionLog extends JTextPane
        implements TransactionListener {

        public TransactionLog() {
            setEditable(false);
        }

        /**
         * Initializes this TransactionLog.
         */
        public void initialize() {
            getMyPlayer().getMarket().addTransactionListener(this);
            setText("");
        }

        /**
         * Cleans up this TransactionLog.
         */
        public void cleanup() {
            getMyPlayer().getMarket().removeTransactionListener(this);
        }

        public void logPurchase(GoodsType goodsType, int amount, int price) {
            int total = amount * price;
            String text = Messages.message(StringTemplate.template("transaction.purchase")
                                           .add("%goods%", goodsType.getNameKey())
                                           .addAmount("%amount%", amount)
                                           .addAmount("%gold%", price))
                + "\n" + Messages.message(StringTemplate.template("transaction.price")
                                          .addAmount("%gold%", total));
            add(text);
        }

        public void logSale(GoodsType goodsType, int amount, int price, int tax) {
            int totalBeforeTax = amount * price;
            int totalTax = totalBeforeTax * tax / 100;
            int totalAfterTax = totalBeforeTax - totalTax;

            String text = Messages.message(StringTemplate.template("transaction.sale")
                                           .add("%goods%", goodsType.getNameKey())
                                           .addAmount("%amount%", amount)
                                           .addAmount("%gold%", price))
            + "\n" + Messages.message(StringTemplate.template("transaction.price")
                                      .addAmount("%gold%", totalBeforeTax))
            + "\n" + Messages.message(StringTemplate.template("transaction.tax")
                                      .addAmount("%tax%", tax)
                                      .addAmount("%gold%", totalTax))
            + "\n" + Messages.message(StringTemplate.template("transaction.net")
                                      .addAmount("%gold%", totalAfterTax));
            add(text);
        }

        private void add(String text) {
            StyledDocument doc = getStyledDocument();
            try {
                if (doc.getLength() > 0) {
                    text = "\n\n" + text;
                }
                doc.insertString(doc.getLength(), text, null);
            } catch(Exception e) {
                logger.warning("Failed to update transaction log: " + e.toString());
            }
        }
    }

    public class EuropeButton extends JButton {

        public EuropeButton(String text, int keyEvent, String command,
                            ActionListener listener) {
            setOpaque(true);
            setText(text);
            setActionCommand(command);
            addActionListener(listener);
            InputMap closeInputMap = new ComponentInputMap(this);
            closeInputMap.put(KeyStroke.getKeyStroke(keyEvent, 0, false),
                              "pressed");
            closeInputMap.put(KeyStroke.getKeyStroke(keyEvent, 0, true),
                              "released");
            SwingUtilities.replaceUIInputMap(this, JComponent.WHEN_IN_FOCUSED_WINDOW, closeInputMap);
            enterPressesWhenFocused(this);
        }
    }
}
