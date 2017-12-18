package tk.dcmmcc;

import tk.dcmmcc.utils.DoubleLinkedList;

import java.io.Console;
import java.util.HashSet;
import java.util.Scanner;

/**
 * 东华大学命令行选课工具
 * Created by DCMMC on 2017/8/30.
 */
public class DHUCourseSelecterCLI {
    //选课队列, 每一个元素的格式为courseId,courseNo, 譬如"11120,45620"
    //因为是HashSet, 所以不会有重复元素
    private HashSet<String> selectCourseQueue = new HashSet<>();
    //任务队列中的所有的Course
    private DoubleLinkedList<Course> coursesQueue = new DoubleLinkedList<>();

    private void execute(int op) {
        switch (op) {
            case 1 : importCourses();break;
            case 2 : search();break;
            case 3 : viewQueue();break;
            case 4 : selectCourse();break;
            case 5 : viewSelected();break;
        }
    }

    private void importCourses() {
        title("智能导入课程");


    }

    private void search() {

    }

    private void viewQueue() {

    }

    private void selectCourse() {

    }

    private void viewSelected() {

    }

    /**
     * menu
     */
    private static int menu() {
        title("Menu");
        o("1. 智能导入课程.");
        o("2. 搜索并导入课程.");
        o("3. 查看选课队列.");
        o("4. 执行选课队列.");
        o("5. 查看已选课程.");
        o("6. 退出.");
        o("请输入要执行的操作的序号: ");
        Scanner scanner = new Scanner(System.in);
        if (scanner.hasNextLine())
            return Integer.valueOf(scanner.nextLine());
        else
            return 0;
    }

    /* tk.dcmmcc.utils */
    /**
     * 那个控制台输出的语句太长啦, 搞个方便一点的.
     * @param obj 要输出的String.
     */
    private static void o(Object obj) throws IllegalArgumentException {
        System.out.println(obj);
    }

    /**
     * 那个控制台输出的语句太长啦, 搞个方便一点的.
     * 重载的一个版本, 不接受任何参数, 就是为了输出一个回车.
     */
    private static void o() {
        System.out.println();
    }


    /**
     * 那个控制台输出的语句太长啦, 搞个方便一点的.
     * 格式化输出.
     * @param format
     *        a format string.
     * @param args
     *        由format中格式说明符指定的内容
     * @throws
     *        NullPointerException format不能为null
     */
    private static void of(String format, Object... args) {
        if (format == null)
            throw new NullPointerException("第一个参数不允许为空");

        System.out.printf(format, args);
    }

    /**
     * 为每道题的输出前面加一行Title, 这样看起来舒服一点
     * @param exName 题目名称
     */
    private static void title(String exName) {
        final int LEN = 120;
        String titleStr = "";

        int prefixLen = (LEN - exName.length()) / 2;
        int suffixLen =  LEN - prefixLen - exName.length();

        for (int i = 0; i < prefixLen; i++)
            titleStr += '#';
        titleStr += exName;
        for (int i = 0; i < suffixLen; i++)
            titleStr += '#';

        o("\n" + titleStr + "\n");
    }

    /**
     * 模拟清屏
     */
    private static void clear() {
        for (int i = 0; i < 15; i++)
            o();
    }

    /**
     * Test client
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        title("");
        title("DHU选课助手 version 0.1 alpha @author DCMMC");
        title("");

        Console console = System.console();

        Scanner scanner = new Scanner(System.in);

        String loginUser = "", loginPwd = "";
        o("请登录: ");

        //大部分的IDE都没有console
        if (console != null) {
            loginUser = console.readLine("学号: ");

            loginPwd = new String(System.console().readPassword("密码: "));
        } else {
            o("学号: ");
            if (scanner.hasNextLine())
                loginUser = scanner.nextLine();
            o("密码: ");
            if (scanner.hasNextLine())
                loginPwd = scanner.nextLine();
        }

        if (loginUser.equals("") || loginPwd.equals("")) {
            o("用户名或密码错误");
            return;
        }

        clear();

        try {
            DHUCurrentUser dhuCurrentUser = new DHUCurrentUser(loginUser, loginPwd);

            o("登录成功, 欢迎~");
            o(dhuCurrentUser.getCurrentInfo());
            //DHUCourseSelecterCLI client = new DHUCourseSelecterCLI();

            /*
            while (true) {
                int op = menu();
                if (op == 6)
                    break;

                client.execute(op);
            }
            */

            DoubleLinkedList<SelectCourseThread> selectCourseThreads = new DoubleLinkedList<>();

            while (true) {
                o("请输入课程编号:(输入q退出)");
                String courseId = scanner.nextLine();
                courseId = courseId.replaceAll("\\s*", "");
                if (courseId.equals("q")) {
                    clear();
                    o("退出...");
                    return;
                }

                CourseType[] courseTypes = CourseType.searchCourse(courseId, dhuCurrentUser.getUserCookie());
                if (courseTypes.length != 1) {
                    o("课程编号错误!");
                    continue;
                }

                Course[] courses = Course.getCourseFromCommonSearch(courseTypes[0], dhuCurrentUser.getUserCookie());
                o("请输入选课序号:(输入q退出)");
                String courseNo = scanner.nextLine();
                courseNo = courseNo.replaceAll("\\s*", "");
                if (courseNo.equals("q")) {
                    clear();
                    o("退出...");
                    continue;
                }

                Course targerCourse = null;

                for (Course course : courses)
                    if (course.getCourseNo().equals(courseNo))
                        targerCourse = course;

                if (targerCourse != null)
                    selectCourseThreads.addLast(new SelectCourseThread(targerCourse, 5,
                            dhuCurrentUser.getUserCookie()));
                else {
                    o("选课序号错误!");
                }

                o("当前任务队列中已有" + selectCourseThreads.getSize() + "个选课班级任务, 输入y开始抢课," +
                        " 输入q退出程序, 输入n继续添加课程");

                switch (scanner.nextLine()) {
                    case "y" : for (SelectCourseThread selectCourseThread : selectCourseThreads)
                                    selectCourseThread.start();
                                break;
                    case "q" : clear(); o("退出...");return;
                    case "n" : break;
                    default: o("输入错误, 按照输入n来处理");break;
                }
            }

            //clear();
            //o("退出...");

        } catch (AccountLoginException ae) {
            o("帐号或密码错误! 请核对: userName: " + loginUser + ", userPassword: " + loginPwd +
                " " + ae.getMessage());
        } catch (JwdepConnectionException je) {
            o("网络错误! 请检查网络. " + je.getMessage());
        } catch (IllegalCourseException ie) {
            o("courseType必须最少提供提供courseId!");
        }

    }
}///~
