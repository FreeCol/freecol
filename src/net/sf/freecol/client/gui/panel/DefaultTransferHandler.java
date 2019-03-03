/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceContext;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.TransferHandler;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.label.AbstractGoodsLabel;
import net.sf.freecol.client.gui.label.Draggable;
import net.sf.freecol.client.gui.label.GoodsLabel;
import net.sf.freecol.client.gui.label.MarketLabel;
import net.sf.freecol.client.gui.label.UnitLabel;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsLocation;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Unit;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * The TransferHandler that is capable of creating ImageSelection objects.
 * Those ImageSelection objects are Transferable.  The DefaultTransferHandler
 * should be attached to JPanels or custom JLabels.
 */
public final class DefaultTransferHandler extends TransferHandler {

    private static final Logger logger = Logger.getLogger(DefaultTransferHandler.class.getName());

    /**
     * This is the default drag handler for drag and drop operations that
     * use the {@code TransferHandler}.
     */
    private static class FreeColDragHandler
        implements DragGestureListener, DragSourceListener {

        private boolean scrolls;


        private void updatePartialChosen(JComponent comp, boolean partial) {
            if (comp instanceof AbstractGoodsLabel) {
                ((AbstractGoodsLabel)comp).setPartialChosen(partial);
            }
        }

        /**
         * Get a suitable cursor for the given component.
         *
         * @param c The component to consider.
         * @return A suitable {@code Cursor}, or null on failure.
         */
        private Cursor getCursor(JComponent c) {
            if (c instanceof JLabel
                && ((JLabel)c).getIcon() instanceof ImageIcon) {
                Toolkit tk = Toolkit.getDefaultToolkit();
                ImageIcon imageIcon = ((ImageIcon)((JLabel)c).getIcon());
                Dimension bestSize = tk.getBestCursorSize(imageIcon.getIconWidth(),
                    imageIcon.getIconHeight());
                    
                if (bestSize.width == 0 || bestSize.height == 0) return null;
                   
                if (bestSize.width > bestSize.height) {
                    bestSize.height = (int)((((double)bestSize.width)
                            / ((double)imageIcon.getIconWidth()))
                        * imageIcon.getIconHeight());
                } else {
                    bestSize.width = (int)((((double)bestSize.height)
                            / ((double)imageIcon.getIconHeight()))
                        * imageIcon.getIconWidth());
                }
                BufferedImage scaled = ImageLibrary
                    .createResizedImage(imageIcon.getImage(),
                                        bestSize.width, bestSize.height);
                Point point = new Point(bestSize.width / 2,
                                        bestSize.height / 2);
                return tk.createCustomCursor(scaled, point,
                                             "freeColDragIcon");
            }
            return null;
        }
        

        // Interface DragGestureListener

        /**
         * {@inheritDoc}
         */
        public void dragGestureRecognized(DragGestureEvent dge) {
            JComponent c = (JComponent)dge.getComponent();
            DefaultTransferHandler th
                = (DefaultTransferHandler)c.getTransferHandler();
            Transferable t = th.createTransferable(c);
            if (t == null) {
                logger.warning("Unable to create transferable for: " + dge);
                th.exportDone(c, null, NONE);
                return;
            }

            this.scrolls = c.getAutoscrolls();
            c.setAutoscrolls(false);
            try {
                Cursor cursor = getCursor(c);
                dge.startDrag(cursor, t, this);
            } catch (RuntimeException re) {
                c.setAutoscrolls(this.scrolls);
            }
        }


        // Interface DragSourceListener

        /**
         * {@inheritDoc}
         */
        public void dragEnter(DragSourceDragEvent dsde) {}

        /**
         * {@inheritDoc}
         */
        public void dragOver(DragSourceDragEvent dsde) {}

        /**
         * {@inheritDoc}
         */
        public void dragExit(DragSourceEvent dsde) {}

        /**
         * {@inheritDoc}
         */
        public void dragDropEnd(DragSourceDropEvent dsde) {
            DragSourceContext dsc = dsde.getDragSourceContext();
            JComponent c = (JComponent)dsc.getComponent();

            if (dsde.getDropSuccess()) {
                ((DefaultTransferHandler)c.getTransferHandler()).exportDone(c,
                    dsc.getTransferable(), dsde.getDropAction());
            } else {
                ((DefaultTransferHandler)c.getTransferHandler()).exportDone(c,
                    null, NONE);
            }
            c.setAutoscrolls(scrolls);
        }

        /**
         * {@inheritDoc}
         */
        public void dropActionChanged(DragSourceDragEvent dsde) {
            DragSourceContext dsc = dsde.getDragSourceContext();
            JComponent comp = (JComponent)dsc.getComponent();
            updatePartialChosen(comp, dsde.getUserAction() == MOVE);
        }
    }

