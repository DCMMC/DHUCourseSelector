package tk.dcmmcc.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Scanner;

/**
 * 从html中解析表格
 * Created by DCMMC on 2017/8/29.
 */
public class Table {
    private String htmlText;
    private URL originURL;
    private DoubleLinkedList<HyperlinkURL>[] table;

    //cannot instanced by default constructor
    private Table() {

    }

    /**
     * 创建Table对象
     * @param htmlText html文本
     * @param originURL 这个html所在的link(绝对路径)
     */
    public Table(String htmlText, URL originURL) {
        this.htmlText = htmlText;
        this.originURL = originURL;

        parse();
    }

    /**
     * 解析html文本
     */
    @SuppressWarnings("unchecked")
    private void parse() {
        Document document = Jsoup.parse(htmlText);
        //所有不属于嵌套表格的行, 用Selector实现(类似于正则表达式)
        Elements trs = document.select("table:not(td > table)").select("tr:not(td > table > tbody > tr)");

        table = new DoubleLinkedList[trs.size()];

        for (int i = 0; i < table.length; i++)
            table[i] = new DoubleLinkedList<>();

        for (int i = 0; i < trs.size(); i++) {
            Elements tds = trs.get(i).select("td:not(td > table > tbody > tr > td)");

            for (Element td : tds) {
                try {
                    //如果是嵌套表格(一般是横跨了上课周次	上课时间	上课地点这三个项目)
                    //也有可能像教务处首页那样, 大类课程横跨11格
                    if (td.hasAttr("colspan")) {
                        int colspan = Integer.valueOf(td.attr("colspan"));
                        Elements nestedTrs = td.select("table").select("tr");

                        //如果像教务处首页那样, 大类课程横跨11格, 但是没有内嵌表格, 这时候nestedTrs.size() == 0
                        if (nestedTrs.size() > 0) {
                            if (trs.size() == 0) {
                                //如果是像实习这样的没有上课时间的课
                                //比如某个课程的开班信息的上课周次	上课时间	上课地点这三个项目
                                for (int j = 0; j < colspan; j++)
                                    table[i].addLast(new HyperlinkURL(null, null));
                                continue;
                            }

                            //上课时间安排
                            for (Element nestedTr : nestedTrs) {
                                Elements nestedTds = nestedTr.select("td");

                                for (Element nestedTd : nestedTds)
                                    table[i].addLast(new HyperlinkURL(nestedTd.text(), null));
                            }
                            continue;
                        }

                    }

                    //获取超链接
                    String linkURL;
                    if (td.getElementsByTag("a").size() > 0) {
                        linkURL = td.getElementsByTag("a").get(0).attr("href");

                        //有可能这个link是相对路径的
                        URL absoluteURL;

                        if (linkURL.contains("http://") || linkURL.contains("https://")) {
                            absoluteURL = new URL(linkURL);
                        } else if(linkURL.contains("javascript:")) {
                            //这是html代码中对js函数的调用, 比如查看已选页面那里的删除按钮里面的href
                            absoluteURL = null;
                        } else if (linkURL.contains("/") && !linkURL.contains("..")) {
                            //相对于host的路径
                            //例如/dhu/ftp/01/010132DG.htm
                            //偷点懒...
                            absoluteURL = new URL("http://jwdep.dhu.edu.cn" + linkURL);
                        } else {
                            //相对路径
                            String targetDictURL = originURL.toString().replaceFirst("(?m)/[^/]*$", "/");

                            while (linkURL.contains("../")) {
                                //如过是相对于当前URL的目录的上一个目录
                                linkURL = linkURL.replaceFirst("\\.\\./", "");

                                //targetDictURL往上一层
                                targetDictURL = targetDictURL.replaceFirst("(?m)/[^/]*/$", "/");
                            }

                            absoluteURL = new URL(targetDictURL + linkURL);
                        }



                        table[i].addLast(new HyperlinkURL(td.text(), absoluteURL));
                    } else
                        table[i].addLast(new HyperlinkURL(td.text(), null));
                } catch (MalformedURLException me) {
                    System.err.println("URL格式错误: ");
                    me.printStackTrace();
                    return;
                }
            }
        }
    }

    public DoubleLinkedList<HyperlinkURL>[] getTable() {
        return table;
    }

    /**
     * Test client
     */
    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(new BufferedInputStream(new FileInputStream(new File(Table.class
                    .getResource("tableHtml.html")
                    .toURI()))));

            String response = "";

            while (scanner.hasNext())
                response += scanner.nextLine();

            URL url = new URL("http://jwdep.dhu.edu.cn/dhu/commonquery/selectcoursetermcourses.jsp?course=%BC%C6%CB%E3%BB%FA&pageSize=27");

            Table demo = new Table(response, url);

            System.out.println("table: ");

            for (DoubleLinkedList d : demo.table) {
                for (Object s : d) {
                    System.out.print(((HyperlinkURL)s).getTextTitle() +
                            (((HyperlinkURL)s).hasLink() ? (((HyperlinkURL)s).getLink().toString() + "  ") : "  "));
                }
                System.out.println();
            }

        } catch (URISyntaxException ue) {
            //URI 格式错误
            System.err.println("URI 格式错误");
        } catch (FileNotFoundException fe) {
            //文件找不到
            System.err.println("文件找不到");
        } catch (MalformedURLException me) {
            //URL格式错误
            System.err.println("URL格式错误");
        }

    }

}///~
