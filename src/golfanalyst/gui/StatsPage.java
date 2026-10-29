package golfanalyst.gui;

import golfanalyst.DataStore;
import golfanalyst.StatsBoard;
import golfanalyst.StatsBoard.Column;
import golfanalyst.StatsBoard.Row;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

/*
 +--------------------------------------------------------------------------+
 | StatsPage                                                                |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | The Stats Board: the statistics of every course and tees played, one     |
 | row each, in a wood frame on the photo. 18-hole rows first; if there     |
 | are nine-hole rounds, a "Nine-hole rounds" band and then the nine-hole   |
 | rows (a row per tee and nine). The band row is always there, even with   |
 | no nine-hole rounds. The figures come from StatsBoard, which has no      |
 | Swing code.                                                              |
 |                                                                          |
 | The list never looks empty: it is at least 12 rows tall, and rows not    |
 | needed are blank white spreadsheet rows. A section with no rows shows a  |
 | faint watermark row instead ("No nine-hole rounds yet", or "No rounds    |
 | yet. Press New Round on the Home screen."). Blank and watermark rows     |
 | cannot be picked.                                                        |
 |                                                                          |
 | Across the top of the frame a pinned wood band holds the column          |
 | headings (click one to sort, again to reverse; the two sections sort     |
 | separately) and the totals for all 18-hole and all nine-hole rounds.     |
 | Below it the rows scroll. Gold marks each column's best figure in each   |
 | section. Clicking a row highlights it (cream with a gold outline);       |
 | the Up and Down keys move the highlight, and a double-click or Enter     |
 | opens the Rounds screen on that course and tees. The sort and the        |
 | highlighted row last for the whole session (this page is never           |
 | rebuilt), not between runs of the program.                               |
 |                                                                          |
 | Sizes: the grid is 266 + 96 + 8 x 62 = 858 px wide, plus the 17 px       |
 | scroll bar (always shown, so nothing jumps) = 875, plus 9 + 9 wood =     |
 | 893 of the 944 px page. Rows are 25 px. The band is always 3 + 3 x 25 +  |
 | 3 = 81. The frame is 9 + 81 + 25 x rows + 9 = 99 + 25 x rows. The list   |
 | is never under 12 rows (300 px, frame 399; the scroll bar's track is     |
 | then 300 - 2 x 17 = 266 px). With 14 px of photo above and below, a 626  |
 | px page holds at most 19 rows (99 + 19 x 25 = 574); past that the list   |
 | scrolls. The limit follows the real page height.                         |
 |                                                                          |
 | Public Methods:                                                          |
 | onShow()        - works the board out again and refills the screen       |
 | sortBy(column)  - sorts by a column, or reverses it if already sorted    |
 | select(key)     - highlights a row                                       |
 | moveSelection(step), openSelected() - the Up / Down / Enter keys         |
 +--------------------------------------------------------------------------+
 */
@SuppressWarnings({"serial", "this-escape"})
public final class StatsPage extends Page
{

    static final int[] COLUMN_WIDTHS = {266, 96, 62, 62, 62, 62, 62, 62, 62, 62};
    static final int GRID_WIDTH = 858;
    static final int ROW_HEIGHT = 25;
    static final int BAND_PAD = 3;
    static final int TAG_INSET = 3;
    static final int PAGE_MARGIN = 14;
    static final int FRAME_WOOD = 9;
    static final String NINE_BAND = "Nine-hole rounds";
    static final int MIN_ROWS = 12;
    static final String NO_NINE_MARK = "No nine-hole rounds yet";
    static final String NO_ROUNDS_MARK = "No rounds yet. Press New Round on the Home screen.";
    static final String NO_EIGHTEEN_MARK = "No 18-hole rounds yet";
    static final Color WATERMARK_TEXT = new Color(165, 160, 150);
    static final String FOOTER_HINT = "Click a course to highlight it, double-click to see its rounds. Click a heading to sort. "
            + "Gold marks the best figure among rows with two or more rounds.";
    static final String NO_ROUNDS = "There are no rounds yet, so the Stats Board is empty. Enter a round with New Round on the Home screen.";
    static final String SAND_TIP = "Sand saves: how often I was in the sand and still made par or better";
    static final Color TOTAL_FACE = new Color(236, 206, 160);
    static final Color TOTAL_EDGE = new Color(70, 35, 5);
    static final Color NINE_BAND_FACE = new Color(214, 190, 150);
    static final Color BEST_FACE = new Color(255, 236, 150);
    private static final int NAME_PAD = 6;
    private static final float CELL_FONT = 14f;
    private static final float NAME_FONT = 13f;
    private static final float LARGEST_HEADING_FONT = 14f;
    private static final float SMALLEST_HEADING_FONT = 8f;
    private static final Column[] COLUMNS = Column.values();