    private static class FreeColDragGestureRecognizer
        extends DragGestureRecognizer {

        FreeColDragGestureRecognizer(DragGestureListener dgl) {
            super(DragSource.getDefaultDragSource(), null, NONE, dgl);
        }

        public void gestured(JComponent c, MouseEvent e, int srcActions,
                             int action) {
            setComponent(c);
            setSourceActions(srcActions);
            appendEvent(e);

            fireDragGestureRecognized(action, e.getPoint());
        }

        // Implement DragGestureRecognizer

        /**
         * {@inheritDoc}
         */
        protected void registerListeners() {}

        /**
         * {@inheritDoc}
         */
        protected void unregisterListeners() {}
    }


    public static final DataFlavor flavor
        = new DataFlavor(ImageSelection.class, "ImageSelection");

    private static final FreeColDragGestureRecognizer recognizer
        = new FreeColDragGestureRecognizer(new FreeColDragHandler());

    /** The enclosing client. */
    private final FreeColClient freeColClient;

    /** The panel where the transfer begins. */
    private final FreeColPanel parentPanel;


    /**
     * Creates the default FreeCol transfer handler.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param parentPanel The layered pane that holds all kinds of information.
     */
    public DefaultTransferHandler(FreeColClient freeColClient,
                                  FreeColPanel parentPanel) {
        this.freeColClient = freeColClient;
        this.parentPanel = parentPanel;
    }


    private JComponent getDropTarget(JComponent component) {
        return (component instanceof DropTarget)
            ? component
            : (component.getParent() instanceof JComponent)
            ? getDropTarget((JComponent)component.getParent())
            : null;
    }

    private void restoreSelection(UnitLabel oldSelectedUnit) {
        if (oldSelectedUnit != null
            && oldSelectedUnit.getParent() instanceof InPortPanel) {
            ((PortPanel) parentPanel).setSelectedUnitLabel(oldSelectedUnit);
        }
    }

    private boolean equipUnitIfPossible(UnitLabel unitLabel,
                                        AbstractGoods goods) {
        final Unit unit = unitLabel.getUnit();
        if (!unit.hasAbility(Ability.CAN_BE_EQUIPPED)
            || unit.getRole().hasAbility(Ability.ESTABLISH_MISSION)) {
            // Do not equip missionaries.  The test below will succeed
            // when dragging incompatible goods (anything:-) because
            // there is no actual missionary equipment.
            return false;
        }

        for (Role role : transform(unit.getAvailableRoles(null),
                                   r -> !r.isDefaultRole())) {
            List<AbstractGoods> required = unit.getGoodsDifference(role, 1);
            int count;
            if (required.size() == 1
                && required.get(0).getType() == goods.getType()
                && (count = Math.min(role.getMaximumCount(),
                        goods.getAmount() / required.get(0).getAmount())) > 0
                && (role != unit.getRole() || count != unit.getRoleCount())) {
                freeColClient.getInGameController()
                    .equipUnitForRole(unit, role, count);
                unitLabel.updateIcon();
                return true;
            }
        }
        return false;
    }

    /**
     * Displays an input dialog box where the user should specify a
     * goods transfer amount.
     *
     * @param goodsType The {@code GoodsType} to transfer.
     * @param available The amount of goods available.
     * @param defaultAmount The default amount of goods to offer.
     * @param needToPay If true limit by available funds.
     * @return The selected amount of goods.
     */
    private int getAmount(GoodsType goodsType, int available,
                          int defaultAmount, boolean needToPay) {
        return this.freeColClient.getGUI()
            .showSelectAmountDialog(goodsType, available, defaultAmount,
                                    needToPay);
    }

    /**
     * Complain about import failure.
     *
     * @param comp The importing component.
     * @param data A description of the data being imported.
     * @return Always false.
     */
    private boolean importFail(JComponent comp, String data) {
        logger.warning("Transfer of " + data + " invalid at: " + comp);
        return false;
    }

