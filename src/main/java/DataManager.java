import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataManager {

    public final String storagePath;

    private final Map<Long, String> dataMap;

    // Added field: Map holding individual guild quotes in memory (GuildID -> (QuoteID -> QuoteText))
    // Left completely empty on startup to prevent unnecessary disk I/O
    private final Map<Long, Map<Integer, String>> quoteLibrary;

    public DataManager(String storagePath) {
        this.storagePath = storagePath;

        this.dataMap = loadOnStartup();
        this.quoteLibrary = new ConcurrentHashMap<>();
    }

    public void onDataReceived(Long id, String payload){
        dataMap.put(id, payload);
        saveData();
    }

    public synchronized void saveData(){
        try (FileOutputStream fileOut = new FileOutputStream(storagePath);
             ObjectOutputStream objectOut = new ObjectOutputStream(fileOut)) {

            // ConcurrentHashMap is also fully Serializable
            objectOut.writeObject(dataMap);

        } catch (IOException e) {
            System.err.println("Failed to persist dynamic data: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<Long, String> loadOnStartup() {
        File file = new File(storagePath);
        if (!file.exists()) {
            return new ConcurrentHashMap<>();
        }
        try (FileInputStream fileIn = new FileInputStream(file);
             ObjectInputStream objectIn = new ObjectInputStream(fileIn)) {
            System.out.println("Loaded data");
            return (Map<Long, String>) objectIn.readObject();

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Could not load historic data, initializing empty map: " + e.getMessage());
            return new ConcurrentHashMap<>();
        }

    }

    public Map<Long, String> getDataMap() {
        return this.dataMap;
    }

    // =========================================================================
    // NEW GUILD QUOTE HANDLING METHODS (Lazy loaded on interaction only)
    // =========================================================================

    /**
     * Safely retrieves or dynamically loads the quote library for a specific guild.
     */
    public Map<Integer, String> getGuildQuotes(Long guildId) {
        return quoteLibrary.computeIfAbsent(guildId, this::loadGuildQuotesFromDisk);
    }

    /**
     * Saves a specific quote to a guild's library and commits it immediately to disk.
     */
    public void saveQuote(Long guildId, Integer quoteId, String quoteContent) {
        Map<Integer, String> guildQuotes = getGuildQuotes(guildId);
        guildQuotes.put(quoteId, quoteContent);
        persistGuildQuotesToDisk(guildId, guildQuotes);
    }

    private File getGuildFile(Long guildId) {
        return new File("guild_" + guildId + ".ser");
    }

    private void persistGuildQuotesToDisk(Long guildId, Map<Integer, String> guildQuotes) {
        synchronized (String.valueOf(guildId).intern()) {
            File guildFile = getGuildFile(guildId);
            try (FileOutputStream fileOut = new FileOutputStream(guildFile);
                 ObjectOutputStream objectOut = new ObjectOutputStream(fileOut)) {
                objectOut.writeObject(guildQuotes);
            } catch (IOException e) {
                System.err.println("Failed to save quotes for guild " + guildId + ": " + e.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, String> loadGuildQuotesFromDisk(Long guildId) {
        File guildFile = getGuildFile(guildId);
        if (!guildFile.exists()) {
            return new ConcurrentHashMap<>();
        }

        synchronized (String.valueOf(guildId).intern()) {
            try (FileInputStream fileIn = new FileInputStream(guildFile);
                 ObjectInputStream objectIn = new ObjectInputStream(fileIn)) {
                System.out.println("Interaction triggered disk read: Loaded quotes for guild " + guildId);
                return (Map<Integer, String>) objectIn.readObject();
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Corrupted quote file for guild " + guildId + ". Resetting: " + e.getMessage());
                return new ConcurrentHashMap<>();
            }
        }
    }
}