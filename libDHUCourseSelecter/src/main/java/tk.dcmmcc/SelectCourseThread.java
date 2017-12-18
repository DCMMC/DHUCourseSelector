package tk.dcmmcc;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.logging.*;

/**
 * 选(抢)课线程
 * Created by DCMMC on 2017/8/29.
 */
public class SelectCourseThread extends Thread {
    /* Fields */
    private Course course;
    private int periodSeconds;
    private String userCookie;
    //request count property
    //已经尝试的请求次数
    private SimpleIntegerProperty requestCountProperty = new SimpleIntegerProperty(0);
    //request max count
    //默认-1表示无限次
    private long maxRequestCount = -1;
    //事件监听器
    private HashSet<SelectCourseListener> listeners;
    //该选课线程是否选课成功
    private SimpleBooleanProperty successProperty = new SimpleBooleanProperty(false);

    private static Logger logger = Logger.getLogger("DHUCourseSelecter");

    static {
        initLogger(logger);
    }

    //不允许用默认构造器来实例化
    private SelectCourseThread() {

    }

    /**
     * 构造器, 构造选课线程
     * @param course 要选课的课程(必须有courseId和courseNo)
     * @param periodSeconds 每次提交选课请求的时间间隔(单位s)
     * @param userCookie 用户Cookie
     * @throws IllegalCourseException 如果Course连courseId和courseNo都没有
     */
    public SelectCourseThread(Course course, int periodSeconds, String userCookie) throws IllegalCourseException {
        this(course, periodSeconds, userCookie, -1);
    }

    /**
     * 构造器, 构造选课线程
     * @param course 要选课的课程(必须有courseId和courseNo)
     * @param periodSeconds 每次提交选课请求的时间间隔(单位s)
     * @param userCookie 用户Cookie
     * @param maxRequestCount 最大尝试次数
     * @throws IllegalCourseException 如果Course连courseId和courseNo都没有
     */
    public SelectCourseThread(Course course, int periodSeconds, String userCookie, long maxRequestCount) throws IllegalCourseException {
        if (course.getCourseId().equals("") || course.getCourseNo().equals("") || course.getCourse() == null)
            throw new IllegalCourseException("Course对象缺少必要信息! 至少需要courseId和courseNoh和courseName!", course);

        this.course = course;
        this.periodSeconds = periodSeconds;
        this.userCookie = userCookie;
        this.maxRequestCount = maxRequestCount;
    }

    /**
     * 添加时间监听器
     */
    public void addListener(SelectCourseListener listener) {
        if (listeners == null)
            listeners = new HashSet<>();
        listeners.add(listener);
    }

    /**
     * 删除事件监听器
     */
    public void removeListener(SelectCourseListener listener) {
        if (listeners != null && listeners.size()> 0)
            listeners.remove(listener);
    }

    /**
     * 初始化logger
     */
    public static Logger initLogger(Logger logger) {
        try {
            //LogManager.getLogManager().readConfiguration(new ByteArrayInputStream((".level=INFO\njava.util.logging." +
            //        "FileHandler.pattern=%h/DHUCourseSelecter.log").getBytes()));

            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.ALL);
            consoleHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(consoleHandler);
            int limit = 1000000; // 1 Mb
            //默认放在user.home文件夹下面
            FileHandler fileHandler = new FileHandler("%h/DHUCourseSelecter.log",  limit, 1,
                    false);
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.ALL);
            logger.addHandler(fileHandler);
        } catch (IOException | NullPointerException ne) {
            //严重错误
            logger.severe("为logger添加FileHandler的时候发生严重错误!");
            ne.printStackTrace();
        }

        return logger;
    }

    /**
     * 通知所有的监听器
     */
    private void notifyListeners(SelectCourseEvent event) {
        if (listeners != null && listeners.size() > 0)
            for (SelectCourseListener listener : listeners)
                listener.selectSuccessEvent(event);
    }


    /**
     * 工作核心方法
     */
    @Override
    public void run() {
        //尝试次数
        int cnt = 0;
        //选课成功的标志
        boolean selectCourseSuccess = false;

        while (!Thread.currentThread().isInterrupted() && !selectCourseSuccess && (maxRequestCount == -1
            || cnt <= maxRequestCount)) {
            try {
                logger.info("正在进行对课程" + course.getCourse().getTextTitle() + " - 班级" +
                        course.getCourseNo() + "进行第" + (++cnt) + "次抢课" + " @" +
                        new SimpleDateFormat("YYYY年MM月dd日 HH:mm:ss").format(new Date()));
                selectCourseSuccess = DHUCurrentUser.selectCourse(course.getCourseNo(), course.getCourseId(), userCookie);

                if (selectCourseSuccess)
                    successProperty.setValue(selectCourseSuccess);
                requestCountProperty.setValue(cnt);

                //间隔由构造器传入参数设定
                Thread.sleep(periodSeconds * 1000);

                //给线程调度器可以切换到其它进程的信号
                //Thread.yield();
            } catch (InterruptedException ie) {
                logger.severe("Thread.Sleep的时候收到了中断请求导致InterruptedException");
                //抛出InterruptedException后中断标志被清除，标准做法是再次调用interrupt恢复中断
                Thread.currentThread().interrupt();
            } catch (IllegalCourseException courseE) {
               logger.severe(courseE.getMessage() + "\nCourse中的信息错误, 或者页面错误, 请核对courseId(" + courseE.getCauseCourse().getCourseId()
                + ")和courseNo(" + courseE.getCauseCourse().getCourseNo() + ")");
            }
        }

        if (selectCourseSuccess) {
            logger.info("课程" + course.getCourse().getTextTitle() + "抢课成功, ");
            //通知所有监视器
            notifyListeners(new SelectCourseEvent(this, course,true));
        } else {
            logger.info("课程" + course.getCourse().getTextTitle() + "抢课未成功但是被用户中止, ");
            //通知所有监视器
            notifyListeners(new SelectCourseEvent(this, course,false));
        }
        logger.info("结束当前线程的选课.");
    }

    /**
     * 停止当前线程的选课
     */
    public void stopCurrentSelectCourse() {
        interrupt();
    }


    /**
     * test client
     */
    public static void main(String[] args) {

    }

    /**
     * set Interval
     */
    public void setPeriodSeconds(int periodSeconds) {
        this.periodSeconds = periodSeconds;
    }

    /**
     * get Interval
     */
    public int getPeriodSeconds() {
        return periodSeconds;
    }

    /**
     * get request count
     */
    public SimpleIntegerProperty getRequestCountProperty() {
        return requestCountProperty;
    }

    public SimpleBooleanProperty getSuccessProperty() {
        return successProperty;
    }
}///~
