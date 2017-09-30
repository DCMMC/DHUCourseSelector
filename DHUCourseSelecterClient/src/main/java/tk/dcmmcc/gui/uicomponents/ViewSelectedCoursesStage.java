package tk.dcmmcc.gui.uicomponents;

import com.jfoenix.controls.*;
import com.jfoenix.effects.JFXDepthManager;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.transform.Scale;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.controlsfx.control.PopOver;
import tk.dcmmcc.*;
import tk.dcmmcc.utils.DoubleLinkedList;
import tk.dcmmcc.utils.ExceptionDialog;
import tk.dcmmcc.utils.LoggerUtil;
import tk.dcmmcc.utils.Pointer;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Logger;

/**
 * TODO 冲突课程点击要更改那些功能按钮的
 * TODO 冲突的课程的ListView里面的信息加入课程的在这一天的节次信息
 * TODO 删课之后刷新页面
 * FIXME 这里假设为1 ~ 18周
 * FIXME 这些JFXSnackBar是透明的, 有问题, 暂时改成了不自动消失
 * 手撸课程表完成
 * 查看已选课程
 * Created by DCMMC on 2017/9/13.
 */
public class ViewSelectedCoursesStage {
    // TODO 手动在设置里面设置或者从服务器更新
    //开学第一周的第一天的日期(星期一)
    private static final MonthDay FirstDayWeek = MonthDay.of(9, 11);
    //存储"week-section"周几加节次和对应的课程表
    private static LinkedHashMap<String, CoursePaneNode> coursesMap = new LinkedHashMap<>();
    //存储"courseId-classNo"对应的CoursePaneNodes
    //P.S. getAllSelectedCourses得到的courses中的courseNo为""(空)!!!
    private static LinkedHashMap<String, DoubleLinkedList<CoursePaneNode>> courseIdClassNoAndCoursePanes = new LinkedHashMap<>();
    //在已录取的课程的ListView中的元素中"courseId-classNo"的course的与对应的在ListView中的序号
    private static LinkedHashMap<String, Integer> matriculatedListViewMap = new LinkedHashMap<>();
    //在已选择但未录取的课程的ListView中的元素中"courseId-classNo"的course的与对应的在ListView中的序号
    private static LinkedHashMap<String, Integer> selectedListViewMap = new LinkedHashMap<>();
    private static HBox operates;
    //记录上一次被选中的coursePane
    private static DoubleLinkedList<CoursePaneNode> lastClickedCoursePane = new DoubleLinkedList<>();
    //上一次被选中的coursePane是已入取的还是未录取的
    //默认是已录取的
    private static boolean lastClickedIsMatriculated = true;
    //已录取课程
    private static JFXListView<CourseLabel> matriculatedCourses;
    //已选择但未录取课程
    private static JFXListView<CourseLabel> selectedCourses;
    //冲突课程 String为"courseId-classNo"
    private static LinkedHashMap<String, DoubleLinkedList<Label>> conflictCoursesMap = new LinkedHashMap<>();
    private static Scene scene;
    private static PopOver popOverInfo = new PopOver();
    //冲突课程的"courseId-classNo"
    private static LinkedHashSet<String> conflictCourseId = new LinkedHashSet<>();
    private static StackPane root;
    private static Logger logger = Logger.getLogger(ViewSelectedCoursesStage.class.getName());

    static {
        LoggerUtil.initLogger(logger);
    }

    /**
     * 主程序
     * @param stage main Stage
     * @param currentUser currentUser
     */
    @SuppressWarnings("unchecked")
    static void launch(Stage stage, DHUCurrentUser currentUser) {
        //把lastClickedCoursePane重置
        // TODO 看看还有没有别的需要重置的, 因为这static的特性
        lastClickedCoursePane = new DoubleLinkedList<>();
        lastClickedIsMatriculated = true;

        JFXButton confirmBtn;
        //指定要查看的周次
        JFXComboBox<Label> viewWeek;
        VBox mainVBox;

        try {
            FXMLLoader loader = new FXMLLoader(ViewSelectedCoursesStage.class
                    .getResource("/fxml/ui/ViewSelected.fxml"));
            root = loader.load();
            confirmBtn = (JFXButton) loader.getNamespace().get("confirmBtn");
            operates = (HBox) loader.getNamespace().get("operates");
            matriculatedCourses = (JFXListView<CourseLabel>) loader.getNamespace().get("matriculatedCourses");
            selectedCourses = (JFXListView<CourseLabel>) loader.getNamespace().get("selectedCourses");
            viewWeek = (JFXComboBox<Label>) loader.getNamespace().get("viewWeek");
            mainVBox = (VBox) loader.getNamespace().get("mainVBox");

            confirmBtn.setOnMouseClicked(event -> {
                stage.close();
            });

            scene = new Scene(root, 910, 800);

            //最小窗口大小
            stage.setMinWidth(910);
            stage.setMinHeight(800);
            stage.setScene(scene);

            mainVBox.prefHeightProperty().bind(scene.heightProperty().subtract(
                    new SimpleDoubleProperty(40)));
            mainVBox.maxHeightProperty().bind(scene.heightProperty().subtract(
                    new SimpleDoubleProperty(40)));

            //课程表
            ScrollPane courseCalendar = (ScrollPane) loader.getNamespace().get("courseCalendar");
            courseCalendar.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            courseCalendar.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
            DoubleBinding prefWidth = scene.widthProperty().subtract(
                    new SimpleDoubleProperty(190.0d));
            courseCalendar.prefWidthProperty().bind(prefWidth);
            //Make the binding valid...
            //prefWidth.getValue();
            DoubleBinding prefHeight = scene.heightProperty().subtract(
                    new SimpleDoubleProperty(100.0d));
            courseCalendar.prefHeightProperty().bind(prefHeight);
            //Make the binding valid...
            //prefHeight.getValue();

            //设置查看不同周次的事件处理
            viewWeek.getSelectionModel().selectedIndexProperty().addListener(((observable, oldValue, newValue) -> {
                //debug
                //if (newValue.intValue() != 1) {
                //    System.out.println("debug");
                //}
                //重置
                if (operates.getChildren().size() > 0)
                    operates.getChildren().remove(0, operates.getChildren().size());

                coursesMap = new LinkedHashMap<>();
                courseIdClassNoAndCoursePanes = new LinkedHashMap<>();

                generateCalendar(courseCalendar, newValue.intValue() + 1, scene, currentUser);
            }));

            //设置当前的周次
            viewWeek.getSelectionModel().select((int)(ChronoUnit.DAYS.between(LocalDate.of(YearMonth.now()
                            .getYear(), FirstDayWeek.getMonth(), FirstDayWeek.getDayOfMonth()),
                    LocalDate.of(YearMonth.now().getYear(), MonthDay.now().getMonth(),
                            MonthDay.now().getDayOfMonth())) / 7));

            scene.getWindow().setOnCloseRequest((event -> {
                popOverInfo.hide();
                stage.close();
            }));

            stage.show();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            //严重问题
            ExceptionDialog.launch(ioe, "发生严重问题", "在加载/fxml/ui/ViewSelected.fxml的时候发生严重问题");
        }
    }

