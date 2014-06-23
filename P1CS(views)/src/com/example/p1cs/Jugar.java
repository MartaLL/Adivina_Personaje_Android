package com.example.p1cs;

import java.io.*;
import java.net.*;
import java.sql.Statement;

import android.os.*;
import android.app.*;
import android.content.*;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.util.Log;
import android.view.*;
import android.view.View.*;
import android.widget.*;

public class Jugar extends Activity {

	private Dialog dialogo = null;
	private TextView tvPregunta;
	private Button si, no;

	private DataInputStream entrada;
	private DataOutputStream salida;

	private String respuesta, nombrePer, apellidoPer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Establecemos el layout correspondiente
		setContentView(R.layout.activity_juego);
		//Se establecen el TextView de las caracter�sticas y los botones
		tvPregunta = (TextView) findViewById(R.id.textView1);
		si = (Button) findViewById(R.id.button3);
		no = (Button) findViewById(R.id.button4);
		//Lo siguiente se realiza para permitir el funcionamiento correcto de los Threads principales en un m�vil f�sico
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);
		//Se realiza la conexi�n a trav�s del Socket
		conexion();
		//Se establece la pregunta en el TextView
		pregunta();
	}

	//Se crea el men� del juego con las opciones Nuevo y Acerca De
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.juego, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.nuevo:
			//En el caso de que se haga click en Nuevo se iniciar� una nueva partida
			nuevo();
			return true;
		case R.id.acercaDe:
			//En el caso de que se haga click en Acerca De se mostrar� la informaci�n sobre la aplicaci�n
			acercaDe();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void conexion() {
		try {
			//Conectamos al Cliente con el Servidor insertando la ip del Servidor y en el puerto en el que est� escuchando
			Socket conexion = new Socket("192.168.4.117", 2804);
			//Creamos la entrada y la salida
			entrada = new DataInputStream(conexion.getInputStream());
			salida = new DataOutputStream(conexion.getOutputStream());
		}catch(IOException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void pregunta() {
		try {
			//Se recibe la pregunta a mostrar
			respuesta = entrada.readUTF();
			//y se pone en el TextView para que se muestre por pantalla
			tvPregunta.setText(respuesta);
			//Si el usuario hace click en si
			si.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					try {
						//Se env�a al Servidor la palabra clave s� y se mira si se recibe la soluci�n
						salida.writeUTF("si");
						solucion(entrada);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
			//Si el usuario hace click en no
			no.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					try {
						//Se env�a al Servidor la palabra clave no y se mira si se recibe la soluci�n
						salida.writeUTF("no");
						solucion(entrada);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	private void nuevo() {
		//Se inicia un nuevo juego con el layout inicial y estableciendo de nuevo el TextView y los botones, adem�s de establecer la Pol�tica para permitir la conexi�n
		//con m�viles reales y por �ltimo realizar la conexi�n y establecer la pregunta que debe aparecer por pantalla
		setContentView(R.layout.activity_juego);
		tvPregunta = (TextView) findViewById(R.id.textView1);
		si = (Button) findViewById(R.id.button3);
		no = (Button) findViewById(R.id.button4);
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);
		conexion();
		pregunta();
	}

	private void acercaDe() {
		//Se abre una Activity con la informaci�n correspondiente
		Intent intent = new Intent(Jugar.this, AcercaDe.class);
		startActivity(intent);
	}

	public void solucion(DataInputStream e) {
		try {
			//Comprobar la palabra clave que se recibe del Servidor
			respuesta = e.readUTF();
			//Si se recibe "person" del Servidor
			if ((respuesta.equals("person"))) {
				//entonces mostrar adivina con nombre y apellido del personaje recibido tambi�n del Servidor
				String personaje = e.readUTF();
				dialogoAdivina(personaje);
			//Si se recibe "question"
			} else if((respuesta.equals("question"))){
				//entonces seguir preguntando
				pregunta();
			//Si se recibe "noresult"
			} else if((respuesta.equals("noresult"))){
				//entonces mostrar el di�logo perder
				dialogoPerder();
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	private void dialogoAdivina(final String respuesta) {
		final TextView nombre;
		final Button yes, not;
		//Se establece vibraci�n cuando se adivine
		Vibrator vibra = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		vibra.vibrate(100);
		//Se crea el dialogo no permitiendo salir cancelando y estableciendo su t�tulo, su layout y especificando su TextView donde mostrar nombre y apellido y los botones
		//si y no para especificar si se ha adivinado el personaje
		dialogo = new Dialog(this);
		dialogo.setCancelable(false);
		dialogo.setTitle("Su personaje es...");
		dialogo.setContentView(R.layout.adivina);
		nombre = (TextView) dialogo.findViewById(R.id.textView2);
		nombre.setText(respuesta);
		yes = (Button) dialogo.findViewById(R.id.button5);
		//Si el usuario dice que el personaje es correcto
		yes.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				//cerramos el di�logo
				dialogo.dismiss();
				try {
					//y se env�a la palabra clave correcto al Servidor para que sepa que no se va a insertar nada
					salida.writeUTF("correcto");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//Cerramos la ventana correspondiente al juego volviendo al men� principal
				Jugar.this.finish();
			}
		});
		not = (Button) dialogo.findViewById(R.id.button6);
		//Si el usuario dice que el personaje no es el correcto
		not.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				//Se ocultan el TextView y los botones
				nombre.setTextColor(Color.TRANSPARENT);
				yes.setVisibility(View.INVISIBLE);
				not.setVisibility(View.INVISIBLE);
				//Y se ponen visibles los TextView y los EditText para la inserci�n del personaje adem�s del bot�n de aceptar
				TextView nombreI = (TextView) dialogo.findViewById(R.id.textView3);
				TextView apellidoI = (TextView) dialogo.findViewById(R.id.textView4);
				nombreI.setVisibility(View.VISIBLE);
				apellidoI.setVisibility(View.VISIBLE);
				final EditText intrNombre = (EditText) dialogo.findViewById(R.id.editText1);
				final EditText intrApellido = (EditText) dialogo.findViewById(R.id.editText2);
				intrNombre.setEnabled(true);
				intrNombre.setVisibility(View.VISIBLE);
				intrApellido.setEnabled(true);
				intrApellido.setVisibility(View.VISIBLE);
				final Button accept = (Button) dialogo.findViewById(R.id.button7);
				accept.setVisibility(View.VISIBLE);
				//Establecemos un toast para que se introduzcan tanto el nombre como el apellido de forma obligatoria
				Toast.makeText(Jugar.this, "Introduzca tanto el nombre como el apellido del personaje", Toast.LENGTH_LONG).show();
				//Si se hace click en aceptar
				accept.setOnClickListener(new OnClickListener() {
					public void onClick(View arg0) {
						try {
							//Se recogen los valores del nombre y del apellido
							nombrePer = intrNombre.getText().toString();
							apellidoPer = intrApellido.getText().toString();
							//Si vienen vacios
							if(nombrePer.equals("")&&apellidoPer.equals("")||nombrePer.equals("")||apellidoPer.equals("")){
								//mostrar el mismo toast que antes para obligar a insertar los datos
								Toast.makeText(Jugar.this, "Introduzca tanto el nombre como el apellido del personaje", Toast.LENGTH_LONG).show();
							//Si no vienen vac�os
							}else{
								//Enviar al Servidor el nombre y apellido para que se puedan insertar
								salida.writeUTF("Nombre#" + nombrePer + "$Apellido#" + apellidoPer);
								//Se cierra el dialogo
								dialogo.dismiss();
								//Se cierra la ventana del juego y se vuelve a la principal
								Jugar.this.finish();
							}
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				});
			}
		});
		//Mostramos el dialogo adivina
		dialogo.show();
	}

	private void dialogoPerder() {
		//Se establece una vibraci�n cuando no se adivina
		Vibrator vibra = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		vibra.vibrate(100);
		//Se crea el dialogo, no se permite que se cancele, se pone su t�tulo y el layout correspondiente
		dialogo = new Dialog(this);
		dialogo.setCancelable(false);
		dialogo.setTitle("No se ha podido adivinar su personaje...");
		dialogo.setContentView(R.layout.pierde);
		//adem�s establecemos los TextView para introducir y el bot�n para aceptar la introducci�n del personaje
		final EditText intrNombre = (EditText) dialogo.findViewById(R.id.editText3);
		final EditText intrApellido = (EditText) dialogo.findViewById(R.id.editText4);
		final Button ok = (Button) dialogo.findViewById(R.id.button8);
		//Establecemos un toast para que se introduzcan tanto el nombre como el apellido de forma obligatoria
		Toast.makeText(Jugar.this, "Introduzca tanto el nombre como el apellido del personaje", Toast.LENGTH_LONG).show();
		//Si se hace click en aceptar
		ok.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				try {
					//Se recogen los valores del nombre y del apellido
					nombrePer = intrNombre.getText().toString();
					apellidoPer = intrApellido.getText().toString();
					//Si vienen vacios
					if(nombrePer.equals("")&&apellidoPer.equals("")||nombrePer.equals("")||apellidoPer.equals("")){
						//mostrar el mismo toast que antes para obligar a insertar los datos
						Toast.makeText(Jugar.this, "Introduzca tanto el nombre como el apellido del personaje", Toast.LENGTH_LONG).show();
					//Si no vienen vac�os
					}else{
						//Enviar al Servidor el nombre y apellido para que se puedan insertar
						salida.writeUTF("Nombre#" + nombrePer + "$Apellido#" + apellidoPer);
						//Cerramos el di�logo
						dialogo.dismiss();
						//Cerramos la ventana del juego y volvemos al men� principal
						Jugar.this.finish();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		//Mostramos el di�logo de no adivinar
		dialogo.show();
	}
	
}
