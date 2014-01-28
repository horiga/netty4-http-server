package org.horiga.study.test.netty4.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpRequestDispatchHandler extends ChannelInboundHandlerAdapter {

	private static Logger log = LoggerFactory.getLogger(HttpRequestDispatchHandler.class);

	private volatile HttpRequest request;

	private volatile ByteBuf _body = UnpooledByteBufAllocator.DEFAULT.buffer();

	/* for debug */private StringBuilder sb = new StringBuilder();

	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		loglog(ctx, "[channelRegistered]");
		super.channelRegistered(ctx);
	}

	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		log.debug("[channelUnregistered]");
		super.channelUnregistered(ctx);
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		log.debug("[channelActive]");
		super.channelActive(ctx);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		log.debug("[channelInactive]");
		super.channelInactive(ctx);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

		try {
			if ((msg instanceof HttpMessage) && HttpHeaders.is100ContinueExpected((HttpMessage)msg))
				ctx.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));

			if (msg instanceof HttpRequest) {
				log.debug("[channelRead: HTTP-REQUEST]");
				this.request = (HttpRequest)msg;
				sb.append("\n>> HTTP REQUEST ----------------------------------\n");
				sb.append(this.request.getProtocolVersion().toString()).append(" ").append(
					this.request.getMethod().name()).append(" ").append(this.request.getUri());
				sb.append("\n");
				HttpHeaders headers = this.request.headers();
				if (!headers.isEmpty()) {
					for (Map.Entry<String, String> header : headers) {
						sb.append(header.getKey()).append(": ").append(header.getValue()).append("\n");
					}
				}
				sb.append("\n");

			} else if (msg instanceof HttpContent) {
				log.debug("[channelRead: HTTP-CONTENT]");

				HttpContent content = (HttpContent)msg;

				ByteBuf thisContent = content.content();
				if (thisContent.isReadable())
					_body.writeBytes(thisContent);

				if (msg instanceof LastHttpContent) {
					log.debug("> handle 'LastHttpContent'");

					sb.append(_body.toString(CharsetUtil.UTF_8));

					LastHttpContent trailer = (LastHttpContent)msg;
					if (!trailer.trailingHeaders().isEmpty()) {
						for (String name : trailer.trailingHeaders().names()) {
							sb.append(name).append("=");
							for (String value : trailer.trailingHeaders().getAll(name)) {
								sb.append(value).append(",");
							}
							sb.append("\n\n");
						}
					}
					sb.append("\n<< HTTP REQUEST ----------------------------------");
				}
			}
		} finally {
			ReferenceCountUtil.release(msg);
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		log.debug("[channelReadComplete]");
		log.debug(sb.toString());

		// This point is Business Logic started.
		writeJSON(ctx, HttpResponseStatus.OK, Unpooled.copiedBuffer("{}", CharsetUtil.UTF_8));
		log.debug("+ response finished.");
		ctx.flush();
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		log.debug("[userEventTriggered] evt={}", evt != null ? evt.toString() : "[null]");
		super.userEventTriggered(ctx, evt);
	}

	@Override
	public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
		log.debug("[channelWritabilityChanged]");
		super.channelWritabilityChanged(ctx);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		log.warn("[exceptionCaught]", cause);
	}

	private static void writeJSON(ChannelHandlerContext ctx, HttpResponseStatus status, ByteBuf content
	/*, boolean isKeepAlive*/) {
		if (ctx.channel().isWritable()) {
			FullHttpResponse msg = null;
			if (content != null) {
				msg = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content);
				msg.headers().set(HttpHeaders.Names.CONTENT_TYPE, "application/json; charset=utf-8");
			} else {
				msg = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
			}

			if (msg.content() != null)
				msg.headers().set(HttpHeaders.Names.CONTENT_LENGTH, msg.content().readableBytes());

			// not keep-alive
			ctx.write(msg).addListener(ChannelFutureListener.CLOSE);
		}
	}

	@SuppressWarnings("deprecation")
	private static void loglog(ChannelHandlerContext ctx, String message) {
		if (!log.isDebugEnabled())
			return;

		// debug
		StringBuilder sb = new StringBuilder(message);
		sb.append("\n").append("name=").append(ctx.name());
		sb.append(", addr=").append(ctx.channel().localAddress().toString());
		Map<ChannelOption<?>, Object> options = ctx.channel().config().getOptions();
		sb.append("\n[ch.opts]");
		for (Map.Entry<ChannelOption<?>, Object> option : options.entrySet()) {
			sb.append("  <").append(option.getKey().name()).append(":").append(option.getValue().toString()).append(
				">\n");
		}

		log.debug(sb.toString());
	}

}
