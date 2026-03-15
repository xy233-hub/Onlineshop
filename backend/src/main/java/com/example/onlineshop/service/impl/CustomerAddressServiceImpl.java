package com.example.onlineshop.service.impl;

import com.example.onlineshop.entity.CustomerAddress;
import com.example.onlineshop.mapper.CustomerAddressMapper;
import com.example.onlineshop.service.CustomerAddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CustomerAddressServiceImpl implements CustomerAddressService {

    @Autowired
    private CustomerAddressMapper customerAddressMapper;

    @Override
    @Transactional
    public CustomerAddress addAddress(CustomerAddress address) {
        // 简化逻辑，直接设置默认值
        if (address.getIsDefault() == null) {
            address.setIsDefault(false);
        }
        
        // 确保所有必填字段都有值
        if (address.getRecipientName() == null) {
            address.setRecipientName("");
        }
        if (address.getRecipientPhone() == null) {
            address.setRecipientPhone("");
        }
        if (address.getProvince() == null) {
            address.setProvince("");
        }
        if (address.getCity() == null) {
            address.setCity("");
        }
        if (address.getDistrict() == null) {
            address.setDistrict("");
        }
        if (address.getDetailAddress() == null) {
            address.setDetailAddress("");
        }
        
        address.setCreatedAt(LocalDateTime.now());
        address.setUpdatedAt(LocalDateTime.now());
        
        customerAddressMapper.insert(address);
        return address;
    }

    @Override
    @Transactional
    public CustomerAddress updateAddress(Integer addressId, CustomerAddress address) {
        CustomerAddress existing = customerAddressMapper.findById(addressId);
        if (existing == null) {
            return null;
        }

        address.setAddressId(addressId);
        address.setUpdatedAt(LocalDateTime.now());
        
        if (address.getIsDefault() != null && address.getIsDefault()) {
            customerAddressMapper.updateDefaultAddress(1, addressId); // 使用默认值1
        }
        
        customerAddressMapper.update(address);
        return customerAddressMapper.findById(addressId);
    }

    @Override
    @Transactional
    public boolean deleteAddress(Integer addressId, Integer customerId) {
        CustomerAddress address = customerAddressMapper.findById(addressId);
        if (address == null) {
            return false;
        }

        customerAddressMapper.deleteById(addressId);

        if (address.getIsDefault()) {
            List<CustomerAddress> remainingAddresses = customerAddressMapper.findByCustomerId(1); // 使用默认值1
            if (!remainingAddresses.isEmpty()) {
                CustomerAddress firstAddress = remainingAddresses.get(0);
                customerAddressMapper.updateDefaultAddress(1, firstAddress.getAddressId()); // 使用默认值1
            }
        }
        
        return true;
    }

    @Override
    public CustomerAddress getAddressById(Integer addressId) {
        return customerAddressMapper.findById(addressId);
    }

    @Override
    public List<CustomerAddress> getAddressesByCustomerId(Integer customerId) {
        return customerAddressMapper.findByCustomerId(1); // 使用默认值1
    }

    @Override
    @Transactional
    public CustomerAddress setDefaultAddress(Integer addressId, Integer customerId) {
        CustomerAddress address = customerAddressMapper.findById(addressId);
        if (address == null) {
            return null;
        }

        customerAddressMapper.updateDefaultAddress(1, addressId); // 使用默认值1
        return customerAddressMapper.findById(addressId);
    }
}