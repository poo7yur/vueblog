package com.example.myApp.demos.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "alipay")
public class PayConfig  {
    /** 应用ID */
    private String appId;
    /** 商户私钥（PKCS8格式） */
    private String privateKey;
    /** 支付宝公钥（用于验签，不是应用公钥！） */
    private String publicKey;
    /** 服务器异步通知页面路径*/
    private String notifyUrl;
    /** 支付宝网关 */
    private String gatewayUrl = "https://openapi.alipay.com/gateway.do";
    /** 签名算法类型 */
    private String signType = "RSA2";
    /** 字符编码格式 */
    private String charset = "UTF-8";
    /** 数据格式 */
    private String format = "JSON";
}
