package bifromq.auth.service.util;

import bifromq.auth.service.checker.Authenticator;
import bifromq.auth.service.checker.Authorizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.util.AttributeKey;

public class ChannelAttr {
    public static AttributeKey<Authenticator> AUTHENTICATOR = AttributeKey.valueOf("authenticator");
    public static AttributeKey<Authorizer> AUTHORIZER = AttributeKey.valueOf("authorizer");
    public static AttributeKey<ObjectMapper> OBJECT_MAPPER = AttributeKey.valueOf("object_mapper");
}
