package tk.dcmmcc;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import tk.dcmmcc.utils.DoubleLinkedList;
import tk.dcmmcc.utils.HyperlinkURL;
import tk.dcmmcc.utils.Table;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Scanner;

/**
 * 封装Course的各类信息, Course指的是具体某个老师开的这个节次的课
 * TODO 对表格的处理应该大改一波, 不能按照每一列都是绝对的数据来了, 因为每个学期都可能会更新一点点
 *  必须按照先对第一行进行解析, 来动态的得到每一列的意义...
 * Created by DCMMC on 2017/8/28.
 */
public class Course {
    //查看选课结果的URL
    private final static String selectedCourse =
            "http://jwdep.dhu.edu.cn/dhu/student/selectcourse/seeselectedcourse.jsp";
    //获得某个课程的开班信息的URL的prefix
    //可以通过POST的方式传入courseId和courseName(记得java.net.URLEncoder.encode( , "utf-8")来对中文转码)来获取
    //courseId和courseName可以在选课首页找到, 有些大类合在一起的课程(比如外语类)需要点进去才能看到
    private final static String prefixTeachClassList =
            "http://jwdep.dhu.edu.cn/dhu/student/selectcourse/teachclasslist.jsp";
    //公共查询(记得java.net.URLEncoder.encode( , "utf-8")来对中文转码)
    //加上courseId作为参数(最好再加一个courseName)就可以POST查询到这个课程对应的所有开班时间表了
    private final static String prefixTimeTable =
            "http://jwdep.dhu.edu.cn/dhu/commonquery/coursetimetableinfo.jsp";
    //教学日历
    private URL calendarURL;

    private HyperlinkURL course;
    private String courseId;
    private String courseNo;
    private String classNo;
    private Teacher teacher;
    private int max;
    private int selected;
    private int matriculated;
    private int startWeek;
    private int endWeek;
    private ClassTime[] classTimes;
    private String[] places;
    private double score;

    //HttpClient
    private static HttpClient httpClient = new HttpClient();

    /* 静态实例初始化 */
    static {
        // 设置 HttpClient 接收 Cookie,用与浏览器一样的策略
        httpClient.getParams().setCookiePolicy(
                CookiePolicy.BROWSER_COMPATIBILITY);
    }

    //默认构造器
    public Course() {

    }

    /**
     * 根据参数创建Course对象
     * @param course 课程名称以及链接
     * @param courseId 课程编号
     * @param courseNo 选课序号
     * @param classNo 组班号(班级序号)
     * @param teacher 教师信息
     * @param score 学分
     * @param max 允许最大人数
     * @param selected 申请人数
     * @param matriculated 录取人数
     * @param startWeek 开始周次
     * @param endWeek 结束周次
     * @param classTimes 在某一个周次范围内每一周的每一天的上课节次, 因为有些课可能1-8周和9-16周上课节次不一样, 所以需要用
     *                   一个数组来储存起来
     * @param places 上课地点(个数应该和classTimes中的元素一样)
     */
    public Course(HyperlinkURL course, String courseId, String courseNo, String classNo, Teacher teacher, double score,
                  int max, int selected, int matriculated, int startWeek, int endWeek, ClassTime[] classTimes,
                  String[] places) {
        this.courseId = courseId;
        this.courseNo = courseNo;
        this.classNo = classNo;
        this.teacher = teacher;
        this.max = max;
        this.selected = selected;
        this.matriculated = matriculated;
        this.startWeek = startWeek;
        this.endWeek = endWeek;
        this.classTimes = classTimes;
        this.course = course;
        this.places = places;
        this.score = score;
    }


    /**
     * 获取某一周的某一天该课程的节次安排
     * @return 如果周次非法或者确实没有节次都是返回null
     * @throws IllegalArgumentException 如果周次不在范围内或者非法都是返回这个
     */
    public int[] getClassTimeDayWeek(int week, int day) throws IllegalArgumentException {
        if (week > endWeek || week < startWeek || day < 1 || day > ClassTime.MAX_DAYS_OF_WEEK)
            throw new IllegalArgumentException("不在范围内!");

        int[] tmpWeekRange;
        for (ClassTime c : classTimes) {
            tmpWeekRange = c.getWeekRange();
            if (week >= tmpWeekRange[0] && week <= tmpWeekRange[1])
                // BugFixed: 修复形如1—16周	周一.5.6节 1—8周	周四.1.2节这样的时候1-16中发现没有周四的课就诶过直接诶
                //返回null的情况
                if (c.getClassTimeThisDay(day) != null)
                    return c.getClassTimeThisDay(day);
        }

        return null;
    }

