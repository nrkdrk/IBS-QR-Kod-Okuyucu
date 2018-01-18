package com.nrkdrk.IbsQrReader;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Berk Can on 24.10.2017.
 */

public class Client extends Activity {

    private static Socket socket;
    private static ServerSocket serverSocket;
    private static InputStreamReader ınputStreamReader;
    public static String data;
    private static PrintWriter printWriter;
    public static String SERVER_IP=null;
    public static int SERVERPORT;
    EditText et;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        et = (EditText) findViewById(R.id.EditText01);
        et.setText(data);
    }

    public void onClick(View view) {
        data=et.getText().toString();
        Task task=new Task();
        task.execute();
        Toast.makeText(this, "Veriler Gönderildi", Toast.LENGTH_SHORT).show();
    }

    class Task extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try{
                socket=new Socket(SERVER_IP,SERVERPORT);
                printWriter = new PrintWriter(socket.getOutputStream());
                printWriter.write(data);
                printWriter.flush();
                printWriter.close();
                socket.close();
            }catch (IOException e){
                e.printStackTrace();
            }
            return null;
        }
    }
}