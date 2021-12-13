package io.fabric8.quickstarts.camel.amq;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.CxfEndpoint;
import org.apache.camel.component.cxf.CxfEndpointConfigurer;
import org.apache.camel.component.cxf.DataFormat;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.AbstractWSDLBasedEndpointFactory;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
class CamelRoute extends RouteBuilder {

  @Override
  public void configure() throws Exception {
      from("timer:test?period=4000")
              .log("Now calling the cxf producer ")
              .process(exchange -> {
                  String body = "<amq:test xmlns:amq=\"http://amq.camel.quickstarts.fabric8.io/\">\n" +
                          "         <amq:input>asdfasdfwefef</amq:input>\n" +
                          "      </amq:test>";
                  exchange.getIn().setBody(body);
              }).to("cxfProducerEndpoint")
                      .log("Done calling the cxf producer");

    from("cxf:bean:proxyBackend")
            .log("In cxf consumer")
            .process(exchange -> {
              byte[] bytes = new byte[44000];
              Arrays.fill(bytes,(byte)'R');
              String val = new String(bytes);
              String resp = "<amq:testResponse xmlns:amq=\"http://amq.camel.quickstarts.fabric8.io/\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                      "       <amq:input>PLACEHOLDER</amq:input>\n" +
                      "      </amq:testResponse>";
              resp = resp.replace("PLACEHOLDER",val);
             //  System.out.println(resp);  // The response is properly sent from here
              exchange.getIn().setBody(resp);
            })
            .log(">>>>> Out of cxf consumer >>>")
            ;
  }

  @Bean(name = "proxyBackend")
  public CxfEndpoint createProxyBK() {
    CxfEndpoint serverEndpoint = new CxfEndpoint();
    serverEndpoint.setAddress("http://localhost:9082/backend");
    serverEndpoint.setServiceClass(CXFService.class);
    serverEndpoint.setDataFormat(DataFormat.PAYLOAD);
    serverEndpoint.setAllowStreaming(false);
    serverEndpoint.setContinuationTimeout(10_000L);
    serverEndpoint.setLoggingFeatureEnabled(false);

    return serverEndpoint;
  }

  @Bean(name = "cxfProducerEndpoint")
  private CxfEndpoint createProducerEndpoint() {
    CxfEndpoint clientEndpoint = new CxfEndpoint();
    clientEndpoint.setDataFormat(DataFormat.PAYLOAD);
    clientEndpoint.setAddress("http://localhost:9082/backend");
    clientEndpoint.setServiceClass(CXFService.class);
    clientEndpoint.setAllowStreaming(false);
    clientEndpoint.setLoggingFeatureEnabled(false);
    setHttpConfiguration(clientEndpoint);
    return clientEndpoint;
  }


  private void setHttpConfiguration(CxfEndpoint cxfEndpoint) {
    CxfEndpointConfigurer cxfEndpointConfigurer = new CxfEndpointConfigurer() {
      public void configure(AbstractWSDLBasedEndpointFactory abstractWSDLBasedEndpointFactory) {
      }

      public void configureClient(Client client) {
        client.getRequestContext().put("use.async.http.conduit",false); /// Doesn't make a difference
        client.getResponseContext().put("use.async.http.conduit",false); /// Doesn't make a difference

       HTTPConduit conduit = (HTTPConduit) client.getConduit();

        HTTPClientPolicy policy = Optional.ofNullable(conduit.getClient()).orElse(new HTTPClientPolicy());

        /* SETTING THIS VALUE LARGER THAN 1 TRIGGERS THE PROBLEMS */
        policy.setMaxRetransmits(3);
        //policy.setChunkLength(1024000); // Setting this gets around the problem
        System.out.println("+++++++++++++++++++++++++++++++++++++ chunk "+policy.isAllowChunking());
        conduit.setClient(policy);
      }

      public void configureServer(Server server) {
      }
    };
    cxfEndpoint.setCxfEndpointConfigurer(cxfEndpointConfigurer);
  }
}
