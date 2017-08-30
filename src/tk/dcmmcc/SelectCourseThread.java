package tk.dcmmcc;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;

/**
 * 选(抢)课线程
 * Created by DCMMC on 2017/8/29.
 */
public class SelectCourseThread extends Thread {
    /* Fields */
    private Course course;
    private int periodSeconds;
    private String userCookie;
    //事件监听器
    HashSet<SelectCourseListener> listeners;

    //不允许用默认构造器来实例化
    private SelectCourseThread() {

    }

    /**
     * 构造器, 构造选课线程
     * @param course 要选课的课程(必须有courseId和courseNo)
     * @param periodSeconds 每次提交选课请求的时间间隔(单位s)
     * @throws IllegalCourseException 如果Course连courseId和courseNo都没有
     */
    public SelectCourseThread(Course course, int periodSeconds, String userCookie) throws IllegalCourseException {
        if (course.getCourseId().equals("") || course.getCourseNo().equals("") || course.getCourse() == null)
            throw new IllegalCourseException("Course对象缺少必要信息! 至少需要courseId和courseNoh和courseName!", course);

        this.course = course;
        this.periodSeconds = periodSeconds;
        this.userCookie = userCookie;
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

        while (!Thread.currentThread().isInterrupted() && !selectCourseSuccess) {
            try {
                System.out.println("正在进行对课程" + course.getCourse().getTextTitle() + "进行第"
                        + (++cnt) + "次抢课" + " @" + new SimpleDateFormat("YYYY年MM月dd日 HH:mm:ss")
                        .format(new Date()));
                selectCourseSuccess = DHUCurrentUser.selectCourse(course.getCourseNo(), course.getCourseId(), userCookie);

                //间隔由构造器传入参数设定
                Thread.sleep(periodSeconds * 1000);

                //给线程调度器可以切换到其它进程的信号
                //Thread.yield();
            } catch (InterruptedException ie) {
                System.err.println("Thread.Sleep的时候收到了中断请求导致InterruptedException");
                //抛出InterruptedException后中断标志被清除，标准做法是再次调用interrupt恢复中断
                Thread.currentThread().interrupt();
            } catch (IllegalCourseException courseE) {
                System.err.println(courseE.getMessage() + "\nCourse中的信息错误, 或者页面错误, 请核对courseId(" + courseE.getCauseCourse().getCourseId()
                + ")和courseNo(" + courseE.getCauseCourse().getCourseNo() + ")");
            }
        }

        if (selectCourseSuccess) {
            System.out.print("课程" + course.getCourse().getTextTitle() + "抢课成功, ");
            //通知所有监视器
            notifyListeners(new SelectCourseEvent(this, course,true));
        } else {
            System.out.print("课程" + course.getCourse().getTextTitle() + "抢课未成功但是被用户中止, ");
            //通知所有监视器
            notifyListeners(new SelectCourseEvent(this, course,false));
        }
        System.out.println("结束当前线程的选课.");
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
}///~
