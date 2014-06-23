package mll.proyecto.servidor;

import java.io.*;
import java.net.*;

public class Servidor {

    private ServerSocket servidor;
    private Hilo hilo[];

    public Servidor() {
        try {
        	//El Servidor a la espera en el puerto 2804
            servidor = new ServerSocket(2804);
            System.out.println("Esperando a jugador...");
            //Ejecutamos los Hilos
            hilo = new Hilo[100];
            for (int i = 0; i < 100; i++) {
                hilo[i] = new Hilo(servidor);
                new Thread(hilo[i]).start();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
    	//Iniciamos el Servidor
        Servidor s = new Servidor();
    }
    
}
