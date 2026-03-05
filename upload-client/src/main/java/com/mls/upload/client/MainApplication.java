package com.mls.upload.client;

import com.mls.upload.client.controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 木林森图片上传客户端主启动类
 * 
 * @author MLS Development Team
 * @version 1.0.0
 */
public class MainApplication extends Application {
    
    private static final Logger logger = LoggerFactory.getLogger(MainApplication.class);
    
    private MainController mainController;
    
    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("启动木林森图片上传客户端");
            
            // 加载FXML文件
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = loader.load();
            
            // 获取控制器
            mainController = loader.getController();
            mainController.setPrimaryStage(primaryStage);
            
            // 创建场景 - 优化默认窗口尺寸，适配更多显示器分辨率
            Scene scene = new Scene(root, 900, 720);

            // 加载CSS样式
            String cssPath = getClass().getResource("/css/main.css").toExternalForm();
            scene.getStylesheets().add(cssPath);

            // 设置舞台属性
            primaryStage.setTitle("木林森图片上传系统 v1.0.0");

            // 设置应用程序图标 - 多尺寸PNG图标
            try {
                // 定义多个尺寸的图标文件
                String[] iconSizes = {"16x16", "32x32", "48x48", "64x64"};
                boolean iconLoaded = false;

                for (String size : iconSizes) {
                    try {
                        String iconPath = "/icons/icon_" + size + ".png";
                        Image icon = new Image(getClass().getResourceAsStream(iconPath));
                        if (!icon.isError()) {
                            primaryStage.getIcons().add(icon);
                            logger.debug("成功加载图标: {}", iconPath);
                            iconLoaded = true;
                        }
                    } catch (Exception e) {
                        logger.debug("加载图标失败: /icons/icon_{}.png - {}", size, e.getMessage());
                    }
                }

                // 如果PNG图标加载失败，尝试加载ICO图标作为备用
                if (!iconLoaded) {
                    try {
                        Image fallbackIcon = new Image(getClass().getResourceAsStream("/icon.ico"));
                        if (!fallbackIcon.isError()) {
                            primaryStage.getIcons().add(fallbackIcon);
                            logger.info("使用备用ICO图标");
                            iconLoaded = true;
                        }
                    } catch (Exception e) {
                        logger.debug("备用ICO图标加载失败: {}", e.getMessage());
                    }
                }

                if (iconLoaded) {
                    logger.info("应用程序图标设置成功，共加载 {} 个尺寸", primaryStage.getIcons().size());
                } else {
                    logger.warn("所有图标加载失败，将使用默认图标");
                }

            } catch (Exception iconEx) {
                logger.error("设置应用程序图标时发生异常: {}", iconEx.getMessage(), iconEx);
            }

            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            primaryStage.setResizable(true);
            
            // 设置关闭事件
            primaryStage.setOnCloseRequest(event -> {
                logger.info("应用程序正在关闭");
                if (mainController != null) {
                    mainController.cleanup();
                }
                System.exit(0);
            });
            
            // 显示窗口
            primaryStage.show();
            
            logger.info("木林森图片上传客户端启动完成");
            
        } catch (Exception e) {
            logger.error("启动应用程序失败: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
    
    public static void main(String[] args) {
        // 设置系统属性以改善图标显示
        System.setProperty("java.awt.headless", "false");
        System.setProperty("prism.lcdtext", "false");

        // 设置应用程序名称，有助于任务栏图标识别
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "木林森图片上传系统");
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        logger.info("木林森图片上传客户端开始启动");
        logger.debug("系统属性已设置，准备启动JavaFX应用程序");

        launch(args);
    }
}
