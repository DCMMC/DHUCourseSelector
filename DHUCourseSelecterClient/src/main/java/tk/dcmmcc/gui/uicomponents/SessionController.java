package tk.dcmmcc.gui.uicomponents;

import com.jfoenix.controls.*;
import com.jfoenix.effects.JFXDepthManager;
import com.jfoenix.svg.SVGGlyph;
import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.TileBuilder;
import io.datafx.controller.ViewController;
import io.datafx.controller.flow.Flow;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.FlowHandler;
import io.datafx.controller.flow.context.FXMLViewFlowContext;
import io.datafx.controller.flow.context.ViewFlowContext;
import io.datafx.controller.util.VetoException;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import tk.dcmmcc.Course;
import tk.dcmmcc.CourseType;
import tk.dcmmcc.datafx.ClassesData;
import tk.dcmmcc.datafx.CourseClassRequestQueue;
import tk.dcmmcc.datafx.CourseData;
import tk.dcmmcc.datafx.SettingData;
import tk.dcmmcc.utils.ExceptionDialog;
import tk.dcmmcc.utils.LoggerUtil;
import javax.annotation.PostConstruct;
import java.time.*;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.logging.Logger;

/**
 * TODO 完成课程控制按钮
 * TODO 选课状态图标大小和自动更换都没有
 * 选课页面
 * Created by DCMMC on 2017/9/5.
 */
@ViewController(value = "/fxml/ui/Session.fxml", title = "Select Courses Session")
public class SessionController {
    @FXMLViewFlowContext
    private ViewFlowContext context;
    @FXML
    private StackPane root;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private VBox vboxRoot;
    private static Logger logger = Logger.getLogger("DHUCourseSelecter");

    static {
        LoggerUtil.initLogger(logger);
    }

