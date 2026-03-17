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
        if (address.getIsDefault() != null && address.getIsDefault()) {
            customerAddressMapper.cancelDefaultByCustomerId(address.getCustomerId());
        } else {
            List<CustomerAddress> existingAddresses = customerAddressMapper.findByCustomerId(address.getCustomerId());
            if (existingAddresses.isEmpty()) {
                address.setIsDefault(true);
            } else {
                address.setIsDefault(false);
            }
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
            customerAddressMapper.updateDefaultAddress(existing.getCustomerId(), addressId);
        }
        
        customerAddressMapper.update(address);
        return customerAddressMapper.findById(addressId);
    }

    @Override
    @Transactional
    public boolean deleteAddress(Integer addressId, Integer customerId) {
        CustomerAddress address = customerAddressMapper.findById(addressId);
        if (address == null || !address.getCustomerId().equals(customerId)) {
            return false;
        }

        customerAddressMapper.deleteById(addressId);

        if (address.getIsDefault()) {
            List<CustomerAddress> remainingAddresses = customerAddressMapper.findByCustomerId(customerId);
            if (!remainingAddresses.isEmpty()) {
                CustomerAddress firstAddress = remainingAddresses.get(0);
                customerAddressMapper.updateDefaultAddress(customerId, firstAddress.getAddressId());
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
        if (address == null || !address.getCustomerId().equals(customerId)) {
            return null;
        }

        customerAddressMapper.updateDefaultAddress(customerId, addressId);
        return customerAddressMapper.findById(addressId);
    }
}