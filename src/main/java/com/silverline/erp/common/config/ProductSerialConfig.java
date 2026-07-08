package com.silverline.erp.common.config;

import com.silverline.erp.domain.inventory.ProductSerial;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProductSerialConfig {

    @Value("${rocs.imei.suffix-length:9}")
    public void setSuffixLength(int length) {
        ProductSerial.suffixLength = length;
    }
}
