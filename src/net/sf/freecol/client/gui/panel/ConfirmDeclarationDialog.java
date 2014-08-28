/**
 *  Copyright (C) 2002-2014   The FreeCol Team
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
import java.util.List;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.Flag.Background;
import net.sf.freecol.client.gui.panel.Flag.Decoration;
import net.sf.freecol.client.gui.panel.Flag.UnionPosition;
import net.sf.freecol.client.gui.panel.Flag.UnionShape;
import net.sf.freecol.client.gui.panel.MigPanel;
import net.sf.freecol.client.gui.plaf.FreeColComboBoxRenderer;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;


/**
 * A dialog used to confirm the declaration of independence.
 */
public class ConfirmDeclarationDialog extends FreeColDialog<List<String>>
    implements ActionListener, ItemListener {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ConfirmDeclarationDialog.class.getName());

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

    private static class EnumRenderer extends FreeColComboBoxRenderer {

        private final String prefix;

        public EnumRenderer(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public void setLabelValues(JLabel c, Object value) {
            c.setText(Messages.message(prefix + value));
        }
    }


    // based on the flag of Venezuela (Colombia and Ecuador are
    // similar)
    public static final Flag SPANISH_FLAG =
        new Flag(Background.FESSES, Decoration.NONE, UnionPosition.MIDDLE)
        .setStripes(3)
        .setUnionColor(null)
        .setBackgroundColors(new Color(0xcf, 0x14, 0x2b),
                             new Color(0, 0x24, 0x7d),
                             new Color(255, 204, 0));

    // based on the flag of Brazil, particularly the Provisional
    // Flag of Republic of the United States of Brazil (November
    // 15–19, 1889)
    public static final Flag PORTUGUESE_FLAG =
        new Flag(Background.FESSES, Decoration.NONE, UnionPosition.CANTON)
        .setUnionColor(new Color(62, 64, 149))
        .setBackgroundColors(new Color(0, 168, 89),
                             new Color(255, 204, 41));

    // based on the current flag of the United States and its
    // various predecessors
    public static final Flag ENGLISH_FLAG =
        new Flag(Background.FESSES, Decoration.NONE, UnionPosition.CANTON)
        .setUnionColor(new Color(.234f, .233f, .430f))
        .setBackgroundColors(new Color(.698f, .132f, .203f),
                             Color.WHITE);

    // based on the flag of Louisiana in 1861 and other similar
    // French colonial flags
    public static final Flag FRENCH_FLAG =
        new Flag(Background.PALES, Decoration.NONE, UnionPosition.LEFT)
        .setStripes(3)
        .setUnionColor(null)
        .setBackgroundColors(new Color(0, 0x23, 0x95),
                             Color.WHITE,
                             new Color(0xed, 0x29, 0x39));

    // Dutch flag
    public static final Flag DUTCH_FLAG =
        new Flag(Background.FESSES, Decoration.NONE, UnionPosition.TOP)
        .setStripes(3)
        .setUnionColor(null)
        .setBackgroundColors(new Color(0xae, 0x1c, 0x28),
                             Color.WHITE,
                             new Color(0x21, 0x46, 0x6b));

    // Swedish flag
    public static final Flag SWEDISH_FLAG =
        new Flag(Background.QUARTERLY, Decoration.SCANDINAVIAN_CROSS, UnionPosition.CANTON)
        .setUnionColor(null)
        .setDecorationColor(new Color(0xFE, 0xCB, 0))
        .setBackgroundColors(new Color(0, 0x52, 0x93));

    // Danish flag
    public static final Flag DANISH_FLAG =
        new Flag(Background.QUARTERLY, Decoration.SCANDINAVIAN_CROSS, UnionPosition.CANTON)
        .setUnionColor(null)
        .setDecorationColor(Color.WHITE)
        .setBackgroundColors(new Color(0xC6, 0x0C, 0x30));

    // Russian flag
    public static final Flag RUSSIAN_FLAG =
        new Flag(Background.FESSES, Decoration.NONE, UnionPosition.MIDDLE)
        .setStripes(3)
        .setUnionColor(null)
        .setBackgroundColors(Color.WHITE,
                             new Color(0, 0x39, 0xa6),
                             new Color(0xd5, 0x2b, 0x1e));

    private final JTextField nationField;

    private final JTextField countryField;

    private final JLabel label = new JLabel();

    private Flag flag;

    @SuppressWarnings("unchecked") // FIXME in Java7
    private final JComboBox decoration = new JComboBox(Flag.Decoration.values());
    @SuppressWarnings("unchecked") // FIXME in Java7
    private JComboBox background = new JComboBox(Flag.Background.values());
    @SuppressWarnings("unchecked") // FIXME in Java7
    private JComboBox unionPosition = new JComboBox(Flag.UnionPosition.values());
    @SuppressWarnings("unchecked") // FIXME in Java7
    private JComboBox unionShape = new JComboBox(Flag.UnionShape.values());
    @SuppressWarnings("unchecked") // FIXME in Java7
    private JComboBox stars = new JComboBox(getNumbers(50));
    @SuppressWarnings("unchecked") // FIXME in Java7
    private JComboBox stripes = new JComboBox(getNumbers(13));

    private ColorButton unionColor = new ColorButton(Color.BLUE);
    private ColorButton starColor = new ColorButton(Color.WHITE);
    private ColorButton decorationColor = new ColorButton(Color.WHITE);
    private ColorButton[] backgroundColors = new ColorButton[] {
        new ColorButton(null), new ColorButton(null), new ColorButton(null),
        new ColorButton(null), new ColorButton(null), new ColorButton(null)
    };


    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public ConfirmDeclarationDialog(FreeColClient freeColClient) {
        super(freeColClient);

        final Player player = freeColClient.getMyPlayer();
        // TODO: put flag data in specification or resources
        String nationId = player.getNationId();
        if ("model.nation.dutch".equals(nationId)) {
            flag = DUTCH_FLAG;
        } else if ("model.nation.spanish".equals(nationId)) {
            flag = SPANISH_FLAG;
        } else if ("model.nation.french".equals(nationId)) {
            flag = FRENCH_FLAG;
        } else if ("model.nation.portuguese".equals(nationId)) {
            flag = PORTUGUESE_FLAG;
        } else if ("model.nation.russian".equals(nationId)) {
            flag = RUSSIAN_FLAG;
        } else if ("model.nation.danish".equals(nationId)) {
            flag = DANISH_FLAG;
        } else if ("model.nation.swedish".equals(nationId)) {
            flag = SWEDISH_FLAG;
        } else {
            // English is default
            flag = ENGLISH_FLAG;
        }

        // Create the main panel
        MigPanel panel = new MigPanel(new MigLayout("wrap 2", "[][fill]", "[fill]"));

        StringTemplate sure
            = StringTemplate.template("declareIndependence.areYouSure.text")
                .add("%monarch%", player.getMonarch().getNameKey());

        StringTemplate country
            = StringTemplate.template("declareIndependence.defaultCountry")
                .add("%nation%", player.getNewLandName());
        countryField = new JTextField(Messages.message(country), 20);
        String cPrompt = Messages.message("declareIndependence.enterCountry");

        StringTemplate nation
            = StringTemplate.template("declareIndependence.defaultNation")
                .addStringTemplate("%nation%", player.getNationName());
        nationField = new JTextField(Messages.message(nation), 20);
        String nPrompt = Messages.message("declareIndependence.enterNation");
        String flagPrompt = Messages.message("declareIndependence.createFlag");

        panel.add(GUI.getDefaultTextArea(Messages.message(sure)), "span");
        panel.add(GUI.getDefaultTextArea(cPrompt), "span");
        panel.add(countryField, "span");
        panel.add(GUI.getDefaultTextArea(nPrompt), "span");
        panel.add(nationField, "span");
        panel.add(GUI.getDefaultTextArea(flagPrompt), "span");

        label.setIcon(new ImageIcon(flag.getImage()));
        panel.add(label, "skip, width 200, height 100");

        addComboBox(panel, background, "flag.background.", flag.getBackground());
        addComboBox(panel, decoration, "flag.decoration.", flag.getDecoration());
        addComboBox(panel, unionPosition, "flag.unionPosition.", flag.getUnionPosition());
        addComboBox(panel, unionShape, "flag.unionShape.", flag.getUnionShape());

        stars.setSelectedIndex(flag.getStars() - 1);
        stars.addItemListener(this);
        panel.add(new JLabel(Messages.message("flag.stars.label")));
        panel.add(stars);

        stripes.setSelectedIndex(flag.getStripes() - 1);
        stripes.addItemListener(this);
        panel.add(new JLabel(Messages.message("flag.stripes.label")));
        panel.add(stripes);

        unionColor.setColor(flag.getUnionColor());
        unionColor.addActionListener(this);
        panel.add(new JLabel(Messages.message("flag.unionColor.label")));
        panel.add(unionColor, "sg colorButton");

        decorationColor.setColor(flag.getDecorationColor());
        decorationColor.addActionListener(this);
        panel.add(new JLabel(Messages.message("flag.decorationColor.label")));
        panel.add(decorationColor);

        starColor.setColor(flag.getStarColor());
        starColor.addActionListener(this);
        panel.add(new JLabel(Messages.message("flag.starColor.label")));
        panel.add(starColor);

        List<Color> flagColors = flag.getBackgroundColors();
        int colors = flagColors.size();
        panel.add(new JLabel(Messages.message("flag.backgroundColors.label")));
        for (int index = 0; index < backgroundColors.length; index++) {
            ColorButton button = backgroundColors[index];
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
        ImageIcon icon = getGUI().getImageLibrary().getImageIcon(player, true);

        final List<String> fake = null;
        List<ChoiceItem<List<String>>> c = choices();
        c.add(new ChoiceItem<List<String>>(Messages.message("declareIndependence.areYouSure.yes"),
                fake).okOption());
        c.add(new ChoiceItem<List<String>>(Messages.message("declareIndependence.areYouSure.no"),
                fake).cancelOption().defaultOption());
        initializeDialog(DialogType.QUESTION, true, panel, icon, c);
    }

    @SuppressWarnings("unchecked") // FIXME in Java7
    private void addComboBox(JPanel panel, JComboBox box, String prefix, Object value) {
        box.setRenderer(new EnumRenderer(prefix));
        box.setSelectedItem(value);
        box.addItemListener(this);
        panel.add(new JLabel(Messages.message(prefix + "label")));
        panel.add(box);
    }


    //@SuppressWarnings("unchecked") // FIXME in Java7
    public void itemStateChanged(ItemEvent e) {
        Flag.Background newBackground = (Flag.Background) background.getSelectedItem();
        Flag.Decoration newDecoration = (Flag.Decoration) decoration.getSelectedItem();
        Flag.UnionPosition newPosition = (Flag.UnionPosition) unionPosition.getSelectedItem();
        Flag.UnionShape newShape = (Flag.UnionShape) unionShape.getSelectedItem();
        flag = new Flag(newBackground, newDecoration, newPosition, newShape);
        flag.setStripes(stripes.getSelectedIndex() + 1);
        flag.setStars(stars.getSelectedIndex() + 1);
        setColors();

        label.setIcon(new ImageIcon(flag.getImage()));
    }

    public void actionPerformed(ActionEvent e) {
        ColorButton button = (ColorButton) e.getSource();
        Color color = JColorChooser
            .showDialog(this,
                        label.getText(),
                        button.getBackground());
        button.setColor(color);
        setColors();
        label.setIcon(new ImageIcon(flag.getImage()));
    }

    private void setColors() {
        flag.setUnionColor(unionColor.getColor());
        flag.setStarColor(starColor.getColor());
        flag.setDecorationColor(decorationColor.getColor());
        List<Color> colors = new ArrayList<Color>();
        for (ColorButton button : backgroundColors) {
            Color color = button.getColor();
            if (color != null) {
                colors.add(color);
            }
            flag.setBackgroundColors(colors);
        }
    }

    public final String[] getNumbers(int count) {
        String[] result = new String[count];
        for (int index = 0; index < count; index++) {
            result[index] = Integer.toString(index + 1);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getResponse() {
        Object value = getValue();
        if (options.get(0).equals(value)) {
            List<String> result = new ArrayList<String>();
            // Sanitize user input, used in save file name
            result.add(nationField.getText().replaceAll("[^\\s\\w]", ""));
            result.add(countryField.getText());
            return result;
        }
        return null;
    }
}
