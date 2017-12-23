package tk.dcmmcc.datafx;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.*;
import javafx.scene.Node;
import tk.dcmmcc.CourseType;
import tk.dcmmcc.utils.DoubleLinkedList;
import tk.dcmmcc.utils.LoggerUtil;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

/**
 * 里面封装这个(单个)课程的所有班级任务，里面的start方法开始所有班级的选课线程，
 * cancel方法停止所有的这个课程的班级的选课进程，内部类listener用于监听所有这个课程的班级的选课进程，
 * 如果设置类中设置了一个课程只允许成功选择一个班级(默认状态)，就把这个课程中的其他班级选课线程给结束
 *
 * Created by DCMMC on 2017/9/9.
 */
public class CourseData {
    //保证没有重复的班级任务, 存储着CourseNo(保持元素的顺序)和对应的ClassesData
    private LinkedHashMap<String, ClassesData> classesDataMap = new LinkedHashMap<>();
    //课程的courseId
    private String courseId = "";
    //Logger
    private static Logger logger = Logger.getLogger("DHUCourseSelecter");
    //start flag
    private boolean started = false;
    //设置当前课程是否选课成功的
    private BooleanProperty selectSuccess = new SimpleBooleanProperty(false);
    //该Course的所有Classes线程的请求次数总和
    private IntegerProperty courseRequestCountProperty = new SimpleIntegerProperty(0);
    //CourseType
    private CourseType courseType = null;
    //Status Property
    //private Property<Node> statusProperty = new SimpleObjectProperty<>();
    /**
     * Status Property
     * 总有4个数值:
     * 1: 还没开始
     * 2: 选课中
     * 3: 被中止
     * 4: 选课完成
     */
    private IntegerProperty slectStatus = new SimpleIntegerProperty(1);

    static {
        LoggerUtil.initLogger(logger);
    }

    /*
    {
        SVGGlyph icon = new SVGGlyph(0, "wait", "M512 1024a512 " +
                "512 0 1 1 512-512 512 512 0 0 1-512 512z m189.6704-362.7264L486.4 556.928V320a38.4 38.4" +
                " 0 0 0-76.8 0v256a37.9136 37.9136 0 0 0 13.1328 28.416 37.1456 37.1456 0 0 0 15.0784 " +
                "14.3104l230.4 111.6416a38.4 38.4 0 1 0 33.4592-69.0944z",Color.web("#d4237a"));
        icon.setPrefSize(32, 32);
        statusProperty.setValue(icon);
        selectSuccess.addListener(((observable, oldValue, newValue) -> {
            //如果成功了, 就完毕所有线程
            if (newValue)
                cancelAll();
        }));
    }
    */

    /**
     * 默认构造器
     */
    public CourseData() {

    }

    /**
     * 构造器
     * @param classesData 要添加的ClassData
     */
    public CourseData(CourseType courseType, ClassesData classesData) {
        this.courseType = courseType;
        addClassData(classesData);
    }

    /**
     * 从数组中创建对象
     */
    public CourseData(CourseType courseTypes, ClassesData[] classesDatas) {
        if (started) {
            logger.warning("任务已经开始了, 请不要再添加了!");
            return;
        }

        if (classesDatas.length > 0) {
            this.courseType = courseTypes;
            this.courseId = classesDatas[0].getCourse().getCourseId();
            classesDataMap.put(classesDatas[0].getCourse().getCourseNo(),
                    classesDatas[0]);
            //courseNoSet.add(classesDatas[0].getCourse().getClassNo());
            //classesDataList.addLast(classesDatas[0]);

            for (int i = 1; i < classesDatas.length; i++) {
                addClassData(classesDatas[i]);
            }
        }
    }

    /**
     * 判断Course中是否已经有了这个courseNo对应的classesData
     * @param courseNo 要判断的课程的courseNo
     */
    public boolean existClass(String courseNo) {
        return classesDataMap.containsKey(courseNo);
    }

