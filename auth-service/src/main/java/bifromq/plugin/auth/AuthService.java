package bifromq.plugin.auth;

import bifromq.plugin.auth.auth.Authenticator;
import bifromq.plugin.auth.auth.Authorizer;
import bifromq.plugin.auth.config.AuthConfig;
import bifromq.plugin.auth.handler.AuthHandler;
import bifromq.plugin.auth.storage.IAuthStorage;
import bifromq.plugin.auth.storage.MySqlStorage;
import bifromq.plugin.auth.util.ChannelAttr;
import bifromq.plugin.auth.util.NettyUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.File;
import java.io.IOException;

@Slf4j
public class AuthService {
    static {
        Thread.setDefaultUncaughtExceptionHandler(
                (t, e) -> log.error("Caught an exception in thread[{}]", t.getName(), e));
    }
    private ChannelFuture tcpChannelF;
    private IAuthStorage storage;
    private Authenticator authenticator;
    private Authorizer authorizer;
    private ObjectMapper objectMapper = new ObjectMapper();
    public static void main(String[] args) {
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        try {
            CommandLine cmd = parser.parse(cliOption(), args);
            File configFile = new File(cmd.getOptionValue("c"));
            if (!configFile.exists()) {
                throw new RuntimeException("Conf file does not exist: " + cmd.getOptionValue("c"));
            }
            AuthService authService = new AuthService(configFile);
            authService.start();
        }catch (Throwable e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            formatter.printHelp("CMD", cliOption());
        }
    }

    private static Options cliOption() {
        return new Options()
                .addOption(Option.builder()
                        .option("c")
                        .longOpt("conf")
                        .desc("the conf file for Starter")
                        .hasArg(true)
                        .optionalArg(false)
                        .argName("CONF_FILE")
                        .required(true)
                        .build());
    }

    AuthService(File configFile) {
        AuthConfig config = getConfig(configFile);
        if (config.getType() == IAuthStorage.Type.Dummy) {
            storage = IAuthStorage.Dummy_Storage;
        }else {
            storage = new MySqlStorage(config.getMySqlStorageConfig());
        }
        authenticator = new Authenticator(storage);
        authorizer = new Authorizer(storage);
        tcpChannelF = buildChannel(config);
        Thread shutdownThread = new Thread(this::shutdown);
        shutdownThread.setName("shutdown");
        Runtime.getRuntime().addShutdownHook(shutdownThread);
    }

    private AuthConfig getConfig(File configFile) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            return mapper.readValue(configFile, AuthConfig.class);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read starter config file: " + configFile, e);
        }
    }

    private ChannelFuture buildChannel(AuthConfig config) {
        EventLoopGroup bossGroup = NettyUtil.createEventLoopGroup(config.getBossThreads(), "auth-boss");
        EventLoopGroup workerGroup = NettyUtil.createEventLoopGroup(config.getWorkerThreads(), "auth-worker");
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NettyUtil.determineSeverSocketChannelClass(bossGroup))
                .childHandler(new ChannelInitializer() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("codec", new HttpServerCodec());
                        pipeline.addLast("aggregator", new HttpObjectAggregator(1048576));
                        pipeline.addLast("auth-handler", new AuthHandler());
                    }
                })
                .childAttr(ChannelAttr.AUTHENTICATOR, authenticator)
                .childAttr(ChannelAttr.AUTHORIZER, authorizer)
                .childAttr(ChannelAttr.OBJECT_MAPPER, objectMapper);
        return b.bind(config.getHost(), config.getPort());
    }

    public void start() {
        try {
            log.info("starting auth service");
            if (tcpChannelF != null) {
                tcpChannelF.sync().channel();
            }
            log.info("auth service started");
        }catch (InterruptedException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public void shutdown() {
        log.info("shutting down the auth service");
        if (tcpChannelF != null) {
            tcpChannelF.channel().close().syncUninterruptibly();
        }
        log.info("auth service shutdown");
    }
}
