import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// html中audio和img的src转base64嵌入到网页中
public class HtmlTool {
    // 将网页内的source标签的src全部换成base64。 data:audio/mpeg;base64,
    public static void sourceGeneralBase64(String inputFilePath, String outputFilePath) {
        String basePath = new File(inputFilePath).getParentFile().getAbsolutePath();
        generalBase64(inputFilePath, outputFilePath, basePath, "source", new String[] { "audio/mpeg", "audio/wav" },
                new String[] { "mp3", "wav" });
    }

    // 将网页内的img标签的src全部换成base64。 data:image/jpg;base64,
    public static void imgGeneralBase64(String inputFilePath, String outputFilePath) {
        String basePath = new File(inputFilePath).getParentFile().getAbsolutePath();
        generalBase64(inputFilePath, outputFilePath, basePath, "img",
                new String[] { "image/jpg", "image/png", "image/webp" },
                new String[] { "jpg", "png", "webp" });
    }

    // HTML的各种标签的src转成base64，默认src是相对路径，要用basePath进行拼接（basePath为空就直接根据src拿文件）
    // data:image/jpg;base64, image/jpg是types，jpg是extNames
    // data:audio/mpeg;base64, audio/mpeg是types，mp3是extNames
    // types[i]要对应extNames[i]
    private static void generalBase64(String inputFilePath, String outputFilePath, String basePath, String tagName, String[] types, String[] extNames) {
        File inputFile = new File(inputFilePath);
        int lastIdx = 0;
        String tempFilePath = "";
        if (basePath != null && !"".equals(basePath)) {
            tempFilePath = basePath + File.separator + "temp" + System.currentTimeMillis();
        } else {
            tempFilePath = "" + System.currentTimeMillis();
        }
        // 防止文件已存在覆盖原有文件
        while (new File(tempFilePath).exists()) {
            if (basePath != null && !"".equals(basePath)) {
                tempFilePath = basePath + File.separator + "temp" + System.currentTimeMillis()
                        + (int) (Math.random() * 9999);
            } else {
                tempFilePath = "" + System.currentTimeMillis() + (int) (Math.random() * 9999);
            }
        }
        String str = FileTool.readFile(inputFile.getAbsolutePath());
        Matcher m1 = Pattern.compile("<" + tagName + "[\\s\\S]*?src\\s*=\\s*[\\\"|\\\'](.*?)[\\\"|\\\'][\\s\\S]*?>").matcher(str);
        boolean handled = false;
        while (m1.find()) {
            // 只支持指定的几个格式的音频
            int extIdx = -1;
            for (int i = 0; i < extNames.length; i++) {
                if (m1.group(1).toLowerCase().endsWith(extNames[i].toLowerCase())) {
                    extIdx = i;
                }
            }
            if (extIdx == -1) {
                continue;
            }
            String base64Data = encryptToBase64(basePath, m1.group(1));
            if (base64Data.equals(m1.group(1))) {// 可能是其他协议的路径（比如file:///），这时就无法转成base64，直接跳过
                continue;
            }
            // replace不支持正则，可以防止文件名中带有需要转义的符号（比如括号）
            String replacement = m1.group(0).replace(m1.group(1), "data:" + types[extIdx] + ";base64," + base64Data);
            // 替换span，不改变原来的str，放到tempStr中，以减少内存占用
            String tempStr = str.substring(lastIdx, m1.start()) + replacement;
            lastIdx = m1.end();
            // 分多次写并写到临时文件中
            FileTool.appendFile(tempFilePath, tempStr);
            handled = true;
        }
        File outf = new File(outputFilePath);
        if (!outf.getParentFile().exists()) {
            outf.getParentFile().mkdirs();
        }
        if (handled) {
            // 如果有对应的标签
            if (outf.exists()) {
                outf.delete();
            }
            FileTool.appendFile(tempFilePath, str.substring(lastIdx));// 把最后的部分写到临时文件
            (new File(tempFilePath)).renameTo(outf);
        } else {
            // 如果没有对应的标签，那么直接把源文件复制一次就行了
            if (!inputFile.getAbsolutePath().equals(outf.getAbsolutePath())) {
                try {
                    Files.copy(Paths.get(inputFilePath), Paths.get(outputFilePath));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (new File(tempFilePath).exists()) {
                (new File(tempFilePath)).delete();
            }
        }
    }

    // 文件转base64，默认filePath是相对路径，要用basePath进行拼接（basePath为空就直接根据filePath拿文件）
    public static String encryptToBase64(String basePath, String filePath) {
        if (filePath == null) {
            return null;
        }
        if (filePath.toLowerCase().startsWith("http")) {// 网络图片
            byte[] data = null;
            InputStream in = null;
            ByteArrayOutputStream out = null;
            try {
                URL url = new URL(filePath);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                in = connection.getInputStream();
                out = new ByteArrayOutputStream();
                byte[] b = new byte[1024];
                int len = 0;
                while ((len = in.read(b)) != -1) {
                    out.write(b, 0, len);
                }
                data = out.toByteArray();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.getStackTrace();
                }
            }
            return Base64.getEncoder().encodeToString(data);
        } else {// 否则可能是本地图片
            try {
                if (basePath != null && !"".equals(basePath)) {
                    filePath = basePath + File.separator + filePath;
                }
                byte[] b = Files.readAllBytes(Paths.get(filePath));
                return Base64.getEncoder().encodeToString(b);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return filePath;
    }

    // 将文件输入流转为base64
    public static String getBase64FromInputStream(InputStream in) {
        // 将图片文件转化为字节数组字符串，并对其进行Base64编码处理
        byte[] data = null;
        // 读取图片字节数组
        try {
            ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
            byte[] buff = new byte[100];
            int rc = 0;
            while ((rc = in.read(buff, 0, 100)) > 0) {
                swapStream.write(buff, 0, rc);
            }
            data = swapStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return Base64.getEncoder().encodeToString(data);
    }
}
