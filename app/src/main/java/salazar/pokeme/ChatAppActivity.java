package salazar.pokeme;

import android.app.Activity;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.UnsupportedEncodingException;


public class ChatAppActivity extends Activity {
    private MessageConsumer mConsumer;
    private TextView mOutput;
    private String QUEUE_NAME="bye";
    private String EXCHANGE_NAME="logs";
    private String message = "";
    private String name = "";
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_app);
        Toast.makeText(ChatAppActivity.this, "RabbitMQ Chat Service!", Toast.LENGTH_LONG).show();

        final EditText etv1 = (EditText) findViewById(R.id.out3);
        etv1.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    // Perform action on key press
                    name = etv1.getText().toString();
                    etv1.setText("");
                    etv1.setVisibility(View.GONE);
                    return true;
                }
                return false;
            }
        });

        final EditText etv = (EditText) findViewById(R.id.out2);
        etv.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    // Perform action on key press
                    message = name + ": " + etv.getText().toString();
                    new send().execute(message);
                    etv.setText("");
                    return true;
                }
                return false;
            }
        });

//The output TextView we'll use to display messages
        mOutput = (TextView) findViewById(R.id.output);

//Create the consumer
        mConsumer = new MessageConsumer("192.168.162.37",
                "logs",
                "fanout");

//Connect to broker
        mConsumer.connectToRabbitMQ();

//register for messages
        mConsumer.setOnReceiveMessageHandler(new MessageConsumer.OnReceiveMessageHandler(){

            public void onReceiveMessage(byte[] message) {
                String text = "";
                try {
                    text = new String(message, "UTF8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                mOutput.append("\n"+text);
            }
        });

    }

    private class send extends AsyncTask<String, Void, Void > {

        @Override
        protected Void doInBackground(String... Message) {
            try {

                ConnectionFactory factory = new ConnectionFactory();
                factory.setHost("192.168.166.43");

//my internet connection is a bit restrictive so I have use an external server
// which has RabbitMQ installed on it. So I use "setUsername" and "setPassword"
               // factory.setUsername("XXXXX");
                //factory.setPassword("xxxx");
                Connection connection = factory.newConnection();
                Channel channel = connection.createChannel();
                channel.exchangeDeclare(EXCHANGE_NAME, "fanout", true);
                channel.queueDeclare(QUEUE_NAME, false, false, false, null);
                String tempstr="";
                for(int i=0;i<Message.length;i++)
                    tempstr+=Message[i];

                channel.basicPublish(EXCHANGE_NAME,QUEUE_NAME, null, tempstr.getBytes());

                channel.close();

                connection.close();

            }
            catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            }
            // TODO Auto-generated method stub
            return null;
        }

    }

    @Override
    protected void onResume() {
        super.onPause();
        mConsumer.connectToRabbitMQ();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mConsumer.dispose();
    }
}