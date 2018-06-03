package tk.dcmmcc;

import com.jfoenix.controls.*;
import com.jfoenix.svg.SVGGlyphLoader;
import tk.dcmmcc.gui.main.MainController;
import io.datafx.controller.flow.Flow;
import io.datafx.controller.flow.FlowHandler;
import io.datafx.controller.flow.container.DefaultFlowContainer;
import io.datafx.controller.flow.context.FXMLViewFlowContext;
import io.datafx.controller.flow.context.ViewFlowContext;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.io.IOException;

public class Main extends Application {
    @FXMLViewFlowContext
    private ViewFlowContext flowContext;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        //load SVG icons
        new Thread(() -> {
            try {
                SVGGlyphLoader.loadGlyphsFont(Main.class.getResourceAsStream("/fonts/icomoon.svg"),
                    "icomoon.svg");
            } catch (IOException ioExc) {
                ioExc.printStackTrace();
            }
        }).start();

        Flow flow = new Flow(MainController.class);
        DefaultFlowContainer container = new DefaultFlowContainer();
        flowContext = new ViewFlowContext();
        flowContext.register("Stage", stage);

        FlowHandler flowHandler = flow.createHandler(flowContext);
        StackPane stackPane = flowHandler.start(container);

        //主窗体
        //后期可更改为是否后台运行还是退出
        JFXDecorator decorator = new JFXDecorator(stage, container.getView());
        JFXDialogLayout dialogLayout = new JFXDialogLayout();
        Label heading = new Label("是否确认退出?");
        heading.setFont(Font.loadFont(Main.class
                .getResourceAsStream("/fonts/SourceHanSansCN-Regular.otf"), 26));
        dialogLayout.setHeading(heading);
        Label body = new Label("\n退出将会结束当前选课, 您可以选择后台运行.\n");
        body.setFont(Font.loadFont(Main.class
                .getResourceAsStream("/fonts/SourceHanSansCN-Regular.otf"), 24));
        dialogLayout.setBody(body);
        JFXButton confirmBtn = new JFXButton("确认退出")
                , backgraoundBtn = new JFXButton("后台运行")
                , cancelBtn = new JFXButton("取消");
        confirmBtn.setFont(Font.font("msyh", FontWeight.BOLD, 28));
        backgraoundBtn.setFont(Font.font("msyh", FontWeight.BOLD, 28));
        cancelBtn.setFont(Font.font("msyh", FontWeight.BOLD, 28));
        confirmBtn.setTextFill(Color.DARKBLUE);
        backgraoundBtn.setTextFill(Color.DARKBLUE);
        cancelBtn.setTextFill(Color.DARKBLUE);
        confirmBtn.setOnMouseClicked((e) ->
                System.exit(0));
        // TODO 后台运行
        backgraoundBtn.setOnMouseClicked( (e) -> {});
        dialogLayout.setActions(cancelBtn, confirmBtn, backgraoundBtn);
        JFXDialog exitDialog = new JFXDialog(stackPane, dialogLayout, JFXDialog.DialogTransition.CENTER);
        cancelBtn.setOnMouseClicked((e) ->
                exitDialog.close());
        decorator.setOnCloseButtonAction(
            exitDialog::show);

        //允许最大化
        decorator.setCustomMaximize(true);
        //900x900窗口大小
        Scene scene = new Scene(decorator, 910, 900);
        final ObservableList<String> stylesheets = scene.getStylesheets();
        stylesheets.addAll(Main.class.getResource("/css/jfoenix-fonts.css").toExternalForm(),
                           Main.class.getResource("/css/jfoenix-design.css").toExternalForm(),
                           Main.class.getResource("/css/jfoenix-main-demo.css").toExternalForm());
        //最小窗口大小
        stage.setMinWidth(910);
        stage.setMinHeight(800);
        stage.setScene(scene);
        stage.show();
    }

}

