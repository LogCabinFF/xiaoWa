package com.lark.oapi;

import java.util.*;
import java.time.*;
import java.time.temporal.ChronoUnit;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.lark.oapi.core.utils.Jsons;
import com.lark.oapi.event.EventDispatcher;
import com.lark.oapi.service.im.ImService;
import com.lark.oapi.service.im.v1.enums.MsgTypeEnum;
import com.lark.oapi.service.im.v1.enums.ReceiveIdTypeEnum;
import com.lark.oapi.service.im.v1.model.CreateMessageReq;
import com.lark.oapi.service.im.v1.model.CreateMessageReqBody;
import com.lark.oapi.service.im.v1.model.CreateMessageResp;
import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1;
import com.lark.oapi.service.im.v1.model.ReplyMessageReq;
import com.lark.oapi.service.im.v1.model.ReplyMessageReqBody;
import com.lark.oapi.service.im.v1.model.ReplyMessageResp;
import com.lark.oapi.service.im.v1.model.ext.MessageText;

public class Main {

    private static final String APP_ID = System.getenv("APP_ID");
    private static final String APP_SECRET = System.getenv("APP_SECRET");

    // 使用List存储节假日数据，每个元素为Map结构‌:ml-citation{ref="1,2" data="citationList"}
    private static final List<Map<String, Object>> HOLIDAYS = Arrays.asList(
    // 元旦
    // Bug 修复：将 HashMap 的泛型参数改为具体的类型
    new HashMap<String, Object>() {{
        put("name", "元旦");
        put("date", LocalDate.of(2025, 1, 1));
        put("duration", 3);
    }},
    // 春节
    new HashMap<String, Object>() {{
        put("name", "春节");
        put("date", LocalDate.of(2025, 1, 28));
        put("duration", 7);
    }},
    // 妇女节
    new HashMap<String, Object>() {{
        put("name", "妇女节（周六）");
        put("date", LocalDate.of(2025, 3, 8));
        put("duration", 0); // 通常包含周末调休
    }},
    // 清明节
    new HashMap<String, Object>() {{
        put("name", "清明节");
        put("date", LocalDate.of(2025, 4, 4));
        put("duration", 3); // 通常包含周末调休
    }},
    // 劳动节
    new HashMap<String, Object>() {{
        put("name", "劳动节");
        put("date", LocalDate.of(2025, 5, 1));
        put("duration", 5); // 含调休
    }},
    // 端午节
    new HashMap<String, Object>() {{
        put("name", "端午节");
        put("date", LocalDate.of(2025, 5, 31));
        put("duration", 3); // 农历五月初五对应公历日期
    }},
    // 中秋节/国庆节
    new HashMap<String, Object>() {{
        put("name", "中秋国庆连休");
        put("date", LocalDate.of(2025, 10, 1));
        put("duration", 8); // 含调休
    }}
);

    /**
     * 创建 LarkClient 对象，用于请求OpenAPI。
     * Create LarkClient object for requesting OpenAPI
     */
    private static final Client client = new Client.Builder(APP_ID, APP_SECRET).build();

