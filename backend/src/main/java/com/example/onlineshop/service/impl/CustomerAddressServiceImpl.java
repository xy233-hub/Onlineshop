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
        if (address.getIsDefault() == null) {
            address.setIsDefault(false);
        }

        // 必填字段兜底（建议后续改为参数校验而不是置空字符串）
        if (address.getRecipientName() == null) address.setRecipientName("");
        if (address.getRecipientPhone() == null) address.setRecipientPhone("");
        if (address.getProvince() == null) address.setProvince("");
        if (address.getCity() == null) address.setCity("");
        if (address.getDistrict() == null) address.setDistrict("");
        if (address.getDetailAddress() == null) address.setDetailAddress("");

        address.setCreatedAt(LocalDateTime.now());
        address.setUpdatedAt(LocalDateTime.now());

        // 如果设为默认：先把该 customer\_id 下其它地址取消默认，再插入
        if (Boolean.TRUE.equals(address.getIsDefault()) && address.getCustomerId() != null) {
            customerAddressMapper.updateDefaultAddress(address.getCustomerId(), null);
        }

        customerAddressMapper.insert(address);
        return address;
    }

    @Override
    @Transactional
    public CustomerAddress updateAddress(Integer addressId, CustomerAddress address) {
        CustomerAddress existing = customerAddressMapper.findById(addressId);
        if (existing == null) return null;

        // 归属校验：只能改自己的地址（address 里若没带 customerId，则用 existing）
        Integer customerId = address.getCustomerId() != null ? address.getCustomerId() : existing.getCustomerId();
        if (customerId == null || !customerId.equals(existing.getCustomerId())) return null;

        address.setAddressId(addressId);
        address.setCustomerId(existing.getCustomerId());
        address.setUpdatedAt(LocalDateTime.now());

        if (Boolean.TRUE.equals(address.getIsDefault())) {
            // 仅取消该 customer\_id 下其它默认
            customerAddressMapper.updateDefaultAddress(customerId, addressId);
        }

        customerAddressMapper.update(address);
        return customerAddressMapper.findById(addressId);
    }

    @Override
    @Transactional
    public boolean deleteAddress(Integer addressId, Integer customerId) {
        CustomerAddress address = customerAddressMapper.findById(addressId);
        if (address == null) return false;

        // 归属校验
        if (customerId == null || address.getCustomerId() == null || !customerId.equals(address.getCustomerId())) {
            return false;
        }

        customerAddressMapper.deleteById(addressId);

        // 若删除的是默认地址：为该 customer\_id 的第一条地址补默认
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            List<CustomerAddress> remaining = customerAddressMapper.findByCustomerId(customerId);
            if (!remaining.isEmpty()) {
                CustomerAddress first = remaining.get(0);
                customerAddressMapper.updateDefaultAddress(customerId, first.getAddressId());
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
        return customerAddressMapper.findByCustomerId(customerId);
    }

    @Override
    @Transactional
    public CustomerAddress setDefaultAddress(Integer addressId, Integer customerId) {
        CustomerAddress address = customerAddressMapper.findById(addressId);
        if (address == null) return null;

        // 归属校验
        if (customerId == null || address.getCustomerId() == null || !customerId.equals(address.getCustomerId())) {
            return null;
        }

        customerAddressMapper.updateDefaultAddress(customerId, addressId);
        return customerAddressMapper.findById(addressId);
    }
}
