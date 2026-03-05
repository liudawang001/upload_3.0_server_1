package com.mls.upload.server.controller;

import com.mls.upload.server.dto.ApiResponse;
import com.mls.upload.server.entity.DataVector;
import com.mls.upload.server.entity.PicInfo;
import com.mls.upload.server.service.DataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据管理控制器
 * 处理数据查询和管理相关的REST API
 *
 * @author MLS Development Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/data")
@CrossOrigin(origins = "*") // 允许跨域请求
public class DataController {

    private static final Logger logger = LoggerFactory.getLogger(DataController.class);

    @Autowired
    private DataService dataService;

    /**
     * 获取数据统计信息
     *
     * @return 统计信息
     */
    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> getStatistics() {
        logger.info("获取数据统计信息");

        try {
            Map<String, Object> statistics = dataService.getDataStatistics();
            return ApiResponse.success("获取统计信息成功", statistics);

        } catch (Exception e) {
            logger.error("获取统计信息异常: error={}", e.getMessage(), e);
            return ApiResponse.error("获取统计信息异常");
        }
    }

    /**
     * 根据商品编号查询图片信息
     *
     * @param productCode 商品编号
     * @return 图片信息列表
     */
    @GetMapping("/pic-info/by-product-code")
    public ApiResponse<List<PicInfo>> getPicInfoByProductCode(@RequestParam String productCode) {
        logger.info("根据商品编号查询图片信息: productCode={}", productCode);

        try {
            List<PicInfo> picInfos = dataService.getPicInfoByProductCode(productCode);

            if (picInfos != null) {
                return ApiResponse.success("查询图片信息成功", picInfos);
            } else {
                return ApiResponse.error("查询图片信息失败");
            }

        } catch (Exception e) {
            logger.error("查询图片信息异常: productCode={}, error={}", productCode, e.getMessage(), e);
            return ApiResponse.error("查询图片信息异常");
        }
    }

    /**
     * 获取特征向量数据
     *
     * @param filename 文件名
     * @return 特征向量数据
     */
    @GetMapping("/feature-vector")
    public ApiResponse<DataVector> getFeatureVector(@RequestParam String filename) {
        logger.info("获取特征向量数据: filename={}", filename);

        try {
            DataVector vector = dataService.getFeatureVector(filename);

            if (vector != null) {
                return ApiResponse.success("获取特征向量成功", vector);
            } else {
                return ApiResponse.notFound("特征向量不存在");
            }

        } catch (Exception e) {
            logger.error("获取特征向量异常: filename={}, error={}", filename, e.getMessage(), e);
            return ApiResponse.error("获取特征向量异常");
        }
    }

    /**
     * 保存特征向量数据
     *
     * @param filename 文件名
     * @param colorVector color特征向量（Base64编码）
     * @param glcmVector glcm特征向量（Base64编码）
     * @param lbpVector lbp特征向量（Base64编码）
     * @param vggVector vgg特征向量（Base64编码）
     * @param vitVector vit特征向量（Base64编码）
     * @return 保存结果
     */
    @PostMapping("/feature-vector")
    public ApiResponse<Void> saveFeatureVector(@RequestParam String filename,
                                              @RequestParam(required = false) String colorVector,
                                              @RequestParam(required = false) String glcmVector,
                                              @RequestParam(required = false) String lbpVector,
                                              @RequestParam(required = false) String vggVector,
                                              @RequestParam(required = false) String vitVector) {
        logger.info("保存特征向量数据: filename={}", filename);

        try {
            // 解码Base64数据
            byte[] colorData = decodeBase64(colorVector);
            byte[] glcmData = decodeBase64(glcmVector);
            byte[] lbpData = decodeBase64(lbpVector);
            byte[] vggData = decodeBase64(vggVector);
            byte[] vitData = decodeBase64(vitVector);

            boolean success = dataService.saveFeatureVector(
                filename, colorData, glcmData, lbpData, vggData, vitData
            );

            if (success) {
                logger.info("特征向量保存成功: filename={}", filename);
                return ApiResponse.success("特征向量保存成功");
            } else {
                logger.warn("特征向量保存失败: filename={}", filename);
                return ApiResponse.error("特征向量保存失败");
            }

        } catch (Exception e) {
            logger.error("保存特征向量异常: filename={}, error={}", filename, e.getMessage(), e);
            return ApiResponse.error("保存特征向量异常");
        }
    }

    /**
     * 请求特征提取
     *
     * @param filename 文件名
     * @param imagePath 图片路径
     * @return 请求结果
     */
    @PostMapping("/extract-features")
    public ApiResponse<Void> extractFeatures(@RequestParam String filename,
                                            @RequestParam String imagePath) {
        logger.info("请求特征提取: filename={}, imagePath={}", filename, imagePath);

        try {
            boolean success = dataService.requestFeatureExtraction(filename, imagePath);

            if (success) {
                logger.info("特征提取请求成功: filename={}", filename);
                return ApiResponse.success("特征提取请求已提交");
            } else {
                logger.warn("特征提取请求失败: filename={}", filename);
                return ApiResponse.error("特征提取请求失败");
            }

        } catch (Exception e) {
            logger.error("特征提取请求异常: filename={}, error={}", filename, e.getMessage(), e);
            return ApiResponse.error("特征提取请求异常");
        }
    }

    /**
     * 删除特征向量数据
     *
     * @param filename 文件名
     * @return 删除结果
     */
    @DeleteMapping("/feature-vector")
    public ApiResponse<Void> deleteFeatureVector(@RequestParam String filename) {
        logger.info("删除特征向量数据: filename={}", filename);

        try {
            boolean success = dataService.deleteFeatureVector(filename);

            if (success) {
                logger.info("特征向量删除成功: filename={}", filename);
                return ApiResponse.success("特征向量删除成功");
            } else {
                logger.warn("特征向量删除失败: filename={}", filename);
                return ApiResponse.error("特征向量删除失败，可能不存在");
            }

        } catch (Exception e) {
            logger.error("删除特征向量异常: filename={}, error={}", filename, e.getMessage(), e);
            return ApiResponse.error("删除特征向量异常");
        }
    }

    /**
     * 健康检查
     *
     * @return 健康状态
     */
    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("数据服务运行正常", "OK");
    }

    /**
     * 解码Base64数据
     *
     * @param base64Data Base64编码的数据
     * @return 解码后的字节数组
     */
    private byte[] decodeBase64(String base64Data) {
        if (base64Data == null || base64Data.trim().isEmpty()) {
            return null;
        }

        try {
            return java.util.Base64.getDecoder().decode(base64Data);
        } catch (Exception e) {
            logger.warn("Base64解码失败: {}", e.getMessage());
            return null;
        }
    }
}