    /**
     * 适用于http://jwdep.dhu.edu.cn/dhu/student/selectcourse/teachclasslist.jsp和
     * http://jwdep.dhu.edu.cn/dhu/commonquery/coursetimetableinfo.jsp查询到的课表
     * 把table中指定的部分解析到Course
     * @param startIndex table中要扫描部分的起始index(包括)
     * @param endIndex table中要扫描部分的结束index(不包括)
     * @param table table
     * @param courseType 课程
     * @return 该课程下的所有课
     */
    private static Course[] getCoursesFromTable(int startIndex, int endIndex, DoubleLinkedList<HyperlinkURL>[] table,
                                      CourseType courseType) {

        String thisCourseNo, thisClassNo;
        int thisMax = 0, thisMatriculated = 0, thisSelected = 0;
        Teacher thisTeacher;

        Course[] courses = new Course[endIndex - startIndex];

        //跳过第startIndex之前的行
        for (int i = startIndex; i < endIndex; i++) {
            thisCourseNo = table[i].popFirst().getTextTitle();
            thisClassNo = table[i].popFirst().getTextTitle();
            thisMax = Integer.valueOf(table[i].popFirst().getTextTitle());
            thisSelected = Integer.valueOf(table[i].popFirst().getTextTitle());
            thisMatriculated = Integer.valueOf(table[i].popFirst().getTextTitle());
            //Major
            table[i].popFirst();
            HyperlinkURL teacher = table[i].popFirst();
            thisTeacher = new Teacher(teacher.getTextTitle(), teacher.getLink());

            String weekRange, classTime;

            int[] lastWeekStartAndEnd = new int[2];
            String lastPlace = "";
            DoubleLinkedList<String> dayAndClass = new DoubleLinkedList<>();
            //课程在某个周次范围的上课节次
            DoubleLinkedList<ClassTime> coursesTimeList = new DoubleLinkedList<>();
            DoubleLinkedList<String> places = new DoubleLinkedList<>();
            while (!table[i].isEmpty() && !table[i].getFirst().getTextTitle().equals("")) {
                weekRange = table[i].popFirst().getTextTitle();
                classTime = table[i].popFirst().getTextTitle();

                //解析周次
                int weekStart, weekEnd;
                weekRange = weekRange.replace("周", "");
                if (weekRange.equals("")) {
                    //如果为空, 就把weekStart和weekEnd都令为-1
                    weekEnd = -1;
                    weekStart = -1;
                } else  {
                    weekStart = Integer.valueOf(weekRange.split("—")[0]);
                    weekEnd = Integer.valueOf(weekRange.split("—")[1]);
                }


                //解析节次
                classTime = classTime.replace("周", "").replace("节", "");
                int day;
                if (classTime.equals("")) {
                    day = -1;
                } else  {
                    day = classTime.charAt(0);
                    classTime = classTime.substring(2).replace(".", ",");

                    switch (day) {
                        case '一' : day = 1;break;
                        case '二' : day = 2;break;
                        case '三' : day = 3;break;
                        case '四' : day = 4;break;
                        case '五' : day = 5;break;
                        case '六' : day = 6;break;
                        case '日' : day = 7;break;
                        default: day = 0;
                    }
                }

                //lastWeekStartAndEnd[0] != 0 表示不是第一次解析
                if (lastWeekStartAndEnd[0] != 0 && (lastWeekStartAndEnd[0] != weekStart
                        || lastWeekStartAndEnd[1] != weekEnd)) {
                    ClassTime ct = new ClassTime(lastWeekStartAndEnd[0], lastWeekStartAndEnd[1]
                            , dayAndClass.toArray());
                    coursesTimeList.addLast(ct);
                    places.addLast(lastPlace);

                    dayAndClass = new DoubleLinkedList<>();
                    lastWeekStartAndEnd[0] = weekStart;
                    lastWeekStartAndEnd[1] = weekEnd;
                }

                //place
                lastPlace = table[i].popFirst().getTextTitle();
                dayAndClass.addLast(day + "");
                dayAndClass.addLast(classTime);

                //该行的最后一个时间表了
                if (table[i].isEmpty()) {
                    ClassTime ct = new ClassTime(weekStart, weekEnd, dayAndClass.toArray());
                    coursesTimeList.addLast(ct);
                    places.addLast(lastPlace);
                }

            }

            //debug
            //System.out.println(coursesTimeList.toString());

            try {

                /*
                * 注意: 学分在这里查不到!
                 */
                courses[i - startIndex] = new Course(new HyperlinkURL(courseType.getCourse().getTextTitle(),
                        new URL(prefixTeachClassList + "?courseId=" + courseType.getCourseId()
                                + "&courseName=" + URLEncoder.encode(courseType.getCourse().getTextTitle(),
                                "utf-8"))),
                        courseType.getCourseId(), thisCourseNo, thisClassNo, thisTeacher,
                        0.0d, thisMax, thisSelected, thisMatriculated,
                        coursesTimeList.getSize() == 0 ? -1 : coursesTimeList.getLast().getWeekRange()[0],
                        coursesTimeList.getSize() == 0 ? -1 : coursesTimeList.getLast().getWeekRange()[1],
                        coursesTimeList.getSize() == 0 ? new ClassTime[]{new ClassTime(-1, -1)}
                        : coursesTimeList.toArray(),
                        places.toArray());
            } catch (UnsupportedEncodingException ue) {
                //...
                System.err.println("不支持的URLEncoder类型: " + ue.getMessage());
            } catch (MalformedURLException me) {
                //..
                System.err.println("URL格式错误: " + me.getMessage());
            }
        }

        return courses;
    }