    /**
     * Import goods specified by label to a component.
     *
     * @param comp The component to import to.
     * @param label The {@code GoodsLabel} specifying the goods.
     * @param oldSelectedUnit A label for the old {@code Unit} to restore
     *     selection to.
     * @return True if the import succeeds.
     */
    private boolean importGoods(JComponent comp, GoodsLabel label,
                                UnitLabel oldSelectedUnit) {
        Goods goods = label.getGoods();

        // Special handling for goods amount.
        if (label.isSuperFullChosen()) {
            if (goods.getLocation() instanceof GoodsLocation) {
                GoodsLocation loc = (GoodsLocation)goods.getLocation();
                int amountToTransfer = loc.getGoodsCount(goods.getType());
                if (comp instanceof DropTarget) {
                    DropTarget dt = (DropTarget) comp;
                    if (dt instanceof CargoPanel) {
                        CargoPanel cp = (CargoPanel) dt;
                        Unit carrier = cp.getCarrier();
                        int spaceTaken = carrier.getCargoSpaceTaken();
                        int availableHolds = carrier.getCargoCapacity() - spaceTaken;
                        if (amountToTransfer > GoodsContainer.CARGO_SIZE * availableHolds) {
                            amountToTransfer = GoodsContainer.CARGO_SIZE * availableHolds;
                            label.setAmount(amountToTransfer);
                        }
                    }
                }
            }
        } else if (label.isPartialChosen()) {
            int defaultAmount = goods.getAmount();
            if (goods.getLocation() instanceof GoodsLocation) {
                GoodsLocation loc = (GoodsLocation)goods.getLocation();
                if (goods.getAmount() > loc.getGoodsCapacity()) {
                    // If over capacity, favour the amount that would
                    // correct the problem.
                    defaultAmount = Math.min(GoodsContainer.CARGO_SIZE,
                        goods.getAmount() - loc.getGoodsCapacity());
                }
            }
            if (comp instanceof DropTarget) {
                int alt = ((DropTarget)comp).suggested(goods.getType());
                if (alt >= 0 && alt < defaultAmount) defaultAmount = alt;
            }
            int amount = getAmount(goods.getType(), goods.getAmount(),
                                   defaultAmount, false);
            if (amount <= 0) {
                return importFail(comp, "weird goods amount (" + amount + ")");
            }
            goods.setAmount(amount);
        } else if (label.isFullChosen()) {
            ; // Amount correct
        } else if (goods.getAmount() > GoodsContainer.CARGO_SIZE) {
            goods.setAmount(GoodsContainer.CARGO_SIZE);
        }

        if (comp instanceof UnitLabel) {
            return equipUnitIfPossible((UnitLabel)comp, goods);

        } else if (comp instanceof DropTarget) {
            DropTarget target = (DropTarget)comp;
            if (!target.accepts(goods)) {
                return importFail(comp, "unacceptable goods (" + goods + ")");
            }
            target.add(label, true);
            restoreSelection(oldSelectedUnit);
            comp.revalidate();
            return true;
        }
        return importFail(comp, "goods");
    }

    /**
     * Import from a market specified by label to a component.
     *
     * @param comp The component to import to.
     * @param label The {@code MarketLabel} specifying the goods.
     * @return True if the import succeeds.
     */
    private boolean importMarket(JComponent comp, MarketLabel label) {
        if (label.isPartialChosen()) {
            int amount = getAmount(label.getType(), label.getAmount(), -1, true);
            if (amount <= 0) return false;
            label.setAmount(amount);
        }
        if (comp instanceof UnitLabel) {
            if (equipUnitIfPossible((UnitLabel)comp,
                                    label.getAbstractGoods())) return true;
            // Try again with parent
            if (comp.getParent() instanceof JComponent) {
                comp = (JComponent)comp.getParent();
            } else {
                return importFail(comp, "equipping market goods");
            }
        }
        if (comp instanceof CargoPanel) {
            ((CargoPanel)comp).add(label, true);
            comp.revalidate();
            return true;
        }
        return importFail(comp, "market goods");
    }

