package bifromq.plugin.config;

import lombok.Data;

@Data
public class AuthProviderConfig {
    private String tenantId;
    private Auth0Config auth0;
    private WechatConfig wechat;
    private DeviceConfig device;
}
