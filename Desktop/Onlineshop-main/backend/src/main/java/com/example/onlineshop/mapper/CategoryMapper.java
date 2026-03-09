// java
package com.example.onlineshop.mapper;

import com.example.onlineshop.dto.response.CategoryResponse;
import com.example.onlineshop.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@Repository
public interface CategoryMapper {

    List<CategoryResponse> selectAllOrdered();

    CategoryResponse selectById(@Param("id") Integer id);

    Integer selectLevelById(@Param("id") Integer id);

    Integer selectParentIdById(@Param("id") Integer id);

    int countByParentId(@Param("parentId") Integer parentId);

    int countByNameAndParent(@Param("name") String name, @Param("parentId") Integer parentId, @Param("excludeId") Integer excludeId);

    int countByConditions(@Param("parentId") Integer parentId, @Param("level") Integer level, @Param("q") String q);

    // 将 limit/offset 改为 Integer，允许为 null
    List<CategoryResponse> selectByConditions(@Param("parentId") Integer parentId,
                                              @Param("level") Integer level,
                                              @Param("q") String q,
                                              @Param("limit") Integer limit,
                                              @Param("offset") Integer offset,
                                              @Param("orderSql") String orderSql);

    int insert(Category category);

    int update(@Param("id") Integer id, @Param("name") String name, @Param("parentId") Integer parentId, @Param("level") int level);

    int deleteById(@Param("id") Integer id);
}


