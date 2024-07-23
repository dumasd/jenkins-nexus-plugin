package io.jenkins.plugins.nexus;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class URLTest {
    @Test
    public void testEncode() {
        String s = URLEncoder.encode("\"/com/example/test-bb/\"", StandardCharsets.UTF_8);
        log.info(s);
    }

    @Test
    public void testUri() throws Exception {
        String s =
                "https://nexus.agileforge.tech/service/rest/v1/search?repository=raw-pp&q=%22%2Fcom%2Fpeliplat%2Fpeliplat-account-api%2F%22";

        URI uri = URI.create(s);
        log.info(uri.toURL().toString());
    }
}
