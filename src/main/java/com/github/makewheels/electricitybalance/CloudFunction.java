package com.github.makewheels.electricitybalance;

import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.fc.runtime.Context;
import com.aliyun.fc.runtime.StreamRequestHandler;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;

public class CloudFunction implements StreamRequestHandler {
    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) {
        String xml = HttpUtil.get("https://jnb.ccnu.edu.cn/ICBS/PurchaseWebService.asmx" +
                "/getReserveHKAM?AmMeter_ID=1001.033717.1");
        String remainPower = StringUtils.substringBetween(xml, "<remainPower>", "</remainPower>");
        String readTime = StringUtils.substringBetween(xml, "<readTime>", "</readTime>");

        //组装发送邮件参数
        JSONObject body = new JSONObject();
        body.put("toAddress", "finalbird@foxmail.com");
        body.put("fromAlias", "push-center");
        body.put("subject", "寝室电费：" + remainPower);
        body.put("htmlBody", "remainPower = " + remainPower + "<br>"
                + "readTime = " + readTime + "<br>"
                + "nanoId = " + IdUtil.nanoId());

        //调用推送中心
        String response = HttpUtil.post(
                "http://82.157.172.71:5025/push/sendEmail",
                body.toJSONString());
        System.out.println(response);
    }
}
