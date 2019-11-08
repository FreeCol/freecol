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

package net.sf.freecol.client.gui.dialog;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ChoiceItem;
import net.sf.freecol.client.gui.panel.*;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit;


/**
 * Panel for choosing the goods to capture.
 * <p>
 * Panel Layout:
 * <p style="display: block; font-family: monospace; white-space: pre; margin: 1em 0;">
 * | ----------------------------|
 * |   captureGoodsDialog.title  |
 * | ----------------------------|
 * |    allButton | noneButton   |
 * | ----------------------------|
 * |        [] goodsList         |
 * | ----------------------------|
 * |                    okButton |
 * | ----------------------------|
 * <p>
 * Each member of goodsList is a {@code GoodsItem} as a
 *      checkbox and text combination, repeated as
 *      needed.
 */
public final class CaptureGoodsDialog extends FreeColDialog<List<Goods>> {

    private static final Logger logger = Logger.getLogger(CaptureGoodsDialog.class.getName());


    private static class GoodsItem extends JCheckBox {

        private final Goods goods;


        public GoodsItem(Goods goods) {
            this.goods = goods;
        }


        public Goods getGoods() {
            return this.goods;
        }


        public String pricePerGood(Market lookup) {
            int total = 0;
            if (lookup != null && goods != null) {
                total = lookup.getBidPrice(goods.getType(), goods.getAmount());
            }
            StringTemplate template = StringTemplate
                .template("captureGoodsDialog.europeanValue")
                .addStringTemplate("%gold%", StringTemplate
                    .template("goldAmount")
                    .addAmount("%amount%", total));
            return Messages.message(template);
        }


        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return Messages.message(this.goods.getLabel());
        }
    }

    private static class CheckBoxRenderer extends JCheckBox
        implements ListCellRenderer<GoodsItem> {

        private Market market;

        public CheckBoxRenderer() {
            //setBackground(UIManager.getColor("List.textBackground"));
            //setForeground(UIManager.getColor("List.textForeground"));
        }

        /**
         * Overload constructor for market lookups on good pricing for display
         *
         * @param forPriceLookup A {@code Market} to add extra price
         *     information from.
         */
        public CheckBoxRenderer(Market forPriceLookup) {
            this.market = forPriceLookup;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getListCellRendererComponent(JList<? extends GoodsItem> list,
                                                      GoodsItem value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean hasFocus) {
            if(market!=null){
                setText(value.toString() +" "+ value.pricePerGood(market));
            }else{
                setText(value.toString());
            }
            setSelected(value.isSelected());
            setEnabled(value.isEnabled());
            return this;
        }
    }

    /** The maximum number of items to loot. */
    private final int maxCargo;

    /** The button to select all items. */
    private final JButton allButton;

    /** The button to select no items. */
    private final JButton noneButton;

    /** The list of goods to display. */
    private final JList<GoodsItem> goodsList;


    /**
     * Creates a new CaptureGoodsDialog.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param frame The owner frame.
     * @param winner The {@code Unit} that is looting.
     * @param loot The {@code Goods} to loot.
     */
    public CaptureGoodsDialog(FreeColClient freeColClient, JFrame frame,
            Unit winner, List<Goods> loot) {
        super(freeColClient, frame);

        this.maxCargo = winner.getSpaceLeft();

        GoodsItem[] goods = new GoodsItem[loot.size()];
        for (int i = 0; i < loot.size(); i++) {
            goods[i] = new GoodsItem(loot.get(i));
        }
        this.goodsList = new JList<>();
        this.goodsList.setListData(goods);

        this.allButton = Utility.localizedButton("all");
        this.allButton.addActionListener((ActionEvent ae) -> {
                JList<GoodsItem> gl = CaptureGoodsDialog.this.goodsList;
                int siz = gl.getModel().getSize();
                for (int i = 0; i < siz && i < CaptureGoodsDialog.this.maxCargo; i++) {
                    GoodsItem gi = gl.getModel().getElementAt(i);
                    gi.setSelected(true);
                    updateComponents();
                }
            });
        this.allButton.setMnemonic('a');
        this.allButton.setActionCommand(this.allButton.getText());
 
        this.noneButton = Utility.localizedButton("none");
        this.noneButton.addActionListener((ActionEvent ae) -> {
                JList<GoodsItem> gl = CaptureGoodsDialog.this.goodsList;
                for (int i = 0; i < gl.getModel().getSize(); i++) {
                    GoodsItem gi = gl.getModel().getElementAt(i);
                    gi.setSelected(false);
                    updateComponents();
                }
            });
        this.noneButton.setMnemonic('n');
        this.noneButton.setActionCommand(this.noneButton.getText());

        this.goodsList.setCellRenderer(new CheckBoxRenderer(winner.getOwner().getMarket()));
        this.goodsList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent me) {
                    JList<GoodsItem> gl = CaptureGoodsDialog.this.goodsList;
                    int index = gl.locationToIndex(me.getPoint());
                    if (index < 0) return;
                    GoodsItem item = gl.getModel().getElementAt(index);
                    if (item.isEnabled()) item.setSelected(!item.isSelected());
                    updateComponents();
                }
            });

        JPanel panel = new MigPanel(new MigLayout("wrap 1", "[center]",
                                                  "[]20[]20[]"));
        panel.add(Utility.localizedHeader("captureGoodsDialog.title", true));
        panel.add(this.allButton, "split 2");
        panel.add(this.noneButton);
        panel.add(this.goodsList);
        panel.setSize(panel.getPreferredSize());

        List<ChoiceItem<List<Goods>>> c = choices();
        c.add(new ChoiceItem<>(Messages.message("ok"),
                               (List<Goods>)null).okOption().defaultOption());
        initializeDialog(frame, DialogType.QUESTION, false, panel,
            new ImageIcon(getImageLibrary().getScaledUnitImage(winner)), c);
    }


    /**
     * Update the components of the {@code goodsList}.
     */
    private void updateComponents() {
        int selectedCount = 0;
        for (int i = 0; i < this.goodsList.getModel().getSize(); i++) {
            GoodsItem gi = this.goodsList.getModel().getElementAt(i);
            if (gi.isSelected()) selectedCount++;
        }

        if (selectedCount >= this.maxCargo) {
            this.allButton.setEnabled(false);
            for (int i = 0; i < this.goodsList.getModel().getSize(); i++) {
                GoodsItem gi = this.goodsList.getModel().getElementAt(i);
                if (!gi.isSelected()) gi.setEnabled(false);
            }
        } else {
            this.allButton.setEnabled(true);
            for (int i = 0; i < this.goodsList.getModel().getSize(); i++) {
                GoodsItem gi = this.goodsList.getModel().getElementAt(i);
                if (!gi.isSelected()) gi.setEnabled(true);
            }
        }

        goodsList.repaint();
    }


    // Implement FreeColDialog

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Goods> getResponse() {
        Object value = getValue();
        List<Goods> gl = new ArrayList<>();
        if (options.get(0).equals(value)) {
            for (int i = 0; i < this.goodsList.getModel().getSize(); i++) {
                GoodsItem gi = this.goodsList.getModel().getElementAt(i);
                if (gi.isSelected()) gl.add(gi.getGoods());
            }
        }
        return gl;
    }            
}