    /**
     * 构造课程表
     * @param calendarRoot 课程表的外层ScrollPane
     * @param week 周次
     * @param scene main scene
     * @param currentUser 当前用户
     */
    private static void generateCalendar(ScrollPane calendarRoot, int week, Scene scene,
                                         DHUCurrentUser currentUser) {
        if (week < 1 || week > 18)
            // TODO 出错
            return;

        /* Test */
        /*
        ArrayList<AnchorPaneNode> allCalendarDays = new ArrayList<>(35);
        // Create the calendar grid pane
        GridPane calendar = new GridPane();
        calendar.prefWidthProperty().bind(calendarRoot.prefWidthProperty().multiply(0.95));
        calendar.setMinHeight(5 * 100.0d);
        //calendar.setGridLinesVisible(true);
        // Create rows and columns with anchor panes for the calendar
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 7; j++) {
                if (i == 1 && j == 4)
                    break;

                if (i == 2 && j == 6) {
                    AnchorPaneNode ap = new AnchorPaneNode();

                    ap.prefWidthProperty().bind(scene.widthProperty()
                            .subtract(new SimpleDoubleProperty(190.0d))
                            .divide(8));
                    ap.setPrefHeight(100.0d * 3);
                    ap.setStyle("-fx-background-color: #01A05E; -fx-background-radius: 15 15 15 15;" +
                            "-fx-opacity: 0.4");
                    JFXDepthManager.setDepth(ap, 4);
                    calendar.add(ap, j, i, 1, 3);
                    allCalendarDays.add(ap);
                    break;
                }

                if (j == 6 && (i == 3 || i == 4)) {
                    break;
                }

                if (i == 4) {
                    AnchorPaneNode ap = new AnchorPaneNode();

                    ap.prefWidthProperty().bind(scene.widthProperty()
                            .subtract(new SimpleDoubleProperty(190.0d)));
                    ap.setPrefHeight(100.0d);
                    ap.setStyle("-fx-background-color: #01A05E; -fx-background-radius: 15 15 15 15;" +
                            "-fx-opacity: 0.4");
                    JFXDepthManager.setDepth(ap, 4);
                    calendar.add(ap, j, i, 7, 1);
                    allCalendarDays.add(ap);
                    break;
                }

                AnchorPaneNode ap = new AnchorPaneNode();
                DoubleBinding prefWidth = calendarRoot.prefWidthProperty()
                        .divide(new SimpleDoubleProperty(8.0d));
                ap.prefWidthProperty().bind(prefWidth);
                //debug
                //prefWidth.addListener(((observable, oldValue, newValue) -> {
                //    System.out.println("debug: new stackPane pref width: " + newValue);
                //}));
                //Make the binding valid...
                prefWidth.getValue();
                ap.setPrefHeight(100.0d);
                calendar.add(ap, j, i);
                allCalendarDays.add(ap);
            }
        }

        YearMonth yearMonth = YearMonth.now();
        // Get the date we want to start with on the calendar
        LocalDate calendarDate = LocalDate.of(yearMonth.getYear(), yearMonth.getMonthValue(), 1);
        // Dial back the day until it is SUNDAY (unless the month starts on a sunday)
        while (!calendarDate.getDayOfWeek().toString().equals("SUNDAY") ) {
            calendarDate = calendarDate.minusDays(1);
        }
        // Populate the calendar with day numbers
        for (AnchorPaneNode ap : allCalendarDays) {
            if (ap.getChildren().size() != 0) {
                ap.getChildren().remove(0);
            }
            Text txt = new Text(String.valueOf(calendarDate.getDayOfMonth()));
            txt.setTextAlignment(TextAlignment.CENTER);
            txt.setFont(Font.font("msyh", FontWeight.BOLD, 18));
            ap.setDate(calendarDate);
            ap.setTopAnchor(txt, ap.getPrefHeight() / 2);
            ap.setLeftAnchor(txt, ap.getPrefWidth() / 2);
            ap.getChildren().add(txt);
            calendarDate = calendarDate.plusDays(1);
        }

        calendarRoot.setContent(calendar);
        */

        GridPane calendar = new GridPane();
        calendar.prefWidthProperty().bind(calendarRoot.prefWidthProperty().multiply(0.95));
        calendar.setMinHeight(5 * 100.0d);
        calendar.setGridLinesVisible(false);
        // FIXME 默认是5天13节课(其中多了两行用来作为午饭和晚饭时间, 还有第一列显示节次和时间)
        int cnt = 0;
        for (int i = 0; i < 16; i++) {
            if (i == 0) {
                /*
                //获取今天所在的周次的第一天的日期
                //Calendar中的DAY_OF_WEEK的第一天是周日
                LocalDate firstDayOfWeek = LocalDate.now().minusDays(Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                        == 1 ? 6 : Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 2);
                */
                //获取week这一周的第一天的日期
                LocalDate firstDayOfWeek = LocalDate.of(YearMonth.now().getYear(),
                        FirstDayWeek.getMonth(), FirstDayWeek.getDayOfMonth()).plusDays((week - 1) * 7);

                calendar.add(generateHeaderPane(firstDayOfWeek.getMonth().getValue() + "月", "",
                        scene.widthProperty()
                                .subtract(190.0d)
                                .multiply(0.95)
                                .divide(6), 50.0d), 0, 0, 1, 1);
                for (int j = 1; j < 6; j++) {
                    calendar.add(generateHeaderPane(toChinese(j),
                            (firstDayOfWeek.getDayOfMonth() + j - 1) + "日",
                            scene.widthProperty()
                                    .subtract(190)
                                    .multiply(0.95)
                                    .divide(6), 50.0d), j, 0, 1, 1);
                }
                continue;
            }

            if (i == 5 || i == 11) {
                StackPane pane = new StackPane();
                pane.prefWidthProperty().bind(scene.widthProperty()
                        .subtract(190).multiply(0.95));
                pane.setPrefHeight(8.0d);
                pane.setStyle("-fx-background-color: #673AB7; -fx-background-radius: 3 3 3 3; ");
                calendar.add(pane, 0, i, 8, 1);
                continue;
            }

            // TODO 识别校区
            ++cnt;
            calendar.add(generateHeaderPane("第" + cnt + "节", getStartTimeByCourseNum(cnt, 2),
                    scene.widthProperty()
                            .subtract(190.0d)
                            .multiply(0.95)
                            .divide(6)
                            .multiply(0.7), 100.0d), 0, i);
        }

        calendarRoot.setContent(calendar);

        //获取已经录取课程
        Course[] matriculated = Course.getMatriculated(currentUser.getUserCookie());
        Course[] selected = Course.getSelected(currentUser.getUserCookie());

        ObservableList<CourseLabel> matriculatedList = FXCollections.observableArrayList();
        ObservableList<CourseLabel> selectedList = FXCollections.observableArrayList();

        int index = 0;

        if (matriculated != null)
            for (Course course : matriculated) {
                CourseLabel courseLabel = new CourseLabel(course, course.getCourse().getTextTitle() + " " +
                        (course.getTeacher() == null ? "" : course.getTeacher().getTextTitle())
                        + "@" + (course.getPlaces() == null || course.getPlaces()[0].equals("")
                        ? "(无确定教室)" : course.getPlaces()[0]));
                matriculatedList.add(courseLabel);
                int indexTmp = index;
                courseLabel.setOnMouseEntered((event -> {
                    // TODO 鼠标放在Label上悬停 现在的这个实现失败了
                    matriculatedCourses.getSelectionModel().select(indexTmp);
                }));
                matriculatedListViewMap.put(course.getCourseId() + "-" + course.getClassNo(), index++);
                generateCoursePane(calendar, course, week, scene.widthProperty()
                    .subtract(190.0d)
                    .multiply(0.95)
                    .divide(6), 100.0d, "81C784", currentUser);
            }

        matriculatedCourses.setItems(matriculatedList);
        matriculatedCourses.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            //最开始的时候是没有选择的选项的
            if (newValue != null)
                courseClickAction(newValue.getCourse(), true, currentUser);
            Course tmp = newValue.getCourse();
            lastClickedCoursePane = courseIdClassNoAndCoursePanes.containsKey(tmp.getCourseId() + "-" +
                    tmp.getClassNo()) ? courseIdClassNoAndCoursePanes.get(tmp.getCourseId() + "-" +
                    tmp.getClassNo()) : new DoubleLinkedList<>();
            lastClickedIsMatriculated = true;
        }));

        index = 0;

        if (selected != null)
            for (Course course : selected) {
                CourseLabel courseLabel = (new CourseLabel(course, course.getCourse().getTextTitle() + " " +
                        (course.getTeacher() == null ? "" : course.getTeacher().getTextTitle())
                        + "@" + (course.getPlaces() == null || course.getPlaces()[0].equals("") ?
                        "(无确定教室)" : course.getPlaces()[0])));
                selectedList.add(courseLabel);
                selectedListViewMap.put(course.getCourseId() + "-" + course.getClassNo(), index++);
                int indexTmp = index;
                courseLabel.setOnMouseEntered((event -> {
                    // TODO 鼠标放在Label上悬停 现在的这个实现失败了
                    matriculatedCourses.getSelectionModel().select(indexTmp);
                }));
                generateCoursePane(calendar, course, week, scene.widthProperty()
                    .subtract(190.0d)
                    .multiply(0.95)
                    .divide(6), 100.0d, "BDBDBD", currentUser);
            }

        selectedCourses.setItems(selectedList);
        selectedCourses.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            //最开始的时候是没有选择的选项的
            if (newValue != null)
                courseClickAction(newValue.getCourse(), false, currentUser);
            Course tmp = newValue.getCourse();
            lastClickedCoursePane = courseIdClassNoAndCoursePanes.containsKey(tmp.getCourseId() + "-" +
                    tmp.getClassNo()) ? courseIdClassNoAndCoursePanes.get(tmp.getCourseId() + "-" +
                    tmp.getClassNo()) : new DoubleLinkedList<>();
            lastClickedIsMatriculated = true;
        }));
    }

    /**
     * 生成日期节次这些的pane
     */
    private static StackPane generateHeaderPane(String firstLien, String secondLine, DoubleBinding width,
                                                double height) {
        StackPane content = new StackPane();
        content.prefWidthProperty().bind(width);
        content.setPrefHeight(height);
        content.setStyle("-fx-background-color: #E0E0E0; -fx-background-radius: 3 3 3 3;");
        Label firstLabel = new Label(firstLien);
        firstLabel.setFont(Font.font("msyh", FontWeight.BOLD, 16));
        firstLabel.setAlignment(Pos.CENTER);
        Label secondLabel = new Label(secondLine);
        secondLabel.setFont(Font.font("msyh", 14));
        secondLabel.setAlignment(Pos.CENTER);
        VBox vBox = new VBox(15.0d, firstLabel, secondLabel);
        vBox.setAlignment(Pos.CENTER);
        content.getChildren().add(vBox);

        return content;
    }

    /**
     * 生成课程的pane
     * @param course 课程
     * @param week 周次
     */
    private static void generateCoursePane(GridPane calendar, Course course, int week, DoubleBinding width,
                                    double singleHeight, String color, DHUCurrentUser currentUser) {
        StackPane child = new StackPane();
        try {
            course.getClassTimeDayWeek(week, 1);
        } catch (IllegalArgumentException ie) {
            //不在这个范围内
            return;
        }

        for (int i = 0; i < ClassTime.getMaxDaysOfWeek(); i++) {
            //debug
            //if (i == 3) {
            //    System.out.println("debug");
            //}

            int[] sections = course.getClassTimeDayWeek(week, i + 1);
            if (sections != null) {
                //处理冲突
                //记录上一个冲突的course的courseNo
                String lastConflict = "";
                //没有冲突的节次(s)
                DoubleLinkedList<DoubleLinkedList<Integer>> notConflictSections = new DoubleLinkedList<>();
                //当前没有冲突的节次
                DoubleLinkedList<Integer> currentNotConflict = new DoubleLinkedList<>();

                //debug
                //if (course.getCourse().getTextTitle().equals("数据结构")) {
                //    System.out.println("debug");
                //}
                Pointer<Course> oldCoursePointer = new Pointer<>(null);
                //Course oldCourse = null;

                for (int section : sections) {
                    if (coursesMap.containsKey((i + 1) + "-" + section)) {
                        if (!lastConflict.equals(coursesMap
                                .get((i + 1) + "-" + section).course.getCourseId())) {
                            oldCoursePointer.setP(coursesMap.get((i + 1) + "-" + section).getCourse());

                            notConflictSections.addLast(currentNotConflict);
                            currentNotConflict = new DoubleLinkedList<>();

                            StackPane extraInfo = coursesMap.get((i + 1) + "-" + section).getExtraInfoPane();
                            Label label = new Label("有冲突课程, 点击查看!");
                            label.setFont(Font.font("msyh", FontWeight.BOLD, 16));
                            //自动换行
                            label.setWrapText(true);
                            label.setPadding(new Insets(6.5, 6.5, 6.5, 6.5));
                            extraInfo.getChildren().add(label);
                            coursesMap.get((i + 1) + "-" + section).addConflictCourse(course);
                            //查看冲突课程
                            //处理extraInfo的点击事件, 弹出这一节次其他冲突课程的信息
                            extraInfo.setOnMouseClicked((event -> {
                                JFXListView<Label> courseListView = new JFXListView<>();
                                if (!conflictCoursesMap.containsKey(oldCoursePointer.getP().getCourseId()
                                        + "-" + oldCoursePointer.getP().getClassNo()))
                                    conflictCoursesMap.put(oldCoursePointer.getP().getCourseId() + "-" + oldCoursePointer.getP().getClassNo(),
                                        new DoubleLinkedList<>(new Label[]{new Label(course.getCourse().getTextTitle() + " " +
                                        (course.getTeacher() == null ? "" : course.getTeacher().getTextTitle())
                                        + "@" + (course.getPlaces() == null || course.getPlaces()[0].equals("") ?
                                        "(无确定教室)" : oldCoursePointer.getP().getPlaces()[0]))}));
                                else if (!conflictCourseId.contains(oldCoursePointer.getP().getCourseId()
                                        + "-" + oldCoursePointer.getP().getClassNo()))
                                    conflictCoursesMap.get(oldCoursePointer.getP().getCourseId()
                                            + "-" + oldCoursePointer.getP().getClassNo()).addLast(new Label(oldCoursePointer.getP().getCourse().getTextTitle() + " " +
                                            (oldCoursePointer.getP().getTeacher() == null ? "" : oldCoursePointer.getP().getTeacher().getTextTitle())
                                            + "@" + (oldCoursePointer.getP().getPlaces() == null || oldCoursePointer.getP().getPlaces()[0].equals("") ?
                                            "(无确定教室)" : oldCoursePointer.getP().getPlaces()[0])));

                                conflictCourseId.add(oldCoursePointer.getP().getCourseId()
                                        + "-" + oldCoursePointer.getP().getClassNo());

                                ObservableList<Label> courseList = FXCollections.observableArrayList(
                                        conflictCoursesMap.get(oldCoursePointer.getP().getCourseId()
                                                + "-" + oldCoursePointer.getP().getClassNo()).toArray());

                                popOverInfo.setAutoHide(true);

                                courseListView.setItems(courseList);
                                StackPane stackPane = new StackPane(courseListView);
                                stackPane.setPrefSize(350, 400);
                                popOverInfo.setContentNode(stackPane);
                                popOverInfo.setTitle("所有与该课程冲突的课程");
                                popOverInfo.setPrefSize(350, 400);
                                popOverInfo.setDetachable(false);
                                popOverInfo.setArrowIndent(5);
                                popOverInfo.show(extraInfo);
                                // FIXME 在显示PopOver的时候如果关闭窗口, 就会出现一堆错误....
                            }));

                            lastConflict = coursesMap.get((i + 1) + "-" + section).getCourse().getCourseId();
                        }
                    } else {
                        currentNotConflict.addLast(section);
                    }
                }
                //最后再添加最后一个
                notConflictSections.addLast(currentNotConflict);

                /*
                if (oldCoursePointer.getP() != null && notConflictSections.getSize() == 0) {
                    //如果完全冲突了, 那就把oldCourse的Pane的body的内容改一下
                    CoursePaneNode oldCoursePane = courseIdClassNoAndCoursePanes.get(oldCoursePointer
                            .getP().getCourseId() + "-" + oldCoursePointer.getP().getClassNo())
                            .get(0);
                    oldCoursePane.setCourse(course);
                    ((Label) ((StackPane)((VBox) oldCoursePane.getChildren().get(0)).getChildren().get(0)).getChildren()
                            .get(0)).setText(course.getCourse().getTextTitle() + " " +
                            (course.getTeacher() == null ? "" : course.getTeacher().getTextTitle())
                            + "@" + (course.getPlaces() == null || course.getPlaces()[0].equals("") ?
                            "(无确定教室)" : course.getPlaces()[0]));
                } else {
                 */
                    for (DoubleLinkedList<Integer> sectionsList : notConflictSections) {
                        if (sectionsList.getSize() == 0)
                            continue;

                        Integer[] sections1 = sectionsList.toArray();

                        //debug
                        //if (sections1 == null)
                        //    System.out.println("debug");

                        CoursePaneNode coursePane = new CoursePaneNode();
                        coursePane.prefWidthProperty().bind(width);
                        coursePane.setPrefHeight(singleHeight * sections1.length);
                        coursePane.setPadding(new Insets(5, 5, 5, 5));
                        JFXDepthManager.setDepth(coursePane, 4);
                        VBox content = new VBox(0);
                        StackPane body = new StackPane();
                        body.setStyle("-fx-background-color: #" + color + "; -fx-background-radius: 8 8 0 0;");
                        body.setPrefHeight(singleHeight * sections1.length * 0.7);
                        Label info = new Label(course.getCourse().getTextTitle() + " " +
                                (course.getTeacher() == null ? "" : course.getTeacher().getTextTitle())
                                + "@" + (course.getPlaces() == null || course.getPlaces()[0].equals("") ?
                                "(无确定教室)" : course.getPlaces()[0]));
                        info.setFont(Font.font("msyh", 16));
                        info.setTextFill(Color.WHITE);
                        info.prefWidthProperty().bind(width);
                        info.setPrefHeight(singleHeight * sections1.length * 0.6);
                        //自动换行
                        info.setWrapText(true);
                        info.setPadding(new Insets(6.5, 6.5, 6.5, 6.5));
                        body.getChildren().add(info);

                        coursePane.setCourse(course);
                        // 处理点击事件, 也就是为operates添加操作按钮
                        body.setOnMouseClicked((event -> {
                            //courseClickAction(course, coursePane, body, currentUser);
                            String tmpStyle = body.getStyle();

                            if (body.getStyle().contains("#81C784")) {
                                //已入取课程
                                courseClickAction(course, true, currentUser);
                                matriculatedCourses.getSelectionModel().select(matriculatedListViewMap.
                                        get(course.getCourseId() + "-" + course.getClassNo()));
                            } else {
                                //已选未录取课程
                                courseClickAction(course, false, currentUser);
                                selectedCourses.getSelectionModel().select(selectedListViewMap.
                                        get(course.getCourseId() + "-" + course.getClassNo()));
                            }

                            lastClickedCoursePane = courseIdClassNoAndCoursePanes.containsKey(course.getCourseId() + "-" +
                                    course.getClassNo()) ? courseIdClassNoAndCoursePanes.get(course.getCourseId() + "-" +
                                    course.getClassNo()) : new DoubleLinkedList<>();
                            lastClickedIsMatriculated =tmpStyle.contains("#81C784");
                        }));

                        StackPane extraInfo = new StackPane();
                        extraInfo.setStyle("-fx-background-color: #" + color + "; -fx-background-radius: 0 0 8 8;");
                        extraInfo.setPrefHeight(singleHeight * sections1.length * 0.4);
                        content.getChildren().addAll(body, extraInfo);
                        coursePane.setExtraInfoPane(extraInfo);
                        //如果没有冲突课程, extraInfo的点击事件与body保持一致
                        extraInfo.setOnMouseClicked((event -> {
                            //debug
                            //System.out.println("operates将出来一堆相关按钮");
                            String tmpStyle = body.getStyle();

                            courseClickAction(course, body.getStyle().contains("#81C784"), currentUser);
                            lastClickedCoursePane = courseIdClassNoAndCoursePanes.containsKey(course.getCourseId() + "-" +
                                    course.getClassNo()) ? courseIdClassNoAndCoursePanes.get(course.getCourseId() + "-" +
                                    course.getClassNo()) : new DoubleLinkedList<>();
                            lastClickedIsMatriculated = tmpStyle.contains("#81C784");
                            //courseClickAction(course, coursePane, body, currentUser);
                        }));
                        coursePane.getChildren().addAll(content);

                        //注册进map
                        for (int section : sections1) {
                            coursesMap.put((i + 1) + "-" + section, coursePane);
                        }

                        DoubleLinkedList<CoursePaneNode> coursePaneNodes;
                        if ((coursePaneNodes = courseIdClassNoAndCoursePanes.get(course.getCourseId() + "-"
                                + course.getClassNo())) != null) {
                            coursePaneNodes.addLast(coursePane);
                        } else {
                            courseIdClassNoAndCoursePanes.put(course.getCourseId() + "-"
                                    + course.getClassNo(), new DoubleLinkedList<>(new CoursePaneNode[]{coursePane}));
                        }

                        //添加进ScrollPane
                        if (sections1[0] > 4 && sections1[0] < 10)
                            calendar.add(coursePane, i + 1, sections1[0] + 1, 1,
                                    sections1.length);
                        else if (sections1[0] > 9)
                            calendar.add(coursePane, i + 1, sections1[0] + 2, 1,
                                    sections1.length);
                        else
                            calendar.add(coursePane, i + 1, sections1[0], 1,
                                    sections1.length);
                    }
            }
        }

    }

    /**
     * 一堆功能按钮
     * @param matriculated 在当前课表中是显示出来的(也就是绿色的)而且是已经录取的课程吗?
     */
    private static void courseClickAction(Course course, boolean matriculated,
                                          DHUCurrentUser currentUser) {
        //debug
        //System.out.println("operates将出来一堆相关按钮");

        //重置
        if (operates.getChildren().size() > 0)
            operates.getChildren().remove(0, operates.getChildren().size());

        //重置上一个选中的panes
        if (lastClickedCoursePane.getSize() > 0) {
            for (CoursePaneNode coursePaneNode : lastClickedCoursePane) {
                VBox vBox = (VBox) coursePaneNode.getChildren().get(0);
                ((vBox.getChildren().get(0))).setStyle("-fx-background-color: #" +
                        (lastClickedIsMatriculated ? "81C784" : "BDBDBD" +
                                ";-fx-background-radius: 8 8 0 0;"));
                ((vBox.getChildren().get(1))).setStyle("-fx-background-color: #" +
                        (lastClickedIsMatriculated ? "81C784" : "BDBDBD" +
                                ";-fx-background-radius: 0 0 8 8;"));
            }
        }

        //设置panes为红色
        if (courseIdClassNoAndCoursePanes.containsKey(course.getCourseId() + "-" +
                course.getClassNo()))
            for (CoursePaneNode coursePaneNode : courseIdClassNoAndCoursePanes.get(course.getCourseId() + "-" +
                    course.getClassNo())) {
                (((VBox) coursePaneNode.getChildren().get(0)).getChildren().get(0))
                        .setStyle("-fx-background-color: #E91E63; -fx-background-radius: 8 8 0 0;");
                (((VBox) coursePaneNode.getChildren().get(0)).getChildren().get(1))
                        .setStyle("-fx-background-color: #E91E63; -fx-background-radius: 0 0 8 8;");
            }

        //TODO FIXME infoSnackBar的背景是透明的, 不知道为什么
        JFXSnackbar infoSnackBar = new JFXSnackbar();
        infoSnackBar.registerSnackbarContainer((StackPane)((AnchorPane)((BorderPane)root.getChildren().get(0)).getBottom())
                .getChildren().get(0));

        JFXButton courseIntro = new JFXButton("查看课程介绍"),
                courseCalendar = new JFXButton("查看教学日历"),
                courseTeacher = new JFXButton("查看任课教师"),
                delCourse = new JFXButton("删除");

        courseIntro.setStyle("-fx-padding: 0.7em 0.57em;\n" +
                "-fx-font-family: msyh;\n" +
                "-fx-font-size: 16;\n" +
                "-fx-font-weight: bold;\n" +
                "-jfx-button-type: RAISED;\n" +
                "-fx-background-color: rgb(77, 102, 204);\n" +
                "-fx-min-width: 150;\n" +
                "-fx-text-fill: WHITE;");
        courseCalendar.setStyle("-fx-padding: 0.7em 0.57em;\n" +
                "-fx-font-family: msyh;\n" +
                "-fx-font-size: 16;\n" +
                "-fx-font-weight: bold;\n" +
                "-jfx-button-type: RAISED;\n" +
                "-fx-background-color: rgb(77, 102, 204);\n" +
                "-fx-min-width: 150;\n" +
                "-fx-text-fill: WHITE;");
        courseTeacher.setStyle("-fx-padding: 0.7em 0.57em;\n" +
                "-fx-font-family: msyh;\n" +
                "-fx-font-size: 16;\n" +
                "-fx-font-weight: bold;\n" +
                "-jfx-button-type: RAISED;\n" +
                "-fx-background-color: rgb(77, 102, 204);\n" +
                "-fx-min-width: 150;\n" +
                "-fx-text-fill: WHITE;");
        delCourse.setStyle("-fx-padding: 0.7em 0.57em;\n" +
                "-fx-font-family: msyh;\n" +
                "-fx-font-size: 16;\n" +
                "-fx-font-weight: bold;\n" +
                "-jfx-button-type: RAISED;\n" +
                "-fx-background-color: rgb(77, 102, 204);\n" +
                "-fx-min-width: 150;\n" +
                "-fx-text-fill: WHITE;");

        courseIntro.setOnMouseClicked((event1 -> {
            loadURL(course.getCourse().getLink(), currentUser.getUserCookie());
        }));

        courseCalendar.setOnMouseClicked((event1 ->
                loadURL(course.getCalendarURL(), currentUser.getUserCookie())));

        courseTeacher.setOnMouseClicked((event1 -> {
            Teacher teacher = course.getTeacher();
            if (!teacher.getTextTitle().equals(""))
                loadURL(teacher.getLink(), currentUser.getUserCookie());
            else
                infoSnackBar.fireEvent(new JFXSnackbar.SnackbarEvent("没有老师信息!",
                        "了解", 2000, true,
                        b -> infoSnackBar.close()));
        }));

        delCourse.setOnMouseClicked(event1 -> {
            if (matriculated) {
                JFXDialogLayout dialogLayout = new JFXDialogLayout();
                Label heading = new Label("是否确认删除课程" + course.getCourse().getTextTitle()
                        + "?");
                heading.setFont(Font.font("msyh", FontWeight.BOLD, 26));
                dialogLayout.setHeading(heading);
                Label body = new Label("");
                dialogLayout.setBody(body);
                JFXButton confirmBtn = new JFXButton("确认删除")
                        , cancelBtn = new JFXButton("取消");
                confirmBtn.setFont(Font.font("msyh", FontWeight.BOLD, 28));
                cancelBtn.setFont(Font.font("msyh", FontWeight.BOLD, 28));
                confirmBtn.setTextFill(Color.DARKBLUE);
                cancelBtn.setTextFill(Color.DARKBLUE);
                dialogLayout.setActions(cancelBtn, confirmBtn);
                JFXDialog delCourseDialog = new JFXDialog(root, dialogLayout, JFXDialog.DialogTransition.CENTER);

                cancelBtn.setOnMouseClicked((event -> delCourseDialog.close()));
                confirmBtn.setOnMouseClicked((e) ->{
                    try {
                        delCourseDialog.close();
                        if (currentUser.deleteMatriculatedCourse(course))
                            infoSnackBar.fireEvent(new JFXSnackbar.SnackbarEvent("课程" +
                                    course.getCourse().getTextTitle() + "删除成功",
                                    "了解", 2000, true,
                                    b -> infoSnackBar.close()));
                        else
                            infoSnackBar.fireEvent(new JFXSnackbar.SnackbarEvent("课程" +
                                    course.getCourse().getTextTitle() + "删除失败! 请检查日志.",
                                    "了解", 2000, true,
                                    b -> infoSnackBar.close()));
                    } catch (IllegalCourseException ie) {
                        //course信息有问题
                        logger.severe("删除已录取课程的时候, 给定的Course中" +
                                "没有classNo或者courseId");
                        ExceptionDialog.launch(ie, "严重错误", "删除已录取课程的时候, 给定的Course中" +
                                "没有classNo或者courseId");
                    }
                });

                delCourseDialog.show();
            } else {
                //删除已选但是未录取课程
                try {
                    if (currentUser.deleteSelectedCourse(course))
                        infoSnackBar.fireEvent(new JFXSnackbar.SnackbarEvent("课程" +
                                course.getCourse().getTextTitle() + "删除成功",
                                "了解", 2000, true,
                                b -> infoSnackBar.close()));
                    else
                        infoSnackBar.fireEvent(new JFXSnackbar.SnackbarEvent("课程" +
                                course.getCourse().getTextTitle() + "删除失败! 请检查日志.",
                                "了解", 2000, true,
                                b -> infoSnackBar.close()));
                } catch (IllegalCourseException ie) {
                    //course信息有问题
                    logger.severe("删除已选但未录取课程的时候, 给定的Course中" +
                            "没有classNo或者courseId");
                    ExceptionDialog.launch(ie, "严重错误", "删除已选但未录取课程的时候, 给定的Course中" +
                            "没有classNo或者courseId");
                }
            }
        });

        operates.getChildren().addAll(courseIntro, courseCalendar, courseTeacher, delCourse);

    }

    /**
     * 加载浏览器
     * @param url 链接
     */
    static void loadURL(URL url, String cookie) {
        Stage webViewStage = new Stage();

        JFXProgressBar progressBar = new JFXProgressBar(0);

        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);
        progressBar.progressProperty().bind(webEngine.getLoadWorker().progressProperty());

        Slider slider = new Slider(0.5,2,1);
        ZoomingPane zoomingPane = new ZoomingPane(webView);
        zoomingPane.zoomFactorProperty().bind(slider.valueProperty());

        Scene scene = new Scene(new BorderPane(zoomingPane, progressBar, null,
                slider, null), 800, 600);

        progressBar.prefWidthProperty().bind(scene.widthProperty());
        webViewStage.setScene(scene);
        webViewStage.show();
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("Set-Cookie", Arrays.asList(cookie.split(";")));
        try {
            java.net.CookieManager.getDefault().put(url.toURI(), headers);
        } catch (URISyntaxException | IOException ioe) {
            // TODO ....

        }

        webEngine.load(url.toString());
    }

    private static class ZoomingPane extends Pane {
        Node content;
        private DoubleProperty zoomFactor = new SimpleDoubleProperty(1);

        private ZoomingPane(Node content) {
            this.content = content;
            getChildren().add(content);
            Scale scale = new Scale(1, 1);
            content.getTransforms().add(scale);

            zoomFactor.addListener((ObservableValue<? extends Number> observable,
                    Number oldValue, Number newValue) -> {
                    scale.setX(newValue.doubleValue());
                    scale.setY(newValue.doubleValue());
                    requestLayout();
                });
        }

        protected void layoutChildren() {
            Pos pos = Pos.TOP_LEFT;
            double width = getWidth();
            double height = getHeight();
            double top = getInsets().getTop();
            double right = getInsets().getRight();
            double left = getInsets().getLeft();
            double bottom = getInsets().getBottom();
            double contentWidth = (width - left - right)/zoomFactor.get();
            double contentHeight = (height - top - bottom)/zoomFactor.get();
            layoutInArea(content, left, top,
                    contentWidth, contentHeight,
                    0, null,
                    pos.getHpos(),
                    pos.getVpos());
        }

        public final Double getZoomFactor() {
            return zoomFactor.get();
        }
        public final void setZoomFactor(Double zoomFactor) {
            this.zoomFactor.set(zoomFactor);
        }
        final DoubleProperty zoomFactorProperty() {
            return zoomFactor;
        }
    }

    private static String toChinese(int i) {
        switch (i) {
            case 1 : return "星期一";
            case 2 : return "星期二";
            case 3 : return "星期三";
            case 4 : return "星期四";
            case 5 : return "星期五";
            case 6 : return "星期六";
            case 7 : return "星期日";
            default: return "";
        }
    }

    /**
     * TODO 改成从教务处读取
     * @param num 节次(1 ~ 13)
     * @param campus 校区代号 1表示延安路校区 2表示松江校区 3表示国际教育中心大楼（IECB）
     * @return 开始时间
     */
    private static String getStartTimeByCourseNum(int num, int campus) {
        if (campus == 1) {
            switch (num) {
                case 1 : return "8:00";
                case 2 : return "8:55";
                case 3 : return "9:55";
                case 4 : return "10:50";
                case 5 : return "13:30";
                case 6 : return "14:25";
                case 7 : return "15:20";
                case 8 : return "16:15";
                case 9 : return "17:10";
                case 10 : return "18:30";
                case 11 : return "19:25";
                case 12 : return "20:20";
                case 13 : return "21:15";
                default: return "";
            }
        } else if (campus == 2) {
            switch (num) {
                case 1 : return "8:15";
                case 2 : return "9:00";
                case 3 : return "10:05";
                case 4 : return "10:50";
                case 5 : return "13:00";
                case 6 : return "13:45";
                case 7 : return "14:50";
                case 8 : return "15:35";
                case 9 : return "16:20";
                case 10 : return "18:00";
                case 11 : return "18:45";
                case 12 : return "19:50";
                case 13 : return "20:35";
                default: return "";
            }
        } else if (campus == 3) {
            // TODO FIXME
        }

        return "";
    }

    /**
     * CoursePaneNode
     */
    private static class CoursePaneNode extends StackPane {
        private Course course;
        //存储这冲突的信息
        private StackPane extraInfoPane;
        //冲突课程
        private DoubleLinkedList<Course> conflictCourses = new DoubleLinkedList<>();

        CoursePaneNode() {
            super();
        }

        void setCourse(Course course) {
            this.course = course;
        }

        Course getCourse() {
            return course;
        }

        void setExtraInfoPane(StackPane extraInfoPane) {
            this.extraInfoPane = extraInfoPane;
        }

        StackPane getExtraInfoPane() {
            return extraInfoPane;
        }

        DoubleLinkedList<Course> addConflictCourse(Course course) {
            conflictCourses.addLast(course);

            return conflictCourses;
        }

        private Course[] getConflictCourses() {
            return conflictCourses.toArray();
        }
    }

    /**
     * Label that contain Course
     */
    private static class CourseLabel extends Label {
        private Course course;

        CourseLabel(Course course, String text) {
            super(text);
            this.course = course;
        }

        public CourseLabel setCourse(Course course) {
            this.course = course;
            return this;
        }

        Course getCourse() {
            return course;
        }
    }

    /*
     * Just for Test
     */
    /*
    private static class AnchorPaneNode  extends AnchorPane {

        // Date associated with this pane
        private LocalDate date;
    */
        /*
         * Create a anchor pane node. Date is not assigned in the constructor.
         * @param children children of the anchor pane
         */
        /*
        public AnchorPaneNode(Node... children) {
            super(children);
            // Add action handler for mouse clicked
            this.setOnMouseClicked(e -> System.out.println("This pane's date is: " + date));
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }
    }
    */
}///~
