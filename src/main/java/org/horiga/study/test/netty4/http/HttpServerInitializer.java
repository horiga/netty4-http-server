package org.horiga.study.test.netty4.http;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

public class HttpServerInitializer extends ChannelInitializer<SocketChannel> {

	@Override
	protected void initChannel(SocketChannel socketChannel) throws Exception {
		ChannelPipeline p = socketChannel.pipeline();
		p.addLast("http.decoder", new HttpRequestDecoder());
		//p.addLast("");
		p.addLast("http.encoder", new HttpResponseEncoder());
		//p.addLast("http.deflater", new HttpResponseEncoder());
		p.addLast("http.request-handler", new HttpRequestDispatchHandler());
	}

}