    /**
     * init fxml when loaded.
     */
    @PostConstruct
    public void init() throws Exception {
        Objects.requireNonNull(context, "context");

        //保证前面有点空白
        vboxRoot.setPadding(new Insets(15, 5, 10, 5));
        StackPane accountInfoPane = new StackPane();
        accountInfoPane.setMinHeight(45);
        JFXDepthManager.setDepth(accountInfoPane, 4);
        accountInfoPane.setStyle("-fx-background-radius: 15 15 15 15; -fx-background-color: #4CAF50");

        /*
        //用这个的时候出现了一个奇怪的bug: 点击这个accountInfo结果我的那些tiles的字体大小就变了...
        //copyable label-like text fields
        TextField accountInfo = new TextField();
        accountInfo.setAlignment(Pos.CENTER);
        accountInfo.setStyle("-fx-text-fill: white; -fx-alignment: baseline-left; -fx-font-weight: bolder; -fx-font-size: 24;" +
                "-fx-background-color: transparent;-fx-background-insets: 0px;");
        */
        //然而用Label还是有这个bug... 点击一下close按钮就会复现
        Label accountInfo = new Label();
        accountInfo.setAlignment(Pos.CENTER);
        accountInfo.setStyle("-fx-text-fill: white; -fx-alignment: baseline-left; -fx-font-weight: bolder;" +
                "-fx-font-size: 24;");

        //绑定info
        accountInfo.textProperty().bind(SettingData.getCurrentInfoProperty());

        accountInfoPane.getChildren().addAll(accountInfo);

        vboxRoot.getChildren().addAll(accountInfoPane);

        /* Control Tiles */
        FlowPane tilesFlowPane = new FlowPane(20, 20);
        //Clock Tile
        Tile clockTile = TileBuilder.create()
                .skinType(Tile.SkinType.CLOCK)
                .prefSize(200, 200)
                .title("当前时间")
                .dateVisible(true)
                .locale(Locale.CHINA)
                .running(true)
                .build();
        //已选课程数目
        Tile selectedCoursesNumTile = TileBuilder.create()
                .skinType(Tile.SkinType.NUMBER)
                .prefSize(200, 200)
                .title("成功录取课程数")
                .unit("个")
                .value(CourseClassRequestQueue.getSelectedCoursesNumberProperty().get())
                .textVisible(false)
                .build();

        //绑定
        selectedCoursesNumTile.valueProperty().bind(CourseClassRequestQueue.getSelectedCoursesNumberProperty());

        //队列中的课程数目
        Tile queueCoursesNumTile = TileBuilder.create()
                .skinType(Tile.SkinType.NUMBER)
                .prefSize(200, 200)
                .title("任务队列中剩余的课程数")
                .unit("个")
                .value(CourseClassRequestQueue.getQueueCoursesNumberProperty().get())
                .textVisible(false)
                .build();

        //绑定
        queueCoursesNumTile.valueProperty().bind(CourseClassRequestQueue.getQueueCoursesNumberProperty());

        //总的抢课次数
        Tile courseRequestCountTile = TileBuilder.create()
                .skinType(Tile.SkinType.NUMBER)
                .prefSize(200, 200)
                .title("抢课次数")
                .unit("次")
                .value(CourseClassRequestQueue.getCourseRequestCountProperty().get())
                .textVisible(false)
                .build();

        //绑定
        courseRequestCountTile.valueProperty().bind(CourseClassRequestQueue.getCourseRequestCountProperty());

        JFXDepthManager.setDepth(clockTile, 4);
        JFXDepthManager.setDepth(selectedCoursesNumTile, 4);
        JFXDepthManager.setDepth(queueCoursesNumTile, 4);
        JFXDepthManager.setDepth(courseRequestCountTile, 4);
        tilesFlowPane.getChildren().addAll(clockTile, selectedCoursesNumTile, queueCoursesNumTile,
                courseRequestCountTile);
        tilesFlowPane.setPadding(new Insets(10, 5, 5, 10));
        vboxRoot.getChildren().addAll(tilesFlowPane);

        //课程队列概览
        AnchorPane anchorPane = new AnchorPane();

        Label courseQueueOverview = new Label("选课队列概览");
        courseQueueOverview.setFont(Font.font("msyh", FontWeight.BOLD, 22));
        courseQueueOverview.setAlignment(Pos.TOP_LEFT);
        courseQueueOverview.setTextFill(Color.web("#303F9F"));
        AnchorPane.setLeftAnchor(courseQueueOverview, 15.0d);

        //辣鸡JFXButton只能通过设置Style来改变style, 用那些方法设置的根本不生效...
        JFXButton editCourseClassRequestQueue = new JFXButton("修改选课队列");
        editCourseClassRequestQueue.setStyle("-fx-padding: 0.7em 0.57em;\n" +
                "-fx-font-family: msyh;\n" +
                "-fx-font-size: 16;\n" +
                "-fx-font-weight: bold;\n" +
                "-jfx-button-type: RAISED;\n" +
                "-fx-background-color: rgb(77, 102, 204);\n" +
                "-fx-min-width: 130;\n" +
                "-fx-text-fill: WHITE;");
        editCourseClassRequestQueue.setOnMouseClicked((e) -> {
            Flow contentFlow = (Flow) context.getRegisteredObject("ContentFlow");
            //注册修改选课队列界面
            contentFlow.withGlobalLink(editCourseClassRequestQueue.getId(),
                    EditCourseClassRequestQueueController.class);
            FlowHandler contentFlowHandler = (FlowHandler) context.getRegisteredObject("ContentFlowHandler");
            try {
                ((BorderPane)(((StackPane)context.getRegisteredObject("MainRoot")).getChildren().get(0)))
                        .setBottom(null);
                //加载修改选课队列界面
                contentFlowHandler.handle(editCourseClassRequestQueue.getId());
            } catch (VetoException | FlowException vfe) {
                //转换界面Controller的时候发生了异常
                ExceptionDialog.launch(vfe, "严重错误!", "转换界面Controller的时候发生了异常");
                logger.warning("转换界面Controller的时候发生了异常");
                vfe.printStackTrace();
            }
        });
        //没有登录或者已经开始的话都不能点击
        editCourseClassRequestQueue.disableProperty().bind(CourseClassRequestQueue.getStartedProperty()
            .or(SettingData.getLoginFlagProperty().not()));

        AnchorPane.setRightAnchor(editCourseClassRequestQueue, 15.0d);

        //Search field
        JFXTextField searchField = new JFXTextField();
        searchField.setPromptText("搜索...");
        searchField.setPrefWidth(40);
        AnchorPane.setRightAnchor(searchField, 200.0d);
        searchField.setFont(Font.font(22));
        //没有登录不能点
        searchField.disableProperty().bind(SettingData.getLoginFlagProperty().not());
        // TODO Search

        anchorPane.getChildren().addAll(courseQueueOverview, searchField, editCourseClassRequestQueue);

        vboxRoot.getChildren().addAll(anchorPane);

        //课程列表
        AnchorPane courseListAnchorPane = new AnchorPane();
        //ListView Root
        JFXListView<JFXListView<ClassesDateLabel>> listViewRoot = new JFXListView<>();
        AnchorPane.setLeftAnchor(listViewRoot, 15.0d);
        AnchorPane.setRightAnchor(listViewRoot, 15.0d);
        AnchorPane.setBottomAnchor(listViewRoot, 200.0d);
        AnchorPane.setTopAnchor(listViewRoot, 20.0d);

        courseListAnchorPane.getChildren().addAll(listViewRoot);

        vboxRoot.getChildren().addAll(listViewRoot);

        // FIXME debug test
        SettingData.getLoginFlagProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue) {
                /*
                try {
                    DoubleLinkedList<CourseData> courseDataList = new DoubleLinkedList<>();
                    courseDataList.addLast(new CourseData(CourseType.searchCourse("LINUX系统",
                                    SettingData.getDhuCurrentUser().getUserCookie())[0], new ClassesData(Course.getCourseFromCommonSearch(
                                    CourseType.searchCourse("LINUX系统",
                                            SettingData.getDhuCurrentUser().getUserCookie())[0],
                                    SettingData.getDhuCurrentUser().getUserCookie())[0])))
                        .addLast(new CourseData(CourseType.searchCourse("131441",
                                SettingData.getDhuCurrentUser().getUserCookie())[0], new ClassesData(Course.getCourseFromCommonSearch(
                                CourseType.searchCourse("131441",
                                        SettingData.getDhuCurrentUser().getUserCookie())[0],
                                SettingData.getDhuCurrentUser().getUserCookie())[0])));
                    CourseClassRequestQueue queue = new CourseClassRequestQueue(courseDataList.toArray());

                    SettingData.setRequestQueue(queue);
                } catch (IllegalCourseException ie) {
                    //....
                }
                */
            }
        }));

        CourseClassRequestQueue courseClassRequestQueue =
                SettingData.getRequestQueue();

        //存储所有CourseData的sub List View的List
        ObservableList<JFXListView<ClassesDateLabel>> courseDataListViews = FXCollections.observableArrayList();

        if (courseClassRequestQueue != null) {
            for (CourseData courseData : courseClassRequestQueue.getCoursesDataMap().values()) {
                courseDataListViews.add(generateCourseDataListView(courseData));
            }
        } else {
            JFXListView<ClassesDateLabel> noItem = new JFXListView<>();
            noItem.setPrefHeight(40);
            Label noItemLabel = new Label("任务队列还是空空如也 ~ , 请点击左上角按钮修改选课队列 ~ ");
            noItem.setGroupnode(noItemLabel);
            noItemLabel.setFont(Font.font("msyh", 18));
            noItem.setDepth(2);
            courseDataListViews.add(noItem);
        }

        listViewRoot.setDepth(4);
        listViewRoot.setItems(courseDataListViews);
        listViewRoot.setExpanded(true);
        listViewRoot.setMinHeight(350);

        /* 功能按钮 */
        // TODO
        //选择的课程或者班级的有关操作
        HBox operateCurrentSelected = new HBox(20);
        operateCurrentSelected.setPrefHeight(40);
        operateCurrentSelected.setAlignment(Pos.CENTER);

        listViewRoot.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            operateCurrentSelected.getChildren().remove(0, operateCurrentSelected.getChildren().size());
            JFXButton delBtn = new JFXButton("中止该课程下面的所有班级线程");
            delBtn.setStyle("-fx-padding: 0.7em 0.57em;\n" +
                    "-fx-font-family: msyh;\n" +
                    "-fx-font-size: 17;\n" +
                    "-fx-font-weight: bold;\n" +
                    "-jfx-button-type: RAISED;\n" +
                    "-fx-background-color: rgb(77, 102, 204);\n" +
                    "-fx-min-width: 200;\n" +
                    "-fx-text-fill: WHITE;");
            delBtn.setOnMouseClicked((event -> ((CourseDateLabel)newValue.getGroupnode())
                    .getCourseData()
                    .cancelAll()));
        }));

        for (JFXListView<ClassesDateLabel> listView : listViewRoot.getItems()) {
            listView.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
                operateCurrentSelected.getChildren().remove(0, operateCurrentSelected.getChildren().size());
                JFXButton delBtn = new JFXButton("中止该选课班级线程");
                delBtn.setStyle("-fx-padding: 0.7em 0.57em;\n" +
                        "-fx-font-family: msyh;\n" +
                        "-fx-font-size: 17;\n" +
                        "-fx-font-weight: bold;\n" +
                        "-jfx-button-type: RAISED;\n" +
                        "-fx-background-color: rgb(77, 102, 204);\n" +
                        "-fx-min-width: 200;\n" +
                        "-fx-text-fill: WHITE;");
                delBtn.setOnMouseClicked((event -> newValue.getClassesDate().cancel()));
            }));
        }

        //概览界面主要的几个功能按钮

        AnchorPane mainButtonsPane = new AnchorPane();
        //放到bottom上面
        ((BorderPane)((StackPane)context.getRegisteredObject("MainRoot")).getChildren().get(0))
                .setBottom(mainButtonsPane);

        //查看当前已选课程
        JFXButton viewSelectedCourses = new JFXButton("查看已选课程");
        viewSelectedCourses.setStyle("-fx-padding: 0.7em 0.57em;\n" +
                "-fx-font-family: msyh;\n" +
                "-fx-font-size: 18;\n" +
                "-fx-font-weight: bold;\n" +
                "-jfx-button-type: RAISED;\n" +
                "-fx-background-color: rgb(77, 102, 204);\n" +
                "-fx-min-width: 150;\n" +
                "-fx-text-fill: WHITE;");

        //查看已选课程
        viewSelectedCourses.setOnMouseClicked((event -> {
            ViewSelectedCoursesStage.launch(new Stage(), SettingData.getDhuCurrentUser());;
        }));

        viewSelectedCourses.disableProperty().bind(CourseClassRequestQueue.getStartedProperty()
            .or(SettingData.getLoginFlagProperty().not().and(SettingData.getJwStopSelectCourses().not())));
        AnchorPane.setLeftAnchor(viewSelectedCourses, 30.0d);
        AnchorPane.setBottomAnchor(viewSelectedCourses, 20.0d);

        //查看当前已选课程
        JFXButton viewLogs = new JFXButton("查看程序日志");
        viewLogs.setStyle("-fx-padding: 0.7em 0.57em;\n" +
                "-fx-font-family: msyh;\n" +
                "-fx-font-size: 18;\n" +
                "-fx-font-weight: bold;\n" +
                "-jfx-button-type: RAISED;\n" +
                "-fx-background-color: rgb(77, 102, 204);\n" +
                "-fx-min-width: 150;\n" +
                "-fx-text-fill: WHITE;");

        //test log
        //logger.info("test info");
        //logger.severe("test severe");

        viewLogs.setOnMouseClicked((event -> {
            //将窗口运行在另外一个线程
            Platform.runLater(() -> {
                try {
                    new LoggerUtil().start(new Stage());
                } catch (Exception e) {
                    //严重错误
                    logger.severe("打开log查看窗口的时候发生严重错误.");
                    e.printStackTrace();
                    ExceptionDialog.launch(e, "发生严重错误", "打开log查看窗口(LoggerUtil)" +
                            "的时候发生严重错误.");
                }});
        }));
        AnchorPane.setLeftAnchor(viewLogs, 200.0d);
        AnchorPane.setBottomAnchor(viewLogs, 20.0d);

        HBox startHBox = new HBox(15);
        AnchorPane.setBottomAnchor(startHBox, 15.0d);
        AnchorPane.setRightAnchor(startHBox, 320.0d);

        JFXListView<JFXButton> startListView = new JFXListView<>();

        JFXButton start = new JFXButton("选课");
        start.setStyle("-fx-padding: 0.7em 0.57em;\n" +
                "-fx-font-family: msyh;\n" +
                "-fx-font-size: 16;\n" +
                "-fx-font-weight: bold;\n" +
                "-jfx-button-type: RAISED;\n" +
                "-fx-background-color: rgb(77, 102, 204);\n" +
                "-fx-min-width: 150;\n" +
                "-fx-text-fill: WHITE;");
        startListView.setGroupnode(start);

        JFXButton startNow = new JFXButton("现在开始选课");
        startNow.setStyle("-fx-padding: 0.7em 0.57em;\n" +
                "-fx-font-family: msyh;\n" +
                "-fx-font-size: 16;\n" +
                "-fx-font-weight: bold;\n" +
                "-jfx-button-type: RAISED;\n" +
                "-fx-background-color: rgb(77, 102, 204);\n" +
                "-fx-min-width: 150;\n" +
                "-fx-text-fill: WHITE;");

        startListView.disableProperty().bind(CourseClassRequestQueue.getStartedProperty());
        JFXTimePicker timePicker = new JFXTimePicker(LocalTime.now());
        timePicker.setDialogParent(root);
        JFXButton startDelay = new JFXButton("指定时间开始");
        startDelay.setStyle("-fx-padding: 0.7em 0.57em;\n" +
                "-fx-font-family: msyh;\n" +
                "-fx-font-size: 16;\n" +
                "-fx-font-weight: bold;\n" +
                "-jfx-button-type: RAISED;\n" +
                "-fx-background-color: rgb(77, 102, 204);\n" +
                "-fx-min-width: 150;\n" +
                "-fx-text-fill: WHITE;");

        JFXListView<JFXListView> startListViewRoot = new JFXListView<>();
        startListViewRoot.disableProperty().bind(CourseClassRequestQueue.getStartedProperty()
                .or(SettingData.getLoginFlagProperty().not()));
        startListViewRoot.setDepth(4);
        startListViewRoot.setPrefHeight(80);
        startListViewRoot.setPrefWidth(210);
        ObservableList<JFXListView> listViews = FXCollections.observableArrayList();
        listViews.addAll(startListView);
        startListViewRoot.setItems(listViews);
        startHBox.getChildren().addAll(startListViewRoot);

        timePicker.setPrefHeight(1);
        timePicker.setPrefWidth(1);
        timePicker.setEditable(false);
        //妈的 java的DateParse真他妈的垃圾, 去你妈的Locale
        timePicker.setConverter(new StringConverter<LocalTime>() {
            @Override
            public String toString(LocalTime object) {
                return object.format(new DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .appendPattern("h:mm a")
                        .toFormatter(Locale.US));
            }

            @Override
            public LocalTime fromString(String string) {
                return LocalTime.parse(string, new DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .appendPattern("h:mm a")
                        .toFormatter(Locale.US));
            }
        });

        //debug
        //logger.info(LocalTime.parse("2:19 AM", new DateTimeFormatterBuilder()
        //        .parseCaseInsensitive()
        //        .appendPattern("h:mm a")
        //        .toFormatter(Locale.US)).toString());

        // timePicker.hide();
        startDelay.setGraphic(timePicker);
        startDelay.setContentDisplay(ContentDisplay.RIGHT);

        startNow.setOnMouseClicked((event -> {
            if (courseClassRequestQueue != null)
                courseClassRequestQueue.startAll();
        }));
        startDelay.setOnMouseClicked((event -> {
            if (courseClassRequestQueue == null)
                return;

            timePicker.show();

            timePicker.showingProperty().addListener(((observable, oldValue, newValue) -> {
                if (!newValue) {
                    //debug
                    //logger.info("debug...");

                    //get Date
                    LocalDate nowDate = LocalDate.now();
                    Date now = new Date();
                    Date startDate = Date.from(timePicker.getValue().atDate(nowDate).
                            atZone(ZoneId.systemDefault()).toInstant());

                    //debug
                    // startDate = new Date();
                    //logger.info("now: " + now.getTime() + " start: " + startDate.getTime());

                    if (now.getTime() >= startDate.getTime() - 1000) {
                        //设置的时间有问题, 不能早于当前时间的后一秒
                        JFXSnackbar wrongTime = new JFXSnackbar();
                        wrongTime.registerSnackbarContainer(root);
                        wrongTime.fireEvent(new JFXSnackbar.SnackbarEvent("你输入的时间有问题! 不能早于当前时间的后一秒.",
                                "了解", 2000, false,
                                b -> wrongTime.close()));
                        return;
                    }

                    courseClassRequestQueue.startAll(startDate);

                    HBox remainSecondsHBox = new HBox(2);
                    remainSecondsHBox.setPrefHeight(50);
                    remainSecondsHBox.setPrefWidth(150);
                    Label remainSeconds = new Label();
                    remainSeconds.textProperty().bind(courseClassRequestQueue.getStartAfterSecondsProperty()
                            .asString());
                    remainSeconds.setFont(Font.font("msyh", 22));
                    Label prefix = new Label("程序将会在");
                    prefix.setFont(Font.font("msyh", 18));
                    Label suffix = new Label("秒后开始选课.");
                    suffix.setFont(Font.font("msyh", 18));
                    remainSecondsHBox.getChildren().addAll(prefix, remainSeconds, suffix);
                    startHBox.getChildren().add(remainSecondsHBox);
                    courseClassRequestQueue.getStartAfterSecondsProperty().addListener(((observable1, oldValue1, newValue1) -> {
                        if (newValue1.equals(0))
                            startHBox.getChildren().remove(1);
                    }));
                }
            }));
        }));

        startListView.setOnMouseClicked((e) -> {
            JFXButton selectedLabel = startListView.getSelectionModel().getSelectedItem();

            if (selectedLabel.getText().equals("现在开始选课")) {
                if (courseClassRequestQueue != null)
                    courseClassRequestQueue.startAll();
            } else {
                if (courseClassRequestQueue == null)
                    return;

                timePicker.show();

                timePicker.showingProperty().addListener(((observable, oldValue, newValue) -> {
                    if (!newValue) {
                        //debug
                        //logger.info("debug...");

                        //get Date
                        LocalDate nowDate = LocalDate.now();
                        Date now = new Date();
                        Date startDate = Date.from(timePicker.getValue().atDate(nowDate).
                                atZone(ZoneId.systemDefault()).toInstant());

                        //debug
                        // startDate = new Date();
                        //logger.info("now: " + now.getTime() + " start: " + startDate.getTime());

                        if (now.getTime() >= startDate.getTime() - 1000) {
                            //设置的时间有问题, 不能早于当前时间的后一秒
                            JFXSnackbar wrongTime = new JFXSnackbar();
                            wrongTime.registerSnackbarContainer(root);
                            wrongTime.fireEvent(new JFXSnackbar.SnackbarEvent("你输入的时间有问题! 不能早于当前时间的后一秒.",
                                    "了解", 2000, false,
                                    b -> wrongTime.close()));
                            return;
                        }

                        courseClassRequestQueue.startAll(startDate);

                        HBox remainSecondsHBox = new HBox(2);
                        remainSecondsHBox.setPrefHeight(50);
                        remainSecondsHBox.setPrefWidth(400);
                        Label remainSeconds = new Label();
                        courseClassRequestQueue.getStartAfterSecondsProperty().addListener(((observable1, oldValue1, newValue1) -> {
                            remainSeconds.setText(newValue1.intValue() + "");
                        }));
                        remainSeconds.setFont(Font.font("msyh", 22));
                        Label prefix = new Label("程序将会在");
                        prefix.setPrefWidth(100);
                        prefix.setFont(Font.font("msyh", 18));
                        Label suffix = new Label("秒后开始选课.");
                        suffix.setPrefWidth(120);
                        suffix.setFont(Font.font("msyh", 18));
                        remainSecondsHBox.getChildren().addAll(prefix, remainSeconds, suffix);
                        startHBox.getChildren().add(remainSecondsHBox);
                        courseClassRequestQueue.getStartAfterSecondsProperty().addListener(((observable1, oldValue1, newValue1) -> {
                            if (newValue1.equals(0))
                                startHBox.getChildren().remove(1);
                        }));
                    }
                }));
            }
        });


        ObservableList<JFXButton> startButtonList = FXCollections.observableArrayList();
        startButtonList.addAll(startNow, startDelay);
        startListView.setItems(startButtonList);

        mainButtonsPane.getChildren().addAll(viewSelectedCourses, viewLogs, startHBox);

        //我已经把mainButtonsPane放在BorderPane的Bottom那里去了
        vboxRoot.getChildren().addAll(operateCurrentSelected);
    }

    private JFXListView<ClassesDateLabel> generateCourseDataListView(CourseData courseData) {
        JFXListView<ClassesDateLabel> courseDataListView = new JFXListView<>();

        CourseType courseType = courseData.getCourseType();
        CourseDateLabel courseInfoLabel = new CourseDateLabel(courseData, courseType.getCourse().getTextTitle() + "  "
            + courseType.getCourseId() + "  " + courseType.getScore() + "  " + courseType.getMajor()
            + "  " + courseType.getRemark());
        courseInfoLabel.setFont(Font.font("msyh", 17));

        postProcessLabel(courseInfoLabel, courseData.getStatus());
        //courseInfoLabel.graphicProperty().bind(courseData.getStatusProperty());
        courseDataListView.setGroupnode(courseInfoLabel);

        ObservableList<ClassesDateLabel> classDataList = FXCollections.observableArrayList();
        for (ClassesData classesData : courseData.getClassesDataMap().values()) {
            Course course = classesData.getCourse();

            ClassesDateLabel classDataLabel = new ClassesDateLabel(classesData, course.getTeacher().getTextTitle() + "  "
                + course.getCourseNo() + " 组班号" + course.getClassNo() + "  " + course.getPlaces()[0]);
            classDataLabel.setFont(Font.font("msyh", 16));

            postProcessLabel(classDataLabel, classesData.getStatus());
            //classDataLabel.graphicProperty().bind(classesData.getStatusProperty());
            classDataList.add(classDataLabel);
        }

        courseDataListView.setItems(classDataList);

        return courseDataListView;
    }

    /**
     * Label that contain Course
     */
    private static class ClassesDateLabel extends Label {
        private ClassesData classesData;

        ClassesDateLabel(ClassesData classesData, String text) {
            super(text);
            this.classesData = classesData;
        }

        SessionController.ClassesDateLabel setCourse(Course course) {
            this.classesData = classesData;
            return this;
        }

        ClassesData getClassesDate() {
            return classesData;
        }
    }

    /**
     * Label that contain CourseType
     */
    private static class CourseDateLabel extends Label {
        private CourseData courseData;

        CourseDateLabel(CourseData courseData, String text) {
            super(text);
            this.courseData = courseData;
        }

        SessionController.CourseDateLabel setCourse(CourseData courseData) {
            this.courseData = courseData;
            return this;
        }

        CourseData getCourseData() {
            return courseData;
        }
    }

    private void postProcessLabel(Label label, IntegerProperty status) {
        label.setGraphic(getWait());
        status.addListener(((observable, oldValue, newValue) -> {
            switch (newValue.intValue()) {
                case 1 : label.setGraphic(getWait()); break;
                case 2 : label.setGraphic(getLoading()); break;
                case 3 : label.setGraphic(getError()); break;
                case 4 : label.setGraphic(getSuccess()); break;
                default: // TODO
                    break;
            }
        }));
    }

    private JFXSpinner getLoading() {
        //选课中
        JFXSpinner loading = new JFXSpinner();
        loading.setPrefSize(32, 32);
        return loading;
    }

    private SVGGlyph getWait() {
        //还没开始
        SVGGlyph wait = new SVGGlyph(0, "wait", "M512 1024a512 " +
                "512 0 1 1 512-512 512 512 0 0 1-512 512z m189.6704-362.7264L486.4 556.928V320a38.4 38.4" +
                " 0 0 0-76.8 0v256a37.9136 37.9136 0 0 0 13.1328 28.416 37.1456 37.1456 0 0 0 15.0784 " +
                "14.3104l230.4 111.6416a38.4 38.4 0 1 0 33.4592-69.0944z", Color.web("#d4237a"));
        wait.setPrefSize(32, 32);
        return wait;
    }

    private SVGGlyph getError() {
        //被中止
        SVGGlyph error = new SVGGlyph(0, "error", "M512 0a512 512 0 1 0 0 " +
                "1024A512 512 0 0 0 512 0z m269.397333 781.482667a41.813333 41.813333 0 0 1-58.88-0.0853" +
                "34L512 570.624 301.397333 781.482667a41.642667 41.642667 0 0 1-58.794666-58.88L452.864" +
                " 512 242.517333 301.397333a41.642667 41.642667 0 0 1 58.88-58.794666L512 453.461333l210" +
                ".517333-210.773333a41.642667 41.642667 0 0 1 58.965334 58.794667L571.050667 512l210.3" +
                "46666 210.602667a41.557333 41.557333 0 0 1 0 58.88z", Color.web("#d81e06"));
        error.setPrefSize(32, 32);
        return error;
    }

    private SVGGlyph getSuccess() {
        //选课完成
        SVGGlyph success = new SVGGlyph(0, "success", "M512 1024C229.451852" +
                " 1024 0 794.548148 0 512S229.451852 0 512 0s512 229.451852 512 512-229.451852 " +
                "512-512 512z m-66.37037-310.992593c17.066667 0 34.133333-5.688889 47.407407-18.9" +
                "62963l254.103704-254.103703c26.548148-26.548148 26.548148-68.266667 0-94.814815s-" +
                "68.266667-26.548148-94.814815 0l-208.592593 208.592593-73.955555-73.955556c-26.54" +
                "8148-26.548148-68.266667-26.548148-92.918519 0-26.548148 26.548148-26.548148 68." +
                "266667 0 92.918518l121.362963 121.362963c13.274074 13.274074 30.340741 18.962963 " +
                "47.407408 18.962963z", Color.web("#0AC380"));
        success.setPrefSize(32, 32);
        return success;
    }
}///~
