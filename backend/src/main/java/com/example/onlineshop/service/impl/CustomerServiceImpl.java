// backend/src/main/java/com/example/onlineshop/service/impl/CustomerServiceImpl.java
package com.example.onlineshop.service.impl;

import com.example.onlineshop.entity.Customer;
import com.example.onlineshop.mapper.CustomerMapper;
import com.example.onlineshop.service.CustomerService;
import com.example.onlineshop.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CustomerServiceImpl implements CustomerService {

    @Autowired
    private CustomerMapper customerMapper;

    @Override
    public Customer registerCustomer(String username, String password, String phone, String defaultAddress) {
        // 检查用户名或手机号是否已存在
        if (customerMapper.findByUsername(username) != null || customerMapper.findByPhone(phone) != null) {
            return null;
        }

        Customer customer = new Customer();
        customer.setUsername(username);
        customer.setPassword(PasswordUtil.encrypt(password)); // 使用正确的加密方法
        customer.setPhone(phone);
        customer.setDefaultAddress(defaultAddress);
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());

        int result = customerMapper.insert(customer);
        if (result > 0) {
            // 重新查询以获取生成的customerId
            return customerMapper.findByUsername(username);
        }
        return null;
    }

    @Override
    public Customer login(String username, String password) {
        Customer customer = customerMapper.findByUsername(username);
        if (customer != null && PasswordUtil.matches(password, customer.getPassword())) {
            return customer;
        }
        return null;
    }

    @Override
    public List<Customer> getAllCustomersWithPagination(int page, int size) {
        Map<String, Object> params = new HashMap<>();
        params.put("offset", (page - 1) * size);
        params.put("limit", size);
        return customerMapper.findAllWithPagination(params);
    }

    @Override
    public int getCustomerCount() {
        return customerMapper.countAll();
    }

    @Override
    public Customer findById(Integer customerId) {
        return customerMapper.findById(customerId);
    }
}
