package com.xx.marketing.controller;

import com.xx.marketing.dto.Result;
import com.xx.marketing.entity.Merchant;
import com.xx.marketing.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    /**
     * 热门商家（首页）
     */
    @GetMapping("/hot")
    public Result<List<Merchant>> hot() {
        return merchantService.getHotMerchants();
    }

    /**
     * 商家详情
     */
    @GetMapping("/detail/{id}")
    public Result<Merchant> detail(@PathVariable Long id) {
        return merchantService.getMerchantDetail(id);
    }
}
