package tk.dcmmcc.gui.main;

import com.jfoenix.controls.*;
import com.jfoenix.controls.JFXPopup.PopupHPosition;
import com.jfoenix.controls.JFXPopup.PopupVPosition;
import javafx.concurrent.Task;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.control.Dialog;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Pair;
import org.controlsfx.control.textfield.CustomTextField;
import tk.dcmmcc.AccountLoginException;
import tk.dcmmcc.DHUCurrentUser;
import tk.dcmmcc.JwdepConnectionException;
import tk.dcmmcc.datafx.ExtendedAnimatedFlowContainer;
import tk.dcmmcc.datafx.SettingData;
import tk.dcmmcc.gui.sidemenu.SideMenuController;
import tk.dcmmcc.gui.uicomponents.SessionController;
import io.datafx.controller.ViewController;
import io.datafx.controller.flow.Flow;
import io.datafx.controller.flow.FlowHandler;
import io.datafx.controller.flow.context.FXMLViewFlowContext;
import io.datafx.controller.flow.context.ViewFlowContext;
import javafx.animation.Transition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.controlsfx.dialog.LoginDialog;
import tk.dcmmcc.utils.ExceptionDialog;
import javax.annotation.PostConstruct;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import static io.datafx.controller.flow.container.ContainerAnimations.SWIPE_LEFT;

//导入Header(标题, 汉堡菜单按钮, 右边的那个按钮, 整体的header)和drawer(主要是drawer的宽度)的配置文件
@ViewController(value = "/fxml/MainController.fxml", title = "DHU Course Selecter")
public final class MainController {

    @FXMLViewFlowContext
    private ViewFlowContext context;

    @FXML
    private StackPane root;

    @FXML
    private StackPane titleBurgerContainer;
    @FXML
    private JFXHamburger titleBurger;
    @FXML
    private StackPane optionsBurger;
    @FXML
    private JFXRippler optionsRippler;
    @FXML
    private JFXDrawer drawer;
    @FXML
    private JFXSnackbar infoSnackBar;

    private JFXPopup toolbarPopup;

    /**
     * init fxml when loaded.
     */
    @PostConstruct
    public void init() throws Exception {

        //设置提示snackBar的Container
	//infoSnackBar.registerSnackbarContainer(root);

        //设置左边的汉堡菜单按钮变形的动画
        // init the title hamburger icon
        drawer.setOnDrawerOpening(e -> {
            final Transition animation = titleBurger.getAnimation();
            animation.setRate(4);
            animation.play();
        });
        drawer.setOnDrawerClosing(e -> {
            final Transition animation = titleBurger.getAnimation();
            animation.setRate(-4);
            animation.play();
        });
        titleBurgerContainer.setOnMouseClicked(e -> {
            if (drawer.isHidden() || drawer.isHiding()) {
                drawer.open();
            } else {
                drawer.close();
            }
        });


        //设置右边的选项按钮
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ui/popup/MainPopup.fxml"));
        loader.setController(new InputController());
        toolbarPopup = new JFXPopup(loader.load());

        optionsBurger.setOnMouseClicked(e -> toolbarPopup.show(optionsBurger,
                                                               PopupVPosition.TOP,
                                                               PopupHPosition.RIGHT,
                                                               -12,
                                                               15));

        // create the inner flow and content
        //context就是一个容器, 所有东西都用register来注册进去, 然后可以在任意一个位置中通过注册的时候的id用
        // getRegisteredObject把存储的对象取出来直接用
        context = new ViewFlowContext();

        //Create by DCMMC
        context.register("MainRoot", root);
        // set the default controller
        //默认controller是选课界面
        Flow innerFlow = new Flow(SessionController.class);

        final FlowHandler flowHandler = innerFlow.createHandler(context);
        context.register("ContentFlowHandler", flowHandler);
        context.register("ContentFlow", innerFlow);
        //设置汉堡菜单的动画的持续时间
        final Duration containerAnimationDuration = Duration.millis(200);
        drawer.setContent(flowHandler.start(new ExtendedAnimatedFlowContainer(containerAnimationDuration, SWIPE_LEFT)));
        context.register("ContentPane", drawer.getContent().get(0));
        context.register("drawer", drawer);
        context.register("titleBurger", titleBurger);

        // side controller will add links to the content flow
        //设置汉堡菜单点击后的sideMenu中的控件
        Flow sideMenuFlow = new Flow(SideMenuController.class);
        final FlowHandler sideMenuFlowHandler = sideMenuFlow.createHandler(context);
        drawer.setSidePane(sideMenuFlowHandler.start(new ExtendedAnimatedFlowContainer(containerAnimationDuration,
                                                                                       SWIPE_LEFT)));

    }

