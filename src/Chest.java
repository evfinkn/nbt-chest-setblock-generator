import java.io.FileWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.regex.*;
import java.util.stream.Collectors;

public class Chest {

    private final String rawData;
    private final int x;
    private final int y;
    private final int z;
    private final String facing;
    private final String type;
    private final String items;

    public Chest(String rawData) throws IllegalArgumentException {
        this.rawData = rawData;
        List<String> parsed = parseChest(rawData);
        x = Integer.parseInt(parsed.get(0));
        y = Integer.parseInt(parsed.get(1));
        z = Integer.parseInt(parsed.get(2));
        facing = "west";
        type = "single";
        items = parsed.get(3);
    }

    public Chest(String rawData, int x, int y, int z, String items, String facing, String type) {
        this.rawData = rawData;
        this.x = x;
        this.y = y;
        this.z = z;
        this.facing = facing;
        this.type = type;
        this.items = items;
    }

    public static List<String> parseChest(String data) throws IllegalArgumentException {
        List<String> parsed = new ArrayList<String>();
        Pattern[] patterns = new Pattern[]{
                Pattern.compile("(?<=x:)[-\\d]+(?=,)"),
                Pattern.compile("(?<=y:)[-\\d]+(?=,)"),
                Pattern.compile("(?<=z:)[-\\d]+(?=,)"),
                // old items regex: (?:Items:\[)[\s\S]*?(?=,z)
                Pattern.compile("Items:\\[[\\s\\S]*?(?=,z:[-\\d]+,id:\"minecraft:chest\")")};
        Matcher matcher;
        for (Pattern p : patterns) {
            matcher = p.matcher(data);
            if (matcher.find()) {parsed.add(data.substring(matcher.start(), matcher.end()));}
            else {throw new IllegalArgumentException();}
        }
        return parsed;
    }

    public String getCommand() {
        StringBuilder command = new StringBuilder("/setblock    minecraft:chest[facing=,type=]{}");
        command.insert(44, items);
        command.insert(42, type);
        command.insert(36, facing);
        command.insert(12, z);
        command.insert(11, y);
        command.insert(10, x);
        return command.toString();
    }

    public static ArrayList<Chest> getChestsFromFile(String filePath) {
        ArrayList<Chest> chests = new ArrayList<>();
        try {
            String data = Files.readString(Path.of(filePath));
            List<String> lines = Arrays.stream(
                            Pattern.compile("\\},\\{keep|block_entities:\\[").split(data))
                    .map(l -> "{keep" + l + "}")
                    .collect(Collectors.toList());
            lines.remove(0);
            for (String s : lines) {
                try {chests.add(new Chest(s));}
                catch (IllegalArgumentException e) {
                    // Ignore lines from the block entities file
                    // that aren't chests
                }
            }
        } catch (IOException e) {e.printStackTrace(); System.exit(1);}
        return chests;
    }

    public static void main(String[] args) {
        ArrayList<Chest> chests = getChestsFromFile("blockentities.txt");
        try {
            FileWriter writer = new FileWriter("chestCommands.txt");
            for (Chest c : chests) {
                writer.write(c.getCommand());
                writer.write("\n\n");
            }
            writer.close();
        }
        catch (IOException e) {e.printStackTrace();}
    }
}
