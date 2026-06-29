package com.xx.marketing.service;

import com.xx.marketing.dto.Result;
import com.xx.marketing.entity.Merchant;
import java.util.List;

public interface MerchantService {
    Result<List<Merchant>> getHotMerchants();
    Result<Merchant> getMerchantDetail(Long id);
}
