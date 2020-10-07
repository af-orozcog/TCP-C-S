package ServerTCP;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.security.MessageDigest;

public class ProtocolThread extends Thread {

	public final static String ARCHIVO_PATH = "data/textos/";

	public final static int MESSAGE_SIZE = 1024; // Tamaño de los paquetes enviados.

	public String archivo = "";

	private Socket socket = null;

	private static BufferedWriter logWriter;

	private int clientId;

	public ProtocolThread(Socket pSocket, String nombreArchivo, BufferedWriter logWriter, int id) {
		socket = pSocket;
		ProtocolThread.logWriter = logWriter;
		clientId = id;
		archivo = ARCHIVO_PATH + nombreArchivo;
		try {
			socket.setSoTimeout(30000);
		} catch (SocketException e) {
			e.printStackTrace();
		}

	}

	public void run() {

		try {
			sendFile();
			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
			try {
				writeLog("Error en el socket");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

	}

	public void sendFile() {

		FileInputStream fis = null;
		BufferedInputStream bis = null;

		OutputStream os = null;

		try {
			os = new BufferedOutputStream(socket.getOutputStream());
			File myFile = new File(archivo);
			byte[] myByteArray = new byte[(int) myFile.length()];
			fis = new FileInputStream(myFile);
			bis = new BufferedInputStream(fis);

			MessageDigest shaDigest = MessageDigest.getInstance("SHA-256");
			String hashEnviar = checkSum(shaDigest, myFile );
			DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
			dOut.writeUTF(hashEnviar);
			dOut.writeInt((int)myFile.length());

			bis.read(myByteArray, 0, myByteArray.length);

			System.out.println("Enviando (" + myByteArray.length + " bytes)");

			int bytesEnviados = 0;
			int numPaquetes = 0;
			while (bytesEnviados < myByteArray.length) {
				numPaquetes++;
				if ((bytesEnviados + MESSAGE_SIZE) < myByteArray.length) {
					os.write(myByteArray, bytesEnviados, MESSAGE_SIZE);
					bytesEnviados += MESSAGE_SIZE;
				} else {
					int faltaPorEnviar = myByteArray.length - bytesEnviados;
					os.write(myByteArray, bytesEnviados, faltaPorEnviar);
					bytesEnviados += faltaPorEnviar + 1;
				}
			}
			
			// IMPORTANTE EL CLIENTE VA A ENVIAR
			// UN BYTE SI EL ARCHVIO SI ES INTEGRO
			// 1 SI ESTÁ BIEN, 0 SI ESTA DAÑADO	
			
			logWriter.write("Se enviaron en total " + numPaquetes + " paquetes al cliente " + clientId + " para un toltal de " + numPaquetes*256 + " bytes");
			logWriter.newLine();
			logWriter.flush();

			logWriter.write("Cada paquete con un tamaño de " + MESSAGE_SIZE + " bytes");
			logWriter.newLine();
			logWriter.flush();

			os.flush();
			System.out.println("Archivo enviado.");
			logWriter.write("Archivo enviado exitosamente al cliente " + clientId);
			logWriter.newLine();
			logWriter.flush();

			logWriter.close();
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
	
	/**
	 * Escribe el mensaje en el log writer
	 * @param log Mensaje a escribir
	 * @throws IOException
	 */
	private static void writeLog(String log) throws IOException {
		logWriter.write(log);
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