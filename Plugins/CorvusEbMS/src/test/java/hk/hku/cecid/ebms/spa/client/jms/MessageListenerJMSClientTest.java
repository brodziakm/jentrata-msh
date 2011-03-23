package hk.hku.cecid.ebms.spa.client.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import hk.hku.cecid.ebms.pkg.EbxmlMessage;
import hk.hku.cecid.ebms.pkg.MessageHeader;
import hk.hku.cecid.ebms.pkg.MessageHeader.PartyId;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.xml.soap.AttachmentPart;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 * @author mhanda
 */
public class MessageListenerJMSClientTest {

	private MessageListenerJMSClient instance;
	private ConnectionFactory cf;
	private Session session;
	private Connection connection;
	private Queue queue;
	private MessageConsumer consumer;

	public MessageListenerJMSClientTest() {
	}

	@BeforeClass
	public static void setUpClass() throws Exception {
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}

	@Before
	public void setUp() throws JMSException {
		
		instance = spy(new MessageListenerJMSClient());
		
		
		doReturn("vm://mdh?broker.persistent=false").when(instance).getConnectionUrl();
		doReturn("testQueue").when(instance).getQueueName();
		instance.init();

		cf = new ActiveMQConnectionFactory("vm://mdh?broker.persistent=false");
		connection = cf.createConnection();
		connection.start();
		session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		queue = session.createQueue("testQueue");
	}

	@After
	public void tearDown() throws JMSException {
		consumer.close();
		session.close();
		connection.close();
	}

	/**
	 * Test of messageReceived method, of class JMSClient.
	 * 
	 * @throws SOAPException
	 */
	@Test
	public void testMessageReceived() throws SOAPException, JMSException {
		// instance receive message
		// now check the message sent to the queue by the instance

		EbxmlMessage requestMessage = spy(new EbxmlMessage());
		
		String messageBody = "This is a test message";
		
		SOAPMessage inMsg = mock(SOAPMessage.class);
		AttachmentPart inPart = mock(AttachmentPart.class);
		
		doReturn(messageBody.getBytes()).when(inPart).getRawContentBytes();
		List<AttachmentPart> attachments = new ArrayList<AttachmentPart>();
		attachments.add(inPart);
		doReturn(attachments.iterator()).when(inMsg).getAttachments();
		inMsg.addAttachmentPart(inPart);
		
		doReturn(inMsg).when(requestMessage).getSOAPMessage();
		
		//mock the headers here
		doReturn("cpaid2").when(requestMessage).getCpaId();
		doReturn("http://10.1.1.234:8080/corvus/httpd/ebms/inbound").when(requestMessage).getService();
		doReturn("B_Send_to_A").when(requestMessage).getAction();
		doReturn("convid").when(requestMessage).getConversationId();
		doReturn("serviceType").when(requestMessage).getServiceType();
		
		instance.messageReceived(requestMessage);

		consumer = session.createConsumer(queue);
		Message message = consumer.receive(100);
		assertNotNull(message);
		assertTrue(message instanceof BytesMessage);

		BytesMessage byteMessage = (BytesMessage) message;
		int msgLength = messageBody.getBytes().length;
		
		byte[] m = new byte[msgLength];
		byteMessage.readBytes(m);
		assertEquals(messageBody.getBytes().length, byteMessage.getBodyLength());
		assertEquals(messageBody, new String(m));
		assertEquals("cpaid2", byteMessage.getStringProperty(MessageListenerJMSClient.MSG_PROPERTY_CPA_ID));
		assertEquals("http://10.1.1.234:8080/corvus/httpd/ebms/inbound", byteMessage.getStringProperty(MessageListenerJMSClient.MSG_PROPERTY_SERVICE));
		assertEquals("B_Send_to_A", byteMessage.getStringProperty(MessageListenerJMSClient.MSG_PROPERTY_ACTION));
		assertEquals("convid", byteMessage.getStringProperty(MessageListenerJMSClient.MSG_PROPERTY_CONV_ID));
		assertEquals("serviceType", byteMessage.getStringProperty(MessageListenerJMSClient.MSG_PROPERTY_SERVICE_TYPE));
	}
}