    final WoodFrame frame = new WoodFrame();
    final JPanel band = new JPanel(new GridBagLayout())
    {
        @Override
        protected void paintComponent(Graphics graphics)
        {
            WoodFrame.paintFrameWood((Graphics2D) graphics, this);
        }
    };
    final Map<Column, WoodTag> headings = new EnumMap<>(Column.class);
    final RowList list = new RowList();
    final JScrollPane scroll;
    final WoodScrollBarUI barUI = new WoodScrollBarUI();
    final List<RowPanel> rowPanels = new ArrayList<>();
    final List<JComponent> totalCells = new ArrayList<>();
    final List<JPanel> blankRows = new ArrayList<>();
    final List<JLabel> watermarks = new ArrayList<>();
    JLabel nineBand = null;

    private Column sortColumn = Column.COURSE;
    private boolean ascending = true;
    private String selectedKey = null;
    private StatsBoard.Board board = null;
    private int bandRows = 3;

    /*
     +--------------------------------------------------------------------------+
     | StatsPage(shell)                                                         |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Builds the empty frame: the band with its headings, the list in a        |
     | scroll pane with the wood scroll bar (always shown, never a              |
     | horizontal one), the frame centred on the photo, and the Up, Down and    |
     | Enter keys. It does not call the window; the rows are filled in onShow.  |
     |                                                                          |
     | Passed in:  shell - the window this page sits in                         |
     | Passed out: the page                                                     |
     +--------------------------------------------------------------------------+
     */
    public StatsPage(Shell shell)
    {
        super(shell, "stats");
        setLayout(new GridBagLayout());
        band.setOpaque(true);
        band.setBorder(BorderFactory.createEmptyBorder(BAND_PAD, 0, BAND_PAD, 0));
        Font headingFont = Theme.font(Font.BOLD, headingFontSize());
        for (Column column : COLUMNS)
        {
            WoodTag tag = new WoodTag(column.heading());
            tag.setFont(headingFont);
            tag.setFixedFont(true);
            tag.setOnClick(() -> sortBy(column));
            if (column == Column.SAND)
            {
                tag.setToolTipText(SAND_TIP);
            }
            headings.put(column, tag);
        }
        showHeadingArrows();

        scroll = new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER)
        {
            @Override
            public Dimension getPreferredSize()
            {
                return new Dimension(GRID_WIDTH + WoodScrollBarUI.BAR_WIDTH, visibleRows() * ROW_HEIGHT);
            }
        };
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setViewportBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUI(barUI);
        scroll.getVerticalScrollBar().setUnitIncrement(ROW_HEIGHT);
        frame.add(band, BorderLayout.NORTH);
        frame.add(scroll, BorderLayout.CENTER);
        GridBagConstraints at = new GridBagConstraints();
        at.insets = new Insets(PAGE_MARGIN, PAGE_MARGIN, PAGE_MARGIN, PAGE_MARGIN);
        add(frame, at);
        fillBand();
        addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent event)
            {
                frame.revalidate();
            }
        });
        bindKey("UP", "up", () -> moveSelection(-1));
        bindKey("DOWN", "down", () -> moveSelection(1));
        bindKey("ENTER", "open", () -> openSelected());
    }

    /*
     * bindKey(key, name, action): makes a key do something whenever the cursor is anywhere on this page.
     * Passed in:  key - e.g. "UP", name - the action's name, action - what it does
     * Passed out: nothing
     */
    private void bindKey(String key, String name, Runnable action)
    {
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(key), name);
        getActionMap().put(name, new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent event)
            {
                action.run();
            }
        });
    }

    /*
     +--------------------------------------------------------------------------+
     | headingFontSize()                                                        |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Finds one font size for every heading: the largest at which every        |
     | heading fits its tag (3 px smaller than its slot, less the tag's 4 px    |
     | margins). The longest heading for its tag, "Putts/Rnd", decides it.      |
     | The sort arrow does not need room beside the text: there is none in a    |
     | 62 px column at any readable size, so it sits in the tag's top right     |
     | corner, above the letters.                                               |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: the font size in points                                      |
     +--------------------------------------------------------------------------+
     */
    float headingFontSize()
    {
        float size = LARGEST_HEADING_FONT;
        while (size > SMALLEST_HEADING_FONT)
        {
            FontMetrics metrics = getFontMetrics(Theme.font(Font.BOLD, size));
            boolean fits = true;
            for (Column column : COLUMNS)
            {
                int room = COLUMN_WIDTHS[column.ordinal()] - TAG_INSET - 8;
                fits = fits && metrics.stringWidth(column.heading()) <= room;
            }
            if (fits)
            {
                return size;
            }
            size = size - 0.5f;
        }
        return size;
    }

    /*
     * visibleRows(): how many rows the list shows before it scrolls: all of them (never fewer than 12), up to what fits the page.
     */
    int visibleRows()
    {
        int pageHeight = (getHeight() > 0 ? getHeight() : 626);
        int bandHeight = 2 * BAND_PAD + bandRows * ROW_HEIGHT;
        int room = (pageHeight - 2 * PAGE_MARGIN - 2 * FRAME_WOOD - bandHeight) / ROW_HEIGHT;
        int wanted = Math.max(MIN_ROWS, list.getComponentCount());
        return Math.max(1, Math.min(wanted, room));
    }

    /*
     * title(): the header text.
     */
    @Override
    public String title()
    {
        return "Stats Board";
    }

    /*
     * helpKey(): the help files are stats-info.txt and stats-help.txt.
     */
    @Override
    public String helpKey()
    {
        return "stats";
    }

    /*
     +--------------------------------------------------------------------------+
     | onShow()                                                                 |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Works the board out again from the data store, so a round saved a        |
     | moment ago is included, refills the band and the rows in the sort        |
     | chosen earlier in this session, keeps the highlighted row if it is       |
     | still there, and puts the hint (or "no rounds yet") in the footer.       |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    @Override
    public void onShow()
    {
        DataStore store = shell.store();
        board = StatsBoard.build(store.roundsByDate(), store.teeMap());
        fillBand();
        fillRows();
        boolean noRounds = board.allEighteen().rounds() == 0 && board.allNine().rounds() == 0;
        shell.setStatus(noRounds ? NO_ROUNDS : FOOTER_HINT);
        list.requestFocusInWindow();
    }

    /*
     * slot(part, width): puts a heading tag or total cell, 3 px smaller than its slot, in the middle of a see-through slot one row tall.
     */
    private static JPanel slot(JComponent part, int width)
    {
        Dimension partSize = new Dimension(width - TAG_INSET, ROW_HEIGHT - TAG_INSET);
        part.setPreferredSize(partSize);
        part.setMinimumSize(partSize);
        JPanel holder = new JPanel(new GridBagLayout());
        holder.setOpaque(false);
        holder.add(part, new GridBagConstraints());
        Dimension slotSize = new Dimension(width, ROW_HEIGHT);
        holder.setPreferredSize(slotSize);
        holder.setMinimumSize(slotSize);
        return holder;
    }

    /*
     +--------------------------------------------------------------------------+
     | fillBand()                                                               |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Lays out the pinned band: the row of heading tags, then the totals of    |
     | all 18-hole rounds and of all nine-hole rounds, both always shown: tan   |
     | cells with a dark brown edge, no rating, the number of rounds (0 if      |
     | none) and dashes where there is nothing to show. The last slot of each   |
     | row is left empty above the scroll bar.                                  |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private void fillBand()
    {
        band.removeAll();
        totalCells.clear();
        List<Row> totals = new ArrayList<>();
        totals.add(board == null ? null : board.allEighteen());
        totals.add(board == null ? null : board.allNine());
        bandRows = 1 + totals.size();
        GridBagConstraints at = new GridBagConstraints();
        for (int col = 0; col < COLUMNS.length; col = col + 1)
        {
            at.gridx = col;
            at.gridy = 0;
            band.add(slot(headings.get(COLUMNS[col]), COLUMN_WIDTHS[col]), at);
        }
        for (int row = 0; row < totals.size(); row = row + 1)
        {
            Row total = totals.get(row);
            for (int col = 0; col < COLUMNS.length; col = col + 1)
            {
                Column column = COLUMNS[col];
                String text = totalText(total, column, row);
                JLabel cell = new JLabel(text, (column == Column.COURSE ? SwingConstants.LEFT : SwingConstants.CENTER));
                cell.setOpaque(true);
                cell.setBackground(TOTAL_FACE);
                cell.setForeground(Theme.FIELD_TEXT);
                cell.setFont(Theme.font(Font.BOLD, CELL_FONT));
                Border edge = BorderFactory.createLineBorder(TOTAL_EDGE, 1);
                cell.setBorder(column == Column.COURSE
                        ? BorderFactory.createCompoundBorder(edge, BorderFactory.createEmptyBorder(0, NAME_PAD - 1, 0, 0)) : edge);
                totalCells.add(cell);
                at.gridx = col;
                at.gridy = row + 1;
                band.add(slot(cell, COLUMN_WIDTHS[col]), at);
            }
        }
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        spacer.setPreferredSize(new Dimension(WoodScrollBarUI.BAR_WIDTH, ROW_HEIGHT));
        at.gridx = COLUMNS.length;
        at.gridy = 0;
        band.add(spacer, at);
        band.revalidate();
        band.repaint();
        frame.revalidate();
    }

    /*
     * totalText(total, column, row): what a total cell shows: its name, nothing under Rating\Slope, the number of rounds (0 if none), and its figures or dashes.
     */
    private static String totalText(Row total, Column column, int row)
    {
        if (column == Column.COURSE)
        {
            return (total == null ? (row == 0 ? StatsBoard.ALL_EIGHTEEN : StatsBoard.ALL_NINE) : total.label());
        }
        if (column == Column.RATING)
        {
            return "";
        }
        if (total == null)
        {
            return (column == Column.ROUNDS ? "0" : "-");
        }
        return total.text(column);
    }

    /*
     +--------------------------------------------------------------------------+
     | fillRows()                                                               |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Fills the list: the 18-hole rows in the chosen order, the "Nine-hole     |
     | rounds" band (always), and the nine-hole rows in the same order, each    |
     | section sorted on its own and with its own gold figures. A section with  |
     | no rows gets a faint watermark row instead, and blank rows fill the      |
     | list up to 12 rows so it looks like a blank sheet.                       |
     | The highlighted row stays highlighted, follows its data to its new       |
     | place, and is scrolled into view.                                        |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private void fillRows()
    {
        list.removeAll();
        rowPanels.clear();
        blankRows.clear();
        watermarks.clear();
        nineBand = null;
        if (board == null)
        {
            return;
        }
        if (board.eighteen().isEmpty())
        {
            boolean noRounds = board.allNine().rounds() == 0;
            list.add(blankRow(noRounds ? NO_ROUNDS_MARK : NO_EIGHTEEN_MARK));
        }
        else
        {
            addSection(board.eighteen());
        }
        nineBand = new JLabel(NINE_BAND, SwingConstants.LEFT);
        nineBand.setOpaque(true);
        nineBand.setBackground(NINE_BAND_FACE);
        nineBand.setForeground(Theme.FIELD_TEXT);
        nineBand.setFont(Theme.font(Font.BOLD, NAME_FONT));
        nineBand.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.GRID_LINE),
                BorderFactory.createEmptyBorder(0, NAME_PAD, 0, 0)));
        fixSize(nineBand, GRID_WIDTH);
        list.add(nineBand);
        if (board.nine().isEmpty())
        {
            list.add(blankRow(NO_NINE_MARK));
        }
        else
        {
            addSection(board.nine());
        }
        while (list.getComponentCount() < MIN_ROWS)
        {
            list.add(blankRow(null));
        }
        if (selectedKey != null && panelFor(selectedKey) == null)
        {
            selectedKey = null;
        }
        list.revalidate();
        list.repaint();
        frame.revalidate();
        showSelected();
    }

    /*
     +--------------------------------------------------------------------------+
     | blankRow(watermark)                                                      |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Makes a row of the blank sheet: ten empty white cells with the same      |
     | hairlines as a course row. With a watermark, its first cell shows that   |
     | text faintly (13 pt italic, light grey). It does not react to the        |
     | mouse, so it cannot be picked.                                           |
     |                                                                          |
     | Passed in:  watermark - the faint text, or null for a blank row          |
     | Passed out: the row                                                      |
     +--------------------------------------------------------------------------+
     */
    private JPanel blankRow(String watermark)
    {
        JPanel row = new JPanel(new GridBagLayout());
        row.setBackground(Color.WHITE);
        Border lines = BorderFactory.createMatteBorder(0, 0, 1, 1, Theme.GRID_LINE);
        GridBagConstraints at = new GridBagConstraints();
        at.fill = GridBagConstraints.BOTH;
        for (int col = 0; col < COLUMNS.length; col = col + 1)
        {
            boolean marked = col == 0 && watermark != null;
            JLabel cell = new JLabel(marked ? watermark : "", SwingConstants.LEFT);
            cell.setOpaque(true);
            cell.setBackground(Color.WHITE);
            cell.setForeground(WATERMARK_TEXT);
            cell.setFont(Theme.font(Font.ITALIC, NAME_FONT));
            cell.setBorder(col == 0 ? BorderFactory.createCompoundBorder(lines, BorderFactory.createEmptyBorder(0, NAME_PAD, 0, 0)) : lines);
            Dimension size = new Dimension(COLUMN_WIDTHS[col], ROW_HEIGHT);
            cell.setPreferredSize(size);
            cell.setMinimumSize(size);
            if (marked)
            {
                watermarks.add(cell);
            }
            at.gridx = col;
            row.add(cell, at);
        }
        fixSize(row, GRID_WIDTH);
        if (watermark == null)
        {
            blankRows.add(row);
        }
        return row;
    }

    /*
     * addSection(rows): adds one section's rows to the list, sorted, with its gold figures.
     */
    private void addSection(List<Row> rows)
    {
        Map<Column, Set<String>> gold = StatsBoard.best(rows);
        for (Row row : StatsBoard.sort(rows, sortColumn, ascending))
        {
            RowPanel panel = new RowPanel(row, gold);
            rowPanels.add(panel);
            list.add(panel);
        }
    }

    /*
     * fixSize(part, width): makes a part exactly one row tall and the given width.
     */
    private static void fixSize(JComponent part, int width)
    {
        Dimension size = new Dimension(width, ROW_HEIGHT);
        part.setPreferredSize(size);
        part.setMinimumSize(size);
        part.setMaximumSize(size);
    }

    /*
     * showHeadingArrows(): puts an up or down arrow on the sorted heading, and none on the others.
     */
    private void showHeadingArrows()
    {
        for (Column column : COLUMNS)
        {
            headings.get(column).setSortArrow(column == sortColumn ? (ascending ? 1 : -1) : 0);
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | sortBy(column)                                                           |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | What clicking a heading does: sorts both sections by that column,        |
     | smallest (or A) first, or reverses the order if the column is already    |
     | the sorted one. The choice is kept in this page for the rest of the      |
     | session. The totals do not move.                                         |
     |                                                                          |
     | Passed in:  column - the column clicked                                  |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    public void sortBy(Column column)
    {
        if (column == sortColumn)
        {
            ascending = !ascending;
        }
        else
        {
            sortColumn = column;
            ascending = true;
        }
        showHeadingArrows();
        fillRows();
    }

    /*
     * sortColumn(): the column the rows are sorted by.
     */
    Column sortColumn()
    {
        return sortColumn;
    }

    /*
     * isAscending(): whether the sort is smallest (or A) first.
     */
    boolean isAscending()
    {
        return ascending;
    }

    /*
     * selectedKey(): the key of the highlighted row (see StatsBoard.Row.key), or null.
     */
    String selectedKey()
    {
        return selectedKey;
    }

    /*
     * panelFor(key): the row on screen with that key, or null.
     */
    RowPanel panelFor(String key)
    {
        for (RowPanel panel : rowPanels)
        {
            if (panel.row.key().equals(key))
            {
                return panel;
            }
        }
        return null;
    }

    /*
     * select(key): highlights one row (any other loses its highlight) and scrolls it into view.
     * Passed in:  key - the row's key, or null for none
     * Passed out: nothing
     */
    public void select(String key)
    {
        selectedKey = key;
        showSelected();
    }

    /*
     * showSelected(): gives every row its colours for the current highlight and scrolls the highlighted row into view.
     */
    private void showSelected()
    {
        for (RowPanel panel : rowPanels)
        {
            panel.colour(panel.row.key().equals(selectedKey));
        }
        RowPanel chosen = (selectedKey == null ? null : panelFor(selectedKey));
        if (chosen != null)
        {
            int place = 0;
            for (Component part : list.getComponents())
            {
                if (part == chosen)
                {
                    break;
                }
                place = place + 1;
            }
            list.scrollRectToVisible(new Rectangle(0, place * ROW_HEIGHT, GRID_WIDTH, ROW_HEIGHT));
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | moveSelection(step)                                                      |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | What the Up and Down keys do: moves the highlight to the row above or    |
     | below, passing over the "Nine-hole rounds" band and stopping at the      |
     | first and last rows. With nothing highlighted it starts at the first     |
     | row.                                                                     |
     |                                                                          |
     | Passed in:  step - -1 for up, 1 for down                                 |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    public void moveSelection(int step)
    {
        if (rowPanels.isEmpty())
        {
            return;
        }
        RowPanel current = (selectedKey == null ? null : panelFor(selectedKey));
        int place = (current == null ? 0 : rowPanels.indexOf(current) + step);
        place = Math.max(0, Math.min(rowPanels.size() - 1, place));
        select(rowPanels.get(place).row.key());
    }

    /*
     * openSelected(): what Enter does: opens the Rounds screen for the highlighted row, if there is one.
     */
    public void openSelected()
    {
        RowPanel current = (selectedKey == null ? null : panelFor(selectedKey));
        if (current != null)
        {
            open(current.row);
        }
    }

    /*
     * open(row): opens the Rounds screen on the newest round of the row's course and tees, with the Show box set to them.
     */
    private void open(Row row)
    {
        shell.browseRoundsOn(row.teeId());
    }

    /*
     +--------------------------------------------------------------------------+
     | RowList                                                                  |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The white list of rows in the scroll pane: rows 25 px tall, stacked in   |
     | a one-column grid so every kind of row starts at the left edge at the    |
     | grid's full width, scrolling a row at a time with the arrows and the   |
     | mouse wheel. It can take the cursor so the Up, Down and Enter keys       |
     | reach the page.                                                          |
     +--------------------------------------------------------------------------+
     */
    final class RowList extends JPanel implements Scrollable
    {
        /*
         * RowList(): an empty white list.
         */
        RowList()
        {
            setLayout(new GridLayout(0, 1));
            setBackground(Color.WHITE);
            setFocusable(true);
            addMouseListener(new MouseAdapter()
            {
                @Override
                public void mousePressed(MouseEvent event)
                {
                    requestFocusInWindow();
                }
            });
        }

        /*
         * getPreferredSize(): the grid's width and 25 px for every row.
         */
        @Override
        public Dimension getPreferredSize()
        {
            return new Dimension(GRID_WIDTH, getComponentCount() * ROW_HEIGHT);
        }

        /*
         * getPreferredScrollableViewportSize(): as much as the list wants; the scroll pane limits it.
         */
        @Override
        public Dimension getPreferredScrollableViewportSize()
        {
            return getPreferredSize();
        }

        /*
         * getScrollableUnitIncrement(...): one row per click of an arrow or the wheel.
         */
        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction)
        {
            return ROW_HEIGHT;
        }

        /*
         * getScrollableBlockIncrement(...): a whole page of rows when the track is clicked.
         */
        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction)
        {
            return Math.max(ROW_HEIGHT, (visible.height / ROW_HEIGHT) * ROW_HEIGHT);
        }

        /*
         * getScrollableTracksViewportWidth(): always as wide as the view, so there is never a sideways scroll bar.
         */
        @Override
        public boolean getScrollableTracksViewportWidth()
        {
            return true;
        }

        /*
         * getScrollableTracksViewportHeight(): taller than the view when there are many rows, so it scrolls.
         */
        @Override
        public boolean getScrollableTracksViewportHeight()
        {
            return false;
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | RowPanel                                                                 |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | One course row: ten white cells with 1 px hairlines under and right of   |
     | each, the name left aligned in 13 pt, the figures centred in 14 pt,      |
     | gold (bold) for a best figure. Highlighted, every cell is cream          |
     | (gold cells stay gold) and the row has a 2 px gold outline. A click      |
     | highlights it; a double-click opens its rounds.                          |
     +--------------------------------------------------------------------------+
     */
    final class RowPanel extends JPanel
    {
        final Row row;
        final JLabel[] cells = new JLabel[COLUMNS.length];
        final boolean[] best = new boolean[COLUMNS.length];
        private boolean highlighted = false;

        /*
         * RowPanel(row, gold): builds the row's cells and marks its gold figures.
         * Passed in:  row - the row's data, gold - the section's gold cells by column
         * Passed out: the row
         */
        RowPanel(Row row, Map<Column, Set<String>> gold)
        {
            super(new GridBagLayout());
            this.row = row;
            setOpaque(true);
            setBackground(Color.WHITE);
            Border lines = BorderFactory.createMatteBorder(0, 0, 1, 1, Theme.GRID_LINE);
            GridBagConstraints at = new GridBagConstraints();
            at.fill = GridBagConstraints.BOTH;
            for (int col = 0; col < COLUMNS.length; col = col + 1)
            {
                Column column = COLUMNS[col];
                boolean name = column == Column.COURSE;
                Set<String> goldKeys = gold.get(column);
                best[col] = goldKeys != null && goldKeys.contains(row.key());
                JLabel cell = new JLabel(row.text(column), (name ? SwingConstants.LEFT : SwingConstants.CENTER));
                cell.setOpaque(true);
                cell.setForeground(Theme.FIELD_TEXT);
                cell.setFont(Theme.font(best[col] ? Font.BOLD : Font.PLAIN, (name ? NAME_FONT : CELL_FONT)));
                cell.setBorder(name ? BorderFactory.createCompoundBorder(lines, BorderFactory.createEmptyBorder(0, NAME_PAD, 0, 0)) : lines);
                Dimension size = new Dimension(COLUMN_WIDTHS[col], ROW_HEIGHT);
                cell.setPreferredSize(size);
                cell.setMinimumSize(size);
                cells[col] = cell;
                at.gridx = col;
                add(cell, at);
            }
            fixSize(this, GRID_WIDTH);
            colour(false);
            addMouseListener(new MouseAdapter()
            {
                @Override
                public void mousePressed(MouseEvent event)
                {
                    list.requestFocusInWindow();
                    select(row.key());
                }

                @Override
                public void mouseClicked(MouseEvent event)
                {
                    if (event.getClickCount() == 2)
                    {
                        open(row);
                    }
                }
            });
        }

        /*
         * colour(selected): white cells (gold for a best figure), or cream ones with the gold outline when the row is highlighted.
         */
        void colour(boolean selected)
        {
            highlighted = selected;
            for (int col = 0; col < cells.length; col = col + 1)
            {
                cells[col].setBackground(best[col] ? BEST_FACE : (selected ? Theme.FIELD_FOCUS : Color.WHITE));
            }
            repaint();
        }

        /*
         * isHighlighted(): whether this row is the highlighted one.
         */
        boolean isHighlighted()
        {
            return highlighted;
        }

        /*
         * paint(graphics): the cells, then the 2 px gold outline on top when highlighted.
         */
        @Override
        public void paint(Graphics graphics)
        {
            super.paint(graphics);
            if (highlighted)
            {
                Graphics2D canvas = (Graphics2D) graphics.create();
                canvas.setColor(Theme.GOLD);
                canvas.setStroke(new BasicStroke(2f));
                canvas.drawRect(1, 1, getWidth() - 2, getHeight() - 2);
                canvas.dispose();
            }
        }
    }
}
