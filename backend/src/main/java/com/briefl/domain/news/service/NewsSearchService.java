package com.briefl.domain.news.service;

import com.briefl.domain.news.dto.NaverNewsItem;
import com.briefl.domain.news.dto.NaverNewsSearchResponse;
import com.briefl.domain.news.dto.NewsItemDto;
import com.briefl.domain.news.dto.NewsSearchResult;
import com.briefl.domain.news.dto.NewsType;
import com.briefl.domain.stock.entity.Stock;
import com.briefl.global.config.NaverProperties;
import com.briefl.global.exception.ErrorCode;
import com.briefl.global.exception.ExternalApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsSearchService {

    private static final int DIRECT_NEWS_LIMIT = 5;
    private static final int INDIRECT_NEWS_LIMIT = 5;
    private static final int SEARCH_DISPLAY_SIZE = 10;
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final NaverProperties naverProperties;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NewsSearchResult searchTodayNews(Stock stock) {
        validateNaverCredentials();

        List<NewsItemDto> directNews = collectTodayNews(
                parseKeywords(stock.getDirectKeywordsJson()),
                NewsType.DIRECT,
                DIRECT_NEWS_LIMIT,
                stock.getStockName(),
                Set.of()
        );
        List<NewsItemDto> indirectNews = collectTodayNews(
                parseKeywords(stock.getIndirectKeywordsJson()),
                NewsType.INDIRECT,
                INDIRECT_NEWS_LIMIT,
                stock.getStockName(),
                normalizedTitles(directNews)
        );

        return new NewsSearchResult(directNews, indirectNews);
    }

    private List<NewsItemDto> collectTodayNews(
            List<String> keywords,
            NewsType newsType,
            int limit,
            String stockName,
            Set<String> excludedTitles
    ) {
        Map<String, NewsItemDto> uniqueNews = new LinkedHashMap<>();
        LocalDate today = LocalDate.now(SEOUL_ZONE);

        for (String keyword : keywords) {
            if (uniqueNews.size() >= limit) {
                break;
            }

            searchByKeyword(keyword, newsType).stream()
                    .filter(news -> news.publishedAt() != null)
                    .filter(news -> news.publishedAt().toLocalDate().isEqual(today))
                    .filter(news -> newsType != NewsType.DIRECT || containsStockName(news, stockName))
                    .filter(news -> !excludedTitles.contains(normalizeTitle(news.title())))
                    .forEach(news -> uniqueNews.putIfAbsent(normalizeTitle(news.title()), news));
        }

        return uniqueNews.values().stream()
                .limit(limit)
                .toList();
    }

    private List<NewsItemDto> searchByKeyword(String keyword, NewsType newsType) {
        try {
            NaverNewsSearchResponse response = webClientBuilder.build()
                    .get()
                    .uri(UriComponentsBuilder.fromUriString(naverProperties.newsSearchUrl())
                            .queryParam("query", keyword)
                            .queryParam("display", SEARCH_DISPLAY_SIZE)
                            .queryParam("start", 1)
                            .queryParam("sort", "date")
                            .encode()
                            .build()
                            .toUri())
                    .header("X-Naver-Client-Id", naverProperties.clientId())
                    .header("X-Naver-Client-Secret", naverProperties.clientSecret())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(body -> new ExternalApiException(
                                            ErrorCode.EXTERNAL_API_ERROR,
                                            "네이버 뉴스 검색 API 호출에 실패했습니다."
                                    )))
                    .bodyToMono(NaverNewsSearchResponse.class)
                    .block(Duration.ofSeconds(10));

            if (response == null || response.items() == null) {
                return List.of();
            }

            return response.items().stream()
                    .filter(Objects::nonNull)
                    .map(item -> toNewsItem(item, newsType))
                    .toList();
        } catch (WebClientResponseException exception) {
            throw new ExternalApiException(ErrorCode.EXTERNAL_API_ERROR, "네이버 뉴스 검색 API 응답 처리에 실패했습니다.");
        } catch (RuntimeException exception) {
            if (exception instanceof ExternalApiException externalApiException) {
                throw externalApiException;
            }
            throw new ExternalApiException(ErrorCode.EXTERNAL_API_ERROR, "네이버 뉴스 검색 API 호출 중 오류가 발생했습니다.");
        }
    }

    private NewsItemDto toNewsItem(NaverNewsItem item, NewsType newsType) {
        String title = cleanText(item.title());
        String description = cleanText(item.description());
        String url = resolveUrl(item);

        return new NewsItemDto(
                title,
                description,
                url,
                extractSource(url),
                parsePublishedAt(item.pubDate()),
                newsType
        );
    }

    private List<String> parseKeywords(String keywordsJson) {
        try {
            return objectMapper.readValue(keywordsJson, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new ExternalApiException(ErrorCode.EXTERNAL_API_ERROR, "종목 뉴스 키워드 파싱에 실패했습니다.");
        }
    }

    private void validateNaverCredentials() {
        if (!StringUtils.hasText(naverProperties.clientId()) || !StringUtils.hasText(naverProperties.clientSecret())) {
            throw new ExternalApiException(ErrorCode.EXTERNAL_API_AUTH_MISSING);
        }
    }

    private String cleanText(String value) {
        if (value == null) {
            return "";
        }
        return HtmlUtils.htmlUnescape(value.replaceAll("<[^>]*>", "")).trim();
    }

    private String resolveUrl(NaverNewsItem item) {
        if (StringUtils.hasText(item.originallink())) {
            return item.originallink();
        }
        if (StringUtils.hasText(item.link())) {
            return item.link();
        }
        return "";
    }

    private String extractSource(String url) {
        if (!StringUtils.hasText(url)) {
            return "네이버 뉴스";
        }

        try {
            String host = URI.create(url).getHost();
            if (!StringUtils.hasText(host)) {
                return "네이버 뉴스";
            }
            return host.replaceFirst("^www\\.", "");
        } catch (IllegalArgumentException exception) {
            return "네이버 뉴스";
        }
    }

    private LocalDateTime parsePublishedAt(String pubDate) {
        if (!StringUtils.hasText(pubDate)) {
            return null;
        }

        try {
            return ZonedDateTime.parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME)
                    .withZoneSameInstant(SEOUL_ZONE)
                    .toLocalDateTime();
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private String normalizeTitle(String title) {
        return title.replaceAll("\\s+", " ").trim();
    }

    private boolean containsStockName(NewsItemDto news, String stockName) {
        return news.title().contains(stockName) || news.description().contains(stockName);
    }

    private Set<String> normalizedTitles(List<NewsItemDto> newsItems) {
        Set<String> titles = new HashSet<>();
        for (NewsItemDto newsItem : newsItems) {
            titles.add(normalizeTitle(newsItem.title()));
        }
        return titles;
    }
}