    /**
     * 在教务处选课首页通过CourseType对象(至少要courseId和courseName)来获得开班信息(也就是这个课程的所有班级)
     * courseId或courseName有一个错误就会引发 500 Servlet Exception
     * @param courseType 课程信息
     * @param userCookie 用户Cookie
     * @return 该课程的所有班级, 出现任何问题都返回null, 比如选了已经选过的或者是已经有了成绩的或者出现了异常
     */
    public static Course[] getCourses(CourseType courseType, String userCookie) {
        //验证CourseType对象是否有courseId和courseName
        if (courseType.getCourse().getTextTitle() == null || courseType.getCourseId() == null) {
            System.err.println("CourseType对象连必要的courseId和courseName都没有");
            return null;
        }

        try {
            //查看该课程的老师开班情况的POST
            PostMethod classesList = new PostMethod(prefixTeachClassList);
            //记得utf-8编码...
            classesList.setRequestBody(new Header[]{new Header("courseId", courseType.getCourseId()),
                    new Header("courseName",
                            URLEncoder.encode(courseType.getCourse().getTextTitle(), "utf-8"))});

            classesList.setRequestHeader("cookie", userCookie);
            //post
            httpClient.executeMethod(classesList);

            Scanner scanner = new Scanner(new BufferedInputStream(classesList.getResponseBodyAsStream()), "gbk");
            String responsePage = "";
            String line;
            while (scanner.hasNextLine()) {
                //如果已经选了这门课不允许再次选择或者是没有开放选课, 就返回null
                if ((line = scanner.nextLine()).contains("不允许再次选择") || line.contains("没有开放选课")) {
                    System.err.println("已经选了这门课不允许再次选择或者是没有开放选课");
                    return null;
                }

                if (line.contains("500 Servlet Exception")) {
                    System.err.println("courseName或courseId错误!");
                    return null;
                }

                responsePage += line + "\n";
            }

            DoubleLinkedList<HyperlinkURL>[] table = new Table(responsePage, new URL(prefixTeachClassList)).getTable();

            return getCoursesFromTable(1, table.length, table, courseType);
        } catch (UnsupportedEncodingException ue) {
            //The Character Encoding is not supported.
            System.err.println("把课程名称encode的时候报错: The Character Encoding is not supported.");
            return null;
        } catch (IOException ioe) {
            //连接教务处服务器的时候发生IO异常
            System.err.println("连接教务处服务器的时候发生IO异常");
            return null;
        }
    }

