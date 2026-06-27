package com.mls.upload.server.service;

import java.io.File;

/**
 * Docker特征提取服务接口
 * 定义与Docker容器中特征提取算法的交互规范
 * 
 * 根据Docker特征提取方法参考.md的技术分析，该服务负责：
 * 1. 图片数据的Base64编码和Data URI构建
 * 2. 与Docker服务的HTTP通信
 * 3. 特征向量数据的解析和转换
 * 4. 错误处理和重试机制
 * 
 * @author MLS Development Team
 * @version 1.0.0
 */
public interface DockerFeatureExtractionService {

    /**
     * CLIP特征向量数据结构。
     */
    class ClipFeatureVector {
        private final float[] feature;
        private final int featureDim;
        private final String modelName;
        private final boolean rotationAugment;
        private final Float clipTime;

        public ClipFeatureVector(float[] feature, int featureDim, String modelName,
                                 boolean rotationAugment, Float clipTime) {
            this.feature = feature;
            this.featureDim = featureDim;
            this.modelName = modelName;
            this.rotationAugment = rotationAugment;
            this.clipTime = clipTime;
        }

        public float[] getFeature() {
            return feature;
        }

        public int getFeatureDim() {
            return featureDim;
        }

        public String getModelName() {
            return modelName;
        }

        public boolean isRotationAugment() {
            return rotationAugment;
        }

        public Float getClipTime() {
            return clipTime;
        }

        public boolean isValid() {
            return feature != null && feature.length == featureDim && featureDim > 0;
        }

        @Override
        public String toString() {
            return "ClipFeatureVector{" +
                    "featureDim=" + featureDim +
                    ", modelName='" + modelName + '\'' +
                    ", rotationAugment=" + rotationAugment +
                    ", clipTime=" + clipTime +
                    '}';
        }
    }

    /**
     * 多特征向量数据结构
     * 包含Docker服务返回的5种特征向量
     */
    class MultiFeatureVector {
        private final float[] color;  // Color特征向量 (256维)
        private final float[] glcm;   // GLCM特征向量 (72维)
        private final float[] lbp;    // LBP特征向量 (256维)
        private final float[] vgg;    // VGG特征向量 (512维)
        private final float[] vit;    // VIT特征向量 (768维)

        public MultiFeatureVector(float[] color, float[] glcm, float[] lbp, float[] vgg, float[] vit) {
            this.color = color;
            this.glcm = glcm;
            this.lbp = lbp;
            this.vgg = vgg;
            this.vit = vit;
        }

        public float[] getColor() {
            return color;
        }

        public float[] getGlcm() {
            return glcm;
        }

        public float[] getLbp() {
            return lbp;
        }

        public float[] getVgg() {
            return vgg;
        }

        public float[] getVit() {
            return vit;
        }

        /**
         * 验证特征向量的完整性
         * 
         * @return true表示所有特征向量都存在且维度正确
         */
        public boolean isValid() {
            return color != null && color.length == 256 &&
                   glcm != null && glcm.length == 72 &&
                   lbp != null && lbp.length == 256 &&
                   vgg != null && vgg.length == 512 &&
                   vit != null && vit.length == 768;
        }

        @Override
        public String toString() {
            return "MultiFeatureVector{" +
                    "colorDim=" + (color != null ? color.length : 0) +
                    ", glcmDim=" + (glcm != null ? glcm.length : 0) +
                    ", lbpDim=" + (lbp != null ? lbp.length : 0) +
                    ", vggDim=" + (vgg != null ? vgg.length : 0) +
                    ", vitDim=" + (vit != null ? vit.length : 0) +
                    '}';
        }
    }

    /**
     * 检查Docker特征提取服务是否可用
     *
     * @return true表示服务可用，false表示服务不可用
     */
    boolean isServiceAvailable();

    /**
     * 检查是否启用覆盖已存在的特征向量
     *
     * @return true表示启用覆盖，false表示跳过已存在的文件
     */
    boolean isOverwriteEnabled();

    /**
     * 从图片文件提取多种特征向量
     * 
     * @param imageFile 图片文件
     * @return 多特征向量对象，如果提取失败返回null
     * @throws IllegalArgumentException 如果图片文件无效
     */
    MultiFeatureVector extractFeatures(File imageFile);

    /**
     * 从图片字节数组提取多种特征向量
     * 
     * @param imageBytes 图片字节数组
     * @param imageName 图片名称（用于日志记录）
     * @return 多特征向量对象，如果提取失败返回null
     * @throws IllegalArgumentException 如果图片数据无效
     */
    MultiFeatureVector extractFeatures(byte[] imageBytes, String imageName);

    /**
     * 从图片文件提取CLIP特征向量。
     *
     * @param imageFile 图片文件
     * @return CLIP特征向量对象，如果提取失败返回null
     */
    ClipFeatureVector extractClipFeatures(File imageFile);

    /**
     * 从图片字节数组提取CLIP特征向量。
     *
     * @param imageBytes 图片字节数组
     * @param imageName 图片名称
     * @return CLIP特征向量对象，如果提取失败返回null
     */
    ClipFeatureVector extractClipFeatures(byte[] imageBytes, String imageName);

    /**
     * 异步提取特征向量并保存到数据库
     * 
     * @param filename 文件名
     * @param imagePath 图片路径
     * @return true表示提取请求提交成功，false表示提交失败
     */
    boolean extractAndSaveFeatures(String filename, String imagePath);

    /**
     * 获取服务健康状态信息
     * 
     * @return 健康状态描述
     */
    String getHealthStatus();
}
