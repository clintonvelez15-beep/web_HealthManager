package com.hnust.health.service;

import com.hnust.health.config.DeepSeekConfig;
import com.hnust.health.entity.PlanHistory;
import com.hnust.health.entity.UserProfile;
import com.hnust.health.entity.WeightRecord;
import com.hnust.health.mapper.PlanHistoryMapper;
import com.hnust.health.mapper.WeightRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeepSeekService {

    private final DeepSeekConfig config;
    private final WeightRecordMapper weightRecordMapper;
    private final PlanHistoryMapper planHistoryMapper;

    public String generatePlan(UserProfile profile, Long userId) {
        // 1. 查询历史体重记录（最近4周）
        List<WeightRecord> records = weightRecordMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WeightRecord>()
                .eq(WeightRecord::getUserId, userId)
                .orderByDesc(WeightRecord::getRecordDate)
                .last("LIMIT 8")
        );

        // 2. 构建体重变化描述
        StringBuilder weightContext = new StringBuilder();
        if (records.isEmpty()) {
            weightContext.append("用户首次使用，暂无历史体重记录。");
        } else {
            weightContext.append("最近体重记录：");
            for (WeightRecord r : records) {
                weightContext.append(String.format("%s: %.1fkg; ", 
                    r.getRecordDate().format(DateTimeFormatter.ofPattern("MM-dd")), r.getWeight()));
            }
            // 计算变化趋势
            if (records.size() >= 2) {
                double latest = records.get(0).getWeight();
                double earliest = records.get(records.size() - 1).getWeight();
                double change = latest - earliest;
                weightContext.append(String.format("总体变化: %.1fkg (%s)", 
                    Math.abs(change), change > 0 ? "上升" : "下降"));
            }
        }

        // 3. 构建Prompt
        String prompt = String.format(
            "你是一位专业的健康管理师。请根据以下用户信息生成个性化的一周健康计划。\n\n" +
            "【用户档案】\n" +
            "年龄: %d岁\n" +
            "性别: %s\n" +
            "身高: %.1fcm\n" +
            "当前体重: %.1fkg\n" +
            "活动水平: %s\n" +
            "饮食偏好: %s\n" +
            "健康目标: %s\n\n" +
            "【体重变化历史】\n%s\n\n" +
            "【要求】\n" +
            "1. 生成一周（周一至周日）的饮食计划，包含每日三餐建议\n" +
            "2. 生成一周的运动计划，包含每日运动类型、时长和强度\n" +
            "3. 根据体重变化趋势给出调整建议（如下降过快需增加热量，停滞需调整运动）\n" +
            "4. 用Markdown格式输出，标题清晰，内容具体可操作",
            profile.getAge(), profile.getGender(), profile.getHeight(), profile.getWeight(),
            profile.getActivityLevel(), profile.getDietPref(), profile.getGoal(),
            weightContext.toString()
        );

        // 4. 调用DeepSeek API
        String response = callDeepSeek(prompt);

        // 5. 保存到历史记录
        PlanHistory history = new PlanHistory();
        history.setUserId(userId);
        history.setPlanType("weekly");
        history.setContent(response);
        history.setWeightContext(weightContext.toString());
        planHistoryMapper.insert(history);

        return response;
    }

    private String callDeepSeek(String prompt) {
        RestTemplate rest = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(config.getApiKey());

        Map<String, Object> body = Map.of(
            "model", config.getModel(),
            "messages", List.of(Map.of("role", "user", "content", prompt)),
            "temperature", 0.7,
            "max_tokens", 2000
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            Map<String, Object> response = rest.postForObject(config.getApiUrl(), request, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            return "AI服务暂时不可用，请稍后重试。错误: " + e.getMessage();
        }
    }
}