    /**
     * 登录的并发task
     */
    private static abstract class LoginTask<V> extends Task<V> {
        protected DHUCurrentUser dhuCurrentUser;
        protected Pair<String, String> userAndPsd;

        public void setUserAndPsd(Pair<String, String> userAndPsd) {
            this.userAndPsd = userAndPsd;
        }

        @Override
        abstract protected V call() throws Exception;

        public DHUCurrentUser getDHUCurrentUser() {
            return dhuCurrentUser;
        }
    }

    /**
     * 加载Controllers
     */
    public final class InputController {
        private void networkErr() {
            //网络错误
            Alert networkErrAlert = new Alert(Alert.AlertType.ERROR);
            networkErrAlert.setTitle("网络错误");
            networkErrAlert.setHeaderText("因为网络问题登录帐号失败!");
            networkErrAlert.setContentText("请检查阁下的网络是否连接正常, 是否能够连接到教务处系统." +
                    "\n点击转跳到VPN可以转跳到教务处VPN连接站点.");
            ButtonType vpnBtn = new ButtonType("转跳到VPN"), confirmBtn = new ButtonType("了解");
            networkErrAlert.getButtonTypes().setAll(vpnBtn, confirmBtn);
            Optional<ButtonType> result = networkErrAlert.showAndWait();
            if (result.get() == vpnBtn) {
                try {
                    Desktop.getDesktop().browse(new URI("https://vip.dhu.edu.cn"));

                    //debug
                    //throw new IOException();
                } catch (URISyntaxException ue) {
                    //..
                    //难道我把教务处vpn网址写错了? 不存在的...

                    ExceptionDialog.launch(ue, "开发者犯了个愚蠢的错误...",
                            "教务处网址写错了.. 可能这软件已经年久失修了或者教务处的网址已经不是" +
                                    "当年的了, 试图联系一下开发者吧 :)");
                } catch (IOException ioe) {
                    //打开浏览器的时候发生了IO异常

                    ExceptionDialog.launch(ioe, "打开浏览器的时候发生了IO异常...",
                            "在打开教务处vpn网站的时候发生的IO异常, 请联系开发者");
                }
            }

            networkErrAlert.close();
        }

        @FXML
        private JFXListView<?> toolbarPopupList;

