package golfanalyst;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/*
 +--------------------------------------------------------------------------+
 | Settings                                                                 |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | The golfer's settings, kept in settings.txt in the data folder (a        |
 | properties file: Key=value lines). Keys are READ ignoring capitals, so   |
 | an old lowercase "golfer" line is still understood, and every key is     |
 | WRITTEN back in PascalCase: Golfer, IncludeHistorical, BackupsToKeep,    |
 | BackupCopyFolder, LastExportFolder. A key the program does not know is   |
 | kept as it was. A missing key means the default. The file is written     |
 | through a temporary file, so it is never left half written.              |
 |                                                                          |
 | Constants:                                                               |
 | FILE                    - "settings.txt"                                 |
 | GOLFER ... LAST_EXPORT_FOLDER - the keys                                 |
 | KEEP_ALL, DEFAULT_KEEP, KEEP_CHOICES - Backups to keep (0 means All)     |
 |                                                                          |
 | Public Methods:                                                          |
 | load(dataDir), save(dataDir) - read and write settings.txt               |
 | get(key), with(key, value)   - one value; with "" removes it             |
 | golfer(), includeHistorical(), backupsToKeep(), backupCopyFolder(),      |
 | lastExportFolder()           - the values, with their defaults           |
 +--------------------------------------------------------------------------+
 */
public final class Settings
{

    public static final String FILE = "settings.txt";
    public static final String GOLFER = "Golfer";
    public static final String INCLUDE_HISTORICAL = "IncludeHistorical";
    public static final String BACKUPS_TO_KEEP = "BackupsToKeep";
    public static final String BACKUP_COPY_FOLDER = "BackupCopyFolder";
    public static final String LAST_EXPORT_FOLDER = "LastExportFolder";
    public static final int KEEP_ALL = 0;
    public static final int DEFAULT_KEEP = 20;
    public static final List<Integer> KEEP_CHOICES = List.of(10, 20, 50, 100, KEEP_ALL);
    private static final List<String> KNOWN = List.of(GOLFER, INCLUDE_HISTORICAL, BACKUPS_TO_KEEP, BACKUP_COPY_FOLDER,
            LAST_EXPORT_FOLDER);
    private final Map<String, String> values;

    /*
     * Settings(values): settings holding these values (keys already in their written form).
     */
    private Settings(Map<String, String> values)
    {
        this.values = values;
    }

    /*
     * canonical(key): a key in the form it is written: the PascalCase name for a key the program knows (matched ignoring capitals), otherwise the key as it was.
     */
    private static String canonical(String key)
    {
        for (String known : KNOWN)
        {
            if (known.equalsIgnoreCase(key.trim()))
            {
                return known;
            }
        }
        return key.trim();
    }

