package org.horiga.study.test.netty4.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.internal.SystemPropertyUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServer {

	private static Logger log = LoggerFactory.getLogger(HttpServer.class);

	private final int port;
	private EventLoopGroup boss = new NioEventLoopGroup();
	private EventLoopGroup worker = new NioEventLoopGroup(SystemPropertyUtil.getInt("events.workerThreads", 300),
		new DefaultThreadFactory("nio-worker", Thread.MAX_PRIORITY));

	public HttpServer() {
		port = SystemPropertyUtil.getInt("default.port", 18080);
	}

	public HttpServer(int port) {
		this.port = port;
	}

	public void init() {
	}

	public void run() throws Exception {
		try {
			ServerBootstrap bootstrap = new ServerBootstrap();
			bootstrap.group(boss, worker).channel(NioServerSocketChannel.class).childHandler(
				new HttpServerInitializer());
			setChannelOptions(bootstrap);

			Channel ch = bootstrap.bind(port).sync().channel();

			log.info(">> startUp server [{}]", ch.localAddress().toString());

			ch.closeFuture().sync(); // blocked

		} finally {
			shutdown();
		}
	}

	protected void shutdown() {
		boss.shutdownGracefully();
		worker.shutdownGracefully();
	}

	protected void setChannelOptions(ServerBootstrap bootstrap) {
		bootstrap.childOption(ChannelOption.MAX_MESSAGES_PER_READ, 36);
	}

	public static void main(String[] args) {
		try {
			new HttpServer(SystemPropertyUtil.getInt("server.port", 18080)).run();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
