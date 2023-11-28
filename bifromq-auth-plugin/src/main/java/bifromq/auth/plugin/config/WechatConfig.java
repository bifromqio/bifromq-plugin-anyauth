package bifromq.auth.plugin.config;

import lombok.Data;

@Data
public class WechatConfig {
    private String appId;
    private String appSecret;
    private String requestUrl;
}