    /**
     * 从公共查询中获取课程信息
     * 这样就可以不用因为选过了或者学院问题进不去了
     * courseType只需要提供courseId即可, 不过要是通过CourseType.searchCourse()得到的结果就更加好了
     */
    public static Course[] getCourseFromCommonSearch(CourseType courseType, String userCookie) throws IllegalCourseException {
        if (courseType.getCourseId().equals(""))
            throw new IllegalCourseException("CourseType至少要包含courseId!");

        try {
            PostMethod courseTimeInfo = new PostMethod(prefixTimeTable);
            courseTimeInfo.setRequestBody(new Header[]{new Header("courseId", courseType.getCourseId()),
                new Header("courseName", courseType.getCourse().getTextTitle())});
            courseTimeInfo.setRequestHeader("cookie", userCookie);

            httpClient.executeMethod(courseTimeInfo);

            String response = "";
            Scanner scanner = new Scanner(new BufferedInputStream(courseTimeInfo.getResponseBodyAsStream()),
                    "gbk");
            while (scanner.hasNextLine())
                response += scanner.nextLine() + "\n";

            DoubleLinkedList<HyperlinkURL>[] table = new Table(response, new URL(prefixTimeTable + "?classId="
                    + courseType.getCourseId() + "&courseName="
                    + URLEncoder.encode(courseType.getCourse().getTextTitle(), "utf-8"))).getTable();


            return getCoursesFromTable(2, table.length, table, courseType);
        } catch (IOException ioe) {
            //连接教务处的时候发生IOException
            System.err.println("连接教务处的时候发生IOException: " + ioe.getMessage());
            return null;
        }
    }

