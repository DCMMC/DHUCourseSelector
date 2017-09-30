package tk.dcmmcc.gui.uicomponents;

import com.jfoenix.controls.*;
import io.datafx.controller.ViewController;
import io.datafx.controller.flow.context.FXMLViewFlowContext;
import io.datafx.controller.flow.context.ViewFlowContext;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import tk.dcmmcc.Course;
import tk.dcmmcc.CourseType;
import tk.dcmmcc.datafx.ClassesData;
import tk.dcmmcc.datafx.CourseClassRequestQueue;
import tk.dcmmcc.datafx.CourseData;
import tk.dcmmcc.datafx.SettingData;
import tk.dcmmcc.utils.LoggerUtil;
import javax.annotation.PostConstruct;
import java.util.logging.Logger;

/**
 * TODO 完成智能导入 手动导入功能
 * TODO 把ListView改成ExpandableListTableView
 * 修改课程选课队列的界面的Controller
 * Created by DCMMC on 2017/9/10.
 */
@ViewController(value = "/fxml/ui/EditQueue.fxml", title = "Select Courses Session")
public class EditCourseClassRequestQueueController {
    @FXMLViewFlowContext
    private ViewFlowContext context;
    @FXML
    private StackPane root;
    @FXML
    private VBox vboxRoot;

    private static Logger logger = Logger.getLogger(SettingData.class.getName());
    private HBox opsBoxRoot;
    private JFXSnackbar infoSnackBar;

    static {
        LoggerUtil.initLogger(logger);
    }

    /**
     * init fxml when loaded.
     */
    @PostConstruct
    public void init() throws Exception {
        infoSnackBar = new JFXSnackbar((StackPane) vboxRoot.getChildren().get(1));
        //把SessionController中的东西重置(比如选课次数之类的)一边
        //重置成功选课的Course数目
        CourseClassRequestQueue.getQueueCoursesNumberProperty().set(0);
        //重置选课次数
        CourseClassRequestQueue.getSelectedCoursesNumberProperty().set(0);
        //重置成功选课个数
        CourseClassRequestQueue.getSelectedCoursesNumberProperty().set(0);

        AnchorPane mainButtonsPane = new AnchorPane();
        //相关操作按钮
        opsBoxRoot = new HBox(10.0d);
        AnchorPane.setLeftAnchor(opsBoxRoot, 50.0d);
        AnchorPane.setBottomAnchor(opsBoxRoot, 15.0d);

        JFXButton confirmBtn = new JFXButton("确认");
        confirmBtn.setStyle("-fx-padding: 0.7em 0.57em;\n" +
                "-fx-font-family: msyh;\n" +
                "-fx-font-size: 16;\n" +
                "-fx-font-weight: bold;\n" +
                "-jfx-button-type: RAISED;\n" +
                "-fx-background-color: rgb(77, 102, 204);\n" +
                "-fx-min-width: 120;\n" +
                "-fx-text-fill: WHITE;");
        // TODO
        confirmBtn.setOnMouseClicked((event -> {

        }));

        opsBoxRoot.getChildren().add(confirmBtn);

        //选课队列信息
        courseListViewFactory();

        mainButtonsPane.getChildren().addAll(opsBoxRoot);
        //放到bottom上面
        ((BorderPane)((StackPane)context.getRegisteredObject("MainRoot")).getChildren().get(0))
                .setBottom(mainButtonsPane);
    }

