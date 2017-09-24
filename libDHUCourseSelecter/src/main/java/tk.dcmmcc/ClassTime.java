package tk.dcmmcc;

/**
 * 封装上课时间的信息
 * Created by DCMMC on 2017/8/28.
 */
public class ClassTime {
    //空时间安排的标志
    private boolean EMPTY_CLASS_TIME = false;

    //一周上课的天数(从周一开始)
    final static int MAX_DAYS_OF_WEEK = 5;
    //一天的课程的节数
    private final static int MAX_CLASSES_A_DAY = 13;

    //记录每天该课程的上课节次安排
    //index为i记录这星期i+1的上课节次
    private String[] classesEachDay = new String[MAX_DAYS_OF_WEEK];

    private int weekStart = 0;
    private int weekEnd = 0;


    /**
     * 空课程安排
     */
    public ClassTime() {

    }

    /**
     * 按照参数来记录每一天的上课节次
     * @param weekStart 开始周次
     * @param weekEnd 结束周次
     * @param dayAndClass 双数个, 每一双的前面那一个String为星期几(就是一个数字), 后面那一个String记录这上课节次(例如"1,2"
     *                    用逗号分隔)
     * @throws IllegalArgumentException 参数必须符合要求
     */
    public ClassTime(int weekStart, int weekEnd, String ... dayAndClass) throws IllegalArgumentException {
        if (weekEnd == -1 && weekStart == -1) {
            //特殊情况, 也就是没有时间安排, 譬如实习课
            EMPTY_CLASS_TIME = true;
            return;
        } else if (dayAndClass.length - 2 > MAX_DAYS_OF_WEEK * 2 || dayAndClass.length % 2 != 0)
            throw new IllegalArgumentException("参数个数不符合要求!");

        this.weekStart = weekStart;
        this.weekEnd = weekEnd;
        for (int i = 0; i < dayAndClass.length; i += 2) {
            classesEachDay[Integer.valueOf(dayAndClass[i]) - 1] = dayAndClass[i + 1];
        }

    }

    /**
     * 获取星期第day天的上课节次
     * @return 上课节次
     * @throws IllegalArgumentException 参数必须符合要求
     */
    public int[] getClassTimeThisDay(int day) throws IllegalArgumentException {
        if (day < 1 || day > MAX_DAYS_OF_WEEK)
            throw new IllegalArgumentException("参数day不在有效范围内!");

        //如果这一天没有存储信息或者这是一个空时间安排的ClassTime
        if (EMPTY_CLASS_TIME || classesEachDay[day - 1] == null)
            return null;

        String[] classes = classesEachDay[day - 1].split(",");
        int[] result = new int[classes.length];

        for (int i = 0; i < classes.length; i++) {
            result[i] = Integer.valueOf(classes[i]);
            if (result[i] < 1 || result[i] > MAX_CLASSES_A_DAY)
                throw new IllegalArgumentException("节次信息不在有效范围内!");
        }


        return result;
    }

    /**
     * 获取周次范围
     * 如果是空时间安排, 就返回{-1, -1}
     * @return 周次范围, 第一个元素是起始的周次, 第二个元素是结束的周次
     */
    public int[] getWeekRange() {
        return EMPTY_CLASS_TIME ? new int[]{-1, -1} : new int[]{weekStart, weekEnd};
    }

    public static int getMaxDaysOfWeek() {
        return MAX_DAYS_OF_WEEK;
    }

    public static int getMaxClassesADay() {
        return MAX_CLASSES_A_DAY;
    }
}///~
