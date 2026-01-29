package com.example.myApp.demos.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class IpUtil {
    /**
     * 获取本机有效内网IP（排除回环地址、虚拟网卡、IPv6）
     * 如果是正式环境 返回公网ip
     * @return 本机内网IP，如172.16.161.40；无有效IP返回127.0.0.1
     */
    public static String getLocalIp() {
        try {
            // 遍历所有网络接口
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                // 过滤虚拟网卡/禁用的网卡
                if (ni.isLoopback() || ni.isVirtual() || !ni.isUp()) {
                    continue;
                }

                // 遍历该网卡下的所有IP地址
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    // 只保留IPv4地址，且不是回环地址
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        String ip = addr.getHostAddress();
                        // 过滤掉Docker等虚拟网络的IP（可选，根据实际场景调整）
                        if (!ip.startsWith("172.17.") && !ip.startsWith("192.168.99.")) {
                            if(ip.startsWith("10.0.0")) return "124.223.89.5";
                            return ip;
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        // 兜底返回回环地址
        return "127.0.0.1";
    }
}