    /**
     * 生成CourseData对应的ListView
     */
    private JFXListView<SessionController.ClassesDateLabel> generateCourseDataListView(CourseData courseData, HBox ops) {
        JFXListView<SessionController.ClassesDateLabel> courseDataListView = new JFXListView<>();
        //单选
        courseDataListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        CourseType courseType = courseData.getCourseType();
        SessionController.CourseDateLabel courseInfoLabel = new SessionController.CourseDateLabel(courseData, courseType.getCourse().getTextTitle() + "  "
                + courseType.getCourseId() + "  " + courseType.getScore() + "分 " + courseType.getMajor()
                + "  " + courseType.getRemark());
        courseInfoLabel.setFont(Font.font("msyh", 17));

        courseInfoLabel.setOnMouseClicked(event -> {
            JFXButton delButton = new JFXButton("删除课程及其所有班级"),
                    viewCourseSyllabus = new JFXButton("教学大纲"),
                    viewCourseCalendar = new JFXButton("教学日历");
            delButton.setStyle("-fx-padding: 0.7em 0.57em;\n" +
                    "-fx-font-family: msyh;\n" +
                    "-fx-font-size: 16;\n" +
                    "-fx-font-weight: bold;\n" +
                    "-jfx-button-type: RAISED;\n" +
                    "-fx-background-color: rgb(77, 102, 204);\n" +
                    "-fx-min-width: 140;\n" +
                    "-fx-text-fill: WHITE;");
            viewCourseCalendar.setStyle("-fx-padding: 0.7em 0.57em;\n" +
                    "-fx-font-family: msyh;\n" +
                    "-fx-font-size: 16;\n" +
                    "-fx-font-weight: bold;\n" +
                    "-jfx-button-type: RAISED;\n" +
                    "-fx-background-color: rgb(77, 102, 204);\n" +
                    "-fx-min-width: 92;\n" +
                    "-fx-text-fill: WHITE;");
            viewCourseSyllabus.setStyle("-fx-padding: 0.7em 0.57em;\n" +
                    "-fx-font-family: msyh;\n" +
                    "-fx-font-size: 16;\n" +
                    "-fx-font-weight: bold;\n" +
                    "-jfx-button-type: RAISED;\n" +
                    "-fx-background-color: rgb(77, 102, 204);\n" +
                    "-fx-min-width: 92;\n" +
                    "-fx-text-fill: WHITE;");

            delButton.setOnMouseClicked(event1 -> {
                JFXDialogLayout dialogLayout = new JFXDialogLayout();
                Label heading = new Label("是否确认删除课程" + courseType.getCourse().getTextTitle()
                        + "及其所有选课班级?");
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

                cancelBtn.setOnMouseClicked(event2 -> delCourseDialog.close());
                confirmBtn.setOnMouseClicked((e) ->{
                    CourseClassRequestQueue.removeCourseData(courseData.getCourseId());
                    courseListViewFactory();
                    delCourseDialog.close();
                    infoSnackBar.fireEvent(new JFXSnackbar.SnackbarEvent("删除课程" + courseData.getCourseType().getCourse()
                            .getTextTitle() + "及其班级成功",
                            "了解", 2000, false,
                            b -> infoSnackBar.close()));
                });

                delCourseDialog.show();
            });

            viewCourseSyllabus.setOnMouseClicked(event1 ->
                ViewSelectedCoursesStage.loadURL(courseType.getCourseSyllabus(), SettingData
                        .getDhuCurrentUser().getUserCookie()));

            viewCourseCalendar.setOnMouseClicked(event1 -> ViewSelectedCoursesStage.loadURL(courseType.getCourseCalendar(),
                    SettingData.getDhuCurrentUser().getUserCookie()));

            ops.getChildren().remove(0, ops.getChildren().size());
            ops.getChildren().addAll(delButton, viewCourseSyllabus, viewCourseCalendar);
        });

        //courseInfoLabel.graphicProperty().bind(courseData.getStatusProperty());
        courseDataListView.setGroupnode(courseInfoLabel);

        ObservableList<SessionController.ClassesDateLabel> classDataList = FXCollections.observableArrayList();
        for (ClassesData classesData : courseData.getClassesDataMap().values()) {
            Course course = classesData.getCourse();

            SessionController.ClassesDateLabel classDataLabel = new SessionController.ClassesDateLabel(classesData, course.getTeacher().getTextTitle() + "  "
                    + course.getCourseNo() + " 组班号" + course.getClassNo() + "  " + course.getPlaces()[0]);
            classDataLabel.setFont(Font.font("msyh", 16));
            classDataList.add(classDataLabel);
        }

        courseDataListView.setItems(classDataList);

        courseDataListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            JFXButton delButton = new JFXButton("删除该选课班级"),
                    viewTeacher = new JFXButton("教师信息");

            delButton.setStyle("-fx-padding: 0.7em 0.57em;\n" +
                    "-fx-font-family: msyh;\n" +
                    "-fx-font-size: 16;\n" +
                    "-fx-font-weight: bold;\n" +
                    "-jfx-button-type: RAISED;\n" +
                    "-fx-background-color: rgb(77, 102, 204);\n" +
                    "-fx-min-width: 130;\n" +
                    "-fx-text-fill: WHITE;");
            viewTeacher.setStyle("-fx-padding: 0.7em 0.57em;\n" +
                    "-fx-font-family: msyh;\n" +
                    "-fx-font-size: 16;\n" +
                    "-fx-font-weight: bold;\n" +
                    "-jfx-button-type: RAISED;\n" +
                    "-fx-background-color: rgb(77, 102, 204);\n" +
                    "-fx-min-width: 92;\n" +
                    "-fx-text-fill: WHITE;");

            delButton.setOnMouseClicked(event -> {
                JFXDialogLayout dialogLayout = new JFXDialogLayout();
                Label heading = new Label("是否确认删除班级" + newValue.getClassesDate().getCourse()
                        .getCourse().getTextTitle()
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

                cancelBtn.setOnMouseClicked(event1 -> delCourseDialog.close());
                confirmBtn.setOnMouseClicked((e) ->{
                    newValue.getClassesDate().getCourseData().removeClassData(newValue.getClassesDate()
                            .getCourse().getCourseNo());
                    courseListViewFactory();
                    delCourseDialog.close();
                    infoSnackBar.fireEvent(new JFXSnackbar.SnackbarEvent("删除班级" + newValue.getClassesDate()
                            .getCourse().getCourse().getTextTitle() + "成功",
                            "了解", 2000, false,
                            b -> infoSnackBar.close()));
                });

                delCourseDialog.show();
            });

            viewTeacher.setOnMouseClicked(event -> ViewSelectedCoursesStage.loadURL(newValue.getClassesDate()
                .getCourse().getTeacher().getLink(), SettingData.getDhuCurrentUser().getUserCookie()));

            ops.getChildren().remove(0, ops.getChildren().size());
            ops.getChildren().addAll(delButton, viewTeacher);
        });

