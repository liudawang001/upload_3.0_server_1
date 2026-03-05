package com.mls.upload.server.mapper;

import com.mls.upload.server.entity.PicUser;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * 用户数据访问接口
 * 处理pic_user表的数据操作
 *
 * @author MLS Development Team
 * @version 1.0.0
 */
@Mapper
public interface PicUserMapper {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    @Select("SELECT id, username, password, nickname, create_time, updata_time " +
            "FROM pic_user WHERE username = #{username}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "username", column = "username"),
        @Result(property = "password", column = "password"),
        @Result(property = "nickname", column = "nickname"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "updataTime", column = "updata_time")
    })
    PicUser findByUsername(@Param("username") String username);

    /**
     * 根据ID查询用户
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @Select("SELECT id, username, password, nickname, create_time, updata_time " +
            "FROM pic_user WHERE id = #{id}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "username", column = "username"),
        @Result(property = "password", column = "password"),
        @Result(property = "nickname", column = "nickname"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "updataTime", column = "updata_time")
    })
    PicUser findById(@Param("id") Integer id);

    /**
     * 查询所有用户
     *
     * @return 用户列表
     */
    @Select("SELECT id, username, password, nickname, create_time, updata_time " +
            "FROM pic_user ORDER BY create_time DESC")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "username", column = "username"),
        @Result(property = "password", column = "password"),
        @Result(property = "nickname", column = "nickname"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "updataTime", column = "updata_time")
    })
    List<PicUser> findAll();

    /**
     * 插入新用户
     *
     * @param user 用户信息
     * @return 影响行数
     */
    @Insert("INSERT INTO pic_user (username, password, nickname, create_time, updata_time) " +
            "VALUES (#{username}, #{password}, #{nickname}, #{createTime}, #{updataTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PicUser user);

    /**
     * 更新用户信息
     *
     * @param user 用户信息
     * @return 影响行数
     */
    @Update("UPDATE pic_user SET password = #{password}, nickname = #{nickname}, " +
            "updata_time = #{updataTime} WHERE id = #{id}")
    int update(PicUser user);

    /**
     * 删除用户
     *
     * @param id 用户ID
     * @return 影响行数
     */
    @Delete("DELETE FROM pic_user WHERE id = #{id}")
    int deleteById(@Param("id") Integer id);

    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     * @return 存在数量
     */
    @Select("SELECT COUNT(*) FROM pic_user WHERE username = #{username}")
    int countByUsername(@Param("username") String username);

    /**
     * 验证用户登录
     *
     * @param username 用户名
     * @param password 密码（已加密）
     * @return 用户信息
     */
    @Select("SELECT id, username, password, nickname, create_time, updata_time " +
            "FROM pic_user WHERE username = #{username} AND password = #{password}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "username", column = "username"),
        @Result(property = "password", column = "password"),
        @Result(property = "nickname", column = "nickname"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "updataTime", column = "updata_time")
    })
    PicUser validateLogin(@Param("username") String username, @Param("password") String password);
}
