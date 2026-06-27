package com.mls.upload.server.service;

import com.mls.upload.server.dto.UploadRequest;
import com.mls.upload.server.dto.UploadResponse;
import com.mls.upload.server.entity.ImageUpload;
import com.mls.upload.server.entity.PicInfo;
import com.mls.upload.server.mapper.ImageUploadMapper;
import com.mls.upload.server.mapper.PicInfoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * 文件服务
 * 处理文件上传、存储和管理业务逻辑
 *
 * @author MLS Development Team
 * @version 1.0.0
 */
@Service
public class FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    @Autowired
    private ImageUploadMapper imageUploadMapper;

    @Autowired
    private PicInfoMapper picInfoMapper;

    @Value("${app.file.upload.flower-path:/data/MLS_Pic/mls_pic}")
    private String flowerImagePath;

    @Value("${app.file.upload.color-path:/data/MLS_Pic/mls_pic/color_scheme}")
    private String colorImagePath;

    @Value("${app.file.upload.max-file-size:50}") // 50MB
    private long maxFileSizeMB;

    /**
     * 处理文件上传
     *
     * @param uploadRequest 上传请求
     * @return 上传响应
     */
    public UploadResponse uploadFile(UploadRequest uploadRequest) {
        logger.info("开始处理文件上传: filename={}, imageType={}, username={}",
                   uploadRequest.getFilename(), uploadRequest.getImageType(), uploadRequest.getUsername());

        try {
            // 参数验证
            if (!validateUploadRequest(uploadRequest)) {
                return UploadResponse.failed(uploadRequest.getFilename(), "上传参数验证失败");
            }

            // 解码Base64数据
            byte[] fileData = decodeBase64Data(uploadRequest.getBase64Data());
            if (fileData == null) {
                return UploadResponse.failed(uploadRequest.getFilename(), "Base64数据解码失败");
            }

            // 检查文件大小
            long maxFileSize = maxFileSizeMB * 1024 * 1024; // 转换为字节
            if (fileData.length > maxFileSize) {
                return UploadResponse.failed(uploadRequest.getFilename(),
                    String.format("文件大小超过限制，最大允许%dMB", maxFileSizeMB));
            }

            // 确定存储路径
            String targetPath = getTargetPath(uploadRequest.getImageType());
            if (targetPath == null) {
                return UploadResponse.failed(uploadRequest.getFilename(), "无效的图片类型");
            }

            // 创建目录
            File targetDir = new File(targetPath);
            if (!targetDir.exists() && !targetDir.mkdirs()) {
                logger.error("创建目录失败: {}", targetPath);
                return UploadResponse.failed(uploadRequest.getFilename(), "创建存储目录失败");
            }

            // 生成文件路径
            String filePath = generateFilePath(targetPath, uploadRequest.getFilename(), uploadRequest.getImageType());

            // 保存文件
            if (!saveFile(fileData, filePath)) {
                return UploadResponse.failed(uploadRequest.getFilename(), "文件保存失败");
            }

            // 记录上传信息
            recordUpload(uploadRequest, filePath, fileData.length);

            // 保存图片信息（如果有）
            if (uploadRequest.getPicInfo() != null) {
                savePicInfo(uploadRequest.getPicInfo(), uploadRequest.getFilename(), filePath);
            }

            // 如果是花型图且有Excel数据，自动创建PicInfo记录
            if (uploadRequest.isFlowerImage() && uploadRequest.getExcelData() != null) {
                createPicInfoFromExcelData(uploadRequest, filePath);
            }

            logger.info("文件上传成功: filename={}, path={}, size={}",
                       uploadRequest.getFilename(), filePath, fileData.length);

            return UploadResponse.success(
                uploadRequest.getFilename(),
                filePath,
                (long) fileData.length,
                uploadRequest.getImageType()
            );

        } catch (Exception e) {
            logger.error("文件上传异常: filename={}, error={}",
                        uploadRequest.getFilename(), e.getMessage(), e);
            return UploadResponse.failed(uploadRequest.getFilename(), "文件上传异常: " + e.getMessage());
        }
    }

    /**
     * 验证上传请求参数
     *
     * @param request 上传请求
     * @return 验证结果
     */
    private boolean validateUploadRequest(UploadRequest request) {
        if (request == null) {
            logger.warn("上传请求为空");
            return false;
        }

        if (!StringUtils.hasText(request.getFilename())) {
            logger.warn("文件名为空");
            return false;
        }

        if (!StringUtils.hasText(request.getImageType()) ||
            (!ImageUpload.IMAGE_TYPE_FLOWER.equals(request.getImageType()) &&
             !ImageUpload.IMAGE_TYPE_COLOR.equals(request.getImageType()))) {
            logger.warn("无效的图片类型: {}", request.getImageType());
            return false;
        }

        if (!StringUtils.hasText(request.getBase64Data())) {
            logger.warn("Base64数据为空");
            return false;
        }

        if (!StringUtils.hasText(request.getUsername())) {
            logger.warn("用户名为空");
            return false;
        }

        return true;
    }

    /**
     * 解码Base64数据
     *
     * @param base64Data Base64编码的数据
     * @return 解码后的字节数组
     */
    private byte[] decodeBase64Data(String base64Data) {
        try {
            // 移除可能的数据URL前缀（如：data:image/jpeg;base64,）
            if (base64Data.contains(",")) {
                base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
            }

            return Base64.getDecoder().decode(base64Data);
        } catch (Exception e) {
            logger.error("Base64解码失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 根据图片类型获取目标路径
     *
     * @param imageType 图片类型
     * @return 目标路径
     */
    private String getTargetPath(String imageType) {
        if (ImageUpload.IMAGE_TYPE_FLOWER.equals(imageType)) {
            return flowerImagePath;
        } else if (ImageUpload.IMAGE_TYPE_COLOR.equals(imageType)) {
            return colorImagePath;
        }
        return null;
    }

    /**
     * 生成文件完整路径
     *
     * @param targetPath 目标目录
     * @param filename 文件名
     * @param imageType 图片类型
     * @return 完整文件路径
     */
    private String generateFilePath(String targetPath, String filename, String imageType) {
        // 根据图片类型采用不同的命名策略
        if (ImageUpload.IMAGE_TYPE_FLOWER.equals(imageType)) {
            // 花型图：保持原始文件名不变，允许覆盖
            return targetPath + File.separator + filename;
        } else if (ImageUpload.IMAGE_TYPE_COLOR.equals(imageType)) {
            // 配色图：添加时间戳避免文件名冲突
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String nameWithoutExt = filename.substring(0, filename.lastIndexOf('.'));
            String extension = filename.substring(filename.lastIndexOf('.'));

            String uniqueFilename = nameWithoutExt + "_" + timestamp + extension;
            return targetPath + File.separator + uniqueFilename;
        } else {
            // 默认策略：添加时间戳
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String nameWithoutExt = filename.substring(0, filename.lastIndexOf('.'));
            String extension = filename.substring(filename.lastIndexOf('.'));

            String uniqueFilename = nameWithoutExt + "_" + timestamp + extension;
            return targetPath + File.separator + uniqueFilename;
        }
    }

    /**
     * 保存文件到磁盘
     *
     * @param fileData 文件数据
     * @param filePath 文件路径
     * @return 保存是否成功
     */
    private boolean saveFile(byte[] fileData, String filePath) {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(fileData);
            fos.flush();
            return true;
        } catch (IOException e) {
            logger.error("文件保存失败: path={}, error={}", filePath, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 记录上传信息到数据库
     *
     * @param request 上传请求
     * @param filePath 文件路径
     * @param fileSize 文件大小
     */
    private void recordUpload(UploadRequest request, String filePath, int fileSize) {
        try {
            ImageUpload upload = new ImageUpload();
            upload.setFilename(request.getFilename());
            upload.setImageType(request.getImageType());
            upload.setUsername(request.getUsername());
            upload.setCreateTime(LocalDateTime.now());

            imageUploadMapper.insert(upload);

            logger.debug("上传记录保存成功: filename={}", request.getFilename());
        } catch (Exception e) {
            logger.error("保存上传记录失败: filename={}, error={}",
                        request.getFilename(), e.getMessage(), e);
        }
    }

    /**
     * 保存图片信息到数据库
     *
     * @param picInfo 图片信息
     * @param filename 文件名
     * @param filePath 文件路径
     */
    private void savePicInfo(PicInfo picInfo, String filename, String filePath) {
        try {
            picInfo.setFilename(filename);
            picInfo.setFilepath(filePath);
            picInfo.setCreateTime(LocalDateTime.now());

            picInfoMapper.insert(picInfo);

            logger.debug("图片信息保存成功: filename={}", filename);
        } catch (Exception e) {
            logger.error("保存图片信息失败: filename={}, error={}",
                        filename, e.getMessage(), e);
        }
    }

    /**
     * 检查文件是否已存在
     *
     * @param filename 文件名
     * @return 文件是否存在
     */
    public boolean fileExists(String filename) {
        if (!StringUtils.hasText(filename)) {
            return false;
        }

        try {
            ImageUpload upload = imageUploadMapper.findByFilename(filename);
            return upload != null;
        } catch (Exception e) {
            logger.error("检查文件存在性异常: filename={}, error={}", filename, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 删除文件
     *
     * @param filename 文件名
     * @return 删除是否成功
     */
    public boolean deleteFile(String filename) {
        logger.info("删除文件请求: filename={}", filename);

        try {
            // 查询文件信息
            ImageUpload upload = imageUploadMapper.findByFilename(filename);
            if (upload == null) {
                logger.warn("删除失败: 文件记录不存在, filename={}", filename);
                return false;
            }

            // 删除数据库记录
            imageUploadMapper.deleteById(upload.getId());

            // 删除图片信息记录（如果存在）
            PicInfo picInfo = picInfoMapper.findByFilename(filename);
            if (picInfo != null) {
                picInfoMapper.deleteById(picInfo.getId());
            }

            logger.info("文件删除成功: filename={}", filename);
            return true;

        } catch (Exception e) {
            logger.error("文件删除异常: filename={}, error={}", filename, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 根据Excel数据创建PicInfo记录
     * 用于花型图上传时同步插入pic_info表
     *
     * @param uploadRequest 上传请求
     * @param filePath 文件路径
     */
    private void createPicInfoFromExcelData(UploadRequest uploadRequest, String filePath) {
        try {
            java.util.Map<String, Object> excelData = uploadRequest.getExcelData();
            if (excelData == null || excelData.isEmpty()) {
                logger.warn("Excel数据为空，跳过PicInfo创建: filename={}", uploadRequest.getFilename());
                return;
            }

            // 创建PicInfo对象
            PicInfo picInfo = new PicInfo();

            // 设置基本信息
            // filename填充逻辑：商品编号+.jpg
            String productCode = getStringValue(excelData, "field01");
            if (productCode != null && !productCode.trim().isEmpty()) {
                picInfo.setFilename(productCode.trim() + ".jpg");
            } else {
                picInfo.setFilename(uploadRequest.getFilename());
            }

            picInfo.setFilepath(filePath);
            picInfo.setCreateTime(java.time.LocalDateTime.now());

            // 设置Excel数据字段
            picInfo.setField01(getStringValue(excelData, "field01")); // 商品编号
            picInfo.setField02(getStringValue(excelData, "field02")); // 分类
            picInfo.setField03(getStringValue(excelData, "field03")); // 订单号
            picInfo.setField04(getStringValue(excelData, "field04")); // 面料
            picInfo.setField05(getStringValue(excelData, "field05")); // 客户名
            picInfo.setField06(getStringValue(excelData, "field06")); // 市场
            picInfo.setField07(getStringValue(excelData, "field07")); // 套数
            picInfo.setField08(getStringValue(excelData, "field08")); // SO日期
            picInfo.setField09(getStringValue(excelData, "field09")); // 大货日期
            picInfo.setField10(getStringValue(excelData, "field10")); // 工厂编号
            picInfo.setField11(getStringValue(excelData, "field11")); // 工厂
            picInfo.setField12(getStringValue(excelData, "field12")); // 描稿公司
            picInfo.setField13(getStringValue(excelData, "field13")); // 描稿人员
            picInfo.setField14(getStringValue(excelData, "field14")); // 备注
            picInfo.setField15(getStringValue(excelData, "field15")); // 理单员
            picInfo.setField16(getStringValue(excelData, "field16")); // 排序

            // 插入数据库
            int result = picInfoMapper.insert(picInfo);
            if (result > 0) {
                logger.info("花型图Excel数据插入pic_info成功: filename={}, productCode={}, id={}",
                           uploadRequest.getFilename(), productCode, picInfo.getId());
            } else {
                logger.warn("花型图Excel数据插入pic_info失败: filename={}, productCode={}",
                           uploadRequest.getFilename(), productCode);
            }

        } catch (Exception e) {
            logger.error("创建PicInfo记录异常: filename={}, error={}",
                        uploadRequest.getFilename(), e.getMessage(), e);
        }
    }

    /**
     * 从Map中获取字符串值
     *
     * @param map 数据Map
     * @param key 键
     * @return 字符串值，如果不存在或为null则返回null
     */
    private String getStringValue(java.util.Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        String stringValue = value.toString().trim();
        return stringValue.isEmpty() ? null : stringValue;
    }
}
