package bifromq.auth.plugin.config;

public class AuthProviderConfig {
    private String tenantId;
    private Auth0Config auth0;
    private WechatConfig wechat;
    private DeviceConfig device;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Auth0Config getAuth0() {
        return auth0;
    }

    public void setAuth0(Auth0Config auth0) {
        this.auth0 = auth0;
    }

    public WechatConfig getWechat() {
        return wechat;
    }

    public void setWechat(WechatConfig wechat) {
        this.wechat = wechat;
    }

    public DeviceConfig getDevice() {
        return device;
    }

    public void setDevice(DeviceConfig device) {
        this.device = device;
    }
}
