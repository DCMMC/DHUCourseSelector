package tk.dcmmcc;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import tk.dcmmcc.utils.HyperlinkURL;
import tk.dcmmcc.utils.Pointer;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * TODO 记得把main()中的用户名和密码设置好
 * UPDATE 教务处选课系统未开放的处理
 * TODO 教务处系统卡炸了出现Service Unavailable的情况
 * 当前用户的教务处登录信息
 * Created by DCMMC on 2017/8/28.
 */
public class DHUCurrentUser {
    /* Fields */
    //新版教务处首页
    private final static String jwdepLogin =
            "http://jwdep.dhu.edu.cn/dhu/login_wz.jsp";
    //选课首页
    private final static String selectCourse =
            "http://jwdep.dhu.edu.cn/dhu/student/selectcourse/selectCourse_ts.jsp";
    //确认选课界面的URL的prefix
    private final static String prefixSelectCourse =
            "http://jwdep.dhu.edu.cn/dhu/student/selectcourse/selectcourse2.jsp?";

    //各种选课请求的统一URL的prefix
    /*
    * 各类请求的Body列表:
    * 1. 确认选课:
    * doWhat : selectcourse
    * courseId 课程编号(课程类别对应的编号)
    * courseNo 课程序号(具体某个老师开的某个时间的课程的序号)
    * courseName 课程名称
    * studentId 学号
    * majorId 专业代码
    * enterYear 入学年份
    * yearTerm 选课学期
    * teacherId 教师代码
    * selectCourseStatus 选课状态
    * 以上信息可以在http://jwdep.dhu.edu.cn/dhu/student/selectcourse/selectcourse2.jsp这个URL中以POST形式传入courseNo
    * 和courseId这两个参数进入的确认页面中获得
    * 2. 删除已入取课程:
    * doWhat : deletematriculatedcourse
    * classNo 组班号
    * courseId
    * 3. 删除已选但是没有录取的课程
    * doWhat : deleteselectedcourse
    * courseNo
    * 以上2和3的信息可以在http://jwdep.dhu.edu.cn/dhu/student/selectcourse/seeselectedcourse.jsp中查看到
     */
    private final static String prefixCourseController =
            "http://jwdep.dhu.edu.cn/dhu/servlet/com.collegesoft.eduadmin.tables.selectcourse.SelectCourseController";

    //当前用户的Cookie
    private String userCookie = "";
    //HttpClient
    private HttpClient httpClient = new HttpClient();
    //log
    private static Logger logger = Logger.getLogger(DHUCurrentUser.class.getName());

      /* 静态实例初始化 */
    static {

    }


    /* Constructors */
    //cannot instanced by defaults
    private DHUCurrentUser() {

    }

