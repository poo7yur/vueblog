package com.example.myApp.demos.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class Order {
    private String orderId;
    private String orderName;
    private String createBy;
    private Date createTime;
    private int status;
    private String remark;
    private String toUser;
    private BigDecimal payAmount;
    private String tradeNo;
}