    /*
     +--------------------------------------------------------------------------+
     | load(dataDir)                                                            |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Reads settings.txt from the data folder. A missing or unreadable file    |
     | gives empty settings (every value its default).                          |
     |                                                                          |
     | Passed in:  dataDir - the data folder                                    |
     | Passed out: the settings                                                 |
     +--------------------------------------------------------------------------+
     */
    public static Settings load(Path dataDir)
    {
        Map<String, String> values = new LinkedHashMap<>();
        Path file = dataDir.resolve(FILE);
        if (Files.exists(file))
        {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8))
            {
                Properties read = new Properties();
                read.load(reader);
                for (String key : read.stringPropertyNames())
                {
                    values.put(canonical(key), read.getProperty(key));
                }
            }
            catch (IOException | IllegalArgumentException unreadable)
            {
                values.clear();
            }
        }
        return new Settings(values);
    }

    /*
     +--------------------------------------------------------------------------+
     | save(dataDir)                                                            |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Writes settings.txt: a comment line, the known keys in PascalCase in a   |
     | fixed order, then any other keys, each escaped the way properties files  |
     | need (backslashes in Windows paths, for example). Written through a      |
     | temporary file.                                                          |
     |                                                                          |
     | Passed in:  dataDir - the data folder                                    |
     | Passed out: nothing (throws IOException if it cannot be written)         |
     +--------------------------------------------------------------------------+
     */
    public void save(Path dataDir) throws IOException
    {
        StringBuilder text = new StringBuilder("#Golf Analyst settings\n");
        for (String key : KNOWN)
        {
            if (values.containsKey(key))
            {
                text.append(line(key, values.get(key)));
            }
        }
        for (Map.Entry<String, String> entry : values.entrySet())
        {
            if (!KNOWN.contains(entry.getKey()))
            {
                text.append(line(entry.getKey(), entry.getValue()));
            }
        }
        Files.createDirectories(dataDir);
        LineReplacer.writeSafely(dataDir.resolve(FILE), text.toString());
    }

    /*
     * line(key, value): one escaped "Key=value" line with its line break, as a properties file writes it.
     */
    private static String line(String key, String value)
    {
        Properties one = new Properties();
        one.setProperty(key, value);
        StringWriter writer = new StringWriter();
        try
        {
            one.store(writer, null);
        }
        catch (IOException never)
        {
            return key + "=" + value + "\n";
        }
        StringBuilder kept = new StringBuilder();
        for (String text : writer.toString().split("\\R"))
        {
            if (!text.startsWith("#") && !text.isEmpty())
            {
                kept.append(text).append('\n');
            }
        }
        return kept.toString();
    }

    /*
     * get(key): a value as written, or "" when it is missing (the key is matched ignoring capitals).
     */
    public String get(String key)
    {
        return values.getOrDefault(canonical(key), "");
    }

    /*
     * with(key, value): sets a value (the key is written in PascalCase); an empty value removes the key, so its default is used.
     * Passed in:  key, value - the setting
     * Passed out: these settings, changed
     */
    public Settings with(String key, String value)
    {
        if (value == null || value.isEmpty())
        {
            values.remove(canonical(key));
        }
        else
        {
            values.put(canonical(key), value);
        }
        return this;
    }

    /*
     * golfer(): the golfer's name, or "".
     */
    public String golfer()
    {
        return get(GOLFER);
    }

    /*
     * includeHistorical(): the Home screen's Include historical rounds box: ticked unless the setting says false.
     */
    public boolean includeHistorical()
    {
        return !get(INCLUDE_HISTORICAL).trim().equalsIgnoreCase("false");
    }

    /*
     * backupsToKeep(): how many everyday backups of each kind to keep: 10, 20, 50 or 100, or KEEP_ALL for All; DEFAULT_KEEP when missing or not understood.
     */
    public int backupsToKeep()
    {
        String text = get(BACKUPS_TO_KEEP).trim();
        if (text.equalsIgnoreCase("all"))
        {
            return KEEP_ALL;
        }
        try
        {
            int count = Integer.parseInt(text);
            return (count > 0 ? count : DEFAULT_KEEP);
        }
        catch (NumberFormatException notNumber)
        {
            return DEFAULT_KEEP;
        }
    }

    /*
     * keepText(count): how a Backups to keep choice is written and shown: "All" for KEEP_ALL, otherwise the number.
     */
    public static String keepText(int count)
    {
        return (count == KEEP_ALL ? "All" : Integer.toString(count));
    }

    /*
     * backupCopyFolder(): the folder every backup is also copied to, or null when it is not set.
     */
    public Path backupCopyFolder()
    {
        String text = get(BACKUP_COPY_FOLDER).trim();
        return (text.isEmpty() ? null : Path.of(text));
    }

    /*
     * lastExportFolder(): the folder Export last wrote into, or null.
     */
    public Path lastExportFolder()
    {
        String text = get(LAST_EXPORT_FOLDER).trim();
        return (text.isEmpty() ? null : Path.of(text));
    }

    /*
     * keys(): the keys as they would be written, for the self-test.
     */
    List<String> keys()
    {
        return List.copyOf(values.keySet());
    }
}
