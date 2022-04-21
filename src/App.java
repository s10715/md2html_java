import java.io.File;

public class App {
    public static void main(String[] args) throws Exception {
        consoleSupport(args);
    }

    // 支持命令行调用
    public static void consoleSupport(String[] args) {
        boolean help = false;
        String inputPath = null;
        String outputPath = null;
        boolean isSame = false;
        boolean encode = false;
        boolean encodeImg = false;
        boolean encodeSource = false;
        boolean markdwon = false;
        boolean latex = false;
        boolean l2p = false;
        boolean toc = false;
        String cssPath = null;
        String jsPath = null;
        for (int i = 0; i < args.length; i++) {
            if (i == 0 && !args[i].startsWith("-"))
                inputPath = args[i];
            else {
                if ("-isSame".equals(args[i]))
                    isSame = true;
                else if ("-encode".equals(args[i]))
                    encode = true;
                else if ("-encodeImg".equals(args[i]))
                    encodeImg = true;
                else if ("-encodeSource".equals(args[i]))
                    encodeSource = true;
                else if ("-markdown".equals(args[i]))
                    markdwon = true;
                else if ("-latex".equals(args[i]))
                    latex = true;
                else if ("-l2p".equals(args[i]))
                    l2p = true;
                else if ("-toc".equals(args[i]))
                    toc = true;
                else if ("-css".equals(args[i]) && (i + 1) < args.length)
                    cssPath = args[i + 1];
                else if ("-js".equals(args[i]) && (i + 1) < args.length)
                    jsPath = args[i + 1];
                else if (i == 1 && !args[i].startsWith("-"))
                    outputPath = args[i];
            }
        }
        if (help || args.length == 0) {
            System.out.println("帮助:");
            System.out.println("(输入的所有文件都必须以UTF-8编码)");
            System.out.println("<inputfilename> [outputfilename] [...options]\t将markdown格式的inputfilename转成HTML格式的outputfilename");
            System.out.println("可在jar所在目录放theme.css和theme.js来添加到生成的html中");
            System.out.println("可选参数有：");
            System.out.println("-isSame\t\t对比inputfilename和outputfilename两个文本文件的内容是否一致(用于debug)");
            System.out.println("-encode\t\t将HTML中的img和source的src转base64嵌入到HTML中");
            System.out.println("-encodeImg\t只把HTML中的img的src转base64嵌入到HTML中");
            System.out.println("-encodeSource\t只把HTML中的source的src转base64嵌入到HTML中");
            System.out.println("-markdown\t处理markdown, 如果不处理markdown就不会处理Latex");
            System.out.println("-latex\t\t处理markdown中的latex公式(不支持的公式显示错误后跳过)");
            System.out.println("-l2p\t\t生成的latex公式是否转图片(默认用base64嵌入在网页中)");
            System.out.println("-toc\t\t生成TOC");
            System.out.println("-css <filepath>\t指定css(不指定则查找theme.css, 没有则使用默认的css)");
            System.out.println("-js <filepath>\t指定js(不指定则查找theme.js, 没有则使用默认的js)");
            System.out.println("生成单文件命令示例: \tfile1 file2 -markdown -latex -toc -encode");
            return;
        }
        if (isSame) {
            if (outputPath != null && !"".equals(outputPath)) {
                int diffLineNuber = FileTool.isTheSame(inputPath, outputPath);
                if (diffLineNuber == -1) {
                    System.out.println("两个文件内容完全一致");
                } else {
                    System.out.println("第" + diffLineNuber + "行开始不一样");
                }
            } else {
                System.out.println("没有指定要对比的文件");
            }
            // 判断两个文件是否相同之后就不应该再用后面的输出文件的功能了，否则会覆盖第二个参数的文件
            return;
        }
        if (inputPath != null && !"".equals(inputPath)) {
            // 为了可以连续使用功能，上一次的输出路径就是下一次的输入路径
            String inPath = inputPath;
            // 由于不知道最后生成的文件的拓展名，中间结果先写到临时文件中
            String tempOutPath = new File(inputPath).getParentFile().getAbsolutePath() + File.separator + "temp"
                    + System.currentTimeMillis() + (int) (Math.random() * 9999);
            String finalExt = "";
            if (markdwon) {
                String mdText = FileTool.readFile(inPath);
                if (latex)
                    mdText = LatexTool.convertLatex(mdText, l2p, new File(inPath).getParent());
                String html = MDTool.md2htmlWithText(mdText, FileTool.readFile(cssPath), FileTool.readFile(jsPath), toc);
                FileTool.writeFile(tempOutPath, html);
                inPath = tempOutPath;
                finalExt = ".html";
            }
            if (encode) {
                HtmlTool.imgGeneralBase64(inPath, tempOutPath);
                HtmlTool.sourceGeneralBase64(tempOutPath, tempOutPath);
                inPath = tempOutPath;
                finalExt = ".html";
            } else {
                if (encodeImg) {
                    HtmlTool.imgGeneralBase64(inPath, tempOutPath);
                    inPath = tempOutPath;
                    finalExt = ".html";
                }
                if (encodeSource) {
                    HtmlTool.sourceGeneralBase64(inPath, tempOutPath);
                    inPath = tempOutPath;
                    finalExt = ".html";
                }
            }
            // 临时文件重命名到outputPath
            File tempFile = new File(tempOutPath);
            if (tempFile.exists()) {
                if (outputPath == null || "".equals(outputPath)) {
                    // 没有指定outputPath就在原来文件名的基础上加上-out再拼接拓展名
                    outputPath = inputPath.substring(0, inputPath.lastIndexOf(".")) + "-out" + finalExt;
                }
                if (new File(outputPath).exists()) {
                    new File(outputPath).delete();
                }
                tempFile.renameTo(new File(outputPath));
            }
        }
    }

}
