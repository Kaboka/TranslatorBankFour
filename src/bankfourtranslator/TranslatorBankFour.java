/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bankfourtranslator;

import gateway.BankGateway;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import dk.cphbusiness.connection.ConnectionCreator;
import gateway.IBankGateway;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import utilities.xml.xmlMapper;
import webservice.LoanRequest;

/**
 *
 * @author Kasper
 */
public class TranslatorBankFour {

    private static final String EXCHANGE_NAME = "ex_translators_gr1";
    private static final String IN_QUEUE = "bank_translator_four_gr1";
    private static final String[] TOPICS = {"expensive.high", "cheap.low" };

    public static void main(String[] args) throws IOException, InterruptedException {
        IBankGateway gateway = new BankGateway();
        ConnectionCreator creator = ConnectionCreator.getInstance();
        Channel channelIn = creator.createChannel();
        channelIn.queueDeclare(IN_QUEUE, true, false, false, null);
        channelIn.exchangeDeclare(EXCHANGE_NAME, "topic");
       
        
        for (String topic : TOPICS) {
            channelIn.queueBind(IN_QUEUE, EXCHANGE_NAME, topic);
        }

        QueueingConsumer consumer = new QueueingConsumer(channelIn);
        channelIn.basicConsume(IN_QUEUE, true, consumer);
        
        
        System.out.println("Translator for BankFour is running");
        while (true) {
            Delivery delivery = consumer.nextDelivery();
    //        channelIn.basicAck(delivery.getEnvelope().getDeliveryTag(), true);
            System.out.println("Got message: " + new String(delivery.getBody()));
            LoanRequest request = translateMessage(delivery);
            gateway.contactBank(request, delivery.getProperties().getCorrelationId());
        }
    }

    private static LoanRequest translateMessage(Delivery delivery) {
        LoanRequest request = new LoanRequest();
        try {
            String message = new String(delivery.getBody());
            Document doc = xmlMapper.getXMLDocument(message);
            XPath xPath = XPathFactory.newInstance().newXPath();
            int loanDuration = Integer.parseInt(xPath.compile("/LoanRequest/loanDuration").evaluate(doc));
            double loanAmount = Double.parseDouble(xPath.compile("/LoanRequest/loanAmount").evaluate(doc));
            int creditScore = Integer.parseInt(xPath.compile("/LoanRequest/creditScore").evaluate(doc));
            String ssn = xPath.compile("/LoanRequest/ssn").evaluate(doc);
            ssn = ssn.replace("-", "");
            request.setCreditScore(creditScore);
            request.setLoanDuration(loanDuration);
            request.setLoanAmount(loanAmount);
            request.setSsn(ssn);
        } catch (XPathExpressionException ex) {
            Logger.getLogger(TranslatorBankFour.class.getName()).log(Level.SEVERE, null, ex);
        }

        return request;
    }

}