    /**
     * 注册事件处理器。
     * Register event handler.
     */
    private static final EventDispatcher EVENT_HANDLER = EventDispatcher.newBuilder("", "")
            /**
             * 注册接收消息事件，处理接收到的消息。
             * Register event handler to handle received messages.
             * https://open.feishu.cn/document/uAjLw4CM/ukTMukTMukTM/reference/im-v1/message/events/receive
             */
            .onP2MessageReceiveV1(new ImService.P2MessageReceiveV1Handler() {
                @Override
                public void handle(P2MessageReceiveV1 event) throws Exception {
                    System.out.printf("[ onP2MessageReceiveV1 access ], data: %s\n",
                            Jsons.DEFAULT.toJson(event.getEvent()));
                    /**
                     * 解析用户发送的消息。
                     * Parse the message sent by the user.
                     */
                    String content = event.getEvent().getMessage().getContent();
                    Map<String, String> respContent = new HashMap<>();
                    try {
                        respContent = new Gson().fromJson(content, new TypeToken<Map<String, String>>() {
                        }.getType());
                    } catch (JsonSyntaxException e) {
                        respContent.put("text", "解析消息失败，请发送文本消息\nparse message failed, please send text message");
                    }

                    /**
                     * 检查消息类型是否为文本
                     * Check if the message type is text
                     */
                    if (!event.getEvent().getMessage().getMessageType().equals("text")) {
                        respContent.put("text", "解析消息失败，请发送文本消息\nparse message failed, please send text message");
                    }
                    /*
                     * xb倒计时功能
                     */
                    String replyContent = "";
                    if(respContent.get("text").contains("tzxb")){
                        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
                        ZonedDateTime targetDateTime = now.with(LocalTime.of(18, 00));
                        Duration duration = Duration.between(now, targetDateTime);
                        /**
                         * 构建回复消息
                         * Build reply message
                         */
                        replyContent = new MessageText.Builder()
                                .textLine("距离xb: " + duration.toMinutes() + "分钟")
                                .build();
                    }else if(respContent.get("text").contains("xb")){
                        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
                        ZonedDateTime targetDateTime = now.with(LocalTime.of(17, 30));
                        Duration duration = Duration.between(now, targetDateTime);
                        /**
                         * 构建回复消息
                         * Build reply message
                         */
                        replyContent = new MessageText.Builder()
                                .textLine("距离xb: " + duration.toMinutes() + "分钟")
                                .build();
                    }else if(respContent.get("text").contains("gzt")){
                        // 获取当前上海时区时间‌:ml-citation{ref="4" data="citationList"}
                        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
                        
                        // 构建目标日期逻辑‌:ml-citation{ref="2,3" data="citationList"}
                        LocalDate targetDate = now.toLocalDate().withDayOfMonth(18);
                        if (now.getDayOfMonth() > 18 && now.getDayOfWeek() != DayOfWeek.SUNDAY) {
                            targetDate = targetDate.plusMonths(1);  // 跨月调整‌:ml-citation{ref="2" data="citationList"}
                        }
                        
                        // 组合目标日期的完整时间（保持原时区，时间设为当日起点）‌:ml-citation{ref="4" data="citationList"}
                        ZonedDateTime targetDateTime = targetDate.atStartOfDay(now.getZone());
                        
                        // 计算自然日间隔（忽略时间部分）‌:ml-citation{ref="2,3" data="citationList"}
                        long daysBetween = ChronoUnit.DAYS.between(now.toLocalDate(), targetDateTime.toLocalDate());
                        // 判断是否为周六或周日
                        DayOfWeek dayOfWeek = targetDateTime.toLocalDate().getDayOfWeek();
                        if(dayOfWeek == DayOfWeek.SATURDAY){
                            daysBetween = daysBetween - 1;
                        }else if(dayOfWeek == DayOfWeek.SUNDAY){
                            daysBetween = daysBetween + 1;
                        }

                        replyContent = new MessageText.Builder()
                                .textLine("距离fgzt: " + daysBetween + "天")
                                .build();
                    }else if(respContent.get("text").contains("fx")){
                        // 获取当前上海时区时间‌:ml-citation{ref="4" data="citationList"}
                        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
                        
                        // 构建目标日期逻辑‌:ml-citation{ref="2,3" data="citationList"}
                        LocalDate targetDate = now.toLocalDate().withDayOfMonth(22);
                        if (now.getDayOfMonth() > 22) {
                            targetDate = targetDate.plusMonths(1);  // 跨月调整‌:ml-citation{ref="2" data="citationList"}
                        }
                        
                        // 组合目标日期的完整时间（保持原时区，时间设为当日起点）‌:ml-citation{ref="4" data="citationList"}
                        ZonedDateTime targetDateTime = targetDate.atStartOfDay(now.getZone());
                        
                        // 计算自然日间隔（忽略时间部分）‌:ml-citation{ref="2,3" data="citationList"}
                        long daysBetween = ChronoUnit.DAYS.between(now.toLocalDate(), targetDateTime.toLocalDate());
                        // 判断是否为周六或周日
                        DayOfWeek dayOfWeek = targetDateTime.toLocalDate().getDayOfWeek();
                        if(dayOfWeek == DayOfWeek.SATURDAY){
                            daysBetween = daysBetween - 1;
                        }else if(dayOfWeek == DayOfWeek.SUNDAY){
                            daysBetween = daysBetween - 2;
                        }

                        replyContent = new MessageText.Builder()
                                .textLine("距离fx: " + daysBetween + "天")
                                .build();
                    }else if(respContent.get("text").contains("fj")){
                        // 获取上海时区当前日期‌:ml-citation{ref="1,2" data="citationList"}
                        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
                        
                        // 筛选未来节假日并排序‌:ml-citation{ref="1,2" data="citationList"}
                        Optional<Map<String, Object>> nearestHoliday = HOLIDAYS.stream()
                            .filter(h -> ((LocalDate)h.get("date")).isAfter(today))
                            .min(Comparator.comparing(h -> (LocalDate)h.get("date")));

                        if (nearestHoliday.isPresent()) {
                            LocalDate holidayDate = (LocalDate) nearestHoliday.get().get("date");
                            long days = ChronoUnit.DAYS.between(today, holidayDate);
                            replyContent = new MessageText.Builder()
                                .textLine("距离" + nearestHoliday.get().get("name") + "还有" + days + "天，放假" + nearestHoliday.get().get("duration") + "天")
                                .build();
                        } else {
                            replyContent = new MessageText.Builder()
                                .textLine("今年法定节假日已全部结束~")
                                .build();
                        }
                    }else if(respContent.get("text").contains("miss")){
                        replyContent = new MessageText.Builder()
                                .textLine("Bye, i will miss you~")
                                .build();
                    }else{
                        replyContent = new MessageText.Builder()
                                .textLine("小蛙还在成长中哦~")
                                .build();
                    }
                    System.out.println(replyContent);
                    if (event.getEvent().getMessage().getChatType().equals("p2p")) {
                        CreateMessageReq req = CreateMessageReq.newBuilder()
                                .receiveIdType(ReceiveIdTypeEnum.CHAT_ID.getValue())
                                .createMessageReqBody(CreateMessageReqBody.newBuilder()
                                        .receiveId(event.getEvent().getMessage().getChatId())
                                        .msgType(MsgTypeEnum.MSG_TYPE_TEXT.getValue())
                                        .content(replyContent)
                                        .build())
                                .build();

                        /**
                         * 使用SDK调用发送消息接口。 Use SDK to call send message interface.
                         * https://open.feishu.cn/document/uAjLw4CM/ukTMukTMukTM/reference/im-v1/message/create
                         */
                        try {
                            CreateMessageResp resp = client.im().message().create(req);
                            if (resp.getCode() != 0) {
                                System.out.println(String.format("logId: %s, error response: \n%s",
                                        resp.getRequestId(), Jsons.DEFAULT.toJson(resp.getError())));
                            }
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                    } else {
                        /**
                         * 使用SDK调用回复消息接口。 Use SDK to call reply message interface.
                         * https://open.feishu.cn/document/server-docs/im-v1/message/reply
                         */
                        ReplyMessageReq req = ReplyMessageReq.newBuilder()
                                .messageId(event.getEvent().getMessage().getMessageId())
                                .replyMessageReqBody(ReplyMessageReqBody.newBuilder()
                                        .content(replyContent)
                                        .msgType("text")
                                        .build())
                                .build();
                        try {
                            // 发起请求
                            ReplyMessageResp resp = client.im().message().reply(req);
                            if (resp.getCode() != 0) {
                                System.out.println(String.format("logId: %s, error response: \n%s", resp.getRequestId(),
                                        Jsons.DEFAULT.toJson(resp.getError())));
                            }
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                    }
                    
                }
            })
            .build();

    /**
     * 启动长连接，并注册事件处理器。
     * Start long connection and register event handler.
     */
    private static final com.lark.oapi.ws.Client wsClient = new com.lark.oapi.ws.Client.Builder(APP_ID, APP_SECRET)
            .eventHandler(EVENT_HANDLER).build();

    public static void main(String[] args) {
        System.out.println("Starting bot...");
        wsClient.start();
    }
}