    /**
     * 从参数指定的帐号和密码进行登录
     * @param loginUser 帐号
     * @param loginPwd 密码
     * @throws JwdepConnectionException 如果在网络测试的时候遇到网络问题或者其他非致命问题就抛出该异常
     */
    public DHUCurrentUser(String loginUser, String loginPwd) throws JwdepConnectionException, AccountLoginException {
        //测试网络
        try {
            HttpURLConnection testConn = (HttpURLConnection)new URL(jwdepLogin).openConnection();
            //超时时间5s
            testConn.setConnectTimeout(5000);
            testConn.setUseCaches(false);

            int statue = testConn.getResponseCode();
            if (200 != statue)
                throw new JwdepConnectionException("连接失败! 服务端返回错误代码: " + statue);
        } catch (MalformedURLException me) {
            //致命问题
            logger.severe("教务处首页网址格式错误.");
            throw new RuntimeException(me);
        } catch (IOException ioe) {
            if (ioe instanceof SocketTimeoutException)
                //网络超时
                logger.severe("网络超时!");
            throw new JwdepConnectionException("打开校务处连接的时候发生了IO异常.", ioe);
        }

        // 设置 HttpClient 接收 Cookie,用与浏览器一样的策略
        httpClient.getParams().setCookiePolicy(
                CookiePolicy.BROWSER_COMPATIBILITY);

        //模拟Post登录
        PostMethod postMethod = new PostMethod(jwdepLogin);

        //用户名和密码
        postMethod.setRequestBody(new Header[]{new Header("userName", loginUser),
                new Header("userPwd", loginPwd)});
        try {
            httpClient.executeMethod(postMethod);
            //获得登陆后的Cookie
            Cookie[] cookies = httpClient.getState().getCookies();
            for (Cookie c : cookies)
                userCookie += c.toString() + ";";

            //判断登录后转跳到界面是否是网站登录问题反馈
            //debug
            //String debug = postMethod.getResponseBodyAsString();
            //System.out.println(debug);

            BufferedInputStream bis = new BufferedInputStream(postMethod.getResponseBodyAsStream());
            //编码记得是GBK
            Scanner scanner = new Scanner(bis, "GBK");
            boolean errFlag = false;
            String line;
            while (scanner.hasNextLine()) {
                //成功
                if ((line = scanner.nextLine()).contains("http://jwdep.dhu.edu.cn/dhu/student"))
                    break;

                //失败
                if (line.contains("网站登录问题反馈"))
                    errFlag = true;

                int index;
                if (errFlag && (index = line.indexOf("服务器的返回信息")) != -1) {
                    index = line.indexOf("red>", index);
                    String errMsg = "";
                    for (int i = index + 4; i < line.length(); i++)
                        if (line.charAt(i) == '<')
                            throw new AccountLoginException("登录出错: " + errMsg);
                        else
                            errMsg += line.charAt(i);
                }
            }
        } catch (IOException ioe) {
            //登录post的时候发生IO异常
            logger.severe("登录post的时候发生IO异常");
            throw new AccountLoginException("登录post的时候发生IO异常", ioe);
        }
    }

    /* Public Methods */

    /**
     * 获取该用户的Cookie
     * @return 由构造器得到的Cookie
     */
    public String getUserCookie() {
        return this.userCookie;
    }

    /**
     * 选课请求
     * 错误的代码会引起jsp返回500错误页面!
     * @param courseNo 课程序号(也就具体要选的那个老师的那个课的代码)
     * @param courseId 课程编号(也就是这一类科目的代码)
     * @return 如果网络超时或者满了的话, 就返回false, 如果选到了或者已经早就选到了, 就返回true
     */
    public boolean selectCourse(String courseNo, String courseId) throws IllegalCourseException {
        return selectCourse(courseNo, courseId, this.userCookie);
    }

