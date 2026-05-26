package com.example.onlineshop.service;

import com.example.onlineshop.dto.response.AddressParseResponse;

public interface AddressParseService {
    AddressParseResponse parseAddress(String addressText);
}