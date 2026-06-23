package com.aiwechat.knowledge.service.seed;

import com.aiwechat.knowledge.service.KnowledgeBaseService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 默认知识种子提供器
 * 提供配送规则、订单规则和 FAQ 的基础知识内容
 */
@Component
public class DefaultKnowledgeSeedProvider {

    public SeedBundle buildDefaultSeeds() {
        List<KnowledgeBaseService.TextItem> deliveryRules = buildDeliveryRules();
        List<KnowledgeBaseService.TextItem> orderRules = buildOrderRules();
        List<KnowledgeBaseService.TextItem> faqs = buildFaqs();
        return new SeedBundle(deliveryRules, orderRules, faqs);
    }

    private List<KnowledgeBaseService.TextItem> buildDeliveryRules() {
        List<KnowledgeBaseService.TextItem> texts = new ArrayList<>();

        texts.add(ruleText(
                "配送规则-配送范围与起送价",
                "delivery_rule",
                "delivery_scope",
                """
                第一条 配送范围
                门店默认配送范围为门店周边 5 公里内，覆盖写字楼、公寓和住宅区。
                超出配送范围的地址，系统会在下单时提示暂不支持配送，建议选择到店自取或更换地址。

                第二条 起送价
                门店标准起送价为 18 元，早餐时段和夜宵时段可能根据运力调整到 25 元，以页面展示为准。
                未达到起送价的订单无法提交，需要继续加购后再支付。

                第三条 地址填写要求
                配送地址需填写楼栋、单元、门牌号，写字楼订单建议补充公司名和前台电话，避免配送延误。
                """
        ));

        texts.add(ruleText(
                "配送规则-配送费与免配送费",
                "delivery_rule",
                "delivery_fee",
                """
                第一条 配送费标准
                0 到 3 公里内基础配送费 3 元，3 到 5 公里内基础配送费 5 元，恶劣天气或高峰期可能上浮 1 到 3 元。

                第二条 免配送费规则
                订单实付金额满 39 元可减免 2 元配送费，满 59 元可免配送费。
                如使用大额优惠券导致实付金额低于门槛，则按实付金额重新计算配送费。

                第三条 费用展示
                配送费以提交订单页展示为准，支付完成后不再单独退回配送费，除非门店或骑手原因导致订单取消。
                """
        ));

        texts.add(ruleText(
                "配送规则-配送时效与异常处理",
                "delivery_rule",
                "delivery_timing",
                """
                第一条 常规时效
                正常情况下接单后 30 到 45 分钟送达，高峰期、暴雨天气或大型活动期间可能延长至 60 分钟以上。

                第二条 时效通知
                订单出餐、骑手取餐、即将送达都会在小程序订单详情页更新状态，用户也可联系门店确认进度。

                第三条 异常处理
                如骑手联系不上用户、地址错误或门禁无法进入，配送员会优先电话联系。
                超过 10 分钟仍无法完成交付的订单，门店可转为自取或与用户协商退款。
                """
        ));

        return texts;
    }

    private List<KnowledgeBaseService.TextItem> buildOrderRules() {
        List<KnowledgeBaseService.TextItem> texts = new ArrayList<>();

        texts.add(ruleText(
                "订单规则-下单支付与自动取消",
                "order_rule",
                "order_lifecycle",
                """
                第一条 下单生效
                用户提交订单并完成支付后，订单状态会进入待接单；未支付订单不进入后厨制作。

                第二条 自动取消
                未支付订单在创建后 15 分钟内未完成支付会自动取消，库存和优惠资格会同步释放。

                第三条 接单说明
                门店确认接单后，订单会进入制作中状态；如门店繁忙或菜品售罄，门店有权拒单并原路退款。
                """
        ));

        texts.add(ruleText(
                "订单规则-取消与退款",
                "order_rule",
                "order_refund",
                """
                第一条 用户主动取消
                订单在门店接单前可由用户自行取消，系统会按原支付路径发起退款。

                第二条 接单后取消
                订单进入制作中后原则上不支持无理由取消；如因地址填写错误、配送超时或菜品异常，可联系人工客服处理。

                第三条 退款时效
                微信支付退款通常在 1 到 3 个工作日内到账，节假日或银行处理高峰可能略有延迟。
                """
        ));

        texts.add(ruleText(
                "订单规则-售后与改单",
                "order_rule",
                "after_sale",
                """
                第一条 订单修改
                订单提交后暂不支持在线改单，如需加菜、减菜或修改地址，请尽快联系门店或人工客服确认是否可处理。

                第二条 售后范围
                如出现漏餐、错餐、包装破损、明显洒漏、口味严重异常等问题，可在签收后 2 小时内提交售后反馈。

                第三条 售后方式
                门店会根据问题类型提供补送、部分退款、整单退款或优惠券补偿，最终处理以客服审核结果为准。
                """
        ));

        return texts;
    }

    private List<KnowledgeBaseService.TextItem> buildFaqs() {
        List<KnowledgeBaseService.TextItem> texts = new ArrayList<>();

        texts.add(faqText(
                "常见问题-营业时间",
                "store_hours",
                """
                Q: 营业时间是几点到几点？
                A: 门店默认营业时间为每天 09:00 到 21:30，节假日或门店活动期间可能调整，以小程序首页公告为准。
                """
        ));

        texts.add(faqText(
                "常见问题-发票",
                "invoice",
                """
                Q: 可以开发票吗？
                A: 支持电子发票。下单后可联系人工客服登记开票信息，发票通常会在 1 个工作日内发送到预留邮箱。
                """
        ));

        texts.add(faqText(
                "常见问题-优惠活动",
                "promotion",
                """
                Q: 现在有什么优惠活动？
                A: 常见活动包括首单立减、满减优惠、满额免配送费和节日限时券，具体活动以小程序下单页实时展示为准。
                """
        ));

        texts.add(faqText(
                "常见问题-口味备注",
                "taste_customization",
                """
                Q: 可以加辣、少辣或者不要香菜吗？
                A: 大部分菜品支持口味备注，可在提交订单前填写少辣、不要香菜、少盐等需求；特殊定制会以门店实际出餐能力为准。
                """
        ));

        texts.add(faqText(
                "常见问题-自取",
                "pickup",
                """
                Q: 支持到店自取吗？
                A: 支持。若地址超出配送范围或用户希望自行取餐，可在下单时选择到店自取，并按预计取餐时间到店领取。
                """
        ));

        texts.add(faqText(
                "常见问题-催单",
                "urge_order",
                """
                Q: 订单太久没到怎么办？
                A: 可先在订单详情页查看当前状态；若超过预计送达时间仍未送达，可联系门店或人工客服协助催单与售后处理。
                """
        ));

        return texts;
    }

    private KnowledgeBaseService.TextItem ruleText(String title, String bizType, String category, String content) {
        return new KnowledgeBaseService.TextItem(title, content, bizType, category, title);
    }

    private KnowledgeBaseService.TextItem faqText(String title, String category, String content) {
        return new KnowledgeBaseService.TextItem(title, content, "faq", category, title);
    }

    public record SeedBundle(
            List<KnowledgeBaseService.TextItem> deliveryRules,
            List<KnowledgeBaseService.TextItem> orderRules,
            List<KnowledgeBaseService.TextItem> faqs) {

        public List<KnowledgeBaseService.TextItem> allTexts() {
            List<KnowledgeBaseService.TextItem> texts = new ArrayList<>();
            texts.addAll(deliveryRules);
            texts.addAll(orderRules);
            texts.addAll(faqs);
            return texts;
        }
    }
}
