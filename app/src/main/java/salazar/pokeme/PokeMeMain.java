package salazar.pokeme;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;


public class PokeMeMain extends ActionBarActivity {
    ConnectionFactory factory = new ConnectionFactory();
    private BlockingDeque publishQueue = new LinkedBlockingDeque();

    Thread publishThread;
    Thread subscribeThread;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poke_me_main);
        try {
            publishQueue.putLast("check1");
            publishQueue.putLast("check2");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //connection object
        setupConnectionFactory();
        publishToAMQP();
        setupPubButton();
        final Handler incomingMessageHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String message = msg.getData().getString("msg");
                TextView tv = (TextView) findViewById(R.id.textView);
                Date now = new Date();
                SimpleDateFormat ft = new SimpleDateFormat("hh:mm:ss");
                tv.append(ft.format(now) + ' ' + message + '\n');
            }
        };
        subscribe(incomingMessageHandler);
    }
    private void setupConnectionFactory() {
        String uri = "amqp://qoistzqu:mrZ8p4eW-BdW3UG1eOsdx-tEGOVwwmxQ@fox.rmq.cloudamqp.com/qoistzqu";
        try {
            factory.setAutomaticRecoveryEnabled(false);
            factory.setUri(uri);
        } catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException e1) {
            e1.printStackTrace();
        }
    }

    void publishMessage(String message) {
        try {
            Log.d("MAIN", "[q] " + message);
            publishQueue.putLast(message);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void setupPubButton() {
        Button button = (Button) findViewById(R.id.publish);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                EditText et = (EditText) findViewById(R.id.text);
                publishMessage(et.getText().toString());
                et.setText("");
            }
        });

    }


    public void publishToAMQP()
    {
        publishThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Connection connection = factory.newConnection();
                        Channel ch = connection.createChannel();
                        ch.confirmSelect();

                        while (true) {
                            String message = publishQueue.takeFirst().toString();
                            try {
                                ch.basicPublish("amq.direct", "routingKeyFromNexus", new AMQP.BasicProperties.Builder()
                                        .userId("qoistzqu")
                                        .build(), message.getBytes());
                                Log.d("Publisher", "[s] " + message);
                                ch.waitForConfirmsOrDie();
                            } catch (ShutdownSignalException sse) {
                                Log.d("SSE Error", "reason" + sse.getReason());
                                Log.d("SSE Error", "cause" + sse.getCause());
                                Log.d("SSE Error", "getReference" + sse.getReference());

                            } catch (IOException ioe) {
                                Log.d("Ioe Error", "cause" + ioe.getCause());
                            }
                            catch (Exception e) {
                                Log.d("", "[f] " + message);
                                publishQueue.putFirst(message);
                                throw e;
                            }
                        }
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e) {
                        Log.d("", "Connection broken: " + e.getClass().getName());
                        try {
                            Thread.sleep(5000); //sleep and then try again
                        } catch (InterruptedException e1) {
                            break;
                        }
                    }
                }
            }
        });
        publishThread.start();
    }
    void subscribe(final Handler handler)
    {
        subscribeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Connection connection = factory.newConnection();
                        Channel channel = connection.createChannel();
                        channel.basicQos(1);
                        AMQP.Queue.DeclareOk q = channel.queueDeclare();
                        channel.queueBind(q.getQueue(), "amq.direct", "routingKeyFromMoto");
                        QueueingConsumer consumer = new QueueingConsumer(channel);
                        channel.basicConsume(q.getQueue(), true, consumer);

                        while (true) {
                            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                            String message = new String(delivery.getBody());
                            String userID=new String(delivery.getProperties().getUserId());
                            Log.d("ConsumerCheck", "[r] " + message+" "+userID);
                            Message msg = handler.obtainMessage();
                            Bundle bundle = new Bundle();
                            bundle.putString("msg", message);
                            msg.setData(bundle);
                            handler.sendMessage(msg);
                        }
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e1) {
                        Log.d("", "Connection broken: " + e1.getClass().getName());
                        try {
                            Thread.sleep(5000); //sleep and then try again
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
            }
        });
        subscribeThread.start();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_poke_me_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
