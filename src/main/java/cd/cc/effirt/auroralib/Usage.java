package cd.cc.effirt.auroralib;

public class Usage {
    public static String buildUsage(int prefix, String[] argsInfos) {
        String space = " ".repeat(prefix);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < argsInfos.length; i += 2) {
            if (argsInfos[i + 1] != null) {
                if (argsInfos[i].length() >= 40) {
                    sb.append(space)
                            .append(argsInfos[i]).append("\n")
                            .append((" ".repeat(40)))
                            .append(argsInfos[i + 1])
                            .append("\n");
                } else {
                    sb.append(space)
                            .append(String.format("%-40s", argsInfos[i]))
                            .append(argsInfos[i + 1])
                            .append("\n");
                }
            } else {
                sb.append(space)
                        .append(argsInfos[i])
                        .append("\n");
            }
        }
        return sb.toString();
    }
}