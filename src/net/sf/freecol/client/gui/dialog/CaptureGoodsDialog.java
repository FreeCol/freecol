/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ChoiceItem;
import net.sf.freecol.client.gui.panel.MigPanel;
import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit;


/**
 * Panel for choosing the goods to capture.
 *
 * <p>This dialog allows the player to select which goods to take after
 * winning combat. The selection is constrained by the winner unit's
 * available cargo slots.</p>
 *
 * <h3>Cargo Slot Rules</h3>
 * <ul>
 *   <li>Each cargo slot holds up to {@code GoodsContainer.CARGO_SIZE}
 *       units (normally 100 units) of a single {@code GoodsType}.</li>
 *   <li>Slot usage is calculated as:
 *       {@code ceil(amount / CARGO_SIZE)} for each goods type.</li>
 *   <li>Adding goods to an existing partial stack does not consume a new slot
 *       unless the combined amount exceeds {@code CARGO_SIZE}.</li>
 *   <li>Adding a new goods type always consumes at least one slot.</li>
 *   <li>The dialog enforces these rules by enabling or disabling items
 *       depending on whether selecting them would exceed the unit's
 *       remaining cargo capacity.</li>
 * </ul>
 *
 * <h3>Panel Layout</h3>
 * <pre>
 * | ----------------------------|
 * |   captureGoodsDialog.title  |
 * | ----------------------------|
 * |     Cargo: X / Y slots      |
 * | ----------------------------|
 * |    allButton | noneButton   |
 * | ----------------------------|
 * |        [] goodsList         |
 * |        [] goodsList         |
 * |        [] goodsList         |
 * |            ...              |
 * | ----------------------------|
 * |                    okButton |
 * | ----------------------------|
 * </pre>
 *
 * <p>Each entry in {@code goodsList} is a {@code GoodsItem} rendered as a
 * checkbox with its goods label and (optionally) its market value.</p>
 */
public final class CaptureGoodsDialog extends DeprecatedFreeColDialog<List<Goods>> {

    // ------------------------------------------------------------
    //  Model: GoodsItem
    // ------------------------------------------------------------
    private static class GoodsItem {
        private final Goods goods;
        private boolean selected;
        private boolean enabled = true;

        GoodsItem(Goods goods) {
            this.goods = goods;
        }

        Goods getGoods() {
            return goods;
        }

        boolean isSelected() {
            return selected;
        }

        void setSelected(boolean selected) {
            this.selected = selected;
        }

        boolean isEnabled() {
            return enabled;
        }

        void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        String pricePerGood(Market market) {
            if (market == null || goods == null) return "";
            int total = market.getBidPrice(goods.getType(), goods.getAmount());
            return Messages.message(
                    StringTemplate.template("goldAmount")
                            .addAmount("%amount%", total)
            );
        }

        @Override
        public String toString() {
            return Messages.message(goods.getLabel());
        }
    }

    // ------------------------------------------------------------
    //  Renderer
    // ------------------------------------------------------------
    private class CheckBoxRenderer extends JPanel implements ListCellRenderer<GoodsItem> {

        private final JCheckBox checkBox = new JCheckBox();
        private final Market market;

        CheckBoxRenderer(Market market) {
            super(new BorderLayout());
            this.market = market;
            setOpaque(true);
            checkBox.setOpaque(true);
            add(checkBox, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(
                JList<? extends GoodsItem> list,
                GoodsItem value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {

            String text = value.toString();

            int extra = extraSlotsIfSelected(value);

            if (extra == 0) {
                text += " " + Messages.message("captureGoodsDialog.mergesZero");
            } else if (extra == 1) {
                text += " " + Messages.message("captureGoodsDialog.mergesOne");
            } else {
                text += " " + Messages.message(
                        StringTemplate.template("captureGoodsDialog.mergesMany")
                            .addAmount("%extra%", extra)
                );
            }

            if (market != null) {
                text += " " + value.pricePerGood(market);
            }

            checkBox.setText(text);
            checkBox.setSelected(value.isSelected());
            checkBox.setEnabled(value.isEnabled());

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                checkBox.setBackground(list.getSelectionBackground());
            } else {
                setBackground(list.getBackground());
                checkBox.setBackground(list.getBackground());
            }

            return this;
        }
    }

    // ------------------------------------------------------------
    //  Fields
    // ------------------------------------------------------------
    private final Unit winner;
    private final JButton allButton;
    private final JButton noneButton;
    private final JList<GoodsItem> goodsList;
    private final JLabel cargoLabel;

    private final List<GoodsItem> items;

    // ------------------------------------------------------------
    //  Constructor
    // ------------------------------------------------------------
    public CaptureGoodsDialog(FreeColClient freeColClient, JFrame frame,
                              Unit winner, List<Goods> loot) {
        super(freeColClient, frame);

        this.winner = winner;

        this.items = loot.stream()
                .map(GoodsItem::new)
                .collect(Collectors.toList());

        this.goodsList = new JList<>(new Vector<>(items));
        this.goodsList.setCellRenderer(new CheckBoxRenderer(winner.getOwner().getMarket()));

        // Mouse click toggles selection if enabled
        this.goodsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = goodsList.locationToIndex(e.getPoint());
                if (index >= 0) {
                    GoodsItem item = items.get(index);
                    if (item.isEnabled()) {
                        item.setSelected(!item.isSelected());
                        updateComponents();
                    }
                }
            }
        });

