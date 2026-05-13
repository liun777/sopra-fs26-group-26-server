package ch.uzh.ifi.hase.soprafs26.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
public class BuildInfoController {

    private static final String UNKNOWN_COMMIT = "unknown";
    private static final String UNKNOWN_DATE = "--------";
    private static final String UNKNOWN_TIME = "--:--";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("ddMMyyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @GetMapping("/build-info")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Map<String, String> getBuildInfo() {
        String commitId = firstNonBlank(
            System.getenv("CABO_SERVER_BUILD_COMMIT_ID"),
            System.getenv("CABO_SERVER_GIT_COMMIT_SHA"),
            System.getenv("GITHUB_SHA"),
            System.getenv("CI_COMMIT_SHA")
        );
        if (commitId == null) {
            return unknownBuildInfo();
        }

        String timestamp = firstNonBlank(
            System.getenv("CABO_SERVER_BUILD_COMMIT_TIMESTAMP"),
            System.getenv("CABO_SERVER_GIT_COMMIT_TIMESTAMP"),
            System.getenv("CI_COMMIT_TIMESTAMP")
        );
        LocalDateTime parsedTimestamp = parseTimestamp(timestamp);

        if (parsedTimestamp == null) {
            return Map.of(
                "commitId", commitId,
                "date", UNKNOWN_DATE,
                "time", UNKNOWN_TIME
            );
        }

        return Map.of(
            "commitId", commitId,
            "date", DATE_FORMATTER.format(parsedTimestamp),
            "time", TIME_FORMATTER.format(parsedTimestamp)
        );
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null) {
                String trimmed = value.trim();
                if (!trimmed.isEmpty()) {
                    return trimmed;
                }
            }
        }
        return null;
    }

    private static LocalDateTime parseTimestamp(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (RuntimeException ignored) {
            // try next parser
        }

        try {
            return Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDateTime();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static Map<String, String> unknownBuildInfo() {
        return Map.of(
            "commitId", UNKNOWN_COMMIT,
            "date", UNKNOWN_DATE,
            "time", UNKNOWN_TIME
        );
    }
}

