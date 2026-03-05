package com.mls.upload.server.controller;

import com.mls.upload.server.dto.ApiResponse;
import com.mls.upload.server.dto.UploadRequest;
import com.mls.upload.server.dto.UploadResponse;
import com.mls.upload.server.entity.ImageUpload;
import com.mls.upload.server.service.DataService;
import com.mls.upload.server.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 文件上传控制器
 * 处理图片文件上传相关的REST API
 *
 * @author MLS Development Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/upload")
@CrossOrigin(origins = "*") // 允许跨域请求
public class FileUploadController {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);

    @Autowired
    private FileService fileService;

    @Autowired
    private DataService dataService;

    /**
     * 上传单个文件
     *
     * @param uploadRequest 上传请求
     * @return 上传响应
     */
    @PostMapping("/single")
    public ApiResponse<UploadResponse> uploadSingle(@Valid @RequestBody UploadRequest uploadRequest) {
        logger.info("收到单文件上传请求: filename={}, imageType={}, username={}",
                   uploadRequest.getFilename(), uploadRequest.getImageType(), uploadRequest.getUsername());

        try {
            UploadResponse uploadResponse = fileService.uploadFile(uploadRequest);

            if (uploadResponse.isSuccess()) {
                logger.info("文件上传成功: filename={}", uploadRequest.getFilename());

                // 如果是花型图且需要特征提取，异步请求特征提取
                if (uploadRequest.isFlowerImage() && uploadRequest.getNeedFeatureExtraction()) {
                    try {
                        dataService.requestFeatureExtraction(
                            uploadResponse.getFilename(),
                            uploadResponse.getFilePath()
                        );
                    } catch (Exception e) {
                        logger.warn("特征提取请求失败: filename={}, error={}",
                                   uploadRequest.getFilename(), e.getMessage());
                        // 不影响上传结果，只记录警告
                    }
                }

                return ApiResponse.success("文件上传成功", uploadResponse);
            } else {
                logger.warn("文件上传失败: filename={}, error={}",
                           uploadRequest.getFilename(), uploadResponse.getErrorMessage());
                return ApiResponse.error("文件上传失败: " + uploadResponse.getErrorMessage());
            }

        } catch (Exception e) {
            logger.error("文件上传异常: filename={}, error={}",
                        uploadRequest.getFilename(), e.getMessage(), e);
            return ApiResponse.error("文件上传异常，请稍后重试");
        }
    }

    /**
     * 批量上传文件
     *
     * @param uploadRequests 上传请求列表
     * @return 批量上传响应
     */
    @PostMapping("/batch")
    public ApiResponse<List<UploadResponse>> uploadBatch(@Valid @RequestBody List<UploadRequest> uploadRequests) {
        logger.info("收到批量文件上传请求: count={}", uploadRequests.size());

        try {
            List<UploadResponse> responses = new java.util.ArrayList<>();

            for (UploadRequest request : uploadRequests) {
                try {
                    UploadResponse response = fileService.uploadFile(request);
                    responses.add(response);

                    // 如果是花型图且需要特征提取，异步请求特征提取
                    if (request.isFlowerImage() && request.getNeedFeatureExtraction() && response.isSuccess()) {
                        try {
                            dataService.requestFeatureExtraction(response.getFilename(), response.getFilePath());
                        } catch (Exception e) {
                            logger.warn("特征提取请求失败: filename={}, error={}",
                                       request.getFilename(), e.getMessage());
                        }
                    }

                } catch (Exception e) {
                    logger.error("批量上传中单个文件处理异常: filename={}, error={}",
                                request.getFilename(), e.getMessage(), e);
                    responses.add(UploadResponse.failed(request.getFilename(), "上传异常: " + e.getMessage()));
                }
            }

            // 统计结果
            long successCount = responses.stream().filter(UploadResponse::isSuccess).count();
            long failedCount = responses.size() - successCount;

            logger.info("批量上传完成: total={}, success={}, failed={}",
                       responses.size(), successCount, failedCount);

            return ApiResponse.success(
                String.format("批量上传完成，成功%d个，失败%d个", successCount, failedCount),
                responses
            );

        } catch (Exception e) {
            logger.error("批量上传异常: error={}", e.getMessage(), e);
            return ApiResponse.error("批量上传异常，请稍后重试");
        }
    }

    /**
     * 检查文件是否已存在
     *
     * @param filename 文件名
     * @return 检查结果
     */
    @GetMapping("/check-exists")
    public ApiResponse<Boolean> checkFileExists(@RequestParam String filename) {
        logger.info("检查文件存在性: filename={}", filename);

        try {
            boolean exists = fileService.fileExists(filename);
            return ApiResponse.success("检查完成", exists);

        } catch (Exception e) {
            logger.error("检查文件存在性异常: filename={}, error={}", filename, e.getMessage(), e);
            return ApiResponse.error("检查文件存在性异常");
        }
    }

    /**
     * 删除文件
     *
     * @param filename 文件名
     * @return 删除结果
     */
    @DeleteMapping("/delete")
    public ApiResponse<Void> deleteFile(@RequestParam String filename) {
        logger.info("收到删除文件请求: filename={}", filename);

        try {
            boolean success = fileService.deleteFile(filename);

            if (success) {
                logger.info("文件删除成功: filename={}", filename);

                // 同时删除特征向量数据
                try {
                    dataService.deleteFeatureVector(filename);
                } catch (Exception e) {
                    logger.warn("删除特征向量失败: filename={}, error={}", filename, e.getMessage());
                }

                return ApiResponse.success("文件删除成功");
            } else {
                logger.warn("文件删除失败: filename={}", filename);
                return ApiResponse.error("文件删除失败，文件可能不存在");
            }

        } catch (Exception e) {
            logger.error("删除文件异常: filename={}, error={}", filename, e.getMessage(), e);
            return ApiResponse.error("删除文件异常，请稍后重试");
        }
    }

    /**
     * 获取用户上传历史
     *
     * @param username 用户名
     * @return 上传历史
     */
    @GetMapping("/history")
    public ApiResponse<List<ImageUpload>> getUploadHistory(@RequestParam String username) {
        logger.info("获取用户上传历史: username={}", username);

        try {
            List<ImageUpload> history = dataService.getUserUploadHistory(username);

            if (history != null) {
                return ApiResponse.success("获取上传历史成功", history);
            } else {
                return ApiResponse.error("获取上传历史失败");
            }

        } catch (Exception e) {
            logger.error("获取上传历史异常: username={}, error={}", username, e.getMessage(), e);
            return ApiResponse.error("获取上传历史异常");
        }
    }

    /**
     * 健康检查
     *
     * @return 健康状态
     */
    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("文件上传服务运行正常", "OK");
    }
}
