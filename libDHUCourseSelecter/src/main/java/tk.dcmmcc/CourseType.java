package tk.dcmmcc;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import tk.dcmmcc.utils.DoubleLinkedList;
import tk.dcmmcc.utils.HyperlinkURL;
import tk.dcmmcc.utils.Pointer;
import tk.dcmmcc.utils.Table;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Scanner;

/**
 * 封装了课程的有关信息
 * Created by DCMMC on 2017/8/29.
 */
public class CourseType {
    //选课首页
    private final static String selectCourse =
            "http://jwdep.dhu.edu.cn/dhu/student/selectcourse/selectCourse_ts.jsp";
    //公共查询的首页, 把要搜索的内容作为参数GET就可以得到查询结果了, 可以通过课程名称或者课程代码查询
    //记得java.net.URLEncoder.encode( , "utf-8")来对中文转码
    private final static String prefixCommonSearch =
            "http://jwdep.dhu.edu.cn/dhu/commonquery/selectcoursetermcourses.jsp";

    private HyperlinkURL course;
    private String courseId;
    private double score;
    private String major;
    private URL courseSyllabus;
    private URL courseCalendar;

    //备注, 课程小类, 比如是文化素质类 艺术类还是啥的
    private String remark = "";

    //HttpClient
    private static HttpClient httpClient = new HttpClient();

    /* 静态实例初始化 */
    static {
        // 设置 HttpClient 接收 Cookie,用与浏览器一样的策略
        httpClient.getParams().setCookiePolicy(
                CookiePolicy.BROWSER_COMPATIBILITY);
    }


    //默认构造器
    public CourseType() {

    }

    /**
     * @param course 课程名称以及链接
     * @param courseId 课程代码
     * @param score 学分
     * @param courseSyllabus 教学大纲
     * @param courseCalendar 教学日历
     * @param major 开设学院
     */
    CourseType(HyperlinkURL course, String courseId, double score, URL courseSyllabus, URL courseCalendar, String major) {
        this.course = course;
        this.courseId = courseId;
        this.score = score;
        this.courseSyllabus = courseSyllabus;
        this.courseCalendar = courseCalendar;
        this.major = major;
    }


    /**
     * 用公共查询来查询课程
     * @param nameOrId 课程的名称或者课程代码的部分或者全部, 会自动进行URLEncoder
     * @param userCookie 用户Cookie, 没有Cookie竟然不能查询...
     */
    public static CourseType[] searchCourse(String nameOrId, String userCookie) {
        try {
            //GET提交形式
            GetMethod commonSearch = new GetMethod(prefixCommonSearch + "?course=" +
                    URLEncoder.encode(nameOrId, "gbk"));
            commonSearch.setRequestHeader("cookie", userCookie);

            //提交请求
            httpClient.executeMethod(commonSearch);

            //debug...
            //String sDebug = commonSearch.getResponseBodyAsString();
            //System.out.println(sDebug);

            String responsePage = "";
            Scanner scanner = new Scanner(new BufferedInputStream(commonSearch.getResponseBodyAsStream()), "gbk");
            while (scanner.hasNextLine()) {
                //结果条数和页数
                String searchResult;

                if ((searchResult = scanner.findInLine("共\\d+页\\d+条")) != null && !searchResult.contains("共1页")) {
                    //如果有多页
                    commonSearch = new GetMethod(prefixCommonSearch + "?course=" +
                   URLEncoder.encode(nameOrId, "gbk") + "&pageSize="
                            + Integer.valueOf(searchResult.replaceAll("共\\d+页", "").replace("条", "")));
                    responsePage = "";
                    break;
                }

                responsePage += scanner.nextLine() + "\n";
            }

            if (responsePage.length() == 0) {
                commonSearch.setRequestHeader("cookie", userCookie);
                httpClient.executeMethod(commonSearch);
                scanner = new Scanner(new BufferedInputStream(commonSearch.getResponseBodyAsStream()), "GBK");
                while (scanner.hasNextLine())
                    responsePage += scanner.nextLine() + "\n";
            }

            //解析html中的表格
            DoubleLinkedList<HyperlinkURL>[] table
                    = new Table(responsePage, new URL(commonSearch.getURI().toString())).getTable();

            //去掉前面两行和最后一行
            CourseType[] result = new CourseType[table.length - 3];

            for (int i = 2; i < table.length - 1; i++) {
                result[i - 2] = new CourseType(table[i].popFirst(), table[i].popFirst().getTextTitle(),
                        Double.valueOf(table[i].popFirst().getTextTitle()), table[i].popFirst().getLink(),
                        table[i].popFirst().getLink(), table[i].popFirst().getTextTitle());
            }

            return result;
        } catch (Exception e) {
            //debug
            throw new RuntimeException(e);
            //任何异常都相当于没有找到任何CourseType
            //return null;
        }

    }

