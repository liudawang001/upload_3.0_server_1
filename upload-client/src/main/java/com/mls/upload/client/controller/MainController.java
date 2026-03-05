package com.mls.upload.client.controller;

import com.mls.upload.client.model.vo.LogEntry;
import com.mls.upload.client.model.entity.ExcelRowData;
import com.mls.upload.client.service.AuthService;
import com.mls.upload.client.service.CredentialManager;
import com.mls.upload.client.service.ExcelService;
import com.mls.upload.client.service.FileUploadService;
import com.mls.upload.client.service.LogService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

/**
 * 主界面控制器
 * 处理JavaFX界面事件和用户交互逻辑
 *
 * @author MLS Development Team
 * @version 1.0.0
 */
public class MainController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    private Stage primaryStage;
    private Timer timeUpdateTimer;

    // 服务类
    private AuthService authService;
    private ExcelService excelService;
    private FileUploadService fileUploadService;
    private LogService logService;
    private CredentialManager credentialManager;

    // 登录相关控件
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberPasswordCheckBox;
    @FXML private Button loginButton;
    @FXML private Label loginStatusLabel;

    // 图片类型选择
    @FXML private ToggleGroup imageTypeGroup;
    @FXML private RadioButton flowerTypeRadio;
    @FXML private RadioButton colorTypeRadio;

    // Excel文件处理
    @FXML private TextField excelPathField;
    @FXML private Button selectExcelButton;
    @FXML private Button uploadExcelButton;
    @FXML private Label excelStatusLabel;

    // 文件夹选择
    @FXML private TextField folderPathField;
    @FXML private Button selectFolderButton;
    @FXML private Button scanFolderButton;
    @FXML private Label folderStatusLabel;

    // 上传控制
    @FXML private Button startUploadButton;
    @FXML private Button pauseUploadButton;
    @FXML private Button stopUploadButton;
    @FXML private ProgressBar uploadProgressBar;
    @FXML private Label uploadStatusLabel;

    // 日志表格
    @FXML private TableView<LogEntry> logTableView;
    @FXML private TableColumn<LogEntry, Boolean> selectColumn;
    @FXML private TableColumn<LogEntry, Integer> indexColumn;
    @FXML private TableColumn<LogEntry, String> productCodeColumn;
    @FXML private TableColumn<LogEntry, String> filenameColumn;
    @FXML private TableColumn<LogEntry, String> matchStatusColumn;
    @FXML private TableColumn<LogEntry, String> uploadStatusColumn;
    @FXML private TableColumn<LogEntry, String> progressColumn;
    @FXML private TableColumn<LogEntry, String> messageColumn;
    @FXML private TableColumn<LogEntry, String> timeColumn;

    // 日志控制按钮
    @FXML private Button selectAllButton;
    @FXML private Button clearLogButton;
    @FXML private Button exportLogButton;

    // 导出功能按钮
    @FXML private Button exportFailedButton;
    @FXML private Button exportUnuploadedButton;
    @FXML private Button exportMatchedImagesButton;

    // 状态栏
    @FXML private Label connectionStatusLabel;
    @FXML private Label totalCountLabel;
    @FXML private Label successCountLabel;
    @FXML private Label failedCountLabel;
    @FXML private Label scanResultLabel;
    @FXML private Label currentTimeLabel;

    // 数据
    private ObservableList<LogEntry> logEntries = FXCollections.observableArrayList();
    private FilteredList<LogEntry> filteredLogEntries; // 过滤后的日志条目（隐藏系统日志）
    private List<ExcelRowData> excelRowDataList = new ArrayList<>(); // Excel行数据列表，支持重复商品编号
    private boolean isLoggedIn = false;
    private boolean isUploading = false;
    private boolean isPaused = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("初始化主界面控制器");

        // 初始化服务
        initializeServices();

        // 初始化界面组件
        initializeComponents();

        // 自动填充保存的登录凭据
        loadSavedCredentials();

        // 启动时间更新定时器
        startTimeUpdateTimer();

        logger.info("主界面控制器初始化完成");
    }

    /**
     * 初始化服务类
     */
    private void initializeServices() {
        try {
            authService = new AuthService();
            excelService = new ExcelService();
            fileUploadService = new FileUploadService();
            logService = new LogService();
            credentialManager = new CredentialManager();

            logger.info("服务类初始化完成");
        } catch (Exception e) {
            logger.error("服务类初始化失败: {}", e.getMessage(), e);
            showErrorAlert("初始化失败", "服务类初始化失败: " + e.getMessage());
        }
    }

    /**
     * 初始化界面组件
     */
    private void initializeComponents() {
        // 初始化表格列
        initializeTableColumns();

        // 初始化过滤列表，隐藏系统日志
        filteredLogEntries = new FilteredList<>(logEntries, this::isUserLogEntry);

        // 设置表格数据（使用过滤后的列表）
        logTableView.setItems(filteredLogEntries);
        
        // 启用表格编辑，允许复选框点击交互
        logTableView.setEditable(true);

        // 设置初始状态
        updateUIState();

        // 设置默认值
        usernameField.setText("admin");
        flowerTypeRadio.setSelected(true);

        logger.info("界面组件初始化完成");
    }

    /**
     * 判断是否为用户操作日志（非系统日志）
     *
     * @param entry 日志条目
     * @return true表示是用户操作日志，false表示是系统日志
     */
    private boolean isUserLogEntry(LogEntry entry) {
        return entry != null && !"系统".equals(entry.getProductCode());
    }

    /**
     * 初始化表格列
     */
    private void initializeTableColumns() {
        // 初始化复选框列
        selectColumn.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));
        selectColumn.setEditable(true);  // 明确标记为可编辑

        indexColumn.setCellValueFactory(new PropertyValueFactory<>("index"));
        productCodeColumn.setCellValueFactory(new PropertyValueFactory<>("productCode"));
        filenameColumn.setCellValueFactory(new PropertyValueFactory<>("filename"));
        matchStatusColumn.setCellValueFactory(new PropertyValueFactory<>("matchStatus"));
        uploadStatusColumn.setCellValueFactory(new PropertyValueFactory<>("uploadStatus"));
        progressColumn.setCellValueFactory(new PropertyValueFactory<>("progress"));
        messageColumn.setCellValueFactory(new PropertyValueFactory<>("message"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("time"));

        // 设置状态列的样式
        matchStatusColumn.setCellFactory(column -> new TableCell<LogEntry, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("匹配".equals(item)) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else if ("不匹配".equals(item)) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        uploadStatusColumn.setCellFactory(column -> new TableCell<LogEntry, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("成功".equals(item)) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else if ("失败".equals(item)) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else if ("上传中".equals(item)) {
                        setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }

    /**
     * 更新界面状态
     */
    private void updateUIState() {
        // 根据登录状态更新按钮状态
        boolean loggedIn = isLoggedIn;

        selectExcelButton.setDisable(!loggedIn);
        uploadExcelButton.setDisable(!loggedIn || excelPathField.getText().isEmpty());
        selectFolderButton.setDisable(!loggedIn);
        scanFolderButton.setDisable(!loggedIn || folderPathField.getText().isEmpty());

        // 根据上传状态更新按钮
        startUploadButton.setDisable(!loggedIn || isUploading || logEntries.isEmpty());
        pauseUploadButton.setDisable(!isUploading || isPaused);
        stopUploadButton.setDisable(!isUploading);

        // 检查是否有失败记录或不匹配记录来控制导出按钮状态
        boolean hasFailedRecords = logEntries.stream()
            .anyMatch(entry -> isFailedOrMismatchedEntry(entry));
        exportFailedButton.setDisable(!loggedIn || !hasFailedRecords);

        // 检查是否有未上传的记录（未勾选的或不匹配的）来控制导出未上传按钮状态
        boolean hasUnuploadedRecords = logEntries.stream()
            .anyMatch(entry -> !entry.isSelected() || !entry.getMatchStatus().equals("匹配"));
        exportUnuploadedButton.setDisable(!loggedIn || !hasUnuploadedRecords);

        // 检查是否有匹配的记录且已选择图片文件夹来控制导出匹配图片按钮状态
        // 注意：这里只检查matchStatus为"匹配"，不要求uploadStatus为"成功"
        boolean hasMatchedRecords = logEntries.stream()
            .anyMatch(entry -> isMatchedEntry(entry));
        boolean hasFolderSelected = !folderPathField.getText().isEmpty();
        exportMatchedImagesButton.setDisable(!loggedIn || !hasMatchedRecords || !hasFolderSelected);

        // 更新连接状态
        if (loggedIn) {
            connectionStatusLabel.setText("服务器连接: 已连接");
            connectionStatusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            loginStatusLabel.setText("已登录: " + usernameField.getText());
            loginStatusLabel.setStyle("-fx-text-fill: #27ae60;");
        } else {
            connectionStatusLabel.setText("服务器连接: 未连接");
            connectionStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            loginStatusLabel.setText("未登录");
            loginStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
        }
    }

    /**
     * 启动时间更新定时器
     */
    private void startTimeUpdateTimer() {
        timeUpdateTimer = new Timer(true);
        timeUpdateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    currentTimeLabel.setText(currentTime);
                });
            }
        }, 0, 1000); // 每秒更新一次
    }

    /**
     * 显示错误对话框
     */
    private void showErrorAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * 显示信息对话框
     */
    private void showInfoAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * 显示确认对话框
     */
    private boolean showConfirmAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    // ==================== 事件处理方法 ====================

    /**
     * 处理登录按钮点击事件
     */
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showErrorAlert("登录失败", "请输入用户名和密码");
            return;
        }

        // 禁用登录按钮，显示加载状态
        loginButton.setDisable(true);
        loginButton.setText("登录中...");

        // 异步执行登录
        Task<Boolean> loginTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return authService.login(username, password);
            }

            @Override
            protected void succeeded() {
                boolean success = getValue();
                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    loginButton.setText("登录");

                    if (success) {
                        isLoggedIn = true;
                        logger.info("用户登录成功: {}", username);

                        // 保存用户凭据
                        MainController.this.saveCredentials(username, password);

                        showInfoAlert("登录成功", "欢迎使用木林森图片上传系统！");
                    } else {
                        logger.warn("用户登录失败: {}", username);
                        showErrorAlert("登录失败", "用户名或密码错误");
                    }

                    updateUIState();
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    loginButton.setText("登录");
                    logger.error("登录异常: {}", getException().getMessage(), getException());
                    showErrorAlert("登录异常", "网络连接失败，请检查服务器连接");
                    updateUIState();
                });
            }
        };

        new Thread(loginTask).start();
    }

    /**
     * 处理选择Excel文件事件
     */
    @FXML
    private void handleSelectExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择Excel文件");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Excel文件", "*.xlsx", "*.xls"),
            new FileChooser.ExtensionFilter("所有文件", "*.*")
        );

        File selectedFile = fileChooser.showOpenDialog(primaryStage);
        if (selectedFile != null) {
            excelPathField.setText(selectedFile.getAbsolutePath());
            excelStatusLabel.setText("已选择: " + selectedFile.getName());
            excelStatusLabel.setStyle("-fx-text-fill: #27ae60;");
            updateUIState();
            logger.info("选择Excel文件: {}", selectedFile.getAbsolutePath());
        }
    }

    /**
     * 处理上传Excel文件事件
     */
    @FXML
    private void handleUploadExcel() {
        String excelPath = excelPathField.getText();
        if (excelPath.isEmpty()) {
            showErrorAlert("错误", "请先选择Excel文件");
            return;
        }

        uploadExcelButton.setDisable(true);
        uploadExcelButton.setText("解析中...");
        excelStatusLabel.setText("正在解析Excel文件...");
        excelStatusLabel.setStyle("-fx-text-fill: #f39c12;");

        Task<List<ExcelRowData>> parseTask = new Task<List<ExcelRowData>>() {
            @Override
            protected List<ExcelRowData> call() throws Exception {
                return excelService.parseExcelFileToRowDataMap(excelPath);
            }

            @Override
            protected void succeeded() {
                List<ExcelRowData> rowDataList = getValue();
                Platform.runLater(() -> {
                    uploadExcelButton.setDisable(false);
                    uploadExcelButton.setText("上传解析");

                    if (rowDataList != null && !rowDataList.isEmpty()) {
                        // 存储Excel行数据列表，支持重复商品编号
                        excelRowDataList = rowDataList;

                        excelStatusLabel.setText("解析成功，共" + rowDataList.size() + "个商品编号");
                        excelStatusLabel.setStyle("-fx-text-fill: #27ae60;");
                        logger.info("Excel解析成功，商品编号数量: {}", rowDataList.size());

                        // 清空现有日志
                        logEntries.clear();

                        // 添加商品编号到日志表格，并关联Excel行数据
                        // 修改：支持重复商品编号，每个Excel行都创建独立的LogEntry
                        int index = 1;
                        for (ExcelRowData rowData : rowDataList) {
                            LogEntry logEntry = new LogEntry();
                            logEntry.setIndex(index++);
                            logEntry.setProductCode(rowData.getProductCode());
                            logEntry.setExcelRowData(rowData); // 关联Excel行数据
                            logEntry.setMatchStatus("待匹配");
                            logEntry.setUploadStatus("待上传");
                            logEntry.setProgress("0%");
                            logEntry.setTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                            logEntries.add(logEntry);
                        }

                        updateStatistics();
                        updateUIState();
                    } else {
                        excelStatusLabel.setText("解析失败，未找到有效数据");
                        excelStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
                        showErrorAlert("解析失败", "Excel文件中未找到有效的商品编号数据");
                    }
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    uploadExcelButton.setDisable(false);
                    uploadExcelButton.setText("上传解析");
                    excelStatusLabel.setText("解析失败");
                    excelStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
                    logger.error("Excel解析失败: {}", getException().getMessage(), getException());
                    showErrorAlert("解析失败", "Excel文件解析失败: " + getException().getMessage());
                });
            }
        };

        new Thread(parseTask).start();
    }

    /**
     * 处理选择文件夹事件
     */
    @FXML
    private void handleSelectFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("选择图片文件夹");

        File selectedDirectory = directoryChooser.showDialog(primaryStage);
        if (selectedDirectory != null) {
            folderPathField.setText(selectedDirectory.getAbsolutePath());
            folderStatusLabel.setText("已选择: " + selectedDirectory.getName());
            folderStatusLabel.setStyle("-fx-text-fill: #27ae60;");
            updateUIState();
            logger.info("选择图片文件夹: {}", selectedDirectory.getAbsolutePath());
        }
    }

    /**
     * 处理扫描文件夹事件
     */
    @FXML
    private void handleScanFolder() {
        String folderPath = folderPathField.getText();
        if (folderPath.isEmpty()) {
            showErrorAlert("错误", "请先选择图片文件夹");
            return;
        }

        scanFolderButton.setDisable(true);
        scanFolderButton.setText("扫描中...");
        folderStatusLabel.setText("正在扫描图片文件...");
        folderStatusLabel.setStyle("-fx-text-fill: #f39c12;");

        // 添加扫描开始日志
        addScanLogEntry("开始扫描图片文件: " + new File(folderPath).getName(), "扫描中");

        Task<List<File>> scanTask = new Task<List<File>>() {
            @Override
            protected List<File> call() throws Exception {
                return fileUploadService.scanImageFiles(folderPath);
            }

            @Override
            protected void succeeded() {
                List<File> imageFiles = getValue();
                Platform.runLater(() -> {
                    scanFolderButton.setDisable(false);
                    scanFolderButton.setText("扫描图片");

                    if (imageFiles != null && !imageFiles.isEmpty()) {
                        folderStatusLabel.setText("扫描完成，共找到" + imageFiles.size() + "个图片文件");
                        folderStatusLabel.setStyle("-fx-text-fill: #27ae60;");
                        logger.info("图片扫描完成，文件数量: {}", imageFiles.size());

                        // 添加扫描完成日志
                        addScanLogEntry("扫描完成，共找到 " + imageFiles.size() + " 个图片文件", "完成");

                        // 执行图片匹配
                        matchImagesWithProducts(imageFiles);
                        updateUIState();
                    } else {
                        folderStatusLabel.setText("未找到图片文件");
                        folderStatusLabel.setStyle("-fx-text-fill: #e74c3c;");

                        // 添加未找到文件日志
                        addScanLogEntry("扫描完成，未找到图片文件", "无结果");

                        showErrorAlert("扫描结果", "在指定文件夹中未找到图片文件");
                    }
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    scanFolderButton.setDisable(false);
                    scanFolderButton.setText("扫描图片");
                    folderStatusLabel.setText("扫描失败");
                    folderStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
                    logger.error("图片扫描失败: {}", getException().getMessage(), getException());

                    // 添加扫描失败日志
                    addScanLogEntry("扫描失败: " + getException().getMessage(), "失败");

                    showErrorAlert("扫描失败", "图片文件扫描失败: " + getException().getMessage());
                });
            }
        };

        new Thread(scanTask).start();
    }

    /**
     * 添加扫描相关的日志条目
     */
    private void addScanLogEntry(String message, String status) {
        Platform.runLater(() -> {
            LogEntry scanLogEntry = new LogEntry();
            scanLogEntry.setIndex(logEntries.size() + 1);
            scanLogEntry.setProductCode("系统");
            scanLogEntry.setFilename("-");
            scanLogEntry.setMatchStatus(status);
            scanLogEntry.setUploadStatus("-");
            scanLogEntry.setProgress("-");
            scanLogEntry.setMessage(message);
            scanLogEntry.setTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

            logEntries.add(scanLogEntry);
            updateStatistics();

            // 滚动到最新日志
            if (!logEntries.isEmpty()) {
                logTableView.scrollTo(logEntries.size() - 1);
            }
        });
    }

    /**
     * 匹配图片与商品编号
     * 修改说明：支持同一张图片匹配多个商品编号，移除break限制
     */
    private void matchImagesWithProducts(List<File> imageFiles) {
        // 添加匹配开始日志
        addScanLogEntry("开始匹配图片与商品编号（支持一对多匹配）...", "匹配中");

        // 使用AtomicInteger解决lambda表达式中的变量作用域问题
        AtomicInteger matchedCount = new AtomicInteger(0);
        AtomicInteger unmatchedCount = new AtomicInteger(0);

        for (LogEntry entry : logEntries) {
            // 跳过系统日志条目
            if ("系统".equals(entry.getProductCode())) {
                continue;
            }

            String productCode = entry.getProductCode();
            boolean matched = false;

            // 修改：遍历所有图片文件，寻找最佳匹配（优先精确匹配）
            File bestMatchFile = null;
            boolean exactMatch = false;

            for (File imageFile : imageFiles) {
                String filename = imageFile.getName();
                String filenameWithoutExt = filename.substring(0, filename.lastIndexOf('.'));

                // 精确匹配：文件名（不含扩展名）等于商品编号
                if (filenameWithoutExt.equalsIgnoreCase(productCode)) {
                    bestMatchFile = imageFile;
                    exactMatch = true;
                    break; // 找到精确匹配就停止
                }
                // 包含匹配：文件名包含商品编号（作为备选）
                else if (!exactMatch && filename.toLowerCase().contains(productCode.toLowerCase())) {
                    bestMatchFile = imageFile;
                }
            }

            if (bestMatchFile != null) {
                // 找到匹配的图片文件
                final File matchedFile = bestMatchFile;
                final boolean isExactMatch = exactMatch;
                Platform.runLater(() -> {
                    entry.setFilename(matchedFile.getName());
                    entry.setFilePath(matchedFile.getAbsolutePath());
                    entry.setMatchStatus("匹配");
                    entry.setMessage(isExactMatch ? "精确匹配" : "包含匹配");
                    refreshTableView();
                });
                matched = true;
                matchedCount.incrementAndGet();
            }

            if (!matched) {
                // 在JavaFX Application Thread中更新UI状态
                Platform.runLater(() -> {
                    entry.setMatchStatus("不匹配");
                    entry.setMessage("未找到对应图片");
                    // 立即刷新表格以显示匹配状态更新
                    refreshTableView();
                });
                unmatchedCount.incrementAndGet();
            }
        }

        // 在JavaFX Application Thread中更新统计信息和状态显示
        Platform.runLater(() -> {
            updateStatistics();
            // 更新状态栏扫描结果显示
            updateScanResult(matchedCount.get());
            // 最终刷新表格确保所有更新都显示
            refreshTableView();
        });

        logger.info("图片匹配完成");

        // 添加匹配完成统计日志
        String matchResult = String.format("匹配完成，成功匹配 %d 个，未匹配 %d 个", matchedCount.get(), unmatchedCount.get());
        addScanLogEntry(matchResult, "完成");
    }

    /**
     * 更新统计信息
     * 基于过滤后的日志条目（只包含用户操作日志，不包含系统日志）
     */
    private void updateStatistics() {
        // 使用过滤后的日志条目进行统计，确保与表格显示一致
        int total = filteredLogEntries.size();
        long success = filteredLogEntries.stream().filter(entry -> "成功".equals(entry.getUploadStatus())).count();
        long failed = filteredLogEntries.stream().filter(entry -> "失败".equals(entry.getUploadStatus())).count();

        totalCountLabel.setText("总数: " + total);
        successCountLabel.setText("成功: " + success);
        failedCountLabel.setText("失败: " + failed);
    }

    /**
     * 更新扫描结果显示
     *
     * @param matchedCount 匹配成功的图片数量
     */
    private void updateScanResult(int matchedCount) {
        if (scanResultLabel != null) {
            String resultText = String.format("扫描完成，匹配%d张图片", matchedCount);
            scanResultLabel.setText(resultText);
            scanResultLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        }
    }

    /**
     * 更新进度条文本显示
     * @param successCount 成功上传数量
     * @param total 总文件数量
     */
    private void updateProgressBarText(int successCount, int total) {
        // 由于JavaFX ProgressBar不直接支持文本显示，我们通过uploadStatusLabel来显示进度文本
        String progressText = String.format("上传进度: %d/%d", successCount, total);
        uploadStatusLabel.setText(progressText);
        uploadStatusLabel.setStyle("-fx-text-fill: #3498db;");
    }

    /**
     * 强制刷新表格视图
     */
    private void refreshTableView() {
        if (logTableView != null) {
            logTableView.refresh();
        }
    }

    /**
     * 获取选择的图片类型
     * @return 图片类型代码（1=花型图，2=配色图）
     */
    private String getSelectedImageType() {
        if (flowerTypeRadio.isSelected()) {
            return "1"; // 花型图
        } else if (colorTypeRadio.isSelected()) {
            return "2"; // 配色图
        } else {
            return "1"; // 默认为花型图
        }
    }

    /**
     * 处理开始上传事件
     * 修改：只上传已选中的记录
     */
    @FXML
    private void handleStartUpload() {
        if (logEntries.isEmpty()) {
            showErrorAlert("错误", "没有可上传的数据");
            return;
        }

        // 统计选中和未选中的行数
        long selectedCount = filteredLogEntries.stream()
            .filter(LogEntry::isSelected)
            .count();
        
        long unselectedCount = filteredLogEntries.stream()
            .filter(entry -> !entry.isSelected())
            .count();

        // 如果没有选中任何行，提示用户
        if (selectedCount == 0) {
            showErrorAlert("错误", "没有选中任何记录，请先勾选要上传的行");
            return;
        }

        // 构建确认对话框信息
        String message = String.format("确定要开始上传吗？\n\n已选中: %d 条\n将跳过: %d 条\n\n只有勾选的记录会被上传", 
            selectedCount, unselectedCount);

        if (!showConfirmAlert("确认上传", message)) {
            return;
        }

        isUploading = true;
        isPaused = false;
        updateUIState();

        uploadStatusLabel.setText("正在上传...");
        uploadStatusLabel.setStyle("-fx-text-fill: #f39c12;");

        logger.info("上传开始 - 已选中: {} 条，将跳过: {} 条", selectedCount, unselectedCount);

        // 开始上传任务
        startUploadTask();
    }

    /**
     * 处理暂停上传事件
     */
    @FXML
    private void handlePauseUpload() {
        isPaused = true;
        updateUIState();
        uploadStatusLabel.setText("上传已暂停");
        uploadStatusLabel.setStyle("-fx-text-fill: #f39c12;");
        logger.info("上传已暂停");
    }

    /**
     * 处理停止上传事件
     */
    @FXML
    private void handleStopUpload() {
        if (!showConfirmAlert("确认停止", "确定要停止上传吗？")) {
            return;
        }

        isUploading = false;
        isPaused = false;
        updateUIState();

        uploadProgressBar.setProgress(0.0);
        uploadStatusLabel.setText("上传已停止");
        uploadStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
        logger.info("上传已停止");
    }

    /**
     * 处理全选/取消全选按钮点击事件
     */
    @FXML
    private void handleSelectAll() {
        // 判断当前是否所有行都已选中
        boolean allSelected = filteredLogEntries.stream()
            .allMatch(LogEntry::isSelected);

        // 根据当前状态切换全选/取消全选
        for (LogEntry entry : filteredLogEntries) {
            entry.setSelected(!allSelected);
        }

        // 更新按钮文本
        selectAllButton.setText(allSelected ? "全选" : "取消全选");
        logger.info("日志全选/取消全选: {}", !allSelected ? "全选" : "取消全选");
    }

    /**
     * 处理清空日志事件
     */
    @FXML
    private void handleClearLog() {
        if (!logEntries.isEmpty() && showConfirmAlert("确认清空", "确定要清空所有日志吗？")) {
            logEntries.clear();
            updateStatistics();
            updateUIState();
            // 重置全选按钮状态
            selectAllButton.setText("全选");
            logger.info("日志已清空");
        }
    }

    /**
     * 处理导出日志事件
     */
    @FXML
    private void handleExportLog() {
        if (logEntries.isEmpty()) {
            showErrorAlert("错误", "没有可导出的日志数据");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导出日志");
        fileChooser.setInitialFileName("upload_log_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV文件", "*.csv")
        );

        File file = fileChooser.showSaveDialog(primaryStage);
        if (file != null) {
            try {
                logService.exportLogToCsv(logEntries, file.getAbsolutePath());
                showInfoAlert("导出成功", "日志已成功导出到: " + file.getAbsolutePath());
                logger.info("日志导出成功: {}", file.getAbsolutePath());
            } catch (Exception e) {
                logger.error("日志导出失败: {}", e.getMessage(), e);
                showErrorAlert("导出失败", "日志导出失败: " + e.getMessage());
            }
        }
    }

    /**
     * 处理导出失败表格事件
     */
    @FXML
    private void handleExportFailedRecords() {
        // 筛选失败记录（包括上传失败和比对不匹配）
        List<LogEntry> failedEntries = logService.filterFailedEntries(logEntries);

        if (failedEntries.isEmpty()) {
            showErrorAlert("错误", "没有失败或不匹配的记录可导出");
            return;
        }

        // 统计各种失败类型的数量（避免重复计算）
        long uploadFailedCount = failedEntries.stream()
            .filter(entry -> "失败".equals(entry.getUploadStatus()))
            .count();

        long matchFailedCount = failedEntries.stream()
            .filter(entry -> isMatchStatusFailed(entry.getMatchStatus()) &&
                           !"失败".equals(entry.getUploadStatus())) // 排除已经是上传失败的记录
            .count();

        // 验证Excel数据完整性
        if (!logService.validateExcelData(failedEntries)) {
            showErrorAlert("错误", "失败记录中缺少Excel数据，无法导出完整表格");
            return;
        }

        // 显示文件保存对话框
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导出失败表格");
        fileChooser.setInitialFileName("失败记录_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel文件", "*.xlsx")
        );

        File file = fileChooser.showSaveDialog(primaryStage);
        if (file != null) {
            // 异步执行导出任务
            exportFailedRecordsAsync(failedEntries, file.getAbsolutePath(), uploadFailedCount, matchFailedCount);
        }
    }

    /**
     * 异步导出失败记录
     *
     * @param failedEntries 失败记录列表
     * @param filePath 导出文件路径
     * @param uploadFailedCount 上传失败记录数
     * @param matchFailedCount 比对失败记录数
     */
    private void exportFailedRecordsAsync(List<LogEntry> failedEntries, String filePath,
                                        long uploadFailedCount, long matchFailedCount) {
        Task<Void> exportTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // 获取Excel标题行
                List<String> headers = logService.extractExcelHeaders();

                // 执行导出
                excelService.exportFailedRecordsToExcel(failedEntries, headers, filePath);

                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    StringBuilder message = new StringBuilder();
                    message.append("失败记录已成功导出到: ").append(filePath).append("\n");
                    message.append("共导出 ").append(failedEntries.size()).append(" 条记录\n");

                    if (uploadFailedCount > 0) {
                        message.append("- 上传失败: ").append(uploadFailedCount).append(" 条\n");
                    }
                    if (matchFailedCount > 0) {
                        message.append("- 比对失败: ").append(matchFailedCount).append(" 条");
                    }

                    showInfoAlert("导出成功", message.toString());
                    logger.info("失败记录导出成功: {}, 总记录数: {}, 上传失败: {}, 比对失败: {}",
                               filePath, failedEntries.size(), uploadFailedCount, matchFailedCount);
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    Throwable exception = getException();
                    String errorMsg = exception != null ? exception.getMessage() : "未知错误";
                    logger.error("失败记录导出失败: {}", errorMsg, exception);
                    showErrorAlert("导出失败", "失败记录导出失败: " + errorMsg);
                });
            }
        };

        // 在后台线程执行导出任务
        Thread exportThread = new Thread(exportTask);
        exportThread.setDaemon(true);
        exportThread.start();
    }

    /**
     * 处理导出未上传表格事件
     * 导出所有未勾选的记录（包括未选中的匹配记录和所有不匹配记录）
     */
    @FXML
    private void handleExportUnuploadedRecords() {
        // 筛选未上传记录：未勾选的或者比对不匹配的
        List<LogEntry> unuploadedEntries = logEntries.stream()
            .filter(entry -> !entry.isSelected() || !entry.getMatchStatus().equals("匹配"))
            .collect(java.util.stream.Collectors.toList());

        if (unuploadedEntries.isEmpty()) {
            showErrorAlert("错误", "没有未上传的记录可导出");
            return;
        }

        // 统计未上传记录的类型
        long unselectedCount = unuploadedEntries.stream()
            .filter(entry -> !entry.isSelected() && entry.getMatchStatus().equals("匹配"))
            .count();

        long unmatchedCount = unuploadedEntries.stream()
            .filter(entry -> !entry.getMatchStatus().equals("匹配"))
            .count();

        // 验证Excel数据完整性
        if (!logService.validateExcelData(unuploadedEntries)) {
            showErrorAlert("错误", "未上传记录中缺少Excel数据，无法导出完整表格");
            return;
        }

        // 显示文件保存对话框
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导出未上传表格");
        fileChooser.setInitialFileName("未上传记录_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel文件", "*.xlsx")
        );

        File file = fileChooser.showSaveDialog(primaryStage);
        if (file != null) {
            // 异步执行导出任务
            exportUnuploadedRecordsAsync(unuploadedEntries, file.getAbsolutePath(), unselectedCount, unmatchedCount);
        }
    }

    /**
     * 异步导出未上传记录
     *
     * @param unuploadedEntries 未上传记录列表
     * @param filePath 导出文件路径
     * @param unselectedCount 未勾选但匹配的记录数
     * @param unmatchedCount 不匹配的记录数
     */
    private void exportUnuploadedRecordsAsync(List<LogEntry> unuploadedEntries, String filePath,
                                            long unselectedCount, long unmatchedCount) {
        Task<Void> exportTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // 获取Excel标题行
                List<String> headers = logService.extractExcelHeaders();

                // 执行导出（使用与导出失败表格相同的方法）
                excelService.exportFailedRecordsToExcel(unuploadedEntries, headers, filePath);

                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    StringBuilder message = new StringBuilder();
                    message.append("未上传记录已成功导出到: ").append(filePath).append("\n");
                    message.append("共导出 ").append(unuploadedEntries.size()).append(" 条记录\n");

                    if (unselectedCount > 0) {
                        message.append("- 未勾选: ").append(unselectedCount).append(" 条\n");
                    }
                    if (unmatchedCount > 0) {
                        message.append("- 不匹配: ").append(unmatchedCount).append(" 条");
                    }

                    showInfoAlert("导出成功", message.toString());
                    logger.info("未上传记录导出成功: {}, 总记录数: {}, 未勾选: {}, 不匹配: {}",
                               filePath, unuploadedEntries.size(), unselectedCount, unmatchedCount);
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    Throwable exception = getException();
                    String errorMsg = exception != null ? exception.getMessage() : "未知错误";
                    logger.error("未上传记录导出失败: {}", errorMsg, exception);
                    showErrorAlert("导出失败", "未上传记录导出失败: " + errorMsg);
                });
            }
        };

        // 在后台线程执行导出任务
        Thread exportThread = new Thread(exportTask);
        exportThread.setDaemon(true);
        exportThread.start();
    }

    /**
     * 开始上传任务
     * 修改：只上传已选中的匹配记录
     */
    private void startUploadTask() {
        logger.info("开始真实文件上传任务");

        // 获取选择的图片类型
        String imageType = getSelectedImageType();
        logger.info("选择的图片类型: {}", imageType);

        // 过滤出匹配成功且被选中的条目
        List<LogEntry> matchedEntries = logEntries.stream()
                .filter(entry -> "匹配".equals(entry.getMatchStatus()))
                .filter(LogEntry::isSelected)  // 新增：只上传选中的
                .collect(java.util.stream.Collectors.toList());

        if (matchedEntries.isEmpty()) {
            Platform.runLater(() -> {
                uploadStatusLabel.setText("没有匹配且被选中的图片可上传");
                uploadStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
                isUploading = false;
                updateUIState();
            });
            return;
        }

        logger.info("开始上传 {} 个选中的匹配图片文件", matchedEntries.size());

        Task<Void> uploadTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                int total = matchedEntries.size();
                java.util.concurrent.atomic.AtomicInteger completed = new java.util.concurrent.atomic.AtomicInteger(0);
                java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0); // 成功上传计数器

                // 初始化进度条显示
                Platform.runLater(() -> {
                    updateProgressBarText(0, total);
                    uploadProgressBar.setProgress(0.0);
                });

                for (int i = 0; i < total && !isCancelled(); i++) {
                    // 检查暂停状态
                    if (isPaused) {
                        while (isPaused && !isCancelled()) {
                            Thread.sleep(100);
                        }
                    }

                    final int index = i;
                    final LogEntry entry = matchedEntries.get(index);

                    // 检查文件路径是否存在
                    if (entry.getFilePath() == null || entry.getFilePath().isEmpty()) {
                        final int currentCompleted = completed.incrementAndGet();
                        Platform.runLater(() -> {
                            entry.setUploadStatus("失败");
                            entry.setProgress("0%");
                            entry.setMessage("文件路径为空");
                            updateStatistics();
                            updateProgressBarText(successCount.get(), total);
                            uploadProgressBar.setProgress((double) currentCompleted / total);
                            refreshTableView(); // 强制刷新表格视图
                        });
                        continue;
                    }

                    // 更新UI状态为上传中
                    Platform.runLater(() -> {
                        entry.setUploadStatus("上传中");
                        entry.setProgress("0%");
                        entry.setMessage("正在上传...");
                        refreshTableView(); // 强制刷新表格视图
                    });

                    try {
                        // 创建文件对象
                        File imageFile = new File(entry.getFilePath());
                        if (!imageFile.exists() || !imageFile.isFile()) {
                            final int currentCompleted = completed.incrementAndGet();
                            Platform.runLater(() -> {
                                entry.setUploadStatus("失败");
                                entry.setProgress("0%");
                                entry.setMessage("文件不存在: " + entry.getFilePath());
                                updateStatistics();
                                updateProgressBarText(successCount.get(), total);
                                uploadProgressBar.setProgress((double) currentCompleted / total);
                                refreshTableView(); // 强制刷新表格视图
                            });
                            continue;
                        }

                        logger.debug("开始上传文件: {} -> {}", entry.getFilename(), entry.getProductCode());

                        // 调用真实的文件上传服务，如果是花型图则传递Excel数据
                        java.util.concurrent.CompletableFuture<java.util.Map<String, Object>> uploadFuture;
                        if ("1".equals(imageType) && entry.getExcelRowData() != null) {
                            // 花型图上传，传递Excel数据用于同步插入pic_info表
                            uploadFuture = fileUploadService.uploadSingleFile(imageFile, entry.getProductCode(),
                                imageType, authService.getCurrentUsername(), entry.getExcelRowData());
                            logger.debug("花型图上传，包含Excel数据: {}", entry.getProductCode());
                        } else {
                            // 配色图上传或无Excel数据的上传
                            uploadFuture = fileUploadService.uploadSingleFile(imageFile, entry.getProductCode(),
                                imageType, authService.getCurrentUsername());
                        }

                        // 异步处理上传结果，提高实时性
                        uploadFuture.whenComplete((uploadResult, throwable) -> {
                            final int currentCompleted = completed.incrementAndGet();

                            Platform.runLater(() -> {
                                if (throwable != null) {
                                    // 处理异常情况
                                    entry.setUploadStatus("失败");
                                    entry.setProgress("0%");
                                    entry.setMessage("上传异常: " + throwable.getMessage());
                                    logger.error("文件上传异常: {}, 错误: {}", entry.getFilename(), throwable.getMessage(), throwable);
                                } else if (uploadResult != null && uploadResult.containsKey("success")) {
                                    Boolean success = (Boolean) uploadResult.get("success");
                                    if (Boolean.TRUE.equals(success)) {
                                        entry.setUploadStatus("成功");
                                        entry.setProgress("100%");
                                        entry.setMessage("上传成功");
                                        successCount.incrementAndGet(); // 增加成功计数
                                        logger.info("文件上传成功: {}", entry.getFilename());
                                    } else {
                                        entry.setUploadStatus("失败");
                                        entry.setProgress("0%");
                                        String errorMsg = (String) uploadResult.getOrDefault("message", "上传失败");
                                        entry.setMessage(errorMsg);
                                        logger.warn("文件上传失败: {}, 错误: {}", entry.getFilename(), errorMsg);
                                    }
                                } else {
                                    entry.setUploadStatus("失败");
                                    entry.setProgress("0%");
                                    entry.setMessage("上传响应异常");
                                    logger.error("文件上传响应异常: {}", entry.getFilename());
                                }

                                // 实时更新进度条和统计信息
                                updateStatistics();
                                updateProgressBarText(successCount.get(), total);
                                uploadProgressBar.setProgress((double) currentCompleted / total);
                                refreshTableView(); // 强制刷新表格视图
                            });
                        });

                        // 等待当前上传完成再继续下一个（保持顺序但提高响应性）
                        uploadFuture.get();

                    } catch (Exception e) {
                        final int currentCompleted = completed.incrementAndGet();
                        logger.error("文件上传异常: {}, 错误: {}", entry.getFilename(), e.getMessage(), e);

                        Platform.runLater(() -> {
                            entry.setUploadStatus("失败");
                            entry.setProgress("0%");
                            entry.setMessage("上传异常: " + e.getMessage());
                            updateStatistics();
                            updateProgressBarText(successCount.get(), total);
                            uploadProgressBar.setProgress((double) currentCompleted / total);
                            refreshTableView(); // 强制刷新表格视图
                        });
                    }
                }

                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    isUploading = false;
                    isPaused = false;
                    updateUIState();
                    
                    // 统计上传结果
                    long uploadedSuccess = matchedEntries.stream()
                        .filter(entry -> "成功".equals(entry.getUploadStatus()))
                        .count();
                    long uploadedFailed = matchedEntries.stream()
                        .filter(entry -> "失败".equals(entry.getUploadStatus()))
                        .count();
                    
                    String resultMessage = String.format("上传完成 ✅\n成功: %d 条，失败: %d 条", 
                        uploadedSuccess, uploadedFailed);
                    uploadStatusLabel.setText(resultMessage);
                    uploadStatusLabel.setStyle("-fx-text-fill: #27ae60;");
                    
                    logger.info("上传任务完成 - 成功: {} 条，失败: {} 条", uploadedSuccess, uploadedFailed);
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    isUploading = false;
                    isPaused = false;
                    updateUIState();
                    uploadStatusLabel.setText("上传失败");
                    uploadStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
                    logger.error("上传任务失败: {}", getException().getMessage(), getException());
                });
            }
        };

        new Thread(uploadTask).start();
    }

    /**
     * 设置主舞台
     */
    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (timeUpdateTimer != null) {
            timeUpdateTimer.cancel();
        }
        logger.info("主界面控制器资源清理完成");
    }

    /**
     * 判断是否为失败或不匹配的条目
     *
     * @param entry 日志条目
     * @return true表示是失败或不匹配的条目
     */
    private boolean isFailedOrMismatchedEntry(LogEntry entry) {
        if (entry == null) {
            return false;
        }

        // 检查上传状态是否失败
        boolean uploadFailed = "失败".equals(entry.getUploadStatus());

        // 检查比对状态是否失败
        boolean matchFailed = isMatchStatusFailed(entry.getMatchStatus());

        return uploadFailed || matchFailed;
    }

    /**
     * 判断比对状态是否为失败状态
     *
     * @param matchStatus 比对状态
     * @return true表示比对失败
     */
    private boolean isMatchStatusFailed(String matchStatus) {
        if (matchStatus == null) {
            return false;
        }

        return "不匹配".equals(matchStatus) ||
               "缺少图片".equals(matchStatus) ||
               "缺少数据".equals(matchStatus);
    }

    /**
     * 判断是否为匹配的条目（不考虑上传状态）
     *
     * @param entry 日志条目
     * @return true表示比对状态为匹配
     */
    private boolean isMatchedEntry(LogEntry entry) {
        if (entry == null) {
            return false;
        }

        // 只检查比对状态是否匹配，不考虑上传状态
        return "匹配".equals(entry.getMatchStatus());
    }

    /**
     * 判断是否为匹配成功的条目（上传成功且匹配成功）
     *
     * @param entry 日志条目
     * @return true表示匹配成功
     */
    private boolean isMatchedSuccessEntry(LogEntry entry) {
        if (entry == null) {
            return false;
        }

        // 检查上传状态是否成功且比对状态是否匹配
        boolean uploadSuccess = "成功".equals(entry.getUploadStatus());
        boolean matchSuccess = "匹配".equals(entry.getMatchStatus());

        return uploadSuccess && matchSuccess;
    }

    /**
     * 处理导出匹配图片事件
     */
    @FXML
    private void handleExportMatchedImages() {
        logger.info("开始导出匹配图片");

        // 筛选匹配的记录（只要匹配状态为"匹配"即可，不需要上传成功）
        List<LogEntry> matchedEntries = logEntries.stream()
            .filter(this::isMatchedEntry)
            .filter(entry -> entry.getFilePath() != null && !entry.getFilePath().isEmpty())
            .collect(Collectors.toList());

        if (matchedEntries.isEmpty()) {
            showInfoAlert("提示", "没有找到匹配的图片记录");
            return;
        }

        // 获取图片文件夹路径
        String imageFolderPath = folderPathField.getText();
        if (imageFolderPath.isEmpty()) {
            showErrorAlert("错误", "请先选择图片文件夹");
            return;
        }

        // 创建目标文件夹
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String targetFolderName = "匹配图片_" + timestamp;
        File targetFolder = new File(imageFolderPath, targetFolderName);

        try {
            if (!targetFolder.exists() && !targetFolder.mkdirs()) {
                showErrorAlert("错误", "无法创建目标文件夹: " + targetFolder.getAbsolutePath());
                return;
            }
        } catch (Exception e) {
            logger.error("创建目标文件夹失败", e);
            showErrorAlert("错误", "创建目标文件夹失败: " + e.getMessage());
            return;
        }

        // 异步执行图片复制
        exportMatchedImagesButton.setDisable(true);
        exportMatchedImagesButton.setText("导出中...");

        Task<Void> exportTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                copyMatchedImages(matchedEntries, targetFolder);
                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    exportMatchedImagesButton.setDisable(false);
                    exportMatchedImagesButton.setText("导出匹配图片");

                    String message = String.format("成功导出 %d 张匹配图片到:\n%s\n\n是否打开目标文件夹？",
                                                  matchedEntries.size(), targetFolder.getAbsolutePath());

                    if (showConfirmAlert("导出成功", message)) {
                        try {
                            Desktop.getDesktop().open(targetFolder);
                        } catch (Exception e) {
                            logger.warn("无法打开文件夹: {}", e.getMessage());
                        }
                    }

                    logger.info("匹配图片导出完成: {} 张图片", matchedEntries.size());
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    exportMatchedImagesButton.setDisable(false);
                    exportMatchedImagesButton.setText("导出匹配图片");

                    Throwable exception = getException();
                    logger.error("导出匹配图片失败", exception);
                    showErrorAlert("导出失败", "导出匹配图片时发生错误: " + exception.getMessage());
                });
            }
        };

        new Thread(exportTask).start();
    }

    /**
     * 复制匹配的图片文件到目标文件夹
     *
     * @param matchedEntries 匹配的记录列表
     * @param targetFolder 目标文件夹
     * @throws Exception 复制过程中的异常
     */
    private void copyMatchedImages(List<LogEntry> matchedEntries, File targetFolder) throws Exception {
        logger.info("开始复制 {} 张匹配图片到: {}", matchedEntries.size(), targetFolder.getAbsolutePath());

        int successCount = 0;
        int skipCount = 0;

        for (int i = 0; i < matchedEntries.size(); i++) {
            LogEntry entry = matchedEntries.get(i);
            String sourceFilePath = entry.getFilePath();

            if (sourceFilePath == null || sourceFilePath.isEmpty()) {
                logger.warn("跳过空文件路径的记录: {}", entry.getFilename());
                skipCount++;
                continue;
            }

            File sourceFile = new File(sourceFilePath);
            if (!sourceFile.exists()) {
                logger.warn("源文件不存在，跳过: {}", sourceFilePath);
                skipCount++;
                continue;
            }

            // 获取文件名
            String fileName = sourceFile.getName();
            File targetFile = new File(targetFolder, fileName);

            // 如果目标文件已存在，添加序号后缀
            if (targetFile.exists()) {
                String baseName = fileName;
                String extension = "";
                int dotIndex = fileName.lastIndexOf('.');
                if (dotIndex > 0) {
                    baseName = fileName.substring(0, dotIndex);
                    extension = fileName.substring(dotIndex);
                }

                int counter = 1;
                do {
                    String newFileName = baseName + "_" + counter + extension;
                    targetFile = new File(targetFolder, newFileName);
                    counter++;
                } while (targetFile.exists());
            }

            try {
                // 使用NIO复制文件
                Path sourcePath = sourceFile.toPath();
                Path targetPath = targetFile.toPath();
                Files.copy(sourcePath, targetPath, StandardCopyOption.COPY_ATTRIBUTES);

                successCount++;
                logger.debug("复制成功: {} -> {}", sourceFile.getName(), targetFile.getName());

                // 更新进度（如果需要的话）
                final int currentIndex = i + 1;
                final int total = matchedEntries.size();
                Platform.runLater(() -> {
                    // 这里可以更新进度条，如果有的话
                    logger.debug("复制进度: {}/{}", currentIndex, total);
                });

            } catch (Exception e) {
                logger.error("复制文件失败: {} -> {}", sourceFile.getName(), targetFile.getName(), e);
                throw new Exception("复制文件失败: " + sourceFile.getName() + " - " + e.getMessage(), e);
            }
        }

        logger.info("图片复制完成: 成功 {} 张，跳过 {} 张", successCount, skipCount);

        if (successCount == 0) {
            throw new Exception("没有成功复制任何图片文件");
        }
    }

    /**
     * 加载保存的用户凭据并自动填充
     */
    private void loadSavedCredentials() {
        try {
            if (credentialManager == null) {
                logger.warn("凭据管理器未初始化，跳过自动填充");
                return;
            }

            CredentialManager.SavedCredentials savedCredentials = credentialManager.loadCredentials();

            if (savedCredentials != null) {
                // 填充用户名和密码
                usernameField.setText(savedCredentials.getUsername());
                passwordField.setText(savedCredentials.getPassword());
                rememberPasswordCheckBox.setSelected(savedCredentials.isRememberPassword());

                logger.info("已自动填充保存的用户凭据: username={}", savedCredentials.getUsername());
            } else {
                // 设置记住密码选项的默认状态
                rememberPasswordCheckBox.setSelected(credentialManager.isRememberPassword());
                logger.debug("没有找到保存的用户凭据");
            }

        } catch (Exception e) {
            logger.error("加载保存的用户凭据失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 保存用户凭据
     *
     * @param username 用户名
     * @param password 密码
     */
    private void saveCredentials(String username, String password) {
        try {
            if (credentialManager == null) {
                logger.warn("凭据管理器未初始化，跳过保存凭据");
                return;
            }

            boolean rememberPassword = rememberPasswordCheckBox.isSelected();
            credentialManager.saveCredentials(username, password, rememberPassword);

            if (rememberPassword) {
                logger.info("用户凭据已保存: username={}", username);
            } else {
                logger.info("用户选择不记住密码，已清除保存的凭据");
            }

        } catch (Exception e) {
            logger.error("保存用户凭据失败: username={}, error={}", username, e.getMessage(), e);
        }
    }

    /**
     * 清除保存的用户凭据
     */
    private void clearSavedCredentials() {
        try {
            if (credentialManager != null) {
                credentialManager.clearCredentials();
                logger.info("已清除所有保存的用户凭据");
            }
        } catch (Exception e) {
            logger.error("清除保存的用户凭据失败: {}", e.getMessage(), e);
        }
    }
}
