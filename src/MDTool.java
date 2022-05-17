import java.io.File;
import java.io.InputStream;
import java.util.Arrays;

import com.vladsch.flexmark.ext.attributes.AttributesExtension;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.definition.DefinitionExtension;
import com.vladsch.flexmark.ext.emoji.EmojiExtension;
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension;
import com.vladsch.flexmark.ext.jekyll.tag.JekyllTagExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.ext.typographic.TypographicExtension;
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.ParserEmulationProfile;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.MutableDataSet;

// flexmark不支持Latex的语法，只能处理基础的markdown
// 应该先处理Latex再用MDTool转换，否则比如两个下划线就会先被生成斜体，Latex解析就会出错了
public class MDTool {

    // cssPath和jsPath是css和js的文件路径，如果传null那么默认查找theme.css和theme.js；generateTOC：是否生成TOC
    public static String md2htmlWithPath(String MDInputFile, String cssPath, String jsPath, boolean generateTOC) {
        String cssText = "";
        String jsText = "";
        if (cssPath == null || cssPath.equals("")) {
            cssText = loadDefaultCss();
        } else {
            cssText = FileTool.readFile(cssPath);
        }
        if (jsPath == null || jsPath.equals("")) {
            jsText = loadDefaultJs();
        } else {
            jsText = FileTool.readFile(jsPath);
        }
        String html = md2htmlWithText(FileTool.readFile(MDInputFile), cssText, jsText, generateTOC);
        return html;
    }

    // mdText：markdown文本；cssText：css文本；jsText：js文本；generateTOC：是否生成TOC
    public static String md2htmlWithText(String mdText, String cssText, String jsText, boolean generateTOC) {
        String head = "";
        String body = "";
        if (generateTOC) {
            // 在最前面添加 [TOC] 生成目录
            mdText = "[TOC]\n" + mdText;
        }

        MutableDataSet option = new MutableDataSet();
        option.setFrom(ParserEmulationProfile.MULTI_MARKDOWN)
                .set(Parser.EXTENSIONS, Arrays.asList(
                        JekyllTagExtension.create(),
                        AttributesExtension.create(),
                        AutolinkExtension.create(), // url形式的字符串转成超链接
                        DefinitionExtension.create(),
                        EmojiExtension.create(), // emoji
                        FootnoteExtension.create(), // 脚注（[^footnote]）
                        TablesExtension.create(), // 表格
                        TocExtension.create(), // 用[TOC]生成目录
                        TypographicExtension.create(), // 部分字符转成html字符（比如把省略号变成&hellip;）
                        YamlFrontMatterExtension.create()// YAML格式文本
                ));
        option.set(TocExtension.TITLE, "目录");// TOC默认只有ul，必须加一个标题才能在外面添加一个div，从而指定div的class
        option.set(TocExtension.DIV_CLASS, "toc");// 指定生成的TOC的class

        Parser parser = Parser.builder(option).build();
        Document doc = parser.parse(mdText);
        HtmlRenderer renderer = HtmlRenderer.builder(option).build();
        body += renderer.render(doc);// markdown生成的html代码（只有body的内容，且不含body标签）
        // 表格超出宽度添加滚动条（在外面加一个div设置overflow为auto）
        body = body.replaceAll("<table>[\\s\\S]*?</table>", "<div style=\"overflow:auto\">$0</div>");
        // 添加css和js，其中js要放在body的最后
        if (cssText == null || cssText.equals("")) {
            cssText = loadDefaultCss();
        }
        if (jsText == null || jsText.equals("")) {
            jsText = loadDefaultJs();
        }
        if (cssText != null && !cssText.equals("")) {
            head += "<style type=\"text/css\">" + cssText + "</style>";
        }
        if (jsText != null && !jsText.equals("")) {
            body += "<script type=\"text/javascript\">" + jsText + "</script>";
        }
        head = "<meta charset=\"UTF-8\">" + head;
        String html = "<!doctype html><html><head>" + head + "</head><body>" + body + "</body></html>";
        return html;
    }

    // 搜索 theme.css
    private static String loadDefaultCss() {
        return getFile("theme.css");
    }

    // 搜索 theme.js
    private static String loadDefaultJs() {
        return getFile("theme.js");
    }

    // 先尝试在项目目录或者生成的jar所在目录找文件，如果没有再尝试在项目的bin目录或者jar内找
    private static String getFile(String filename) {
        // 获取到的是项目目录，或者jar所在目录
        // System.out.println(new File("").getAbsolutePath());
        // 获取的是生成的class所在目录，如果是生成的jar格式是xxx.jar!/xxx/xxx
        // System.out.println(new MDTool().getClass().getResource("").getPath());
        String fileContent = null;
        File f = new File(filename);
        if (f.exists()) {
            fileContent = FileTool.readFile(f.getAbsolutePath());
        } else {
            try {
                InputStream stream = new MDTool().getClass().getResourceAsStream(filename);
                fileContent = new String(stream.readAllBytes(), "utf-8");
            } catch (Exception e) {
                // e.printStackTrace();
            }
        }
        return fileContent;
    }
}