        // Buttons
        this.allButton = Utility.localizedButton("all");
        this.allButton.addActionListener(e -> selectAll());
        this.allButton.setMnemonic('a');

        this.noneButton = Utility.localizedButton("none");
        this.noneButton.addActionListener(e -> selectNone());
        this.noneButton.setMnemonic('n');

        this.cargoLabel = new JLabel();
        updateCargoLabel();

        // Layout
        JPanel panel = new MigPanel(new MigLayout("wrap 1", "[center]", "[]20[]20[]"));
        panel.add(Utility.localizedHeader("captureGoodsDialog.title", Utility.FONTSPEC_SUBTITLE));
        panel.add(cargoLabel);
        panel.add(allButton, "split 2");
        panel.add(noneButton);
        panel.add(new JScrollPane(goodsList), "grow");

        List<ChoiceItem<List<Goods>>> choices = choices();
        choices.add(new ChoiceItem<List<Goods>>(Messages.message("ok"), (List<Goods>) null)
                .okOption()
                .defaultOption());

        initializeDialog(frame, DialogType.QUESTION, false, panel,
                new ImageIcon(getImageLibrary().getScaledUnitImage(winner)), choices);
    }

    /**
     * Simulates how many cargo slots would be used if the currently
     * selected goods were added to the winner's existing cargo.
     */
    private int simulateSlotsUsed() {

        // Build a simulated map of goods type → amount
        Map<GoodsType, Integer> simulated = new HashMap<>();

        // Start with the winner's existing cargo
        for (Goods g : winner.getGoodsContainer().getCompactGoodsList()) {
            simulated.put(g.getType(), g.getAmount());
        }

        // Add selected loot
        for (GoodsItem gi : items) {
            if (gi.isSelected()) {
                Goods g = gi.getGoods();
                simulated.merge(g.getType(), g.getAmount(), Integer::sum);
            }
        }

        // Compute slots using the same logic as GoodsContainer.getSpaceTaken()
        int slots = 0;
        for (int amount : simulated.values()) {
            slots += (amount % GoodsContainer.CARGO_SIZE == 0)
                    ? amount / GoodsContainer.CARGO_SIZE
                    : amount / GoodsContainer.CARGO_SIZE + 1;
        }

        return slots;
    }

    private int extraSlotsIfSelected(GoodsItem item) {
        Goods g = item.getGoods();
        GoodsType type = g.getType();
        int amount = g.getAmount();
        int size = GoodsContainer.CARGO_SIZE;

        // Current cargo amounts
        Map<GoodsType, Integer> simulated = new HashMap<>();
        for (Goods existing : winner.getGoodsContainer().getCompactGoodsList()) {
            simulated.put(existing.getType(), existing.getAmount());
        }

        int before = simulated.containsKey(type)
                ? (simulated.get(type) + size - 1) / size
                : 0;

        int after = (simulated.getOrDefault(type, 0) + amount + size - 1) / size;

        return after - before;
    }

    private boolean canSelect(GoodsItem item) {
        if (item.isSelected()) return true;

        // Temporarily select it
        item.setSelected(true);
        int used = simulateSlotsUsed();
        item.setSelected(false);

        return used <= winner.getCargoCapacity();
    }

    // ------------------------------------------------------------
    //  Selection Logic
    // ------------------------------------------------------------
    private void selectAll() {
        for (GoodsItem gi : items) {
            if (!gi.isSelected() && canSelect(gi)) {
                gi.setSelected(true);
            }
        }
        updateComponents();
    }

    private void selectNone() {
        items.forEach(i -> i.setSelected(false));
        updateComponents();
    }

    // ------------------------------------------------------------
    //  UI Update Logic
    // ------------------------------------------------------------
    private void updateComponents() {

        for (GoodsItem gi : items) {
            gi.setEnabled(canSelect(gi));
        }

        allButton.setEnabled(items.stream().anyMatch(this::canSelect));
        noneButton.setEnabled(items.stream().anyMatch(GoodsItem::isSelected));

        updateCargoLabel();
        goodsList.repaint();
    }

    private void updateCargoLabel() {
        int used = simulateSlotsUsed();
        int total = winner.getCargoCapacity();
        int free = total - used;

        StringTemplate cargoTemplate = StringTemplate
                .template("captureGoodsDialog.cargoSpace")
                .addAmount("%free%", free)
                .addAmount("%total%", total);

        cargoLabel.setText(Messages.message(cargoTemplate));
    }

    // ------------------------------------------------------------
    //  Result
    // ------------------------------------------------------------
    @Override
    public List<Goods> getResponse() {
        Object value = getValue();
        if (!options.get(0).equals(value)) return List.of();

        return items.stream()
                .filter(GoodsItem::isSelected)
                .map(GoodsItem::getGoods)
                .collect(Collectors.toList());
    }
}
