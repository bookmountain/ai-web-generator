package com.book.aiwebgenerator.langgraph4j.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.book.aiwebgenerator.langgraph4j.model.ImageResource;
import com.book.aiwebgenerator.langgraph4j.model.enums.ImageCategoryEnum;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class UndrawIllustrationTool {

    private static final String UNDRAW_SEARCH_URL = "https://undraw.co/search/%s";
    private static final String NEXT_DATA_ID = "id=\"__NEXT_DATA__\"";
    private static final String SCRIPT_END = "</script>";

    @Tool("Search for illustration images for website embellishment and decoration")
    public List<ImageResource> searchIllustrations(@P("Search keyword") String query) {
        List<ImageResource> imageList = new ArrayList<>();
        int searchCount = 12;
        if (StrUtil.isBlank(query)) {
            return imageList;
        }

        String searchTerm = query.trim().replaceAll("\\s+", "-");
        String encodedSearchTerm = URLEncoder.encode(searchTerm, StandardCharsets.UTF_8)
                .replace("+", "%20");
        String searchUrl = String.format(UNDRAW_SEARCH_URL, encodedSearchTerm);

        // Use try-with-resources to automatically release HTTP resources
        try (HttpResponse response = HttpRequest.get(searchUrl).timeout(10000).execute()) {
            if (!response.isOk()) {
                log.warn("unDraw search returned HTTP {} for query '{}'", response.getStatus(), query);
                return imageList;
            }

            String nextData = extractNextData(response.body());
            if (StrUtil.isBlank(nextData)) {
                log.warn("unDraw search response did not contain __NEXT_DATA__ for query '{}'", query);
                return imageList;
            }

            JSONObject result = JSONUtil.parseObj(nextData);
            JSONObject props = result.getJSONObject("props");
            JSONObject pageProps = props == null ? null : props.getJSONObject("pageProps");
            if (pageProps == null) {
                return imageList;
            }
            JSONArray initialResults = pageProps.getJSONArray("initialResults");
            if (initialResults == null || initialResults.isEmpty()) {
                return imageList;
            }
            int actualCount = Math.min(searchCount, initialResults.size());
            for (int i = 0; i < actualCount; i++) {
                JSONObject illustration = initialResults.getJSONObject(i);
                String title = illustration.getStr("title", "Illustration");
                String media = illustration.getStr("media", "");
                if (StrUtil.isNotBlank(media)) {
                    imageList.add(ImageResource.builder()
                            .category(ImageCategoryEnum.ILLUSTRATION)
                            .description(title)
                            .url(media)
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("Failed to search illustrations: {}", e.getMessage(), e);
        }
        return imageList;
    }

    private String extractNextData(String html) {
        int idStart = html.indexOf(NEXT_DATA_ID);
        if (idStart < 0) {
            return null;
        }

        int contentStart = html.indexOf('>', idStart);
        int contentEnd = html.indexOf(SCRIPT_END, contentStart + 1);
        if (contentStart < 0 || contentEnd < 0) {
            return null;
        }
        return html.substring(contentStart + 1, contentEnd);
    }
}