    /**
     * 从大类课程得到其下的所有课程
     * @return 发生任何问题都返回null, 比如异常或者计算机类，体育类每学期只能选修1门
     */
    public static CourseType[] getCourseTypes(CommonCourseType commonCourseType, String userCookie) {
        GetMethod commonCourses = new GetMethod(commonCourseType.getCourseNameAndLink().getLink().toString());
        commonCourses.setRequestHeader("cookie", userCookie);

        try {
            httpClient.executeMethod(commonCourses);

            String response = "";
            String line;
            Scanner scanner = new Scanner(new BufferedInputStream(commonCourses.getResponseBodyAsStream()),
                    "gbk");
            while (scanner.hasNextLine()) {
                if ((line = scanner.nextLine()).contains("只能选修1门"))
                    return null;

                response += line + "\n";
            }


            DoubleLinkedList<HyperlinkURL>[] table = new Table(response, new URL(selectCourse)).getTable();
            DoubleLinkedList<CourseType> result = new DoubleLinkedList<>();

            HyperlinkURL thisCourse;
            String thisCourseId, thisRemark;
            URL thisCourseSyllabus, thisCourseCalendar;
            double thisScore;

            //跳过第一行
            for (int i = 1; i < table.length; i++) {
                thisCourse = table[i].popFirst();
                thisCourseId = table[i].popFirst().getTextTitle();
                thisScore = Double.valueOf(table[i].popFirst().getTextTitle());
                thisCourseCalendar = table[i].popFirst().getLink();
                thisCourseSyllabus = table[i].popFirst().getLink();
                //课程大类
                table[i].popFirst();
                thisRemark = table[i].popFirst().getTextTitle();
                //如果有了修读成绩, 这个就不算
                //妈个鸡没有成绩就是一个空格 =,= 我还以为是""
                if (table[i].popFirst().getTextTitle().equals(" ")) {
                    result.addLast(new CourseType(thisCourse, thisCourseId, thisScore, thisCourseSyllabus,
                            thisCourseCalendar, ""));
                }
            }

            return result.toArray();
        } catch(IOException ioe) {
            //连接教务处的时候发生IOException
            System.err.println("连接教务处的时候发生IOException: " + ioe.getMessage());
            return null;
        }
    }

