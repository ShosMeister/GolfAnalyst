package golfanalyst;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/*
 +--------------------------------------------------------------------------+
 | ZipCodes                                                                 |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | Looks up a US zip code's city and state, so the New Course screen can    |
 | fill them in and warn when they do not match. The list is zipcodes.tsv   |
 | in the program folder (columns Zip, City, State; built from the GeoNames |
 | US postal file). It is read once, the first time it is needed. If the    |
 | file is missing or cannot be read the list is empty and every check      |
 | quietly does nothing. Cities are compared ignoring capitals and extra    |
 | spaces.                                                                  |
 |                                                                          |
 | Nested types:                                                            |
 | Place   - a zip's city and state                                         |
 | Verdict - what a check found                                             |
 | Check   - a verdict, the zip's place and the words for the footer        |
 |                                                                          |
 | Public Methods:                                                          |
 | standard()             - the program's list, read once from zipcodes.tsv |
 | load(file)             - a list read from a file (empty if it cannot be) |
 | find(zip)              - the place for a zip, or null                    |
 | check(zip, city, state) - compares a typed city and state with the list  |
 +--------------------------------------------------------------------------+
 */
public final class ZipCodes
{

    public static final String FILE = "zipcodes.tsv";
    private static ZipCodes standard = null;
    private final Map<String, Place> places;

    /*
     * Place: the city and state a zip belongs to.
     */
    public record Place(String city, String state)
    {
    }

    /*
     * Verdict: NONE (nothing to say: no list, or the zip is not complete), FILL (city and state are both empty: fill them in), MATCH, STATE_DIFFERS, CITY_DIFFERS, UNKNOWN (the zip is not in the list).
     */
    public enum Verdict
    {
        NONE, FILL, MATCH, STATE_DIFFERS, CITY_DIFFERS, UNKNOWN
    }

    /*
     * Check: what a check found, the zip's place (null when it is not in the list) and the words for the footer ("" when there is nothing to say).
     */
    public record Check(Verdict verdict, Place place, String message)
    {
    }

    /*
     * ZipCodes(places): a list of zips.
     */
    private ZipCodes(Map<String, Place> places)
    {
        this.places = places;
    }

    /*
     * standard(): the program's own list, read from zipcodes.tsv in the program folder the first time it is asked for.
     * Passed in:  nothing
     * Passed out: the list (empty if the file is missing or unreadable)
     */
    public static synchronized ZipCodes standard()
    {
        if (standard == null)
        {
            standard = load(ProgramFolders.active().programFile(FILE));
        }
        return standard;
    }

    /*
     +--------------------------------------------------------------------------+
     | load(file)                                                               |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Reads a list of zips: tab separated, a header row with Zip, City and     |
     | State (found by name), one row per zip. Rows without a 5-digit zip and   |
     | a two-letter state are passed over. A missing or unreadable file gives   |
     | an empty list, never an error.                                           |
     |                                                                          |
     | Passed in:  file - the list                                              |
     | Passed out: the list                                                     |
     +--------------------------------------------------------------------------+
     */
    public static ZipCodes load(Path file)
    {
        Map<String, Place> places = new HashMap<>();
        try
        {
            if (!Files.isRegularFile(file))
            {
                return new ZipCodes(places);
            }
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            int zipCol = -1;
            int cityCol = -1;
            int stateCol = -1;
            for (String line : lines)
            {
                String clean = (line.startsWith("﻿") ? line.substring(1) : line);
                if (clean.isBlank() || clean.trim().startsWith("#"))
                {
                    continue;
                }
                String[] cells = clean.split("\t", -1);
                if (zipCol < 0)
                {
                    for (int col = 0; col < cells.length; col = col + 1)
                    {
                        String name = DataLoader.normalize(cells[col]);
                        zipCol = (name.equals("zip") ? col : zipCol);
                        cityCol = (name.equals("city") ? col : cityCol);
                        stateCol = (name.equals("state") ? col : stateCol);
                    }
                    if (zipCol < 0 || cityCol < 0 || stateCol < 0)
                    {
                        return new ZipCodes(new HashMap<>());
                    }
                    continue;
                }
                if (cells.length <= Math.max(zipCol, Math.max(cityCol, stateCol)))
                {
                    continue;
                }
                String zip = cells[zipCol].trim();
                String state = cells[stateCol].trim().toUpperCase(Locale.ROOT);
                if (zip.matches("\\d{5}") && state.matches("[A-Z]{2}"))
                {
                    places.putIfAbsent(zip, new Place(cells[cityCol].trim(), state));
                }
            }
        }
        catch (IOException | RuntimeException unreadable)
        {
            places.clear();
        }
        return new ZipCodes(places);
    }

    /*
     * isEmpty(): whether the list has no zips (the file was missing or unreadable).
     */
    public boolean isEmpty()
    {
        return places.isEmpty();
    }

    /*
     * find(zip): the city and state of a zip, or null if it is not in the list.
     */
    public Place find(String zip)
    {
        return places.get(zip.trim());
    }

    /*
     * sameName(text): a name with capitals and extra spaces ignored.
     */
    private static String sameName(String text)
    {
        return text.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /*
     +--------------------------------------------------------------------------+
     | check(zip, city, state)                                                  |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Compares a typed city and state with the list. NONE when there is no     |
     | list or the zip is not 5 digits. UNKNOWN when the zip is not in the      |
     | list. FILL when both city and state are empty. STATE_DIFFERS when a      |
     | state is typed and is not the zip's ("ZIP 29708 is in SC, but the state  |
     | says NC."). CITY_DIFFERS when only the city differs (a hint: the course  |
     | may be in a nearby town). Otherwise MATCH.                               |
     |                                                                          |
     | Passed in:  zip, city, state - as typed                                  |
     | Passed out: the Check                                                    |
     +--------------------------------------------------------------------------+
     */
    public Check check(String zip, String city, String state)
    {
        String typedZip = zip.trim();
        if (places.isEmpty() || !typedZip.matches("\\d{5}"))
        {
            return new Check(Verdict.NONE, null, "");
        }
        Place place = places.get(typedZip);
        if (place == null)
        {
            return new Check(Verdict.UNKNOWN, null, "That ZIP code is not in my list (it may be new). Please check it.");
        }
        if (city.isBlank() && state.isBlank())
        {
            return new Check(Verdict.FILL, place, "");
        }
        if (!state.isBlank() && !state.trim().equalsIgnoreCase(place.state()))
        {
            return new Check(Verdict.STATE_DIFFERS, place, "ZIP " + typedZip + " is in " + place.state() + ", but the state says "
                    + state.trim().toUpperCase(Locale.ROOT) + ".");
        }
        if (!city.isBlank() && !sameName(city).equals(sameName(place.city())))
        {
            return new Check(Verdict.CITY_DIFFERS, place, "ZIP " + typedZip + " is listed under " + place.city()
                    + ". That is fine if the course is in a nearby town.");
        }
        return new Check(Verdict.MATCH, place, "");
    }

    /*
     * saveQuestion(zip, city, state): the question Save asks when the state differs: "ZIP 29708 is Fort Mill, SC, but you entered Charlotte, NC. Save anyway?"
     * Passed in:  zip, city, state - as typed (the zip must be in the list)
     * Passed out: the question
     */
    public String saveQuestion(String zip, String city, String state)
    {
        Place place = places.get(zip.trim());
        String entered = (city.isBlank() ? state.trim() : city.trim() + ", " + state.trim());
        return "ZIP " + zip.trim() + " is " + place.city() + ", " + place.state() + ", but you entered " + entered + ". Save anyway?";
    }
}
