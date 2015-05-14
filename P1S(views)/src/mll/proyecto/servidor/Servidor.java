package mll.proyecto.servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Servidor {

	private ServerSocket servidor;
	private Hilo hilo[];

	/**
	 * Constructor del Servidor que se encarga de inicializar los hilos y el Socket
	 */
	public Servidor() {
		Logger.getLogger(getClass().getName()).log(Level.INFO, "-->>CREANDO SERVIDOR<<--");
		try {
			//El Servidor a la espera en el puerto 2812
			servidor = new ServerSocket(2812);
			Logger.getLogger(getClass().getName()).log(Level.INFO, "-->Esperando a jugador...");
			//Ejecutamos los Hilos
			Logger.getLogger(getClass().getName()).log(Level.INFO, "-->>CREANDO HILOS<<--");
			hilo = new Hilo[100];
			for (int i = 0; i < 100; i++) {
				hilo[i] = new Hilo(servidor);
//				new Thread(hilo[i]).start();
				Thread thread = new Thread(hilo[i]);
				thread.start();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Ejecución del servidor
	 * @param args
	 */
	public static void main(String[] args) {
		new Servidor();
	}

}
