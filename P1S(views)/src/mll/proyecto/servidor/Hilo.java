package mll.proyecto.servidor;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;

public class Hilo implements Runnable {

	private ServerSocket servidor;
	private Socket cliente;

	private DataOutputStream salida;
	private DataInputStream entrada;

	private Connection conexion;
	private Statement stm;
	private ResultSet rs;
	private PreparedStatement pstmt;

	private String consulta, insertar, consultA[], usuario = "root", contrasena = "Yusv.wH3";;
	private int numero, contadorVista, contador, contadorIntermedia;
	private static final int caracteristicas = 106;

	private ArrayList<Integer> carAfirmativas = new ArrayList<Integer>();

	public Hilo(ServerSocket s) {
		//El Hilo tiene que saber cual es el Servidor
		servidor = s;
	}

	public void ejecutaConsulta(int numero){
		//Cuando ejecutamos una consulta hacemos saber al Cliente (Jugar) que se está haciendo 
		//una pregunta para que la muestre por pantalla con la palabra clave "question"
		try {
			salida.writeUTF("question");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//y efectuamos la consulta
		consulta(numero);
	}

	public void consulta(int numero){
		String linea, resultado = "";
		try { 
			//Cogemos la consulta que corresponde a la característica almacenada
			consulta = consultA[numero];
			try {
				//Ejecutamos la consulta
				stm = conexion.createStatement();
				rs = stm.executeQuery(consulta);
				while (rs.next()) {
					resultado = rs.getString(1);
				}
			} catch (SQLException e) {
				try {
					rs.close();
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				e.printStackTrace();
			}
			//Enviamos al Cliente el resultado de la consulta para pregunta
			salida.writeUTF(resultado);
			try{
				//Leemos si el Jugador ha dicho que si o que no a la característica mostrada
				linea = entrada.readUTF();
				if(linea !=null){
					if(!(linea.equals(""))){
						//Si la respuesta a la pregunta es si, metemos la característica, en este caso su equivalente en número 
						//en un ArrayList de características afirmativas. También creamos la vista de la respuesta afirmativa y 
						//realizamos la lógica de que pregunta viene después en caso afirmativo
						if (linea.equals("si")) {
							carAfirmativas.add(numero);
							crearVista(numero);
							logicaSi(numero);
						}
						//Si la respuesta a la pregunta es no, simplemente efectuamos la lógica para saber que pregunta viene
						//después así como en caso de preguntas antagónicas añadir el dato a caracteristicas afirmativas y crear
						//la vista correspondiente
						if (linea.equals("no")) {				
							logicaNo(numero);
						}
					}
				} 
			}catch(IOException e){
				e.printStackTrace();
			//Una vez terminado que cierre la salida, la entrada y el cliente
			}finally {
				if(salida != null){
					salida.flush();
					salida.close();
				}if(entrada !=null){
					entrada.close();
				}if(cliente !=null){
					cliente.close();
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			rs.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void crearVista(int afirmativa){
		//Creación de vista de caracteristicas afirmativas con el formato vista_X siendo X el número de característica
		String vista="Create or replace view adivina_personaje.vista_? as select distinct per_id from adivina_personaje.tener where car_id = ?";
		if(carAfirmativas.contains(afirmativa)){
			try {
				//Se ejecuta la vista para crearla o reemplazarla en el caso de que existan vistas anteriores de otros
				//juegos
				pstmt = conexion.prepareStatement(vista);
				pstmt.setInt(1, contadorVista+1);
				pstmt.setInt(2, afirmativa+1);
				int filas = pstmt.executeUpdate();
			} catch (SQLException e) {
				try {
					pstmt.close();
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				//En el caso de que no se haya podido crear la vista se muestra en el Servidor un error
				System.out.println("No se ha podido crear la vista de los personajes con respecto a las caracteristicas");
			}
			//Se incrementa el número de la vista
			contadorVista++;
			//Se crea la vista intermedia de las vistas y se efectúa el inner join de las mismas para las características comunes
			ejecutarInnerJoinIntermedias();
		}
	}

	public void crearVistaIntermedia(){
		String intermedia="Create or replace view adivina_personaje.intermedia_? as select distinct adivina_personaje.vista_?.per_id from adivina_personaje.vista_? inner join adivina_personaje.vista_? on adivina_personaje.vista_?.per_id = adivina_personaje.vista_?.per_id";
		try {
			//Se ejecuta la vista intermedia que conjuga las características comunes de las vistas dos a dos
			pstmt = conexion.prepareStatement(intermedia);
			pstmt.setInt(1, contadorVista-1);
			pstmt.setInt(2, contadorVista-1);
			pstmt.setInt(3, contadorVista-1);
			pstmt.setInt(4, contadorVista);
			pstmt.setInt(5, contadorVista-1);
			pstmt.setInt(6, contadorVista);
			int filas = pstmt.executeUpdate();
		} catch (SQLException e) {
			try {
				pstmt.close();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			//En el caso de que no se haya podido crear la vista intermedia se muestra un error en Servidor
			System.out.println("No se ha podido crear la vista intermedias para los joins de las vistas");
		}
		//Se incrementa el contador de vistas intermedias
		contadorIntermedia++;
	}

	public void ejecutarInnerJoinIntermedias(){
		//Si el contador de la vista es mayor que uno se crea la vista intermedia porque precisa de dos vistas para poder crearse
		if(contadorVista > 1){
			crearVistaIntermedia();
			//Si el contador de la vista intermedia es mayor que uno se efectua el inner join de las vistas intermedias ya que precisa de dos intermedias
			//para poder efectuarse
			if(contadorIntermedia > 1){
				//Efectuamos el inner join de las vistas intermedias dos a dos
				String consultaInner="Select distinct adivina_personaje.intermedia_"+(contadorIntermedia-1)+".per_id from adivina_personaje.intermedia_"+
						(contadorIntermedia-1)+" inner join adivina_personaje.intermedia_"+(contadorIntermedia)+" on adivina_personaje.intermedia_"+(contadorIntermedia-1)+
						".per_id = adivina_personaje.intermedia_"+(contadorIntermedia)+".per_id";
				try {
					//Ejecutamos el inner join
					rs = stm.executeQuery(consultaInner);
					contador = 0;
					while(rs.next()){
						numero = rs.getInt(1);
						contador++;
					}
					//Si la consulta devuelve mas de un resultado
					if(contador > 1){
						//Seguir preguntando
					//Si la consulta devuelve un resultado
					}else if(contador == 1){
						//se ha adivinado el personaje por tanto se efectúa el método resultado
						resultadoPersonaje(numero);
					//Si la consulta no devuelve ningun resultado
					}else if(contador == 0){
						//querrá decir que no existe ningún personaje con dichas características
						//Para tener un número razonable de características a insertar, obligaremos a que sean 8 siendo el tamaño del ArrayList mayor que 8
						if(carAfirmativas.size()>8){
							try {
								//Enviamos al Cliente la palabra clave "noresult" para que muestre por pantalla el dialogo de que no se ha adivinado personaje
								salida.writeUTF("noresult");
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							//y además insertamos el personaje y borramos el ArrayList de características afirmativas para posteriores juegos
							insertarPersonaje();
							carAfirmativas.clear();
						}
					}
				} catch (SQLException e) {
					try {
						rs.close();
					} catch (SQLException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					//En el caso de que no se haya podido hacer el join de las vistas intermedias se muestra un error en el Servidor
					System.out.println("No se ha podido hacer el join de las vistas intermedias");
				}
			}
		}
	}

	public void logicaSi(int numero){
		//Por ejemplo en el primer caso si es hombre, preguntar si es calvo
		//y si es mujer preguntar si tiene pelo largo
		switch(numero){
		case 0: ejecutaConsulta(2);break;//hombre
		case 1: ejecutaConsulta(4);break;//mujer
		case 2: ejecutaConsulta(3);break;//calvo
		case 3: ejecutaConsulta(4);break;//barba o bigote
		case 4: ejecutaConsulta(6);break;//pelo largo
		case 5: ejecutaConsulta(6);break;//pelo corto
		case 6: ejecutaConsulta(12);break;//canoso
		case 7: ejecutaConsulta(12);break;//pelirrojo
		case 8: ejecutaConsulta(12);break;//rubio
		case 9: ejecutaConsulta(12);break;//moreno
		case 10: ejecutaConsulta(12);break;//castaño
		case 11: ejecutaConsulta(12);break;//color de pelo poco común
		case 12: ejecutaConsulta(14);break;//ojos claros
		case 13: ejecutaConsulta(14);break;//ojos oscuros
		case 14: ejecutaConsulta(16);break;//alto
		case 15: ejecutaConsulta(16);break;//bajo
		case 16: ejecutaConsulta(19);break;//complexión gruesa
		case 17: ejecutaConsulta(19);break;//complexión normal
		case 18: ejecutaConsulta(19);break;//complexión pequeña
		case 19: ejecutaConsulta(23);break;//de raza blanca
		case 20: ejecutaConsulta(23);break;//de raza amarilla
		case 21: ejecutaConsulta(23);break;//de raza negra
		case 22: ejecutaConsulta(23);break;//mulato
		case 23: ejecutaConsulta(24);break;//gafas
		case 24: ejecutaConsulta(25);break;//actual
		case 25: ejecutaConsulta(32);break;//menos de 20 años
		case 26: ejecutaConsulta(32);break;//20-30
		case 27: ejecutaConsulta(32);break;//31-40
		case 28: ejecutaConsulta(32);break;//41-50
		case 29: ejecutaConsulta(32);break;//51-60
		case 30: ejecutaConsulta(32);break;//61-70
		case 31: ejecutaConsulta(32);break;//más de 70
		case 32: ejecutaConsulta(33);break;//de una isla
		case 33: ejecutaConsulta(54);break;//de las Islas Baleares
		case 34: ejecutaConsulta(54);break;//de las Islas Canarias
		case 35: ejecutaConsulta(36);break;//de una ciudad autonoma
		case 36: ejecutaConsulta(54);break;//de Ceuta
		case 37: ejecutaConsulta(54);break;//de Melilla
		case 38: ejecutaConsulta(39);break;//de una comunidad uniprovincial
		case 39: ejecutaConsulta(54);break;//de Asturias
		case 40: ejecutaConsulta(54);break;//de Cantabria
		case 41: ejecutaConsulta(54);break;//de La Rioja
		case 42: ejecutaConsulta(54);break;//de Madrid
		case 43: ejecutaConsulta(54);break;//de Murcia
		case 44: ejecutaConsulta(54);break;//de Navarra
		case 45: ejecutaConsulta(54);break;//de Andalucía
		case 46: ejecutaConsulta(54);break;//de Aragón
		case 47: ejecutaConsulta(54);break;//de Castilla y León
		case 48: ejecutaConsulta(54);break;//de Castilla-La Mancha
		case 49: ejecutaConsulta(54);break;//de Cataluña
		case 50: ejecutaConsulta(54);break;//de la Comunidad Valenciana
		case 51: ejecutaConsulta(54);break;//de Extremadura
		case 52: ejecutaConsulta(54);break;//de Galicia
		case 53: ejecutaConsulta(54);break;//del País Vasco
		case 54: ejecutaConsulta(55);break;//del mundo de la política
		case 55: ejecutaConsulta(56);break;//del partido del gobierno
		case 58: ejecutaConsulta(59);break;//ministro
		case 71: ejecutaConsulta(72);break;//oposición
		case 76: ejecutaConsulta(77);break;//secretario general sindicato
		case 79: ejecutaConsulta(80);break;//del mundo del deporte
		case 82: ejecutaConsulta(83);break;//jugador de baloncesto
		case 86: ejecutaConsulta(87);break;//ciclista
		case 87: ejecutaConsulta(88);break;//ganador vuelta ciclista
		case 91: ejecutaConsulta(92);break;//futbolista
		case 92: ejecutaConsulta(93);break;//de Primera División
		case 96: ejecutaConsulta(97);break;//de Segunda División
		case 98: ejecutaConsulta(99);break;//del mundo de la música
		case 99: ejecutaConsulta(100);break;//cantante
		case 100: ejecutaConsulta(101);break;//grupo musical
		case 101: ejecutaConsulta(103);break;//conocido solo en españa
		}
	}

	public void logicaNo(int numero){
		switch(numero){
		case 0: carAfirmativas.add(1); crearVista(1); ejecutaConsulta(4); break;//no hombre --> mujer
		case 4: carAfirmativas.add(5); crearVista(5); ejecutaConsulta(6); break;//no pelo largo --> corto
		case 10: carAfirmativas.add(11); crearVista(11); ejecutaConsulta(12); break;//no castaño --> color poco común
		case 12: carAfirmativas.add(13); crearVista(13); ejecutaConsulta(14); break;//no ojos claros --> oscuros
		case 14: carAfirmativas.add(15); crearVista(15); ejecutaConsulta(16); break;//no alto --> bajo
		case 17: carAfirmativas.add(18); crearVista(18); ejecutaConsulta(19); break;//no complexión normal --> delgado
		case 24: ejecutaConsulta(32); break;
		case 32: ejecutaConsulta(35); break;
		case 35: ejecutaConsulta(38); break;
		case 38: ejecutaConsulta(45); break;
		case 54: ejecutaConsulta(79); break;
		case 79: ejecutaConsulta(98); break;
		case 82: ejecutaConsulta(85); break;
		case 86: ejecutaConsulta(89); break;
		case 91: ejecutaConsulta(98); break;
		case 98: ejecutaConsulta(104); break;
		case 99: ejecutaConsulta(101); break;
		default: ejecutaConsulta(numero+1); break;
		}
	}
	
	public void insertarPersonaje() {
		String afirNeg, nombre, apellido;
		try {
			//Si recibimos respuesta
			if((afirNeg = entrada.readUTF())!=null){
				if (!(afirNeg.equals(""))) {
					//Si el dato recibido contiene la palabra nombre querrá decir que recibimos el nombre y apellido del personaje
					if (afirNeg.contains("Nombre")) {
						//por tanto los recogemos
						int posN = afirNeg.indexOf("#");
						int posA = afirNeg.lastIndexOf("#");
						int posFN = afirNeg.indexOf("$");
						nombre = afirNeg.substring(posN + 1, posFN);
						apellido = afirNeg.substring(posA + 1);
						//Si no se inserta ni nombre ni apellido o se introduce nombre sin apellido o viceversa
						if ((nombre.equals("")) || (apellido.equals("")) || ((apellido.equals("")) && (nombre.equals("")))) {
							//Se dice que no se ha podido insertar
							System.out.println("No se ha podido insertar nada por falta de datos");
						} else {
							//Se comprueba si existe el personaje haciendo la consulta en la base de datos con dicho nombre y apellido
							String existe = "Select per_id from adivina_personaje.personaje where nombre like '%"+nombre+"%' and apellido like '%"+apellido+"%'";
							try {
								//Se ejecuta la consulta
								stm = conexion.createStatement();
								rs = stm.executeQuery(existe);
								contador = 0;
								while (rs.next()) {
									//Si se obtiene resultado lo veremos en contador
									numero = rs.getInt(1);
									contador++;
								}
								//Si no existe el personaje
								if(contador == 0){
									//Se tiene que insertar tanto en la tabla Personaje como en la tabla Tener, para ello se tiene que saber cual es la identificación
									//del personaje, para ello se cuentan el numero de personajes que existen para insertar el siguiente número
									consulta = "Select count(*) from adivina_personaje.personaje";
									try {
										stm = conexion.createStatement();
										rs = stm.executeQuery(consulta);
										while (rs.next()) {
											numero = rs.getInt(1);
										}
										insertarTablaPersonaje(nombre,apellido);
										insertarTablaTener(numero+1);
									} catch (SQLException e) {
										try {
											rs.close();
										} catch (SQLException e1) {
											// TODO Auto-generated catch block
											e1.printStackTrace();
										}
										e.printStackTrace();
									}
								}else{
									//Si la consulta devuelve un resultado se actualiza el personaje, por tanto en la tabla Tener se insertan 
									//los valores correspondientes de las características a añadir del personaje existente
									insertarTablaTener(numero);
								}
							} catch (SQLException e) {
								try {
									rs.close();
								} catch (SQLException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
								e.printStackTrace();
							}
						}
					//Si se recibe correcto en la respuesta querrá decir que el usuario ha dicho que si es el personaje que ha pensado por tanto no es necesario
					//que insertemos nada
					} else if (afirNeg.contains("correcto")) {
						System.out.println("No se ha tenido que insertar nada por que el personaje es el adecuado");
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//Inserción en la tabla personaje recogiendo el nombre y apellido correspondiente
	public void insertarTablaPersonaje(String nombre, String apellido){
		//La id será el número de personajes existentes más uno
		int id = numero + 1;
		try {
			//Se realiza la insercción con el id, el nombre y el apellido
			insertar = "Insert into adivina_personaje.personaje values(?,?,?)";
			pstmt = conexion.prepareStatement(insertar);
			pstmt.setInt(1, id);
			pstmt.setString(2, nombre);//coger nombre introducido en dialogo
			pstmt.setString(3, apellido);//coger apellido introducido en dialogo
			int filas = pstmt.executeUpdate();
			System.out.println("Se ha insertado un personaje con id " + id + " ,nombre " + nombre + " y apellido " + apellido);
		} catch (SQLException e) {
			try {
				pstmt.close();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			e.printStackTrace();
		}	
	}

	//Inserción en tabla Tener
	public void insertarTablaTener(int id){
		//La id del personaje se recoge el método insertarPersonaje que será el mismo número en caso de que el personaje ya exista, y un número mayor en el caso de que
		//no exista el personaje y haya que insertar sus características y la id de las características será las almacendas en carAfirmativas más uno para que se 
		//correspondan con las almacenadas en la base de datos
		int id_car = 0;
		//Se recorre el ArrayList con un iterador
		Iterator<Integer> it=carAfirmativas.iterator();
		//mientras tenga valores
		while(it.hasNext()){
			//la id de la característica será como he dicho antes el valor almacenado en el ArrayList más uno
			id_car = it.next();
			id_car = id_car +1;
			//Comprobamos si existe ese personaje con esa característica
			String existe = "Select * from adivina_personaje.tener where per_id = "+id+" and car_id = "+ id_car;
			try {
				stm = conexion.createStatement();
				rs = stm.executeQuery(existe);
				contador = 0;
				while (rs.next()) {
					numero = rs.getInt(1);
					contador++;
				}
			} catch (SQLException e2) {
				try {
					rs.close();
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				e2.printStackTrace();
			}
			try {
				//Si no existe insertamos el id del personaje con el id de la característica correspondiente
				if(contador == 0){
					insertar = "Insert into adivina_personaje.tener values(?,?)";
					pstmt = conexion.prepareStatement(insertar);
					pstmt.setInt(1, id);
					pstmt.setInt(2, id_car);//coger id_caracteristicas que se han almacenado en el ArrayList correspondiente
					int filas = pstmt.executeUpdate();
					System.out.println("Se ha insertado un personaje con id " + id + " y la caracteristica " + id_car);
				}
				else
				{
					//Si existe simplemente establecemos un mensaje en el Servidor conforme ese personaje con esa característica ya existe
					System.out.println("El personaje con id " + id + " y la caracteristica " + id_car + " ya existe");
				}
			} catch (SQLException e) {
				try {
					pstmt.close();
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				e.printStackTrace();
			}
		}
	}
	
	//Permite mostrar el personaje e insertarlo si fuese el caso
	public void resultadoPersonaje(int numero){
		String resultadoA[];
		resultadoA = new String[2];
		//Enviamos la palabra clave "person" para que el Cliente sepa que se ha adivinado el personaje
		try {
			salida.writeUTF("person");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			//Ejecutamos la consulta con el número correspondiente al resultado de las vistas
			consulta = "Select nombre, apellido from adivina_personaje.personaje where per_id = "+numero;
			rs = stm.executeQuery(consulta);
			while (rs.next()) {
				resultadoA[0] = rs.getString(1);
				resultadoA[1] = rs.getString(2);
			}
			try {
				//Enviamos el nombre y apellido del personaje para que se muestre por pantalla en la parte del Cliente
				salida.writeUTF(resultadoA[0] + " " + resultadoA[1]);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//Si no es el personaje correcto se tendrá que insertar y además eliminar el ArrayList de las características para futuras jugadas
			insertarPersonaje();
			carAfirmativas.clear();
		} catch (SQLException e) {
			try {
				rs.close();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			System.out.println("No se ha podido devolver el resultado, con nombre y apellido de personaje");
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			//Primero, aceptamos el Cliente
			cliente = servidor.accept();
			System.out.println("Conectado a jugador");
			//Creamos la entrada y la salida
			salida = new DataOutputStream(cliente.getOutputStream());
			entrada = new DataInputStream(cliente.getInputStream());
			//Conectamos el Servidor con la base de datos MySQL con su usuario y contraseña correspondientes
			try {
				DriverManager.registerDriver(new com.mysql.jdbc.Driver());
				conexion = DriverManager.getConnection("jdbc:mysql://localhost/adivina_personaje", usuario, contrasena);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//Se crea un Array de consultas de características de tal manera que en el momento que se inicia el juego están a disposición del Servidor para 
			//mostrarlas al Cliente
			consultA = new String[caracteristicas];
			for (int i = 0; i < caracteristicas; i++) {
				consultA[i] = "Select nombre from caracteristica where car_id = " + (i + 1);
			}
			//Iniciamos la consulta con la característica hombre
			consulta(0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
