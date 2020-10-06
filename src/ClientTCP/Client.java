package ClientTCP;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Scanner;

public class Client {

	private static final int MESSAGE_SIZE = 1024;
	
	private static final String DIR_DESCARGA = "data/descargas/des_";
	public final static String UBICACION_LOG = "data/logs/log_";
	
	
	private static BufferedWriter logWriter;

	/**
	 * Método principal del cliente, se encarga de la comunicación con el servidor
	 * y dar inicio a la descarga de archivos
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Cliente TCP: ");
		
		// HERRAMIENTA PARA LECTURA DE LA CONFIGURACIÓN DE LA CONEXIÓN
		Scanner console = new Scanner(System.in);
		
		// PREPARACIÓN DEL SOCKET
		Socket socket = null;

		try {
			// INCIALIZACIÓN DEL LOG SEGÚN FECHA
			logWriter = new BufferedWriter(new FileWriter(	new File(UBICACION_LOG + 
										(new SimpleDateFormat("HH-mm-ss_dd/MM/yyyy")
										.format(Calendar.getInstance().getTime())) + ".txt")));
			
			// 2. Conectarse al servidor TCP y mostrar que se ha realizado dicha conexión. 
			// 	  Mostrar el estado de la conexión.

			// CREACIÓN DEL SOCKET 
			//LECTURA DE IP Y PUERTO PARA REALIZAR LA CONEXIÓN
			System.out.println("Escriba la dirección ip del servidor al que se desea conectar:");
			String direccion = console.next();

			System.out.println("Escriba el puerto del servidor:");
			int puerto = console.nextInt();
						
			// CONEXIÓN AL SOCKET
			socket = new Socket(direccion, puerto);
			System.out.println("ESTADO DE CONEXIÓN: " + !socket.isClosed() + " Esperando nombre del archivo...");

			// CANAL DE ENTRADA Y SALIDA CON EL SERVIDOR
			DataInputStream inputStream = new DataInputStream(socket.getInputStream());
			DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
						
			if (inputStream.readByte() == 1) {
				int id = inputStream.readByte();
				String fileName = inputStream.readUTF();
				System.out.println("Nombre del archivo: " + fileName + " - ID del archivo: " + id);
				
				// 3. Enviar notificación de preparado para recibir datos de parte del servidor.
				// INFORMA AL SERVIDOR QUE SE VA A INICIAR LA DESCARGA
				outputStream.writeByte(2);
				download(id, fileName, socket);
			}
			else {
				System.out.println("Formato erróneo");
			}
			
		}

		catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}
		
		finally {
			console.close();
			try {
				socket.close();
			} catch (IOException e) {
				System.out.println("Socket error - IOException: " + e.getMessage());
			}

		}
	}

	public static void download( int id, String fileName, Socket socket ) {
		

		
		
		int currentByte = 0;

		try {
			
			// CANAL PARA LECTURA DEL ARCHIVO
			FileOutputStream _FOS = new FileOutputStream(DIR_DESCARGA + fileName);
			BufferedOutputStream _BOS = new BufferedOutputStream(_FOS);

			// LOG DE LA FECHA, NOMBRE Y TAMAÑO
			String timeLog = new SimpleDateFormat("HH-mm-ss_dd/MM/yyyy").format(Calendar.getInstance().getTime());
			writeLog("Time_Date: " + timeLog);
			writeLog("File name: " + fileName + ".");
			writeLog("File size: " + MESSAGE_SIZE + " bytes.");

			// Recibir Hash
			DataInputStream dIn = new DataInputStream(socket.getInputStream());
			String hashRecibido = dIn.readUTF();
			
			// Comenzar a recibir el archivo
			// INICIALIZACION DEL BUFFER Y EL INPUTSTREAM PARA RECIBIR EL ARCHIVO
			byte[] bufferByte = new byte[700000000];
			InputStream is = socket.getInputStream();
			
			// EN EL MOMENTO EN QUE SE RECIBA EL PRIMER PAQUETE SE INICIA LA RECEPCIÓN DEL ARCHIVO
			int readedBytes = 0;
			while (readedBytes == 0) {
				readedBytes = is.read(bufferByte, 0, bufferByte.length);
			}
			System.out.println("Inicia la descarga");
			currentByte = readedBytes;
			int numPaquetes = 1;

			// VARIABLE PARA MEDIR EL TIEMPO DE DESCARGA
			long startTime = System.currentTimeMillis();

			int totalMessages = 0;
			int totalBytes = 0;
			do {
				// LECTURA DE UN PAQUETE
				readedBytes = is.read(bufferByte, currentByte, (bufferByte.length - currentByte));
				numPaquetes++;
				
				// CALCULO DEL TOTAL DE BYTES LEÍDOS Y EL NÚMERO DE MENSAJES LEÍDOS
				totalBytes += readedBytes;
				if (totalBytes >= MESSAGE_SIZE) {
					int readedMessages =  (totalBytes / MESSAGE_SIZE);
					totalMessages += readedMessages;
					totalBytes -= MESSAGE_SIZE * readedMessages;
				}
				
				// ACTUALIZACIÓN DEL BYTE ACTUAL DEL MENSAJE
				if (readedBytes >= 0)
					currentByte += readedBytes;
				
			} while (readedBytes > -1);

			_BOS.write(bufferByte, 0, currentByte);
			_BOS.flush();

			long endTime = System.currentTimeMillis();
			
			System.out.println("Archivo descargado (" + currentByte + " bytes leidos)");

			System.out.println("La descarga tomó " + (endTime - startTime) + " milisegundos");

			logWriter.write("Se recibieron en total " + numPaquetes + " paquetes.");
			logWriter.newLine();
			logWriter.flush();

			logWriter.write("Archivo descargado (" + currentByte + " bytes leidos)");
			logWriter.newLine();
			logWriter.flush();

			logWriter.write("La descarga tomó " + (endTime - startTime) + " milisegundos");
			logWriter.newLine();
			logWriter.flush();

			DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
			dOut.writeByte(id);

			// Verificación del hash
			File myFile = new File(DIR_DESCARGA + fileName);
			byte[] bytesArchivo = new byte[(int) myFile.length()];
			byte[] hashSacado = new byte[1];
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			hashSacado = md.digest(bytesArchivo);
			String hashGenerado = new String(hashSacado);
			if (hashRecibido.equals(hashGenerado)) {
				System.out.println("Se completo la verificación del hash exitosamente");
				logWriter.write("Se verificó la integridad de los archivos recibidos");
				logWriter.newLine();
				logWriter.flush();
			} else
				System.out.println("No se pudo comprobar la integridad del archivo");

			_FOS.close();
			_BOS.close();

		} catch (Exception e) {
			System.out.println(e.getMessage());
			try {
				logWriter.write("Hubo un error con el envío");
				logWriter.newLine();
				logWriter.flush();
			} catch (Exception ex) {
				// TODO: handle exception
			}
		}
	}

	
	public static void writeLog(String message) throws IOException {
		logWriter.write(message);
		logWriter.newLine();
		logWriter.flush();
	}
}