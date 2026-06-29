package com.xx.marketing.controller;

import com.xx.marketing.dto.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 公开首页API — 无需登录
 */
@RestController
public class HomeController {

    @GetMapping("/api/home")
    public Result<Map<String, Object>> home() {
        return Result.ok(Map.of(
                "message", "欢迎来到 XX 全域营销平台",
                "version", "1.0.0",
                "features", new String[]{"优惠券抢购", "商家服务", "秒杀活动"}
        ));
    }
}
