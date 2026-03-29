// src/main/java/com/ecommerce/model/Address.java
package com.ecommerce.model;

import lombok.Data;

@Data
public class Address {
    private String addressId;
    private String userId;
    private String name;
    private String phone;
    private String province;
    private String city;
    private String district;
    private String detail;
    private Boolean isDefault;
}