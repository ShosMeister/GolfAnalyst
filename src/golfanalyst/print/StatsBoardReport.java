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
 | (StatsBoard's own text for every cell). First the two tan total rows,    |
 | "All 18-hole rounds" and "All nine-hole rounds", then a row per tee for  |
 | 18-hole rounds, then a "Nine-hole rounds" band and the nine-hole rows.   |
 | The best figure of a column among rows with 2 or more rounds is shaded   |
 | (255,240,190) where the screen shows gold, each section on its own.      |
 | Rows are 24 high from y 170; every page repeats the header row.          |
 |                                                                          |
 | Public Methods:                                                          |
 | the Report methods; board(); table(); rowsPerPage(height)                |
 +--------------------------------------------------------------------------+
 */
public final class StatsBoardReport extends Report
{

    public static final String NAME = "Stats Board";
    public static final String NINE_BAND = "Nine-hole rounds";
    static final double TABLE_TOP = 170;
    static final double ROW_HEIGHT = 24;
    private static final double[] WIDTHS = {266, 96, 62, 62, 62, 62, 62, 62, 62, 62};
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
     * row(row, kind, best): one board row as a table row, its best figures shaded.
     */
    private static Table.Row row(StatsBoard.Row row, Table.Kind kind, Map<StatsBoard.Column, Set<String>> best)
    {
        List<String> cells = new ArrayList<>();
        List<Integer> shaded = new ArrayList<>();
        StatsBoard.Column[] columns = StatsBoard.Column.values();
        for (int col = 0; col < columns.length; col = col + 1)
        {
            cells.add(row.text(columns[col]));
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
            columns.add(new Table.Column(boardColumns[col].heading(), WIDTHS[col], align));
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
            header.add(column.heading());
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
                String words = boardRow.text(column);
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