    /**
     * 选课请求
     * 错误的代码会引起jsp返回500错误页面!
     * 研究发现courseNo正确但是courseId也可以, 所以courseNo最重要
     * @param courseNo 课程序号(也就具体要选的那个老师的那个课的代码)
     * @param courseId 课程编号(也就是这一类科目的代码)
     * @return 如果满了的话, 就返回false, 如果选到了或者已经早就选到了, 就返回true
     */
    public static boolean selectCourse(String courseNo, String courseId, String userCookie) throws IllegalCourseException {
        //每一个静态方法都要使用一个新的httpClient, 不然多个请求到同一个httpClient会出现IOEception
        HttpClient httpClient0 = new HttpClient();
        // 设置 HttpClient 接收 Cookie,用与浏览器一样的策略
        httpClient0.getParams().setCookiePolicy(
                CookiePolicy.BROWSER_COMPATIBILITY);

        try {
            //courseId和courseNo只取前6位(教务处的设定)
            String urlSelectCourse = prefixSelectCourse + "courseNo=" + courseNo + "&courseId=" + courseId;

            //选课确认页面
            PostMethod getConfirmPage = new PostMethod(urlSelectCourse);
            getConfirmPage.setRequestHeader("cookie", userCookie);
            //post
            httpClient0.executeMethod(getConfirmPage);

            BufferedInputStream bis = new BufferedInputStream(getConfirmPage.getResponseBodyAsStream());
            Scanner scanner = new Scanner(bis, "GBK");

            StringBuilder responsePage = new StringBuilder();
            String line;
            while (scanner.hasNextLine()) {
                line = scanner.nextLine();

                if (line.contains("对不起人数已满"))
                    return false;

                if (line.contains("错误的参数"))
                    //说明courseNo或者courseId参数错误
                    throw new IllegalCourseException("教务处报错: 错误的参数, 可能原因: courseId(" + courseId +
                            ")或者courseNo(" + courseNo + ")有问题.");

                if (line.contains("课程选课没有开放"))
                    //说明courseNo或者courseId参数错误
                    throw new IllegalCourseException("教务处报错: 课程选课没有开放, 可能原因: courseId(" + courseId +
                            ")或者courseNo(" + courseNo + ")有问题.");

                responsePage.append(line).append("\n");
            }

            //待会选课请求需要的所有的bodys的keys
            String[] keys = ("doWhat,courseId,courseNo,courseName,studentId,majorId,enterYear,yearTerm,teacherId," +
                    "selectCourseStatus").split(",");
            scanner = new Scanner(responsePage.toString());
            //标识是否已经到了目标form那一行了
            boolean reachForm = false;
            //临时存储value
            String value = "";
            //Headers
            Header[] headers = new Header[keys.length];
            int cnt = 0;

            //获取Headers
            while (scanner.hasNextLine()) {
                line = scanner.nextLine();

                //debug
                //System.out.println(line);

                if (line.contains("post\"")) {
                    reachForm = true;
                    continue;
                }


                //因为查询的时候没有courseName这一个参数, 所以确认选课界面中的courseName也是null, 不过研究发现
                //提交选课请求的时候其实只有只有courseNo会被校验... 其他竟然连studentId和courseId都不会被校验...
                if (reachForm)
                    for (String s : keys)
                        if (line.contains(s)) {
                            for (int i = line.indexOf(s) + s.length() + 9; line.charAt(i) != '\"' && i < line.length(); i++)
                                value += line.charAt(i);
                            value = value.replaceAll("[\\s\r\n]*", "");
                            headers[cnt++] = new Header(s, value);
                            value = "";
                            break;
                        }

                if (cnt == keys.length)
                    break;
            }

            //页面中的信息不够所有headers, 那说明Course的信息或者页面有问题
            if (cnt != keys.length)
                throw new IllegalCourseException("Course的信息不正确或者页面有问题!", new Course(null, courseId, courseNo,
                        "", null, 0.0d, 0, 0, 0, 0, 0,
                        null, null));

            //确认提交选课请求
            PostMethod confirmSelect = new PostMethod(prefixCourseController);
            //set bodies
            confirmSelect.setRequestBody(headers);
            //set Cookie
            confirmSelect.setRequestHeader("cookie", userCookie);
            //post!
            //超时4s
            httpClient0.getParams().setConnectionManagerTimeout(4000);
            httpClient0.executeMethod(confirmSelect);

            //如果满了, 返回的页面是一个值alert弹窗提示: 录取人数已满，请重选其它课程！
            scanner = new Scanner(new BufferedInputStream(confirmSelect.getResponseBodyAsStream()), "gbk");

            //如果原来就已经录取了或者这次选课成功了, 就是返回到教务处选课首页
            String tmpLine = scanner.nextLine();
            if (tmpLine.contains("err.jsp?errMsg"))
                throw new Exception("发生了严重错误! 错误信息: " + tmpLine);

            //TODO 添加开关是否确认选择有冲突的课程
            //处理转跳事件, 比如课程冲突的时候会转跳到冲突提示界面
            if (tmpLine.contains("The URL has moved")) {
                //debug
                logger.warning("所选课程与已选课程有冲突或者已经选了!");

                //debug
                //logger.info(tmpLine);
                // FIXME 发现bug: 在选择相同的一门课程(已经选过这个课程相同时间的其他班级)的时候, 会提示已经选了这门课程了
                // FIXME 怀疑教务处是按照courseId来判断的
                String redirect = tmpLine.replace("The URL has moved <a href=\"", "")
                        .replace("\">here</a>", "");
                GetMethod redirectPage = new GetMethod(redirect);
                redirectPage.setRequestHeader("cookie", userCookie);

                httpClient0.executeMethod(redirectPage);

                bis = new BufferedInputStream(redirectPage.getResponseBodyAsStream());
                scanner = new Scanner(bis, "GBK");

                keys = ("doWhat,studentId,courseId,courseNo,yearTerm,selectCourseStatus").split(",");
                cnt = 0;
                headers = new Header[keys.length];

                //获取Headers
                while (scanner.hasNextLine()) {
                    line = scanner.nextLine();

                    if (line.contains("这门课已经选了")) {
                        //你这门课已经选了,不允许再次选择了！
                        logger.info("已经选上了该课程.");
                        return true;
                    }

                    if (line.contains("<input")) {
                        for (String s : keys)
                            if (line.contains(s)) {
                                for (int i = line.indexOf(s) + s.length() + 9; line.charAt(i) != '\"' && i < line.length(); i++)
                                    value += line.charAt(i);
                                value = value.replaceAll("[\\s\r\n]*", "");
                                headers[cnt++] = new Header(s, value);
                                value = "";
                                break;
                            }

                        continue;
                    }
                }

                //确认提交选课请求
                confirmSelect = new PostMethod(prefixCourseController);
                //set bodies
                confirmSelect.setRequestBody(headers);
                //set Cookie
                confirmSelect.setRequestHeader("cookie", userCookie);
                //post!
                //超时4s
                httpClient0.getParams().setConnectionManagerTimeout(4000);
                httpClient0.executeMethod(confirmSelect);

                //如果满了, 返回的页面是一个值alert弹窗提示: 录取人数已满，请重选其它课程！
                scanner = new Scanner(new BufferedInputStream(confirmSelect.getResponseBodyAsStream()), "gbk");

                //如果原来就已经录取了或者这次选课成功了, 就是返回到教务处选课首页
                tmpLine = scanner.nextLine();
                if (tmpLine.contains("err.jsp?errMsg"))
                    throw new Exception("发生了严重错误! 错误信息: " + tmpLine);
                else if (tmpLine.contains("录取人数已满") || tmpLine.contains("Unavailable"))
                    return false;

                while (scanner.hasNextLine() && (tmpLine = scanner.nextLine()) != null) {
                    if (tmpLine.contains("对不起，操作出错"))
                        throw new Exception("发生了严重错误! 错误信息: " + tmpLine);
                    else if (tmpLine.contains("录取人数已满") || tmpLine.contains("Unavailable"))
                        return false;
                }

                // FIXME
                return true;
            } else {
                //如果原来就已经录取了或者这次选课成功了, 就是返回到教务处选课首页
                tmpLine = scanner.nextLine();
                if (tmpLine.contains("err.jsp?errMsg"))
                    throw new Exception("发生了严重错误! 错误信息: " + tmpLine);
                else if (tmpLine.contains("录取人数已满") || tmpLine.contains("Unavailable"))
                    return false;
                else if (tmpLine.contains("这门课已经选了"))
                    //你这门课已经选了,不允许再次选择了！
                    return true;

                while (scanner.hasNextLine() && (tmpLine = scanner.nextLine()) != null) {
                    if (tmpLine.contains("对不起，操作出错"))
                        throw new Exception("发生了严重错误! 错误信息: " + tmpLine);
                    else if (tmpLine.contains("录取人数已满") || tmpLine.contains("Unavailable"))
                        return false;
                    else if (tmpLine.contains("这门课已经选了"))
                        //你这门课已经选了,不允许再次选择了！
                        return true;
                }

                // FIXME
                return true;
            }

        } catch (IOException ioe) {
            if (ioe instanceof SocketTimeoutException)  {
                //网络超时, 可能是教务处卡爆了哦...
                logger.severe("网络超时, 可能是教务处卡爆了哦...");
                ioe.printStackTrace();
                //按照选课失败对待
                return false;
            } else {
                //getResponseBodyAsStream的时候出现的IO异常
                //throw new RuntimeException("获取返回的页面的时候发生IO异常", ioe);
                logger.severe("获取返回的页面的时候发生IO异常(可能是教务处网络卡爆了)");
                ioe.printStackTrace();
                return false;
            }

        } catch (Exception e) {
            // TODO 严重错误
            logger.severe(e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        }

    }

    /**
     * 删除已入取课程
     * @param course 要删除的课程
     * @return 只会在抛出异常或者转跳到错误界面才会返回false, 如果courseId或者classNo错误的话, 还是会返回成功....
     * @throws IllegalCourseException course中没有classNo或者courseId
     */
    public boolean deleteMatriculatedCourse(Course course) throws IllegalCourseException {
        if (course.getClassNo().equals("") || course.getCourseId().equals(""))
            throw new IllegalCourseException("course中没有classNo或者courseId!", course);

        PostMethod deleteCourse = new PostMethod(prefixCourseController);
        deleteCourse.setRequestHeader("cookie", userCookie);

        //course是已入取课程
        deleteCourse.setRequestBody(new Header[]{new Header("doWhat", "deletematriculatedcourse"),
                new Header("classNo", course.getClassNo()),
                new Header("courseId", course.getCourseId())});

        try {
            httpClient.executeMethod(deleteCourse);

            //debug
            String debug = deleteCourse.getResponseBodyAsString();
            //System.out.println(debug);

            return !debug.contains("errMsg");
        } catch (IOException ioe) {
            //连接的时候发生IO异常
            logger.severe("删除已录取课程的时候发生IO异常");
            return false;
        }
    }

    /**
     * P.S. 未测试过...
     * 删除以选择但是未录取的课程
     */
    public boolean deleteSelectedCourse(Course course) throws IllegalCourseException {
        if (course.getClassNo().equals(""))
            throw new IllegalCourseException("course中缺少classNo!", course);

        PostMethod deleteCourse = new PostMethod(prefixCourseController);
        deleteCourse.setRequestHeader("cookie", userCookie);

        //course是已入取课程
        deleteCourse.setRequestBody(new Header[]{new Header("doWhat", "deleteselectedcourse"),
                new Header("classNo", course.getClassNo())});

        try {
            httpClient.executeMethod(deleteCourse);

            //debug
            String debug = deleteCourse.getResponseBodyAsString();
            //System.out.println(debug);

            return !debug.contains("errMsg");
        } catch (IOException ioe) {
            //连接的时候发生IO异常
            logger.severe("删除以选择但未录取课程的时候发生IO异常");
            return false;
        }
    }

    /**
     * 获取当前用户的名字
     */
    public String getCurrentInfo() {
        GetMethod home = new GetMethod(selectCourse);
        home.setRequestHeader("cookie", getUserCookie());

        try {
            httpClient.executeMethod(home);

            Scanner scanner = new Scanner(new BufferedInputStream(home.getResponseBodyAsStream()),
                    "gbk");
            String line;
            while (scanner.hasNextLine())
                if ((line = scanner.nextLine()).contains("当前学生")) {
                    return line.replaceAll("<[^<>]+>", "");
                } else if (line.contains("选课没有开放")) {
                    // UPDATE: 处理教务处停止选课的情况
                    return "教务处已经停止选课!";
                }

        } catch (IOException ioe) {
            logger.severe("似乎网络有问题: " + ioe.getMessage());
        }

        return "";
    }

    /**
     * 获取Cookie对应的用户的名字
     */
    public static String getCurrentInfo(String cookie) {
        GetMethod home = new GetMethod(selectCourse);
        home.setRequestHeader("cookie", cookie);

        try {
            HttpClient httpClient = new HttpClient();
            // 设置 HttpClient 接收 Cookie,用与浏览器一样的策略
            httpClient.getParams().setCookiePolicy(
                    CookiePolicy.BROWSER_COMPATIBILITY);

            httpClient.executeMethod(home);

            Scanner scanner = new Scanner(new BufferedInputStream(home.getResponseBodyAsStream()),
                    "gbk");
            String line;
            while (scanner.hasNextLine())
                if ((line = scanner.nextLine()).contains("当前学生")) {
                    return line.replaceAll("<[^<>]+>", "");
                } else if (line.contains("选课没有开放")) {
                    // UPDATE: 处理教务处停止选课的情况
                    return "教务处已经停止选课!";
                }

        } catch (IOException ioe) {
            logger.severe("似乎网络有问题: " + ioe.getMessage());
        }

        return "";
    }

    /**
     * Test Client
     * @param args no need
     */
    public static void main(String[] args) {
        /* only for test */
        //test login account
        String loginUser = "";
        String loginPwd = "";

        try {
            DHUCurrentUser demo = new DHUCurrentUser(loginUser, loginPwd);

            String userCookie = demo.getUserCookie();
            GetMethod getMethod = new GetMethod(selectCourse);
            //访问教务处选课首页
            demo.httpClient.executeMethod(getMethod);
            // 每次访问需授权的网址时需带上前面的 cookie 作为通行证
            getMethod.setRequestHeader("cookie", userCookie);

            //debug
            logger.info(userCookie);
            // 打印出返回数据，检验一下是否成功
            //String text = getMethod.getResponseBodyAsString();
            //System.out.println(text);

            //选课
            //String courseNo = "214752";
            //String courseId = "090111";
            //System.out.println("选课结果: " + demo.selectCourse(courseNo, courseId));

            //删课
            /*
            System.out.println("删课");
            try {
                demo.deleteMatriculatedCourse(new Course(null, "130001", "", "1", null,
                        0.0, 0, 0, 0,
                        0, 0, null, null));
            } catch (Exception e) {
                //...
            }
            */


            //搜索课程
            String searchName = "数学分析";

            CourseType[] courses = CourseType.searchCourse(searchName, userCookie);

            System.out.println("课程名称           课程链接");
            if (courses == null || courses.length == 0)
                System.out.println("搜索结果为空!");
            else
                for (CourseType c : courses)
                    System.out.println(c.getCourse().getTextTitle() + "  " + c.getCourse().getLink());

            //删除课程
            //Course toDel = new Course(new HyperlinkURL("日本文化", null), "122021", "213223", "1", new Teacher("孙",
            //        new URL("http://jwdep.dhu.edu.cn/dhu/admin/selectcourse/teacherinfo.jsp?teacherId=10219022")),
            //         0.0d, 0, 0, 0,9, 16, null, null);

            //System.out.println(demo.deleteMatriculatedCourse(toDel));


            if (courses != null) {
                //通过CourseType对象(至少要courseId和courseName)来获得开班信息
                System.out.println(courses[0].getCourse().getTextTitle() + "在教务处选课页面查询到的的开班信息: ");

                Course[] classes = Course.getCourses(courses[0], userCookie);

                if (classes != null)
                    for (Course c : classes) {
                        System.out.println(c != null ? c.getCourse().getTextTitle() + " " + c.getCourseId()
                                + c.getCourseNo() : "");

                        if (c != null)
                            for (ClassTime ct : c.getClassTimes())
                                for (int i = 0; i < ClassTime.MAX_DAYS_OF_WEEK; i++) {
                                    if (ct.getClassTimeThisDay(i + 1) != null) {
                                        System.out.println("周" + (i + 1) + "上课节次: ");
                                        for (int classNum : ct.getClassTimeThisDay(i + 1)) {
                                            System.out.printf("%3d", classNum);
                                        }
                                        System.out.println();
                                    }
                                }
                    }
            }

            //查看所有已选课程
            System.out.println("所有已选课程: ");
            Course[] courses1 = Course.getMatriculated(userCookie);
            if (courses1 != null)
                for (Course c : courses1) {
                    System.out.println(c.getCourse().getTextTitle() + "(" + c.getCourse().getLink() + ")" +
                        "  Week: " + c.getStartWeek() + " ~ " + c.getEndWeek());
                }


            //抢课线程
            /*
            System.out.println("抢课开始: ");
            String courseId = "080171";
            String courseNo = "213642";

            try {
                SelectCourseThread thread = new SelectCourseThread(new Course(new HyperlinkURL("幸福心理学", null),
                        courseId, courseNo, "", null, 0.0d, 0, 0, 0,
                        0, 0, null, null), 2, demo.getUserCookie());

                SelectCourseThread thread2 = new SelectCourseThread(new Course(new HyperlinkURL("日本文化", null),
                        "122021", "213223", "", null, 0.0d, 0, 0,
                        0, 0, 0, null, null), 4, demo.getUserCookie());

                thread.addListener(new SelectCourseListener());
                thread2.addListener(new SelectCourseListener());

                thread.start();
                thread2.start();

                Thread.sleep(10 * 1000);

                thread.stopCurrentSelectCourse();
            } catch (IllegalCourseException ie) {
                //...
                System.err.println(ie);
            } catch (InterruptedException e) {
                //...
                System.err.println(e);
            }
            */


            //从教务处首页导入课程
            Pointer<String> message = new Pointer<>("");
            CourseType[] courseTypes = CourseType.intelliImportCourseType(demo.getUserCookie(), message);

            System.out.println("智能导入: " + "\nMessage: \n" + message.p + "\n列表: \n");

            if (courseTypes != null)
                for (CourseType c : courseTypes) {
                    System.out.println(c.getCourse().getTextTitle());
                }

            //查看文化素质类课程
            CommonCourseType[] commonCourseTypes = CommonCourseType.intelliImportCommonCourseType(demo.userCookie);

            if (commonCourseTypes != null)
                for (CommonCourseType c : commonCourseTypes)
                    if (c.getCourseNameAndLink().getTextTitle().equals("文化素质类"))
                        courseTypes = CourseType.getCourseTypes(c, demo.getUserCookie());

            System.out.println("\n所有未读过的文化素质课: ");
            if (courseTypes != null)
                for (CourseType c : courseTypes)
                    System.out.println(c.getCourse().getTextTitle());

            //在公共查询中查询courseType的开课信息
            if (courses != null)
                try {
                    System.out.println(courses[0].getCourse().getTextTitle() + "在公共查询中的开班信息: ");
                    Course[] result = Course.getCourseFromCommonSearch(courses[0], demo.getUserCookie());

                    if (result != null)
                        for (Course c : result) {
                            System.out.println(c != null ? c.getCourse().getTextTitle() + " " + c.getCourseId()
                                    + c.getCourseNo() : "");

                            if (c != null)
                                for (ClassTime ct : c.getClassTimes())
                                    for (int i = 0; i < ClassTime.MAX_DAYS_OF_WEEK; i++) {
                                        if (ct.getClassTimeThisDay(i + 1) != null) {
                                            System.out.println("周" + (i + 1) + "上课节次: ");
                                            for (int classNum : ct.getClassTimeThisDay(i + 1)) {
                                                System.out.printf("%3d", classNum);
                                            }
                                            System.out.println();
                                        }
                                    }
                        }
                } catch (IllegalCourseException ie) {
                    //CourseType至少要包含courseId!
                }



        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } catch (JwdepConnectionException je) {
            throw new RuntimeException("网络测试失败!", je);
        } catch (AccountLoginException ae) {
            throw new RuntimeException("登录出错", ae);
        }

    }
}///~