    /**
     * Import a unit specified by its label to a component.
     *
     * @param comp The component to import to.
     * @param label The {@code UnitLabel} specifying the unit.
     * @param oldSelectedUnit A label for the old {@code Unit} to restore
     *     selection to.
     * @return True if the import succeeds.
     */
    private boolean importUnit(JComponent comp, UnitLabel label,
                               UnitLabel oldSelectedUnit) {
        if (!(comp instanceof DropTarget)) return importFail(comp, "unit");
        final DropTarget target = (DropTarget)comp;

        // Check if the unit can be dragged to comp.
        final Unit unit = label.getUnit();
        if (!target.accepts(unit)) {
            return importFail(comp, "unacceptable unit (" + unit + ")");
        }

        // OK, add it.
        target.add(label, true);

        // Update unit selection.
        // New unit selection has already been taken care of
        // if this unit was moved to ToAmericaPanel
        restoreSelection(oldSelectedUnit);
        comp.revalidate();
        return true;
    }

    // Override TransferHandler

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canImport(JComponent comp, DataFlavor[] flavor) {
        return (comp instanceof JPanel || comp instanceof JLabel)
            && any(flavor, matchKeyEquals(DefaultTransferHandler.flavor));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Transferable createTransferable(JComponent comp) {
        return (comp instanceof JLabel && comp instanceof Draggable)
            ? new ImageSelection((JLabel) comp)
            : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void exportAsDrag(JComponent comp, InputEvent e, int action) {
        final int srcActions = getSourceActions(comp);
        final int dragAction = (e instanceof MouseEvent)
            ? (srcActions & action)
            : NONE;
        if (dragAction != NONE) { // Use the recognizer
            recognizer.gestured(comp, (MouseEvent)e, srcActions, dragAction);
        } else {
            exportDone(comp, null, NONE);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSourceActions(JComponent comp) {
        return COPY_OR_MOVE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean importData(JComponent comp, Transferable t) {
        if (!t.isDataFlavorSupported(DefaultTransferHandler.flavor)) {
            return importFail(comp, "data flavor");
        }

        boolean ret;
        // This variable is used to temporarily keep the old
        // selected unit, while moving cargo from one carrier to another:
        UnitLabel oldSelectedUnit = null;
        try {
            // Get the data to transfer.
            JLabel data = (JLabel)t.getTransferData(DefaultTransferHandler.flavor);
            // Do not allow a transferable to be dropped upon itself:
            if (comp == data) return false;

            // Make sure we don't drop onto other Labels.
            if (comp instanceof AbstractGoodsLabel) {
                comp = getDropTarget(comp);
            } else if (comp instanceof UnitLabel) {
                UnitLabel unitLabel = (UnitLabel)comp;
                /**
                 * If the unit/cargo is dropped on a carrier in port
                 * then the ship is selected and the unit is added to
                 * its cargo.  If the unit is not a carrier, but can
                 * be equipped, and the goods can be converted to
                 * equipment, equip the unit.
                 *
                 * If not, assume that the user wished to drop the
                 * unit/cargo on the panel below.
                 */
                if (unitLabel.getUnit().isCarrier()
                    && unitLabel.getParent() instanceof InPortPanel
                    && parentPanel instanceof PortPanel) {
                    PortPanel portPanel = (PortPanel) parentPanel;
                    if (data instanceof Draggable
                        && ((Draggable)data).isOnCarrier()) {
                        oldSelectedUnit = portPanel.getSelectedUnitLabel();
                    }
                    portPanel.setSelectedUnitLabel(unitLabel);
                    comp = portPanel.getCargoPanel();
                } else if (unitLabel.canUnitBeEquippedWith(data)) {
                    // don't do anything before partial amount has been checked
                } else {
                    comp = getDropTarget(comp);
                }
            }

            ret = (data.getParent() == comp)
                ? importFail(comp, "data-already-present")
                : (data instanceof GoodsLabel)
                ? importGoods(comp, (GoodsLabel)data, oldSelectedUnit)
                : (data instanceof MarketLabel)
                ? importMarket(comp, (MarketLabel)data)
                : (data instanceof UnitLabel)
                ? importUnit(comp, (UnitLabel)data, oldSelectedUnit)
                : importFail(comp, data.toString());
        } catch (Exception e) { // FIXME: Suggest a reconnect?
            logger.log(Level.WARNING, "Import fail", e);
            ret = importFail(comp, "crash: " + e);
        }
        return ret;
    }
}
