package com.sam.netty_tcp.server;

import com.sam.netty_tcp.entity.Message;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;
import java.util.Map;
import java.util.HashMap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * {@link MessageHandler} is the TCP Message Handler and reply the client after message parsing.
 * @author Sameer Narkhede See <a href="https://narkhedesam.com">https://narkhedesam.com</a>
 * @since Sept 2020
 * 
 */
public class MessageHandler extends SimpleChannelInboundHandler<Message> {

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		super.exceptionCaught(ctx, cause);
		cause.printStackTrace();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {

		handleMessage(ctx, msg);

	}

	/**
	 * Actual Message handling and reply to server.
	 * 
	 * @param ctx  {@link ChannelHandlerContext}
	 * @param msg  {@link Message}
	 */
	private void handleMessage(ChannelHandlerContext ctx, Message msg) {
            // Extract headers + body from message payload
            String raw = msg.getMessage();
            String[] lines = raw.split("\n");
            Map<String, String> contextMap = new HashMap<>();
            StringBuilder body = new StringBuilder();
            boolean readingHeaders = true;

            for (String line : lines) {
                if (line.trim().isEmpty()) {
                    readingHeaders = false;
                    continue;
                }
                if (readingHeaders && line.contains(":")) {
                    String[] parts = line.split(":", 2);
                    contextMap.put(parts[0].trim(), parts[1].trim());
                } else {
                    body.append(line).append("\n");
                }
            }

	    Tracer tracer = GlobalTracer.get();
	    io.opentracing.propagation.TextMap extractAdapter = new TextMapExtractAdapter(contextMap);
            Span span = tracer.buildSpan("tcp.message.received")
                .asChildOf(tracer.extract(Format.Builtin.TEXT_MAP, (io.opentracing.propagation.TextMap) new TextMapExtractAdapter(contextMap)))
                .withTag("message.body", body.toString().trim())
                .start();

            try {
		System.out.println("Message Received : " + body.toString().trim());

		ByteBuf buf = Unpooled.wrappedBuffer("Hey Sameer Here!!!!".getBytes());

		// Send reply
		final WriteListener listener = new WriteListener() {
			@Override
			public void messageRespond(boolean success) {
				System.out.println(success ? "reply success" : "reply fail");
			}
		};

		ctx.writeAndFlush(buf).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (listener != null) {
					listener.messageRespond(future.isSuccess());
				}
			}
		});
	    } finally {
                span.finish(); 
            }
	}

	/**
	 * {@link WriteListener} is the lister message status interface.
	 * @author Sameer Narkhede See <a href="https://narkhedesam.com">https://narkhedesam.com</a>
	 * @since Sept 2020
	 * 
	 */
	public interface WriteListener {
		void messageRespond(boolean success);
	}

}