    /**
     * 添加classData到List去
     * @param classesData 要添加的ClassData
     * @return 如果成功就返回添加的这个ClassData, 如果该班级不属于该课程或者该课程班级已经在队列中就返回null
     */
    public ClassesData addClassData(ClassesData classesData) {
        if (started) {
            logger.warning("任务已经开始了, 请不要再添加了!");
            return null;
        }

        if (this.courseId.equals("")) {
            this.courseType = CourseType.searchCourse(classesData.getCourse().getCourseId(),
                    SettingData.getDhuCurrentUser().getUserCookie())[0];
            this.courseId = classesData.getCourse().getCourseId();
        } else if (!classesData.getCourse().getCourseId().equals(this.courseId)) {
            logger.warning("该班级不属于该课程!");
            return null;
        }

        if (classesDataMap.containsKey(classesData.getCourse().getCourseNo())) {
            logger.warning("该课程班级已经在队列中了!");
            return null;
        } else {
            classesDataMap.put(classesData.getCourse().getCourseNo(), classesData);
            return classesData;
        }
    }

    /**
     * 按照courseNo来移除ClassData(包括终结ClassData中的线程)
     * @param courseNo 要移除的ClassData的courseNo
     * @return 成功就返回ClassData, 失败就返回null
     */
    public ClassesData removeClassData(String courseNo) {
        if (started) {
            logger.warning("任务已经开始了, 请不要再添加了!");
            return null;
        }

        if (!classesDataMap.containsKey(courseNo)) {
            logger.warning("要移除的ClassData不存在!");
            return null;
        }

        ClassesData tmp = classesDataMap.get(courseNo);

        classesDataMap.remove(courseNo);

        return tmp;
    }

    /**
     * 开始所有班级的选课线程
     * 注意: start之后一定不能再添加或者删除课程班级了
     */
    public void startAll() {
        //Node loading = new JFXSpinner();
        setStatus(2);

        DoubleLinkedList<ClassesData> classesDataList  =
                new DoubleLinkedList<ClassesData>(classesDataMap.values().toArray());

        if (classesDataList.getSize() == 1) {
            selectSuccess.bind(classesDataList.get(0).getSelectCourseThread().getSuccessProperty());
            classesDataList.get(0).getSelectCourseThread().start();
            //classesDataList.get(0).setStatus(loading);
            classesDataList.get(0).setStatus(2);
            courseRequestCountProperty.bind(classesDataList.get(0)
                    .getSelectCourseThread()
                    .getRequestCountProperty());
            return;
        } else if (classesDataList.getSize() == 0) {
            logger.warning("当前课程没有任何选课班级!");
            return;
        }

        BooleanBinding booleanBinding;
        IntegerBinding integerBinding;
        if (!SettingData.getMultiClassesInSameCourse()) {
            booleanBinding = classesDataList.get(0).getSelectCourseThread().getSuccessProperty()
                    .or(classesDataList.get(1).getSelectCourseThread().getSuccessProperty());
            integerBinding = (IntegerBinding) classesDataList.get(0).getSelectCourseThread().getRequestCountProperty()
                    .add(classesDataList.get(1).getSelectCourseThread().getRequestCountProperty());
            int cnt = 0;
            for (ClassesData classesData : classesDataList) {
                if (++cnt > 2) {
                    booleanBinding = booleanBinding.or(classesData.getSelectCourseThread().getSuccessProperty());
                    integerBinding = (IntegerBinding) integerBinding.add(classesData.getSelectCourseThread().getRequestCountProperty());
                }
                //classesData.setStatus(loading);
                classesData.setStatus(2);
                classesData.getSelectCourseThread().start();
            }
        } else {
            booleanBinding = classesDataList.get(0).getSelectCourseThread().getSuccessProperty()
                    .and(classesDataList.get(1).getSelectCourseThread().getSuccessProperty());
            integerBinding = (IntegerBinding) classesDataList.get(0).getSelectCourseThread().getRequestCountProperty()
                    .add(classesDataList.get(1).getSelectCourseThread().getRequestCountProperty());
            int cnt = 0;
            for (ClassesData classesData : classesDataList) {
                if (++cnt > 2) {
                    booleanBinding = booleanBinding.and(classesData.getSelectCourseThread().getSuccessProperty());
                    integerBinding = (IntegerBinding) integerBinding.add(classesData.getSelectCourseThread().getRequestCountProperty());
                }
                //classesData.setStatus(loading);
                classesData.setStatus(2);
                classesData.getSelectCourseThread().start();
            }
        }

        selectSuccess.bind(booleanBinding);
        courseRequestCountProperty.bind(integerBinding);
    }

