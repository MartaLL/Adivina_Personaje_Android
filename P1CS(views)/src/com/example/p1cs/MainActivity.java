package com.example.p1cs;

import android.os.Bundle;
import android.os.Vibrator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Establecemos el layout correspondiente
		setContentView(R.layout.activity_main);
		//Definimos el bot�n de jugar y abrimos el juego en s�
		Button btnJuego=(Button)findViewById(R.id.button1);
		btnJuego.setOnClickListener(new OnClickListener(){
			public void onClick(View arg0) {
				Intent intent=new Intent(MainActivity.this,Jugar.class);
				startActivity(intent);
			}
		});
		//Definimos el bot�n salir para salir del men� principal y del juego
		Button btnSalir=(Button)findViewById(R.id.button2);
		btnSalir.setOnClickListener(new OnClickListener(){
			public void onClick(View arg0) {
				dialogoSalir();
			}
		});
	}

	//Establecemos el men� que en este caso ser� innecesario por ello se devuelve false
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return false;
	}

	public void dialogoSalir()
	{
		//Establecemos la vibraci�n cuando salimos
		Vibrator vibra=(Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
		vibra.vibrate(200);
		//Creamos un dialogo de alerta con su t�tulo y su pregunta correspondiente, as� como los botones
		//de si se desea salir o no
		AlertDialog.Builder dialogo=new AlertDialog.Builder(this);
		dialogo.setTitle("Salir");
		dialogo.setMessage("�Esta seguro que desea salir?");
		//Si el usuario dice que no quiere salir
		dialogo.setNegativeButton("No", new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int which){
				//simplemente salimos del di�logo de alerta
				dialog.cancel();
			}
		});
		//Si el usuario dice que si quiere salir
		dialogo.setPositiveButton("Si", new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int which){
				//cerramos el juego
				MainActivity.this.finish();
			}
		});
		//Mostramos el di�logo de alerta
		dialogo.show();
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		//Cuando hacemos click en la flecha atr�s estando en el men� principal
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			//Se muestra el di�logo salir 
			dialogoSalir();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	} 
}
