package bifromq.plugin.auth.handler;

import bifromq.plugin.auth.checker.Authenticator;
import bifromq.plugin.auth.checker.Authorizer;
import bifromq.plugin.auth.model.UserAction;
import bifromq.plugin.auth.util.ChannelAttr;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
public class AuthHandler extends ChannelInboundHandlerAdapter {
    private Authenticator authenticator;
    private Authorizer authorizer;
    private ObjectMapper objectMapper;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        authenticator = ctx.channel().attr(ChannelAttr.AUTHENTICATOR).get();
        authorizer = ctx.channel().attr(ChannelAttr.AUTHORIZER).get();
        objectMapper = ctx.channel().attr(ChannelAttr.OBJECT_MAPPER).get();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;
            HttpMethod method = request.method();
            String uri = request.uri();

            Optional<JsonNode> jsonObject = jsonStringToJsonObject(request.content().toString(CharsetUtil.UTF_8));

            if (!jsonObject.isPresent()) {
                HttpResponse response = new DefaultHttpResponse(request.protocolVersion(),
                        HttpResponseStatus.BAD_REQUEST);
                response.headers().set("Content-Type", "application/json");
                flush(response, ctx);
            } else if (uri.equals("/auth") && method == HttpMethod.POST) {
                authenticator.auth(jsonObject.get().get("username").asText(), jsonObject.get().get("password").asText())
                        .thenAccept(value -> {
                            String jsonString;
                            if (value) {
                                jsonString = "{\"auth\":true}";
                            }else {
                                jsonString = "{\"auth\":false}";
                            }
                            ByteBuf content = Unpooled.copiedBuffer(jsonString, StandardCharsets.UTF_8);
                            FullHttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(),
                                    HttpResponseStatus.OK, content);
                            response.headers().set("Content-Type", "application/json");
                            flush(response, ctx);
                        });
            } else if (uri.equals("/check") && method == HttpMethod.POST) {
                authorizer.check(jsonObject.get().get("username").asText(),
                            jsonObject.get().get("topic").asText(),
                            UserAction.Action.valueOf(jsonObject.get().get("action").asText()))
                        .thenAccept(value -> {
                            String jsonString;
                            if (value) {
                                jsonString = "{\"check\":true}";
                            }else {
                                jsonString = "{\"check\":false}";
                            }
                            ByteBuf content = Unpooled.copiedBuffer(jsonString, StandardCharsets.UTF_8);
                            FullHttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(),
                                    HttpResponseStatus.OK, content);
                            response.headers().set("Content-Type", "application/json");
                            flush(response, ctx);
                        });
            }else {
                HttpResponse response = new DefaultHttpResponse(request.protocolVersion(),
                        HttpResponseStatus.NOT_FOUND);
                response.headers().set("Content-Type", "application/json");
                flush(response, ctx);
            }
            request.release();
        }
    }

    private void flush(HttpResponse response, ChannelHandlerContext ctx) {
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private Optional<JsonNode> jsonStringToJsonObject(String jsonStr) {
        try {
            return Optional.of(objectMapper.readTree(jsonStr));
        } catch (Exception e) {
            log.error("parse jsonStr failed: ", e);
            return Optional.empty();
        }
    }
}
