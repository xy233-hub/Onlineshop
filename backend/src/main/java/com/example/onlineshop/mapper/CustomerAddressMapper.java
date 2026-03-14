package com.example.onlineshop.mapper;

import com.example.onlineshop.entity.CustomerAddress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface CustomerAddressMapper {
    
    int insert(CustomerAddress address);
    
    int update(CustomerAddress address);
    
    int deleteById(@Param("addressId") Integer addressId);
    
    CustomerAddress findById(@Param("addressId") Integer addressId);
    
    List<CustomerAddress> findByCustomerId(@Param("customerId") Integer customerId);
    
    int updateDefaultAddress(@Param("customerId") Integer customerId, @Param("addressId") Integer addressId);
    
    int cancelDefaultByCustomerId(@Param("customerId") Integer customerId);
}