// mapper/CustomerMapper.java
package com.example.onlineshop.mapper;

import com.example.onlineshop.entity.Customer;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface CustomerMapper {

    /**
     * 插入新客户
     *
     * @param customer 客户信息
     * @return 插入成功的记录数
     */
    int insert(Customer customer);

    /**
     * 根据用户名查找客户
     *
     * @param username 用户名
     * @return 客户信息，如果不存在返回null
     */
    Customer findByUsername(String username);

    /**
     * 根据手机号查找客户
     *
     * @param phone 手机号
     * @return 客户信息，如果不存在返回null
     */
    Customer findByPhone(String phone);

    /**
     * 根据ID查找客户
     *
     * @param customerId 客户ID
     * @return 客户信息，如果不存在返回null
     */
    Customer findById(Integer customerId);

    /**
     * 分页查询所有客户
     *
     * @param params 查询参数，包含offset和limit
     * @return 客户列表
     */
    List<Customer> findAllWithPagination(Map<String, Object> params);

    /**
     * 查询客户总数
     *
     * @return 客户总数
     */
    int countAll();
}
