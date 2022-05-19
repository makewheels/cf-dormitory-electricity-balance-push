package com.github.makewheels.electricitybalance;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.XML;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.fc.runtime.Context;
import com.aliyun.fc.runtime.StreamRequestHandler;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class CloudFunction implements StreamRequestHandler {

    private String getHistoryHtmlTable() {
        //查询的开始和结束时间
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.add(Calendar.DAY_OF_MONTH, -10);

        Calendar endCalendar = Calendar.getInstance();

        String startDate = new SimpleDateFormat("yyyy/M/d").format(startCalendar.getTime());
        String endDate = new SimpleDateFormat("yyyy/M/d").format(endCalendar.getTime());

        //发请求拿到历史用量
        String xml = HttpUtil.get("https://jnb.ccnu.edu.cn/ICBS/PurchaseWebService.asmx/getMeterDayValue" +
                "?AmMeter_ID=1001.033717.1" +
                "&startDate=" + URLUtil.encode(startDate)
                + "&endDate=" + URLUtil.encode(endDate));
        String json = XML.toJSONObject(xml).toJSONString(0);
        JSONObject jsonObject = JSONObject.parseObject(json);
        JSONArray DayValueInfo = jsonObject.getJSONObject("resultDayVuleInfo")
                .getJSONObject("dayValueInfoList").getJSONArray("DayValueInfo");

        //组装table html
        StringBuilder table = new StringBuilder();
        table.append("<table border=\"1\">");
        for (int i = 0; i < DayValueInfo.size(); i++) {
            JSONObject each = DayValueInfo.getJSONObject(i);
            table.append("<tr>"
                    + "<th>" + each.getString("curDayTime") + "</th>"
                    + "<th>" + each.getString("dayValue") + each.getString("dw") + "</th>"
                    + "<th>￥" + each.getString("dayUseMeony") + "</th>"
                    + "</tr>");
        }
        table.append("</table>");
        return table.toString();
    }

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
                + "nanoId = " + IdUtil.nanoId() + "<br>"
                + getHistoryHtmlTable());
        //调用推送中心
        String response = HttpUtil.post(
                "http://push-center.java8.icu:5025/push/sendEmail",
                body.toJSONString());
        System.out.println(response);
    }

    public static void main(String[] args) {
        new CloudFunction().handleRequest(null, null, null);
    }
}