    /**
     * 查看已选课程
     * P.S. 如果课程没有时间安排, 那么weekStart和weekEnd都是-1, ClassTime中的day也都是-1
     * P.P.S. 这里生成的Course中的链接是课程纲要
     * @param userCookie 用户Cookie
     * @return 第一个数组是已录取的, 第二个是未录取的
     */
    private static Course[][] getAllSelectedCourses(String userCookie) {
        //先去查看选课结果中找到组班号
        GetMethod selected = new GetMethod(selectedCourse);
        selected.setRequestHeader("cookie", userCookie);

        try {
            httpClient.executeMethod(selected);

            //debug
            //String strDebug = selected.getResponseBodyAsString();

            Scanner scanner = new Scanner(new BufferedInputStream(selected.getResponseBodyAsStream()), "gbk");
            String responsePage = "";
            //已经录取课程数量
            int matriculateCount = 0;
            //已经录取的学分
            double matriculateScore = 0.0d;
            //已经选择但是没有录取的课程数量
            int selectedCount = 0;
            //已经选择但是没有录取的课程的学分
            double selectedScore = 0.0d;
            //按照html代码, 上述是个变量在页面中分别是count count0 count1 count2 count3的innerText成员


            String line;
            while (scanner.hasNextLine()) {
                if ((line = scanner.nextLine()).contains(".innerText=")) {
                    if (line.contains("count."))
                        matriculateCount = Integer.valueOf(line.replaceAll("[^\\d.]+", "").substring(1));
                    else if (line.contains("count0"))
                        matriculateScore = Double.valueOf(line.replaceAll("[^\\d.]+", "").substring(2));
                    else if (line.contains("count2"))
                        selectedCount = Integer.valueOf(line.replaceAll("[^\\d.]+", "").substring(2));
                    else if (line.contains("count3"))
                        selectedScore = Double.valueOf(line.replaceAll("[^\\d.]+", "").substring(2));
                }
                responsePage += line + "\n";
            }


            DoubleLinkedList<HyperlinkURL>[] table = new Table(responsePage, new URL(selectedCourse)).getTable();

            //已入取课程
            Course[] matriculatedCourse = new Course[matriculateCount];
            //已选但是未录取课程
            Course[] selectedCourse = new Course[selectedCount];

            String thisCourseId, thisClassNo;
            HyperlinkURL thisCourse;
            double thisScore;
            Teacher thisTeacher;
            URL thisCalendar;

            // UPDATE: 教务处在关闭选课的时候查看选课列表中的删除按钮就没了
            boolean selectClosed = DHUCurrentUser.getCurrentInfo(userCookie).equals("教务处已经停止选课!");

            //一般有意义的表格行的元素个数为13或者16的样子， 那些元素个数为0或者1的就是无效的
            //记录当前存入的课程数目， 如果大于已录取数目, 就代表正在处理已选择但是未录取的课程
            int courseLoadedCount = 0;
            //是否是单双周模式
            boolean isSingleDoubleWeekMode = false;
            //FIXME 默认单周
            boolean isSingleWeek = true;
            //FIXME 如果每一行元素布局改变(教务处更新的话), 程序将会出错!!!
            for (int i = 0; i < table.length; i++) {
                // FIXME 这样的假设不够严谨, 是不是要设置阀值为别的会更加好一点?
                //如果该行元素只有1个或者0个或者6个(6个是已选择但是未录取那一个表格的第一行的内容), 就说明是无用行
                if (table[i].getSize() < 8)
                    continue;

                //这门课将被读取
                courseLoadedCount++;

                thisCourseId = table[i].popFirst().getTextTitle();
                thisCourse = table[i].popFirst();
                thisScore = Double.valueOf(table[i].popFirst().getTextTitle());
                //优选专业, 只会在已选但是未录取里面有
				if (courseLoadedCount > matriculateCount)
					table[i].popFirst();
                //课程类别
                table[i].popFirst();
                thisClassNo = table[i].popFirst().getTextTitle();
                //教学日历
                thisCalendar = table[i].popFirst().getLink();
                //删除按钮
                // UPDATE: 教务处在关闭选课的时候查看选课列表中的删除按钮就没了
                HyperlinkURL delLink = table[i].popFirst();
                if (selectClosed) {
                    //如果不是删除按钮, 就add回去
                    table[i].addFirst(delLink);
                }

                //教务处第一次选课的时候, 会在这里多一个是否录取的一列
                //暴力判断一波
                if (table[i].getFirst().getTextTitle().matches("[是否]{1}"))
                    table[i].popFirst();

                HyperlinkURL teacher = table[i].popFirst();

                //debug
                //if (teacher.getTextTitle().equals("周鸿颖")) {
                //    System.out.println("debug");
                //}
                //if ((teacher.getTextTitle().equals("") || teacher.getTextTitle() == null)
                //        && table[i].getFirst().getTextTitle().equals(""))
                //    System.out.println("debug");

                thisTeacher = new Teacher(teacher.getTextTitle(), teacher.getLink());

                String weekRange, classTime;

                int[] lastWeekStartAndEnd = new int[2];
                String lastPlace = "";
                DoubleLinkedList<String> dayAndClass = new DoubleLinkedList<>();
                //课程在某个周次范围的上课节次
                DoubleLinkedList<ClassTime> coursesTimeList = new DoubleLinkedList<>();
                DoubleLinkedList<String> places = new DoubleLinkedList<>();
                while (!table[i].isEmpty() && !table[i].getFirst().getTextTitle().equals("")) {
                    weekRange = table[i].popFirst().getTextTitle();
                    //debug
                    //if (table[i].isEmpty())
                    //    System.out.println("debug");

                    classTime = table[i].popFirst().getTextTitle();

                    //debug
                    //if (weekRange.contains("单周")) {
                    //    System.out.println("单周");
                    //}

                    //解析周次
                    int weekStart, weekEnd;
                    weekRange = weekRange.replace("周", "");
                    //有些可能是单双周的
                    if (weekRange.matches(".*\\([单双]\\).*")) {
                        isSingleDoubleWeekMode = true;
                        isSingleWeek = weekRange.matches(".*单.*");
                        weekRange = weekRange.replaceFirst("\\(.*\\)", "");
                    }

                    if (weekRange.equals("")) {
                        //如果为空, 就把weekStart和weekEnd都令为-1
                        weekEnd = -1;
                        weekStart = -1;
                    } else  {
                        weekStart = Integer.valueOf(weekRange.split("—")[0]);
                        weekEnd = Integer.valueOf(weekRange.split("—")[1]);
                    }


                    //解析节次
                    classTime = classTime.replaceFirst("周", "").replace("节", "");
                    int day;
                    if (classTime.equals("")) {
                        day = -1;
                    } else  {
                        day = classTime.charAt(0);
                        classTime = classTime.substring(2).replace(".", ",");

                        switch (day) {
                            case '一' : day = 1;break;
                            case '二' : day = 2;break;
                            case '三' : day = 3;break;
                            case '四' : day = 4;break;
                            case '五' : day = 5;break;
                            case '六' : day = 6;break;
                            case '日' : day = 7;break;
                            default: day = 0;
                        }
                    }

                    //lastWeekStartAndEnd[0] != 0 表示不是第一次解析
                    if (lastWeekStartAndEnd[0] != 0 && (lastWeekStartAndEnd[0] != weekStart
                            || lastWeekStartAndEnd[1] != weekEnd)) {
                        ClassTime ct = isSingleDoubleWeekMode ? new ClassTime(isSingleWeek, lastWeekStartAndEnd[0],
                                lastWeekStartAndEnd[1], dayAndClass.toArray())
                                : new ClassTime(lastWeekStartAndEnd[0], lastWeekStartAndEnd[1]
                                , dayAndClass.toArray());
                        coursesTimeList.addLast(ct);
                        places.addLast(lastPlace);

                        dayAndClass = new DoubleLinkedList<>();
                    }

                    //place
                    lastPlace = table[i].popFirst().getTextTitle();
                    dayAndClass.addLast(day + "");
                    dayAndClass.addLast(classTime);
                    lastWeekStartAndEnd[0] = weekStart;
                    lastWeekStartAndEnd[1] = weekEnd;

                    //该行的最后一个时间表了
                    if (table[i].isEmpty()) {
                        ClassTime ct = isSingleDoubleWeekMode ? new ClassTime(isSingleWeek, weekStart, weekEnd, dayAndClass.toArray())
                            : new ClassTime(weekStart, weekEnd, dayAndClass.toArray());
                        coursesTimeList.addLast(ct);
                        places.addLast(lastPlace);
                    }
                }

                //如果该死的那个上课周次那三格直接放了一个空的table
                if (coursesTimeList.isEmpty()) {
                    coursesTimeList.addLast(new ClassTime(-1, -1));
                }

                int minWeekStart = 16, maxWeekEnd = -1;
                for (ClassTime c : coursesTimeList) {
                    if (c.getWeekRange()[0] < minWeekStart)
                        minWeekStart = c.getWeekRange()[0];
                    if (c.getWeekRange()[1] > maxWeekEnd)
                        maxWeekEnd = c.getWeekRange()[1];
                }


                if (courseLoadedCount <= matriculateCount)
                    //创建该行对应的Course对象
                    matriculatedCourse[courseLoadedCount - 1] = new Course(thisCourse, thisCourseId, "", thisClassNo, thisTeacher, thisScore,
                            0, 0, 0, minWeekStart, maxWeekEnd,
                            coursesTimeList.getSize() == 0 ? new ClassTime[]{new ClassTime(-1, -1)}
                            : coursesTimeList.toArray(),
                            places.toArray()).setCalendarURL(thisCalendar);
                else
                    //创建该行对应的Course对象
                    selectedCourse[courseLoadedCount - matriculateCount - 1] = new Course(thisCourse, thisCourseId, "", thisClassNo, thisTeacher, thisScore,
                            0, 0, 0, minWeekStart, maxWeekEnd,
                            coursesTimeList.getSize() == 0 ? new ClassTime[]{new ClassTime(-1, -1)}
                                    : coursesTimeList.toArray(), places.toArray()).setCalendarURL(thisCalendar);
            }

            return new Course[][]{matriculatedCourse, selectedCourse};
        } catch (IOException ioe) {
            //进入查看已选页面时发生IO异常
            System.err.println("进入查看已选页面时发生IO异常");
            return null;
        }
    }