    /**
     * 停止队列中的所有剩余课程班级选课线程
     */
    public void cancelAll() {
        Node status;
        if (selectSuccess.get()) {
            slectStatus.set(4);
            /*
            status = new SVGGlyph(0, "success", "M512 1024C229.451852" +
                    " 1024 0 794.548148 0 512S229.451852 0 512 0s512 229.451852 512 512-229.451852 " +
                    "512-512 512z m-66.37037-310.992593c17.066667 0 34.133333-5.688889 47.407407-18.9" +
                    "62963l254.103704-254.103703c26.548148-26.548148 26.548148-68.266667 0-94.814815s-" +
                    "68.266667-26.548148-94.814815 0l-208.592593 208.592593-73.955555-73.955556c-26.54" +
                    "8148-26.548148-68.266667-26.548148-92.918519 0-26.548148 26.548148-26.548148 68." +
                    "266667 0 92.918518l121.362963 121.362963c13.274074 13.274074 30.340741 18.962963 " +
                    "47.407408 18.962963z", Color.web("#0AC380"));
            ((SVGGlyph) status).setPrefSize(32, 32);
            */
        } else {
            slectStatus.set(3);
            /*
            status = new SVGGlyph(0, "error", "M512 0a512 512 0 1 0 0 " +
                    "1024A512 512 0 0 0 512 0z m269.397333 781.482667a41.813333 41.813333 0 0 1-58.88-0.0853" +
                    "34L512 570.624 301.397333 781.482667a41.642667 41.642667 0 0 1-58.794666-58.88L452.864" +
                    " 512 242.517333 301.397333a41.642667 41.642667 0 0 1 58.88-58.794666L512 453.461333l210" +
                    ".517333-210.773333a41.642667 41.642667 0 0 1 58.965334 58.794667L571.050667 512l210.3" +
                    "46666 210.602667a41.557333 41.557333 0 0 1 0 58.88z", Color.web("#d81e06"));
            ((SVGGlyph) status).setPrefSize(32, 32);
            */
        }

        for (ClassesData classesData : classesDataMap.values()) {
            classesData.getSelectCourseThread().stopCurrentSelectCourse();
            //classesData.setStatus(status);
            classesData.setStatus(slectStatus.get());
        }
    }

    /**
     * 当前课程班级队列是否已经完成
     */
    public BooleanProperty getSelectSuccess() {
        return selectSuccess;
    }

    public String getCourseId() {
        return courseId;
    }

    public IntegerProperty getCourseRequestCountProperty() {
        return courseRequestCountProperty;
    }

    public LinkedHashMap<String, ClassesData> getClassesDataMap() {
        return classesDataMap;
    }

    public CourseType getCourseType() {
        return courseType;
    }

    public IntegerProperty getStatus() {
        return slectStatus;
    }

    public void setStatus(int slectStatus) {
        this.slectStatus.set(slectStatus);
    }

    /*
    public Property<Node> getStatusProperty() {
        return statusProperty;
    }

    public void setStatus(Node status) {
        this.statusProperty.setValue(status);
    }
    */
}///~
