package ClientTCP;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Scanner;

public class Client {
	
	private static final String DOWNLOADS_PATH = "data/downloads/download-";
	public final static String lOG_PATH = "data/informes/log-";
	
	public static String DIRECCION = "192.168.4.162";
	public static int PUERTO = 49200;
	
	/**
	 * M�todo principal del cliente, se encarga de la comunicaci�n con el servidor
	 * y dar inicio a la descarga de archivos
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Cliente TCP: ");
		
		// HERRAMIENTA PARA LECTURA DE LA CONFIGURACI�N DE LA CONEXI�N
		Scanner console = new Scanner(System.in);
		
		// PREPARACI�N DEL SOCKET
		Socket socket = null;

		try {
			// 2. Conectarse al servidor TCP y mostrar que se ha realizado dicha conexi�n. 
			// 	  Mostrar el estado de la conexi�n.

			//LECTURA DE IP Y PUERTO PARA REALIZAR LA CONEXI�N
			//System.out.println("Escriba la direcci�n ip del servidor al que se desea conectar:");
			//String direccion = console.next();

			//System.out.println("Escriba el puerto del servidor:");
			//int puerto = console.nextInt();
						
			// CONEXI�N AL SOCKET
			System.out.println("Conectando al servidor...");
			socket = new Socket(DIRECCION, PUERTO);
			System.out.println("ESTADO DE CONEXION: " + !socket.isClosed() + " Esperando nombre del archivo...");

			// CANAL DE ENTRADA Y SALIDA CON EL SERVIDOR
			DataInputStream _DIS = new DataInputStream(socket.getInputStream());
			DataOutputStream _DOS = new DataOutputStream(socket.getOutputStream());
						
			if (_DIS.readByte() == 1) {
				// 3. Enviar notificaci�n de preparado para recibir datos de parte del servidor.
				
				// RECIBE DEL SERVIDOR EL ID Y EL NOMBRE DEL ARCHIVO
				String fileName = _DIS.readUTF();
				int id = _DIS.readByte();
				System.out.println("Nombre del archivo: " + fileName + " - ID del archivo: " + id);

				// INFORMA AL SERVIDOR QUE SE VA A INICIAR LA DESCARGA
				_DOS.writeUTF("OK");
				fileDownload(id, fileName, socket);
			}
			else {
				System.out.println("Formato erroneo");
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
		FileOutputStream _FOS = new FileOutputStream(DOWNLOADS_PATH + fileName);
		BufferedOutputStream _BOS  = new BufferedOutputStream(_FOS);

		// CANAL DE COMUNICACI�N CON EL SERVIDOR
		DataInputStream _DIS = new DataInputStream(socket.getInputStream());
		DataOutputStream _DOS = new DataOutputStream(socket.getOutputStream());
		
		try {
			
			// LOG DE LA FECHA, NOMBRE Y TAMA�O
			String timeLog = new SimpleDateFormat("HH-mm-ss_dd/MM/yyyy").format(Calendar.getInstance().getTime());
			
			// RECIBE EL HASH DEL ARCHIVO
			String serverHash = _DIS.readUTF();
			
			// RECIBE EL TAMA�O DEL ARCHIVO
			int fileSize = _DIS.readInt();
			
			// INICIALIZACION DEL BUFFER Y EL INPUTSTREAM PARA RECIBIR EL ARCHIVO
			byte[] buffer = new byte[700000000];
			InputStream is = socket.getInputStream();
			
			// 4. Recibir un archivo del servidor por medio de una comunicaci�n a trav�s de sockets TCP.
			// RECEPCI�N DEL PRIMER PAQUETE
			int readedBytes = is.read(buffer, 0, buffer.length);
			int currentByte = readedBytes;
			int numPaquetes = 1;

			System.out.println("Inicia la descarga del archivo");

			// VARIABLE PARA MEDIR EL TIEMPO DE DESCARGA
			long totalTime = System.currentTimeMillis();
			
			// LECTURA DE LOS SIGUIENTES PAQUETES
			readedBytes = is.read(buffer, currentByte, (buffer.length - currentByte));
			while (readedBytes > -1) {
				// LECTURA DE UN PAQUETE
				numPaquetes++;
		
				// ACTUALIZACI�N DEL BYTE ACTUAL DEL MENSAJE
				if (readedBytes >= 0)
					currentByte += readedBytes;
				
				// LECTURA DEL SIGUIENTE PAQUETE
				readedBytes = is.read(buffer, currentByte, (buffer.length - currentByte));
			}
			
			// SE GUARDA EL TIEMPO TOTAL EN EJECUTARSE
			totalTime = System.currentTimeMillis() - totalTime;
			
			// ALMACENAMIENTO DEL ARCHIVO EN EL LUGAR DE DESCARGA
			_BOS.write(buffer, 0, currentByte);
			_BOS.flush();

			// 5. Verificar la integridad del archivo con respeto a la informaci�n entregada por el servidor. 
			// GENERACI�N DEL HASH
			MessageDigest shaDigest = MessageDigest.getInstance("SHA-256");
			String generatedHash = hash(shaDigest, ( new File(DOWNLOADS_PATH + fileName) ) );
			
			// VERIFICACI�N DE INTEGRIDAD
			boolean integridad = serverHash.equals(generatedHash);
			
			// 6. Enviar notificaci�n de recepci�n del archivo al servidor.
			if(integridad) {
				_DOS.writeByte(1);
				System.out.println("Verificada la integridad del archivo");
			}
			else {
				_DOS.writeByte(0);
				System.out.println("Integridad del archivo comprometida");
			}

			// 7. La aplicaci�n debe reportar si el archivo est� completo, correcto y 
			// el tiempo total de transferencia, para esto genere un log para cada intercambio de
			// datos entre cliente y servidor.
			
			// REPORTE AL USUARIO
			System.out.println("Descarga terminada. Tamanio del archivo: " + currentByte);
			System.out.println("Tiempo total de la descarga: " + totalTime + "miliseconds" );
			System.out.println("N�mero de paquetes le�dos: " + numPaquetes);
			System.out.println("El archivo est� completo?: " + (currentByte == fileSize));
			System.out.println("El archivo est� correcto?: " + integridad);
			

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		finally {
			_FOS.close();
			_BOS.close(); 
			_DIS.close();
			_DOS.close();
		}
	}
	
	
	/**
	 * Calcula el hash del archivo usando un digest
	 * @param digest Digest a utilizar
	 * @param file Archivo
	 * @return El hash del archivo
	 * @throws IOException
	 */
	private static String hash(MessageDigest digest, File file) throws IOException
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