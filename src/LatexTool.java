import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.JLabel;

import org.scilab.forge.jlatexmath.ParseException;
import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;

public class LatexTool {

    // 处理整篇markdown
    // toPicFile：true为生成img标签用src引入，fasle生成img并将src转成base64
    // 如果要生成文件，需要通过basePath指定在哪个目录下生成
    public static String convertLatex(String mdText, boolean toPicFile, String basePath) {
        // 查找多行公式
        Pattern p1 = Pattern.compile("\\$\\$([\\s\\S]*?)\\$\\$");
        Matcher m1 = p1.matcher(mdText);
        int devation = 0;
        while (m1.find()) {
            String imgData = getImgElement(m1.group(1), toPicFile, basePath, true);
            // 要是出错返回了传入的内容，那就什么也不改
            if (imgData.equals(m1.group(1))) {
                continue;
            }
            mdText = mdText.substring(0, m1.start() + devation) + imgData + mdText.substring(m1.end() + devation);
            devation += imgData.length() - m1.group(0).length();
        }
        // 查找单行公式
        Pattern p2 = Pattern.compile("\\$(.+?)\\$");
        Matcher m2 = p2.matcher(mdText);
        devation = 0;
        while (m2.find()) {
            String imgData = getImgElement(m2.group(1), toPicFile, basePath, false);
            // 要是出错返回了传入的内容，那就什么也不改
            if (imgData.equals(m2.group(1))) {
                continue;
            }
            mdText = mdText.substring(0, m2.start() + devation) + imgData + mdText.substring(m2.end() + devation);
            devation += imgData.length() - m2.group(0).length();
        }
        return mdText;
    }

    private static long lastTimeMillis = 0l;
    private static int postFix = 0;

    // 处理一个latex公式
    // 返回图片的html标签，src根据toPicFile决定是生成图片文件还是转base64嵌入到html中
    // 如果要生成文件，需要通过basePath指定在哪个目录下生成
    // 多行公式需要通过multiLine指定样式
    private static String getImgElement(String latex, boolean toPicFile, String basePath, boolean multiLine) {
        // 默认img等于latex，即使出错还能显示原来的latex
        String imgStr = latex;
        try {
            Measure measure = new Measure();
            byte[] bytes = getLatexTransparentImgBytes(latex, measure);
            if (bytes != null) {
                // 设置图片的宽高，单位转换为rem
                String imgStyle = "width:" + (float) (Math.round(measure.w / 16.0 * 100)) / 100 + "rem;height:"
                        + (float) (Math.round(measure.h / 16.0 * 100)) / 100 + "rem;";
                if (multiLine) {
                    imgStyle += "display:block;margin:auto;padding:1rem;";
                }
                if (toPicFile) {
                    // 用时间做文件名有可能会重复，重复时后面再加一个数，保险起见，最后的文件名还要加随机数
                    long currentTimeMillis = System.currentTimeMillis();
                    String str = "" + currentTimeMillis;
                    if (currentTimeMillis == lastTimeMillis) {
                        str = str + postFix++;
                    } else {
                        postFix = 0;
                    }
                    lastTimeMillis = currentTimeMillis;
                    String filename = "" + str + (int) (Math.random() * 999) + ".png";
                    if (basePath == null || "".equals(basePath)) {
                        Files.write(Paths.get(filename), bytes, StandardOpenOption.CREATE);
                    } else {
                        Files.write(Paths.get(basePath + File.separator + filename), bytes, StandardOpenOption.CREATE);
                    }
                    imgStr = "<img style=\"" + imgStyle + "\" src=\"" + filename + "\">";
                } else {
                    imgStr = "<img style=\"" + imgStyle + "\" src=\"data:image/png;base64," + Base64.getEncoder().encodeToString(bytes) + "\">";
                }
            }
        } catch (ParseException e) {
            // 出错说明有不支持的符号，返回原公式让网页显示源码
            System.err.println(e.getMessage());
            System.out.println("error Latex: " + latex);
            return latex;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imgStr;
    }

    // 获取latex公式图片的字节码，这种方式无法设置透明背景
    public static byte[] getLatexImgBytes(String latex, Measure measure) throws IOException {
        TeXFormula formula = new TeXFormula(latex);
        TeXIcon icon = formula.new TeXIconBuilder()
                .setStyle(TeXConstants.STYLE_DISPLAY)
                .setFGColor(Color.BLACK)
                .setSize(16).build();
        BufferedImage bimage = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = bimage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);// 抗锯齿
        g2d.setColor(Color.WHITE);// 设置背景色
        g2d.fillRect(0, 0, icon.getIconWidth(), icon.getIconHeight());
        if (measure != null) {// 保存要返回的图片信息
            measure.w = icon.getIconWidth();
            measure.h = icon.getIconHeight();
        }
        JLabel jl = new JLabel();
        jl.setForeground(new Color(0, 0, 0));
        icon.paintIcon(jl, g2d, 0, 0);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(bimage, "png", out);
        byte[] bytes = out.toByteArray();
        out.close();
        return bytes;
    }

    // 获取透明背景的latex公式图片的字节码
    public static byte[] getLatexTransparentImgBytes(String latex, Measure measure) throws IOException {
        BufferedImage bimage = (BufferedImage) TeXFormula.createBufferedImage(latex, TeXConstants.STYLE_DISPLAY, 16, Color.BLACK, null);
        if (measure != null) {// 保存要返回的图片信息
            measure.w = bimage.getWidth();
            measure.h = bimage.getHeight();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(bimage, "png", out);
        byte[] bytes = out.toByteArray();
        out.close();
        return bytes;
    }

    private static class Measure {
        int w;
        int h;

        Measure() {
        }
    }
}
