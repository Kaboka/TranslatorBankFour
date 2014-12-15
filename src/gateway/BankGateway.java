/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gateway;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import dk.cphbusiness.connection.ConnectionCreator;
import gateway.IBankGateway;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import utilities.xml.xmlMapper;
import webservice.BankWebService_Service;
import webservice.LoanRequest;
import webservice.LoanResponse;

/**
 *
 * @author Kasper
 */
public class BankGateway implements IBankGateway {

    private Channel channelOut;
    private static final String OUT_QUEUE = "bank_four_normalizer";
    private static final String EXCHANGE = "normalizer_exchange";
    
    public BankGateway() {
        ConnectionCreator creator = ConnectionCreator.getInstance();
        try{
        channelOut = creator.createChannel();
        channelOut.queueDeclare(OUT_QUEUE, false, false, false, null); //bind til en exchange
        channelOut.exchangeDeclare(EXCHANGE, "fanout");
        } catch (IOException ex) {
            Logger.getLogger(BankGateway.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void contactBank(LoanRequest request) {
        String xmlMessage = "";
        BankWebService_Service service = new BankWebService_Service();
        LoanResponse response = service.getBankWebServicePort().getIntrestRate(request);
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            
            Element loanResponse = doc.createElement("LoanResponse");
            doc.appendChild(loanResponse);
            Element ssn = doc.createElement("ssn");
            ssn.appendChild(doc.createTextNode(request.getSsn()));
            Element bankName = doc.createElement("bankName");
            bankName.appendChild(doc.createTextNode(response.getBankName()));
            Element interestRate = doc.createElement("intrestRate");
            interestRate.appendChild(doc.createTextNode(Double.toString(response.getInterrestRate())));
            loanResponse.appendChild(ssn);
            loanResponse.appendChild(bankName);
            loanResponse.appendChild(interestRate);
            xmlMessage = xmlMapper.getStringFromDoc(doc);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(BankGateway.class.getName()).log(Level.SEVERE, null, ex);
        }
        BasicProperties probs = new BasicProperties.Builder().correlationId("1").build(); //need real correlationID
        try {
            System.out.println("Publishing message: " + xmlMessage);
            channelOut.basicPublish(EXCHANGE, OUT_QUEUE, probs, xmlMessage.getBytes());
        } catch (IOException ex) {
            Logger.getLogger(BankGateway.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    
}
