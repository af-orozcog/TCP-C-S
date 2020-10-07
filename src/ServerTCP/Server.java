package ServerTCP;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Scanner;

public class Server {

	public static int CANT_THREADS;
	public static String[] respuestasClientes;
	public final static String TEXTOS_PATH = "data/textos/";
	public final static String NOMBRE_1 = "texto1.txt";
	public final static String NOMBRE_2 = "texto2.txt";
	public final static String UBICACION_LOG = "data/informes/log-";
	public static BufferedWriter logWriter;
	
	public static int PUERTO = 49200;

	public static void main(String[] args) {
		System.out.println("Servidor TCP: ");
		
		// HERRAMIENTA PARA LECTURA DE LA CONFIGURACI�N DE LA CONEXI�N
		Scanner console = new Scanner(System.in);
		
		// PREPARACI�N DEL SOCKET
		ServerSocket socket = null;
		
		try {
			// INCIALIZACI�N DEL LOG SEG�N FECHA
			String time = new SimpleDateFormat("dd_MM_yyyy-HH_mm_ss").format(Calendar.getInstance().getTime());
			File logFile = new File(UBICACION_LOG + time + ".txt");
			logWriter = new BufferedWriter(new FileWriter(logFile));
			
			//System.out.println("Escriba el puerto en el que quiere realizar la conexi�n");
			//PUERTO = lectorConsola.nextInt();

			// CREACI�N DEL SOCKET
			socket = new ServerSocket(PUERTO);

			System.out.println("Ingrese n�mero de conexiones: ");
			CANT_THREADS = console.nextInt();

			writeLog("N�mero de conexiones: " + CANT_THREADS);

			String timeLog = new SimpleDateFormat("HH-mm-ss_dd/MM/yyyy").format(Calendar.getInstance().getTime());
			writeLog("Hora_Fecha: " + timeLog);
			
			// ELECCI�N DEL ARCHIVO
			System.out.println("Que archivo se va a enviar? (Escribir \"1\" para el de 250 MiB o \"2\" para el de 100 MiB)");
			
			String fileName;
			switch (console.nextInt()) {
			case 1:
				fileName = NOMBRE_1;
				break;
			case 2:
				fileName = NOMBRE_2;
				break;
			default:
				System.out.println("Error: N�mero de archivo inv�lido");
				return;
			}
			writeLog("Nombre del archivo: " + fileName + ".");
			writeLog("Tama�o del archivo: " + new File(TEXTOS_PATH + fileName) + " bytes.");
			console.close();
			
			// INICIALIZACI�N DE TODOS LOS SOCKETS
			Socket[] conections = new Socket[CANT_THREADS];
			System.out.println("Esperando conexiones...");
			int idCliente = 0;
			
			// RECEPCI�N DE UN NUEVO CLIENTE
			while (idCliente < CANT_THREADS) {
				try {
					// LLEGADA DEL NUEVO CLIENTE
					conections[idCliente] = socket.accept();
					
					// CONFIGURACI�N DE CANALES CON EL CLIENTE
					DataOutputStream _DOS = new DataOutputStream(conections[idCliente].getOutputStream());
					DataInputStream _DIS = new DataInputStream(conections[idCliente].getInputStream());
					System.out.println("Conectando con el cliente #: " + (++idCliente) );
					
					// ENV�A SU NOMBRE Y ID
					_DOS.writeByte(1);
					_DOS.writeUTF(fileName);
					_DOS.flush();
					
					_DOS.write(idCliente);
					
					// CONFIRMACI�N DE LA CONEXI�N
					if(_DIS.readUTF().contentEquals("OK")) {
						System.out.println("Cliente #"+ idCliente + " recibi� el nombre del archivo y su ID");
						writeLog("Conexi�n exitosa con el cliente #" + idCliente);
					}
					
				} catch (Exception e) {
					System.out.println("Error de conexi�n con los clientes");
				}
			}
			System.out.println("Inicio de envi� del archivo " + fileName + " a los " + CANT_THREADS + " clientes.");
			for (int i = 0; i < conections.length; i++) {
				writeLog("Inicia envi� al cliente #" + (i + 1));
				ProtocolThread thread = new ProtocolThread(conections[i], fileName, logWriter, (i + 1));
				thread.start();
			}
		}
		catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(1);
			try {
				logWriter.write("Hubo un error con el env�o");
				logWriter.newLine();
				logWriter.flush();
			} catch (Exception ex) {
				// TODO: handle exception
			}
		}
	}
	
	/**
	 * Escribe el mensaje en el log writer
	 * @param message Mensaje a escribir
	 * @throws IOException
	 */
	private static void writeLog(String message) throws IOException {
		logWriter.write(message);
		logWriter.newLine();
		logWriter.flush();
	}
}