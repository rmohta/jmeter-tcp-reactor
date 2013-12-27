package test.tcp.client;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.Environment;
import reactor.function.Consumer;
import reactor.tcp.TcpClient;
import reactor.tcp.TcpConnection;
import reactor.tcp.encoding.StandardCodecs;
import reactor.tcp.netty.NettyTcpClient;
import reactor.tcp.spec.TcpClientSpec;

/**
 * 
 * @author Rohit Mohta
 *
 */
public class ReactorTcpClient {
	private final static Logger logger = LoggerFactory.getLogger(ReactorTcpClient.class);
	private Environment environment;
	private InetSocketAddress remoteAddress;
	private TcpClient<String , String> client;
	
	public ReactorTcpClient(Environment env,InetSocketAddress remoteAddress) {
		super();
		this.environment = env;
		this.remoteAddress = remoteAddress;
		this.client = new TcpClientSpec<String,String>(NettyTcpClient.class)
				.env(this.environment)
				.codec(StandardCodecs.LINE_FEED_CODEC)
				.connect(this.remoteAddress)
				.get();
	}
	
	public void start() {
		this.client.open().consume(new Consumer<TcpConnection<String,String>>() {
			public void accept(final TcpConnection<String, String> connection) {
				
				connection.in().consume(new Consumer<String>() {
					public void accept(String line) {
						logger.info("Received message: "+line);
					} /* end: accept(String) **/
				}); 
				/*
				 * Smart Heartbeat Functionality
				 */
				connection.on().readIdle(1000, //idle in ms
						new Runnable() {
							public void run() {
								connection.send("readIdle");
							}
						})
						.writeIdle(1000, //idle in ms
								new Runnable() {
									public void run() {
										connection.send("writeIdle");
									}
								});
				//Write data to server
				for(int i=0; i < 100; i++) {
					connection.sendAndForget("Echo Message");
				}
			} /* end: accept(TcpConnection) **/
		});
	}
	
	public static void main(String[] args) {
		logger.info("Launching..");
		Environment environment = new Environment(); //one per server
		InetSocketAddress remote = new InetSocketAddress("127.0.0.1", 8000);
		ReactorTcpClient client = new ReactorTcpClient(environment, remote);
		client.start();
	}
	
}
