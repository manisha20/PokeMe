package salazar.pokeme;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Salazar on 17-04-2016.
 */

    public abstract class IConnectToRabbitMQ {
        public String mServer;
        public String mExchange;

        protected Channel mModel = null;
        protected Connection  mConnection;


        protected boolean Running ;

        protected  String MyExchangeType ;

        /**
         *
         * @param server The server address
         * @param exchange The named exchange
         * @param exchangeType The exchange type name
         */
        public IConnectToRabbitMQ(String server, String exchange, String exchangeType)
        {
            mServer = server;
            mExchange = exchange;
            MyExchangeType = exchangeType;
        }

    public void Dispose()
    {
        Running = false;

        try {
            if (mConnection!=null)
                mConnection.close();
            if (mModel != null)
                mModel.abort();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
        /**
         * Connect to the broker and create the exchange
         * @return success
         */
        public boolean connectToRabbitMQ()
        {
            if(mModel!= null && mModel.isOpen() )//already declared
                return true;
            try
            {
                ConnectionFactory connectionFactory = new ConnectionFactory();
                String uri = "amqp://qoistzqu:mrZ8p4eW-BdW3UG1eOsdx-tEGOVwwmxQ@fox.rmq.cloudamqp.com/qoistzqu";
                try {
                    connectionFactory.setAutomaticRecoveryEnabled(false);
                    connectionFactory.setUri(uri);
                } catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException e1) {
                    e1.printStackTrace();
                }
                //connectionFactory.setHost(mServer);
                mConnection = connectionFactory.newConnection();
                mModel = mConnection.createChannel();
                mModel.exchangeDeclare(mExchange, MyExchangeType, true);

                return true;
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return false;
            }
        }
    }

