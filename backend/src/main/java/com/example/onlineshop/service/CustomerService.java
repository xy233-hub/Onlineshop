// backend/src/main/java/com/example/onlineshop/service/CustomerService.java
package com.example.onlineshop.service;

import com.example.onlineshop.entity.Customer;
import java.util.List;
import java.util.Map;

public interface CustomerService {

    /**
     * 注册新客户
     *
     * @param username 用户名
     * @param password 密码
     * @param phone 手机号
     * @param defaultAddress 默认地址
     * @return 注册成功的客户信息，如果注册失败返回null
     */
    Customer registerCustomer(String username, String password, String phone, String defaultAddress);

    /**
     * 客户登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录成功的客户信息，如果登录失败返回null
     */
    Customer login(String username, String password);

    /**
     * 分页获取所有客户信息
     *
     * @param page 页码
     * @param size 每页数量
     * @return 客户列表
     */
    List<Customer> getAllCustomersWithPagination(int page, int size);

    /**
     * 获取客户总数
     *
     * @return 客户总数
     */
    int getCustomerCount();

    /**
     * 根据ID查找客户
     *
     * @param customerId 客户ID
     * @return 客户信息
     */
    Customer findById(Integer customerId);
}