    /**
     * P.S. 选修专业课只需要从中选取一部分, 请适当删除一部分!!!
     * 从教务处选课首页智能导入当前学期的推荐课程(非大类课程, 且是未读状态的)
     * @param userCookie 用户Cookie
     * @param callbackMessage 导入完成之后的反馈信息
     */
    @SuppressWarnings("unchecked")
    public static CourseType[] intelliImportCourseType(String userCookie, Pointer<String> callbackMessage) {
        //选课首页
        GetMethod selectCourseHome = new GetMethod(selectCourse);
        selectCourseHome.setRequestHeader("cookie", userCookie);

        try {
            httpClient.executeMethod(selectCourseHome);

            Scanner scanner = new Scanner(new BufferedInputStream(selectCourseHome.getResponseBodyAsStream()),
                    "gbk");

            //当前选课学期 1 ~ 8
            int currentSemester = 0;
            //综合实践必修, 实践教学选修, 学科基础必修, 通识教育必修, 学科基础选修, 专业方向选修, 通识教育选修, 实践教学必修
            //已经获得的学分
            double[] score = new double[8];
            //上述类别的要求学分
            double[] needScore = new double[8];
            //程序已经读取的学分个数
            int loaded = 0;

            String responsePage = "";
            String line;
            //需要读取上述各种学分的标志
            boolean readScores = false;
            //暂存stack,用来在发现style=background:#eaf1f1字段的时候向前遍历
            DoubleLinkedList<String> auxStack = new DoubleLinkedList<>();
            while (scanner.hasNextLine()) {
                line = scanner.nextLine();
                //确定当前选课学期
                if (currentSemester == 0) {
                    auxStack.addFirst(line);

                    if (line.contains("style=background:#eaf1f1")) {
                        String tmp;

                        //将tmp定位到这一行的第一个单元格所在的html代码的下面那一行
                        while (!(tmp = auxStack.popFirst()).contains("tr")) {
                            //这是及格或者不及格成绩的那一格
                            //if (tmp.contains("<font color"))
                            //    continue;

                            if (tmp.equals("<td >") || tmp.equals("<td>"))
                                currentSemester++;
                        }

                        //记得加上有style=background:#eaf1f1字段的这一行
                        currentSemester++;
                    }
                }

                if (readScores) {
                    //开始读取
                    String[] typeAndScore = line.replace("showCredit(\"", "").replace("\");", "").split("\",\"");

                    switch (typeAndScore[0]) {
                        case "综合实践必修" : score[5] = Double.valueOf(typeAndScore[1].substring(5));break;
                        case "实践教学选修" : score[7] = Double.valueOf(typeAndScore[1].substring(5));break;
                        case "学科基础必修" : score[2] = Double.valueOf(typeAndScore[1].substring(5));break;
                        case "通识教育必修" : score[0] = Double.valueOf(typeAndScore[1].substring(5));break;
                        case "学科基础选修" : score[3] = Double.valueOf(typeAndScore[1].substring(5));break;
                        case "专业方向选修" : score[4] = Double.valueOf(typeAndScore[1].substring(5));break;
                        case "通识教育选修" : score[1] = Double.valueOf(typeAndScore[1].substring(5));break;
                        case "实践教学必修" : score[6] = Double.valueOf(typeAndScore[1].substring(5));break;
                    }

                    if (++loaded == 8)
                        readScores = false;
                }

                if (line.contains("function initStudCredit()"))
                    readScores = true;

                responsePage += line + "\n";
            }


            DoubleLinkedList<HyperlinkURL>[] table = new Table(responsePage, new URL(selectCourse)).getTable();

            //Parsing table
            //8大类
            DoubleLinkedList<CourseType>[] courseTypes = new DoubleLinkedList[8];
            for (int i = 0; i < 8; i++)
                courseTypes[i] = new DoubleLinkedList<>();
            //8大类哪几类会因为把当前学期推荐课程全选导致超过要求学分
            boolean[] scoreTooMuch = new boolean[8];
            //学科大类的名称
            String[] courseKinds = {
                    "通识教育必修", "通识教育选修", "学科基础必修", "学科基础选修(可用其他专业的专业课抵充)",
                    "专业方向选修", "综合实践必修", "实践教学必修", "实践教学选修"
            };
            //当前大类学科已修或者已选学分
            double gotScore = score[0];
            //为读取各类学分记数
            loaded = 0;

            //暂存
            HyperlinkURL courseIdAndLink;
            String courseName;
            double courseScore;

            //成功导入的课程数目
            int courseCnt = 0;

            //跳过前面两行
            for (int i = 2; i < table.length; i++) {
                if (loaded < 8 && table[i].getSize() == 2 && table[i].get(1).getTextTitle().contains("要求学分")) {
                    //例如: 通识教育 | 必修 要求学分:56.00
                    String scoreStr = table[i].get(1).getTextTitle().replaceAll("[^\\d.]*", "");
                    //发现的一个bug: "选修 要求学分:6.00 获得学分:2.0 选择其他专业的学科基础课（1门以"解析成了6.001
                    if (scoreStr.charAt(scoreStr.length() - 1) == '1')
                        scoreStr = scoreStr.substring(0, scoreStr.length() - 1);
                    needScore[loaded] = Double.valueOf(scoreStr);

                    if (loaded > 0) {
                        //至少是第二类课程的开头了, 这时候上一个课程的学分否已经加好了
                        if (gotScore > needScore[loaded - 1])
                            scoreTooMuch[loaded - 1] = true;
                    }

                    gotScore = score[loaded++];

                } else if (table[i].getSize() == 12 && table[i].get(3 + currentSemester).getTextTitle().equals("未读")) {
                    //if (loaded == 8)
                    //    System.out.println("debug");

                    //课程类别
                    table[i].popFirst();
                    courseIdAndLink = table[i].popFirst();
                    courseName = table[i].popFirst().getTextTitle();
                    courseScore = Double.valueOf(table[i].popFirst().getTextTitle());
                    gotScore += courseScore;
                    courseTypes[loaded - 1].addLast(new CourseType(new HyperlinkURL(courseName, courseIdAndLink.getLink()),
                            courseIdAndLink.getTextTitle(), courseScore, null, null,
                            ""));
                    courseCnt++;
                }
            }
            //最后一种课程了
            if (gotScore > needScore[loaded - 1])
                scoreTooMuch[loaded - 1] = true;

            //结果
            CourseType[] result = new CourseType[courseCnt];
            courseCnt = 0;

            //反馈信息
            StringBuilder callback = new StringBuilder();
            callback.append("已导入课程: \n");
            for (int i = 0; i < 8; i++) {
                callback.append(courseKinds[i]).append(":\n");

                if (scoreTooMuch[i])
                    callback.append("警告: 获得学分 + 已选学分超过要求学分了, 请直行删减.(本学期可选学分: ")
                            .append(needScore[i] - score[i]).append(")\n");

                if (courseTypes[i].getSize() > 0) {
                    while (!courseTypes[i].isEmpty()) {
                        CourseType type = courseTypes[i].popFirst();
                        result[courseCnt++] = type;
                        callback.append(type.getCourse().getTextTitle())
                                .append(" 学分: ").append(type.getScore()).append("\n");
                    }
                }
            }

            callbackMessage.setP(callback.toString());

            return result;
        } catch (IOException ioe) {
            //进入教务处选课首页的时候发生IOException
            System.err.println("httpClient进入教务处选课首页的时候发生IOException");
            return null;
        }

    }

    public String getCourseId() {
        return courseId;
    }

    public HyperlinkURL getCourse() {
        return course;
    }

    public double getScore() {
        return score;
    }

    public String getMajor() {
        return major;
    }

    public URL getCourseSyllabus() {
        return courseSyllabus;
    }

    public URL getCourseCalendar() {
        return courseCalendar;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getRemark() {
        return remark;
    }
}///~
