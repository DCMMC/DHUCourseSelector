package tk.dcmmcc;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Scanner;

/**
 * 封装了大类课程的有关信息, 比如外语类, 体育类
 * Created by DCMMC on 2017/8/30.
 */
public class CommonCourseType {
    /* 字段 */
    //选课首页
    private final static String selectCourse =
            "http://jwdep.dhu.edu.cn/dhu/student/selectcourse/selectCourse_ts.jsp";
    private HyperlinkURL courseNameAndLink;

    //HttpClient
    private static HttpClient httpClient = new HttpClient();

    /* 静态实例初始化 */
    static {
        // 设置 HttpClient 接收 Cookie,用与浏览器一样的策略
        httpClient.getParams().setCookiePolicy(
                CookiePolicy.BROWSER_COMPATIBILITY);
    }

    /* 构造器 */
    public CommonCourseType() {

    }

    public CommonCourseType(HyperlinkURL courseNameAndLink) {
        this.courseNameAndLink = courseNameAndLink;
    }

    /* 公共方法 */
    /**
     * 导入所有大类课程
     */
    public static CommonCourseType[] intelliImportCommonCourseType(String userCookie) {
        GetMethod selectCourseHome = new GetMethod(selectCourse);
        selectCourseHome.setRequestHeader("cookie", userCookie);

        try {
            httpClient.executeMethod(selectCourseHome);

            String response = "";
            Scanner scanner = new Scanner(new BufferedInputStream(selectCourseHome.getResponseBodyAsStream()),
                    "gbk");
            while (scanner.hasNextLine())
                response += scanner.nextLine() + "\n";

            DoubleLinkedList<HyperlinkURL>[] table = new Table(response, new URL(selectCourse)).getTable();

            DoubleLinkedList<CommonCourseType> result = new DoubleLinkedList<>();

            //跳过前面两行
            for (int i = 2; i < table.length; i++) {
                if (table[i].getSize() == 2 && table[i].get(1).getLink() != null) {
                    result.addLast(new CommonCourseType(table[i].get(1)));
                }
            }

            return result.toArray();
        } catch (IOException ioe) {
            //连接教务处的时候发生IOException
            System.err.println("连接教务处的时候发生IOException: " + ioe.getMessage());
            return null;
        }

    }

    public HyperlinkURL getCourseNameAndLink() {
        return courseNameAndLink;
    }
}///~