    /**
     * 获得已录取的课程
     * @return 已录取的课程
     */
    public static Course[] getMatriculated(String userCookie) {
        Course[][] result = getAllSelectedCourses(userCookie);

        return result == null ? null : result[0];
    }

    /**
     * 获取已选择但是未录取的课程
     * @return 已选择但是未录取的课程
     */
    public static Course[] getSelected(String userCookie) {
        Course[][] result = getAllSelectedCourses(userCookie);

        return result == null ? null : result[1];
    }


    public HyperlinkURL getCourse() {
        return course;
    }

    public ClassTime[] getClassTimes() {
        return classTimes;
    }

    public int getStartWeek() {
        return startWeek;
    }

    public int getEndWeek() {
        return endWeek;
    }

    public String getCourseId() {
        return courseId;
    }

    public String getCourseNo() {
        return courseNo;
    }

    public String getClassNo() {
        return classNo;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public String[] getPlaces() {
        return places;
    }

    public int getMax() {
        return max;
    }

    public int getSelected() {
        return selected;
    }

    public int getMatriculated() {
        return matriculated;
    }

    public double getScore() {
        return score;
    }

    public void setClassNo(String classNo) {
        this.classNo = classNo;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public Course setCalendarURL(URL calendarURL) {
        this.calendarURL = calendarURL;

        return this;
    }

    public URL getCalendarURL() {
        return calendarURL;
    }
}///~


