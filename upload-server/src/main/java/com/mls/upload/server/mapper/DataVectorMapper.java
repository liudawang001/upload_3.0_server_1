package com.mls.upload.server.mapper;

import com.mls.upload.server.entity.DataVector;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * 特征向量数据访问接口
 * 处理data_vector表的数据操作
 *
 * @author MLS Development Team
 * @version 1.0.0
 */
@Mapper
public interface DataVectorMapper {

    /**
     * 插入特征向量数据
     *
     * @param dataVector 特征向量数据
     * @return 影响行数
     */
    @Insert("INSERT INTO data_vector (filename, color, glcm, lbp, vgg, vit, create_time) " +
            "VALUES (#{filename}, #{color}, #{glcm}, #{lbp}, #{vgg}, #{vit}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DataVector dataVector);

    /**
     * 根据ID查询特征向量数据
     *
     * @param id 记录ID
     * @return 特征向量数据
     */
    @Select("SELECT id, filename, color, glcm, lbp, vgg, vit, create_time " +
            "FROM data_vector WHERE id = #{id}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "filename", column = "filename"),
        @Result(property = "color", column = "color"),
        @Result(property = "glcm", column = "glcm"),
        @Result(property = "lbp", column = "lbp"),
        @Result(property = "vgg", column = "vgg"),
        @Result(property = "vit", column = "vit"),
        @Result(property = "createTime", column = "create_time")
    })
    DataVector findById(@Param("id") Integer id);

    /**
     * 根据文件名查询特征向量数据
     *
     * @param filename 文件名
     * @return 特征向量数据
     */
    @Select("SELECT id, filename, color, glcm, lbp, vgg, vit, create_time " +
            "FROM data_vector WHERE filename = #{filename}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "filename", column = "filename"),
        @Result(property = "color", column = "color"),
        @Result(property = "glcm", column = "glcm"),
        @Result(property = "lbp", column = "lbp"),
        @Result(property = "vgg", column = "vgg"),
        @Result(property = "vit", column = "vit"),
        @Result(property = "createTime", column = "create_time")
    })
    DataVector findByFilename(@Param("filename") String filename);

    /**
     * 查询所有特征向量数据
     *
     * @return 特征向量数据列表
     */
    @Select("SELECT id, filename, color, glcm, lbp, vgg, vit, create_time " +
            "FROM data_vector ORDER BY create_time DESC")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "filename", column = "filename"),
        @Result(property = "color", column = "color"),
        @Result(property = "glcm", column = "glcm"),
        @Result(property = "lbp", column = "lbp"),
        @Result(property = "vgg", column = "vgg"),
        @Result(property = "vit", column = "vit"),
        @Result(property = "createTime", column = "create_time")
    })
    List<DataVector> findAll();

    /**
     * 更新特征向量数据
     *
     * @param dataVector 特征向量数据
     * @return 影响行数
     */
    @Update("UPDATE data_vector SET color = #{color}, glcm = #{glcm}, lbp = #{lbp}, " +
            "vgg = #{vgg}, vit = #{vit}, create_time = #{createTime} WHERE id = #{id}")
    int update(DataVector dataVector);

    /**
     * 删除特征向量数据
     *
     * @param id 记录ID
     * @return 影响行数
     */
    @Delete("DELETE FROM data_vector WHERE id = #{id}")
    int deleteById(@Param("id") Integer id);

    /**
     * 根据文件名删除特征向量数据
     *
     * @param filename 文件名
     * @return 影响行数
     */
    @Delete("DELETE FROM data_vector WHERE filename = #{filename}")
    int deleteByFilename(@Param("filename") String filename);

    /**
     * 统计特征向量数据数量
     *
     * @return 记录总数
     */
    @Select("SELECT COUNT(*) FROM data_vector")
    int count();

    /**
     * 检查文件是否已存在特征向量
     *
     * @param filename 文件名
     * @return 存在数量
     */
    @Select("SELECT COUNT(*) FROM data_vector WHERE filename = #{filename}")
    int countByFilename(@Param("filename") String filename);

    /**
     * 查询已提取特征的文件列表
     *
     * @return 文件名列表
     */
    @Select("SELECT DISTINCT filename FROM data_vector ORDER BY filename")
    List<String> findExtractedFilenames();
}