        return courseDataListView;
    }

    /**
     * 生成(更新)CoursesListView
     */
    private VBox courseListViewFactory() {
        //操作按钮
        HBox opsBox, otherButtons;
        if (opsBoxRoot.getChildren().size() >= 2) {
            opsBox = (HBox) opsBoxRoot.getChildren().get(1);
            otherButtons = (HBox) opsBox.getChildren().get(2);
        } else {
            opsBox = new HBox(10.0d);

            //智能导入课程按钮
            JFXButton intelliImport = new JFXButton("智能导入课程");
            intelliImport.setStyle("-fx-padding: 0.7em 0.57em;\n" +
                    "-fx-font-family: msyh;\n" +
                    "-fx-font-size: 16;\n" +
                    "-fx-font-weight: bold;\n" +
                    "-jfx-button-type: RAISED;\n" +
                    "-fx-background-color: rgb(77, 102, 204);\n" +
                    "-fx-min-width: 130;\n" +
                    "-fx-text-fill: WHITE;");
            intelliImport.setOnMouseClicked(event -> {
                // TODO
            });

            JFXButton manualImport = new JFXButton("手动导入课程");
            manualImport.setStyle("-fx-padding: 0.7em 0.57em;\n" +
                    "-fx-font-family: msyh;\n" +
                    "-fx-font-size: 16;\n" +
                    "-fx-font-weight: bold;\n" +
                    "-jfx-button-type: RAISED;\n" +
                    "-fx-background-color: rgb(77, 102, 204);\n" +
                    "-fx-min-width: 130;\n" +
                    "-fx-text-fill: WHITE;");
            manualImport.setOnMouseClicked(event -> {
                // TODO
            });

            //其他目标-special的按钮
            otherButtons = new HBox(10.0d);
            opsBox.getChildren().addAll(intelliImport, manualImport, otherButtons);

            opsBoxRoot.getChildren().addAll(opsBox);
        }

        //课程列表
        AnchorPane courseListAnchorPane = new AnchorPane();
        //ListView Root
        JFXListView<JFXListView<SessionController.ClassesDateLabel>> listViewRoot = new JFXListView<>();
        AnchorPane.setLeftAnchor(listViewRoot, 15.0d);
        AnchorPane.setRightAnchor(listViewRoot, 15.0d);
        AnchorPane.setBottomAnchor(listViewRoot, 200.0d);
        AnchorPane.setTopAnchor(listViewRoot, 20.0d);

        courseListAnchorPane.getChildren().add(listViewRoot);

        CourseClassRequestQueue courseClassRequestQueue =
                SettingData.getRequestQueue();

        //存储所有CourseData的sub List View的List
        ObservableList<JFXListView<SessionController.ClassesDateLabel>> courseDataListViews = FXCollections.observableArrayList();

        if (courseClassRequestQueue != null) {
            for (CourseData courseData : CourseClassRequestQueue.getCoursesDataMap().values()) {
                courseDataListViews.add(generateCourseDataListView(courseData, otherButtons));
            }
        } else {
            JFXListView<SessionController.ClassesDateLabel> noItem = new JFXListView<>();
            noItem.setPrefHeight(40);
            Label noItemLabel = new Label("任务队列还是空空如也 ~");
            noItem.setGroupnode(noItemLabel);
            noItemLabel.setFont(Font.font("msyh", 18));
            noItem.setDepth(2);
            courseDataListViews.add(noItem);
        }

        listViewRoot.setDepth(4);
        //单选
        listViewRoot.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        listViewRoot.setItems(courseDataListViews);
        listViewRoot.setExpanded(true);
        listViewRoot.setMinHeight(350);

        //Search field
        AnchorPane searchPane = new AnchorPane();
        JFXTextField searchField = new JFXTextField();
        searchField.setPromptText("搜索选课班级...");
        searchField.setPrefWidth(35);
        AnchorPane.setRightAnchor(searchField, 150.0d);
        searchField.setFont(Font.font(18));
        searchPane.getChildren().add(searchField);

        //Search
        searchField.textProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.equals(""))
                return;

            listViewRoot.setExpanded(true);
            for (JFXListView<SessionController.ClassesDateLabel> subListView : listViewRoot.getItems()) {
                int index = 0;
                for (SessionController.ClassesDateLabel classesDate : subListView.getItems()) {
                    if (classesDate.getText().contains(newValue)) {
                        subListView.setExpanded(true);
                        subListView.getSelectionModel().select(index);
                        return;
                    }
                    index++;
                }
            }
        }));

        //添加到或者更新VBoxRoot
        if (vboxRoot.getChildren().size() >= 4) {
            vboxRoot.getChildren().remove(2, 4);
        }

        vboxRoot.getChildren().add(2, searchPane);
        vboxRoot.getChildren().add(3, courseListAnchorPane);

        return vboxRoot;
    }
}///~
