/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
import java.util.Arrays;
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
import net.sf.freecol.client.gui.SwingGUI;
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
     * use the <code>TransferHandler</code>.
     */
    private static class FreeColDragHandler
        implements DragGestureListener, DragSourceListener {

        private boolean scrolls;


        // --- DragGestureListener methods -----------------------------------

        /**
         * A Drag gesture has been recognized.
         */
        @Override
        public void dragGestureRecognized(DragGestureEvent dge) {
            JComponent c = (JComponent)dge.getComponent();
            DefaultTransferHandler th
                = (DefaultTransferHandler)c.getTransferHandler();
            Transferable t = th.createTransferable(c);

            if (t != null) {
                scrolls = c.getAutoscrolls();
                c.setAutoscrolls(false);
                try {
                    if (c instanceof JLabel
                        && ((JLabel)c).getIcon() instanceof ImageIcon) {
                        Toolkit tk = Toolkit.getDefaultToolkit();
                        ImageIcon imageIcon = ((ImageIcon)((JLabel)c).getIcon());
                        Dimension bestSize = tk.getBestCursorSize(imageIcon.getIconWidth(),
                            imageIcon.getIconHeight());

                        if (bestSize.width == 0 || bestSize.height == 0) {
                            dge.startDrag(null, t, this);
                            return;
                        }

                        if (bestSize.width > bestSize.height) {
                            bestSize.height = (int)((((double)bestSize.width)
                                    / ((double)imageIcon.getIconWidth()))
                                * imageIcon.getIconHeight());
                        } else {
                            bestSize.width = (int)((((double)bestSize.height)
                                    / ((double)imageIcon.getIconHeight()))
                                * imageIcon.getIconWidth());
                        }
                        BufferedImage scaled = ImageLibrary.createResizedImage(
                            imageIcon.getImage(),
                            bestSize.width, bestSize.height);

                        Point point = new Point(bestSize.width / 2,
                                                bestSize.height / 2);
                        Cursor cursor;
                        try {
                            cursor = tk.createCustomCursor(scaled, point,
                                                           "freeColDragIcon");
                        } catch (RuntimeException re) {
                            cursor = null;
                        }
                        // Point point = new Point(0, 0);
                        dge.startDrag(cursor, t, this);
                    } else {
                        dge.startDrag(null, t, this);
                    }

                    return;
                } catch (RuntimeException re) {
                    c.setAutoscrolls(scrolls);
                }
            }

            th.exportDone(c, null, NONE);
        }

        // --- DragSourceListener methods -----------------------------------

        /**
         * As the hotspot enters a platform dependent drop site.
         */
        @Override
        public void dragEnter(DragSourceDragEvent dsde) {}

        /**
         * As the hotspot moves over a platform dependent drop site.
         */
        @Override
        public void dragOver(DragSourceDragEvent dsde) {}

        /**
         * As the hotspot exits a platform dependent drop site.
         */
        @Override
        public void dragExit(DragSourceEvent dsde) {}

        /**
         * As the operation completes.
         */
        @Override
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

        @Override
        public void dropActionChanged(DragSourceDragEvent dsde) {
            DragSourceContext dsc = dsde.getDragSourceContext();
            JComponent comp = (JComponent)dsc.getComponent();
            updatePartialChosen(comp, dsde.getUserAction() == MOVE);
        }

        private void updatePartialChosen(JComponent comp, boolean partial) {
            if (comp instanceof AbstractGoodsLabel) {
                ((AbstractGoodsLabel)comp).setPartialChosen(partial);
            }
        }
    }

    private static class FreeColDragGestureRecognizer extends DragGestureRecognizer {

        FreeColDragGestureRecognizer(DragGestureListener dgl) {
            super(DragSource.getDefaultDragSource(), null, NONE, dgl);
        }

        void gestured(JComponent c, MouseEvent e, int srcActions, int action) {
            setComponent(c);
            setSourceActions(srcActions);
            appendEvent(e);

            fireDragGestureRecognized(action, e.getPoint());
        }

        /**
         * Register this DragGestureRecognizer's Listeners with the
         * Component.
         */
        @Override
        protected void registerListeners() {}

        /**
         * Unregister this DragGestureRecognizer's Listeners with the
         * Component.
         *
         * subclasses must override this method
         */
        @Override
        protected void unregisterListeners() {}
    }


    public static final DataFlavor flavor
        = new DataFlavor(ImageSelection.class, "ImageSelection");

    private static FreeColDragGestureRecognizer recognizer = null;

    private final FreeColClient freeColClient;

    private final SwingGUI gui;

    private final FreeColPanel parentPanel;


    /**
     * Creates the default FreeCol transfer handler.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param parentPanel The layered pane that holds all kinds of information.
     */
    public DefaultTransferHandler(FreeColClient freeColClient,
                                  FreeColPanel parentPanel) {
        this.freeColClient = freeColClient;
        this.gui = (SwingGUI)freeColClient.getGUI();
        this.parentPanel = parentPanel;
    }


    /**
     * Get the action that can be done to an ImageSelection on the
     * given component.
     *
     * @return The action that can be done to an ImageSelection on the
     *     given component.
     */
    @Override
    public int getSourceActions(JComponent comp) {
        return COPY_OR_MOVE;
    }


    /**
     * Can the given component import a selection of a given flavor.
     *
     * @param comp The component that needs to be checked.
     * @param flavor The flavor that needs to be checked for.
     * @return True if the given component can import a selection of
     *     the flavor that is indicated by the second parameter.
     */
    @Override
    public boolean canImport(JComponent comp, DataFlavor[] flavor) {
        return (comp instanceof JPanel || comp instanceof JLabel)
            && any(flavor, f -> f.equals(DefaultTransferHandler.flavor));
    }

    /**
     * Creates a Transferable (an ImageSelection to be precise) of the
     * data that is represented by the given component and returns that
     * object.
     *
     * @param comp The component to create a Transferable of.
     * @return The resulting Transferable (an ImageSelection object).
     */
    @Override
    public Transferable createTransferable(JComponent comp) {
        if (comp instanceof JLabel && comp instanceof Draggable) {
            return new ImageSelection((JLabel) comp);
        } else {
            return null;
        }
    }

    /**
     * Imports the data represented by the given Transferable into
     * the given component. Returns 'true' on success, 'false' otherwise.
     *
     * @param comp The component to import the data to.
     * @param t The Transferable that holds the data.
     * @return True if the import succeeded.
     */
    @Override
    public boolean importData(JComponent comp, Transferable t) {
        try {
            JLabel data;

            // This variable is used to temporarily keep the old
            // selected unit, while moving cargo from one carrier to another:
            UnitLabel oldSelectedUnit = null;

            // Check flavor.
            if (t.isDataFlavorSupported(DefaultTransferHandler.flavor)) {
                data = (JLabel)t.getTransferData(DefaultTransferHandler.flavor);
            } else {
                logger.warning("Data flavor is not supported for " + t);
                return false;
            }

            // Do not allow a transferable to be dropped upon itself:
            if (comp == data) return false;

            // Make sure we don't drop onto other Labels.
            if (comp instanceof UnitLabel) {
                UnitLabel unitLabel = (UnitLabel) comp;
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
            } else if (comp instanceof AbstractGoodsLabel) {
                comp = getDropTarget(comp);
            }
            // Ignore if data is already in comp.
            if (data.getParent() == comp) return false;

            if (data instanceof GoodsLabel) {
                // Check if the goods can be dragged to comp.
                GoodsLabel label = (GoodsLabel)data;
                Goods goods = label.getGoods();

                // Import the data.
                if (label.isPartialChosen()) {
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
                        if (alt >= 0 && alt < defaultAmount) {
                            defaultAmount = alt;
                        }
                    }
                    int amount = getAmount(goods.getType(), goods.getAmount(),
                                           defaultAmount, false);
                    if (amount <= 0) return false;
                    goods.setAmount(amount);
                } else if (label.isFullChosen()) {
                } else if (goods.getAmount() > GoodsContainer.CARGO_SIZE) {
                    goods.setAmount(GoodsContainer.CARGO_SIZE);
                }

                if (comp instanceof UnitLabel) {
                    return equipUnitIfPossible((UnitLabel)comp, goods);

                } else if (comp instanceof DropTarget) {
                    DropTarget target = (DropTarget)comp;
                    if (!target.accepts(goods)) return false;
                    target.add(data, true);
                    restoreSelection(oldSelectedUnit);
                    comp.revalidate();
                    return true;

                } else if (comp instanceof JLabel) {
                    logger.warning("Failed to handle: " + comp);
                }

            } else if (data instanceof MarketLabel) {
                MarketLabel label = (MarketLabel)data;
                if (label.isPartialChosen()) {
                    int amount = getAmount(label.getType(), label.getAmount(),
                                           -1, true);
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
                        return false;
                    }
                }
                if (comp instanceof CargoPanel) {
                    ((CargoPanel)comp).add(data, true);
                    comp.revalidate();
                    return true;
                } else if (comp instanceof JLabel) {
                    logger.warning("Failed to handle: " + comp);
                    return true;
                } else {
                    logger.warning("Invalid type for receiving component: "
                                   + comp);
                }

            } else if (data instanceof UnitLabel) {
                // Check if the unit can be dragged to comp.
                Unit unit = ((UnitLabel)data).getUnit();
                if (!(comp instanceof DropTarget)) return false;

                DropTarget target = (DropTarget)comp;
                if (!target.accepts(unit)) return false;
                target.add(data, true);

                // Update unit selection.
                // New unit selection has already been taken care of
                // if this unit was moved to ToAmericaPanel
                restoreSelection(oldSelectedUnit);
                comp.revalidate();
                return true;

            } else {
                logger.warning("Invalid type for dragged component: " + data);
            }
        } catch (Exception e) { // FIXME: Suggest a reconnect?
            logger.log(Level.WARNING, "Import data fail", e);
        }
        return false;
    }

    public JComponent getDropTarget(JComponent component) {
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

        for (Role role : unit.getAvailableRoles(null)) {
            if (role.isDefaultRole()) continue;
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
     */
    private int getAmount(GoodsType goodsType, int available,
                          int defaultAmount, boolean needToPay) {
        return gui.showSelectAmountDialog(goodsType, available, defaultAmount,
                                          needToPay);
    }

    @Override
    public void exportAsDrag(JComponent comp, InputEvent e, int action) {
        int srcActions = getSourceActions(comp);
        int dragAction = srcActions & action;
        if (!(e instanceof MouseEvent)) {
            dragAction = NONE;
        }

        if (dragAction != NONE) {
            if (recognizer == null) {
                recognizer = new FreeColDragGestureRecognizer(new FreeColDragHandler());
            }

            recognizer.gestured(comp, (MouseEvent)e, srcActions, dragAction);
        } else {
            exportDone(comp, null, NONE);
        }
    }
}
