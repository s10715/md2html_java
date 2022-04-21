import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

// 可以在指定的路径下查找某一个文件名的文件（另外指定拓展名），读、写文件，判断两个文件是否相同等
public class FileTool {

    // 目录缓存
    private static String dirCachePath = "";// dirCache对应的路径，查找该路径下的所有文件，把相对路径放到dirCache中
    private static List<String> dirCache;// dirListPath + File.separator + dirCache.get(x) 就是绝对路径

    // 刷新目录缓存
    public static void updateDirCache(String basePath) {
        if (basePath != null && !"".equals(basePath))
            dirCachePath = basePath;
        dirCache = generalDirList(dirCachePath, null);
    }

    // 尝试在basePath目录下（包括子目录）搜索是否存在targetFileName+exts[i]，存在的话就返回查找到的文件相对路径集合fileList
    // targetFileName不带拓展名，它可能是exts中的任意一种
    public static List<String> findFiles(String basePath, String targetFileName, String[] exts, boolean forceUpdate) {
        // basePath改了之后再重新生成目录，不需要每次调用都生成一遍，forceUpdate可以在没有改basePath的情况下要求刷新缓存
        if (dirCachePath != basePath || forceUpdate) {
            updateDirCache(basePath);
        }
        List<String> result = new ArrayList<>();
        for (String filePath : dirCache) {
            for (String ext : exts) {
                if (filePath.indexOf(targetFileName + ext) != -1) {
                    // 即使filePath中有对应的文件名，也有可能是文件夹名，还要再判断文件名
                    File foundFile = new File(basePath + File.separator + filePath);
                    if (foundFile.getName().equals(targetFileName + ext)) {
                        // 找到一个和目标文件名一致的文件
                        result.add(filePath);
                    }
                }
            }
        }
        return result;
    }

    // 遍历baseSearchPath下所有的文件夹、文件，返回相对路径的List
    // 调用时只需传入前一个参数，其他的是递归需要的参数，直接传null就可以了
    public static List<String> generalDirList(String baseSearchPath, List<String> fileList) {
        if (fileList == null) {
            fileList = new ArrayList<>();
        }
        if (baseSearchPath == null || baseSearchPath.equals("")) {
            baseSearchPath = dirCachePath;
        }
        File baseDir = new File(baseSearchPath);
        if (baseDir.exists() && baseDir.isDirectory()) {
            String[] filelist = baseDir.list();
            for (int i = 0; i < filelist.length; i++) {
                File readfile = new File(baseSearchPath + File.separator + filelist[i]);
                if (!readfile.isDirectory()) {
                    // 计算相对路径
                    String relativePath = readfile.getAbsolutePath().replace((new File(dirCachePath)).getAbsolutePath(), "");
                    // 去掉开头的路径分隔符
                    if (relativePath.startsWith("\\") || relativePath.startsWith("/")) {
                        relativePath = relativePath.substring(1, relativePath.length());
                    }
                    fileList.add(relativePath);
                } else if (readfile.isDirectory()) {
                    generalDirList(baseSearchPath + File.separator + filelist[i], fileList);
                }
            }
        }
        return fileList;
    }

    // 判断两个文件的内容是否一模一样，返回-1表示一模一样，否则返回不一样的行数
    public static int isTheSame(String filePath1, String filePath2) {
        int diffLineNuber = -1;
        List<String> data1 = FileTool.readLines(filePath1);
        List<String> data2 = FileTool.readLines(filePath2);
        if (data2.size() < data1.size()) {// 短的那个放在data1，防止越界
            List<String> temp = data1;
            data1 = data2;
            data2 = temp;
        }
        for (int i = 0; i < data1.size(); i++) {
            if (!data1.get(i).equals(data2.get(i))) {
                diffLineNuber = i + 1;// 行数是下标+1
                break;
            }
        }
        if (data1.size() != data2.size() && diffLineNuber == -1) {
            // 说明两个文件前面的部分全部相同，只是有一个文件要多一点内容
            diffLineNuber = data1.size();
        }
        return diffLineNuber;
    }

    public static String readFile(String filePath) {
        if (filePath == null || "".equals(filePath)) {
            return null;
        }
        String result = "";
        try {
            Path path = Paths.get(filePath);
            byte[] data;
            data = Files.readAllBytes(path);
            result = new String(data, "utf-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static List<String> readLines(String filePath) {
        if (filePath == null || "".equals(filePath)) {
            return null;
        }
        List<String> data = null;
        try {
            Path path = Paths.get(filePath);
            data = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    public static void writeFile(String outputPath, String data) {
        if (outputPath == null || data == null || "".equals(data)) {
            return;
        }
        File f = new File(outputPath);
        if (!f.getParentFile().exists()) {
            f.getParentFile().mkdirs();
        }
        Path path = Paths.get(outputPath);
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void appendFile(String outputPath, String data) {
        if (outputPath == null || data == null || "".equals(data)) {
            return;
        }
        Path path = Paths.get(outputPath);
        File f = path.toFile();
        if (!f.getParentFile().exists()) {
            f.getParentFile().mkdirs();
        }
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.APPEND)) {
            writer.write(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
