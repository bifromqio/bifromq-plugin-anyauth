package bifromq.plugin.auth.util;

import bifromq.plugin.auth.checker.Authenticator;
import bifromq.plugin.auth.checker.Authorizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.util.AttributeKey;

public class ChannelAttr {
    public static AttributeKey<Authenticator> AUTHENTICATOR = AttributeKey.valueOf("authenticator");
    public static AttributeKey<Authorizer> AUTHORIZER = AttributeKey.valueOf("authorizer");
    public static AttributeKey<ObjectMapper> OBJECT_MAPPER = AttributeKey.valueOf("object_mapper");
}
