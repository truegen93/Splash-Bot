import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.awt.Color;

public class Scraper {

    // A structured internal data container class to store the parsed details cleanly
    public static class TwitterProfile {
        public String username;
        public String displayName;
        public String bio;
        public Color themeColor;
        public String pfpUrl;
    }

    public static TwitterProfile fetchFullProfile(String twitterUrl) {
        String pythonExecutable = "venv/bin/python3"; // Or your full absolute path
        String scriptPath = "scraper.py";
        try {
            ProcessBuilder pb = new ProcessBuilder(pythonExecutable, scriptPath, twitterUrl);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            boolean finished = process.waitFor(7, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return null;
            }
            JSONObject json = new JSONObject(output.toString().trim());
            if (!"success".equals(json.optString("status"))) {
                System.err.println("Scraper error: " + json.optString("message"));
                return null;
            }
            TwitterProfile profile = new TwitterProfile();
            profile.username = json.getString("username");
            profile.displayName = json.getString("display_name");
            profile.bio = json.getString("bio");
            String rawPfp = json.getString("pfp_url");
            profile.pfpUrl = rawPfp.replaceAll("(_normal|_bigger|_mini)(?=\\.[a-zA-Z0-9]+$)", "");
            String hexColor = json.optString("theme_color", "#1DA1F2");
            profile.themeColor = Color.decode(hexColor);
            return profile;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}