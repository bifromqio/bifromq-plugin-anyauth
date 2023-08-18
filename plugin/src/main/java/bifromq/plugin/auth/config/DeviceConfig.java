package bifromq.plugin.auth.config;

import lombok.Data;

@Data
public class DeviceConfig {
    private String authUrl;
    private String checkUrl;
}
