package ClientTCP;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
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

	private static final int BUFFER_SIZE = 1024;
	
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
			String time = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss").format(Calendar.getInstance().getTime());
			File logFile = new File(UBICACION_LOG + time + ".txt");
			logWriter = new BufferedWriter(new FileWriter(logFile));
			
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
				fileDownload(id, fileName, socket);
			}
			else {
				System.out.println("Formato erróneo");
			}
			
		}
		
		// Manejo de excepciones para saber donde ocurrio el error
		catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}
		// cierre de los sockets sin importar como este estructurado el computador
		finally {
			console.close();
			try {
				socket.close();
			} catch (IOException e) {
				System.out.println("Socket error - IOException: " + e.getMessage());
			}

		}
	}

	/**
	 * Metodo para descargar los archivos
	 * @param id
	 * @param fileName
	 * @param socket
	 * @throws IOException
	 */
	public static void fileDownload( int id, String fileName, Socket socket ) throws IOException {
		// CANAL PARA LECTURA DEL ARCHIVO
		FileOutputStream _FOS = new FileOutputStream(DIR_DESCARGA + fileName);
		BufferedOutputStream _BOS  = new BufferedOutputStream(_FOS);;

		// CANAL DE COMUNICACIÓN CON EL SERVIDOR
		DataInputStream _DIS = new DataInputStream(socket.getInputStream());
		DataOutputStream _DOS = new DataOutputStream(socket.getOutputStream());
		
		try {
			
			// LOG DE LA FECHA, NOMBRE Y TAMAÑO
			String timeLog = new SimpleDateFormat("HH-mm-ss_dd/MM/yyyy").format(Calendar.getInstance().getTime());
			writeLog("Hora_Fecha: " + timeLog);
			writeLog("Nombre del archivo: " + fileName + ".");
			writeLog("Tamaño máximo de los paquetes: " + BUFFER_SIZE + " bytes.");
			
			// RECIBE EL HASH DEL ARCHIVO
			String serverHash = _DIS.readUTF();
			
			// RECIBE EL TAMAÑO DEL ARCHIVO
			int fileSize = _DIS.readInt();
			
			// INICIALIZACION DEL BUFFER Y EL INPUTSTREAM PARA RECIBIR EL ARCHIVO
			byte[] buffer = new byte[fileSize];
			InputStream is = socket.getInputStream();
			
			// 4. Recibir un archivo del servidor por medio de una comunicación a través de sockets TCP.
			// EN EL MOMENTO EN QUE SE RECIBA EL PRIMER PAQUETE SE INICIA LA RECEPCIÓN DEL ARCHIVO
			int readedBytes = 0;
			while (readedBytes == 0) {
				readedBytes = is.read(buffer, 0, BUFFER_SIZE);
			}
			System.out.println("Inicia la descarga");
			int currentByte = readedBytes;
			int numPaquetes = 1;

			// VARIABLE PARA MEDIR EL TIEMPO DE DESCARGA
			long totalTime = System.currentTimeMillis();
			
			// LECTURA DE LOS SIGUIENTES PAQUETES
			readedBytes = is.read(buffer, currentByte, BUFFER_SIZE);
			while (readedBytes > -1) {
				// LECTURA DE UN PAQUETE
				numPaquetes++;
		
				// ACTUALIZACIÓN DEL BYTE ACTUAL DEL MENSAJE
				if (readedBytes >= 0)
					currentByte += readedBytes;
				
				// LECTURA DEL SIGUIENTE PAQUETE
				readedBytes = is.read(buffer, currentByte, BUFFER_SIZE);
			}
			// SE GUARDA EL TIEMPO TOTAL EN EJECUTARSE
			totalTime = System.currentTimeMillis() - totalTime;

			// 5. Verificar la integridad del archivo con respeto a la información entregada por el servidor. 
			// GENERACIÓN DEL HASH
			MessageDigest shaDigest = MessageDigest.getInstance("SHA-256");
			String generatedHash = checkSum(shaDigest, ( new File(DIR_DESCARGA + fileName) ) );
			
			// VERIFICACIÓN DE INTEGRIDAD
			boolean integridad = serverHash.equals(generatedHash);
			
			// 6. Enviar notificación de recepción del archivo al servidor.
			if(integridad) {
				_DOS.writeByte(1);
				System.out.println("Verificada la integridad del archivo");
			}
			else {
				_DOS.writeByte(0);
				System.out.println("Integridad del archivo comprometida");
			}

			// 7. La aplicación debe reportar si el archivo está completo, correcto y 
			// el tiempo total de transferencia, para esto genere un log para cada intercambio de
			// datos entre cliente y servidor.
			
			// ALMACENAMIENTO DEL ARCHIVO EN EL LUGAR DE DESCARGA
			_BOS.write(buffer, 0, currentByte);
			_BOS.flush();
			
			// REPORTE AL USUARIO
			System.out.println("Descarga terminada. Tamaño del archivo: " + currentByte);
			System.out.println("Tiempo total de la descarga: " + totalTime );
			System.out.println("Número de paquetes leídos: " + numPaquetes);
			System.out.println("El archivo está completo?: " + (currentByte == fileSize) );
			System.out.println("El archivo está correcto?: " + integridad);
			
			// REPORTE GURARDADO EN EL LOG	
			writeLog("Descarga terminada. Tamaño del archivo: " + currentByte);
			writeLog("Tiempo total de la descarga: " + totalTime );
			writeLog("Número de paquetes leídos: " + numPaquetes);
			writeLog("El archivo está completo?: " + (currentByte == fileSize) );
			writeLog("El archivo está correcto?: " + integridad);

		} catch (Exception e) {
			System.out.println(e.getMessage());
			writeLog("Error la recepción del archivo: " + e.getMessage());
		}
		finally {
			_FOS.close();
			_BOS.close(); 
			_DIS.close();
			_DOS.close();
		}
	}

	
	private static void writeLog(String message) throws IOException {
		logWriter.write(message);
		logWriter.newLine();
		logWriter.flush();
	}
	
	
	/**
	 * Checksum hash example from from: https://howtodoinjava.com/java/io/sha-md5-file-checksum-hash/
	 * @param digest
	 * @param file
	 * @return
	 * @throws IOException
	 */
	private static String checkSum(MessageDigest digest, File file) throws IOException
	{
	    //Get file input stream for reading the file content
	    FileInputStream fis = new FileInputStream(file);
	     
	    //Create byte array to read data in chunks
	    byte[] byteArray = new byte[1024];
	    int bytesCount = 0; 
	      
	    //Read file data and update in message digest
	    while ((bytesCount = fis.read(byteArray)) != -1) {
	        digest.update(byteArray, 0, bytesCount);
	    };
	     
	    //close the stream; We don't need it now.
	    fis.close();
	     
	    //Get the hash's bytes
	    byte[] bytes = digest.digest();
	     
	    //This bytes[] has bytes in decimal format;
	    //Convert it to hexadecimal format
	    StringBuilder sb = new StringBuilder();
	    for(int i=0; i< bytes.length ;i++)
	    {
	        sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
	    }
	     
	    //return complete hash
	   return sb.toString();
	}
}