        //设置右边的选项按钮的功能
        @FXML
        private void submit() {
            if (toolbarPopupList.getSelectionModel().getSelectedIndex() == 0) {
                if (SettingData.getDhuCurrentUser() != null) {
                    infoSnackBar.fireEvent(new JFXSnackbar.SnackbarEvent("你已经登录过了!",
                            "了解", 2000, false,
                            b -> infoSnackBar.close()));
                    return;
                }

                //登录教务处帐号
                LoginDialog loginDialog = new LoginDialog(null, null);
                loginDialog.setResizable(true);
                //先设置不能点确认
                ButtonType loginButtonType = null;
                for (ButtonType bt : loginDialog.getDialogPane().getButtonTypes()) {
                    if (bt.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                        loginButtonType = bt;
                        break;
                    }
                }
                ButtonType fuckLoginBT = loginButtonType;
                loginDialog.getDialogPane()
                        .lookupButton(fuckLoginBT)
                        .setDisable(true);
                if (fuckLoginBT != null) {
                    //在帐号那一栏不为空的时候才能登录
                    ((CustomTextField)((VBox)loginDialog.getDialogPane().getContent()).getChildren().get(1))
                            .textProperty().addListener((observable, oldValue, newValue) -> {
                        loginDialog.getDialogPane()
                                .lookupButton(fuckLoginBT)
                                .setDisable(newValue.trim().isEmpty());
                    });
                }

                loginDialog.showAndWait().ifPresent(userAndPsd -> {
                    //debug...
                    //System.out.println("User: " + userAndPsd.getKey() + "\nPassword: " + userAndPsd.getValue());

                    loginDialog.close();

                    //正在登录的对话框(并发)
                    LoginTask<Boolean> task = new LoginTask<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            if (userAndPsd.getValue() == null || userAndPsd.getValue().equals(""))
                                throw new AccountLoginException("密码不能为空!");

                            this.dhuCurrentUser = new DHUCurrentUser(userAndPsd.getKey(),
                                    userAndPsd.getValue());
                            return true;
                        }
                    };
                    task.setUserAndPsd(userAndPsd);

                    //创建正在登录的对话框
                    Dialog<Pair<String, String>> loadingDialog = new Dialog<>();
                    //设置永远置顶窗口
                    Stage stageDialog = (Stage) loadingDialog.getDialogPane().getScene().getWindow();
                    stageDialog.setAlwaysOnTop(true);
                    stageDialog.toFront(); // not sure if necessary
                    loadingDialog.setTitle("正在登录");
                    loadingDialog.setHeaderText("正在尝试登录帐号: " + userAndPsd.getKey());
                    DialogPane dialogPane = loadingDialog.getDialogPane();
                    dialogPane.getButtonTypes().addAll(ButtonType.APPLY);
                    //不能点确认
                    dialogPane.lookupButton(ButtonType.APPLY).setDisable(true);
                    JFXSpinner loading = new JFXSpinner();
                    final StackPane content = new StackPane(loading);
                    content.setAlignment(Pos.CENTER);
                    dialogPane.setContent(content);

                    //登录的时候显示正在加载的对话框
                    task.setOnRunning((e) -> loadingDialog.show());
                    task.setOnSucceeded((e) -> {
                        loadingDialog.close();
                        SettingData.login(task.getDHUCurrentUser());
                    });
                    //譬如登录的时候抛出异常
                    task.setOnFailed((e) -> {
                        loadingDialog.close();

                        if (task.getException() instanceof Exception) {
                            Exception exception = (Exception) task.getException();

                            if (exception instanceof JwdepConnectionException) {
                                networkErr();
                            } else if (exception instanceof AccountLoginException) {
                                //帐号或者密码错误

                                Alert networkErrAlert = new Alert(Alert.AlertType.ERROR);
                                networkErrAlert.setTitle("教务处帐号或者密码错误");
                                networkErrAlert.setHeaderText("教务处帐号或者密码错误");
                                networkErrAlert.setContentText("请检查阁下的教务处帐号密码是否正确.\n" +
                                        "P.S. 教务处密码和校园公共Wifi密码有区别.");
                                networkErrAlert.showAndWait();
                            } else {
                                ExceptionDialog.launch(exception, "发生了意料之外的异常",
                                        "请联系开发者.");
                            }
                        } else {
                            ExceptionDialog.launch(new Exception(task.getException()), "发生了意料之外的异常",
                                    "请联系开发者.");
                        }
                    });

                    //Start TASK
                    new Thread(task).start();

                });
            } else if (toolbarPopupList.getSelectionModel().getSelectedIndex() == 1) {
                //第二个按钮(i.e., 登出)
                if (SettingData.getDhuCurrentUser() == null) {
                    infoSnackBar.fireEvent(new JFXSnackbar.SnackbarEvent("你还没有登录!",
                            "了解", 2000, false,
                            b -> infoSnackBar.close()));
                    return;
                }

                //退出
                SettingData.signOut();
                infoSnackBar.fireEvent(new JFXSnackbar.SnackbarEvent("退出成功",
                        "了解", 2000, false,
                        b -> infoSnackBar.close()));
            }
        }
    }
}
