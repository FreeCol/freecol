package net.sf.freecol.client.gui.dialog;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.panel.MigPanel;
import net.sf.freecol.client.gui.panel.Utility;

import javax.swing.*;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.common.model.MapDetails;

/**
 * A dialog for typing in the author and the description of the created map.
 */
public final class MapDetailsDialog extends FreeColInputDialog<MapDetails> {

    private static final int COLUMNS = 5;
    private static final int ROWS = 5;

    /** The author name. */
    private final JTextField authorName
            = new JTextField(COLUMNS);

    /** The map description. */
    private final JTextArea mapDescription
            = new JTextArea(ROWS, COLUMNS);

    public MapDetailsDialog(FreeColClient freeColClient, JFrame frame) {
        super(freeColClient, frame);

        JLabel authorLabel = Utility.localizedLabel("name");
        authorLabel.setLabelFor(authorName);

        JLabel descriptionLabel = Utility.localizedLabel("description");
        descriptionLabel.setLabelFor(mapDescription);

        JPanel panel = new MigPanel(new MigLayout("wrap 2"));
        panel.add(Utility.localizedHeader("mapAuthorshipDialog.mapAuthorship", true),
                "span, align center");
        panel.add(authorLabel, "newline 20");
        panel.add(authorName);
        panel.add(descriptionLabel);
        panel.add(mapDescription);

        initializeInputDialog(frame, true, panel, null, "ok", "cancel");
    }

    @Override
    protected MapDetails getInputValue() {
        String author, description;

        author = authorName.getText();
        description = mapDescription.getText();

        return new MapDetails(author, description);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestFocus() {
        this.authorName.requestFocus();
    }
}
