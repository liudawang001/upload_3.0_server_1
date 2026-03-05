package com.mls.upload.server.mapper;

import com.mls.upload.server.entity.ImageUpload;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 图片上传记录数据访问接口
 * 处理image_upload表的数据操作
 *
 * @author MLS Development Team
 * @version 1.0.0
 */
@Mapper
public interface ImageUploadMapper {

    /**
     * 插入上传记录
     *
     * @param imageUpload 上传记录
     * @return 影响行数
     */
    @Insert("INSERT INTO image_upload (filename, image_type, create_time, username) " +
            "VALUES (#{filename}, #{imageType}, #{createTime}, #{username})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ImageUpload imageUpload);

    /**
     * 根据ID查询上传记录
     *
     * @param id 记录ID
     * @return 上传记录
     */
    @Select("SELECT id, filename, image_type, create_time, username " +
            "FROM image_upload WHERE id = #{id}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "filename", column = "filename"),
        @Result(property = "imageType", column = "image_type"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "username", column = "username")
    })
    ImageUpload findById(@Param("id") Integer id);

    /**
     * 根据文件名查询上传记录
     *
     * @param filename 文件名
     * @return 上传记录
     */
    @Select("SELECT id, filename, image_type, create_time, username " +
            "FROM image_upload WHERE filename = #{filename}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "filename", column = "filename"),
        @Result(property = "imageType", column = "image_type"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "username", column = "username")
    })
    ImageUpload findByFilename(@Param("filename") String filename);

    /**
     * 根据用户名查询上传记录
     *
     * @param username 用户名
     * @return 上传记录列表
     */
    @Select("SELECT id, filename, image_type, create_time, username " +
            "FROM image_upload WHERE username = #{username} ORDER BY create_time DESC")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "filename", column = "filename"),
        @Result(property = "imageType", column = "image_type"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "username", column = "username")
    })
    List<ImageUpload> findByUsername(@Param("username") String username);

    /**
     * 根据图片类型查询上传记录
     *
     * @param imageType 图片类型
     * @return 上传记录列表
     */
    @Select("SELECT id, filename, image_type, create_time, username " +
            "FROM image_upload WHERE image_type = #{imageType} ORDER BY create_time DESC")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "filename", column = "filename"),
        @Result(property = "imageType", column = "image_type"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "username", column = "username")
    })
    List<ImageUpload> findByImageType(@Param("imageType") String imageType);

    /**
     * 查询指定时间范围内的上传记录
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 上传记录列表
     */
    @Select("SELECT id, filename, image_type, create_time, username " +
            "FROM image_upload WHERE create_time BETWEEN #{startTime} AND #{endTime} " +
            "ORDER BY create_time DESC")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "filename", column = "filename"),
        @Result(property = "imageType", column = "image_type"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "username", column = "username")
    })
    List<ImageUpload> findByTimeRange(@Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);

    /**
     * 查询所有上传记录
     *
     * @return 上传记录列表
     */
    @Select("SELECT id, filename, image_type, create_time, username " +
            "FROM image_upload ORDER BY create_time DESC")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "filename", column = "filename"),
        @Result(property = "imageType", column = "image_type"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "username", column = "username")
    })
    List<ImageUpload> findAll();

    /**
     * 删除上传记录
     *
     * @param id 记录ID
     * @return 影响行数
     */
    @Delete("DELETE FROM image_upload WHERE id = #{id}")
    int deleteById(@Param("id") Integer id);

    /**
     * 统计上传记录数量
     *
     * @return 记录总数
     */
    @Select("SELECT COUNT(*) FROM image_upload")
    int count();

    /**
     * 统计指定用户的上传记录数量
     *
     * @param username 用户名
     * @return 记录数量
     */
    @Select("SELECT COUNT(*) FROM image_upload WHERE username = #{username}")
    int countByUsername(@Param("username") String username);

    /**
     * 统计指定图片类型的上传记录数量
     *
     * @param imageType 图片类型
     * @return 记录数量
     */
    @Select("SELECT COUNT(*) FROM image_upload WHERE image_type = #{imageType}")
    int countByImageType(@Param("imageType") String imageType);
}
