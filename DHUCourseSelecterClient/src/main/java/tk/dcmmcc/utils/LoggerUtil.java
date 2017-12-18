package tk.dcmmcc.utils;

import jp.uphy.javafx.console.ConsoleApplication;
import java.io.*;
import java.util.Scanner;
import java.util.logging.*;

/**
 * 用于处理Log的小程序
 * Created by DCMMC on 2017/9/13.
 */
public class LoggerUtil extends ConsoleApplication {
    @Override
    protected void invokeMain(final String[] args) {
        /* log位于 %CLASSPATH%/log.log */
        try {
            Scanner scanner = new Scanner(new BufferedInputStream(new FileInputStream(
                    System.getProperty("user.home") + File.separator + "DHUCourseSelecter.log")));
            while (scanner.hasNextLine())
                System.out.println(scanner.nextLine() + "\n");

            //查看更多的logfile
            File file;
            int cnt = 1;
            while ((file = new File(System.getProperty("user.home") + File.separator + "DHUCourseSelecter.log" + "."
                + cnt)).exists()) {
                scanner = new Scanner(new BufferedInputStream(new FileInputStream(
                        System.getProperty("user.home") + File.separator + "DHUCourseSelecter.log" + "."
                                + (cnt++) )));
                while (scanner.hasNextLine())
                    System.out.println(scanner.nextLine() + "\n");
            }
        } catch (FileNotFoundException fe) {
            System.out.println("日志文件没有找到(%CLASSPATH%/log.log)");
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        launch(args);
    }

    /**
     * 初始化logger
     */
    public static Logger initLogger(Logger logger) {
        try {
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.ALL);
            consoleHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(consoleHandler);
            int limit = 1000000; // 1 Mb

            FileHandler fileHandler;
            //默认放在user.home文件夹下面
            fileHandler = new FileHandler("%h/DHUCourseSelecter.log",  limit, 1,
                    false);


            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.ALL);
            logger.addHandler(fileHandler);
        } catch (IOException | NullPointerException ne) {
            //严重错误
            logger.severe("为logger添加FileHandler的时候发生严重错误!");
            ne.printStackTrace();
            ExceptionDialog.launch(ne, "发生严重错误! ", "为logger添加FileHandler的时候发生严重错误!");
        }

        return logger;
    }
}///~
