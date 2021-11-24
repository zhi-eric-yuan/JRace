package tune;

import algo.Configuration;

import java.util.ArrayList;

public class TuningStatus {
    public static ArrayList<Integer> usedBudget = new ArrayList<>();
    public static ArrayList<Configuration> bestConfigurations = new ArrayList<>();

    public static void update(int usedExp, Configuration config) {
        usedBudget.add(usedExp);
        bestConfigurations.add(config);
    }

    public static boolean isEmpty() {
        return usedBudget.isEmpty();
    }

    public static String listAll() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < usedBudget.size(); i++) {
            sb.append(usedBudget.get(i)).append(" ").append(bestConfigurations.get(i)).append("\n");
        }
        return sb.toString();
    }

    public static String listLast() {
        int lastIndex = usedBudget.size() - 1;
        return new StringBuilder().append(usedBudget.get(lastIndex)).append(" ")
                .append(bestConfigurations.get(lastIndex)).toString();
    }

    public static Configuration lastBestConfiguration() {
        if (isEmpty()) {
            return null;
        }
        return bestConfigurations.get(bestConfigurations.size() - 1);
    }
}
