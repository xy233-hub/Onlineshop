package com.example.onlineshop.service;

import com.example.onlineshop.entity.CustomerAddress;
import java.util.List;

public interface CustomerAddressService {
    
    CustomerAddress addAddress(CustomerAddress address);
    
    CustomerAddress updateAddress(Integer addressId, CustomerAddress address);
    
    boolean deleteAddress(Integer addressId, Integer customerId);
    
    CustomerAddress getAddressById(Integer addressId);
    
    List<CustomerAddress> getAddressesByCustomerId(Integer customerId);
    
    CustomerAddress setDefaultAddress(Integer addressId, Integer customerId);
}