// java
package com.example.onlineshop.controller;

import com.example.onlineshop.dto.response.CategoryResponse;
import com.example.onlineshop.service.CategoryService;
import com.example.onlineshop.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 公共分类控制器（对外开放的分类接口）
 * 提供：
 * - /api/categories?tree=... 列表（树形或平铺）
 * - /api/categories/{id} 单个分类详情
 */
@RestController
@RequestMapping("/api/categories")
public class PublicCategoryController {

    /**
     * 注入分类服务，负责具体业务逻辑
     */
    @Autowired
    private CategoryService categoryService;

    /**
     * 列表接口
     * 参数说明：
     * - tree: 是否返回树形结构，默认 true
     * - parentId / parent_id: 父分类 id（支持驼峰和下划线兼容）
     * - level: 分类层级过滤
     * - q: 模糊查询关键词
     * - page/size/sort_by/order: 分页与排序
     *
     * 逻辑要点：
     * - 当 tree=true 且存在筛选（parent/level/q）时，强制使用平铺查询以使筛选生效
     * - 否则根据 tree 参数返回树形或平铺结果
     */
    @GetMapping("")
    public Object list( @RequestParam(name = "tree", defaultValue = "true") boolean tree,
                        @RequestParam(name = "parentId", required = false) Integer parentId,
                        @RequestParam(name = "parent_id", required = false) Integer parent_id, // 兼容下划线参数
                        @RequestParam(name = "level", required = false) Integer level,
                        @RequestParam(name = "q", required = false) String q,
                        @RequestParam(name = "page", defaultValue = "1") int page,
                        @RequestParam(name = "size", defaultValue = "50") int size,
                        @RequestParam(name = "sort_by", required = false) String sortBy,
                        @RequestParam(name = "order", defaultValue = "asc") String order) {
        try {
            Integer realParent = parentId != null ? parentId : parent_id;
            boolean hasFilter = realParent != null || level != null || (q != null && !q.isBlank());

            if (tree) {
                // tree=true 且有筛选时使用平铺查询以支持过滤
                if (hasFilter) {
                    Map<String, Object> r = categoryService.listFlat(realParent, level, q, page, size, sortBy, order);
                    return ResponseUtil.success("获取成功", r);
                }
                // 无筛选时返回完整树
                List<CategoryResponse> r = categoryService.listAllAsTree();
                return ResponseUtil.success("获取成功", r);
            } else {
                // 强制平铺列表
                Map<String, Object> r = categoryService.listFlat(realParent, level, q, page, size, sortBy, order);
                return ResponseUtil.success("获取成功", r);
            }
        } catch (Exception e) {
            // 统一错误处理，返回异常信息
            return ResponseUtil.error("获取失败: " + e.getMessage());
        }
    }

    /**
     * 获取单个分类详情
     * - 若分类不存在，返回自定义 404 响应
     */
    @GetMapping("/{id}")
    public Object get(@PathVariable Integer id) {
        try {
            CategoryResponse r = categoryService.getById(id);
            if (r == null) return ResponseUtil.custom(404, "类别不存在", null);
            return ResponseUtil.success("获取成功", r);
        } catch (Exception e) {
            return ResponseUtil.error("获取失败: " + e.getMessage());
        }
    }
}


