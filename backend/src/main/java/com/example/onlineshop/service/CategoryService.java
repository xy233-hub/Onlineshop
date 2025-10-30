// java
// file: src/main/java/com/example/onlineshop/service/CategoryService.java
package com.example.onlineshop.service;

import com.example.onlineshop.dto.request.CategoryMoveRequest;
import com.example.onlineshop.dto.request.CategoryRequest;
import com.example.onlineshop.dto.response.CategoryResponse;
import com.example.onlineshop.entity.Category;
import com.example.onlineshop.mapper.CategoryMapper;
import com.example.onlineshop.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ProductMapper productMapper;

    @Transactional(readOnly = true)
    public List<CategoryResponse> listAllAsTree() {
        List<CategoryResponse> all = categoryMapper.selectAllOrdered();
        Map<Integer, CategoryResponse> map = new LinkedHashMap<>();
        List<CategoryResponse> roots = new ArrayList<>();
        for (CategoryResponse c : all) map.put(c.getCategoryId(), c);
        for (CategoryResponse c : all) {
            if (c.getParentId() == null) roots.add(c);
            else {
                CategoryResponse p = map.get(c.getParentId());
                if (p != null) p.getChildren().add(c);
                else roots.add(c);
            }
        }
        return roots;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listFlat(Integer parentId, Integer level, String q, int page, int size, String sortBy, String order) {
        // 计算 limit/offset，并调用 mapper.selectByConditions（示例逻辑，按需调整）
        int limit = size;
        int offset = (page - 1) * size;
        String orderSql = null;
        if (sortBy != null && !sortBy.isBlank()) {
            orderSql = sortBy + " " + (order == null ? "asc" : order);
        }
        List<CategoryResponse> items = categoryMapper.selectByConditions(parentId, level, q, limit, offset, orderSql);
        // 构造返回 Map（总数等可按需查询）
        return Map.of("items", items, "page", page, "size", size);
    }



    public CategoryResponse getById(Integer id) {
        return categoryMapper.selectById(id);
    }

    @Transactional
    public CategoryResponse create(CategoryRequest req) {
        String name = req.getCategoryName();
        if (name == null || name.trim().isEmpty()) throw new IllegalStateException("categoryName 必填");
        name = name.trim();
        Integer parentId = req.getParentId();
        int level = 1;
        if (parentId != null) {
            Integer pLevel = categoryMapper.selectLevelById(parentId);
            if (pLevel == null) throw new IllegalStateException("父分类不存在");
            level = pLevel + 1;
            if (level > 2) throw new IllegalStateException("最大层级为 2");
        }
        int dup = categoryMapper.countByNameAndParent(name, parentId, null);
        if (dup > 0) throw new IllegalStateException("同级已存在同名分类");
        Category c = Category.builder().parentId(parentId).categoryName(name).categoryLevel(level).build();
        int rows = categoryMapper.insert(c);
        if (c.getCategoryId() == null || c.getCategoryId() <= 0) throw new IllegalStateException("创建失败");
        return getById(c.getCategoryId());
    }

    @Transactional
    public CategoryResponse update(Integer id, CategoryRequest req) {
        CategoryResponse exist = getById(id);
        if (exist == null) throw new IllegalStateException("类别不存在");
        String newName = req.getCategoryName() == null ? exist.getCategoryName() : req.getCategoryName().trim();
        if (newName == null || newName.isEmpty()) throw new IllegalStateException("categoryName 必填");
        Integer newParent = req.getParentId() == null ? exist.getParentId() : req.getParentId();
        if (newParent != null && newParent.equals(id)) throw new IllegalStateException("不能设置自身为父分类");
        int newLevel = 1;
        if (newParent != null) {
            Integer pLevel = categoryMapper.selectLevelById(newParent);
            if (pLevel == null) throw new IllegalStateException("目标父分类不存在");
            newLevel = pLevel + 1;
            if (newLevel > 2) throw new IllegalStateException("最大层级为 2");
            Integer p = newParent;
            while (p != null) {
                if (p.equals(id)) throw new IllegalStateException("不能设置自身或子孙为父分类");
                p = categoryMapper.selectParentIdById(p);
            }
        }
        int dup = categoryMapper.countByNameAndParent(newName, newParent, id);
        if (dup > 0) throw new IllegalStateException("同级已存在同名分类");
        categoryMapper.update(id, newName, newParent, newLevel);
        return getById(id);
    }

    @Transactional
    public void delete(Integer id) {
        int childCount = categoryMapper.countByParentId(id);
        if (childCount > 0) throw new IllegalStateException("删除失败：存在子分类");
        int prodCount = productMapper.countByCategoryId(id);
        if (prodCount > 0) throw new IllegalStateException("删除失败：存在商品引用");
        categoryMapper.deleteById(id);
    }

    @Transactional
    public CategoryResponse move(Integer id, CategoryMoveRequest req) {
        CategoryResponse exist = getById(id);
        if (exist == null) throw new IllegalStateException("类别不存在");
        Integer newParent = req.getNewParentId();
        if (newParent != null && newParent.equals(id)) throw new IllegalStateException("不能设置自身为父分类");
        int newLevel = 1;
        if (newParent != null) {
            Integer pLevel = categoryMapper.selectLevelById(newParent);
            if (pLevel == null) throw new IllegalStateException("目标父分类不存在");
            newLevel = pLevel + 1;
            if (newLevel > 2) throw new IllegalStateException("最大层级为 2");
            Integer p = newParent;
            while (p != null) {
                if (p.equals(id)) throw new IllegalStateException("不能移动到自身或子孙下面");
                p = categoryMapper.selectParentIdById(p);
            }
        }
        int dup = categoryMapper.countByNameAndParent(exist.getCategoryName(), newParent, id);
        if (dup > 0) throw new IllegalStateException("同级已存在同名分类");
        categoryMapper.update(id, exist.getCategoryName(), newParent, newLevel);
        return getById(id);
    }
}
