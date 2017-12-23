package tk.dcmmcc.datafx;

import javafx.beans.property.*;
import tk.dcmmcc.DHUCurrentUser;

/**
 * 封装了设置的信息
 * Created by DCMMC on 2017/9/8.
 */
public class SettingData {
    /*
    * FIXME 这里有一个问题, 就是设置更新之后原来已经由设置中的内容产生的那些CoursesData之类的东西中的设置并不会改变
    */
    //每个选课班级线程的选课请求的间隔时间, 默认10s
    // 推荐不低于5s
    private static int requestInterval = 10;
    private final static int MinInterval = 5;
    //默认某个课程中的任意一个班级选课线程选课成功
    //就会把这个课程中的其他班级选课线程给关闭(这个还是在发布的时候不要搞出来。。)
    private static boolean multiClassesInSameCourse = false;
    //默认每个选课班级线程最多选课次数为无限次(值为-1)
    private static long maxTryCount = -1;
    //保存了用户信息的DHUCourseSelecter
    private static DHUCurrentUser dhuCurrentUser = null;
    //CurrentInfo Property
    private static Property<String> currentInfoProperty = new SimpleObjectProperty<>(
            "你当前还没有登录, 请点击右上角的按钮选择登录");
    //记录登录状态
    private static BooleanProperty loginFlagProperty = new SimpleBooleanProperty(false);
	//记录教务处是否停止选课
	private static BooleanProperty jwStopSelectCourses = new SimpleBooleanProperty(false);
    //任务队列
    private static CourseClassRequestQueue requestQueue = null;


    public static DHUCurrentUser login(DHUCurrentUser currentUser) {
        dhuCurrentUser = currentUser;
        currentInfoProperty.setValue(currentUser.getCurrentInfo());
        // UPDATE: 如果教务处选课系统关闭了, 还是设置为未登录状态
        if (currentInfoProperty.getValue().equals("教务处已经停止选课!")) {
            loginFlagProperty.setValue(false);
			jwStopSelectCourses.setValue(true);
		} else {
            loginFlagProperty.setValue(true);
			jwStopSelectCourses.setValue(false);
		}

        // debug
        //loginFlagProperty.setValue(true);

        return currentUser;
    }

    public static DHUCurrentUser signOut() {
        DHUCurrentUser tmp = dhuCurrentUser;
        dhuCurrentUser = null;
        currentInfoProperty.setValue("你当前还没有登录, 请点击右上角的按钮选择登录");
        loginFlagProperty.setValue(false);
        return tmp;
    }

    /* Getters and Setters */

    public static DHUCurrentUser getDhuCurrentUser() {
        return dhuCurrentUser;
    }

    public static Property<String> getCurrentInfoProperty() {
        return currentInfoProperty;
    }

    public static int getRequestInterval() {
        return requestInterval;
    }

    public static long getMaxTryCount() {
        return maxTryCount;
    }

    public static boolean getMultiClassesInSameCourse() {
        return multiClassesInSameCourse;
    }

    /**
     *
     * @param requestInterval 每个线程中的选课请求的时间间隔, >= 1
     */
    public static void setRequestInterval(int requestInterval) {
        if (requestInterval < MinInterval)
            requestInterval = MinInterval;

        SettingData.requestInterval = requestInterval;
    }

    public static void setMaxTryCount(long maxTryCount) {
        SettingData.maxTryCount = maxTryCount;
    }

    /*
    public static void setMultiClassesInSameCourse(boolean multiClassesInSameCourse) {
        SettingData.multiClassesInSameCourse = multiClassesInSameCourse;
    }
    */

    public static BooleanProperty getLoginFlagProperty() {
        return loginFlagProperty;
    }

    public static CourseClassRequestQueue getRequestQueue() {
        return requestQueue;
    }

	public static BooleanProperty getJwStopSelectCourses() {
		return jwStopSelectCourses;
	}

    public static void setRequestQueue(CourseClassRequestQueue requestQueue) {
        SettingData.requestQueue = requestQueue;
    }

    /**
     * 重置所有设置
     */
    public static void reset() {
       requestInterval = 10;
       multiClassesInSameCourse = false;
       maxTryCount = -1;
       requestQueue = null;
    }
}///~
