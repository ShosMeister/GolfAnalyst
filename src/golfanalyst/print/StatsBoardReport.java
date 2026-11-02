package golfanalyst.print;

import golfanalyst.StatsBoard;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 +--------------------------------------------------------------------------+
 | StatsBoardReport                                                         |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | The Stats Board on paper (on its side): the screen's columns (Course :   |
 | Tee Box 266, Rating\Slope 96, then Rnds, Avg, Best, Fwy %, GIR %,        |
 | 2-Putt %, Putts/Rnd and Sand % at 62 each), its rows and its rounding    |
 | (StatsBoard's own text for every figure), the widths scaled up in        |
 | proportion so the table fills the 980 units between the margins. First   |
 | the two tan total rows,                                                  |
 | "All 18-hole rounds" and "All nine-hole rounds", then a row per tee for  |
 | 18-hole rounds, then a "Nine-hole rounds" band and the nine-hole rows.   |
 | The cells the screen shows gold are shaded (255,240,190): the very same  |
 | StatsBoard.best, each section on its own (Avg, Best and Putts/Rnd lowest,|
 | the four percentages highest, among rows with 2 or more rounds when 2 or |
 | more such rows exist; ties as shown all count; "-" never).               |
 | Rows are 24 high from y 170; every page repeats the header row. The      |
 | names follow the other printed reports, not the screen: the headings     |
 | "Course (tee)" and "Rating / slope", the rows "Edgewater (Senior)" and   |
 | "Sample Links (White), front nine" (spreadsheet files too).              |
 |                                                                          |
 | Public Methods:                                                          |
 | the Report methods; board(); table(); rowsPerPage(height)                |
 +--------------------------------------------------------------------------+
 */
public final class StatsBoardReport extends Report
{

    public static final String NAME = "Stats Board";
    public static final String NINE_BAND = "Nine-hole rounds";
    public static final String COURSE_HEADING = "Course (tee)";
    public static final String RATING_HEADING = "Rating / slope";
    static final double TABLE_TOP = 170;
    static final double ROW_HEIGHT = 24;
    private static final double[] WIDTHS = {266, 96, 62, 62, 62, 62, 62, 62, 62, 62};
    static final double SCREEN_WIDTH = 858;
    static final double ACROSS = Sheet.LETTER_LONG - Sheet.MARGIN - Sheet.MARGIN;
    private final boolean includeHistorical;
    private final StatsBoard.Board board;

    /*
     * StatsBoardReport(data, includeHistorical): the board of the data, with or without historical rounds.
     */
    public StatsBoardReport(ReportData data, boolean includeHistorical)
    {
        super(data);
        this.includeHistorical = includeHistorical;
        this.board = StatsBoard.build(data.rounds(), data.tees(), includeHistorical);
    }

    /*
     * board(): the figures printed.
     */
    public StatsBoard.Board board()
    {
        return board;
    }

    /*
     * name(): "Stats Board".
     */
    @Override
    public String name()
    {
        return NAME;
    }

    /*
     * landscape(): printed on its side.
     */
    @Override
    public boolean landscape()
    {
        return true;
    }

    /*
     * isEmpty(): true when there are no rows (no rounds).
     */
    @Override
    public boolean isEmpty()
    {
        return board.eighteen().isEmpty() && board.nine().isEmpty();
    }

    /*
     * describe(): "the Stats Board: 12 rows".
     */
    @Override
    public String describe()
    {
        int rows = board.eighteen().size() + board.nine().size();
        if (rows == 1)
        {
            return "the Stats Board: 1 row";
        }
        return "the Stats Board: " + rows + " rows";
    }

    /*
     * heading(): "Stats Board".
     */
    @Override
    protected String heading()
    {
        return NAME;
    }

    /*
     * subtitle(page): the name and whether historical rounds are included.
     */
    @Override
    protected String subtitle(int page)
    {
        if (includeHistorical)
        {
            return named("historical rounds included");
        }
        return named("historical rounds not included");
    }

    /*
     * heading(column): a column's printed heading: "Course (tee)" and "Rating / slope", the others as on the screen.
     */
    static String heading(StatsBoard.Column column)
    {
        if (column == StatsBoard.Column.COURSE)
        {
            return COURSE_HEADING;
        }
        if (column == StatsBoard.Column.RATING)
        {
            return RATING_HEADING;
        }
        return column.heading();
    }

    /*
     +--------------------------------------------------------------------------+
     | label(row)                                                               |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | A row's printed name, as the other reports name tees: "Edgewater         |
     | (Senior)", "Sample Links (White), front nine" or ", back nine" for a     |
     | nine of an 18-hole tee. Total rows keep their own names.                 |
     |                                                                          |
     | Passed in:  row - a board row                                            |
     | Passed out: the name                                                     |
     +--------------------------------------------------------------------------+
     */
    String label(StatsBoard.Row row)
    {
        golfanalyst.Tee tee = data.tees().get(row.teeId());
        if (tee == null)
        {
            return row.label();
        }
        if (row.part() == StatsBoard.Part.FRONT)
        {
            return tee.label() + ", front nine";
        }
        if (row.part() == StatsBoard.Part.BACK)
        {
            return tee.label() + ", back nine";
        }
        return tee.label();
    }

    /*
     * cell(row, column): a row's printed words in a column: its name in the first, the screen's text in the others.
     */
    private String cell(StatsBoard.Row row, StatsBoard.Column column)
    {
        if (column == StatsBoard.Column.COURSE)
        {
            return label(row);
        }
        return row.text(column);
    }

    /*
     * row(row, kind, best): one board row as a table row, its best figures shaded.
     */
    private Table.Row row(StatsBoard.Row row, Table.Kind kind, Map<StatsBoard.Column, Set<String>> best)
    {
        List<String> cells = new ArrayList<>();
        List<Integer> shaded = new ArrayList<>();
        StatsBoard.Column[] columns = StatsBoard.Column.values();
        for (int col = 0; col < columns.length; col = col + 1)
        {
            cells.add(cell(row, columns[col]));
            Set<String> keys = best.get(columns[col]);
            if (keys != null && keys.contains(row.key()))
            {
                shaded.add(col);
            }
        }
        return new Table.Row(cells, kind, shaded);
    }

    /*
     +--------------------------------------------------------------------------+
     | table()                                                                  |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The whole board as one table (see the class comment for the order).      |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: the table                                                    |
     +--------------------------------------------------------------------------+
     */
    public Table table()
    {
        List<Table.Column> columns = new ArrayList<>();
        StatsBoard.Column[] boardColumns = StatsBoard.Column.values();
        for (int col = 0; col < boardColumns.length; col = col + 1)
        {
            Table.Align align = Table.Align.CENTRE;
            if (col == 0)
            {
                align = Table.Align.LEFT;
            }
            columns.add(new Table.Column(heading(boardColumns[col]), WIDTHS[col] * ACROSS / SCREEN_WIDTH, align));
        }
        Table table = new Table(columns, ROW_HEIGHT);
        table.add(row(board.allEighteen(), Table.Kind.TOTAL, Map.of()));
        table.add(row(board.allNine(), Table.Kind.TOTAL, Map.of()));
        Map<StatsBoard.Column, Set<String>> bestEighteen = StatsBoard.best(board.eighteen());
        for (StatsBoard.Row row : board.eighteen())
        {
            table.add(row(row, Table.Kind.NORMAL, bestEighteen));
        }
        table.add(new Table.Row(List.of(NINE_BAND), Table.Kind.GROUP));
        Map<StatsBoard.Column, Set<String>> bestNine = StatsBoard.best(board.nine());
        for (StatsBoard.Row row : board.nine())
        {
            table.add(row(row, Table.Kind.NORMAL, bestNine));
        }
        return table;
    }

    /*
     +--------------------------------------------------------------------------+
     | spreadsheet()                                                            |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The board for a spreadsheet file in the screen's columns: the two total  |
     | rows, the 18-hole rows and the nine-hole rows (no band row), each cell   |
     | as the screen shows it, with "-" left empty.                             |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: the header row and one row per board row                     |
     +--------------------------------------------------------------------------+
     */
    @Override
    public List<List<String>> spreadsheet()
    {
        List<List<String>> rows = new ArrayList<>();
        List<String> header = new ArrayList<>();
        for (StatsBoard.Column column : StatsBoard.Column.values())
        {
            header.add(heading(column));
        }
        rows.add(header);
        List<StatsBoard.Row> boardRows = new ArrayList<>();
        boardRows.add(board.allEighteen());
        boardRows.add(board.allNine());
        boardRows.addAll(board.eighteen());
        boardRows.addAll(board.nine());
        for (StatsBoard.Row boardRow : boardRows)
        {
            List<String> cells = new ArrayList<>();
            for (StatsBoard.Column column : StatsBoard.Column.values())
            {
                String words = cell(boardRow, column);
                if (words.equals("-"))
                {
                    words = "";
                }
                cells.add(words);
            }
            rows.add(cells);
        }
        return rows;
    }

    /*
     * rowsPerPage(height): how many rows fit on a page this high under the header row (at least 1).
     */
    public int rowsPerPage(double height)
    {
        return Math.max(1, table().rowsThatFit(bodyBottom(height) - TABLE_TOP));
    }

    /*
     * pageCount(height): how many pages the rows take.
     */
    @Override
    public int pageCount(double height)
    {
        int perPage = rowsPerPage(height);
        return Math.max(1, (table().rows().size() + perPage - 1) / perPage);
    }

    /*
     * paintBody(sheet, page, pages): the header row and this page's rows.
     */
    @Override
    protected void paintBody(Sheet sheet, int page, int pages)
    {
        Table table = table();
        int perPage = rowsPerPage(sheet.height());
        int first = Math.min(table.rows().size(), page * perPage);
        int last = Math.min(table.rows().size(), first + perPage);
        table.draw(sheet, Sheet.MARGIN, TABLE_TOP, first, last);
    }
}
