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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ChoiceItem;
import net.sf.freecol.client.gui.panel.Flag.Background;
import net.sf.freecol.client.gui.panel.Flag.Decoration;
import net.sf.freecol.client.gui.panel.Flag.UnionPosition;
import net.sf.freecol.client.gui.panel.Flag.UnionShape;
import net.sf.freecol.client.gui.plaf.FreeColComboBoxRenderer;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;


/**
 * A dialog used to confirm the declaration of independence.
 */
public class ConfirmDeclarationDialog extends FreeColDialog<List<String>>
    implements ActionListener, ItemListener {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ConfirmDeclarationDialog.class.getName());

    /** A button for a colour.  Public for FlagTest. */
    public static class ColorButton extends JButton {

        private Color color = null;


        public ColorButton(Color color) {
            setColor(color);
        }

        public final Color getColor() {
            return color;
        }

        public final void setColor(Color color) {
            this.color = color;
            setBackground(color);
            setText(color == null ? "X" : null);
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public String getUIClassID() {
            return "ColorButtonUI";
        }
    }

    /** Simple renderer for Messages with a prefix. */
    private static class EnumRenderer<T> extends FreeColComboBoxRenderer<T> {

        private final String prefix;


        public EnumRenderer(String prefix) {
            this.prefix = prefix;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void setLabelValues(JLabel c, T value) {
            c.setText(Messages.message(prefix + value.toString()));
        }
    }


    // based on the flag of Venezuela (Colombia and Ecuador are
    // similar)
    public static final Flag SPANISH_FLAG
        = new Flag(Background.FESSES, Decoration.NONE, UnionPosition.MIDDLE)
        .setStripes(3)
        .setUnionColor(null)
        .setBackgroundColors(new Color(0xcf, 0x14, 0x2b),
                             new Color(0, 0x24, 0x7d),
                             new Color(255, 204, 0));

    // based on the flag of Brazil, particularly the Provisional
    // Flag of Republic of the United States of Brazil (November
    // 15–19, 1889)
    public static final Flag PORTUGUESE_FLAG
        = new Flag(Background.FESSES, Decoration.NONE, UnionPosition.CANTON)
        .setUnionColor(new Color(62, 64, 149))
        .setBackgroundColors(new Color(0, 168, 89),
                             new Color(255, 204, 41));

    // based on the current flag of the United States and its
    // various predecessors
    public static final Flag ENGLISH_FLAG
        = new Flag(Background.FESSES, Decoration.NONE, UnionPosition.CANTON)
        .setUnionColor(new Color(.234f, .233f, .430f))
        .setBackgroundColors(new Color(.698f, .132f, .203f),
                             Color.WHITE);

    // based on the flag of Louisiana in 1861 and other similar
    // French colonial flags
    public static final Flag FRENCH_FLAG
        = new Flag(Background.PALES, Decoration.NONE, UnionPosition.LEFT)
        .setStripes(3)
        .setUnionColor(null)
        .setBackgroundColors(new Color(0, 0x23, 0x95),
                             Color.WHITE,
                             new Color(0xed, 0x29, 0x39));

    // Dutch flag
    public static final Flag DUTCH_FLAG
        = new Flag(Background.FESSES, Decoration.NONE, UnionPosition.TOP)
        .setStripes(3)
        .setUnionColor(null)
        .setBackgroundColors(new Color(0xae, 0x1c, 0x28),
                             Color.WHITE,
                             new Color(0x21, 0x46, 0x6b));

    // Swedish flag
    public static final Flag SWEDISH_FLAG
        = new Flag(Background.QUARTERLY, Decoration.SCANDINAVIAN_CROSS, UnionPosition.CANTON)
        .setUnionColor(null)
        .setDecorationColor(new Color(0xFE, 0xCB, 0))
        .setBackgroundColors(new Color(0, 0x52, 0x93));

    // Danish flag
    public static final Flag DANISH_FLAG
        = new Flag(Background.QUARTERLY, Decoration.SCANDINAVIAN_CROSS, UnionPosition.CANTON)
        .setUnionColor(null)
        .setDecorationColor(Color.WHITE)
        .setBackgroundColors(new Color(0xC6, 0x0C, 0x30));

    // Russian flag
    public static final Flag RUSSIAN_FLAG
        = new Flag(Background.FESSES, Decoration.NONE, UnionPosition.MIDDLE)
        .setStripes(3)
        .setUnionColor(null)
        .setBackgroundColors(Color.WHITE,
                             new Color(0, 0x39, 0xa6),
                             new Color(0xd5, 0x2b, 0x1e));

    /** A map of default nation flags. */
    private static final Map<String, Flag> defaultFlags = new HashMap<>();
    static {
        defaultFlags.put("model.nation.dutch",      DUTCH_FLAG);
        defaultFlags.put("model.nation.english",    ENGLISH_FLAG);
        defaultFlags.put("model.nation.french",     FRENCH_FLAG);
        defaultFlags.put("model.nation.spanish",    SPANISH_FLAG);
        defaultFlags.put("model.nation.danish",     DANISH_FLAG);
        defaultFlags.put("model.nation.portuguese", PORTUGUESE_FLAG);
        defaultFlags.put("model.nation.russian",    RUSSIAN_FLAG);
        defaultFlags.put("model.nation.swedish",    SWEDISH_FLAG);
    }

    /** Independent country name. */
    private final JTextField countryField;

    /** Independent nation name. */
    private final JTextField nationField;

    /** Label with the icon of the flag. */
    private final JLabel label;

    /** The flag to use for the new nation. */
    private Flag flag;

    /** A box to select the flag background from. */
    private final JComboBox<Background> background
        = new JComboBox<>(Background.values());

    /** A box to select the flag decoration from. */
    private final JComboBox<Decoration> decoration
        = new JComboBox<>(Decoration.values());

    /** A box to select the union position with. */
    private final JComboBox<UnionPosition> unionPosition
        = new JComboBox<>(UnionPosition.values());

    /** A box to select the union shap with. */
    private final JComboBox<UnionShape> unionShape
        = new JComboBox<>(UnionShape.values());

    /** A box to select the number of stars with. */
    private final JComboBox<String> stars
        = new JComboBox<>(getNumbers(50));

    /** A box to select the number of stripes with. */
    private final JComboBox<String> stripes
        = new JComboBox<>(getNumbers(13));

    /** The selected decoration colour. */
    private final ColorButton decorationColor = new ColorButton(Color.WHITE);

    /** The selected union colour. */
    private final ColorButton unionColor = new ColorButton(Color.BLUE);

    /** The selected star colour. */
    private final ColorButton starColor = new ColorButton(Color.WHITE);

    /** The selected background colours. */
    private final ColorButton[] backgroundColors = {
        new ColorButton(null), new ColorButton(null), new ColorButton(null),
        new ColorButton(null), new ColorButton(null), new ColorButton(null)
    };


    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param frame The owner frame.
     */
    public ConfirmDeclarationDialog(FreeColClient freeColClient, JFrame frame) {
        super(freeColClient, frame);

        final Player player = freeColClient.getMyPlayer();
        this.flag = defaultFlags.get(player.getNationId());
        if (this.flag == null) this.flag = ENGLISH_FLAG; // default to USA-style

        StringTemplate sure = StringTemplate
            .template("confirmDeclarationDialog.areYouSure.text")
            .addNamed("%monarch%", player.getMonarch());

        StringTemplate country = StringTemplate
            .template("confirmDeclarationDialog.defaultCountry")
            .addName("%nation%", player.getNewLandName());
        this.countryField = new JTextField(Messages.message(country), 20);

        StringTemplate nation = StringTemplate
            .template("confirmDeclarationDialog.defaultNation")
            .addStringTemplate("%nation%", player.getNationLabel());
        this.nationField = new JTextField(Messages.message(nation), 20);

        this.label = new JLabel();
        this.label.setIcon(new ImageIcon(this.flag.getImage()));

        // Create the main panel
        MigPanel panel = new MigPanel(new MigLayout("wrap 2", "[][fill]",
                                                    "[fill]"));
        panel.add(Utility.localizedTextArea(sure), "span");
        panel.add(Utility.localizedTextArea("confirmDeclarationDialog.enterCountry"), "span");
        panel.add(this.countryField, "span");
        panel.add(Utility.localizedTextArea("confirmDeclarationDialog.enterNation"), "span");
        panel.add(this.nationField, "span");
        panel.add(Utility.localizedTextArea("confirmDeclarationDialog.createFlag"), "span");

        panel.add(this.label, "skip, width 200, height 100");

        addComboBox(panel, this.background, "flag.background.",
                    this.flag.getBackground());
        addComboBox(panel, this.decoration, "flag.decoration.",
                    this.flag.getDecoration());
        addComboBox(panel, this.unionPosition, "flag.unionPosition.",
                    this.flag.getUnionPosition());
        addComboBox(panel, this.unionShape, "flag.unionShape.",
                    this.flag.getUnionShape());

        this.stars.setSelectedIndex(this.flag.getStars() - 1);
        this.stars.addItemListener(this);
        panel.add(Utility.localizedLabel("flag.stars.label"));
        panel.add(this.stars);

        this.stripes.setSelectedIndex(this.flag.getStripes() - 1);
        this.stripes.addItemListener(this);
        panel.add(Utility.localizedLabel("flag.stripes.label"));
        panel.add(this.stripes);

        this.unionColor.setColor(this.flag.getUnionColor());
        this.unionColor.addActionListener(this);
        panel.add(Utility.localizedLabel("flag.unionColor.label"));
        panel.add(this.unionColor, "sg colorButton");

        this.decorationColor.setColor(this.flag.getDecorationColor());
        this.decorationColor.addActionListener(this);
        panel.add(Utility.localizedLabel("flag.decorationColor.label"));
        panel.add(this.decorationColor);

        this.starColor.setColor(this.flag.getStarColor());
        this.starColor.addActionListener(this);
        panel.add(Utility.localizedLabel("flag.starColor.label"));
        panel.add(this.starColor);

        List<Color> flagColors = this.flag.getBackgroundColors();
        int colors = flagColors.size();
        panel.add(Utility.localizedLabel("flag.backgroundColors.label"));
        for (int index = 0; index < this.backgroundColors.length; index++) {
            ColorButton button = this.backgroundColors[index];
            if (index < colors) button.setColor(flagColors.get(index));
            button.addActionListener(this);
            if (index == 0) {
                panel.add(button, "split 3, sg colorButton");
            } else if (index % 3 == 0) {
                panel.add(button, "skip, split 3, sg colorButton");
            } else {
                panel.add(button, "sg colorButton");
            }
        }

        panel.setPreferredSize(panel.getPreferredSize()); // Prevent NPE

        // Use the coat of arms image icon.  Is there something better?
        ImageIcon icon = new ImageIcon(getImageLibrary().getSmallMiscIconImage(player.getNation()));

        final List<String> fake = null;
        List<ChoiceItem<List<String>>> c = choices();
        c.add(new ChoiceItem<>(Messages.message("confirmDeclarationDialog.areYouSure.yes"),
                fake).okOption());
        c.add(new ChoiceItem<>(Messages.message("confirmDeclarationDialog.areYouSure.no"),
                fake).cancelOption().defaultOption());
        initializeDialog(frame, DialogType.QUESTION, true, panel, icon, c);
    }


    private <T> void addComboBox(JPanel panel, JComboBox<T> box,
                                 String prefix, T value) {
        box.setRenderer(new EnumRenderer<T>(prefix));
        box.setSelectedItem(value);
        box.addItemListener(this);
        panel.add(Utility.localizedLabel(prefix + "label"));
        panel.add(box);
    }

    private void setColors() {
        this.flag.setUnionColor(this.unionColor.getColor());
        this.flag.setStarColor(this.starColor.getColor());
        this.flag.setDecorationColor(this.decorationColor.getColor());
        List<Color> colors = new ArrayList<>();
        for (ColorButton button : this.backgroundColors) {
            Color color = button.getColor();
            if (color != null) {
                colors.add(color);
            }
            this.flag.setBackgroundColors(colors);
        }
    }

    private String[] getNumbers(int count) {
        String[] result = new String[count];
        for (int index = 0; index < count; index++) {
            result[index] = Integer.toString(index + 1);
        }
        return result;
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        ColorButton button = (ColorButton)ae.getSource();
        Color color = JColorChooser.showDialog(this, this.label.getText(),
                                               button.getBackground());
        button.setColor(color);
        setColors();
        this.label.setIcon(new ImageIcon(this.flag.getImage()));
    }


    // Interface ItemListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void itemStateChanged(ItemEvent e) {
        Background newBackground
            = (Background)this.background.getSelectedItem();
        Decoration newDecoration
            = (Decoration)this.decoration.getSelectedItem();
        UnionPosition newPosition
            = (UnionPosition)this.unionPosition.getSelectedItem();
        UnionShape newShape
            = (UnionShape)this.unionShape.getSelectedItem();
        this.flag = new Flag(newBackground, newDecoration,
                             newPosition, newShape);
        this.flag.setStars(this.stars.getSelectedIndex() + 1);
        this.flag.setStripes(this.stripes.getSelectedIndex() + 1);
        setColors();
        this.label.setIcon(new ImageIcon(this.flag.getImage()));
    }


    // Override FreeColDialog

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getResponse() {
        Object value = getValue();
        if (options.get(0).equals(value)) {
            List<String> result = new ArrayList<>();
            // Sanitize user input, used in save file name
            result.add(this.nationField.getText().replaceAll("[^\\s\\w]", ""));
            result.add(this.countryField.getText());
            return result;
        }
        return null;
    }
}
