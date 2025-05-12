package com.sam.netty_tcp;

import com.sam.netty_tcp.client.Client;
import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapInjectAdapter;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import java.util.HashMap;
import java.util.Map;

/**
 * Client Main is the {@link Client} handler.
 * @author Sameer Narkhede See <a href="https://narkhedesam.com">https://narkhedesam.com</a>
 * @since Sept 2020
 * 
 */
public class ClientMain {
	
	
	public static void main(String[] args) {
		
		try {
	        // Create a client
			System.out.println("Creating new Client");
			
			Client client = new Client(11111);
	        ChannelFuture channelFuture = client.startup();
	        
	        System.out.println("New Client is created");
	        
	        // wait for 5 seconds
	        Thread.sleep(5000);
	        // check the connection is successful 
	        if (channelFuture.isSuccess()) {
	        	// send message to server
		    Span span = GlobalTracer.get()
                        .buildSpan("tcp.message.send")
                        .withTag("message.sent", "Hello")
                        .start();

		    try {
	                // Inject span context into map
                    	Map<String, String> contextMap = new HashMap<>();
			io.opentracing.propagation.TextMap injectAdapter = new TextMapInjectAdapter(contextMap);
			GlobalTracer.get().inject(span.context(), Format.Builtin.TEXT_MAP, (io.opentracing.propagation.TextMap) new TextMapInjectAdapter(contextMap));

                    	// Construct payload: headers + blank line + body
                    	StringBuilder sb = new StringBuilder();
                    	for (Map.Entry<String, String> entry : contextMap.entrySet()) {
                            sb.append(entry.getKey()).append(":").append(entry.getValue()).append("\n");
                    	}
                    	sb.append("\n"); // Delimiter between headers and body
                    	sb.append("Hello"); // Message body

                    	// Send message with embedded trace context
			channelFuture.channel().writeAndFlush(Unpooled.wrappedBuffer("Hello".getBytes()))
			    .addListener(new ChannelFutureListener() {
	    			@Override
	    			public void operationComplete(ChannelFuture future) throws Exception {
	    				System.out.println(future.isSuccess()? "Message sent to server : Hello" : "Message sending failed");
	    			}
	    		    });
		    } finally {
                        span.finish(); 
                    }
	        }
	        // timeout before closing client
	        Thread.sleep(5000);
	        // close the client
	        client.shutdown();
	    }
	    catch(Exception e){
	        e.printStackTrace();
	    	System.out.println("Try Starting Server First !!");
	    	
	    }
	}
	
}
