package bifromq.plugin.auth.config;

import lombok.Data;

@Data
public class WechatConfig {
    private String appId;
    private String appSecret;
    private String requestUrl;
}
