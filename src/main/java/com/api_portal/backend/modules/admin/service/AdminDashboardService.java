package com.api_portal.backend.modules.admin.service;

import com.api_portal.backend.modules.admin.dto.DashboardStatsResponse;
import com.api_portal.backend.modules.admin.dto.PendingSubscriptionResponse;
import com.api_portal.backend.modules.admin.dto.RecentActivityResponse;
import com.api_portal.backend.modules.admin.dto.SystemAlertsResponse;
import com.api_portal.backend.modules.admin.dto.TopRankingsResponse;
import com.api_portal.backend.modules.api.domain.Api;
import com.api_portal.backend.modules.api.domain.enums.ApiStatus;
import com.api_portal.backend.modules.api.repository.ApiRepository;
import com.api_portal.backend.modules.subscription.domain.entity.Subscription;
import com.api_portal.backend.modules.subscription.domain.enums.SubscriptionStatus;
import com.api_portal.backend.modules.subscription.domain.repository.SubscriptionRepository;
import com.api_portal.backend.modules.user.domain.User;
import com.api_portal.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardService {
    
    private final UserRepository userRepository;
    private final ApiRepository apiRepository;
    private final SubscriptionRepository subscriptionRepository;
    
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        return DashboardStatsResponse.builder()
            .userStats(getUserStats())
            .apiStats(getApiStats())
            .subscriptionStats(getSubscriptionStats())
            .activityStats(getActivityStats())
            .build();
    }
    
    private DashboardStatsResponse.UserStats getUserStats() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        
        long totalUsers = userRepository.count();
        long totalProviders = userRepository.countByRolesContaining("PROVIDER");
        long totalConsumers = userRepository.countByRolesContaining("CONSUMER");
        long totalAdmins = userRepository.countByRolesContaining("SUPER_ADMIN");
        long newUsersLast7Days = userRepository.countByCreatedAtAfter(sevenDaysAgo);
        long usersLast30Days = userRepository.countByCreatedAtAfter(thirtyDaysAgo);
        
        double growthPercentage = calculateGrowthPercentage(newUsersLast7Days, usersLast30Days - newUsersLast7Days);
        
        return DashboardStatsResponse.UserStats.builder()
            .totalUsers(totalUsers)
            .totalProviders(totalProviders)
            .totalConsumers(totalConsumers)
            .totalAdmins(totalAdmins)
            .newUsersLast7Days(newUsersLast7Days)
            .growthPercentage(growthPercentage)
            .build();
    }
    
    private DashboardStatsResponse.ApiStats getApiStats() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        
        long totalApis = apiRepository.count();
        long publishedApis = apiRepository.countByStatus(ApiStatus.PUBLISHED);
        long draftApis = apiRepository.countByStatus(ApiStatus.DRAFT);
        long newApisLast7Days = apiRepository.countByCreatedAtAfter(sevenDaysAgo);
        
        double growthPercentage = totalApis > 0 ? (newApisLast7Days * 100.0 / totalApis) : 0.0;
        
        // Contar APIs por categoria
        List<Api> allApis = apiRepository.findAll();
        Map<String, Long> apisByCategory = allApis.stream()
            .collect(Collectors.groupingBy(
                api -> api.getCategory() != null ? api.getCategory().getName() : "Sem Categoria",
                Collectors.counting()
            ));
        
        return DashboardStatsResponse.ApiStats.builder()
            .totalApis(totalApis)
            .publishedApis(publishedApis)
            .draftApis(draftApis)
            .newApisLast7Days(newApisLast7Days)
            .growthPercentage(growthPercentage)
            .apisByCategory(apisByCategory)
            .build();
    }
    
    private DashboardStatsResponse.SubscriptionStats getSubscriptionStats() {
        long totalSubscriptions = subscriptionRepository.count();
        long activeSubscriptions = subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);
        long pendingSubscriptions = subscriptionRepository.countByStatus(SubscriptionStatus.PENDING);
        long revokedSubscriptions = subscriptionRepository.countByStatus(SubscriptionStatus.REVOKED);
        
        double growthPercentage = totalSubscriptions > 0 ? (activeSubscriptions * 100.0 / totalSubscriptions) : 0.0;
        
        return DashboardStatsResponse.SubscriptionStats.builder()
            .totalSubscriptions(totalSubscriptions)
            .activeSubscriptions(activeSubscriptions)
            .pendingSubscriptions(pendingSubscriptions)
            .revokedSubscriptions(revokedSubscriptions)
            .growthPercentage(growthPercentage)
            .build();
    }
    
    private DashboardStatsResponse.ActivityStats getActivityStats() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        
        long activeUsersLast30Days = userRepository.countByLastLoginAfter(thirtyDaysAgo);
        long totalUsers = userRepository.count();
        
        double engagementRate = totalUsers > 0 ? (activeUsersLast30Days * 100.0 / totalUsers) : 0.0;
        
        return DashboardStatsResponse.ActivityStats.builder()
            .activeUsersLast30Days(activeUsersLast30Days)
            .engagementRate(engagementRate)
            .totalApiCalls(0L) // TODO: Implementar quando houver sistema de métricas
            .build();
    }
    
    @Transactional(readOnly = true)
    public List<RecentActivityResponse> getRecentActivities(int limit) {
        List<RecentActivityResponse> activities = new ArrayList<>();
        
        // Últimos usuários registrados
        List<User> recentUsers = userRepository.findTop5ByOrderByCreatedAtDesc();
        for (User user : recentUsers) {
            activities.add(RecentActivityResponse.builder()
                .id(user.getId().toString())
                .type(RecentActivityResponse.ActivityType.USER_REGISTERED)
                .title("Novo Usuário Registrado")
                .description(user.getUsername() + " se registrou na plataforma")
                .userName(user.getUsername())
                .userEmail(user.getEmail())
                .timestamp(user.getCreatedAt())
                .icon("ri-user-add-line")
                .iconColor("text-success")
                .build());
        }
        
        // Últimas APIs publicadas
        List<Api> recentApis = apiRepository.findTop5ByStatusOrderByPublishedAtDesc(ApiStatus.PUBLISHED);
        for (Api api : recentApis) {
            activities.add(RecentActivityResponse.builder()
                .id(api.getId().toString())
                .type(RecentActivityResponse.ActivityType.API_PUBLISHED)
                .title("Nova API Publicada")
                .description(api.getName() + " foi publicada")
                .userName(api.getProviderName())
                .userEmail(api.getProviderEmail())
                .timestamp(api.getPublishedAt() != null ? api.getPublishedAt() : api.getCreatedAt())
                .icon("ri-rocket-line")
                .iconColor("text-primary")
                .build());
        }
        
        // Últimas subscriptions
        List<Subscription> recentSubscriptions = subscriptionRepository.findTop5ByOrderByCreatedAtDesc();
        for (Subscription sub : recentSubscriptions) {
            String icon = "ri-checkbox-circle-line";
            String color = "text-info";
            String title = "Nova Subscription";
            
            if (sub.getStatus() == SubscriptionStatus.ACTIVE) {
                icon = "ri-checkbox-circle-line";
                color = "text-success";
                title = "Subscription Aprovada";
            } else if (sub.getStatus() == SubscriptionStatus.PENDING) {
                icon = "ri-time-line";
                color = "text-warning";
                title = "Subscription Pendente";
            }
            
            activities.add(RecentActivityResponse.builder()
                .id(sub.getId().toString())
                .type(RecentActivityResponse.ActivityType.SUBSCRIPTION_CREATED)
                .title(title)
                .description(sub.getConsumerName() + " subscreveu " + sub.getApi().getName())
                .userName(sub.getConsumerName())
                .userEmail(sub.getConsumerEmail())
                .timestamp(sub.getCreatedAt())
                .icon(icon)
                .iconColor(color)
                .build());
        }
        
        // Ordenar por timestamp e limitar
        return activities.stream()
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<PendingSubscriptionResponse> getPendingSubscriptions() {
        List<Subscription> pendingSubscriptions = subscriptionRepository.findByStatus(SubscriptionStatus.PENDING);
        
        return pendingSubscriptions.stream()
            .map(sub -> {
                long daysWaiting = ChronoUnit.DAYS.between(sub.getCreatedAt(), LocalDateTime.now());
                
                return PendingSubscriptionResponse.builder()
                    .id(sub.getId())
                    .apiName(sub.getApi().getName())
                    .apiSlug(sub.getApi().getSlug())
                    .apiId(sub.getApi().getId())
                    .consumerName(sub.getConsumerName())
                    .consumerEmail(sub.getConsumerEmail())
                    .consumerId(sub.getConsumerId())
                    .providerName(sub.getApi().getProviderName())
                    .providerId(sub.getApi().getProviderId())
                    .requestedAt(sub.getCreatedAt())
                    .daysWaiting(daysWaiting)
                    .build();
            })
            .collect(Collectors.toList());
    }
    
    private double calculateGrowthPercentage(long current, long previous) {
        if (previous == 0) return current > 0 ? 100.0 : 0.0;
        return ((current - previous) * 100.0) / previous;
    }
    
    @Transactional(readOnly = true)
    public TopRankingsResponse getTopRankings() {
        return TopRankingsResponse.builder()
            .topApis(getTopApis())
            .topProviders(getTopProviders())
            .topConsumers(getTopConsumers())
            .build();
    }
    
    private List<TopRankingsResponse.TopApi> getTopApis() {
        List<Api> allApis = apiRepository.findByStatus(ApiStatus.PUBLISHED);
        
        return allApis.stream()
            .map(api -> {
                long subCount = subscriptionRepository.countByApiId(api.getId());
                return TopRankingsResponse.TopApi.builder()
                    .id(api.getId())
                    .name(api.getName())
                    .slug(api.getSlug())
                    .subscriptionCount(subCount)
                    .providerName(api.getProviderName())
                    .categoryName(api.getCategory() != null ? api.getCategory().getName() : "Sem Categoria")
                    .build();
            })
            .sorted((a, b) -> Long.compare(b.getSubscriptionCount(), a.getSubscriptionCount()))
            .limit(5)
            .collect(Collectors.toList());
    }
    
    private List<TopRankingsResponse.TopProvider> getTopProviders() {
        List<Api> allApis = apiRepository.findAll();
        
        return allApis.stream()
            .collect(Collectors.groupingBy(Api::getProviderId))
            .entrySet().stream()
            .map(entry -> {
                String providerId = entry.getKey();
                List<Api> providerApis = entry.getValue();
                
                long totalSubs = providerApis.stream()
                    .mapToLong(api -> subscriptionRepository.countByApiId(api.getId()))
                    .sum();
                
                Api firstApi = providerApis.get(0);
                
                return TopRankingsResponse.TopProvider.builder()
                    .providerId(providerId)
                    .providerName(firstApi.getProviderName())
                    .providerEmail(firstApi.getProviderEmail())
                    .apiCount((long) providerApis.size())
                    .totalSubscriptions(totalSubs)
                    .build();
            })
            .sorted((a, b) -> Long.compare(b.getApiCount(), a.getApiCount()))
            .limit(5)
            .collect(Collectors.toList());
    }
    
    private List<TopRankingsResponse.TopConsumer> getTopConsumers() {
        List<Subscription> allSubs = subscriptionRepository.findAll();
        
        return allSubs.stream()
            .collect(Collectors.groupingBy(Subscription::getConsumerId))
            .entrySet().stream()
            .map(entry -> {
                String consumerId = entry.getKey();
                List<Subscription> consumerSubs = entry.getValue();
                
                long activeSubs = consumerSubs.stream()
                    .filter(sub -> sub.getStatus() == SubscriptionStatus.ACTIVE)
                    .count();
                
                Subscription firstSub = consumerSubs.get(0);
                
                return TopRankingsResponse.TopConsumer.builder()
                    .consumerId(consumerId)
                    .consumerName(firstSub.getConsumerName())
                    .consumerEmail(firstSub.getConsumerEmail())
                    .subscriptionCount((long) consumerSubs.size())
                    .activeSubscriptions(activeSubs)
                    .build();
            })
            .sorted((a, b) -> Long.compare(b.getSubscriptionCount(), a.getSubscriptionCount()))
            .limit(5)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public SystemAlertsResponse getSystemAlerts() {
        List<SystemAlertsResponse.SystemAlert> alerts = new ArrayList<>();
        
        // APIs sem subscriptions há mais de 30 dias
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Api> publishedApis = apiRepository.findByStatusAndPublishedAtBefore(ApiStatus.PUBLISHED, thirtyDaysAgo);
        
        for (Api api : publishedApis) {
            long subCount = subscriptionRepository.countByApiId(api.getId());
            if (subCount == 0) {
                alerts.add(SystemAlertsResponse.SystemAlert.builder()
                    .id(api.getId())
                    .type(SystemAlertsResponse.AlertType.API_NO_SUBSCRIPTIONS)
                    .severity(SystemAlertsResponse.AlertSeverity.WARNING)
                    .title("API sem Subscriptions")
                    .message("A API '" + api.getName() + "' está publicada há mais de 30 dias sem nenhuma subscription")
                    .entityId(api.getId().toString())
                    .entityName(api.getName())
                    .createdAt(api.getPublishedAt())
                    .actionUrl("/admin/apis/" + api.getSlug())
                    .build());
            }
        }
        
        // Subscriptions pendentes há mais de 7 dias
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Subscription> oldPendingSubs = subscriptionRepository.findByStatusAndCreatedAtBefore(
            SubscriptionStatus.PENDING, sevenDaysAgo
        );
        
        for (Subscription sub : oldPendingSubs) {
            long daysWaiting = ChronoUnit.DAYS.between(sub.getCreatedAt(), LocalDateTime.now());
            alerts.add(SystemAlertsResponse.SystemAlert.builder()
                .id(sub.getId())
                .type(SystemAlertsResponse.AlertType.SUBSCRIPTION_PENDING_LONG)
                .severity(daysWaiting > 14 ? SystemAlertsResponse.AlertSeverity.ERROR : SystemAlertsResponse.AlertSeverity.WARNING)
                .title("Subscription Pendente há " + daysWaiting + " dias")
                .message(sub.getConsumerName() + " aguarda aprovação para " + sub.getApi().getName())
                .entityId(sub.getId().toString())
                .entityName(sub.getApi().getName())
                .createdAt(sub.getCreatedAt())
                .actionUrl("/admin/subscriptions/" + sub.getId())
                .build());
        }
        
        // APIs em draft há mais de 60 dias
        LocalDateTime sixtyDaysAgo = LocalDateTime.now().minusDays(60);
        List<Api> oldDraftApis = apiRepository.findByStatusAndCreatedAtBefore(ApiStatus.DRAFT, sixtyDaysAgo);
        
        for (Api api : oldDraftApis) {
            long daysInDraft = ChronoUnit.DAYS.between(api.getCreatedAt(), LocalDateTime.now());
            alerts.add(SystemAlertsResponse.SystemAlert.builder()
                .id(api.getId())
                .type(SystemAlertsResponse.AlertType.API_DRAFT_LONG)
                .severity(SystemAlertsResponse.AlertSeverity.INFO)
                .title("API em Draft há " + daysInDraft + " dias")
                .message("A API '" + api.getName() + "' está em draft há muito tempo")
                .entityId(api.getId().toString())
                .entityName(api.getName())
                .createdAt(api.getCreatedAt())
                .actionUrl("/admin/apis/" + api.getSlug())
                .build());
        }
        
        // Ordenar por severidade e data
        return SystemAlertsResponse.builder()
            .alerts(alerts.stream()
                .sorted((a, b) -> {
                    int severityCompare = getSeverityOrder(b.getSeverity()) - getSeverityOrder(a.getSeverity());
                    if (severityCompare != 0) return severityCompare;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .collect(Collectors.toList()))
            .build();
    }
    
    private int getSeverityOrder(SystemAlertsResponse.AlertSeverity severity) {
        if (severity == SystemAlertsResponse.AlertSeverity.CRITICAL) return 4;
        if (severity == SystemAlertsResponse.AlertSeverity.ERROR) return 3;
        if (severity == SystemAlertsResponse.AlertSeverity.WARNING) return 2;
        return 1; // INFO
    }

}
