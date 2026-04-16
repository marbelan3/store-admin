package com.g2u.admin.web.dto;

public record PlaceMarketplaceOrderRequest(
        String shippingCountry,
        String shippingProvince,
        String shippingCity,
        String shippingAddress,
        String shippingZip,
        String shippingCustomerName,
        String shippingPhone
) {}
