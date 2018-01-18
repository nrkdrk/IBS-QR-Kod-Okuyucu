package com.nrkdrk.IbsQrReader;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Scroller;
import android.widget.TextView;
import android.widget.Toast;

import com.dlazaro66.qrcodereaderview.QRCodeReaderView;
import com.dlazaro66.qrcodereaderview.QRCodeReaderView.OnQRCodeReadListener;

import org.apache.http.conn.util.InetAddressUtils;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Enumeration;

public class DecoderActivity extends AppCompatActivity
    implements ActivityCompat.OnRequestPermissionsResultCallback, OnQRCodeReadListener {

  private static final int MY_PERMISSION_REQUEST_CAMERA = 0;
  private ViewGroup mainLayout;
  private TextView resultTextView;
  private QRCodeReaderView qrCodeReaderView;
  private CheckBox flashlightCheckBox,enableDecodingCheckBox;
  private ScrollView scrollingView;
  private PointsOverlayView pointsOverlayView;
  public static String data,qrEditValue,qrValue,SERVER_IP;
  public static View contentQr;
  private static Socket socket;
  private static PrintWriter printWriter;
  private static int SERVERPORT = 7844;

  @RequiresApi(api = Build.VERSION_CODES.M)
  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_decoder);
    SERVER_IP=getIPAddress();
    mainLayout = (ViewGroup) findViewById(R.id.main_layout);
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
      initQRCodeReaderView();
    } else {
      requestCameraPermission();
    }

    if (!SharedPreferencesSettings.GetKey(getBaseContext(), "SERVER_IP").equals("")) {
      SERVER_IP = SharedPreferencesSettings.GetKey(getBaseContext(), "SERVER_IP");
      Toast.makeText(this, SERVER_IP.toString() + " adresine bağlanıldı", Toast.LENGTH_LONG).show();
    }else{
      IPControlDialog();
    }
  }

  @Override protected void onResume() {
    super.onResume();
    if (qrCodeReaderView != null) {
      qrCodeReaderView.startCamera();
    }
  }

  @Override protected void onPause() {
    super.onPause();
    if (qrCodeReaderView != null) {
      qrCodeReaderView.stopCamera();
    }
  }

  @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
    @NonNull int[] grantResults) {
    if (requestCode != MY_PERMISSION_REQUEST_CAMERA) {
      return;
    }

    if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      Snackbar.make(mainLayout, "Kamera izni verildi.", Snackbar.LENGTH_SHORT).show();
      initQRCodeReaderView();
    } else {
      Snackbar.make(mainLayout, "Kamera izin isteği reddedildi.", Snackbar.LENGTH_SHORT)
          .show();
    }
  }

  @Override public void onQRCodeRead(String text, PointF[] points) {
    resultTextView.setText(text);
    data=text;
    pointsOverlayView.setPoints(points);
  }

  private void requestCameraPermission() {
    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
      Snackbar.make(mainLayout, "Kamera erişimi gerekiyor.",
          Snackbar.LENGTH_INDEFINITE).setAction("Tamam", new View.OnClickListener() {
        @Override public void onClick(View view) {
          ActivityCompat.requestPermissions(DecoderActivity.this, new String[] {
              Manifest.permission.CAMERA
          }, MY_PERMISSION_REQUEST_CAMERA);
        }
      }).show();
    } else {
      Snackbar.make(mainLayout, "İzin mevcut değil. Kamera izni gerekli.",
          Snackbar.LENGTH_SHORT).show();
      ActivityCompat.requestPermissions(this, new String[] {
          Manifest.permission.CAMERA
      }, MY_PERMISSION_REQUEST_CAMERA);
    }
  }

  private void initQRCodeReaderView() {
    contentQr = getLayoutInflater().inflate(R.layout.content_decoder, mainLayout, true);

    qrCodeReaderView = (QRCodeReaderView) contentQr.findViewById(R.id.qrdecoderview);
    resultTextView = (TextView) contentQr.findViewById(R.id.result_text_view);
    scrollingView=(ScrollView) contentQr.findViewById(R.id.scroll);
    flashlightCheckBox = (CheckBox) contentQr.findViewById(R.id.flashlight_checkbox);
    enableDecodingCheckBox = (CheckBox) contentQr.findViewById(R.id.enable_decoding_checkbox);
    pointsOverlayView = (PointsOverlayView) contentQr.findViewById(R.id.points_overlay_view);

    qrCodeReaderView.setAutofocusInterval(2000L);
    qrCodeReaderView.setOnQRCodeReadListener(this);
    qrCodeReaderView.setBackCamera();

    flashlightCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        qrCodeReaderView.setTorchEnabled(isChecked);
      }
    });
    enableDecodingCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        qrCodeReaderView.setQRDecodingEnabled(isChecked);
      }
    });
    qrCodeReaderView.startCamera();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.activity_decoder, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.QrValueTransfer:
          if (data!=null)
            QrValueShowDialog(data.toString());
          else
            Toast.makeText(this, "Herhangi bir QR değeri bulunmuyor", Toast.LENGTH_SHORT).show();
        break;
      case R.id.QrValueSave:
          Toast.makeText(this, "kaydedildi", Toast.LENGTH_SHORT).show();
        break;
      case R.id.Settings:
        SettingsPageDialog();
        break;
    }
    return true;
  }

  private void QrValueShowDialog(final String qrData){
    final AlertDialog.Builder alert = new AlertDialog.Builder(this);
    final TextView textView = new TextView(getApplicationContext());
    final EditText edittext = new EditText(getApplicationContext());

    textView.setTextColor(Color.BLACK);
    edittext.setTextColor(Color.BLACK);
    //alert.setMessage("Enter Your Message");
    alert.setTitle("QR Değerini Pc'ye Aktar veya Düzenle");
    alert.setView(textView);
    //alert.setView(edittext);
    textView.setScroller(new Scroller(this));
    textView.setVerticalScrollBarEnabled(true);
    textView.setMovementMethod(new ScrollingMovementMethod());
    //alert.setView(edittext);
    textView.setText(qrData);
    //edittext.setText(qrData);

    alert.setPositiveButton("Aktar", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        qrValue = textView.getText().toString();
        if (SERVER_IP != null) {
          SocketClass SocketClass=new SocketClass();
          SocketClass.execute();
          Toast.makeText(DecoderActivity.this, "QR Değeri Pc'ye Gönderildi", Toast.LENGTH_SHORT).show();
        }else {
          Toast.makeText(DecoderActivity.this, "Bağlantı IP'si bulunmuyor", Toast.LENGTH_SHORT).show();
        }
      }
    });

    alert.setNeutralButton("Düzenle", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        QrValueEdit(qrData);
      }
    });

    alert.setNegativeButton("Vazgeç", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {

      }
    });

    alert.show();
  }

  private void QrValueEdit(final String qrData){
    final AlertDialog.Builder alert = new AlertDialog.Builder(this);
    AlertDialog dialog = alert.create();
    dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE  | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

    final EditText edittext = new EditText(getApplicationContext());
    edittext.setTextColor(Color.BLACK);
    //alert.setMessage("Enter Your Message");
    alert.setTitle("QR Değerini Düzenle ve Pc'ye Aktar");
    alert.setView(edittext);
    edittext.setText(qrData);

    alert.setPositiveButton("Aktar", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        qrEditValue = edittext.getText().toString();
        qrValue=qrEditValue.toString();
        if (SERVER_IP != null) {
          SocketClass SocketClass=new SocketClass();
          SocketClass.execute();
          Toast.makeText(DecoderActivity.this, "QR Değeri Pc'ye Gönderildi", Toast.LENGTH_SHORT).show();
        }else {
          Toast.makeText(DecoderActivity.this, "Bağlantı IP'si bulunmuyor", Toast.LENGTH_SHORT).show();
        }
      }
    });

    alert.setNegativeButton("Vazgeç", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
      }
    });

    alert.show();
  }

  private void SettingsPageDialog(){
    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
    LayoutInflater inflater = this.getLayoutInflater();
    final View dialogView = inflater.inflate(R.layout.settings_dialog_layout, null);
    dialogBuilder.setView(dialogView);

    final EditText ıpEdt = (EditText) dialogView.findViewById(R.id.ıpEdt);
    final EditText portEdt = (EditText) dialogView.findViewById(R.id.portEdt);
    ıpEdt.setText(SERVER_IP);
    portEdt.setText(String.valueOf(SERVERPORT));

    dialogBuilder.setTitle("Sistem Ayarları");
    dialogBuilder.setPositiveButton("Tamam", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        SERVER_IP=ıpEdt.getText().toString();
        SERVERPORT=Integer.parseInt(portEdt.getText().toString());
        Client.SERVER_IP=SERVER_IP;
        Client.SERVERPORT=SERVERPORT;
        SharedPreferencesSettings.SaveKey(getBaseContext(), "SERVER_IP", SERVER_IP);
      }
    });
    dialogBuilder.setNegativeButton("Vazgeç", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        //pass
      }
    });
    AlertDialog b = dialogBuilder.create();
    b.show();
  }

  private void IPControlDialog(){
    AlertDialog.Builder builder = new AlertDialog.Builder(DecoderActivity.this);
    builder.setTitle("IBS QR Kod Okuyucu");
    builder.setMessage("Daha önce belirlenmiş bir bağlantı IP'si bulunmuyor.\nIP Adresi belirlemek ister misiniz?");
    builder.setNegativeButton("Hayır", new DialogInterface.OnClickListener(){
      public void onClick(DialogInterface dialog, int id) {

        //İptal butonuna basılınca yapılacaklar.Sadece kapanması isteniyorsa boş bırakılacak

      }
    });

    builder.setPositiveButton("Evet", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        SettingsPageDialog();
      }
    });
    builder.show();
  }

  class SocketClass extends AsyncTask<Void,Void,Void> {
    @Override
    protected Void doInBackground(Void... params) {
      try{
        socket=new Socket(SERVER_IP,SERVERPORT);
        printWriter = new PrintWriter(socket.getOutputStream());
        printWriter.write(qrValue);
        printWriter.flush();
        printWriter.close();
        socket.close();
      }catch (IOException e){
        e.printStackTrace();
      }
      return null;
    }
  }

  public String getIPAddress() {
    String ipaddress = "";

    try {
      for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
        NetworkInterface intf = en.nextElement();
        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
          InetAddress inetAddress = enumIpAddr.nextElement();
          if (!inetAddress.isLoopbackAddress() && InetAddressUtils.isIPv4Address(inetAddress.getHostAddress()) ) {
            ipaddress = inetAddress.getHostAddress();
          }
        }
      }
    } catch (SocketException ex) {
    }

    return ipaddress;
  }
}