package com.mls.upload.server.mapper;

import com.mls.upload.server.entity.PicInfo;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * 图片信息数据访问接口
 * 处理pic_info表的数据操作
 *
 * @author MLS Development Team
 * @version 1.0.0
 */
@Mapper
public interface PicInfoMapper {

    /**
     * 插入图片信息
     *
     * @param picInfo 图片信息
     * @return 影响行数
     */
    @Insert("INSERT INTO pic_info (filename, filepath, create_time, field01, field02, field03, field04, field05, " +
            "field06, field07, field08, field09, field10, field11, field12, field13, field14, field15, " +
            "field16, field17, field18, field19, field20) " +
            "VALUES (#{filename}, #{filepath}, #{createTime}, #{field01}, #{field02}, #{field03}, #{field04}, #{field05}, " +
            "#{field06}, #{field07}, #{field08}, #{field09}, #{field10}, #{field11}, #{field12}, #{field13}, #{field14}, #{field15}, " +
            "#{field16}, #{field17}, #{field18}, #{field19}, #{field20})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PicInfo picInfo);

    /**
     * 根据ID查询图片信息
     *
     * @param id 图片ID
     * @return 图片信息
     */
    @Select("SELECT id, filename, filepath, create_time, field01, field02, field03, field04, field05, " +
            "field06, field07, field08, field09, field10, field11, field12, field13, field14, field15, " +
            "field16, field17, field18, field19, field20 FROM pic_info WHERE id = #{id}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "filename", column = "filename"),
        @Result(property = "filepath", column = "filepath"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "field01", column = "field01"),
        @Result(property = "field02", column = "field02"),
        @Result(property = "field03", column = "field03"),
        @Result(property = "field04", column = "field04"),
        @Result(property = "field05", column = "field05"),
        @Result(property = "field06", column = "field06"),
        @Result(property = "field07", column = "field07"),
        @Result(property = "field08", column = "field08"),
        @Result(property = "field09", column = "field09"),
        @Result(property = "field10", column = "field10"),
        @Result(property = "field11", column = "field11"),
        @Result(property = "field12", column = "field12"),
        @Result(property = "field13", column = "field13"),
        @Result(property = "field14", column = "field14"),
        @Result(property = "field15", column = "field15"),
        @Result(property = "field16", column = "field16"),
        @Result(property = "field17", column = "field17"),
        @Result(property = "field18", column = "field18"),
        @Result(property = "field19", column = "field19"),
        @Result(property = "field20", column = "field20")
    })
    PicInfo findById(@Param("id") Integer id);

