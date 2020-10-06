package ServerTCP;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Scanner;

public class Server {

	public static int NUMERO_CONEXIONES;
	public static String[] respuestasClientes;
	public final static String NOMBRE_1 = "archivo250";
	public final static String NOMBRE_2 = "archivo100";
	public final static String UBICACION_LOG = "data/informes/log";
	public static BufferedWriter writer;
	
	public static String DIRECCION = "192.168.137.1";
	public static int PUERTO = 49200;

	public static void main(String[] args) {

		Scanner lectorConsola = new Scanner(System.in);
		ServerSocket socket;

		// Declaramos un bloque try y catch para controlar la ejecución del subprograma
		try {
			String time = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss").format(Calendar.getInstance().getTime());
			File logFile = new File(UBICACION_LOG + time + ".txt");
			writer = new BufferedWriter(new FileWriter(logFile));
			String timeLog = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(Calendar.getInstance().getTime());
			writer.write("Fecha y hora: " + timeLog);
			writer.newLine();
			writer.flush();
			
			//System.out.println("Escriba el puerto en el que quiere realizar la conexión");
			//int puerto = lectorConsola.nextInt();

			socket = new ServerSocket(PUERTO);

			System.out.println("Ingrese el numero de conexiones que se van a manejar: ");
			int numConexiones = lectorConsola.nextInt();
			NUMERO_CONEXIONES = numConexiones;

			writer.write("Conexiones para la prueba: " + numConexiones);
			writer.newLine();
			writer.flush();

			System.out.println(
					"Ingrese el numero del archivo que quiere enviar: (1) para el de 250 MB o (2) para el de 100 MB");
			int numeroArchivo = lectorConsola.nextInt();
			lectorConsola.close();
			if (numeroArchivo == 1)
				writer.write("Se va a realizar el envío de un archivo llamado " + NOMBRE_1);
			else
				writer.write("Se va a realizar el envío de un archivo llamado " + NOMBRE_2);
			writer.newLine();
			writer.flush();


			Socket[] socketCli = new Socket[numConexiones];
			int numClientes = 0;
			System.out.println("Esperando clientes");
			while (numClientes < NUMERO_CONEXIONES) {
				try {
					socketCli[numClientes] = socket.accept();
					DataOutputStream dOut = new DataOutputStream(socketCli[numClientes].getOutputStream());
					DataInputStream dIn = new DataInputStream(socketCli[numClientes].getInputStream());
					if (numeroArchivo == 1) {
						dOut.writeByte(1);
						dOut.writeUTF(NOMBRE_1);
						dOut.flush();
					} else {
						dOut.writeByte(1);
						dOut.writeUTF(NOMBRE_2);
						dOut.flush();
					}
					numClientes++;
					if (dIn.readByte() == 2)
						System.out
								.println("Llegó el cliente " + numClientes + " y está esperando el envío del archivo");
					dOut.write(numClientes);
					writer.write("Cliente conectado número " + numClientes);
					writer.newLine();
					writer.flush();
				} catch (Exception e) {
					// TODO: handle exception
					System.out.println("Hubo un problema con algún cliente");
				}
			}
			System.out.println("Se comenzará el envio de archivos a los clientes");
			for (int i = 0; i < socketCli.length; i++) {
				ProtocolThread thread = new ProtocolThread(socketCli[i], numeroArchivo, writer, (i + 1));
				thread.start();
			}
		}
		catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(1);
			try {
				writer.write("Hubo un error con el envío");
				writer.newLine();
				writer.flush();
			} catch (Exception ex) {
				// TODO: handle exception
			}
		}
	}
}