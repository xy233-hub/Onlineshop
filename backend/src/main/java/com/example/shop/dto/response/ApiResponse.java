// 包路径需与控制器中的import对应（根据你的项目实际包名调整）
package com.example.shop.dto.response;  // 注意：如果你的groupId是org.example，包名应为org.example.shop...

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 全局统一响应实体
 * 所有接口返回结果都用该类封装
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse {
    private Integer code;    // 状态码（200=成功，400=参数错误，401=未认证等）
    private String msg;      // 提示信息
    private Object data;     // 响应数据（可选，成功时返回业务数据，失败时为null）
}