    /**
     * 根据文件名查询图片信息
     *
     * @param filename 文件名
     * @return 图片信息
     */
    @Select("SELECT id, filename, filepath, create_time, field01, field02, field03, field04, field05, " +
            "field06, field07, field08, field09, field10, field11, field12, field13, field14, field15, " +
            "field16, field17, field18, field19, field20 FROM pic_info WHERE filename = #{filename}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "filename", column = "filename"),
        @Result(property = "filepath", column = "filepath"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "field01", column = "field01"),
        @Result(property = "field02", column = "field02"),
        @Result(property = "field03", column = "field03"),
        @Result(property = "field04", column = "field04"),
        @Result(property = "field05", column = "field05"),
        @Result(property = "field06", column = "field06"),
        @Result(property = "field07", column = "field07"),
        @Result(property = "field08", column = "field08"),
        @Result(property = "field09", column = "field09"),
        @Result(property = "field10", column = "field10"),
        @Result(property = "field11", column = "field11"),
        @Result(property = "field12", column = "field12"),
        @Result(property = "field13", column = "field13"),
        @Result(property = "field14", column = "field14"),
        @Result(property = "field15", column = "field15"),
        @Result(property = "field16", column = "field16"),
        @Result(property = "field17", column = "field17"),
        @Result(property = "field18", column = "field18"),
        @Result(property = "field19", column = "field19"),
        @Result(property = "field20", column = "field20")
    })
    PicInfo findByFilename(@Param("filename") String filename);

    /**
     * 根据商品编号查询图片信息
     *
     * @param productCode 商品编号（field01）
     * @return 图片信息列表
     */
    @Select("SELECT id, filename, filepath, create_time, field01, field02, field03, field04, field05, " +
            "field06, field07, field08, field09, field10, field11, field12, field13, field14, field15, " +
            "field16, field17, field18, field19, field20 FROM pic_info WHERE field01 = #{productCode}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "filename", column = "filename"),
        @Result(property = "filepath", column = "filepath"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "field01", column = "field01"),
        @Result(property = "field02", column = "field02"),
        @Result(property = "field03", column = "field03"),
        @Result(property = "field04", column = "field04"),
        @Result(property = "field05", column = "field05"),
        @Result(property = "field06", column = "field06"),
        @Result(property = "field07", column = "field07"),
        @Result(property = "field08", column = "field08"),
        @Result(property = "field09", column = "field09"),
        @Result(property = "field10", column = "field10"),
        @Result(property = "field11", column = "field11"),
        @Result(property = "field12", column = "field12"),
        @Result(property = "field13", column = "field13"),
        @Result(property = "field14", column = "field14"),
        @Result(property = "field15", column = "field15"),
        @Result(property = "field16", column = "field16"),
        @Result(property = "field17", column = "field17"),
        @Result(property = "field18", column = "field18"),
        @Result(property = "field19", column = "field19"),
        @Result(property = "field20", column = "field20")
    })
    List<PicInfo> findByProductCode(@Param("productCode") String productCode);

    /**
     * 查询所有图片信息
     *
     * @return 图片信息列表
     */
    @Select("SELECT id, filename, filepath, create_time, field01, field02, field03, field04, field05, " +
            "field06, field07, field08, field09, field10, field11, field12, field13, field14, field15, " +
            "field16, field17, field18, field19, field20 FROM pic_info ORDER BY create_time DESC")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "filename", column = "filename"),
        @Result(property = "filepath", column = "filepath"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "field01", column = "field01"),
        @Result(property = "field02", column = "field02"),
        @Result(property = "field03", column = "field03"),
        @Result(property = "field04", column = "field04"),
        @Result(property = "field05", column = "field05"),
        @Result(property = "field06", column = "field06"),
        @Result(property = "field07", column = "field07"),
        @Result(property = "field08", column = "field08"),
        @Result(property = "field09", column = "field09"),
        @Result(property = "field10", column = "field10"),
        @Result(property = "field11", column = "field11"),
        @Result(property = "field12", column = "field12"),
        @Result(property = "field13", column = "field13"),
        @Result(property = "field14", column = "field14"),
        @Result(property = "field15", column = "field15"),
        @Result(property = "field16", column = "field16"),
        @Result(property = "field17", column = "field17"),
        @Result(property = "field18", column = "field18"),
        @Result(property = "field19", column = "field19"),
        @Result(property = "field20", column = "field20")
    })
    List<PicInfo> findAll();

    /**
     * 更新图片信息
     *
     * @param picInfo 图片信息
     * @return 影响行数
     */
    @Update("UPDATE pic_info SET filepath = #{filepath}, field01 = #{field01}, field02 = #{field02}, " +
            "field03 = #{field03}, field04 = #{field04}, field05 = #{field05}, field06 = #{field06}, " +
            "field07 = #{field07}, field08 = #{field08}, field09 = #{field09}, field10 = #{field10}, " +
            "field11 = #{field11}, field12 = #{field12}, field13 = #{field13}, field14 = #{field14}, " +
            "field15 = #{field15}, field16 = #{field16}, field17 = #{field17}, field18 = #{field18}, " +
            "field19 = #{field19}, field20 = #{field20} WHERE id = #{id}")
    int update(PicInfo picInfo);

    /**
     * 删除图片信息
     *
     * @param id 图片ID
     * @return 影响行数
     */
    @Delete("DELETE FROM pic_info WHERE id = #{id}")
    int deleteById(@Param("id") Integer id);

    /**
     * 统计图片信息数量
     *
     * @return 记录总数
     */
    @Select("SELECT COUNT(*) FROM pic_info")
    int count();

    /**
     * 根据商品编号统计图片数量
     *
     * @param productCode 商品编号
     * @return 图片数量
     */
    @Select("SELECT COUNT(*) FROM pic_info WHERE field01 = #{productCode}")
    int countByProductCode(@Param("productCode") String productCode);
}
