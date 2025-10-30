// java
package com.example.onlineshop.controller;

import com.example.onlineshop.dto.request.CategoryMoveRequest;
import com.example.onlineshop.dto.request.CategoryRequest;
import com.example.onlineshop.dto.response.CategoryResponse;
import com.example.onlineshop.service.CategoryService;
import com.example.onlineshop.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;
import java.util.List;

/**
 * 卖家端类别管理控制器
 * 提供分类树、平铺列表、增删改查以及移动类别的 HTTP 接口
 */
@RestController
@RequestMapping("/api/seller/categories")
public class SellerCategoryController {

    /**
     * 注入分类服务，处理具体的业务逻辑
     */
    @Autowired
    private CategoryService categoryService;

    /**
     * GET /tree
     * 返回所有分类的树形结构（用于前端树形选择或展示）
     * 返回值：成功时包含 List<CategoryResponse> 的数据
     */
    @GetMapping("/tree")
    public Object tree() {
        try {
            List<CategoryResponse> r = categoryService.listAllAsTree();
            return ResponseUtil.success("获取成功", r);
        } catch (Exception e) {
            // 捕获所有异常并返回统一的错误响应
            return ResponseUtil.error("获取失败: " + e.getMessage());
        }
    }

    /**
     * GET /
     * 平铺列出分类（支持分页、按父级筛选、模糊搜索、排序等）
     * 参数：
     * - parent_id: 可选，按父类别筛选
     * - level: 可选，按层级筛选
     * - q: 可选，模糊搜索关键词
     * - page: 可选，页码，默认 1
     * - size: 可选，分页大小，默认 50
     * - sort_by: 可选，排序字段
     * - order: 可选，排序方向（asc/desc）
     * 返回值：Map 包含分页和数据列表
     */
    @GetMapping("")
    public Object list(@RequestParam(required = false) Integer parent_id,
                       @RequestParam(required = false) Integer level,
                       @RequestParam(required = false) String q,
                       @RequestParam(required = false, defaultValue = "1") int page,
                       @RequestParam(required = false, defaultValue = "50") int size,
                       @RequestParam(required = false) String sort_by,
                       @RequestParam(required = false) String order) {
        try {
            Map<String, Object> r = categoryService.listFlat(parent_id, level, q, page, size, sort_by, order);
            return ResponseUtil.success("获取成功", r);
        } catch (Exception e) {
            return ResponseUtil.error("获取失败: " + e.getMessage());
        }
    }

    /**
     * GET /{id}
     * 根据 id 获取单个分类详情
     * 如果不存在，返回 404 自定义响应
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

    /**
     * POST /
     * 创建新分类，使用 @Valid 对请求体进行校验
     * 捕获 IllegalStateException 并返回 400（通常用于业务校验失败）
     */
    @PostMapping("")
    public Object create(@Valid @RequestBody CategoryRequest req) {
        try {
            CategoryResponse r = categoryService.create(req);
            return ResponseUtil.success("创建成功", r);
        } catch (IllegalStateException ise) {
            // 业务异常（例如重复名称、父级错误等），返回自定义 400
            return ResponseUtil.custom(400, ise.getMessage(), null);
        } catch (Exception e) {
            return ResponseUtil.error("创建失败: " + e.getMessage());
        }
    }

    /**
     * PUT /{id}
     * 更新已有分类，路径参数为分类 id，body 为更新数据（同样进行校验）
     */
    @PutMapping("/{id}")
    public Object update(@PathVariable Integer id, @Valid @RequestBody CategoryRequest req) {
        try {
            CategoryResponse r = categoryService.update(id, req);
            return ResponseUtil.success("更新成功", r);
        } catch (IllegalStateException ise) {
            return ResponseUtil.custom(400, ise.getMessage(), null);
        } catch (Exception e) {
            return ResponseUtil.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * DELETE /{id}
     * 删除分类，若存在业务限制（例如有子分类或被商品使用）抛出 IllegalStateException
     */
    @DeleteMapping("/{id}")
    public Object delete(@PathVariable Integer id) {
        try {
            categoryService.delete(id);
            return ResponseUtil.success("删除成功", null);
        } catch (IllegalStateException ise) {
            return ResponseUtil.custom(400, ise.getMessage(), null);
        } catch (Exception e) {
            return ResponseUtil.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * PATCH /{id}/move
     * 移动分类（改变父级或排序位置），请求体为 CategoryMoveRequest
     * 返回移动后的分类数据
     */
    @PatchMapping("/{id}/move")
    public Object move(@PathVariable Integer id, @RequestBody CategoryMoveRequest req) {
        try {
            CategoryResponse r = categoryService.move(id, req);
            return ResponseUtil.success("移动成功", r);
        } catch (IllegalStateException ise) {
            return ResponseUtil.custom(400, ise.getMessage(), null);
        } catch (Exception e) {
            return ResponseUtil.error("移动失败: " + e.getMessage());
        }
    }
}



