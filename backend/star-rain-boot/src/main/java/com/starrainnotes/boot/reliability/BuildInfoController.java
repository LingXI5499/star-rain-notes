package com.starrainnotes.boot.reliability;

import com.starrainnotes.common.result.ApiResponse;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public deployment identity; excludes host names, paths, credentials and environment values. */
@RestController
public class BuildInfoController {
    private final ObjectProvider<BuildProperties> builds;
    public BuildInfoController(ObjectProvider<BuildProperties> builds) { this.builds = builds; }
    @GetMapping("/api/public/site/build-info")
    public ApiResponse<Map<String,String>> info() {
        var build = builds.getIfAvailable();
        return ApiResponse.ok(Map.of("batch", "V2.2", "version", build == null ? "development" : build.getVersion(),
                "commit", build == null || build.get("commit") == null ? "unknown" : build.get("commit")));
    }
}
